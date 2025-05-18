package com.deadside.bot.commands;

import com.deadside.bot.commands.admin.BountySettingsCommand;
import com.deadside.bot.commands.admin.HistoricalParserCommand;
import com.deadside.bot.commands.admin.PremiumCommand;
import com.deadside.bot.commands.admin.ProcessHistoricalDataCommand;
import com.deadside.bot.commands.admin.ServerCommand;
import com.deadside.bot.commands.admin.TestCommand;
import com.deadside.bot.commands.admin.TestServerIsolationCommand;
import com.deadside.bot.commands.economy.BalanceCommand;
import com.deadside.bot.commands.economy.BankCommand;
import com.deadside.bot.commands.economy.BlackjackCommand;
import com.deadside.bot.commands.economy.BountyCommandWrapper;
import com.deadside.bot.commands.economy.DailyCommand;
import com.deadside.bot.commands.economy.RouletteCommand;
import com.deadside.bot.commands.economy.SlotCommand;
import com.deadside.bot.commands.economy.WorkCommand;
import com.deadside.bot.commands.economy.AdminEconomyCommand;
import com.deadside.bot.commands.faction.FactionCommand;
import com.deadside.bot.commands.info.HelpCommand;
import com.deadside.bot.commands.player.LinkCommand;
import com.deadside.bot.commands.server.SetBountyChannelCommandWrapper;
import com.deadside.bot.commands.stats.LeaderboardCommand;
import com.deadside.bot.commands.stats.StatsCommand;
import com.deadside.bot.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages command registration and execution
 */
public class CommandManager {
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);
    private final Map<String, ICommand> commands = new HashMap<>();
    private final Config config = Config.getInstance();
    
    public CommandManager() {
        registerCommand(new ServerCommand());
        registerCommand(new StatsCommand());
        registerCommand(new LeaderboardCommand());
        registerCommand(new LinkCommand());
        registerCommand(new FactionCommand());
        registerCommand(new BalanceCommand());
        registerCommand(new BankCommand());
        registerCommand(new DailyCommand());
        registerCommand(new SlotCommand());
        registerCommand(new BlackjackCommand());
        registerCommand(new RouletteCommand());
        registerCommand(new WorkCommand());
        registerCommand(new AdminEconomyCommand());
        registerCommand(new PremiumCommand());
        
        // Bounty system commands
        registerCommand(new BountyCommandWrapper());
        registerCommand(new SetBountyChannelCommandWrapper());
        registerCommand(new BountySettingsCommand());
        
        // Admin data management commands
        registerCommand(new ProcessHistoricalDataCommand());
        registerCommand(new TestServerIsolationCommand());
        registerCommand(new HistoricalParserCommand());
        
        // Info commands
        registerCommand(new HelpCommand());
        
        // Test commands
        registerCommand(new TestCommand());
    }
    
    /**
     * Register a command handler
     */
    private void registerCommand(ICommand command) {
        commands.put(command.getName(), command);
        logger.debug("Registered command: {}", command.getName());
    }
    
    /**
     * Register all commands with Discord
     */
    public void registerCommands(JDA jda) {
        List<CommandData> globalCommands = new ArrayList<>();
        
        // Collect command data from each command
        for (ICommand command : commands.values()) {
            globalCommands.add(command.getCommandData());
        }
        
        // Register global commands
        jda.updateCommands().addCommands(globalCommands).queue(
            success -> logger.info("Successfully registered {} global commands", globalCommands.size()),
            error -> logger.error("Failed to register global commands", error)
        );
    }
    
    /**
     * Handle a slash command interaction
     */
    public void handleCommand(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        ICommand command = commands.get(commandName);
        
        if (command != null) {
            try {
                command.execute(event);
            } catch (Exception e) {
                logger.error("Error executing command: {}", commandName, e);
                
                // If the interaction has not been acknowledged, reply with an error
                if (!event.isAcknowledged()) {
                    event.reply("An error occurred while executing this command. Please try again later.")
                         .setEphemeral(true)
                         .queue();
                }
            }
        } else {
            logger.warn("Unknown command received: {}", commandName);
            event.reply("Unknown command.").setEphemeral(true).queue();
        }
    }
    
    /**
     * Handle autocomplete interactions
     */
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        String commandName = event.getName();
        ICommand command = commands.get(commandName);
        
        if (command != null) {
            try {
                List<Choice> choices = command.handleAutoComplete(event);
                
                if (!choices.isEmpty()) {
                    // Reply with suggestions (max 25 choices)
                    event.replyChoices(choices.size() > 25 ? choices.subList(0, 25) : choices).queue();
                } else {
                    // No suggestions
                    event.replyChoices().queue();
                }
            } catch (Exception e) {
                logger.error("Error handling autocomplete for command: {}", commandName, e);
                event.replyChoices().queue(); // Reply with no choices on error
            }
        } else {
            logger.warn("Autocomplete requested for unknown command: {}", commandName);
            event.replyChoices().queue();
        }
    }
    
    /**
     * Check if a user is the bot owner
     */
    public boolean isOwner(long userId) {
        // Hardcoded owner ID as requested
        return userId == 462961235382763520L;
    }
}
