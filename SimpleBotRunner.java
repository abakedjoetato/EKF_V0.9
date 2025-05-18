import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class SimpleBotRunner extends ListenerAdapter {
    public static void main(String[] args) {
        try {
            // Get token from environment variable
            String token = System.getenv("DISCORD_TOKEN");
            if (token == null || token.isEmpty()) {
                System.err.println("DISCORD_TOKEN environment variable not set!");
                System.exit(1);
            }

            // Build JDA instance
            JDA jda = JDABuilder.createDefault(token)
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.playing("Deadside"))
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, 
                                  GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new SimpleBotRunner())
                    .build();

            System.out.println("Discord bot is now starting up...");
            
            // Wait for JDA to be ready
            jda.awaitReady();
            System.out.println("Bot is ready! Connected to " + jda.getGuilds().size() + " servers");
            
        } catch (Exception e) {
            System.err.println("Error starting bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onReady(net.dv8tion.jda.api.events.session.ReadyEvent event) {
        System.out.println("Bot is ready and connected to Discord!");
    }
}