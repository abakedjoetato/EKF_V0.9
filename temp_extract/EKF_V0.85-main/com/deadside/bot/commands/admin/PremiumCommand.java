package com.deadside.bot.commands.admin;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.premium.PremiumManager;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

/**
 * Command for managing premium features and subscriptions
 */
public class PremiumCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(PremiumCommand.class);
    private final PremiumManager premiumManager;
    private static final Color PREMIUM_COLOR = new Color(26, 188, 156); // Emerald green color for premium

    public PremiumCommand() {
        this.premiumManager = new PremiumManager();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Defer reply immediately to prevent timeout
        event.deferReply().queue();
        
        Member member = event.getMember();
        Guild guild = event.getGuild();

        if (guild == null || member == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Error", "This command can only be used in a server."))
                .setEphemeral(true)
                .queue();
            return;
        }

        // Check if user has admin permissions
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Permission Denied", 
                    "You need Administrator permission to manage premium features."))
                .setEphemeral(true)
                .queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Error", "Invalid subcommand."))
                .setEphemeral(true)
                .queue();
            return;
        }

        switch (subcommand) {
            case "status":
                handleStatusSubcommand(event, guild);
                break;

            case "enable":
                handleEnableSubcommand(event, guild);
                break;

            case "disable":
                handleDisableSubcommand(event, guild);
                break;

            case "verify":
                handleVerifySubcommand(event, guild, member);
                break;

            default:
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Error", "Unknown subcommand."))
                    .setEphemeral(true)
                    .queue();
                break;
        }
    }

    /**
     * Handle the /premium status subcommand
     */
    private void handleStatusSubcommand(SlashCommandInteractionEvent event, Guild guild) {
        String statusDetails = premiumManager.getPremiumStatusDetails(guild.getIdLong());
        boolean hasPremium = premiumManager.hasPremium(guild.getIdLong());

        if (hasPremium) {
            event.getHook().sendMessageEmbeds(EmbedUtils.customEmbed(
                    "✨ Premium Status",
                    "This server has **PREMIUM** features enabled!\n\n" + statusDetails + 
                    "\n\nPremium features include:\n" +
                    "• Advanced statistics and leaderboards\n" +
                    "• Faction system\n" +
                    "• Economy features\n" +
                    "• Real-time event notifications\n" +
                    "• Server monitoring and detailed logs",
                    PREMIUM_COLOR
                ))
                .queue();
        } else {
            event.getHook().sendMessageEmbeds(EmbedUtils.customEmbed(
                    "Premium Status",
                    "This server is using the **FREE** tier.\n\n" + statusDetails + 
                    "\n\nOnly the basic killfeed feature is available. Upgrade to premium to unlock:\n" +
                    "• Advanced statistics and leaderboards\n" +
                    "• Faction system\n" +
                    "• Economy features\n" +
                    "• Real-time event notifications\n" +
                    "• Server monitoring and detailed logs\n\n" +
                    "Use `/premium verify` to check payment status or visit our website to purchase premium.",
                    new Color(189, 195, 199) // Light gray for free tier
                ))
                .queue();
        }
    }

    /**
     * Handle the /premium enable subcommand
     */
    private void handleEnableSubcommand(SlashCommandInteractionEvent event, Guild guild) {
        // Only bot owner can manually enable premium
        if (!isOwner(event.getUser().getIdLong())) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Permission Denied", 
                    "Only the bot owner can manually enable premium. Please use `/premium verify` to activate your premium subscription."))
                .setEphemeral(true)
                .queue();
            return;
        }

        OptionMapping durationOption = event.getOption("days");
        int days = durationOption != null ? durationOption.getAsInt() : 0;

        premiumManager.enablePremium(guild.getIdLong(), days);

        String durationText = days > 0 ? "for " + days + " days" : "with no expiration";
        event.getHook().sendMessageEmbeds(EmbedUtils.customEmbed(
                "✨ Premium Enabled",
                "Premium features have been enabled for this server " + durationText + "!\n\n" +
                "All premium features are now available.",
                PREMIUM_COLOR
            ))
            .queue();

        logger.info("Premium manually enabled for guild ID: {} by user ID: {} for {} days", 
                guild.getId(), event.getUser().getId(), days);
    }

    /**
     * Handle the /premium disable subcommand
     */
    private void handleDisableSubcommand(SlashCommandInteractionEvent event, Guild guild) {
        // Only bot owner can manually disable premium
        if (!isOwner(event.getUser().getIdLong())) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Permission Denied", 
                    "Only the bot owner can manually disable premium."))
                .setEphemeral(true)
                .queue();
            return;
        }

        premiumManager.disablePremium(guild.getIdLong());

        event.getHook().sendMessageEmbeds(EmbedUtils.customEmbed(
                "Premium Disabled",
                "Premium features have been disabled for this server.\n\n" +
                "Only the basic killfeed feature is now available.",
                new Color(189, 195, 199) // Light gray
            ))
            .queue();

        logger.info("Premium manually disabled for guild ID: {} by user ID: {}", 
                guild.getId(), event.getUser().getId());
    }

    /**
     * Handle the /premium verify subcommand
     */
    private void handleVerifySubcommand(SlashCommandInteractionEvent event, Guild guild, Member member) {
        event.deferReply().queue(); // This might take a while, so defer the reply

        boolean verified = premiumManager.verifyTip4servPayment(guild.getIdLong(), member.getIdLong());

        if (verified) {
            event.getHook().sendMessageEmbeds(EmbedUtils.customEmbed(
                    "✨ Payment Verified",
                    "Your payment has been verified and premium features are now enabled for this server!\n\n" +
                    "All premium features are now available.",
                    PREMIUM_COLOR
                ))
                .queue();

            logger.info("Premium payment verified for guild ID: {} by user ID: {}", 
                    guild.getId(), member.getId());
        } else {
            event.getHook().sendMessageEmbeds(EmbedUtils.customEmbed(
                    "Payment Verification",
                    "No active payment was found for this server.\n\n" +
                    "If you recently purchased premium, it may take a few minutes to process. " +
                    "If the problem persists, please check you used the correct Discord account during checkout " +
                    "or contact support with your purchase confirmation.",
                    new Color(189, 195, 199) // Light gray
                ))
                .queue();

            logger.info("Premium payment verification failed for guild ID: {} by user ID: {}", 
                    guild.getId(), member.getId());
        }
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("premium", "Manage premium features and subscription")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .addSubcommands(
                new SubcommandData("status", "Check the premium status of this server"),
                new SubcommandData("verify", "Verify a premium payment for this server"),
                new SubcommandData("enable", "Enable premium features for this server (Bot Owner Only)")
                    .addOption(OptionType.INTEGER, "days", "Duration in days (0 for unlimited)", false),
                new SubcommandData("disable", "Disable premium features for this server (Bot Owner Only)")
            );
    }

    /**
     * Check if a user is the bot owner
     */
    private boolean isOwner(long userId) {
        return userId == Config.BOT_OWNER_ID;
    }
}