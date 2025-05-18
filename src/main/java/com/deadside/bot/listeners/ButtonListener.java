package com.deadside.bot.listeners;

import com.deadside.bot.commands.economy.BlackjackCommand;
import com.deadside.bot.commands.economy.RouletteCommand;
import com.deadside.bot.commands.economy.SlotCommand;
import com.deadside.bot.db.models.Faction;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.LinkedPlayer;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.FactionRepository;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.LinkedPlayerRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.premium.FeatureGate;
import com.deadside.bot.premium.PremiumManager;
import com.deadside.bot.utils.EmbedUtils;
import com.deadside.bot.utils.ModalUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for button interactions
 */
public class ButtonListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ButtonListener.class);
    private final SlotCommand slotCommand = new SlotCommand();
    private final BlackjackCommand blackjackCommand = new BlackjackCommand();
    private final RouletteCommand rouletteCommand = new RouletteCommand();
    private final LinkedPlayerRepository linkedPlayerRepository = new LinkedPlayerRepository();
    private final PlayerRepository playerRepository = new PlayerRepository();
    private final FactionRepository factionRepository = new FactionRepository();
    private final GameServerRepository gameServerRepository = new GameServerRepository();
    private final PremiumManager premiumManager = new PremiumManager();
    
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        try {
            String[] buttonData = event.getComponentId().split(":");
            
            if (buttonData.length < 2) {
                return;
            }
            
            String buttonType = buttonData[0];
            String action = buttonData[1];
            
            switch (buttonType) {
                // Game-related buttons
                case "slot" -> handleSlotButton(event, buttonData);
                case "blackjack" -> blackjackCommand.handleButtonInteraction(event, buttonData);
                case "roulette" -> rouletteCommand.handleButtonInteraction(event, buttonData);
                
                // Navigation and information buttons
                case "leaderboard" -> handleLeaderboardButton(event, buttonData);
                case "server" -> handleServerButton(event, buttonData);
                case "faction" -> handleFactionButton(event, buttonData);
                case "player" -> handlePlayerButton(event, buttonData);
                
                // Premium feature buttons
                case "premium" -> handlePremiumButton(event, buttonData);
                
                // Generic pagination and action buttons
                case "page" -> handlePaginationButton(event, buttonData);
                case "confirm" -> handleConfirmationButton(event, buttonData);
                case "refresh" -> handleRefreshButton(event, buttonData);
                case "close" -> handleCloseButton(event, buttonData);
                
                default -> {
                    // Unknown button type
                    logger.warn("Unknown button type: {}", buttonType);
                }
            }
        } catch (Exception e) {
            logger.error("Error handling button interaction", e);
            event.reply("An error occurred: " + e.getMessage()).setEphemeral(true).queue();
        }
    }
    
    /**
     * Handle slot machine buttons
     */
    private void handleSlotButton(ButtonInteractionEvent event, String[] buttonData) {
        if (buttonData.length < 3) {
            return;
        }
        
        String action = buttonData[1];
        int originalBet = Integer.parseInt(buttonData[2]);
        int newBet = originalBet;
        
        // Calculate new bet amount based on action
        switch (action) {
            case "playAgain" -> newBet = originalBet;
            case "double" -> newBet = originalBet * 2;
            case "half" -> newBet = Math.max(10, originalBet / 2);
            default -> {
                // Unknown action
                logger.warn("Unknown slot button action: {}", action);
                return;
            }
        }
        
        // Get linked player
        LinkedPlayer linkedPlayer = linkedPlayerRepository.findByDiscordId(event.getUser().getIdLong());
        
        if (linkedPlayer == null) {
            event.reply("You don't have a linked Deadside account. Use `/link` to connect your Discord and Deadside accounts.")
                 .setEphemeral(true).queue();
            return;
        }
        
        // Get player
        Player player = playerRepository.findByPlayerId(linkedPlayer.getMainPlayerId());
        
        if (player == null) {
            event.reply("Unable to find your player data. This could be because you haven't been active yet.")
                 .setEphemeral(true).queue();
            return;
        }
        
        // Check if player has enough balance
        if (player.getCurrency().getCoins() < newBet) {
            event.reply("You don't have enough coins for this bet. Your current balance is " + 
                       String.format("%,d", player.getCurrency().getCoins()) + " coins.")
                 .setEphemeral(true).queue();
            return;
        }
        
        // Acknowledge the button click by deferring edit
        event.deferEdit().queue();
        
        // Start a new slot machine game with the new bet
        startNewSlotGame(event, player, newBet);
    }
    
    /**
     * Start a new slot machine game
     */
    private void startNewSlotGame(ButtonInteractionEvent event, Player player, int betAmount) {
        // First, take the bet
        player.getCurrency().removeCoins(betAmount);
        
        // Animation phases
        final String[] spinningSymbols = {"🎰", "💫", "✨", "🎲", "🎯"};
        
        // Initial message with spinning animation
        String initialDisplay = "[ " + spinningSymbols[0] + " | " + spinningSymbols[0] + " | " + spinningSymbols[0] + " ]";
        
        // Create the initial response message
        StringBuilder initialMessage = new StringBuilder();
        initialMessage.append("**Bet**: `").append(String.format("%,d", betAmount)).append(" coins`\n\n");
        initialMessage.append("# ").append(initialDisplay).append("\n\n");
        initialMessage.append("*Spinning the reels again...*\n\n");
        initialMessage.append("**Balance**: `").append(String.format("%,d", player.getCurrency().getCoins())).append(" coins`");
        
        // Update the message to show spinning
        event.getHook().editOriginalEmbeds(
                EmbedUtils.customEmbed("Slot Machine - Spinning", initialMessage.toString(), java.awt.Color.BLUE)
        ).setComponents().queue(message -> {
            // Start the slot machine animation using the existing method in SlotCommand
            slotCommand.accessAnimateSlotMachine(message, player, betAmount);
        });
        
        // Log the new bet
        logger.info("User {} bet {} coins on slots (via button)", event.getUser().getName(), betAmount);
    }
    
    /**
     * Handle premium-related buttons
     */
    private void handlePremiumButton(ButtonInteractionEvent event, String[] buttonData) {
        if (buttonData.length < 2) {
            return;
        }
        
        String action = buttonData[1];
        Guild guild = event.getGuild();
        User user = event.getUser();
        
        if (guild == null) {
            event.reply("This button can only be used in a server.").setEphemeral(true).queue();
            return;
        }
        
        switch (action) {
            case "info" -> showPremiumInfo(event, guild);
            case "purchase" -> showPremiumPurchase(event, guild, user);
            case "verify" -> verifyPremiumPayment(event, guild, user);
            case "features" -> showPremiumFeatures(event, guild);
            default -> {
                logger.warn("Unknown premium button action: {}", action);
                event.reply("Unknown premium action. Please try again.").setEphemeral(true).queue();
            }
        }
    }
    
    /**
     * Show premium information
     */
    private void showPremiumInfo(ButtonInteractionEvent event, Guild guild) {
        event.deferReply(true).queue(); // Ephemeral response
        
        long guildId = guild.getIdLong();
        boolean hasPremium = premiumManager.hasPremium(guildId);
        String statusDetails = premiumManager.getPremiumStatusDetails(guildId);
        
        StringBuilder description = new StringBuilder();
        description.append("## Deadside Premium\n\n");
        
        if (hasPremium) {
            description.append("This server has **PREMIUM** features enabled!\n\n")
                       .append(statusDetails)
                       .append("\n\n");
        } else {
            description.append("This server is using the **FREE** tier.\n\n")
                       .append(statusDetails)
                       .append("\n\n")
                       .append("Only the basic killfeed feature is available in the free tier.\n\n");
        }
        
        description.append("### Premium Features\n")
                   .append("• Advanced player statistics\n")
                   .append("• Detailed leaderboards\n")
                   .append("• Faction system\n")
                   .append("• Economy and gambling\n")
                   .append("• Real-time event notifications\n")
                   .append("• Server monitoring and detailed logs\n\n");
        
        description.append("### Pricing\n")
                   .append("Premium is available from $4.99/month per server.\n")
                   .append("Discounts are available for longer subscription periods.");
        
        // Add action buttons based on current premium status
        event.getHook().sendMessageEmbeds(
                EmbedUtils.premiumEmbed("Premium Information", description.toString())
        ).addActionRow(
                hasPremium ? 
                        Button.primary("premium:features", "View Premium Features")
                        : Button.success("premium:purchase", "Get Premium"),
                Button.link("https://deadside.com/premium", "Learn More")
        ).queue();
    }
    
    /**
     * Show premium purchase options
     */
    private void showPremiumPurchase(ButtonInteractionEvent event, Guild guild, User user) {
        long guildId = guild.getIdLong();
        
        // Check if guild already has premium
        if (premiumManager.hasPremium(guildId)) {
            event.reply("This server already has an active premium subscription: " + 
                    premiumManager.getPremiumStatusDetails(guildId))
                 .setEphemeral(true).queue();
            return;
        }
        
        // Show purchase modal
        event.replyModal(
                ModalUtils.createPremiumPurchaseModal(event)
        ).queue();
    }
    
    /**
     * Verify premium payment
     */
    private void verifyPremiumPayment(ButtonInteractionEvent event, Guild guild, User user) {
        event.deferReply(true).queue(); // Ephemeral response
        
        long guildId = guild.getIdLong();
        long userId = user.getIdLong();
        
        // Check if guild already has premium
        if (premiumManager.hasPremium(guildId)) {
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.infoEmbed("Already Premium", 
                            "This server already has an active premium subscription: " + 
                            premiumManager.getPremiumStatusDetails(guildId))
            ).queue();
            return;
        }
        
        // Verify payment through Tip4serv
        boolean verified = premiumManager.verifyTip4servPayment(guildId, userId);
        
        if (verified) {
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.premiumEmbed("✨ Payment Verified", 
                            "Your payment has been verified and premium features are now enabled for this server!\n\n" +
                            "All premium features are now available.")
            ).queue();
        } else {
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.infoEmbed("Payment Verification", 
                            "No active payment was found for this server.\n\n" +
                            "If you recently purchased premium, it may take a few minutes to process. " +
                            "If the problem persists, please check you used the correct Discord account " +
                            "during checkout or contact support with your purchase confirmation.")
            ).addActionRow(
                    Button.success("premium:purchase", "Purchase Premium"),
                    Button.link("https://deadside.com/support", "Contact Support")
            ).queue();
        }
    }
    
    /**
     * Show premium features
     */
    private void showPremiumFeatures(ButtonInteractionEvent event, Guild guild) {
        event.deferReply(true).queue(); // Ephemeral response
        
        long guildId = guild.getIdLong();
        boolean hasPremium = premiumManager.hasPremium(guildId);
        
        StringBuilder description = new StringBuilder();
        
        if (hasPremium) {
            description.append("## Premium Features\n\n")
                       .append("This server has the following premium features enabled:\n\n");
        } else {
            description.append("## Premium Features\n\n")
                       .append("Upgrade to premium to unlock the following features:\n\n");
        }
        
        // Player Statistics
        description.append("### 📊 Advanced Player Statistics\n")
                   .append("• Detailed kill/death stats\n")
                   .append("• Weapon usage tracking\n")
                   .append("• Player matchup analysis\n")
                   .append("• Historical performance trends\n\n");
        
        // Leaderboards
        description.append("### 🏆 Comprehensive Leaderboards\n")
                   .append("• Kill/death/KD leaderboards\n")
                   .append("• Weapon mastery rankings\n")
                   .append("• Faction performance rankings\n")
                   .append("• Custom time period filters\n\n");
        
        // Factions
        description.append("### 🛡️ Faction System\n")
                   .append("• Create and manage factions\n")
                   .append("• Track faction performance\n")
                   .append("• Faction vs faction statistics\n")
                   .append("• Hierarchical role management\n\n");
        
        // Economy
        description.append("### 💰 Economy System\n")
                   .append("• In-game currency based on performance\n")
                   .append("• Gambling features (slots, blackjack, roulette)\n")
                   .append("• Daily rewards and economy management\n")
                   .append("• Server owner customization\n\n");
        
        event.getHook().sendMessageEmbeds(
                EmbedUtils.premiumEmbed("Premium Features", description.toString())
        ).addActionRow(
                hasPremium ? 
                        Button.secondary("refresh:premium_status", "Refresh Status")
                        : Button.success("premium:purchase", "Get Premium"),
                Button.link("https://deadside.com/premium", "Learn More")
        ).queue();
    }
    
    /**
     * Handle pagination buttons
     */
    private void handlePaginationButton(ButtonInteractionEvent event, String[] buttonData) {
        if (buttonData.length < 3) {
            return;
        }
        
        String contentType = buttonData[1];
        String action = buttonData[2];
        
        event.deferEdit().queue();
        
        // Placeholder for pagination - this would be expanded in a full implementation
        event.getHook().editOriginalEmbeds(
                EmbedUtils.infoEmbed("Pagination", 
                        "Pagination for " + contentType + " (action: " + action + ") would be implemented here.")
        ).queue();
    }
    
    /**
     * Handle confirmation buttons
     */
    private void handleConfirmationButton(ButtonInteractionEvent event, String[] buttonData) {
        if (buttonData.length < 3) {
            return;
        }
        
        String confirmType = buttonData[1];
        String action = buttonData[2];
        
        // This would contain specific actions based on confirmation type
        event.deferEdit().queue();
        event.getHook().editOriginalEmbeds(
                EmbedUtils.successEmbed("Confirmed", 
                        "Action confirmed: " + confirmType + " - " + action)
        ).setComponents().queue();
    }
    
    /**
     * Handle refresh buttons
     */
    private void handleRefreshButton(ButtonInteractionEvent event, String[] buttonData) {
        if (buttonData.length < 2) {
            return;
        }
        
        String contentType = buttonData[1];
        
        event.deferEdit().queue();
        
        // This would refresh different content types
        event.getHook().editOriginalEmbeds(
                EmbedUtils.infoEmbed("Content Refreshed", 
                        "The " + contentType + " data has been refreshed.")
        ).queue();
    }
    
    /**
     * Handle close buttons
     */
    private void handleCloseButton(ButtonInteractionEvent event, String[] buttonData) {
        event.deferEdit().queue();
        event.getHook().deleteOriginal().queue();
        logger.debug("Closed interactive message for user: {}", event.getUser().getId());
    }
    
    /**
     * Handle leaderboard buttons
     */
    private void handleLeaderboardButton(ButtonInteractionEvent event, String[] buttonData) {
        if (buttonData.length < 2) {
            return;
        }
        
        // Placeholder for leaderboard buttons
        event.deferReply().queue();
        event.getHook().sendMessage("Leaderboard button action would be implemented here.").queue();
    }
    
    /**
     * Handle server buttons
     */
    private void handleServerButton(ButtonInteractionEvent event, String[] buttonData) {
        if (buttonData.length < 2) {
            return;
        }
        
        // Placeholder for server buttons
        event.deferReply().queue();
        event.getHook().sendMessage("Server button action would be implemented here.").queue();
    }
    
    /**
     * Handle faction buttons
     */
    private void handleFactionButton(ButtonInteractionEvent event, String[] buttonData) {
        if (buttonData.length < 2) {
            return;
        }
        
        // Placeholder for faction buttons
        event.deferReply().queue();
        event.getHook().sendMessage("Faction button action would be implemented here.").queue();
    }
    
    /**
     * Handle player buttons
     */
    private void handlePlayerButton(ButtonInteractionEvent event, String[] buttonData) {
        if (buttonData.length < 2) {
            return;
        }
        
        // Placeholder for player buttons
        event.deferReply().queue();
        event.getHook().sendMessage("Player button action would be implemented here.").queue();
    }
}