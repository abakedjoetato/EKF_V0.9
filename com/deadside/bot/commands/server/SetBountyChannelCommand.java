package com.deadside.bot.commands.server;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Command to set the bounty channel for a server
 */
public class SetBountyChannelCommand {
    private static final Logger logger = LoggerFactory.getLogger(SetBountyChannelCommand.class);
    
    public static CommandData getCommandData() {
        return Commands.slash("setbountychannel", "Set the channel where bounty notifications will be sent")
                .addOptions(
                        new OptionData(OptionType.STRING, "server", "The name of the server", true),
                        new OptionData(OptionType.CHANNEL, "channel", "The channel to use for bounty notifications", true)
                )
                .setDefaultPermissions(Permission.MANAGE_SERVER);
    }
    
    public static void execute(SlashCommandInteractionEvent event) {
        // Use deferReply to avoid interaction timeout during DB operations
        event.deferReply().queue();
        
        try {
            String serverName = event.getOption("server", OptionMapping::getAsString);
            TextChannel channel = event.getOption("channel", OptionMapping::getAsChannel).asTextChannel();
            long guildId = event.getGuild().getIdLong();
            
            if (serverName == null || channel == null) {
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                        "Missing Required Arguments",
                        "Both server name and channel must be provided."
                )).queue();
                return;
            }
            
            GameServerRepository repository = new GameServerRepository();
            List<GameServer> servers = repository.findByName(serverName);
            
            if (servers.isEmpty()) {
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                        "Server Not Found",
                        "No game server with that name was found."
                )).queue();
                return;
            }
            
            // Filter by guild ID
            GameServer server = null;
            for (GameServer s : servers) {
                if (s.getGuildId() == guildId) {
                    server = s;
                    break;
                }
            }
            
            if (server == null) {
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                        "Server Not Found",
                        "No game server with that name was found in this Discord server."
                )).queue();
                return;
            }
            
            // Update server with new bounty channel
            server.setBountyChannelId(channel.getIdLong());
            repository.update(server);
            
            event.getHook().sendMessageEmbeds(EmbedUtils.successEmbed(
                    "Bounty Channel Set",
                    String.format("Bounty notifications for server **%s** will now be sent to %s.",
                            server.getName(), channel.getAsMention())
            )).queue();
            
            logger.info("Bounty channel for server {} set to {}", server.getName(), channel.getId());
            
        } catch (Exception e) {
            logger.error("Error setting bounty channel", e);
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                    "Error",
                    "An error occurred while setting the bounty channel: " + e.getMessage()
            )).queue();
        }
    }
}