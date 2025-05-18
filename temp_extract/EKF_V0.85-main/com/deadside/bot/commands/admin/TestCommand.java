package com.deadside.bot.commands.admin;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.utils.EmbedUtils;
import com.deadside.bot.utils.ResourceManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Command for testing various game event embed formats
 * Creates mock outputs for different types of game events
 */
public class TestCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(TestCommand.class);
    private static final Random random = new Random();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Sample data for mock events
    private static final String[] PLAYER_NAMES = {
        "EmeraldHunter", "DeadsideVeteran", "ApexSurvivor", "WastelandScout", 
        "RadiationRanger", "ScrapCollector", "BountyKiller", "ZoneExplorer", 
        "cuhslice", "Jolina"
    };
    
    private static final String[] WEAPONS = {
        "M4A1", "AK-74", "Mosin", "Glock-17", "MP5", "SR-25", "SVD", "KA-M", "Sawed-Off Shotgun"
    };
    
    private static final String[] DEATH_TYPES = {
        "killfeed", "connection", "airdrop", "helicrash", "mission", "trader", "bounty",
        "suicide", "falling"
    };
    
    private static final String[] LOCATIONS = {
        "Military Base", "Riverside Town", "Northern Mountains", "Railway Station", 
        "Abandoned Factory", "Central Town", "Western Forest", "Eastern Checkpoint"
    };
    
    private static final String[] FACTIONS = {
        "Emerald Raiders", "Deadside Nomads", "Zone Stalkers", "Wasteland Scavengers",
        "Last Hope", "Raven Company", "Black Market Traders", "Lone Wolves"
    };
    
    @Override
    public String getName() {
        return "test";
    }
    
    @Override
    public CommandData getCommandData() {
        OptionData eventTypeOption = new OptionData(OptionType.STRING, "event", "Type of event to test", true)
                .addChoice("Killfeed", "killfeed")
                .addChoice("Connection", "connection")
                .addChoice("Airdrop", "airdrop")
                .addChoice("Helicrash", "helicrash")
                .addChoice("Mission", "mission")
                .addChoice("Trader", "trader")
                .addChoice("Bounty", "bounty")
                .addChoice("Suicide", "suicide")
                .addChoice("Falling", "falling");
        
        return Commands.slash("test", "Generate test event messages")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOptions(eventTypeOption);
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Defer reply to avoid timeout
        event.deferReply().queue();
        
        // Extract event type from options
        String eventType = event.getOption("event", "killfeed", OptionMapping::getAsString);
        
        try {
            // Create appropriate mock event based on type
            switch (eventType.toLowerCase()) {
                case "killfeed":
                    sendKillfeedEvent(event);
                    break;
                case "connection":
                    sendConnectionEvent(event);
                    break;
                case "airdrop":
                    sendAirdropEvent(event);
                    break;
                case "helicrash":
                    sendHelicrashEvent(event);
                    break;
                case "mission":
                    sendMissionEvent(event);
                    break;
                case "trader":
                    sendTraderEvent(event);
                    break;
                case "bounty":
                    sendBountyEvent(event);
                    break;
                case "suicide":
                    sendSuicideEvent(event);
                    break;
                case "falling":
                    sendFallingDeathEvent(event);
                    break;
                default:
                    event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                            "Invalid Event Type",
                            "The specified event type is not supported.")
                    ).queue();
            }
        } catch (Exception e) {
            logger.error("Error processing test event", e);
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                    "Error",
                    "An error occurred while generating the test event.")
            ).queue();
        }
    }
    
    @Override
    public List<Choice> handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        return new ArrayList<>(); // No autocomplete needed, using fixed choices
    }
    
    /**
     * Send a mock killfeed event using the actual embed generator
     */
    private void sendKillfeedEvent(SlashCommandInteractionEvent event) {
        String killer = getRandomPlayer();
        String victim = getRandomPlayer();
        while (victim.equals(killer)) {
            victim = getRandomPlayer(); // Ensure different players
        }
        
        String weapon = getRandomWeapon();
        int distance = random.nextInt(500) + 50;
        
        // Use our actual embed generator with the same data
        MessageEmbed embed = EmbedUtils.pvpKillfeedEmbed(killer, victim, weapon, distance);
        
        // Add the file attachment for the thumbnail
        FileUpload icon = ResourceManager.getImageAsFileUpload(ResourceManager.KILLFEED_LOGO);
        event.getHook().sendMessageEmbeds(embed)
                .addFiles(icon)
                .queue();
    }
    
    /**
     * Send a mock connection event
     */
    private void sendConnectionEvent(SlashCommandInteractionEvent event) {
        String player = getRandomPlayer();
        boolean isJoining = random.nextBoolean();
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        
        String title = isJoining ? "Player Connected" : "Player Disconnected";
        String description = isJoining ? 
                String.format("%s has entered the wasteland", player) :
                String.format("%s has left the wasteland", player);
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(isJoining ? EmbedUtils.EMERALD_GREEN : EmbedUtils.DARK_GRAY)
                .addField("Player", player, true)
                .addField("Status", isJoining ? "Connected" : "Disconnected", true)
                .addField("Time", time, true)
                .setFooter(EmbedUtils.STANDARD_FOOTER)
                .setTimestamp(Instant.now())
                .setThumbnail("attachment://" + ResourceManager.CONNECTIONS_ICON);
        
        FileUpload icon = ResourceManager.getImageAsFileUpload(ResourceManager.CONNECTIONS_ICON);
        event.getHook().sendMessageEmbeds(embed.build())
                .addFiles(icon)
                .queue();
    }
    
    /**
     * Send a mock airdrop event
     */
    private void sendAirdropEvent(SlashCommandInteractionEvent event) {
        String location = getRandomLocation();
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        
        String[] titles = {
            "Supply Drop Inbound",
            "Airdrop Detected",
            "Supplies From Above",
            "Emergency Provisions"
        };
        
        String[] descriptions = {
            String.format("Military supplies have been dropped at %s", location),
            String.format("An aircraft has delivered vital resources near %s", location),
            String.format("Valuable gear has been airdropped at %s. Rush to claim it!", location),
            String.format("Supply drop confirmed at %s. High-tier loot available.", location)
        };
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getRandomElement(titles))
                .setDescription(getRandomElement(descriptions))
                .setColor(EmbedUtils.STEEL_BLUE)
                .addField("Location", location, true)
                .addField("Status", "Active", true)
                .addField("Time", time, true)
                .setFooter(EmbedUtils.STANDARD_FOOTER)
                .setTimestamp(Instant.now())
                .setThumbnail("attachment://" + ResourceManager.AIRDROP_ICON);
        
        FileUpload icon = ResourceManager.getImageAsFileUpload(ResourceManager.AIRDROP_ICON);
        event.getHook().sendMessageEmbeds(embed.build())
                .addFiles(icon)
                .queue();
    }
    
    /**
     * Send a mock helicrash event
     */
    private void sendHelicrashEvent(SlashCommandInteractionEvent event) {
        String location = getRandomLocation();
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        
        String[] titles = {
            "Helicopter Down",
            "Crash Site Located",
            "Military Helicopter Crashed",
            "Wreckage Spotted"
        };
        
        String[] descriptions = {
            String.format("A military helicopter has crashed near %s", location),
            String.format("Helicopter wreckage detected at %s. Military gear available.", location),
            String.format("A downed chopper has been spotted near %s. Approach with caution.", location),
            String.format("Helicopter crash at %s. Valuable military equipment scattered in the area.", location)
        };
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getRandomElement(titles))
                .setDescription(getRandomElement(descriptions))
                .setColor(EmbedUtils.RUST_ACCENT)
                .addField("Location", location, true)
                .addField("Status", "Active", true)
                .addField("Time", time, true)
                .setFooter(EmbedUtils.STANDARD_FOOTER)
                .setTimestamp(Instant.now())
                .setThumbnail("attachment://" + ResourceManager.HELICRASH_ICON);
        
        FileUpload icon = ResourceManager.getImageAsFileUpload(ResourceManager.HELICRASH_ICON);
        event.getHook().sendMessageEmbeds(embed.build())
                .addFiles(icon)
                .queue();
    }
    
    /**
     * Send a mock mission event
     */
    private void sendMissionEvent(SlashCommandInteractionEvent event) {
        String location = getRandomLocation();
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        String faction = getRandomFaction();
        
        String[] titles = {
            "Mission Activated",
            "High Priority Target",
            "Special Operation",
            "Tactical Objective"
        };
        
        String[] descriptions = {
            String.format("A special mission has begun at %s", location),
            String.format("%s has initiated operations at %s. Proceed with caution.", faction, location),
            String.format("Military forces have established a mission at %s. High rewards await.", location),
            String.format("A high-value target has been spotted near %s. Elimination authorized.", location)
        };
        
        String[] rewards = {
            "High-tier weapons", "Military equipment", "Rare ammunition", "Medical supplies",
            "Faction credentials", "Weapon attachments", "Military clothing"
        };
        
        String selectedRewards = Arrays.stream(getRandomElements(rewards, 3))
                .collect(Collectors.joining(", "));
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getRandomElement(titles))
                .setDescription(getRandomElement(descriptions))
                .setColor(EmbedUtils.STEEL_BLUE)
                .addField("Location", location, true)
                .addField("Faction", faction, true)
                .addField("Time", time, true)
                .addField("Potential Rewards", selectedRewards, false)
                .setFooter(EmbedUtils.STANDARD_FOOTER)
                .setTimestamp(Instant.now())
                .setThumbnail("attachment://" + ResourceManager.MISSION_ICON);
        
        FileUpload icon = ResourceManager.getImageAsFileUpload(ResourceManager.MISSION_ICON);
        event.getHook().sendMessageEmbeds(embed.build())
                .addFiles(icon)
                .queue();
    }
    
    /**
     * Send a mock trader event
     */
    private void sendTraderEvent(SlashCommandInteractionEvent event) {
        String location = getRandomLocation();
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        
        String[] titles = {
            "Trader Spotted",
            "Black Market Active",
            "Trade Opportunity",
            "Wandering Merchant"
        };
        
        String[] descriptions = {
            String.format("A trader has set up shop at %s", location),
            String.format("Rare items are available for purchase at %s", location),
            String.format("A merchant caravan is passing through %s. Limited time offers available.", location),
            String.format("Black market trader spotted at %s. Special deals available.", location)
        };
        
        String[] specialItems = {
            "Weapon modifications", "Rare ammunition", "Military-grade medicine", 
            "Base building materials", "Rare weapon skins", "Special equipment"
        };
        
        String selectedItems = Arrays.stream(getRandomElements(specialItems, 3))
                .collect(Collectors.joining(", "));
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getRandomElement(titles))
                .setDescription(getRandomElement(descriptions))
                .setColor(new Color(173, 137, 39)) // Gold color for trader
                .addField("Location", location, true)
                .addField("Status", "Active", true)
                .addField("Time", time, true)
                .addField("Notable Items", selectedItems, false)
                .setFooter(EmbedUtils.STANDARD_FOOTER)
                .setTimestamp(Instant.now())
                .setThumbnail("attachment://" + ResourceManager.TRADER_ICON);
        
        FileUpload icon = ResourceManager.getImageAsFileUpload(ResourceManager.TRADER_ICON);
        event.getHook().sendMessageEmbeds(embed.build())
                .addFiles(icon)
                .queue();
    }
    
    /**
     * Send a mock bounty event
     */
    private void sendBountyEvent(SlashCommandInteractionEvent event) {
        String target = getRandomPlayer();
        String issuer = getRandomPlayer();
        while (issuer.equals(target)) {
            issuer = getRandomPlayer(); // Ensure different players
        }
        
        int amount = (random.nextInt(10) + 1) * 1000;
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        
        String[] titles = {
            "Bounty Issued",
            "Contract Open",
            "Target Marked",
            "Hunt Authorized"
        };
        
        String[] descriptions = {
            String.format("A bounty of %d coins has been placed on %s's head", amount, target),
            String.format("%s has been marked for elimination. %d coin reward awaits.", target, amount),
            String.format("%s wants %s dead or alive. %d coin bounty posted.", issuer, target, amount),
            String.format("The hunt is on. %s must be eliminated for a %d coin reward.", target, amount)
        };
        
        String[] reasons = {
            "Unprovoked attack", "Theft of supplies", "Betrayal", "Territory dispute",
            "Personal vendetta", "Resource competition", "Faction conflict"
        };
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getRandomElement(titles))
                .setDescription(getRandomElement(descriptions))
                .setColor(EmbedUtils.RUST_ACCENT)
                .addField("Target", target, true)
                .addField("Reward", amount + " coins", true)
                .addField("Issued By", issuer, true)
                .addField("Reason", getRandomElement(reasons), false)
                .setFooter(EmbedUtils.STANDARD_FOOTER)
                .setTimestamp(Instant.now())
                .setThumbnail("attachment://" + ResourceManager.BOUNTY_ICON);
        
        FileUpload icon = ResourceManager.getImageAsFileUpload(ResourceManager.BOUNTY_ICON);
        event.getHook().sendMessageEmbeds(embed.build())
                .addFiles(icon)
                .queue();
    }
    
    /**
     * Helper method to get a random player name
     */
    private String getRandomPlayer() {
        return getRandomElement(PLAYER_NAMES);
    }
    
    /**
     * Helper method to get a random weapon
     */
    private String getRandomWeapon() {
        return getRandomElement(WEAPONS);
    }
    
    /**
     * Helper method to get a random location
     */
    private String getRandomLocation() {
        return getRandomElement(LOCATIONS);
    }
    
    /**
     * Send a mock suicide event using the actual embed generator
     */
    private void sendSuicideEvent(SlashCommandInteractionEvent event) {
        String player = "cuhslice"; // Use the example name from screenshot
        
        // Use our actual embed generator with the same data
        MessageEmbed embed = EmbedUtils.suicideEmbed(player, "Menu Suicide");
        
        // Add the file attachment for the thumbnail
        FileUpload icon = ResourceManager.getImageAsFileUpload(ResourceManager.KILLFEED_LOGO);
        event.getHook().sendMessageEmbeds(embed)
                .addFiles(icon)
                .queue();
    }
    
    /**
     * Send a mock falling death event using the actual embed generator
     */
    private void sendFallingDeathEvent(SlashCommandInteractionEvent event) {
        String player = "Jolina"; // Use the example name from screenshot
        int height = 30; // Example height in meters
        
        // Use our actual embed generator with the same data
        MessageEmbed embed = EmbedUtils.fallingDeathEmbed(player, height);
        
        // Add the file attachment for the thumbnail
        FileUpload icon = ResourceManager.getImageAsFileUpload(ResourceManager.KILLFEED_LOGO);
        event.getHook().sendMessageEmbeds(embed)
                .addFiles(icon)
                .queue();
    }
    
    /**
     * Helper method to get a random faction
     */
    private String getRandomFaction() {
        return getRandomElement(FACTIONS);
    }
    
    /**
     * Helper method to get a random element from an array
     */
    private <T> T getRandomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }
    
    /**
     * Helper method to get n random elements from an array
     */
    private <T> T[] getRandomElements(T[] array, int n) {
        if (n >= array.length) {
            return array;
        }
        
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            indices.add(i);
        }
        
        // Shuffle the indices and take the first n
        java.util.Collections.shuffle(indices, random);
        indices = indices.subList(0, n);
        
        @SuppressWarnings("unchecked")
        T[] result = (T[]) java.lang.reflect.Array.newInstance(
                array.getClass().getComponentType(), n);
        
        for (int i = 0; i < n; i++) {
            result[i] = array[indices.get(i)];
        }
        
        return result;
    }
}