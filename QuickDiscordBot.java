import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class QuickDiscordBot extends ListenerAdapter {
    public static void main(String[] args) throws Exception {
        // Get Discord token from environment variable
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isEmpty()) {
            System.err.println("ERROR: DISCORD_TOKEN environment variable not set!");
            System.exit(1);
        }
        
        System.out.println("Starting Deadside Discord bot...");
        
        // Create and start the bot
        JDABuilder.createDefault(token)
            .setStatus(OnlineStatus.ONLINE)
            .setActivity(Activity.playing("Deadside"))
            .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
            .addEventListeners(new QuickDiscordBot())
            .build();
            
        System.out.println("Bot is now connected to Discord!");
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Don't respond to bot messages (prevents feedback loops)
        if (event.getAuthor().isBot()) return;
        
        String message = event.getMessage().getContentRaw();
        
        // Simple commands that don't require database access
        if (message.equals("!ping")) {
            event.getChannel().sendMessage("Pong! The bot is working.").queue();
        }
        else if (message.equals("!help")) {
            event.getChannel().sendMessage("**Deadside Bot Commands**\n" +
                "- !ping: Check if the bot is online\n" +
                "- !help: Show this help message").queue();
        }
    }
}
