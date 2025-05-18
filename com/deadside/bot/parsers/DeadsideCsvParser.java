package com.deadside.bot.parsers;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parser for Deadside CSV death log files
 * Format: timestamp;killer;killerId;victim;victimId;weapon;distance;platform1;platform2
 * Example: 2025.05.15-00.11.07;Fatalben0;0002548521ba4271a497e39d5bfe5611;Rogue731;00022ac42542497589f654e6ac2c0a6f;MR5;20;XSX;XSX;
 */
public class DeadsideCsvParser {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideCsvParser.class);
    private final JDA jda;
    private final SftpConnector sftpConnector;
    private final PlayerRepository playerRepository;
    
    // Map to keep track of processed files for each server
    private final Map<String, Set<String>> processedFiles = new HashMap<>();
    
    // Format of the CSV death log: timestamp;killer;killerId;victim;victimId;weapon;distance;platform1;platform2;
    // Format example: 2025.05.15-00.11.07;Fatalben0;0002548521ba4271a497e39d5bfe5611;Rogue731;00022ac42542497589f654e6ac2c0a6f;MR5;20;XSX;XSX;
    // Updated pattern to be more flexible with various CSV formats and handle empty fields
    private static final Pattern CSV_LINE_PATTERN = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2});([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);?");
    private static final SimpleDateFormat CSV_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
    // Alternative format seen in the CSV files (2025.05.15-00.11.07)
    private static final SimpleDateFormat ALT_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
    
    // Death causes
    private static final Set<String> SUICIDE_CAUSES = new HashSet<>(Arrays.asList(
            "suicide_by_relocation", "suicide", "falling", "bleeding", "drowning", "starvation"
    ));
    
    // Additional categorization for specific suicide types
    private static final Set<String> RELOCATION_CAUSES = new HashSet<>(Arrays.asList(
            "suicide_by_relocation"
    ));
    
    private static final Set<String> FALLING_CAUSES = new HashSet<>(Arrays.asList(
            "falling"
    ));
    
    public DeadsideCsvParser(JDA jda, SftpConnector sftpConnector, PlayerRepository playerRepository) {
        this.jda = jda;
        this.sftpConnector = sftpConnector;
        this.playerRepository = playerRepository;
        
        // Set timezone for date parsing
        CSV_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    /**
     * Process death logs for a server
     * @param server The game server to process
     * @return Number of deaths processed
     */
    public int processDeathLogs(GameServer server) {
        // Check if parser is paused for this server
        if (com.deadside.bot.utils.ParserStateManager.isCSVParserPaused(server.getName(), server.getGuildId())) {
            String reason = com.deadside.bot.utils.ParserStateManager.getParserState(
                    server.getName(), server.getGuildId()).getCSVPauseReason();
            logger.info("Skipping CSV death log processing for server '{}' - Parser paused: {}", 
                    server.getName(), reason);
            return 0;
        }
        
        int totalProcessed = 0;
        
        try {
            // Skip if killfeed channel not set
            if (server.getKillfeedChannelId() == 0) {
                // Check if a suitable channel exists
                TextChannel channel = findSuitableChannelForDeathEvents(server);
                if (channel == null) {
                    logger.warn("No suitable channel found for death events for server {}", server.getName());
                    return 0;
                }
            }
            
            // Get all CSV files from the server
            List<String> csvFiles = sftpConnector.findDeathlogFiles(server);
            if (csvFiles.isEmpty()) {
                return 0;
            }
            
            // Sort files by name (which includes date)
            Collections.sort(csvFiles);
            
            // Get the last processed information using ParserStateManager to ensure server isolation
            com.deadside.bot.utils.ParserStateManager.ParserState state = 
                com.deadside.bot.utils.ParserStateManager.getParserState(server.getName(), server.getGuildId());
                
            // Try to get state from memory first, fall back to server object if not available
            String lastProcessedFile = (String) state.retrieveFromMemory("lastProcessedFile");
            if (lastProcessedFile == null) {
                lastProcessedFile = server.getLastProcessedKillfeedFile();
                // Store for future use
                state.storeInMemory("lastProcessedFile", lastProcessedFile);
            }
            
            Long lastLineObj = (Long) state.retrieveFromMemory("lastProcessedLine");
            long lastProcessedLine = lastLineObj != null ? lastLineObj : server.getLastProcessedKillfeedLine();
            
            // Improved logic for historical vs. regular processing
            List<String> filesToProcess = new ArrayList<>();
            
            // Determine if we're doing historical processing or regular processing
            boolean isHistoricalProcessing = false;
            
            // Check if this is a special historical processing run
            // This would be triggered by an external command or system flag
            if (com.deadside.bot.utils.ParserStateManager.isHistoricalParsingEnabled(server.getName(), server.getGuildId())) {
                isHistoricalProcessing = true;
                logger.info("Historical parsing mode enabled for server: {}", server.getName());
            }
            
            if (isHistoricalProcessing) {
                // For historical parsing, we process multiple files in sequence - FULL RESET of tracking state
                // Reset all parser memory when starting historical processing to ensure clean start
                state.clearParserMemory();
                
                if (!csvFiles.isEmpty()) {
                    // Process ALL files in the server's folder, starting from the oldest
                    // This ensures we don't miss any historical data
                    for (int i = 0; i < csvFiles.size(); i++) {
                        filesToProcess.add(csvFiles.get(i));
                    }
                    
                    // Update tracking to start with the first file we'll process
                    lastProcessedFile = filesToProcess.get(0);
                    lastProcessedLine = -1; // Process the entire file
                    
                    // Store this fresh state in parser memory
                    state.storeInMemory("lastProcessedFile", lastProcessedFile);
                    state.storeInMemory("lastProcessedLine", lastProcessedLine);
                    
                    logger.info("Starting historical processing with {} files, beginning with: {}", 
                            filesToProcess.size(), lastProcessedFile);
                } else {
                    return 0;
                }
            } else {
                // For regular operation, just focus on the newest file
                if (!csvFiles.isEmpty()) {
                    String newestFile = csvFiles.get(csvFiles.size() - 1);
                    
                    if (lastProcessedFile.isEmpty() || !csvFiles.contains(lastProcessedFile) ||
                            !lastProcessedFile.equals(newestFile)) {
                        // First run or newer file available - switch to newest file
                        filesToProcess.add(newestFile);
                        lastProcessedFile = newestFile;
                        lastProcessedLine = -1; // Start from the beginning of new file
                        logger.info("Processing newest CSV file: {}", newestFile);
                    } else {
                        // Continue with current file
                        filesToProcess.add(lastProcessedFile);
                        logger.debug("Continuing with current CSV file: {}", lastProcessedFile);
                    }
                } else {
                    return 0;
                }
            }
            
            // Process files in chronological order
            for (int fileIndex = 0; fileIndex < filesToProcess.size(); fileIndex++) {
                String csvFile = filesToProcess.get(fileIndex);
                try {
                    String content = sftpConnector.readDeathlogFile(server, csvFile);
                    if (content == null || content.isEmpty()) {
                        logger.warn("Empty or unreadable death log file: {}", csvFile);
                        continue;
                    }
                    
                    // Split into lines
                    String[] lines = content.split("\\n");
                    
                    // Determine start line - only apply lastProcessedLine to the first file we're processing
                    long startLine = 0;
                    if (fileIndex == 0 && csvFile.equals(lastProcessedFile)) {
                        startLine = lastProcessedLine + 1; // Start after the last processed line
                    }
                    
                    // Process only the relevant lines
                    int deathsProcessed = 0;
                    for (long i = startLine; i < lines.length; i++) {
                        String line = lines[(int)i].trim();
                        if (line.isEmpty()) continue;
                        
                        try {
                            if (processDeathLine(server, line)) {
                                deathsProcessed++;
                            }
                            
                            // Update last processed line for tracking using ParserStateManager
                            lastProcessedLine = i;
                            state.storeInMemory("lastProcessedLine", lastProcessedLine);
                        } catch (Exception e) {
                            logger.warn("Error processing line {} in file {}: {}", 
                                    i, csvFile, e.getMessage());
                        }
                    }
                    
                    // Update tracking for file transition
                    if (fileIndex < filesToProcess.size() - 1) {
                        // Moving to next file - reset line counter
                        lastProcessedFile = filesToProcess.get(fileIndex + 1);
                        lastProcessedLine = -1;
                        
                        // Store updated tracking in server-isolated state
                        state.storeInMemory("lastProcessedFile", lastProcessedFile);
                        state.storeInMemory("lastProcessedLine", lastProcessedLine);
                    }
                    
                    totalProcessed += deathsProcessed;
                    logger.info("Processed death log file {} for server {}, {} deaths", 
                            csvFile, server.getName(), deathsProcessed);
                } catch (Exception e) {
                    logger.error("Error processing death log file {} for server {}: {}", 
                            csvFile, server.getName(), e.getMessage(), e);
                }
            }
            
            // Update server tracking with final values
            if (totalProcessed > 0 || !lastProcessedFile.isEmpty()) {
                // Only update database persistence for regular parsing, not during historical
                if (!isHistoricalProcessing) {
                    server.updateKillfeedProgress(lastProcessedFile, lastProcessedLine);
                    logger.debug("Updated server database tracking to file: {}, line: {}", 
                            lastProcessedFile, lastProcessedLine);
                }
                
                // Always update memory state for proper server isolation
                state.storeInMemory("lastProcessedFile", lastProcessedFile);
                state.storeInMemory("lastProcessedLine", lastProcessedLine);
                logger.debug("Updated state memory tracking for server {} to file: {}, line: {}", 
                        server.getName(), lastProcessedFile, lastProcessedLine);
            }
            
            return totalProcessed;
        } catch (Exception e) {
            logger.error("Error processing death logs for server {}: {}", 
                    server.getName(), e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Find a suitable channel for death events if killfeed channel is not set
     */
    private TextChannel findSuitableChannelForDeathEvents(GameServer server) {
        try {
            Guild guild = jda.getGuildById(server.getGuildId());
            if (guild == null) {
                return null;
            }
            
            // Try to find a channel with a relevant name
            List<TextChannel> channels = guild.getTextChannels();
            for (TextChannel channel : channels) {
                String name = channel.getName().toLowerCase();
                if (name.contains("kill") || name.contains("death") || 
                    name.contains("log") || name.contains("feed")) {
                    return channel;
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error finding suitable channel for death events: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Process a single death log line
     * @param server The game server
     * @param line The CSV line to process
     * @return true if a valid death event was processed, false otherwise
     */
    private boolean processDeathLine(GameServer server, String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        
        // Use the pattern matcher to extract data directly
        java.util.regex.Matcher matcher = CSV_LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            logger.debug("Line doesn't match expected CSV pattern: {}", line);
            return false;
        }
        
        try {
            // Extract values from matcher groups
            String timestamp = matcher.group(1);
            String killer = matcher.group(2);
            String killerId = matcher.group(3);
            String victim = matcher.group(4);
            String victimId = matcher.group(5);
            String weapon = matcher.group(6);
            
            // Handle distance field safely - may not be an integer in all cases
            int distance = 0;
            try {
                distance = Integer.parseInt(matcher.group(7));
            } catch (NumberFormatException e) {
                logger.debug("Non-integer distance value: {}, defaulting to 0", matcher.group(7));
            }
            
            // Handle additional platform information if available
            String killerPlatform = matcher.group(8);
            String victimPlatform = matcher.group(9);
            
            // Log the parsed data for debugging
            logger.debug("Parsed death entry: {} killed {} with {} from {}m", 
                    killer, victim, weapon, distance);
            
            // Check for special cases like suicide or falling deaths
            boolean isSuicide = killer.equals(victim);
            boolean isFalling = "falling".equalsIgnoreCase(weapon);
            boolean isRelocationSuicide = "suicide_by_relocation".equalsIgnoreCase(weapon);
            
            if (isSuicide) {
                logger.debug("Detected suicide: {} killed themselves with {}", killer, weapon);
            }
            
            if (isFalling) {
                logger.debug("Detected falling death: {} died from falling", victim);
            }
            
            if (isRelocationSuicide) {
                logger.debug("Detected relocation suicide: {} relocated", victim);
            }
            
            // Process death regardless of timestamp - we're using line-based tracking now
            // so we won't double-process entries
            processDeath(server, timestamp, victim, victimId, killer, killerId, weapon, distance);
            return true;
        } catch (Exception e) {
            logger.warn("Error processing death log line: {}", line, e);
            return false;
        }
    }
    
    /**
     * Process a death log file content (legacy method, retained for compatibility)
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
            
            if (processDeathLine(server, line)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Process a death event
     */
    private void processDeath(GameServer server, String timestamp, String victim, String victimId, 
                             String killer, String killerId, String weapon, int distance) {
        // Normalize weapon name
        String normalizedWeapon = weapon.toLowerCase();
        
        // Handle different death types
        boolean isSuicide = victim.equals(killer);  // Killer is the same as victim
        boolean isSystemSuicide = SUICIDE_CAUSES.contains(normalizedWeapon);
        boolean isRelocation = RELOCATION_CAUSES.contains(normalizedWeapon);
        boolean isFalling = FALLING_CAUSES.contains(normalizedWeapon);
        
        // Add detailed processing for better logs
        if (isSuicide) {
            if (isRelocation) {
                // Handle relocation suicides - don't count these in stats
                sendRelocationKillfeed(server, timestamp, victim, victimId);
                logger.debug("Relocation recorded for player: {}", victim);
            } else if (isFalling) {
                // Handle falling deaths
                sendSuicideKillfeed(server, timestamp, victim, victimId, "falling");
                updateSuicideStats(victim, victimId, "falling");
                logger.debug("Falling death recorded for player: {}", victim);
            } else if (isSystemSuicide) {
                // Handle other game-mechanic suicides (bleeding, drowning, etc)
                sendSuicideKillfeed(server, timestamp, victim, victimId, normalizedWeapon);
                updateSuicideStats(victim, victimId, normalizedWeapon);
                logger.debug("Suicide recorded for {}: {}", victim, normalizedWeapon);
            } else {
                // Handle self-kill (player killed themselves with a weapon)
                sendSuicideKillfeed(server, timestamp, victim, victimId, "self");
                updateSuicideStats(victim, victimId, weapon);
                logger.debug("Self-kill recorded for {}: {}", victim, weapon);
            }
        } else {
            // Normal PvP kill
            sendPlayerKillKillfeed(server, timestamp, victim, victimId, killer, killerId, weapon, distance);
            
            // Only update kill/death stats for legitimate PvP kills
            updateKillerStats(killer, killerId);
            updateVictimStats(victim, victimId);
            logger.debug("Kill recorded: {} killed {} with {} from {}m", killer, victim, weapon, distance);
        }
    }
    
    /**
     * Update the killer's stats
     */
    private void updateKillerStats(String killer, String killerId) {
        try {
            Player player = playerRepository.findByDeadsideId(killerId);
            if (player == null) {
                // If player doesn't exist in database, don't create yet
                // They can be created when they link their account
                return;
            }
            
            // Update kills and score
            player.setKills(player.getKills() + 1);
            player.setScore(player.getScore() + 10); // +10 points per kill
            
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
     * Update suicide stats for a player
     */
    private void updateSuicideStats(String victim, String victimId, String cause) {
        try {
            Player player = playerRepository.findByDeadsideId(victimId);
            if (player == null) {
                // If player doesn't exist in database, don't create yet
                return;
            }
            
            // Increment suicide count instead of kills/deaths
            player.setSuicides(player.getSuicides() + 1);
            
            playerRepository.save(player);
            logger.debug("Updated suicide count for {}: cause={}", victim, cause);
        } catch (Exception e) {
            logger.error("Error updating suicide stats for {}: {}", victim, e.getMessage(), e);
        }
    }
    
    /**
     * Send killfeed message for player kill
     */
    private void sendPlayerKillKillfeed(GameServer server, String timestamp, String victim, String victimId,
                                       String killer, String killerId, String weapon, int distance) {
        try {
            // Use our advanced killfeed embed with proper styling
            MessageEmbed embed = EmbedUtils.pvpKillfeedEmbed(killer, victim, weapon, distance);
            
            // Send to death channel
            sendToKillfeedChannel(server, embed);
        } catch (Exception e) {
            logger.error("Error sending kill feed for {}: {}", victim, e.getMessage(), e);
        }
    }
    
    /**
     * Send a relocation notification
     * These are special events when players teleport/relocate in the game
     * Usually we don't want to show these in the killfeed
     */
    private void sendRelocationKillfeed(GameServer server, String timestamp, String victim, String victimId) {
        // For relocations, we don't usually send messages to the killfeed
        // They're very common and not interesting game events
        
        // Just log for debugging purposes
        logger.debug("Player {} relocated at {}", victim, timestamp);
        
        // Optional: If you want these in a separate admin channel, uncomment and implement
        // sendToAdminChannel(server, "Player " + victim + " relocated");
    }
    
    /**
     * Send killfeed message for suicide with modern styling
     */
    private void sendSuicideKillfeed(GameServer server, String timestamp, String victim, String victimId, String cause) {
        try {
            MessageEmbed embed;
            
            // Check if it's falling damage or another type of suicide
            if (cause.equalsIgnoreCase("falling")) {
                // For falling deaths, use the specialized embed
                int height = 0; // Default height (not available in log)
                embed = EmbedUtils.fallingDeathEmbed(victim, height);
            } else {
                // For regular suicides, use suicide embed with normalized cause
                embed = EmbedUtils.suicideEmbed(victim, cause);
            }
            
            // Send to death channel
            sendToKillfeedChannel(server, embed);
        } catch (Exception e) {
            logger.error("Error sending suicide feed for {}: {}", victim, e.getMessage(), e);
        }
    }
    
    /**
     * Send embed message to the server's killfeed channel
     */
    private void sendToKillfeedChannel(GameServer server, net.dv8tion.jda.api.entities.MessageEmbed embed) {
        Guild guild = jda.getGuildById(server.getGuildId());
        if (guild == null) {
            logger.warn("Guild not found for server {}: {}", server.getName(), server.getGuildId());
            return;
        }
        
        TextChannel killfeedChannel = guild.getTextChannelById(server.getKillfeedChannelId());
        if (killfeedChannel == null) {
            logger.warn("Killfeed channel not found for server {}: {}", 
                    server.getName(), server.getKillfeedChannelId());
            return;
        }
        
        killfeedChannel.sendMessageEmbeds(embed).queue(
                success -> logger.debug("Sent killfeed to channel {}", killfeedChannel.getId()),
                error -> logger.error("Failed to send killfeed: {}", error.getMessage())
        );
    }
}