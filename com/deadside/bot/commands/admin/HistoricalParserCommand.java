package com.deadside.bot.commands.admin;

import com.deadside.bot.commands.CommandCategory;
import com.deadside.bot.commands.ICommand;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.utils.EmbedUtils;
import com.deadside.bot.utils.ParserStateManager;
import com.deadside.bot.utils.ResourceManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Command for controlling historical data parsing
 * Allows admins to enable/disable historical parsing for specific servers
 */
public class HistoricalParserCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(HistoricalParserCommand.class);
    private final GameServerRepository gameServerRepository;
    
    public HistoricalParserCommand() {
        this.gameServerRepository = new GameServerRepository();
    }
    
    @Override
    public String getName() {
        return "historical";
    }
    
    @Override
    public String getDescription() {
        return "Control historical data parsing for servers";
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.ADMIN;
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription())
                .addSubcommands(
                        new SubcommandData("enable", "Enable historical parsing for a server")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "server", "The server name", true)
                                ),
                        new SubcommandData("disable", "Disable historical parsing for a server")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "server", "The server name", true)
                                ),
                        new SubcommandData("status", "Check the status of historical parsing")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "server", "The server name", true)
                                )
                )
                .setDefaultPermissions(Permission.ADMINISTRATOR);
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Only allow admins to use this command
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.errorEmbed(
                    "Permission Denied",
                    "You need Administrator permission to use this command."
            )).setEphemeral(true).queue();
            return;
        }
        
        // Get the subcommand
        String subcommand = event.getSubcommandName();
        
        if (subcommand == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed(
                    "Invalid Command",
                    "Please specify a valid subcommand: enable, disable, or status."
            )).setEphemeral(true).queue();
            return;
        }
        
        // Get the server name
        String serverName = event.getOption("server").getAsString();
        
        // Get guild ID
        long guildId = event.getGuild().getIdLong();
        
        // Find the specified server
        List<GameServer> servers = gameServerRepository.findByGuildIdAndName(guildId, serverName);
        if (servers.isEmpty()) {
            event.replyEmbeds(EmbedUtils.errorEmbed(
                    "Server Not Found",
                    "No server found with name: " + serverName
            )).setEphemeral(true).queue();
            return;
        }
        
        GameServer server = servers.get(0);
        
        // Process the subcommand
        switch (subcommand) {
            case "enable":
                enableHistoricalParsing(event, server);
                break;
            case "disable":
                disableHistoricalParsing(event, server);
                break;
            case "status":
                checkHistoricalParsingStatus(event, server);
                break;
            default:
                event.replyEmbeds(EmbedUtils.errorEmbed(
                        "Invalid Subcommand",
                        "Please specify a valid subcommand: enable, disable, or status."
                )).setEphemeral(true).queue();
        }
    }
    
    /**
     * Enable historical parsing for a server
     */
    private void enableHistoricalParsing(SlashCommandInteractionEvent event, GameServer server) {
        try {
            // Enable historical parsing
            ParserStateManager.enableHistoricalParsing(server.getName(), server.getGuildId());
            
            // Reset tracking to force a fresh start
            server.setLastProcessedKillfeedFile("");
            server.setLastProcessedKillfeedLine(0);
            gameServerRepository.save(server);
            
            // Send success message
            MessageEmbed embed = EmbedUtils.createEmbed(
                    "Historical Parsing Enabled",
                    "Historical parsing has been enabled for server: " + server.getName(),
                    EmbedUtils.DEADSIDE_SUCCESS,
                    ResourceManager.getAttachmentString(ResourceManager.MAIN_LOGO)
            );
            
            event.replyEmbeds(embed).setEphemeral(true).queue();
            
            logger.info("Historical parsing enabled for server: {} in guild: {} by user: {}",
                    server.getName(), server.getGuildId(), event.getUser().getAsTag());
        } catch (Exception e) {
            event.replyEmbeds(EmbedUtils.errorEmbed(
                    "Error",
                    "An error occurred while enabling historical parsing: " + e.getMessage()
            )).setEphemeral(true).queue();
            
            logger.error("Error enabling historical parsing for server: {}", server.getName(), e);
        }
    }
    
    /**
     * Disable historical parsing for a server
     */
    private void disableHistoricalParsing(SlashCommandInteractionEvent event, GameServer server) {
        try {
            // Disable historical parsing - ParserStateManager.disableHistoricalParsing already calls clearParserMemory()
            ParserStateManager.disableHistoricalParsing(server.getName(), server.getGuildId());
            
            // Update server database with last processed values from memory if needed
            // This ensures continuity between historical and regular parsing
            ParserStateManager.ParserState state = ParserStateManager.getParserState(
                    server.getName(), server.getGuildId());
            
            String lastFile = (String) state.retrieveFromMemory("lastProcessedFile");
            Long lastLine = (Long) state.retrieveFromMemory("lastProcessedLine");
            
            if (lastFile != null && lastLine != null) {
                server.setLastProcessedKillfeedFile(lastFile);
                server.setLastProcessedKillfeedLine(lastLine);
                gameServerRepository.save(server);
                logger.info("Updated server database state after historical parsing: file={}, line={}", 
                        lastFile, lastLine);
            }
            
            // Send success message
            MessageEmbed embed = EmbedUtils.createEmbed(
                    "Historical Parsing Disabled",
                    "Historical parsing has been disabled for server: " + server.getName() + 
                    "\nServer is now ready for regular parsing with updated tracking.",
                    EmbedUtils.DEADSIDE_SUCCESS,
                    ResourceManager.getAttachmentString(ResourceManager.MAIN_LOGO)
            );
            
            event.replyEmbeds(embed).setEphemeral(true).queue();
            
            logger.info("Historical parsing disabled for server: {} in guild: {} by user: {}",
                    server.getName(), server.getGuildId(), event.getUser().getAsTag());
        } catch (Exception e) {
            event.replyEmbeds(EmbedUtils.errorEmbed(
                    "Error",
                    "An error occurred while disabling historical parsing: " + e.getMessage()
            )).setEphemeral(true).queue();
            
            logger.error("Error disabling historical parsing for server: {}", server.getName(), e);
        }
    }
    
    /**
     * Check historical parsing status for a server
     */
    private void checkHistoricalParsingStatus(SlashCommandInteractionEvent event, GameServer server) {
        try {
            // Get historical parsing status
            boolean isEnabled = ParserStateManager.isHistoricalParsingEnabled(server.getName(), server.getGuildId());
            
            // Send status message
            String status = isEnabled ? "Enabled" : "Disabled";
            MessageEmbed embed = EmbedUtils.createEmbed(
                    "Historical Parsing Status",
                    "Status for server " + server.getName() + ": **" + status + "**\n\n" +
                            "Last processed killfeed file: " + (server.getLastProcessedKillfeedFile().isEmpty() ? 
                                    "*None*" : server.getLastProcessedKillfeedFile()) + "\n" +
                            "Last processed line: " + server.getLastProcessedKillfeedLine(),
                    EmbedUtils.DEADSIDE_INFO,
                    ResourceManager.getAttachmentString(ResourceManager.MAIN_LOGO)
            );
            
            event.replyEmbeds(embed).setEphemeral(true).queue();
            
            logger.info("Historical parsing status checked for server: {} in guild: {} by user: {}",
                    server.getName(), server.getGuildId(), event.getUser().getAsTag());
        } catch (Exception e) {
            event.replyEmbeds(EmbedUtils.errorEmbed(
                    "Error",
                    "An error occurred while checking historical parsing status: " + e.getMessage()
            )).setEphemeral(true).queue();
            
            logger.error("Error checking historical parsing status for server: {}", server.getName(), e);
        }
    }
}