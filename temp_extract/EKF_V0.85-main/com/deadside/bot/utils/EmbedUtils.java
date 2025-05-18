package com.deadside.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.Instant;

/**
 * Utility class for creating Discord message embeds
 * Uses Deadside-themed emerald color palette for consistency
 */
public class EmbedUtils {
    // Deadside themed color palette
    public static final Color DEADSIDE_PRIMARY = new Color(39, 174, 96);      // Primary emerald green
    public static final Color DEADSIDE_SECONDARY = new Color(46, 204, 113);   // Secondary emerald green
    public static final Color DEADSIDE_DARK = new Color(24, 106, 59);         // Darker emerald shade
    public static final Color DEADSIDE_LIGHT = new Color(88, 214, 141);       // Lighter emerald shade
    public static final Color DEADSIDE_ACCENT = new Color(26, 188, 156);      // Accent turquoise
    
    // Standard colors for different embed types (using Deadside palette)
    private static final Color SUCCESS_COLOR = DEADSIDE_SECONDARY;            // Success green
    private static final Color ERROR_COLOR = new Color(231, 76, 60);          // Error red
    private static final Color INFO_COLOR = DEADSIDE_PRIMARY;                 // Info emerald
    private static final Color WARNING_COLOR = new Color(243, 156, 18);       // Warning orange
    
    /**
     * Create a success embed
     */
    public static MessageEmbed successEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ " + title)
                .setDescription(description)
                .setColor(SUCCESS_COLOR)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
        
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.MAIN_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create an error embed
     */
    public static MessageEmbed errorEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ " + title)
                .setDescription(description)
                .setColor(ERROR_COLOR)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
        
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.MAIN_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create an info embed
     */
    public static MessageEmbed infoEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("ℹ️ " + title)
                .setDescription(description)
                .setColor(INFO_COLOR)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
        
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.MAIN_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create a warning embed
     */
    public static MessageEmbed warningEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⚠️ " + title)
                .setDescription(description)
                .setColor(WARNING_COLOR)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
        
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.MAIN_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create a custom colored embed
     */
    public static MessageEmbed customEmbed(String title, String description, Color color) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create a custom colored embed with thumbnail
     */
    public static MessageEmbed customEmbedWithThumbnail(String title, String description, 
                                                       Color color, String thumbnailUrl) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setThumbnail(thumbnailUrl)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create a player stats embed
     */
    public static EmbedBuilder playerStatsEmbed(String playerName) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 Stats for " + playerName)
                .setColor(DEADSIDE_ACCENT) // Accent color for stats
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
                
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.WEAPON_STATS_LOGO));
        
        return embed;
    }
    
    /**
     * Create a killfeed embed
     */
    public static MessageEmbed killfeedEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(new Color(204, 0, 0)) // Dark red for killfeed (kept for visibility)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
                
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.KILLFEED_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create a faction embed
     */
    public static MessageEmbed factionEmbed(String title, String description, Color color) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛡️ " + title)
                .setDescription(description)
                .setColor(color != null ? color : DEADSIDE_PRIMARY)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
                
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.FACTION_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create an economy embed
     */
    public static MessageEmbed economyEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("💰 " + title)
                .setDescription(description)
                .setColor(DEADSIDE_SECONDARY)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
                
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.TRADER_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create a premium feature embed
     */
    public static MessageEmbed premiumEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✨ " + title)
                .setDescription(description)
                .setColor(DEADSIDE_ACCENT)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
                
        // Add logo thumbnail  
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.MAIN_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create a server event embed
     */
    public static MessageEmbed eventEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔔 " + title)
                .setDescription(description)
                .setColor(DEADSIDE_DARK)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
                
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.HELICRASH_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create a bounty embed
     */
    public static MessageEmbed bountyEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏆 " + title)
                .setDescription(description)
                .setColor(DEADSIDE_SECONDARY)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
                
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.BOUNTY_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create an airdrop embed
     */
    public static MessageEmbed airdropEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📦 " + title)
                .setDescription(description)
                .setColor(DEADSIDE_LIGHT)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
                
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.AIRDROP_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create a mission embed
     */
    public static MessageEmbed missionEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎯 " + title)
                .setDescription(description)
                .setColor(new Color(204, 51, 51)) // Red for missions
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
                
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.MISSION_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create a connections embed
     */
    public static MessageEmbed connectionsEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔗 " + title)
                .setDescription(description)
                .setColor(DEADSIDE_ACCENT)
                .setFooter("Powered by Discord.gg/EmeraldServers", null)
                .setTimestamp(Instant.now());
                
        // Add logo thumbnail
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.CONNECTIONS_LOGO));
        
        return embed.build();
    }
    
    /**
     * Create a standard PvP killfeed embed with themed messaging
     *
     * @param killer The name of the player who made the kill
     * @param victim The name of the player who was killed
     * @param weapon The weapon used in the kill
     * @param distance The distance of the kill in meters
     * @return A styled killfeed embed following Phase 7 theming guidelines
     */
    public static MessageEmbed pvpKillfeedEmbed(String killer, String victim, String weapon, int distance) {
        // Dynamic titles from Phase 7 requirements
        String[] titles = {
            "ELIMINATION CONFIRMED",
            "NO SURVIVORS", 
            "PRECISION ELIMINATED",
            "DEADSIDE CASUALTY"
        };
        
        // Dynamic descriptions for killfeed from Phase 7
        String[] messages = {
            String.format("Clean headshot at %dm.", distance),
            String.format("%s never saw it coming from %dm away.", victim, distance),
            String.format("One %s round was all it took.", weapon)
        };
        
        String[] contextMessages = {
            "The wasteland claims another victim.",
            "Another survivor's journey ends.",
            "Clean shot. No time to react."
        };
        
        // Random selection for variety
        int titleIndex = (int)(Math.random() * titles.length);
        int msgIndex = (int)(Math.random() * messages.length);
        int contextIndex = (int)(Math.random() * contextMessages.length);
        
        // Server information
        String serverName = "Emerald EU"; // Default server name
        String time = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        // Create embed with the correct styling per design guidelines
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(titles[titleIndex])
                .setDescription(killer + "\neliminated " + victim + " with " + weapon)
                .setColor(new Color(25, 25, 25)) // Very dark gray background
                .addField("", messages[msgIndex], false)
                .addField("", contextMessages[contextIndex], false)
                .setFooter("Server: " + serverName + " | discord.gg/EmeraldServers | " + time).setTimestamp(Instant.now())
                .setTimestamp(Instant.now());
                
        // Add thumbnail logo
        embed.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.KILLFEED_LOGO));
                
        return embed.build();
    }
    
    /**
     * Create a suicide embed (including menu suicide) with themed messaging
     *
     * @param victim The name of the player who committed suicide
     * @param cause The cause of death (e.g., "Menu Suicide")
     * @return A styled suicide embed following Phase 7 theming guidelines
     */
    public static MessageEmbed suicideEmbed(String victim, String cause) {
        // Normalize "Suicide_by_relocation" to "Menu Suicide" as per Phase 5 requirements
        if (cause.equalsIgnoreCase("Suicide_by_relocation")) {
            cause = "Menu Suicide";
        }
        
        // Dynamic titles from Phase 7 requirements
        String[] titles = {
            "SELF-TERMINATION LOGGED",
            "SILENT EXIT",
            "COMBAT RESOLUTION"
        };
        
        // Dynamic descriptions based on cause
        String[] descriptions;
        String[] messages;
        String[] contextMessages;
        
        if (cause.equalsIgnoreCase("Menu Suicide")) {
            descriptions = new String[] {
                "found the ultimate exit strategy",
                "chose a permanent solution",
                "couldn't handle the wasteland anymore"
            };
            
            messages = new String[] {
                "Menu navigation claimed another victim.",
                "Sometimes the biggest threat is yourself.",
                "They chose the easy way out."
            };
        } else {
            descriptions = new String[] {
                "chose to end their journey",
                "returned to the void",
                "decided their time was up"
            };
            
            messages = new String[] {
                "Self-elimination confirmed.",
                "Sometimes the biggest threat is yourself.",
                "Another soul lost to their own demons."
            };
        }
        
        contextMessages = new String[] {
            "In the wasteland, your worst enemy is often yourself.",
            "The mind breaks before the body in this harsh world.",
            "No rescue. No respawn. Just the void."
        };
        
        // Random selection for variety
        int titleIndex = (int)(Math.random() * titles.length);
        int descIndex = (int)(Math.random() * descriptions.length);
        int msgIndex = (int)(Math.random() * messages.length);
        int contextIndex = (int)(Math.random() * contextMessages.length);
        
        // Server information
        String serverName = "Emerald EU"; // Default server name
        String time = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        // Create embed with orange sidebar styling as per screenshot example
        return new EmbedBuilder()
                .setTitle(titles[titleIndex])
                .setDescription(victim + "\n" + descriptions[descIndex])
                .setColor(new Color(25, 25, 25)) // Very dark gray background
                .addField("", messages[msgIndex], false)
                .addField("", contextMessages[contextIndex], false)
                .setFooter("Server: " + serverName + " | discord.gg/EmeraldServers | " + time).setTimestamp(Instant.now())
                .setThumbnail(ResourceManager.getAttachmentString(ResourceManager.KILLFEED_LOGO))
                .build();
    }
    
    /**
     * Create a falling death embed with themed messaging
     *
     * @param victim The name of the player who died from falling
     * @param height The height of the fall (optional, can be 0 if unknown)
     * @return A styled falling death embed following Phase 7 theming guidelines
     */
    public static MessageEmbed fallingDeathEmbed(String victim, int height) {
        // Dynamic titles from Phase 7 requirements
        String[] titles = {
            "TERMINAL VELOCITY",
            "GRAVITY CLAIMED ANOTHER",
            "DEADLY ENCOUNTER"
        };
        
        // Dynamic descriptions
        String[] descriptions = {
            "took the easy way out",
            "fell from a great height",
            "discovered gravity's unforgiving nature"
        };
        
        // Dynamic messages
        String[] messages = {
            "Gravity: 1, Player: 0.",
            "They didn't fall. They descended.",
            "The ground doesn't negotiate."
        };
        
        String[] contextMessages = {
            "Sometimes the most dangerous weapon is the one in your hands.",
            "The wasteland claims victims in many ways.",
            "No safety nets in Deadside."
        };
        
        // Add height context if available
        if (height > 0) {
            messages = new String[] {
                String.format("Gravity: 1, Player: 0. (Fell %dm)", height),
                String.format("They didn't fall. They descended %dm.", height),
                String.format("The %dm drop proved fatal.", height)
            };
        }
        
        // Random selection for variety
        int titleIndex = (int)(Math.random() * titles.length);
        int descIndex = (int)(Math.random() * descriptions.length);
        int msgIndex = (int)(Math.random() * messages.length);
        int contextIndex = (int)(Math.random() * contextMessages.length);
        
        // Server information
        String serverName = "Emerald EU"; // Default server name 
        String time = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        // Create embed with orange sidebar styling as per screenshot example
        return new EmbedBuilder()
                .setTitle(titles[titleIndex])
                .setDescription(victim + "\n" + descriptions[descIndex])
                .setColor(new Color(25, 25, 25)) // Very dark gray background
                .addField("", messages[msgIndex], false)
                .addField("", contextMessages[contextIndex], false)
                .setFooter("Server: " + serverName + " | discord.gg/EmeraldServers | " + time).setTimestamp(Instant.now())
                .setThumbnail(ResourceManager.getAttachmentString(ResourceManager.KILLFEED_LOGO))
                .build();
    }
}