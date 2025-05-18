package com.deadside.bot.commands.help;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.utils.EmbedUtils;
import com.deadside.bot.utils.ResourceManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Advanced help command with categories and dropdown menus
 * Provides detailed information about the bot's functionality
 */
public class HelpCommand extends ListenerAdapter implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(HelpCommand.class);
    
    // Command categories for organization
    private static final String CATEGORY_GENERAL = "general";
    private static final String CATEGORY_ECONOMY = "economy";
    private static final String CATEGORY_STATS = "stats";
    private static final String CATEGORY_ADMIN = "admin";
    private static final String CATEGORY_EVENTS = "events";
    private static final String CATEGORY_FACTION = "faction";
    
    // Map of category IDs to their display names and descriptions
    private final Map<String, CategoryInfo> categories = new HashMap<>();
    
    // Map of category IDs to their commands
    private final Map<String, List<CommandInfo>> commandsByCategory = new HashMap<>();
    
    /**
     * Initialize the help command with categories and command information
     */
    public HelpCommand() {
        initializeCategories();
        populateCommands();
    }
    
    /**
     * Initialize category information
     */
    private void initializeCategories() {
        // Define categories with names, descriptions, and emoji
        categories.put(CATEGORY_GENERAL, new CategoryInfo(
                "General",
                "Basic bot commands and utilities",
                "🌐"
        ));
        
        categories.put(CATEGORY_ECONOMY, new CategoryInfo(
                "Economy & Bounties",
                "Currency, bounties, and gambling commands",
                "💰"
        ));
        
        categories.put(CATEGORY_STATS, new CategoryInfo(
                "Player Statistics",
                "Commands for viewing player and server stats",
                "📊"
        ));
        
        categories.put(CATEGORY_ADMIN, new CategoryInfo(
                "Admin Controls",
                "Server configuration and administrative commands",
                "⚙️"
        ));
        
        categories.put(CATEGORY_EVENTS, new CategoryInfo(
                "Game Events",
                "Commands related to in-game events",
                "🚁"
        ));
        
        categories.put(CATEGORY_FACTION, new CategoryInfo(
                "Factions",
                "Faction management and information",
                "👥"
        ));
        
        // Initialize command lists for each category
        for (String category : categories.keySet()) {
            commandsByCategory.put(category, new ArrayList<>());
        }
    }
    
    /**
     * Populate commands for each category
     */
    private void populateCommands() {
        // General commands
        commandsByCategory.get(CATEGORY_GENERAL).add(new CommandInfo(
                "help",
                "Display this help message with detailed command information",
                "Use the dropdown menu to navigate between command categories",
                false
        ));
        
        commandsByCategory.get(CATEGORY_GENERAL).add(new CommandInfo(
                "link",
                "Link your Discord account to your in-game player",
                "Use `/link <playername>` to connect your accounts",
                false
        ));
        
        // Economy commands
        commandsByCategory.get(CATEGORY_ECONOMY).add(new CommandInfo(
                "balance",
                "Check your current coin balance",
                "Use `/balance [user]` to view your coins or another player's",
                false
        ));
        
        commandsByCategory.get(CATEGORY_ECONOMY).add(new CommandInfo(
                "bounty",
                "Interact with the bounty system",
                "Subcommands: place, list, claim, top",
                false
        ));
        
        commandsByCategory.get(CATEGORY_ECONOMY).add(new CommandInfo(
                "daily",
                "Claim your daily coin reward",
                "Use once every 24 hours to get free coins",
                false
        ));
        
        commandsByCategory.get(CATEGORY_ECONOMY).add(new CommandInfo(
                "work",
                "Complete tasks to earn coins",
                "Available once per hour",
                false
        ));
        
        commandsByCategory.get(CATEGORY_ECONOMY).add(new CommandInfo(
                "bank",
                "Manage your bank account",
                "Subcommands: deposit, withdraw, balance",
                false
        ));
        
        commandsByCategory.get(CATEGORY_ECONOMY).add(new CommandInfo(
                "blackjack",
                "Play blackjack to win coins",
                "Place bets and try to beat the dealer",
                false
        ));
        
        commandsByCategory.get(CATEGORY_ECONOMY).add(new CommandInfo(
                "roulette",
                "Play roulette to win coins",
                "Bet on numbers, colors, or sections",
                false
        ));
        
        commandsByCategory.get(CATEGORY_ECONOMY).add(new CommandInfo(
                "slot",
                "Play the slot machine to win coins",
                "Pull the lever and match symbols to win",
                false
        ));
        
        // Stats commands
        commandsByCategory.get(CATEGORY_STATS).add(new CommandInfo(
                "stats",
                "View player statistics",
                "Check kills, deaths, K/D ratio, and more",
                false
        ));
        
        commandsByCategory.get(CATEGORY_STATS).add(new CommandInfo(
                "leaderboard",
                "View server leaderboards",
                "Rankings for kills, deaths, K/D, and wealth",
                false
        ));
        
        // Admin commands
        commandsByCategory.get(CATEGORY_ADMIN).add(new CommandInfo(
                "server",
                "Configure server settings",
                "Set server name, description, and other settings",
                true
        ));
        
        commandsByCategory.get(CATEGORY_ADMIN).add(new CommandInfo(
                "setbountychannel",
                "Set the channel for bounty notifications",
                "Configure where bounty alerts are sent",
                true
        ));
        
        commandsByCategory.get(CATEGORY_ADMIN).add(new CommandInfo(
                "bountysettings",
                "Configure bounty system settings",
                "Set minimum and maximum bounty amounts",
                true
        ));
        
        commandsByCategory.get(CATEGORY_ADMIN).add(new CommandInfo(
                "admineconomy",
                "Administrative economy commands",
                "Add or remove coins from players",
                true
        ));
        
        commandsByCategory.get(CATEGORY_ADMIN).add(new CommandInfo(
                "premium",
                "Manage premium features",
                "Check premium status and features",
                true
        ));
        
        commandsByCategory.get(CATEGORY_ADMIN).add(new CommandInfo(
                "test",
                "Generate test event messages",
                "Create sample outputs for different events",
                true
        ));
        
        // Event commands
        commandsByCategory.get(CATEGORY_EVENTS).add(new CommandInfo(
                "killfeed",
                "View recent kills on the server",
                "Detailed information about player eliminations",
                false
        ));
        
        // Faction commands
        commandsByCategory.get(CATEGORY_FACTION).add(new CommandInfo(
                "faction",
                "Manage your faction",
                "Create, join, or leave factions",
                false
        ));
    }
    
    @Override
    public String getName() {
        return "help";
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash("help", "Display help information for bot commands");
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        try {
            // Create category selection menu
            StringSelectMenu categoriesMenu = StringSelectMenu.create("help:category")
                    .setPlaceholder("Select a command category")
                    .addOptions(getCategoryOptions())
                    .build();
            
            // Create main help embed
            EmbedBuilder mainEmbed = new EmbedBuilder()
                    .setTitle("Deadside Bot Help")
                    .setDescription("Welcome to the Deadside Discord Bot help system. Use the dropdown menu below to browse command categories.")
                    .setColor(EmbedUtils.EMERALD_GREEN)
                    .addField("About", "This bot provides features for Deadside game servers including killfeed tracking, player statistics, an economy system, and more.", false)
                    .addField("Navigation", "Select a category from the dropdown menu to see available commands.", false)
                    .setFooter(EmbedUtils.STANDARD_FOOTER)
                    .setTimestamp(Instant.now())
                    .setThumbnail("attachment://" + ResourceManager.MAIN_LOGO);
            
            FileUpload icon = ResourceManager.getImageAsFileUpload(ResourceManager.MAIN_LOGO);
            
            // Reply with the embed and category selection menu
            event.replyEmbeds(mainEmbed.build())
                    .addComponents(ActionRow.of(categoriesMenu))
                    .addFiles(icon)
                    .queue();
            
        } catch (Exception e) {
            logger.error("Error executing help command", e);
            event.replyEmbeds(EmbedUtils.errorEmbed(
                    "Error",
                    "An error occurred while processing the help command.")
            ).setEphemeral(true).queue();
        }
    }
    
    @Override
    public List<Choice> handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        return new ArrayList<>(); // No autocomplete needed
    }
    
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        // Handle dropdown menu selections for help categories
        if (!event.getComponentId().startsWith("help:")) {
            return; // Not our component
        }
        
        if (event.getComponentId().equals("help:category")) {
            // Category selection
            String selectedCategory = event.getValues().get(0);
            displayCategoryCommands(event, selectedCategory);
        } else if (event.getComponentId().equals("help:back")) {
            // Back to categories
            displayMainHelp(event);
        }
    }
    
    /**
     * Display the main help menu with category selection
     */
    private void displayMainHelp(StringSelectInteractionEvent event) {
        StringSelectMenu categoriesMenu = StringSelectMenu.create("help:category")
                .setPlaceholder("Select a command category")
                .addOptions(getCategoryOptions())
                .build();
        
        EmbedBuilder mainEmbed = new EmbedBuilder()
                .setTitle("Deadside Bot Help")
                .setDescription("Welcome to the Deadside Discord Bot help system. Use the dropdown menu below to browse command categories.")
                .setColor(EmbedUtils.EMERALD_GREEN)
                .addField("About", "This bot provides features for Deadside game servers including killfeed tracking, player statistics, an economy system, and more.", false)
                .addField("Navigation", "Select a category from the dropdown menu to see available commands.", false)
                .setFooter(EmbedUtils.STANDARD_FOOTER)
                .setTimestamp(Instant.now())
                .setThumbnail("attachment://" + ResourceManager.MAIN_LOGO);
        
        FileUpload icon = ResourceManager.getImageAsFileUpload(ResourceManager.MAIN_LOGO);
        
        event.editMessageEmbeds(mainEmbed.build())
                .setComponents(ActionRow.of(categoriesMenu))
                .setFiles(icon)
                .queue();
    }
    
    /**
     * Display commands for a specific category
     */
    private void displayCategoryCommands(StringSelectInteractionEvent event, String categoryId) {
        if (!categories.containsKey(categoryId)) {
            event.reply("Invalid category selected.").setEphemeral(true).queue();
            return;
        }
        
        CategoryInfo category = categories.get(categoryId);
        List<CommandInfo> commands = commandsByCategory.get(categoryId);
        
        // Create back button
        StringSelectMenu backMenu = StringSelectMenu.create("help:back")
                .setPlaceholder("Return to categories")
                .addOption("Back to Categories", "back", "Return to the main help menu", Emoji.fromUnicode("⬅️"))
                .build();
        
        // Build embed for category
        EmbedBuilder categoryEmbed = new EmbedBuilder()
                .setTitle(category.name + " Commands")
                .setDescription(category.description)
                .setColor(EmbedUtils.EMERALD_GREEN)
                .setFooter(EmbedUtils.STANDARD_FOOTER)
                .setTimestamp(Instant.now());
        
        // Add icon based on category
        String iconName = getIconForCategory(categoryId);
        if (iconName != null) {
            categoryEmbed.setThumbnail("attachment://" + iconName);
        }
        
        // Add commands to embed
        if (commands.isEmpty()) {
            categoryEmbed.addField("No Commands", "There are no commands in this category yet.", false);
        } else {
            for (CommandInfo command : commands) {
                String adminNote = command.requiresAdmin ? " *(Admin Only)*" : "";
                categoryEmbed.addField("/" + command.name + adminNote, 
                        command.description + "\n*" + command.usage + "*", false);
            }
        }
        
        // Send the embed with the back button
        FileUpload icon = null;
        if (iconName != null) {
            icon = ResourceManager.getImageAsFileUpload(iconName);
            event.editMessageEmbeds(categoryEmbed.build())
                    .setComponents(ActionRow.of(backMenu))
                    .setFiles(icon)
                    .queue();
        } else {
            event.editMessageEmbeds(categoryEmbed.build())
                    .setComponents(ActionRow.of(backMenu))
                    .queue();
        }
    }
    
    /**
     * Get SelectOptions for all categories
     */
    private List<SelectOption> getCategoryOptions() {
        List<SelectOption> options = new ArrayList<>();
        
        for (Map.Entry<String, CategoryInfo> entry : categories.entrySet()) {
            String id = entry.getKey();
            CategoryInfo info = entry.getValue();
            
            options.add(SelectOption.of(info.name, id)
                    .withDescription(info.description)
                    .withEmoji(Emoji.fromUnicode(info.emoji)));
        }
        
        return options;
    }
    
    /**
     * Get icon filename for a category
     */
    private String getIconForCategory(String categoryId) {
        switch (categoryId) {
            case CATEGORY_GENERAL:
                return ResourceManager.MAIN_LOGO;
            case CATEGORY_ECONOMY:
                return ResourceManager.BOUNTY_ICON;
            case CATEGORY_STATS:
                return ResourceManager.WEAPON_STATS_ICON;
            case CATEGORY_ADMIN:
                return ResourceManager.TRADER_ICON;
            case CATEGORY_EVENTS:
                return ResourceManager.KILLFEED_LOGO;
            case CATEGORY_FACTION:
                return ResourceManager.FACTION_ICON;
            default:
                return null;
        }
    }
    
    /**
     * Simple class to store category information
     */
    private static class CategoryInfo {
        final String name;
        final String description;
        final String emoji;
        
        CategoryInfo(String name, String description, String emoji) {
            this.name = name;
            this.description = description;
            this.emoji = emoji;
        }
    }
    
    /**
     * Simple class to store command information
     */
    private static class CommandInfo {
        final String name;
        final String description;
        final String usage;
        final boolean requiresAdmin;
        
        CommandInfo(String name, String description, String usage, boolean requiresAdmin) {
            this.name = name;
            this.description = description;
            this.usage = usage;
            this.requiresAdmin = requiresAdmin;
        }
    }
}