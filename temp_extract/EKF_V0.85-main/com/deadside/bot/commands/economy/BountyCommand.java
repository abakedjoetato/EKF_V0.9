package com.deadside.bot.commands.economy;

import com.deadside.bot.db.models.Bounty;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.GuildConfig;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.BountyRepository;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.GuildConfigRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Command for managing bounties
 */
public class BountyCommand {
    private static final Logger logger = LoggerFactory.getLogger(BountyCommand.class);
    // Default values for bounty limits (used if custom settings aren't set)
    private static final int DEFAULT_MIN_BOUNTY = 100;
    private static final int DEFAULT_MAX_BOUNTY = 50000;
    
    // Setting keys for guild configuration
    private static final String MIN_BOUNTY_KEY = "bounty.min.amount";
    private static final String MAX_BOUNTY_KEY = "bounty.max.amount";
    
    // Emerald color for bounty notifications
    private static final Color EMERALD_COLOR = new Color(46, 204, 113);
    
    public static CommandData getCommandData() {
        return Commands.slash("bounty", "Place or view bounties on players")
                .addSubcommands(
                        new SubcommandData("place", "Place a bounty on a player")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "playername", "The name of the player to place a bounty on", true),
                                        new OptionData(OptionType.INTEGER, "amount", "Amount of currency to offer as bounty (min 100, max 50000)", true)
                                ),
                        new SubcommandData("list", "List all active bounties")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "server", "Server name (optional)", false)
                                ),
                        new SubcommandData("check", "Check bounties on a specific player")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "playername", "Player name to check", true)
                                )
                );
    }
    
    public static void execute(SlashCommandInteractionEvent event) {
        // Defer reply since all commands involve database operations
        event.deferReply().queue();
        
        try {
            String subcommand = event.getSubcommandName();
            if (subcommand == null) {
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                        "Error",
                        "Please specify a subcommand."
                )).queue();
                return;
            }
            
            long guildId = event.getGuild().getIdLong();
            long userId = event.getUser().getIdLong();
            
            switch (subcommand) {
                case "place":
                    handlePlaceBounty(event, guildId, userId);
                    break;
                case "list":
                    handleListBounties(event, guildId);
                    break;
                case "check":
                    handleCheckPlayerBounties(event, guildId);
                    break;
                default:
                    event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                            "Unknown Subcommand",
                            "That subcommand is not recognized."
                    )).queue();
            }
        } catch (Exception e) {
            logger.error("Error executing bounty command", e);
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                    "Error",
                    "An error occurred: " + e.getMessage()
            )).queue();
        }
    }
    
    /**
     * Handle the place subcommand
     */
    private static void handlePlaceBounty(SlashCommandInteractionEvent event, long guildId, long userId) {
        // Get guild-specific bounty limits
        GuildConfigRepository guildConfigRepo = new GuildConfigRepository();
        GuildConfig guildConfig = guildConfigRepo.findByGuildId(guildId);

        // Get min and max bounty limits (using defaults if not set)
        int minBounty = DEFAULT_MIN_BOUNTY;
        int maxBounty = DEFAULT_MAX_BOUNTY;
        
        if (guildConfig != null) {
            try {
                String minStr = guildConfig.getSetting(MIN_BOUNTY_KEY, String.valueOf(DEFAULT_MIN_BOUNTY));
                minBounty = Integer.parseInt(minStr);
                
                String maxStr = guildConfig.getSetting(MAX_BOUNTY_KEY, String.valueOf(DEFAULT_MAX_BOUNTY));
                maxBounty = Integer.parseInt(maxStr);
            } catch (NumberFormatException e) {
                logger.warn("Error parsing bounty limits for guild {}, using defaults", guildId, e);
            }
        }
        
        String targetName = event.getOption("playername", OptionMapping::getAsString);
        int amount = event.getOption("amount", 0, OptionMapping::getAsInt);
        
        if (targetName == null || amount < minBounty) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                    "Invalid Parameters",
                    String.format("Please provide a player name and an amount between %d and %d coins.",
                            minBounty, maxBounty)
            )).queue();
            return;
        }
        
        // Cap bounty amount
        if (amount > maxBounty) {
            amount = maxBounty;
        }
        
        // Check if player has enough coins
        PlayerRepository playerRepo = new PlayerRepository();
        Player player = playerRepo.findByDiscordId(userId, guildId);
        
        if (player == null || player.getCoins() < amount) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                    "Insufficient Funds",
                    "You don't have enough coins to place this bounty."
            )).queue();
            return;
        }
        
        // Check if the target exists in the database
        if (playerRepo.findByName(targetName) == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                    "Unknown Player",
                    "That player wasn't found in our database. They need to have played on the server at least once."
            )).queue();
            return;
        }
        
        // Create and save the bounty
        BountyRepository bountyRepo = new BountyRepository();
        Bounty bounty = new Bounty(targetName, amount, guildId, userId);
        bountyRepo.save(bounty);
        
        // Deduct coins from player
        playerRepo.removeCoins(userId, guildId, amount);
        
        // Send confirmation embed
        event.getHook().sendMessageEmbeds(EmbedUtils.customEmbed(
                "🏆 Bounty Placed",
                String.format("You have placed a **%d coin** bounty on **%s**.\n\n" +
                                "This bounty will expire in 24 hours if not claimed.\n" +
                                "You cannot cancel a bounty once placed.",
                        amount, targetName),
                EMERALD_COLOR
        )).queue();
        
        // Announce the bounty in the bounty channel, if configured
        announceNewBounty(event.getGuild(), targetName, amount, userId, bounty);
    }
    
    /**
     * Announce a new bounty in the bounty channel
     */
    private static void announceNewBounty(Guild guild, String targetName, long amount, long issuerId, Bounty bounty) {
        try {
            // Get server configurations for this guild
            GameServerRepository gameServerRepo = new GameServerRepository();
            List<GameServer> servers = gameServerRepo.findByGuildId(guild.getIdLong());
            
            if (servers.isEmpty()) {
                logger.warn("No servers configured for guild {}", guild.getId());
                return;
            }
            
            // Find a server with a bounty channel
            GameServer server = null;
            for (GameServer s : servers) {
                if (s.getBountyChannelId() != 0) {
                    server = s;
                    break;
                }
            }
            
            if (server == null) {
                logger.warn("No bounty channel configured for guild {}", guild.getId());
                return;
            }
            
            TextChannel bountyChannel = guild.getTextChannelById(server.getBountyChannelId());
            if (bountyChannel == null) {
                logger.warn("Bounty channel not found for server {}", server.getName());
                return;
            }
            
            // Send announcement to bounty channel
            bountyChannel.sendMessageEmbeds(EmbedUtils.customEmbed(
                    "🏆 New Bounty Contract",
                    String.format("**Target:** %s\n" +
                                    "**Reward:** %d coins\n" +
                                    "**Posted by:** <@%d>\n" +
                                    "**Expires in:** %s\n\n" +
                                    "*Hunt them down to claim your reward!*",
                            targetName, amount, issuerId, bounty.getTimeLeftFormatted()),
                    EMERALD_COLOR
            )).queue();
            
        } catch (Exception e) {
            logger.error("Error announcing new bounty", e);
        }
    }
    
    /**
     * Handle the list subcommand
     */
    private static void handleListBounties(SlashCommandInteractionEvent event, long guildId) {
        String serverName = event.getOption("server", OptionMapping::getAsString);
        
        BountyRepository bountyRepo = new BountyRepository();
        List<Bounty> bounties = bountyRepo.findActiveByGuildId(guildId);
        
        if (bounties.isEmpty()) {
            event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed(
                    "Bounty Board Empty",
                    "There are no active bounties at this time."
            )).queue();
            return;
        }
        
        // Filter by server if provided
        if (serverName != null && !serverName.isEmpty()) {
            GameServerRepository gameServerRepo = new GameServerRepository();
            List<GameServer> servers = gameServerRepo.findByName(serverName);
            
            if (servers.isEmpty() || servers.stream().noneMatch(s -> s.getGuildId() == guildId)) {
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                        "Server Not Found",
                        "No game server with that name was found in this Discord server."
                )).queue();
                return;
            }
            
            // We found the server, continue with all bounties (no way to filter by server yet)
        }
        
        // Sort bounties by amount (highest first)
        bounties.sort((b1, b2) -> Long.compare(b2.getAmount(), b1.getAmount()));
        
        // Build the embed
        StringBuilder description = new StringBuilder("Here are all active bounty contracts:\n\n");
        
        for (Bounty bounty : bounties) {
            if (bounty.isAiGenerated()) {
                description.append("• **").append(bounty.getTargetName()).append("** - ")
                        .append(bounty.getAmount()).append(" coins (AI-generated)\n")
                        .append("  Expires in: ").append(bounty.getTimeLeftFormatted()).append("\n\n");
            } else {
                description.append("• **").append(bounty.getTargetName()).append("** - ")
                        .append(bounty.getAmount()).append(" coins (by <@")
                        .append(bounty.getIssuerId()).append(">)\n")
                        .append("  Expires in: ").append(bounty.getTimeLeftFormatted()).append("\n\n");
            }
            
            // To keep the message within Discord's limits
            if (description.length() > 3800) {
                description.append("...and more. Please check the bounty channel for all active bounties.");
                break;
            }
        }
        
        event.getHook().sendMessageEmbeds(EmbedUtils.customEmbed(
                "🏆 Bounty Board",
                description.toString(),
                EMERALD_COLOR
        )).queue();
    }
    
    /**
     * Handle the check subcommand
     */
    private static void handleCheckPlayerBounties(SlashCommandInteractionEvent event, long guildId) {
        String playerName = event.getOption("playername", OptionMapping::getAsString);
        
        if (playerName == null || playerName.isEmpty()) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                    "Missing Player Name",
                    "Please provide a player name to check."
            )).queue();
            return;
        }
        
        BountyRepository bountyRepo = new BountyRepository();
        List<Bounty> bounties = bountyRepo.findActiveByTargetName(playerName);
        
        if (bounties.isEmpty()) {
            event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed(
                    "No Bounties Found",
                    String.format("**%s** doesn't have any active bounties.", playerName)
            )).queue();
            return;
        }
        
        // Filter by guild
        List<Bounty> guildBounties = new ArrayList<>();
        for (Bounty bounty : bounties) {
            if (bounty.getGuildId() == guildId) {
                guildBounties.add(bounty);
            }
        }
        
        if (guildBounties.isEmpty()) {
            event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed(
                    "No Bounties Found",
                    String.format("**%s** doesn't have any active bounties in this Discord server.", playerName)
            )).queue();
            return;
        }
        
        // Sort bounties by amount (highest first)
        guildBounties.sort((b1, b2) -> Long.compare(b2.getAmount(), b1.getAmount()));
        
        // Calculate total bounty amount
        long totalAmount = 0;
        for (Bounty bounty : guildBounties) {
            totalAmount += bounty.getAmount();
        }
        
        // Build the embed
        StringBuilder description = new StringBuilder();
        description.append(String.format("**%s** has **%d** active bounties totaling **%d coins**:\n\n", 
                playerName, guildBounties.size(), totalAmount));
        
        for (Bounty bounty : guildBounties) {
            if (bounty.isAiGenerated()) {
                description.append("• **").append(bounty.getAmount()).append(" coins** (AI-generated)\n")
                        .append("  Expires in: ").append(bounty.getTimeLeftFormatted()).append("\n\n");
            } else {
                description.append("• **").append(bounty.getAmount()).append(" coins** (by <@")
                        .append(bounty.getIssuerId()).append(">)\n")
                        .append("  Expires in: ").append(bounty.getTimeLeftFormatted()).append("\n\n");
            }
        }
        
        description.append("*Eliminate them in-game to claim these bounties!*");
        
        event.getHook().sendMessageEmbeds(EmbedUtils.customEmbed(
                "🏆 Bounty Check",
                description.toString(),
                EMERALD_COLOR
        )).queue();
    }
}