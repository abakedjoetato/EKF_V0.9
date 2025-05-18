package com.deadside.bot.commands.admin;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.KillRecord;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.KillRecordRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.utils.EmbedUtils;
import com.deadside.bot.utils.ParserStateManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.deadside.bot.db.models.KillRecord;
import com.deadside.bot.sftp.SftpConnector;

/**
 * Command for processing historical data for a server
 */
public class ProcessHistoricalDataCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(ProcessHistoricalDataCommand.class);
    private final GameServerRepository serverRepository = new GameServerRepository();
    private final KillRecordRepository killRecordRepository = new KillRecordRepository();
    
    @Override
    public String getName() {
        return "processhistorical";
    }
    
    @Override
    public CommandData getCommandData() {
        // Create option data for server name with autocomplete
        OptionData serverNameOption = new OptionData(OptionType.STRING, "name", "The name of the server", true)
                .setAutoComplete(true);
        
        OptionData clearExistingData = new OptionData(OptionType.BOOLEAN, "clearexisting", 
                "Whether to clear existing data before processing", true);
        
        return Commands.slash(getName(), "Process historical data for a game server")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOptions(serverNameOption, clearExistingData);
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Defer reply immediately as this will be a long operation
        event.deferReply().queue();
        
        if (event.getGuild() == null) {
            event.getHook().sendMessage("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }
        
        // Check permissions
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().sendMessage("You need Administrator permission to use this command.").setEphemeral(true).queue();
            return;
        }
        
        String serverName = event.getOption("name", OptionMapping::getAsString);
        boolean clearExisting = event.getOption("clearexisting", false, OptionMapping::getAsBoolean);
        
        // Get the server
        GameServer server = serverRepository.findByGuildIdAndName(event.getGuild().getIdLong(), serverName);
        if (server == null) {
            event.getHook().sendMessage("No server found with name: " + serverName).setEphemeral(true).queue();
            return;
        }
        
        // Send initial status message
        event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Processing Started",
                "Starting historical data processing for server **" + serverName + "**...\n" +
                (clearExisting ? "Existing data will be cleared first." : "Existing data will be preserved.")
        )).queue();
        
        // Start historical processing in a separate thread
        CompletableFuture.runAsync(() -> {
            try {
                // Create required components for historical processing
                SftpConnector sftpConnector = new SftpConnector();
                com.deadside.bot.db.repositories.PlayerRepository playerRepository = 
                    new com.deadside.bot.db.repositories.PlayerRepository();
                com.deadside.bot.parsers.KillfeedParser killfeedParser = 
                    new com.deadside.bot.parsers.KillfeedParser(event.getJDA());
                DeadsideCsvParser csvParser = new DeadsideCsvParser(
                    event.getJDA(), sftpConnector, playerRepository);
                
                // Create the historical data processor with all necessary components
                com.deadside.bot.utils.HistoricalDataProcessor historicalProcessor = 
                    new com.deadside.bot.utils.HistoricalDataProcessor(
                        serverRepository, killRecordRepository, sftpConnector, csvParser, killfeedParser);
                
                // Pause regular parsers for this server
                ParserStateManager.pauseKillfeedParser(server.getName(), server.getGuildId(), 
                    "Historical data processing");
                ParserStateManager.pauseCSVParser(server.getName(), server.getGuildId(), 
                    "Historical data processing");
                
                try {
                    // Process all historical data with the improved batch processor
                    com.deadside.bot.utils.HistoricalDataProcessor.ProcessingResult result = 
                        historicalProcessor.processHistoricalData(server, event, clearExisting);
                    
                    logger.info("Historical data processing complete for server '{}': {} death logs, {} killfeed entries", 
                        server.getName(), result.getDeathLogs(), result.getKillfeed());
                } finally {
                    // Always resume the parsers
                    ParserStateManager.resumeKillfeedParser(server.getName(), server.getGuildId());
                    ParserStateManager.resumeCSVParser(server.getName(), server.getGuildId());
                }
            } catch (Exception e) {
                logger.error("Error processing historical data for server: {}", serverName, e);
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Error",
                        "An error occurred while processing historical data: " + e.getMessage()
                )).queue();
            }
        });
    }
    
    /**
     * Process historical data for a server
     */
    private void processHistoricalData(Guild guild, GameServer server, boolean clearExisting, 
                                     SlashCommandInteractionEvent event) {
        String serverName = server.getName();
        long guildId = guild.getIdLong();
        
        try {
            // Pause regular parsers for this server
            ParserStateManager.pauseKillfeedParser(serverName, guildId, "Historical data processing");
            ParserStateManager.pauseCSVParser(serverName, guildId, "Historical data processing");
            
            // Send status update
            event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Processing Status",
                    "Paused regular parsers for server **" + serverName + "**. Beginning historical data processing..."
            )).queue();
            
            // Clear existing data if requested
            long deletedRecords = 0;
            if (clearExisting) {
                deletedRecords = killRecordRepository.deleteByServerIdAndGuildId(serverName, guildId);
                
                // Reset the last processed file and line in the server
                server.setLastProcessedKillfeedFile("");
                server.setLastProcessedKillfeedLine(0);
                server.setLastProcessedLogFile("");
                server.setLastProcessedLogLine(0);
                serverRepository.save(server);
                
                event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Processing Status",
                        "Cleared " + deletedRecords + " existing kill records for server **" + serverName + "**."
                )).queue();
            }
            
            // Create parser
            SftpConnector sftpConnector = new SftpConnector();
            DeadsideCsvParser csvParser = new DeadsideCsvParser(event.getJDA(), sftpConnector, 
                    new com.deadside.bot.db.repositories.PlayerRepository());
            
            // Send status update
            event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Processing Status",
                    "Beginning to process CSV death logs for server **" + serverName + "**..."
            )).queue();
            
            // Get the killfeed parser to ensure consistent handling of suicides and falling deaths
            com.deadside.bot.parsers.KillfeedParser killfeedParser = new com.deadside.bot.parsers.KillfeedParser(event.getJDA());
            
            // Process all historical data in chronological order
            int processed = processAllHistoricalData(server, csvParser, killfeedParser, event);
            
            // Send final status message
            if (processed > 0) {
                event.getHook().sendMessageEmbeds(EmbedUtils.successEmbed("Processing Complete",
                        "Successfully processed historical data for server **" + serverName + "**.\n" +
                        "• " + (clearExisting ? deletedRecords + " existing records cleared\n" : "") +
                        "• " + processed + " new deaths processed"
                )).queue();
            } else {
                event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Processing Complete",
                        "Historical data processing completed for server **" + serverName + "** but no new data was found.\n" +
                        (clearExisting ? "• " + deletedRecords + " existing records were cleared.\n" : "") +
                        "Check your server configuration and CSV log files."
                )).queue();
            }
            
            // Resume parsers
            ParserStateManager.resumeKillfeedParser(serverName, guildId);
            ParserStateManager.resumeCSVParser(serverName, guildId);
            
            logger.info("Completed historical data processing for server '{}', processed {} deaths", 
                    serverName, processed);
        } catch (Exception e) {
            // Ensure parsers are resumed even if there's an error
            ParserStateManager.resumeKillfeedParser(serverName, guildId);
            ParserStateManager.resumeCSVParser(serverName, guildId);
            
            logger.error("Error processing historical data for server: {}", serverName, e);
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Error",
                    "An error occurred while processing historical data: " + e.getMessage() + "\n" +
                    "Regular parsers have been resumed."
            )).queue();
        }
    }
    
    /**
     * Process all historical data in chronological order (oldest to newest)
     * This is important to establish the correct "last line" position for the regular parser to use
     * @param server The server to process
     * @param csvParser The CSV parser to use
     * @param event The command event for progress updates
     * @return Total number of deaths processed
     */
    private int processAllHistoricalData(GameServer server, DeadsideCsvParser csvParser,
                                        com.deadside.bot.parsers.KillfeedParser killfeedParser,
                                        SlashCommandInteractionEvent event) {
        try {
            // Store current timestamp before processing historical data
            long currentTimestamp = server.getLastProcessedTimestamp();
            logger.info("Starting historical processing for server: {}. Current timestamp: {}", 
                      server.getName(), currentTimestamp);
            
            // Clear the timestamp temporarily to process ALL historical data without filtering
            server.setLastProcessedTimestamp(0);
            gameServerRepository.update(server);
            logger.info("Cleared timestamp for historical processing for server: {}", server.getName());
            
            // Get SFTP connector to find all CSV files
            SftpConnector sftpConnector = new SftpConnector();
            List<String> allFiles = sftpConnector.findDeathlogFiles(server);
            
            if (allFiles.isEmpty()) {
                logger.warn("No deathlog files found for server: {}", server.getName());
                return 0;
            }
            
            // Sort files chronologically (oldest first)
            Collections.sort(allFiles);
            
            int totalProcessed = 0;
            int fileCounter = 0;
            int totalFiles = allFiles.size();
            
            // Process each file in order from oldest to newest
            for (String csvFile : allFiles) {
                fileCounter++;
                
                // Send progress update every 5 files or for the last file
                if (fileCounter % 5 == 0 || fileCounter == totalFiles) {
                    event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Processing Progress",
                            "Processing historical data for server **" + server.getName() + "**\n" +
                            "Progress: " + fileCounter + "/" + totalFiles + " files (" + 
                            Math.round((fileCounter * 100.0) / totalFiles) + "%)\n" +
                            "Deaths processed so far: " + totalProcessed
                    )).queue();
                }
                
                // Read and process this file
                String content = sftpConnector.readDeathlogFile(server, csvFile);
                if (content == null || content.isEmpty()) {
                    logger.warn("Empty or unreadable file: {} for server: {}", 
                            csvFile, server.getName());
                    continue;
                }
                
                // Use the KillfeedParser directly to ensure consistent handling of death types
                List<KillRecord> records = parseDeathlogContentWithKillfeedParser(server, content, csvFile, killfeedParser);
                int thisFileProcessed = records.size();
                
                // Save the records to the database
                if (!records.isEmpty()) {
                    killRecordRepository.saveAll(records);
                    totalProcessed += thisFileProcessed;
                    
                    // Update the server's last processed file and line
                    server.setLastProcessedKillfeedFile(csvFile);
                    // Line count is the number of records we processed
                    server.setLastProcessedKillfeedLine(records.size() - 1);
                    // Save updates
                    serverRepository.save(server);
                }
                
                logger.info("Processed historical file {} for server {}: {} deaths", 
                        csvFile, server.getName(), thisFileProcessed);
            }
            
            // Update the server's timestamp to the current time to mark where live processing should resume
            long newTimestamp = System.currentTimeMillis();
            server.setLastProcessedTimestamp(newTimestamp);
            serverRepository.update(server);
            logger.info("Historical processing complete for server: {}. Updated timestamp to: {}", 
                    server.getName(), newTimestamp);
            
            return totalProcessed;
        } catch (Exception e) {
            logger.error("Error processing historical data for server: {}", server.getName(), e);
            throw e; // Let the caller handle the exception
        }
    }
    
    /**
     * Parse death log content into kill records using the KillfeedParser for consistency
     * This ensures we handle suicides and falling deaths exactly the same way
     */
    private List<KillRecord> parseDeathlogContentWithKillfeedParser(GameServer server, String content, 
                                                                 String filename, 
                                                                 com.deadside.bot.parsers.KillfeedParser killfeedParser) {
        List<KillRecord> records = new ArrayList<>();
        
        try {
            // Split into lines
            String[] lines = content.split("\n");
            
            // Track statistics for logging
            int totalLines = 0;
            int successfullyParsed = 0;
            int suicides = 0;
            int fallingDeaths = 0;
            
            // Process each line using the same parser as the regular killfeed
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                totalLines++;
                
                try {
                    // Process the line directly for consistent parsing
                    // Parse the line manually to match the CSV format:
                    // timestamp;killer;killerId;victim;victimId;weapon;distance;killerPlatform;victimPlatform
                    String[] parts = line.split(";");
                    if (parts.length >= 9) {
                        String timestamp = parts[0];
                        String killer = parts[1];
                        String killerId = parts[2];
                        String victim = parts[3];
                        String victimId = parts[4];
                        String weapon = parts[5];
                        int distance = 0;
                        try {
                            distance = Integer.parseInt(parts[6]);
                        } catch (NumberFormatException e) {
                            // Ignore non-numeric distances
                        }
                        
                        // Create a KillRecord directly
                        KillRecord record = new KillRecord(
                            server.getName(),
                            server.getGuildId(),
                            killer,
                            killerId,
                            victim,
                            victimId,
                            weapon,
                            distance,
                            timestamp
                        );
                    
                        // Successfully parsed a kill record
                        successfullyParsed++;
                        
                        // Check for suicides (killer == victim)
                        if (record.getKiller().equals(record.getVictim())) {
                            suicides++;
                        }
                        
                        // Check for falling deaths (weapon contains "fall" or similar)
                        if (record.getWeapon().toLowerCase().contains("fall")) {
                            fallingDeaths++;
                        }
                        
                        records.add(record);
                    }
                } catch (Exception e) {
                    // Log only at debug level since there might be many parsing errors
                    logger.debug("Error parsing line '{}' in file {}: {}", 
                            line, filename, e.getMessage());
                }
            }
            
            // Log summary statistics
            logger.info("Processed file {} for server {}: {} total lines, {} kill records, {} suicides, {} falling deaths",
                    filename, server.getName(), totalLines, successfullyParsed, suicides, fallingDeaths);
            
        } catch (Exception e) {
            logger.error("Error processing file {} for server {}: {}", 
                    filename, server.getName(), e.getMessage(), e);
        }
        
        return records;
    }
    
    @Override
    public List<Choice> handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        if (event.getGuild() == null) return List.of();
        
        String focusedOption = event.getFocusedOption().getName();
        
        if ("name".equals(focusedOption)) {
            String currentInput = event.getFocusedOption().getValue().toLowerCase();
            List<GameServer> servers = serverRepository.findAllByGuildId(event.getGuild().getIdLong());
            
            return servers.stream()
                    .filter(server -> server.getName().toLowerCase().contains(currentInput))
                    .map(server -> new Choice(server.getName(), server.getName()))
                    .limit(25)
                    .collect(Collectors.toList());
        }
        
        return List.of();
    }
}