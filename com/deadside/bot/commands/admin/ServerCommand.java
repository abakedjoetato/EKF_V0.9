package com.deadside.bot.commands.admin;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.config.Config;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.KillfeedParser;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.utils.ParserStateManager;
import java.util.concurrent.CompletableFuture;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.GuildConfig;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.GuildConfigRepository;
import com.deadside.bot.sftp.SftpManager;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * Command for managing game servers
 */
public class ServerCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(ServerCommand.class);
    private final GameServerRepository serverRepository = new GameServerRepository();
    private final GuildConfigRepository guildConfigRepository = new GuildConfigRepository();
    private final SftpManager sftpManager = new SftpManager();
    
    @Override
    public String getName() {
        return "server";
    }
    
    @Override
    public CommandData getCommandData() {
        // Create option data for server name with autocomplete
        OptionData serverNameOption = new OptionData(OptionType.STRING, "name", "The name of the server", true)
                .setAutoComplete(true);
        
        return Commands.slash(getName(), "Manage Deadside game servers")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                        new SubcommandData("add", "Add a new game server")
                                .addOption(OptionType.STRING, "name", "The name of the server", true)
                                .addOption(OptionType.STRING, "host", "SFTP host address", true)
                                .addOption(OptionType.INTEGER, "port", "SFTP port", true)
                                .addOption(OptionType.STRING, "username", "SFTP username", true)
                                .addOption(OptionType.STRING, "password", "SFTP password", true)
                                .addOption(OptionType.INTEGER, "gameserver", "Game server ID", true),
                        new SubcommandData("remove", "Remove a game server")
                                .addOptions(serverNameOption),
                        new SubcommandData("list", "List all configured game servers"),
                        new SubcommandData("test", "Test SFTP connection to a server")
                                .addOptions(serverNameOption),
                        new SubcommandData("setkillfeed", "Set the killfeed channel for a server")
                                .addOptions(serverNameOption)
                                .addOption(OptionType.CHANNEL, "channel", "Channel for killfeed updates", true),
                        new SubcommandData("setlogs", "Set the server log channel for events")
                                .addOptions(serverNameOption)
                                .addOption(OptionType.CHANNEL, "channel", "Channel for server events and player join/leave logs", true)
                );
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Defer reply immediately to prevent timeout during database operations
        event.deferReply().queue();
        
        if (event.getGuild() == null) {
            event.getHook().sendMessage("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }
        
        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().sendMessage("You need Administrator permission to use this command.").setEphemeral(true).queue();
            return;
        }
        
        String subCommand = event.getSubcommandName();
        if (subCommand == null) {
            event.getHook().sendMessage("Invalid command usage.").setEphemeral(true).queue();
            return;
        }
        
        try {
            switch (subCommand) {
                case "add" -> addServer(event);
                case "remove" -> removeServer(event);
                case "list" -> listServers(event);
                case "test" -> testServerConnection(event);
                case "setkillfeed" -> setKillfeed(event);
                case "setlogs" -> setLogs(event);
                default -> event.getHook().sendMessage("Unknown subcommand: " + subCommand).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            logger.error("Error executing server command", e);
            event.getHook().sendMessage("An error occurred: " + e.getMessage()).setEphemeral(true).queue();
        }
    }
    
    private void addServer(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        // Parse all options
        String name = event.getOption("name", OptionMapping::getAsString);
        String host = event.getOption("host", OptionMapping::getAsString);
        int port = event.getOption("port", OptionMapping::getAsInt); // Now required
        String username = event.getOption("username", OptionMapping::getAsString);
        String password = event.getOption("password", OptionMapping::getAsString);
        int gameServerId = event.getOption("gameserver", OptionMapping::getAsInt);
        
        // Acknowledge the command immediately
        event.deferReply(true).queue();
        
        // Check if guild config exists, create if not
        GuildConfig guildConfig = guildConfigRepository.findByGuildId(guild.getIdLong());
        if (guildConfig == null) {
            guildConfig = new GuildConfig(guild.getIdLong());
            guildConfigRepository.save(guildConfig);
        }
        
        // Check if server already exists
        if (serverRepository.findByGuildIdAndName(guild.getIdLong(), name) != null) {
            event.getHook().sendMessage("A server with this name already exists.").queue();
            return;
        }
        
        // Create new server
        GameServer gameServer = new GameServer(
                guild.getIdLong(),
                name,
                host,
                port,
                username,
                password,
                gameServerId
        );
        
        // Test the connection first
        try {
            boolean connectionResult = sftpManager.testConnection(gameServer);
            if (!connectionResult) {
                event.getHook().sendMessage("Failed to connect to the SFTP server. Please check your credentials and try again.").queue();
                return;
            }
            
            // Save the server if connection was successful
            serverRepository.save(gameServer);
            
            // Send initial success message
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.successEmbed("Server Added", 
                            "Successfully added server **" + name + "**\n" +
                            "Host: " + host + "\n" +
                            "Port: " + port + "\n" +
                            "Game Server ID: " + gameServerId + "\n\n" +
                            "You can set a killfeed channel with `/server setkillfeed " + name + " #channel`\n" +
                            "The bot will look for logs in: " + gameServer.getLogDirectory() + "\n" +
                            "And deathlogs in: " + gameServer.getDeathlogsDirectory())
            ).queue();
            
            // Start processing historical data in the background with progress updates
            processHistoricalDataWithUpdates(event, gameServer);
            
            logger.info("Added new game server '{}' for guild {}", name, guild.getId());
        } catch (Exception e) {
            logger.error("Error adding server", e);
            event.getHook().sendMessage("Error adding server: " + e.getMessage()).queue();
        }
    }
    
    private void setKillfeed(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        String serverName = event.getOption("name", OptionMapping::getAsString);
        TextChannel channel = event.getOption("channel", OptionMapping::getAsChannel).asTextChannel();
        
        // Look up the server
        GameServer server = serverRepository.findByGuildIdAndName(guild.getIdLong(), serverName);
        if (server == null) {
            event.getHook().sendMessage("No server found with name: " + serverName).setEphemeral(true).queue();
            return;
        }
        
        // Update the killfeed channel
        server.setKillfeedChannelId(channel.getIdLong());
        serverRepository.save(server);
        
        event.getHook().sendMessage("Killfeed channel for server **" + serverName + "** has been set to <#" + channel.getId() + ">.").queue();
        logger.info("Updated killfeed channel for server '{}' to {}", serverName, channel.getId());
    }
    
    private void removeServer(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        String name = event.getOption("name", OptionMapping::getAsString);
        
        // Look up the server
        GameServer server = serverRepository.findByGuildIdAndName(guild.getIdLong(), name);
        if (server == null) {
            event.getHook().sendMessage("No server found with name: " + name).setEphemeral(true).queue();
            return;
        }
        
        try {
            // First pause any parsers for this server to prevent race conditions
            com.deadside.bot.utils.ParserStateManager.pauseKillfeedParser(
                    server.getName(), server.getGuildId(), "Server removal in progress");
            com.deadside.bot.utils.ParserStateManager.pauseCSVParser(
                    server.getName(), server.getGuildId(), "Server removal in progress");
            
            // Use the centralized cleanup utility to delete all associated data
            com.deadside.bot.utils.ServerDataCleanupUtil.CleanupSummary cleanupSummary = 
                    com.deadside.bot.utils.ServerDataCleanupUtil.cleanupServerData(server);
            
            if (!cleanupSummary.isSuccess()) {
                // Log the error but continue with server removal
                logger.warn("Partial error cleaning up data for server '{}': {}", 
                        name, cleanupSummary.getErrorMessage());
            }
            
            // After cleanup, remove the server itself
            serverRepository.delete(server);
            
            // Build a detailed success message
            StringBuilder detailsBuilder = new StringBuilder();
            detailsBuilder.append("Server **").append(name).append("** has been removed.\n");
            detailsBuilder.append("Data cleanup summary:\n");
            detailsBuilder.append("• ").append(cleanupSummary.getKillRecordsDeleted())
                    .append(" kill records deleted\n");
            detailsBuilder.append("• ").append(cleanupSummary.getPlayerRecordsDeleted())
                    .append(" player stats reset\n");
            detailsBuilder.append("• ").append(cleanupSummary.getBountiesDeleted())
                    .append(" bounties deleted\n");
            detailsBuilder.append("• ").append(cleanupSummary.getFactionsDeleted())
                    .append(" factions deleted\n");
            
            event.getHook().sendMessageEmbeds(EmbedUtils.successEmbed(
                    "Server Removed", detailsBuilder.toString())).queue();
            
            logger.info("Successfully removed game server '{}' and all associated data from guild {}: {}", 
                    name, guild.getId(), cleanupSummary);
        } catch (Exception e) {
            // If there's any error during the cleanup process, we still attempt to delete the server
            // to prevent zombie servers in the database
            try {
                serverRepository.delete(server);
                logger.warn("Deleted server '{}' from database despite cleanup errors", name);
                
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Partial Success",
                        "Server **" + name + "** has been removed, but there were errors " +
                        "cleaning up some associated data: " + e.getMessage() + "\n" +
                        "This may result in orphaned data in the database.")).queue();
            } catch (Exception deleteEx) {
                logger.error("Error removing server '{}' from guild {}", name, guild.getId(), e);
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Error",
                        "An error occurred while removing the server: " + e.getMessage())).queue();
            }
        } finally {
            // Always reset the parser state manager for this server regardless of success/failure
            com.deadside.bot.utils.ParserStateManager.resetParserState(
                    server.getName(), server.getGuildId());
        }
    }
    
    private void listServers(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        // Get all servers for this guild
        List<GameServer> servers = serverRepository.findAllByGuildId(guild.getIdLong());
        
        if (servers.isEmpty()) {
            event.getHook().sendMessage("No game servers have been configured for this Discord server.").queue();
            return;
        }
        
        // Build server list
        StringBuilder description = new StringBuilder();
        for (GameServer server : servers) {
            description.append("**").append(server.getName()).append("**\n");
            description.append("Host: ").append(server.getHost()).append("\n");
            description.append("Killfeed Channel: <#").append(server.getKillfeedChannelId()).append(">\n\n");
        }
        
        event.getHook().sendMessageEmbeds(
                EmbedUtils.infoEmbed("Configured Game Servers", description.toString())
        ).queue();
    }
    
    private void testServerConnection(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        String name = event.getOption("name", OptionMapping::getAsString);
        
        // Acknowledge the command immediately
        event.deferReply(true).queue();
        
        // Look up the server
        GameServer server = serverRepository.findByGuildIdAndName(guild.getIdLong(), name);
        if (server == null) {
            event.getHook().sendMessage("No server found with name: " + name).queue();
            return;
        }
        
        // Test the connection
        try {
            boolean result = sftpManager.testConnection(server);
            
            if (result) {
                event.getHook().sendMessageEmbeds(
                        EmbedUtils.successEmbed("Connection Successful", 
                                "Successfully connected to SFTP server **" + server.getName() + "**")
                ).queue();
            } else {
                event.getHook().sendMessageEmbeds(
                        EmbedUtils.errorEmbed("Connection Failed", 
                                "Failed to connect to SFTP server **" + server.getName() + "**\n" +
                                "Please check your credentials and try again.")
                ).queue();
            }
        } catch (Exception e) {
            logger.error("Error testing connection to server", e);
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.errorEmbed("Connection Error", 
                            "Error testing connection to server: " + e.getMessage())
            ).queue();
        }
    }
    
    /**
     * Process historical data for a newly added server with regular progress updates
     * @param event The slash command interaction event
     * @param server The game server to process
     */
    private void processHistoricalDataWithUpdates(SlashCommandInteractionEvent event, GameServer server) {
        // Run this in a separate thread to avoid blocking
        CompletableFuture.runAsync(() -> {
            try {
                // Set up the thread name for better logging
                Thread.currentThread().setName("HistoricalProcessor-" + server.getName());
                
                // Send initial message
                event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Historical Data Processing",
                        "Starting to look for historical data for server **" + server.getName() + "**...\n" +
                        "This process will run in the background and send progress updates.")).queue();
                
                // Temporarily pause regular parsers
                String serverName = server.getName();
                long guildId = server.getGuildId();
                ParserStateManager.pauseKillfeedParser(serverName, guildId, "Initial historical data processing");
                ParserStateManager.pauseCSVParser(serverName, guildId, "Initial historical data processing");
                
                try {
                    // Create the required parsers and services
                    SftpConnector sftpConnector = new SftpConnector();
                    PlayerRepository playerRepository = new PlayerRepository();
                    KillfeedParser killfeedParser = new KillfeedParser(event.getJDA());
                    DeadsideCsvParser csvParser = new DeadsideCsvParser(
                            event.getJDA(), sftpConnector, playerRepository);
                    KillRecordRepository killRecordRepository = new KillRecordRepository();
                    
                    // Create the historical data processor
                    HistoricalDataProcessor historicalProcessor = new HistoricalDataProcessor(
                            serverRepository, killRecordRepository, sftpConnector, csvParser, killfeedParser);
                    
                    // Process historical data with efficient batch processing and progress updates
                    event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Historical Data Processing Started",
                            "Starting historical data processing for server **" + server.getName() + "**.\n" +
                            "This will process all death logs, player stats, and killfeed entries.\n" +
                            "Progress updates will be sent periodically.")).queue();
                    
                    // Process the historical data - this will give regular progress updates
                    HistoricalDataProcessor.ProcessingResult result = 
                            historicalProcessor.processHistoricalData(server, event, false);
                    
                    if (result.getTotal() == 0) {
                        // No historical data was found or processed
                        event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("No Historical Data Found",
                                "No historical data was found for server **" + server.getName() + "**.\n" +
                                "The bot will begin processing new data as it becomes available.\n\n" +
                                "**Important:** Set up a killfeed channel with `/server setkillfeed " + 
                                serverName + " #channel` to see kill notifications.")).queue();
                    } else {
                        // Historical data was processed successfully
                        event.getHook().sendMessageEmbeds(EmbedUtils.successEmbed("Historical Data Processing Complete",
                                String.format("Successfully processed historical data for server **%s**:\n" +
                                "• %d death logs processed\n" +
                                "• %d killfeed entries processed\n" +
                                "• All player statistics have been updated\n\n" +
                                "**Important:** Set up a killfeed channel with `/server setkillfeed %s #channel` to see kill notifications.",
                                server.getName(), result.getDeathLogs(), result.getKillfeed(), serverName))).queue();
                    }
                    
                } finally {
                    // Always resume the parsers
                    ParserStateManager.resumeKillfeedParser(serverName, guildId);
                    ParserStateManager.resumeCSVParser(serverName, guildId);
                }
                
            } catch (Exception e) {
                logger.error("Error during historical data processing for server {}", 
                        server.getName(), e);
                
                // Send error message
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Error Processing Data",
                        "An error occurred while processing historical data for server **" + 
                        server.getName() + "**:\n" + e.getMessage() + "\n\n" +
                        "The server has been added, but you may need to set up channels manually.")).queue();
            }
        });
    }
    
    private void setLogs(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        String serverName = event.getOption("name", OptionMapping::getAsString);
        TextChannel channel = event.getOption("channel", OptionMapping::getAsChannel).asTextChannel();
        
        if (channel == null) {
            event.getHook().sendMessage("Invalid channel specified.").setEphemeral(true).queue();
            return;
        }
        
        // Look up the server
        GameServer server = serverRepository.findByGuildIdAndName(guild.getIdLong(), serverName);
        if (server == null) {
            event.getHook().sendMessage("No server found with name: " + serverName).setEphemeral(true).queue();
            return;
        }
        
        // Update the log channel
        server.setLogChannelId(channel.getIdLong());
        serverRepository.save(server);
        
        event.getHook().sendMessage("Server log channel for **" + serverName + "** has been set to <#" + channel.getId() + ">. " +
                "You will now receive notifications for player joins/leaves and server events.").queue();
        logger.info("Updated log channel for server '{}' to {}", serverName, channel.getId());
    }
    
    @Override
    public List<Choice> handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return List.of();
        
        String focusedOption = event.getFocusedOption().getName();
        
        // We only have autocomplete for server names
        if ("name".equals(focusedOption)) {
            String currentInput = event.getFocusedOption().getValue().toLowerCase();
            List<GameServer> servers = serverRepository.findAllByGuildId(guild.getIdLong());
            
            // Filter for servers that match the current input
            return servers.stream()
                .filter(server -> server.getName().toLowerCase().contains(currentInput))
                .map(server -> new Choice(server.getName(), server.getName()))
                .limit(25) // Discord has a max of 25 choices
                .collect(Collectors.toList());
        }
        
        return List.of(); // Empty list for no suggestions
    }
}
