package com.deadside.bot.commands.admin;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.db.models.GuildConfig;
import com.deadside.bot.db.repositories.GuildConfigRepository;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Command for setting bounty-related configuration options
 * Allows administrators to customize min and max bounty amounts
 */
public class BountySettingsCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(BountySettingsCommand.class);
    private static final String MIN_BOUNTY_KEY = "bounty.min.amount";
    private static final String MAX_BOUNTY_KEY = "bounty.max.amount";
    private static final int DEFAULT_MIN_BOUNTY = 100;
    private static final int DEFAULT_MAX_BOUNTY = 50000;

    private final GuildConfigRepository guildConfigRepository;

    public BountySettingsCommand() {
        this.guildConfigRepository = new GuildConfigRepository();
    }

    @Override
    public String getName() {
        return "bountysettings";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("bountysettings", "Configure bounty system settings")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                        new SubcommandData("min", "Set the minimum bounty amount")
                                .addOptions(
                                        new OptionData(OptionType.INTEGER, "amount", "Minimum amount for placing bounties (default: 100)", true)
                                                .setMinValue(10)
                                                .setMaxValue(10000)
                                ),
                        new SubcommandData("max", "Set the maximum bounty amount")
                                .addOptions(
                                        new OptionData(OptionType.INTEGER, "amount", "Maximum amount for placing bounties (default: 50000)", true)
                                                .setMinValue(1000)
                                                .setMaxValue(1000000)
                                ),
                        new SubcommandData("reset", "Reset bounty settings to default values"),
                        new SubcommandData("view", "View current bounty settings")
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        // Defer reply since this involves database operations
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
            GuildConfig config = guildConfigRepository.findByGuildId(guildId);

            // Create config if it doesn't exist
            if (config == null) {
                config = new GuildConfig(guildId);
            }

            switch (subcommand) {
                case "min":
                    handleSetMinBounty(event, config);
                    break;
                case "max":
                    handleSetMaxBounty(event, config);
                    break;
                case "reset":
                    handleResetSettings(event, config);
                    break;
                case "view":
                    handleViewSettings(event, config);
                    break;
                default:
                    event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                            "Unknown Subcommand",
                            "That subcommand is not recognized."
                    )).queue();
            }
        } catch (Exception e) {
            logger.error("Error executing bounty settings command", e);
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                    "Error",
                    "An error occurred: " + e.getMessage()
            )).queue();
        }
    }

    /**
     * Handle the min subcommand to set minimum bounty amount
     */
    private void handleSetMinBounty(SlashCommandInteractionEvent event, GuildConfig config) {
        int amount = event.getOption("amount", DEFAULT_MIN_BOUNTY, OptionMapping::getAsInt);
        
        // Get current max bounty
        int currentMax = getMaxBounty(config);
        
        // Make sure min doesn't exceed max
        if (amount >= currentMax) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                    "Invalid Amount",
                    String.format("Minimum bounty amount (%d) must be less than maximum amount (%d).", 
                            amount, currentMax)
            )).queue();
            return;
        }
        
        // Save the new minimum
        config.setSetting(MIN_BOUNTY_KEY, String.valueOf(amount));
        guildConfigRepository.save(config);
        
        event.getHook().sendMessageEmbeds(EmbedUtils.successEmbed(
                "Minimum Bounty Updated",
                String.format("The minimum bounty amount has been set to **%d coins**.", amount)
        )).queue();
    }

    /**
     * Handle the max subcommand to set maximum bounty amount
     */
    private void handleSetMaxBounty(SlashCommandInteractionEvent event, GuildConfig config) {
        int amount = event.getOption("amount", DEFAULT_MAX_BOUNTY, OptionMapping::getAsInt);
        
        // Get current min bounty
        int currentMin = getMinBounty(config);
        
        // Make sure max is greater than min
        if (amount <= currentMin) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed(
                    "Invalid Amount",
                    String.format("Maximum bounty amount (%d) must be greater than minimum amount (%d).", 
                            amount, currentMin)
            )).queue();
            return;
        }
        
        // Save the new maximum
        config.setSetting(MAX_BOUNTY_KEY, String.valueOf(amount));
        guildConfigRepository.save(config);
        
        event.getHook().sendMessageEmbeds(EmbedUtils.successEmbed(
                "Maximum Bounty Updated",
                String.format("The maximum bounty amount has been set to **%d coins**.", amount)
        )).queue();
    }

    /**
     * Handle the reset subcommand to reset bounty settings to defaults
     */
    private void handleResetSettings(SlashCommandInteractionEvent event, GuildConfig config) {
        // Remove the settings
        config.setSetting(MIN_BOUNTY_KEY, String.valueOf(DEFAULT_MIN_BOUNTY));
        config.setSetting(MAX_BOUNTY_KEY, String.valueOf(DEFAULT_MAX_BOUNTY));
        guildConfigRepository.save(config);
        
        event.getHook().sendMessageEmbeds(EmbedUtils.successEmbed(
                "Bounty Settings Reset",
                String.format("Bounty settings have been reset to defaults:\n" +
                        "Minimum: **%d coins**\n" +
                        "Maximum: **%d coins**", 
                        DEFAULT_MIN_BOUNTY, DEFAULT_MAX_BOUNTY)
        )).queue();
    }

    /**
     * Handle the view subcommand to show current bounty settings
     */
    private void handleViewSettings(SlashCommandInteractionEvent event, GuildConfig config) {
        int minBounty = getMinBounty(config);
        int maxBounty = getMaxBounty(config);
        
        event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed(
                "Bounty Settings",
                String.format("Current bounty settings for this server:\n\n" +
                        "Minimum Bounty: **%d coins**\n" +
                        "Maximum Bounty: **%d coins**", 
                        minBounty, maxBounty)
        )).queue();
    }

    /**
     * Get the minimum bounty amount from guild config
     */
    private int getMinBounty(GuildConfig config) {
        try {
            String minStr = config.getSetting(MIN_BOUNTY_KEY, String.valueOf(DEFAULT_MIN_BOUNTY));
            return Integer.parseInt(minStr);
        } catch (NumberFormatException e) {
            return DEFAULT_MIN_BOUNTY;
        }
    }

    /**
     * Get the maximum bounty amount from guild config
     */
    private int getMaxBounty(GuildConfig config) {
        try {
            String maxStr = config.getSetting(MAX_BOUNTY_KEY, String.valueOf(DEFAULT_MAX_BOUNTY));
            return Integer.parseInt(maxStr);
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_BOUNTY;
        }
    }

    @Override
    public List<Choice> handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // No autocomplete for this command
        return List.of();
    }
}