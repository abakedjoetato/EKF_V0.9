package com.deadside.bot.commands.server;

import com.deadside.bot.commands.ICommand;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;

/**
 * Wrapper for SetBountyChannelCommand to implement ICommand interface
 */
public class SetBountyChannelCommandWrapper implements ICommand {
    @Override
    public String getName() {
        return "setbountychannel";
    }

    @Override
    public CommandData getCommandData() {
        return SetBountyChannelCommand.getCommandData();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        SetBountyChannelCommand.execute(event);
    }

    @Override
    public List<Choice> handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // No autocomplete for now
        return List.of();
    }
}