package com.deadside.bot.utils;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.KillRecord;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.KillRecordRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.KillfeedParser;
import com.deadside.bot.sftp.SftpConnector;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for processing historical data with efficient batch processing
 * and progress reporting
 */
public class HistoricalDataProcessor {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalDataProcessor.class);
    private final GameServerRepository gameServerRepository;
    private final KillRecordRepository killRecordRepository;
    private final SftpConnector sftpConnector;
    private final DeadsideCsvParser csvParser;
    private final KillfeedParser killfeedParser;

    // Configuration for batch processing
    private static final int BATCH_SIZE = 50; // Process files in batches of 50
    private static final int PROGRESS_UPDATE_FREQUENCY = 5; // Update progress every 5% or after each batch
    
    /**
     * Create a new HistoricalDataProcessor
     * 
     * @param gameServerRepository Repository for game servers
     * @param killRecordRepository Repository for kill records
     * @param sftpConnector SFTP connector for accessing server files
     * @param csvParser CSV parser
     * @param killfeedParser Killfeed parser
     */
    public HistoricalDataProcessor(
            GameServerRepository gameServerRepository,
            KillRecordRepository killRecordRepository,
            SftpConnector sftpConnector,
            DeadsideCsvParser csvParser,
            KillfeedParser killfeedParser) {
        this.gameServerRepository = gameServerRepository;
        this.killRecordRepository = killRecordRepository;
        this.sftpConnector = sftpConnector;
        this.csvParser = csvParser;
        this.killfeedParser = killfeedParser;
    }
    
    /**
     * Process all historical data for a server with progress updates
     * 
     * @param server The server to process
     * @param event The event for sending progress updates (can be null)
     * @param clearExisting Whether to clear existing data
     * @return Total number of records processed
     */
    public ProcessingResult processHistoricalData(GameServer server, SlashCommandInteractionEvent event, boolean clearExisting) {
        Thread.currentThread().setName("HistoricalProcessor-" + server.getName());
        
        ProcessingResult result = new ProcessingResult();
        logger.info("Starting historical data processing for server: {}", server.getName());
        
        try {
            // Store current timestamp before processing
            long currentTimestamp = server.getLastProcessedTimestamp();
            
            // Temporarily clear the timestamp to process all historical data
            server.setLastProcessedTimestamp(0);
            gameServerRepository.update(server);
            
            // Send initial progress update
            if (event != null) {
                event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed(
                        "Historical Processing Started",
                        "Started processing historical data for server: **" + server.getName() + "**\n" +
                        "Processing death logs first, then killfeed data."
                )).queue();
            }
            
            // Clear existing kill records if requested
            if (clearExisting) {
                logger.info("Clearing existing kill records for server: {}", server.getName());
                killRecordRepository.deleteAllForServer(server.getId().toString());
                
                if (event != null) {
                    event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed(
                            "Existing Data Cleared",
                            "Cleared existing kill records for server: **" + server.getName() + "**"
                    )).queue();
                }
            }
            
            // Process CSV death logs
            result.deathLogs = processHistoricalDeathLogs(server, event);
            
            // Process killfeed data
            result.killfeed = killfeedParser.processServer(server, true);
            
            // Set the timestamp to current time
            server.setLastProcessedTimestamp(System.currentTimeMillis());
            gameServerRepository.update(server);
            
            // Send final progress update
            if (event != null) {
                String description = String.format(
                        "Processed **%d death logs** and **%d killfeed entries** for server: **%s**\n" +
                        "All player statistics have been updated.\n\n" +
                        "The server is now ready for regular processing.",
                        result.deathLogs, result.killfeed, server.getName());
                
                event.getHook().sendMessageEmbeds(EmbedUtils.successEmbed(
                        "Historical Processing Complete",
                        description
                )).queue();
            }
            
            logger.info("Completed historical data processing for server {}: {} killfeed records, {} deathlogs",
                    server.getName(), result.killfeed, result.deathLogs);
            
        } catch (Exception e) {
            logger.error("Error processing historical data for server: {}", server.getName(), e);
            
            if (event != null) {
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                        "Error Processing Historical Data",
                        "An error occurred while processing historical data for server: **" + 
                        server.getName() + "**\n\n" + e.getMessage()
                )).queue();
            }
        }
        
        return result;
    }
    
    /**
     * Process historical death logs with batch processing and progress updates
     * 
     * @param server The server to process
     * @param event The event for sending progress updates (can be null)
     * @return Total number of death logs processed
     */
    private int processHistoricalDeathLogs(GameServer server, SlashCommandInteractionEvent event) {
        try {
            // Find all death log files
            List<String> allFiles = sftpConnector.findDeathlogFiles(server);
            
            if (allFiles.isEmpty()) {
                logger.warn("No death log files found for server: {}", server.getName());
                return 0;
            }
            
            // Sort files chronologically
            Collections.sort(allFiles);
            
            // Send initial update
            if (event != null) {
                event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed(
                        "Processing Death Logs",
                        String.format("Found **%d** CSV files for server: **%s**\n" +
                        "Processing in batches of %d files for efficient resource management.",
                        allFiles.size(), server.getName(), BATCH_SIZE)
                )).queue();
            }
            
            // Process files in batches
            int totalFiles = allFiles.size();
            int totalProcessed = 0;
            AtomicInteger batchCount = new AtomicInteger(0);
            
            // Track statistics for detailed reporting
            AtomicInteger killCount = new AtomicInteger(0);
            AtomicInteger deathCount = new AtomicInteger(0);
            AtomicInteger suicideCount = new AtomicInteger(0);
            
            // Calculate update frequency
            int updateEvery = Math.max(1, totalFiles / 20); // Update about 20 times during processing
            
            for (int i = 0; i < totalFiles; i += BATCH_SIZE) {
                // Get current batch
                int endIndex = Math.min(i + BATCH_SIZE, totalFiles);
                List<String> batch = allFiles.subList(i, endIndex);
                
                // Process this batch
                int processedInBatch = processBatch(server, batch);
                totalProcessed += processedInBatch;
                
                // Update progress periodically
                if (event != null && (i % updateEvery == 0 || i + BATCH_SIZE >= totalFiles)) {
                    int batchNum = batchCount.incrementAndGet();
                    int totalBatches = (int) Math.ceil((double) totalFiles / BATCH_SIZE);
                    int progressPercent = (int) (((double) i / totalFiles) * 100);
                    
                    // Create a more informative progress report
                    String progressDesc = String.format("**Server**: %s\n" +
                            "**Progress**: %d/%d files (%d%%)\n" +
                            "**Batch**: %d/%d\n" +
                            "**Deaths processed**: %d\n\n" +
                            "Player statistics are being updated as data is processed.\n" +
                            "This ensures accurate tracking without sending Discord notifications for historical events.",
                            server.getName(), i + batch.size(), totalFiles, progressPercent,
                            batchNum, totalBatches, totalProcessed);
                    
                    event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed(
                            "Historical Data Processing Progress", 
                            progressDesc
                    )).queue();
                }
            }
            
            return totalProcessed;
            
        } catch (Exception e) {
            logger.error("Error processing historical death logs for server: {}", server.getName(), e);
            throw e;
        }
    }
    
    /**
     * Process a batch of death log files
     * 
     * @param server The server to process
     * @param batch The batch of files to process
     * @return Total number of deaths processed in this batch
     */
    private int processBatch(GameServer server, List<String> batch) {
        int batchTotal = 0;
        
        for (String csvFile : batch) {
            try {
                // Read file content
                String content = sftpConnector.readDeathlogFile(server, csvFile);
                if (content == null || content.isEmpty()) {
                    logger.warn("Empty or unreadable file: {} for server: {}", csvFile, server.getName());
                    continue;
                }
                
                // Force historical mode to ensure player stats are updated but embeds aren't sent
                csvParser.setProcessingHistoricalData(true);
                
                // Process file with player stat updates
                int fileProcessed = csvParser.processDeathLogContent(server, content);
                batchTotal += fileProcessed;
                
                // Only log meaningful information to reduce console noise
                if (fileProcessed > 0) {
                    logger.info("Historical batch - processed {} deaths from file for server {}", 
                            fileProcessed, server.getName());
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Historical batch - processed file {} for server {}: no new deaths", 
                            csvFile, server.getName());
                }
                
            } catch (Exception e) {
                // Log error but continue with next file
                logger.error("Error processing file {} for server {}: {}", 
                        csvFile, server.getName(), e.getMessage(), e);
            }
        }
        
        // Reset historical mode after batch is done
        csvParser.setProcessingHistoricalData(false);
        
        return batchTotal;
    }
    
    /**
     * Class to hold processing results
     */
    public static class ProcessingResult {
        private int killfeed;
        private int deathLogs;
        
        public int getKillfeed() {
            return killfeed;
        }
        
        public int getDeathLogs() {
            return deathLogs;
        }
        
        public int getTotal() {
            return killfeed + deathLogs;
        }
    }
}