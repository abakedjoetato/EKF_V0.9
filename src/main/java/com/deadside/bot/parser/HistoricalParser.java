package com.deadside.bot.parser;

import com.deadside.bot.data.MongoDBHandler;
import com.mongodb.client.MongoClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.logging.Logger;

/**
 * Parser for historical CSV log files from Deadside servers.
 * Processes all historical data without sending embed messages to Discord.
 */
public class HistoricalParser {
    private static final Logger LOGGER = Logger.getLogger(HistoricalParser.class.getName());
    private final MongoDBHandler mongoDBHandler;
    private final CSVLineProcessor lineProcessor;
    private final Map<String, String> serverLastLines = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public HistoricalParser(MongoDBHandler mongoDBHandler, CSVLineProcessor lineProcessor) {
        this.mongoDBHandler = mongoDBHandler;
        this.lineProcessor = lineProcessor;
    }

    /**
     * Process all historical log files for a given server.
     * @param host The server host (IP or name)
     * @param serverId The server ID
     * @param guildId The Discord guild ID
     * @return The number of processed entries
     */
    public int processHistoricalLogs(String host, String serverId, String guildId) {
        LOGGER.info("Starting historical log processing for server: " + host + "_" + serverId);
        
        // Clear previous statistical data for this server
        mongoDBHandler.clearServerStats(guildId, host + "_" + serverId);
        
        // Reset last parsed line memory
        serverLastLines.remove(host + "_" + serverId);
        
        String dirPath = "./" + host + "_" + serverId + "/actual1/deathlogs/";
        Path directory = Paths.get(dirPath);
        
        if (!Files.exists(directory)) {
            LOGGER.warning("Directory does not exist: " + dirPath);
            return 0;
        }
        
        try {
            // Get all CSV files in the directory, sorted by date (using filename)
            List<File> csvFiles = Files.list(directory)
                    .filter(path -> path.toString().endsWith(".csv"))
                    .map(Path::toFile)
                    .sorted(this::sortByFileName)
                    .collect(Collectors.toList());
            
            if (csvFiles.isEmpty()) {
                LOGGER.warning("No CSV files found in directory: " + dirPath);
                return 0;
            }
            
            LOGGER.info("Found " + csvFiles.size() + " CSV files to process");
            
            AtomicInteger totalProcessedLines = new AtomicInteger(0);
            
            // Process each file in chronological order
            for (File csvFile : csvFiles) {
                LOGGER.info("Processing file: " + csvFile.getName());
                int processedLines = processHistoricalFile(csvFile, host, serverId, guildId, false);
                totalProcessedLines.addAndGet(processedLines);
                LOGGER.info("Processed " + processedLines + " lines from " + csvFile.getName());
            }
            
            LOGGER.info("Completed historical log processing for server: " + host + "_" + serverId);
            LOGGER.info("Total processed lines: " + totalProcessedLines.get());
            
            return totalProcessedLines.get();
            
        } catch (IOException e) {
            LOGGER.severe("Error accessing directory: " + dirPath + " - " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Process a single historical CSV file
     * @param csvFile The CSV file to process
     * @param host The server host
     * @param serverId The server ID
     * @param guildId The Discord guild ID
     * @param sendEmbeds Whether to send Discord embeds (always false for historical parsing)
     * @return The number of processed lines
     */
    private int processHistoricalFile(File csvFile, String host, String serverId, String guildId, boolean sendEmbeds) {
        // Historical parsing should never send embeds
        sendEmbeds = false;
        
        int processedLines = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Process the CSV line without sending embeds
                boolean processed = lineProcessor.processLine(line, host, serverId, guildId, sendEmbeds);
                if (processed) {
                    processedLines++;
                }
            }
        } catch (IOException e) {
            LOGGER.severe("Error reading file: " + csvFile.getName() + " - " + e.getMessage());
            e.printStackTrace();
        }
        
        return processedLines;
    }
    
    /**
     * Sort CSV files by their filename (which should contain date information)
     */
    private int sortByFileName(File file1, File file2) {
        try {
            String name1 = file1.getName();
            String name2 = file2.getName();
            
            // Extract date from filename (assuming format like kill_log_2023-04-01_12-30-00.csv)
            int dateStart1 = name1.indexOf("_") + 1;
            int dateStart2 = name2.indexOf("_") + 1;
            
            if (dateStart1 > 0 && dateStart2 > 0) {
                int dateEnd1 = name1.lastIndexOf(".");
                int dateEnd2 = name2.lastIndexOf(".");
                
                if (dateEnd1 > dateStart1 && dateEnd2 > dateStart2) {
                    String dateStr1 = name1.substring(dateStart1, dateEnd1);
                    String dateStr2 = name2.substring(dateStart2, dateEnd2);
                    
                    try {
                        Date date1 = dateFormat.parse(dateStr1);
                        Date date2 = dateFormat.parse(dateStr2);
                        return date1.compareTo(date2);
                    } catch (ParseException e) {
                        // Fall back to string comparison if date parsing fails
                        return dateStr1.compareTo(dateStr2);
                    }
                }
            }
            
            // Fall back to simple name comparison if date extraction fails
            return name1.compareTo(name2);
            
        } catch (Exception e) {
            LOGGER.warning("Error sorting files: " + e.getMessage());
            return file1.getName().compareTo(file2.getName());
        }
    }
}
