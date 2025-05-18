package com.deadside.bot.commands.admin;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.KillRecord;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.KillRecordRepository;
import com.deadside.bot.utils.EmbedUtils;
import com.deadside.bot.utils.ServerDataCleanupUtil;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command for testing server isolation functionality 
 * This command simulates removing and re-adding a server to verify data isolation
 */
public class TestServerIsolationCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(TestServerIsolationCommand.class);
    private final GameServerRepository serverRepository = new GameServerRepository();
    private final KillRecordRepository killRecordRepository = new KillRecordRepository();
    
    @Override
    public String getName() {
        return "testisolation";
    }
    
    @Override
    public CommandData getCommandData() {
        // Create option data for server name with autocomplete
        OptionData serverNameOption = new OptionData(
                OptionType.STRING, "name", "The name of the server to test isolation", true)
                .setAutoComplete(true);
        
        return Commands.slash(getName(), "Test server data isolation by simulating remove/readd")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOptions(serverNameOption);
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Defer reply since this will be a longer operation
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
        
        // Look up the server
        GameServer server = serverRepository.findByGuildIdAndName(event.getGuild().getIdLong(), serverName);
        if (server == null) {
            event.getHook().sendMessage("No server found with name: " + serverName).setEphemeral(true).queue();
            return;
        }
        
        // Start the test
        testServerIsolation(event, server);
    }
    
    private void testServerIsolation(SlashCommandInteractionEvent event, GameServer server) {
        try {
            Guild guild = event.getGuild();
            String serverName = server.getName();
            long guildId = guild.getIdLong();
            
            // Step 1: Check existing kill records
            long initialKillRecords = killRecordRepository.countByServerIdAndGuildId(serverName, guildId);
            
            // Step 2: Create a backup of the server
            GameServer backupServer = createServerBackup(server);
            
            // Step 3: Send initial status
            event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Isolation Test Started",
                    "Testing server isolation for **" + serverName + "**\n" +
                    "Current kill records: " + initialKillRecords + "\n\n" +
                    "Step 1: Removing server and cleaning up data...")).queue();
            
            // Step 4: Remove server and clean up data
            ServerDataCleanupUtil.CleanupSummary cleanupSummary = 
                    ServerDataCleanupUtil.cleanupServerData(server);
            serverRepository.delete(server);
            
            // Step 5: Check that data is gone
            long afterRemovalKillRecords = killRecordRepository.countByServerIdAndGuildId(serverName, guildId);
            
            // Step 6: Send interim status
            event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Isolation Test Progress",
                    "Server **" + serverName + "** removed\n" +
                    "Cleanup summary: " + cleanupSummary + "\n" +
                    "Kill records before: " + initialKillRecords + "\n" +
                    "Kill records after removal: " + afterRemovalKillRecords + "\n\n" +
                    "Step 2: Re-adding server...")).queue();
            
            // Step 7: Re-add the server
            serverRepository.save(backupServer);
            
            // Step 8: Check that data is still clean
            long afterReaddKillRecords = killRecordRepository.countByServerIdAndGuildId(serverName, guildId);
            
            // Step 9: Send final results
            if (afterRemovalKillRecords == 0 && afterReaddKillRecords == 0) {
                event.getHook().sendMessageEmbeds(EmbedUtils.successEmbed("Isolation Test Successful",
                        "Server **" + serverName + "** isolation test passed!\n\n" +
                        "• Initial kill records: " + initialKillRecords + "\n" +
                        "• After removal: " + afterRemovalKillRecords + " (should be 0)\n" +
                        "• After re-adding: " + afterReaddKillRecords + " (should be 0)\n\n" +
                        "Server data is being properly isolated and cleaned up during removal.")).queue();
            } else {
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Isolation Test Failed",
                        "Server **" + serverName + "** isolation test failed!\n\n" +
                        "• Initial kill records: " + initialKillRecords + "\n" +
                        "• After removal: " + afterRemovalKillRecords + " (should be 0)\n" +
                        "• After re-adding: " + afterReaddKillRecords + " (should be 0)\n\n" +
                        "There may be issues with the data cleanup process.")).queue();
            }
            
            logger.info("Completed isolation test for server '{}' in guild {}", serverName, guildId);
        } catch (Exception e) {
            logger.error("Error during isolation test for server: {}", server.getName(), e);
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Test Error",
                    "An error occurred during the isolation test: " + e.getMessage())).queue();
        }
    }
    
    /**
     * Create a backup of a server object
     */
    private GameServer createServerBackup(GameServer original) {
        GameServer backup = new GameServer(
                original.getGuildId(),
                original.getName(),
                original.getHost(),
                original.getPort(),
                original.getUsername(),
                original.getPassword(),
                original.getServerId()
        );
        
        backup.setKillfeedChannelId(original.getKillfeedChannelId());
        backup.setLogChannelId(original.getLogChannelId());
        backup.setLastProcessedKillfeedFile(original.getLastProcessedKillfeedFile());
        backup.setLastProcessedKillfeedLine(original.getLastProcessedKillfeedLine());
        backup.setLastProcessedLogFile(original.getLastProcessedLogFile());
        backup.setLastProcessedLogLine(original.getLastProcessedLogLine());
        
        return backup;
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