# Deadside Discord Bot - Autocomplete Implementations

This document demonstrates the autocomplete features implemented across various economy commands in the Deadside Discord Bot.

## 1. SlotCommand Autocomplete

The SlotCommand now offers intelligent bet suggestions based on player's wallet balance:

```java
@Override
public List<Choice> handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
    String focusedOption = event.getFocusedOption().getName();
    
    if ("amount".equals(focusedOption)) {
        try {
            // Get the user's wallet balance for smart suggestions
            long userId = event.getUser().getIdLong();
            LinkedPlayer linkedPlayer = linkedPlayerRepository.findByDiscordId(userId);
            
            // If player isn't linked or doesn't exist, just use default suggestions
            if (linkedPlayer == null) {
                return getDefaultBetSuggestions();
            }
            
            Player player = playerRepository.findByPlayerId(linkedPlayer.getMainPlayerId());
            if (player == null) {
                return getDefaultBetSuggestions();
            }
            
            // Get wallet balance
            long walletBalance = player.getCurrency().getCoins();
            
            // Handle user's input
            String currentValue = event.getFocusedOption().getValue();
            boolean hasCustomValue = !currentValue.isEmpty();
            long customValue = 0;
            
            if (hasCustomValue) {
                try {
                    customValue = Long.parseLong(currentValue);
                } catch (NumberFormatException e) {
                    // Invalid number, ignore and use suggestions
                    hasCustomValue = false;
                }
            }
            
            return generateBetSuggestions(walletBalance, hasCustomValue, customValue);
            
        } catch (Exception e) {
            logger.error("Error generating autocomplete suggestions for slots", e);
        }
        
        // Fallback to default suggestions
        return getDefaultBetSuggestions();
    }
    
    return List.of(); // No suggestions for other options
}

/**
 * Generate smart suggestions for bet amounts based on wallet balance and slot constraints
 */
private List<Choice> generateBetSuggestions(long walletBalance, boolean hasCustomValue, long customValue) {
    List<Choice> suggestions = new ArrayList<>();
    
    // Maximum bet for slots is 1000
    long maxBet = Math.min(walletBalance, 1000);
    
    // If user entered a custom value that's valid for slots, add it first
    if (hasCustomValue && customValue >= 10 && customValue <= maxBet) {
        suggestions.add(new Choice(formatAmount(customValue) + " coins", customValue));
    }
    
    // Add max bet option if they have enough coins
    if (maxBet >= 10) {
        if (maxBet == 1000) {
            suggestions.add(new Choice("Maximum bet (1,000)", 1000));
        } else {
            suggestions.add(new Choice("All in! (" + formatAmount(maxBet) + ")", maxBet));
        }
    }
    
    // Add common slot betting options
    long[] slotBets = {10, 20, 50, 100, 200, 500, 1000};
    for (long bet : slotBets) {
        if (bet <= maxBet && bet <= 1000) {
            suggestions.add(new Choice(formatAmount(bet) + " coins", bet));
        }
    }
    
    return suggestions;
}
```

## 2. BlackjackCommand Autocomplete

The BlackjackCommand implementation offers intelligent bet suggestions with more options based on the game's higher betting limits:

```java
@Override
public List<Choice> handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
    String focusedOption = event.getFocusedOption().getName();
    
    if ("bet".equals(focusedOption)) {
        try {
            // Get the user's wallet balance for smart suggestions
            long userId = event.getUser().getIdLong();
            LinkedPlayer linkedPlayer = linkedPlayerRepository.findByDiscordId(userId);
            
            // If player isn't linked or doesn't exist, just use default suggestions
            if (linkedPlayer == null) {
                return getDefaultBetSuggestions();
            }
            
            Player player = playerRepository.findByPlayerId(linkedPlayer.getMainPlayerId());
            if (player == null) {
                return getDefaultBetSuggestions();
            }
            
            // Get wallet balance
            long walletBalance = player.getCurrency().getCoins();
            
            // Handle user's input
            String currentValue = event.getFocusedOption().getValue();
            boolean hasCustomValue = !currentValue.isEmpty();
            long customValue = 0;
            
            if (hasCustomValue) {
                try {
                    customValue = Long.parseLong(currentValue);
                } catch (NumberFormatException e) {
                    // Invalid number, ignore and use suggestions
                    hasCustomValue = false;
                }
            }
            
            return generateBetSuggestions(walletBalance, hasCustomValue, customValue);
            
        } catch (Exception e) {
            logger.error("Error generating autocomplete suggestions for blackjack", e);
        }
        
        // Fallback to default suggestions
        return getDefaultBetSuggestions();
    }
    
    return List.of(); // No suggestions for other options
}

/**
 * Generate smart suggestions for bet amounts based on wallet balance and blackjack constraints
 */
private List<Choice> generateBetSuggestions(long walletBalance, boolean hasCustomValue, long customValue) {
    List<Choice> suggestions = new ArrayList<>();
    
    // Blackjack min bet is 50, max is 2000
    long minBet = 50;
    long maxBet = Math.min(walletBalance, 2000);
    
    // If user entered a custom value that's valid for blackjack, add it first
    if (hasCustomValue && customValue >= minBet && customValue <= maxBet) {
        suggestions.add(new Choice(formatAmount(customValue) + " coins", customValue));
    }
    
    // Add max bet option if they have enough coins
    if (maxBet >= minBet) {
        if (maxBet == 2000) {
            suggestions.add(new Choice("Maximum bet (2,000)", 2000));
        } else {
            suggestions.add(new Choice("All in! (" + formatAmount(maxBet) + ")", maxBet));
        }
    }
    
    // Add percentage-based options if balance is sufficient
    if (walletBalance >= 200) {
        suggestions.add(new Choice("Half balance (" + formatAmount(walletBalance / 2) + ")", 
                Math.min(walletBalance / 2, 2000)));
        
        if (walletBalance >= 1000) {
            suggestions.add(new Choice("25% balance (" + formatAmount(walletBalance / 4) + ")", 
                    Math.min(walletBalance / 4, 2000)));
        }
    }
    
    // Add common blackjack betting options
    long[] blackjackBets = {50, 100, 200, 500, 1000, 1500, 2000};
    for (long bet : blackjackBets) {
        if (bet <= maxBet && bet >= minBet) {
            suggestions.add(new Choice(formatAmount(bet) + " coins", bet));
        }
    }
    
    return suggestions;
}
```

## 3. Admin Economy Command (Renamed to "eco")

The AdminEconomyCommand has been renamed to "eco" for better usability and now features intelligent autocomplete for various administrative operations:

```java
@Override
public List<Choice> handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
    if (!isAdminOrOwner(event.getUser().getIdLong())) {
        return List.of(); // Non-admins don't get suggestions
    }
    
    String focusedOption = event.getFocusedOption().getName();
    String subcommand = event.getSubcommandName();
    
    if ("amount".equals(focusedOption)) {
        // Handle user input if any
        String currentValue = event.getFocusedOption().getValue();
        boolean hasCustomValue = !currentValue.isEmpty();
        long customValue = 0;
        
        if (hasCustomValue) {
            try {
                customValue = Long.parseLong(currentValue);
            } catch (NumberFormatException e) {
                hasCustomValue = false;
            }
        }
        
        // Get appropriate suggestions based on subcommand
        return switch (subcommand) {
            case "give" -> getGiveAmountSuggestions(event, hasCustomValue, customValue);
            case "take" -> getTakeAmountSuggestions(event, hasCustomValue, customValue);
            case "set" -> getSetAmountSuggestions(event, hasCustomValue, customValue);
            case "setdaily" -> getDailyAmountSuggestions(hasCustomValue, customValue);
            case "setworkmax" -> getWorkMaxAmountSuggestions(hasCustomValue, customValue);
            case "setworkmin" -> getWorkMinAmountSuggestions(hasCustomValue, customValue);
            default -> List.of();
        };
    }
    
    return List.of(); // No suggestions for other options
}
```

The command has specialized suggestion generators for different administrative operations:

```java
private List<Choice> getGiveAmountSuggestions(CommandAutoCompleteInteractionEvent event, boolean hasCustomValue, long customValue) {
    List<Choice> suggestions = new ArrayList<>();
    
    // Add custom value if valid
    if (hasCustomValue && customValue > 0) {
        suggestions.add(new Choice(formatAmount(customValue) + " coins", customValue));
    }
    
    // Try to get target user's balance for context-aware suggestions
    OptionMapping userOption = event.getOption("user");
    if (userOption != null) {
        User targetUser = userOption.getAsUser();
        LinkedPlayer linkedPlayer = linkedPlayerRepository.findByDiscordId(targetUser.getIdLong());
        
        if (linkedPlayer != null) {
            Player player = playerRepository.findByPlayerId(linkedPlayer.getMainPlayerId());
            if (player != null) {
                // Add balance-based suggestions
                long balance = player.getCurrency().getCoins();
                
                // Add percentage-based suggestions
                suggestions.add(new Choice("Match balance: " + formatAmount(balance), balance));
                if (balance >= 1000) {
                    suggestions.add(new Choice("10% of balance: " + formatAmount(balance / 10), balance / 10));
                    suggestions.add(new Choice("Double balance: " + formatAmount(balance * 2), balance * 2));
                }
            }
        }
    }
    
    // Add standard increments
    addCommonAdminAmounts(suggestions);
    
    return suggestions;
}
```

## Key Features Implemented

1. **Context-Aware Suggestions** - Options change based on the player's current balance
2. **Custom Value Support** - User's current input is validated and suggested if valid
3. **Smart Formatting** - All amounts are nicely formatted with commas for readability
4. **Default Fallbacks** - Sensible defaults when player data isn't available
5. **Command Optimization** - Admin commands renamed from "admineconomy" to "eco" for better UX

## Integration with Discord Commands

Each command's `getCommandData()` method has been updated to enable autocomplete:

```java
@Override
public CommandData getCommandData() {
    // Create option with autocomplete for bet amounts
    OptionData betOption = new OptionData(OptionType.INTEGER, "amount", "Amount to bet (min: 10, max: 1000)", true)
            .setAutoComplete(true);
    
    return Commands.slash(getName(), "Play the slot machine and try your luck")
            .addOptions(betOption);
}
```

For the AdminEconomyCommand (now "eco"), all relevant subcommands have autocomplete enabled:

```java
@Override
public CommandData getCommandData() {
    // Create options with autocomplete
    OptionData giveAmountOption = new OptionData(OptionType.INTEGER, "amount", "The amount of coins to give", true)
            .setAutoComplete(true);
    OptionData takeAmountOption = new OptionData(OptionType.INTEGER, "amount", "The amount of coins to take", true)
            .setAutoComplete(true);
    // ...other options...
            
    return Commands.slash(getName(), "Admin commands for managing the economy system")
            .addSubcommands(
                    new SubcommandData("give", "Give coins to a player")
                            .addOption(OptionType.USER, "user", "The user to give coins to", true)
                            .addOptions(giveAmountOption)
                            .addOption(OptionType.STRING, "reason", "The reason for giving coins", false),
                    // ...other subcommands...
            );
}
```

## Summary

These autocomplete implementations greatly enhance the user experience by providing intelligent suggestions based on context. The features support:

- Player balance-aware bet suggestions
- Smart formatting of currency amounts
- Support for both standard suggestions and user-entered values
- Specialized suggestions for different command types
- Optimized command names for better usability