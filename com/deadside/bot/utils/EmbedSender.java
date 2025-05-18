package com.deadside.bot.utils;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;

/**
 * Utility class for sending embeds with attached logos
 */
public class EmbedSender {
    private static final Logger logger = LoggerFactory.getLogger(EmbedSender.class);

    /**
     * Send a reply to a slash command with an embed and attached logo
     * 
     * @param event The slash command event
     * @param embed The embed to send
     * @param ephemeral Whether the reply should be ephemeral
     */
    public static void replyEmbed(SlashCommandInteractionEvent event, MessageEmbed embed, boolean ephemeral) {
        try {
            // Check if the embed has a thumbnail
            String thumbnailUrl = embed.getThumbnail() != null ? embed.getThumbnail().getUrl() : null;
            
            // If there's no thumbnail or it's not an attachment, just send the embed
            if (thumbnailUrl == null || !thumbnailUrl.startsWith("attachment://")) {
                event.replyEmbeds(embed).setEphemeral(ephemeral).queue();
                return;
            }
            
            // Get the attachment filename
            String fileName = thumbnailUrl.substring("attachment://".length());
            
            // Get the logo file as FileUpload
            FileUpload logoUpload = getLogoFileUpload(fileName);
            
            if (logoUpload != null) {
                // Send with attachment
                event.replyEmbeds(embed)
                     .addFiles(logoUpload)
                     .setEphemeral(ephemeral)
                     .queue();
            } else {
                // Fallback to just the embed without attachment
                event.replyEmbeds(embed).setEphemeral(ephemeral).queue();
            }
        } catch (Exception e) {
            logger.error("Error sending embed: {}", e.getMessage(), e);
            
            // Fallback to just the embed
            event.replyEmbeds(embed).setEphemeral(ephemeral).queue();
        }
    }
    
    /**
     * Send an embed via an interaction hook with attached logo
     * 
     * @param hook The interaction hook
     * @param embed The embed to send
     */
    public static void sendEmbed(InteractionHook hook, MessageEmbed embed) {
        try {
            // Check if the embed has a thumbnail
            String thumbnailUrl = embed.getThumbnail() != null ? embed.getThumbnail().getUrl() : null;
            
            // If there's no thumbnail or it's not an attachment, just send the embed
            if (thumbnailUrl == null || !thumbnailUrl.startsWith("attachment://")) {
                hook.sendMessageEmbeds(embed).queue();
                return;
            }
            
            // Get the attachment filename
            String fileName = thumbnailUrl.substring("attachment://".length());
            
            // Get the logo file as FileUpload
            FileUpload logoUpload = getLogoFileUpload(fileName);
            
            if (logoUpload != null) {
                // Send with attachment
                hook.sendMessageEmbeds(embed)
                    .addFiles(logoUpload)
                    .queue();
            } else {
                // Fallback to just the embed without attachment
                hook.sendMessageEmbeds(embed).queue();
            }
        } catch (Exception e) {
            logger.error("Error sending embed: {}", e.getMessage(), e);
            
            // Fallback to just the embed
            hook.sendMessageEmbeds(embed).queue();
        }
    }
    
    /**
     * Get a logo file as a FileUpload
     * 
     * @param fileName The name of the logo file
     * @return A FileUpload object or null if not found
     */
    private static FileUpload getLogoFileUpload(String fileName) {
        try {
            // Use ResourceManager to get the logo stream
            InputStream logoStream = ResourceManager.getResourceAsStream(fileName);
            
            if (logoStream != null) {
                return FileUpload.fromData(logoStream, fileName);
            }
            
            logger.warn("Logo file not found: {}", fileName);
            return null;
        } catch (Exception e) {
            logger.error("Error getting logo file: {}", e.getMessage(), e);
            return null;
        }
    }
}