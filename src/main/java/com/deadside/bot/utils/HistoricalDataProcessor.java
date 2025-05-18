package com.deadside.bot.utils;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.GuildConfig;
import com.deadside.bot.db.repositories.GuildConfigRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.KillfeedParser;
import com.deadside.bot.sftp.SftpConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to handle processing historical data from game servers
 * with a built-in delay to allow for database synchronization
 */
public class HistoricalDataProcessor {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalDataProcessor.class);
    
    /**
     * Schedule processing of historical data for a server with a delay
     * 
     * @param jda The JDA instance for Discord interaction
     * @param server The game server to process historical data for
     */
    public static void scheduleProcessing(JDA jda, GameServer server) {
        // Create a new thread to handle the delayed processing
        Thread processingThread = new Thread(() -> {
            try {
                // Wait 30 seconds to allow database to fully populate and SFTP connections to initialize
                logger.info("Scheduling historical data processing for server {} in 30 seconds", server.getName());
                Thread.sleep(30000);
                
                logger.info("Starting historical data processing for server {}", server.getName());
                
                // Find the admin channel to send status updates
                TextChannel adminChannel = findAdminChannel(jda, server);
                
                // Send initial processing message with modern embed
                if (adminChannel != null) {
                    String title = "Historical Data Processing Started";
                    String description = "Starting to process all historical data for **" + server.getName() + "**.\n\n" +
                                        "This will analyze all past server events and may take several minutes depending on the amount of data.";
                    
                    MessageEmbed embed = new EmbedBuilder()
                        .setTitle(title)
                        .setDescription(description)
                        .setColor(EmbedUtils.STEEL_BLUE)
                        .setFooter(EmbedUtils.STANDARD_FOOTER)
                        .setTimestamp(Instant.now())
                        .build();
                    
                    adminChannel.sendMessageEmbeds(embed).queue();
                }
                
                // Create the parsers
                KillfeedParser killfeedParser = new KillfeedParser(jda);
                DeadsideCsvParser csvParser = new DeadsideCsvParser(jda, new SftpConnector(), new PlayerRepository());
                
                // Process killfeed data
                int killfeedProcessed = killfeedParser.processServer(server, true);
                
                // Send progress update after killfeed processing with modern embed
                if (adminChannel != null) {
                    String title = "Killfeed Processing Complete";
                    String description = "Successfully processed killfeed data for **" + server.getName() + "**.\n" +
                                       "Now continuing with death logs processing...";
                    
                    MessageEmbed embed = new EmbedBuilder()
                        .setTitle(title)
                        .setDescription(description)
                        .setColor(EmbedUtils.STEEL_BLUE)
                        .addField("Killfeed Records", String.valueOf(killfeedProcessed), true)
                        .addField("Status", "Processing Death Logs...", true)
                        .setFooter(EmbedUtils.STANDARD_FOOTER)
                        .setTimestamp(Instant.now())
                        .build();
                    
                    adminChannel.sendMessageEmbeds(embed).queue();
                }
                
                // Process death logs
                int deathlogsProcessed = csvParser.processDeathLogs(server, true);
                
                // Send final completion message with modern embed styling
                if (adminChannel != null) {
                    String title = "Historical Data Import Complete";
                    String description = "Successfully processed historical data for **" + server.getName() + "**";
                    
                    // Create a modern styled embed with all the statistics
                    MessageEmbed embed = new EmbedBuilder()
                        .setTitle(title)
                        .setDescription(description)
                        .setColor(EmbedUtils.EMERALD_GREEN)
                        .addField("Killfeed Records", String.valueOf(killfeedProcessed), true)
                        .addField("Death Logs", String.valueOf(deathlogsProcessed), true)
                        .setFooter(EmbedUtils.STANDARD_FOOTER)
                        .setTimestamp(Instant.now())
                        .build();
                    
                    adminChannel.sendMessageEmbeds(embed).queue();
                }
                
                logger.info("Completed historical data processing for server {}: {} killfeed records, {} deathlogs", 
                        server.getName(), killfeedProcessed, deathlogsProcessed);
            } catch (Exception e) {
                logger.error("Error during historical data processing for server {}: {}", 
                        server.getName(), e.getMessage(), e);
            }
        });
        
        // Set as daemon thread so it doesn't block JVM shutdown
        processingThread.setDaemon(true);
        processingThread.setName("HistoricalProcessor-" + server.getName());
        processingThread.start();
    }
    
    /**
     * Find the admin channel for a server to send progress updates
     * 
     * @param jda The JDA instance
     * @param server The game server to find the admin channel for
     * @return The admin channel, or null if not found
     */
    private static TextChannel findAdminChannel(JDA jda, GameServer server) {
        try {
            // Get the guild from the server
            Guild guild = jda.getGuildById(server.getGuildId());
            if (guild == null) {
                logger.warn("Could not find guild for server {}", server.getName());
                return null;
            }
            
            // Try to find the admin channel from guild config
            GuildConfigRepository guildConfigRepository = new GuildConfigRepository();
            GuildConfig guildConfig = guildConfigRepository.findByGuildId(server.getGuildId());
            
            if (guildConfig != null && guildConfig.getPrimaryLogChannelId() != 0) {
                TextChannel adminChannel = guild.getTextChannelById(guildConfig.getPrimaryLogChannelId());
                if (adminChannel != null) {
                    return adminChannel;
                }
            }
            
            // If no admin channel is configured, try to use the killfeed channel
            if (server.getKillfeedChannelId() != 0) {
                TextChannel killfeedChannel = guild.getTextChannelById(server.getKillfeedChannelId());
                if (killfeedChannel != null) {
                    return killfeedChannel;
                }
            }
            
            // If no suitable channel is found, try to use the system channel
            return guild.getSystemChannel();
        } catch (Exception e) {
            logger.error("Error finding admin channel for server {}: {}", server.getName(), e.getMessage());
            return null;
        }
    }
}