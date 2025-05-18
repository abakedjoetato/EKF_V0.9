# Discord Interaction Response Timing Pattern

This document outlines the interaction response timing pattern implemented throughout the Deadside Discord bot to ensure all interactions comply with Discord's 3-second acknowledgment requirement.

## Discord's Interaction Constraints

Discord requires that all interaction events (slash commands, buttons, modals, select menus) must be acknowledged within 3 seconds. Failure to do so results in an "interaction failed" error shown to users.

Two primary methods exist for acknowledging interactions:
1. **Immediate response** - `event.reply()`
2. **Deferred response** - `event.deferReply()` followed later by `event.getHook().sendMessage()`

## Implementation Pattern

The bot follows a consistent pattern across all interactions:

### Slash Commands

```java
@Override
public void execute(SlashCommandInteractionEvent event) {
    try {
        // 1. IMMEDIATELY defer the reply to prevent timeout
        event.deferReply().queue();
        
        // 2. Perform validation, database queries, or other operations
        // [Database operations, API calls, etc.]
        
        // 3. Send the actual response using the hook
        event.getHook().sendMessage("Command completed successfully!").queue();
    } catch (Exception e) {
        // Handle errors with appropriate logging
        logger.error("Error in command execution", e);
        
        // Send error response using the hook since we already deferred
        if (event.isAcknowledged()) {
            event.getHook().sendMessage("An error occurred: " + e.getMessage()).queue();
        } else {
            // Fallback if somehow the command wasn't acknowledged
            event.reply("An error occurred: " + e.getMessage()).queue();
        }
    }
}
```

### Button Interactions

```java
@Override
public void onButtonInteraction(ButtonInteractionEvent event) {
    try {
        // For buttons on existing messages, use deferEdit()
        event.deferEdit().queue();
        
        // Process the button action
        // [Database operations, etc.]
        
        // Edit the original message with updated content
        event.getHook().editOriginal("Button action processed!").queue();
    } catch (Exception e) {
        // Error handling
        logger.error("Error in button interaction", e);
        
        if (event.isAcknowledged()) {
            event.getHook().sendMessage("An error occurred: " + e.getMessage())
                  .setEphemeral(true).queue();
        } else {
            event.reply("An error occurred: " + e.getMessage())
                  .setEphemeral(true).queue();
        }
    }
}
```

### Modal and Select Menu Interactions

These follow the same pattern as slash commands, with immediate `deferReply()` at the start of the handler.

## Key Implementation Principles

1. **Always Defer First** - Call `deferReply()` or `deferEdit()` immediately before any other operations
2. **Proper Hook Usage** - Use `event.getHook()` for all responses after deferring
3. **Error Handling** - Check `event.isAcknowledged()` in error handlers
4. **Ephemeral Responses** - Use `setEphemeral(true)` for private responses when appropriate
5. **Queue All Responses** - Always call `.queue()` on responses, never use `.complete()`

## Special Cases

### Early Validation

For simple validations that can be processed quickly (under 3 seconds):

```java
// If we can validate and respond quickly without database operations
if (someQuickValidationFails) {
    event.reply("Validation failed: " + errorMessage).setEphemeral(true).queue();
    return;
}

// Otherwise, defer and proceed with longer operations
event.deferReply().queue();
// Long operations...
```

### Follow-up Messages

To send additional messages after the initial response:

```java
// First response happens with getHook() after deferReply()
event.getHook().sendMessage("Initial response").queue();

// Later follow-ups use the channel directly
event.getChannel().sendMessage("Follow-up information").queue();
```

## Implementation Across Bot Components

This pattern has been systematically implemented across:
- All slash commands in each command category
- All button interaction handlers
- Modal submission handlers
- Select menu interaction handlers

By following this pattern consistently, the bot ensures reliable handling of all Discord interactions within the required time constraints.