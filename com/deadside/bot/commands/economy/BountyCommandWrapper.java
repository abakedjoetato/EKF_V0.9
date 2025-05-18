package com.deadside.bot.commands.economy;

import com.deadside.bot.commands.ICommand;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;

/**
 * Wrapper for BountyCommand to implement ICommand interface
 */
public class BountyCommandWrapper implements ICommand {
    @Override
    public String getName() {
        return "bounty";
    }

    @Override
    public CommandData getCommandData() {
        return BountyCommand.getCommandData();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        BountyCommand.execute(event);
    }

    @Override
    public List<Choice> handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // No autocomplete for now
        return List.of();
    }
}