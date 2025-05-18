package com.deadside.bot.commands.info;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.commands.CommandCategory;
import com.deadside.bot.utils.EmbedUtils;
import com.deadside.bot.utils.ResourceManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;

/**
 * Command to display help information for all available commands
 */
public class HelpCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(HelpCommand.class);
    
    @Override
    public String getName() {
        return "help";
    }
    
    @Override
    public String getDescription() {
        return "Display help information for all available commands";
    }
    
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.INFO;
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription());
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Acknowledge the command immediately
        event.deferReply().queue();
        
        try {
            // Get logo file as input stream for attachment using our ResourceManager
            InputStream logoStream = ResourceManager.getResourceAsStream(ResourceManager.MAIN_LOGO);
            
            if (logoStream != null) {
                // Create FileUpload for the logo
                FileUpload logoUpload = FileUpload.fromData(logoStream, ResourceManager.MAIN_LOGO);
                
                // Create help embed
                EmbedBuilder helpEmbed = createHelpEmbed();
                helpEmbed.setThumbnail("attachment://" + ResourceManager.MAIN_LOGO);
                
                // Add the attachment to the message
                event.getHook().sendMessageEmbeds(helpEmbed.build())
                        .addFiles(logoUpload)
                        .queue();
            } else {
                // Fallback: Send the help without an image
                logger.warn("Could not find logo file for help command: {}", ResourceManager.MAIN_LOGO);
                event.getHook().sendMessageEmbeds(createHelpEmbed().build()).queue();
            }
            
        } catch (Exception e) {
            logger.error("Error displaying help command: {}", e.getMessage(), e);
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                    "Help Error", 
                    "An error occurred while displaying the help information. Please try again later."))
                    .queue();
        }
    }
    
    /**
     * Create a comprehensive help embed
     */
    private EmbedBuilder createHelpEmbed() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Deadside Bot Command Reference");
        embed.setColor(EmbedUtils.DEADSIDE_PRIMARY);
        embed.setTimestamp(Instant.now());
        embed.setFooter("Powered by Discord.gg/EmeraldServers", null);
        
        // Player Commands Section
        embed.addField("__Player Commands__", 
                "`/stats [player]` - View player statistics\n" +
                "`/rank [player]` - Check player ranking\n" +
                "`/leaderboard` - View server leaderboards\n" +
                "`/link [steamid]` - Link your Steam account\n" +
                "`/balance` - Check your in-game currency\n" +
                "`/daily` - Claim daily rewards",
                false);
        
        // Admin Commands Section
        embed.addField("__Admin/Moderator Commands__", 
                "`/server add [name] [host] [path]` - Add a game server\n" +
                "`/server remove [name]` - Remove a game server\n" +
                "`/server list` - List all configured servers\n" +
                "`/server setkillfeed [name] [channel]` - Set killfeed channel\n" +
                "`/server setlogs [name] [channel]` - Set log channel\n" +
                "`/premium add [user] [days]` - Add premium time",
                false);
        
        // Economy/Game Commands Section
        embed.addField("__Game & Economy__", 
                "`/bounty place [player] [amount]` - Place a bounty\n" +
                "`/bounty list` - View active bounties\n" +
                "`/work` - Earn currency by working\n" +
                "`/roulette [amount]` - Play roulette\n" +
                "`/blackjack [amount]` - Play blackjack",
                false);
        
        // Faction Commands Section
        embed.addField("__Faction System__", 
                "`/faction create [name] [tag]` - Create a faction\n" +
                "`/faction join [name]` - Join a faction\n" +
                "`/faction leave` - Leave your faction\n" +
                "`/faction info [name]` - View faction information\n" +
                "`/faction list` - List all factions",
                false);
        
        // Premium Features Section
        embed.addField("__Premium Features__", 
                "• Exclusive discord roles\n" +
                "• Priority server access\n" +
                "• Enhanced stats tracking\n" +
                "• Custom embeds & notifications\n" +
                "• Higher bounty limits",
                false);
        
        // Info Commands
        embed.addField("__Information__", 
                "`/help` - Display this help menu\n" +
                "`/info` - Bot information and status\n" +
                "`/ping` - Check bot response time", 
                false);
        
        return embed;
    }
}