import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SimpleDiscordBot extends ListenerAdapter {
    public static void main(String[] args) {
        try {
            String token = System.getenv("DISCORD_TOKEN");
            if (token == null || token.isEmpty()) {
                System.err.println("Error: DISCORD_TOKEN environment variable not set!");
                System.exit(1);
            }
            
            System.out.println("Starting Discord bot...");
            
            JDA jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .setActivity(Activity.playing("Deadside"))
                    .setStatus(OnlineStatus.ONLINE)
                    .addEventListeners(new SimpleDiscordBot())
                    .build();
                    
            System.out.println("Bot is connecting to Discord...");
            
            // Wait for JDA to be ready
            jda.awaitReady();
            System.out.println("Bot is ready and connected to Discord!");
            
        } catch (Exception e) {
            System.err.println("Error starting bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Bot is connected to " + event.getGuildTotalCount() + " servers!");
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore messages from bots to prevent potential feedback loops
        if (event.getAuthor().isBot()) return;
        
        String message = event.getMessage().getContentRaw();
        
        if (message.equalsIgnoreCase("!ping")) {
            event.getChannel().sendMessage("Pong!").queue();
        }
        
        if (message.equalsIgnoreCase("!help")) {
            event.getChannel().sendMessage("Available commands:\n" +
                                          "!ping - Check if the bot is responsive\n" +
                                          "!help - Display this help message").queue();
        }
    }
}