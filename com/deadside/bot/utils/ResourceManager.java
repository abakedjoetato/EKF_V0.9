package com.deadside.bot.utils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * Utility class for managing access to bot resources like images
 */
public class ResourceManager {
    // Base path for resources
    private static final String IMAGES_PATH = "com/deadside/bot/resources/images/";
    
    // Image file names
    public static final String MAIN_LOGO = "Mainlogo.png";
    public static final String KILLFEED_LOGO = "Killfeed.png";
    public static final String BOUNTY_LOGO = "Bounty.png";
    public static final String FACTION_LOGO = "Faction.png";
    public static final String TRADER_LOGO = "Trader.png";
    public static final String AIRDROP_LOGO = "Airdrop.png";
    public static final String HELICRASH_LOGO = "Helicrash.png";
    public static final String MISSION_LOGO = "Mission.png";
    public static final String WEAPON_STATS_LOGO = "WeaponStats.png";
    public static final String CONNECTIONS_LOGO = "Connections.png";
    
    /**
     * Get the URL for a resource image
     */
    public static String getImageUrl(String imageName) {
        try {
            // First try accessing from resources (for JAR deployment)
            URL url = ResourceManager.class.getClassLoader().getResource(IMAGES_PATH + imageName);
            
            if (url != null) {
                return url.toString();
            }
            
            // Fallback to file system (for development)
            File file = new File("./" + IMAGES_PATH + imageName);
            if (file.exists()) {
                return file.toURI().toString();
            }
            
            // Default fallback
            return "https://i.imgur.com/6vYRsJ8.png";
        } catch (Exception e) {
            // Fallback to placeholder
            return "https://i.imgur.com/6vYRsJ8.png";
        }
    }
    
    /**
     * Get the main logo URL
     */
    public static String getMainLogoUrl() {
        return getImageUrl(MAIN_LOGO);
    }
    
    /**
     * Get the killfeed logo URL
     */
    public static String getKillfeedLogoUrl() {
        return getImageUrl(KILLFEED_LOGO);
    }
    
    /**
     * Get the bounty logo URL
     */
    public static String getBountyLogoUrl() {
        return getImageUrl(BOUNTY_LOGO);
    }
    
    /**
     * Get the faction logo URL
     */
    public static String getFactionLogoUrl() {
        return getImageUrl(FACTION_LOGO);
    }
    
    /**
     * Get the trader logo URL
     */
    public static String getTraderLogoUrl() {
        return getImageUrl(TRADER_LOGO);
    }
    
    /**
     * Get the airdrop logo URL
     */
    public static String getAirdropLogoUrl() {
        return getImageUrl(AIRDROP_LOGO);
    }
    
    /**
     * Get the helicrash logo URL
     */
    public static String getHelicrashLogoUrl() {
        return getImageUrl(HELICRASH_LOGO);
    }
    
    /**
     * Get the mission logo URL
     */
    public static String getMissionLogoUrl() {
        return getImageUrl(MISSION_LOGO);
    }
    
    /**
     * Get the weapon stats logo URL
     */
    public static String getWeaponStatsLogoUrl() {
        return getImageUrl(WEAPON_STATS_LOGO);
    }
    
    /**
     * Get the connections logo URL
     */
    public static String getConnectionsLogoUrl() {
        return getImageUrl(CONNECTIONS_LOGO);
    }
    
    /**
     * Get the attachment string for an image
     * Format: attachment://filename.png
     * This is the proper format for Discord embed thumbnails
     * 
     * @param filename The image filename
     * @return The formatted attachment string
     */
    public static String getAttachmentString(String filename) {
        return "attachment://" + filename;
    }
    
    /**
     * Get input stream for a resource image
     * This is used to attach images to embed messages
     * 
     * @param imageName The image filename
     * @return InputStream for the image or null if not found
     */
    public static InputStream getResourceAsStream(String imageName) {
        try {
            // Try from classpath resources first (for JAR deployment)
            InputStream stream = ResourceManager.class.getClassLoader().getResourceAsStream(IMAGES_PATH + imageName);
            
            if (stream != null) {
                return stream;
            }
            
            // Try from resources directory
            File file = new File("./" + IMAGES_PATH + imageName);
            if (file.exists()) {
                return java.nio.file.Files.newInputStream(file.toPath());
            }
            
            // Try from attached_assets directory as a last resort
            file = new File("./attached_assets/" + imageName);
            if (file.exists()) {
                return java.nio.file.Files.newInputStream(file.toPath());
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}