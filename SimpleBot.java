import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.EnumSet;

public class SimpleBot extends ListenerAdapter {
    private static JDA jda;

    public static void main(String[] args) {
        // Get token from environment variable
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isEmpty()) {
            System.err.println("DISCORD_TOKEN environment variable not set!");
            System.exit(1);
        }

        // Build JDA instance
        JDABuilder builder = JDABuilder.createDefault(token)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("Deadside"))
                .enableIntents(EnumSet.of(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.MESSAGE_CONTENT
                ))
                .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER)
                .addEventListeners(new SimpleBot());

        try {
            jda = builder.build();
            System.out.println("Starting bot...");
        } catch (Exception e) {
            System.err.println("Error starting bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Bot is ready! Connected to " + event.getGuildTotalCount() + " guilds");
        
        // Register a simple slash command
        event.getJDA().updateCommands().addCommands(
            Commands.slash("ping", "Check if the bot is responsive")
        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("ping")) {
            long time = System.currentTimeMillis();
            event.reply("Pong!").queue(response -> {
                response.editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time).queue();
            });
        }
    }
}