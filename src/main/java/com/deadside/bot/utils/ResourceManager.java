package com.deadside.bot.utils;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages resources for the Deadside Bot including logos and images
 */
public class ResourceManager {
    // Paths for the resource folders
    private static final String MAIN_RESOURCES_PATH = "src/main/resources";
    private static final String CLASS_RESOURCES_PATH = "com/deadside/bot/resources";
    
    // Image resources for embeds
    private static final String IMAGES_FOLDER = "/images/";
    
    // Image filenames
    public static final String MAIN_LOGO = "Mainlogo.png";
    public static final String KILLFEED_ICON = "Killfeed.png";
    public static final String BOUNTY_ICON = "Bounty.png";
    public static final String MISSION_ICON = "Mission.png";
    public static final String FACTION_ICON = "Faction.png";
    public static final String AIRDROP_ICON = "Airdrop.png";
    public static final String TRADER_ICON = "Trader.png";
    public static final String CONNECTIONS_ICON = "Connections.png";
    public static final String WEAPON_STATS_ICON = "WeaponStats.png";
    public static final String HELICRASH_ICON = "Helicrash.png";
    
    // Cache for FileUpload objects
    private static final Map<String, FileUpload> fileUploadCache = new HashMap<>();
    
    /**
     * Get a FileUpload for an image resource with enhanced support for Discord file attachments
     * @param imageName The name of the image file
     * @return FileUpload object for the image
     */
    public static FileUpload getImageAsFileUpload(String imageName) {
        try {
            // For attachment:// URLs, extract just the filename
            if (imageName.startsWith("attachment://")) {
                imageName = imageName.substring("attachment://".length());
            }
            
            // Create FileUpload from attached_assets directory (highest priority)
            File attachedFile = new File("attached_assets/" + imageName);
            if (attachedFile.exists() && attachedFile.isFile() && attachedFile.canRead()) {
                System.out.println("Found image in attached_assets: " + attachedFile.getAbsolutePath());
                return FileUpload.fromData(attachedFile, imageName);
            }
            
            // Try resources/images folder
            File resourcesFile = new File("src/main/resources/images/" + imageName);
            if (resourcesFile.exists() && resourcesFile.isFile() && resourcesFile.canRead()) {
                System.out.println("Found image in resources: " + resourcesFile.getAbsolutePath());
                return FileUpload.fromData(resourcesFile, imageName);
            }
            
            // Try target/classes/images folder (for compiled resources)
            File targetFile = new File("target/classes/images/" + imageName);
            if (targetFile.exists() && targetFile.isFile() && targetFile.canRead()) {
                System.out.println("Found image in target: " + targetFile.getAbsolutePath());
                return FileUpload.fromData(targetFile, imageName);
            }
            
            // Try absolute paths for other resources directories
            File[] possibleLocations = {
                // Try various common locations
                new File(System.getProperty("user.dir") + "/resources/images/" + imageName),
                new File(System.getProperty("user.dir") + "/images/" + imageName),
                new File(System.getProperty("user.dir") + "/" + imageName),
                new File("resources/images/" + imageName),
                new File("images/" + imageName)
            };
            
            for (File possibleFile : possibleLocations) {
                if (possibleFile.exists() && possibleFile.isFile() && possibleFile.canRead()) {
                    System.out.println("Found image in alternative location: " + possibleFile.getAbsolutePath());
                    return FileUpload.fromData(possibleFile, imageName);
                }
            }
            
            // Use classpath resource loading as last resort
            try (InputStream is = ResourceManager.class.getClassLoader().getResourceAsStream("images/" + imageName)) {
                if (is != null) {
                    // Create temporary file from stream
                    Path tempFile = Files.createTempFile("discord-image-", imageName);
                    Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Created temp file from classpath resource: " + tempFile);
                    return FileUpload.fromData(tempFile.toFile(), imageName);
                }
            }
            
            // If we reach here, log the failure and return null
            System.err.println("FAILED to find image: " + imageName + " - Images will not display correctly");
            return null;
            
        } catch (Exception e) {
            System.err.println("Error loading image resource: " + imageName);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get an array of FileUploads for the given image names
     * @param imageNames The names of the image files
     * @return Array of FileUpload objects
     */
    public static FileUpload[] getImagesAsFileUploads(String... imageNames) {
        FileUpload[] uploads = new FileUpload[imageNames.length];
        for (int i = 0; i < imageNames.length; i++) {
            uploads[i] = getImageAsFileUpload(imageNames[i]);
        }
        return uploads;
    }
    
    /**
     * Get the attachment syntax for a specific image
     * @param imageName The name of the image file
     * @return The attachment syntax string for use in embeds
     */
    public static String getAttachmentString(String imageName) {
        return "attachment://" + imageName;
    }
}