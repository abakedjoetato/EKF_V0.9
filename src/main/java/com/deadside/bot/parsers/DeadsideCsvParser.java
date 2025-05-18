package com.deadside.bot.parsers;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.GuildConfig;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.GuildConfigRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parser for Deadside CSV death log files
 * Format: timestamp;victim;victimId;killer;killerId;weapon;distance
 */
public class DeadsideCsvParser {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideCsvParser.class);
    private final JDA jda;
    private final SftpConnector sftpConnector;
    private final PlayerRepository playerRepository;
    
    // Map to keep track of processed files for each server
    private final Map<String, Set<String>> processedFiles = new HashMap<>();
    
    // Flag to indicate if we're processing historical data (to prevent sending to killfeed channels)
    private boolean isProcessingHistoricalData = false;
    
    // Format of the CSV death log: timestamp;victim;victimId;killer;killerId;weapon;distance;victimPlatform;killerPlatform
    private static final Pattern CSV_LINE_PATTERN = Pattern.compile("^\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2};.*;.*;.*;.*;.*;\\d+;.*;.*;$");
    private static final SimpleDateFormat CSV_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
    
    // Death causes
    private static final Set<String> SUICIDE_CAUSES = new HashSet<>(Arrays.asList(
            "suicide_by_relocation", "suicide", "falling", "bleeding", "drowning", "starvation"
    ));
    
    public DeadsideCsvParser(JDA jda, SftpConnector sftpConnector, PlayerRepository playerRepository) {
        this.jda = jda;
        this.sftpConnector = sftpConnector;
        this.playerRepository = playerRepository;
        
        // Set timezone for date parsing
        CSV_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    /**
     * Process death logs for a server (default behavior - not processing historical data)
     * @param server The game server to process
     * @return Number of deaths processed
     */
    public int processDeathLogs(GameServer server) {
        return processDeathLogs(server, false);
    }
    
    /**
     * Process death logs for a server
     * @param server The game server to process
     * @param processHistorical If true, reprocess all files even if already processed before
     * @return Number of deaths processed
     */
    public int processDeathLogs(GameServer server, boolean processHistorical) {
        int totalProcessed = 0;
        
        try {
            // Set historical processing flag to prevent output to killfeed channels during historical processing
            this.isProcessingHistoricalData = processHistorical;
            
            // Check if the server has a configured guild
            if (server.getGuildId() == 0) {
                logger.warn("Server {} has no configured Discord guild", server.getName());
                return 0;
            }
            
            // Get list of already processed files for this server
            Set<String> processed = processedFiles.computeIfAbsent(
                    server.getName(), k -> new HashSet<>());
            
            // Get CSV files from the server - limit to the most recent files for performance
            List<String> csvFiles;
            if (processHistorical) {
                // For historical processing, get all files
                csvFiles = sftpConnector.findDeathlogFiles(server);
            } else {
                // For regular processing, only get the newest file or files not yet processed
                csvFiles = sftpConnector.findRecentDeathlogFiles(server, 1);
            }
            
            if (csvFiles.isEmpty()) {
                return 0;
            }
            
            // Sort files by name (which includes date)
            Collections.sort(csvFiles);
            
            // Process each file that hasn't been processed yet
            for (String csvFile : csvFiles) {
                // Skip already processed files (unless we're reprocessing historical data)
                if (!processHistorical && processed.contains(csvFile)) {
                    continue;
                }
                
                try {
                    String content = sftpConnector.readDeathlogFile(server, csvFile);
                    int deathsProcessed = processDeathLog(server, content);
                    totalProcessed += deathsProcessed;
                    
                    // Mark as processed
                    processed.add(csvFile);
                    logger.info("Processed death log file {} for server {}, {} deaths", 
                            csvFile, server.getName(), deathsProcessed);
                } catch (Exception e) {
                    logger.error("Error processing death log file {} for server {}: {}", 
                            csvFile, server.getName(), e.getMessage(), e);
                }
            }
            
            // Limit the size of the processed files set to prevent memory issues
            if (processed.size() > 100) {
                // Keep only the most recent 50 files
                List<String> filesList = new ArrayList<>(processed);
                Collections.sort(filesList);
                Set<String> newProcessed = new HashSet<>(
                        filesList.subList(Math.max(0, filesList.size() - 50), filesList.size()));
                processedFiles.put(server.getName(), newProcessed);
            }
            
            return totalProcessed;
        } catch (Exception e) {
            logger.error("Error processing death logs for server {}: {}", 
                    server.getName(), e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Process a death log file content
     * @param server The game server
     * @param content The file content
     * @return Number of deaths processed
     */
    private int processDeathLog(GameServer server, String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        
        String[] lines = content.split("\\n");
        int count = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            // Simple validation that this looks like a death log line
            if (!CSV_LINE_PATTERN.matcher(line).matches()) {
                continue;
            }
            
            try {
                String[] parts = line.split(";");
                if (parts.length < 9) {
                    logger.warn("Death log entry has fewer fields than expected ({} vs 9): {}", parts.length, line);
                    continue;
                }
                
                // Parse death log entry
                String timestamp = parts[0];
                String victim = parts[1];
                String victimId = parts[2];
                String killer = parts[3];
                String killerId = parts[4];
                String weapon = parts[5];
                int distance = Integer.parseInt(parts[6]);
                String victimPlatform = parts[7];
                String killerPlatform = parts[8];
                
                // Only check timestamp if the server has a non-zero lastProcessedTimestamp
                try {
                    Date deathTime = CSV_DATE_FORMAT.parse(timestamp);
                    if (server.getLastProcessedTimestamp() > 0 && deathTime.getTime() < server.getLastProcessedTimestamp()) {
                        logger.debug("Skipping old death from {} ({}), server timestamp is {}", 
                                timestamp, deathTime.getTime(), server.getLastProcessedTimestamp());
                        continue;
                    }
                } catch (ParseException e) {
                    logger.warn("Could not parse death timestamp: {}", timestamp);
                }
                
                // Process death
                processDeath(server, timestamp, victim, victimId, killer, killerId, weapon, distance);
                count++;
            } catch (Exception e) {
                logger.warn("Error processing death log line: {}", line, e);
            }
        }
        
        // Update server's last processed timestamp
        if (count > 0) {
            server.setLastProcessedTimestamp(System.currentTimeMillis());
        }
        
        return count;
    }
    
    /**
     * Process a death event
     */
    private void processDeath(GameServer server, String timestamp, String victim, String victimId, 
                             String killer, String killerId, String weapon, int distance) {
        try {
            // Handle different death types
            boolean isSuicide = SUICIDE_CAUSES.contains(weapon.toLowerCase()) || 
                                victim.equals(killer);
            
            if (isSuicide) {
                // Send suicide message to death channel
                sendSuicideKillfeed(server, timestamp, victim, victimId, weapon);
            } else {
                // Determine appropriate event type based on the death
                String eventType = "kill"; // Default
                
                // Special event types
                if (weapon.toLowerCase().contains("airdrop") || 
                    weapon.toLowerCase().contains("supply")) {
                    eventType = "airdrop";
                } else if (distance > 300) {
                    eventType = "longshot";
                }
                
                // Send to appropriate channel based on event type
                sendPlayerKillKillfeed(server, timestamp, victim, victimId, killer, killerId, weapon, distance, eventType);
                
                // Update player stats with weapon and distance information
                updateKillerStats(killer, killerId, weapon, distance);
                updateVictimStats(victim, victimId);
            }
        } catch (Exception e) {
            logger.error("Error processing death: {} killed by {}: {}", victim, killer, e.getMessage(), e);
        }
    }
    
    /**
     * Update the killer's stats
     */
    private void updateKillerStats(String killer, String killerId) {
        updateKillerStats(killer, killerId, null);
    }
    
    /**
     * Update the killer's stats with weapon information
     */
    private void updateKillerStats(String killer, String killerId, String weapon) {
        // Default distance for older method calls
        updateKillerStats(killer, killerId, weapon, 0);
    }
    
    /**
     * Update the killer's stats with weapon and distance information
     */
    private void updateKillerStats(String killer, String killerId, String weapon, int distance) {
        try {
            Player player = playerRepository.findByDeadsideId(killerId);
            if (player == null) {
                // If player doesn't exist in database, don't create yet
                // They can be created when they link their account
                return;
            }
            
            // Update kills and score
            player.setKills(player.getKills() + 1);
            
            // Base score per kill
            int scoreIncrease = 10;
            
            // Bonus points for special kills
            if (weapon != null) {
                if (weapon.toLowerCase().contains("airdrop") || 
                    weapon.toLowerCase().contains("supply")) {
                    // Airdrop kills are more valuable
                    scoreIncrease += 15;
                } else if (distance > 300) {
                    // Longshot kills
                    scoreIncrease += (int)(distance / 100); // +1 point per 100m over 300m
                }
            }
            
            // Add score to player
            player.setScore(player.getScore() + scoreIncrease);
            
            // Add kill reward
            // TODO: Add economy reward here if implemented
            
            playerRepository.save(player);
        } catch (Exception e) {
            logger.error("Error updating killer stats for {}: {}", killer, e.getMessage(), e);
        }
    }
    
    /**
     * Update the victim's stats
     */
    private void updateVictimStats(String victim, String victimId) {
        try {
            Player player = playerRepository.findByDeadsideId(victimId);
            if (player == null) {
                // If player doesn't exist in database, don't create yet
                return;
            }
            
            // Update deaths
            player.setDeaths(player.getDeaths() + 1);
            
            playerRepository.save(player);
        } catch (Exception e) {
            logger.error("Error updating victim stats for {}: {}", victim, e.getMessage(), e);
        }
    }
    
    /**
     * Send killfeed message for player kill
     */
    private void sendPlayerKillKillfeed(GameServer server, String timestamp, String victim, String victimId,
                                       String killer, String killerId, String weapon, int distance) {
        sendPlayerKillKillfeed(server, timestamp, victim, victimId, killer, killerId, weapon, distance, "kill");
    }
    
    /**
     * Send killfeed message for player kill with specific event type using modern styling
     */
    private void sendPlayerKillKillfeed(GameServer server, String timestamp, String victim, String victimId,
                                       String killer, String killerId, String weapon, int distance, String eventType) {
        try {
            MessageEmbed embed;
            
            // Check if this is a special kill type and create appropriate embed
            if (eventType.equals("airdrop")) {
                // For airdrop kills, use a specialized airdrop embed
                embed = EmbedUtils.airdropEmbed("Airdrop Kill", 
                        killer + " eliminated " + victim + " near an airdrop");
            } else if (eventType.equals("longshot")) {
                // For longshot kills
                String title = "Longshot Elimination";
                String description = killer + " sniped " + victim + " from an impressive " + distance + "m";
                
                embed = new EmbedBuilder()
                        .setTitle(title)
                        .setDescription(description)
                        .setColor(EmbedUtils.STEEL_BLUE) // Steel blue for longshots
                        .addField("Marksman", killer, true)
                        .addField("Target", victim, true)
                        .addField("Weapon", weapon, true)
                        .addField("Distance", distance + "m", true)
                        .setFooter(EmbedUtils.STANDARD_FOOTER)
                        .setTimestamp(Instant.now())
                        .build();
            } else {
                // For standard kills
                embed = new EmbedBuilder()
                        .setTitle("Elimination Confirmed")
                        .setDescription(killer + " eliminated " + victim)
                        .setColor(EmbedUtils.EMERALD_GREEN)
                        .addField("Killer", killer, true)
                        .addField("Victim", victim, true)
                        .addField("Weapon", weapon, true)
                        .addField("Distance", distance + "m", true)
                        .setFooter(EmbedUtils.STANDARD_FOOTER)
                        .setTimestamp(Instant.now())
                        .build();
            }
            
            sendToKillfeedChannel(server, embed, eventType);
        } catch (Exception e) {
            logger.error("Error sending kill feed for {}: {}", victim, e.getMessage(), e);
        }
    }
    
    /**
     * Send killfeed message for suicide with modern styling
     */
    private void sendSuicideKillfeed(GameServer server, String timestamp, String victim, String victimId, String cause) {
        try {
            MessageEmbed embed;
            
            // Check if it's falling damage or another type of suicide
            if (cause.equals("falling")) {
                // For falling deaths, use the specialized embed
                int height = 0; // Default height (not available in log)
                embed = EmbedUtils.fallingDeathEmbed(victim, height);
            } else {
                // For regular suicides, use suicide embed with normalized cause
                embed = EmbedUtils.suicideEmbed(victim, cause);
            }
            
            // Send to death channel
            sendToKillfeedChannel(server, embed, "death");
        } catch (Exception e) {
            logger.error("Error sending suicide feed for {}: {}", victim, e.getMessage(), e);
        }
    }
    
    /**
     * Send embed message to the appropriate channel
     * 
     * @param server The server to send the message for
     * @param embed The message embed to send
     * @param eventType The type of event (defaults to "death" if not specified)
     */
    private void sendToKillfeedChannel(GameServer server, net.dv8tion.jda.api.entities.MessageEmbed embed) {
        sendToKillfeedChannel(server, embed, "death");
    }
    
    /**
     * Send embed message to the appropriate channel based on event type
     * Non-historical data is sent to killfeed channels, historical data is not
     */
    private void sendToKillfeedChannel(GameServer server, net.dv8tion.jda.api.entities.MessageEmbed embed, String eventType) {
        // Check if we're in historical processing mode - skip sending to channels if true
        if (isProcessingHistoricalData) {
            logger.debug("Skipping killfeed output during historical data processing for server {}", server.getName());
            return;
        }
        
        TextChannel killfeedChannel = getTextChannel(server, eventType);
        if (killfeedChannel == null) {
            logger.warn("No suitable channel found for {} events for server {}", 
                    eventType, server.getName());
            return;
        }
        
        killfeedChannel.sendMessageEmbeds(embed).queue(
                success -> logger.debug("Sent killfeed to channel {}", killfeedChannel.getId()),
                error -> logger.error("Failed to send killfeed: {}", error.getMessage())
        );
    }
    
    /**
     * Get the appropriate Text Channel to send messages to based on event type
     * @param server The game server
     * @param eventType The type of event (kill, death, connection, airdrop, etc.)
     * @return The text channel, or null if not found/configured
     */
    private TextChannel getTextChannel(GameServer server, String eventType) {
        if (jda == null || server.getGuildId() == 0) {
            return null;
        }
        
        // Default to log channel from server config
        long channelId = server.getLogChannelId();
        
        // Try to find guild configuration for multi-channel support
        GuildConfigRepository guildConfigRepo = new GuildConfigRepository();
        GuildConfig guildConfig = guildConfigRepo.findByGuildId(server.getGuildId());
        
        if (guildConfig != null) {
            // If guild has specialized channels, use the appropriate one for the event type
            long eventChannelId = guildConfig.getLogChannelForEventType(eventType);
            if (eventChannelId != 0) {
                channelId = eventChannelId;
            }
        }
        
        // If no channel configured, return null
        if (channelId == 0) {
            return null;
        }
        
        // Get the channel from JDA
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            logger.warn("Could not find text channel with ID {} for server {} and event type {}",
                    channelId, server.getName(), eventType);
        }
        
        return channel;
    }
}