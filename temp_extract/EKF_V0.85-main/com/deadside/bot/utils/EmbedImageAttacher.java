package com.deadside.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for attaching images to embeds with proper thumbnails
 */
public class EmbedImageAttacher {
    private static final Logger logger = LoggerFactory.getLogger(EmbedImageAttacher.class);
    
    /**
     * Get the logo file for a specific type
     * @param logoFileName The name of the logo file
     * @return A FileUpload object for the logo or null if not found
     */
    public static FileUpload getLogoFileUpload(String logoFileName) {
        try {
            // Try loading from classpath first
            InputStream logoStream = EmbedImageAttacher.class.getClassLoader().getResourceAsStream(
                    "com/deadside/bot/resources/images/" + logoFileName);
            
            if (logoStream != null) {
                return FileUpload.fromData(logoStream, logoFileName);
            }
            
            // Try loading from attached_assets directory
            File logoFile = new File("./attached_assets/" + logoFileName);
            if (logoFile.exists()) {
                return FileUpload.fromData(logoFile, logoFileName);
            }
            
            // Try loading from root directory
            logoFile = new File("./" + logoFileName);
            if (logoFile.exists()) {
                return FileUpload.fromData(logoFile, logoFileName);
            }
            
            logger.warn("Logo file not found: {}", logoFileName);
            return null;
        } catch (Exception e) {
            logger.error("Error getting logo file: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Create an embed with attached thumbnail
     * @param embed The embed builder to use
     * @param logoFileName The name of the logo file
     * @return The embed with thumbnail and a list of file uploads
     */
    public static EmbedWithAttachments createEmbedWithThumbnail(EmbedBuilder embed, String logoFileName) {
        List<FileUpload> uploads = new ArrayList<>();
        
        // Try to get the logo file
        FileUpload logoUpload = getLogoFileUpload(logoFileName);
        if (logoUpload != null) {
            uploads.add(logoUpload);
            embed.setThumbnail("attachment://" + logoFileName);
        }
        
        return new EmbedWithAttachments(embed.build(), uploads);
    }
    
    /**
     * Class to hold an embed and its attachments
     */
    public static class EmbedWithAttachments {
        private final MessageEmbed embed;
        private final List<FileUpload> attachments;
        
        public EmbedWithAttachments(MessageEmbed embed, List<FileUpload> attachments) {
            this.embed = embed;
            this.attachments = attachments;
        }
        
        public MessageEmbed getEmbed() {
            return embed;
        }
        
        public List<FileUpload> getAttachments() {
            return attachments;
        }
    }
}