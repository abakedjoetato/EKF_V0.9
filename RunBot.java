/**
 * Launcher for Deadside Discord Bot
 */
public class RunBot {
    public static void main(String[] args) {
        try {
            // Get Discord token from environment variable
            String token = System.getenv("DISCORD_TOKEN");
            if (token == null || token.isEmpty()) {
                System.err.println("ERROR: DISCORD_TOKEN environment variable is not set!");
                System.exit(1);
            }
            
            // Create and start the bot
            DeadsideBot bot = new DeadsideBot(token);
            
            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down bot...");
                bot.shutdown();
            }));
            
            // Start the bot
            bot.start();
        } catch (Exception e) {
            System.err.println("Error starting Deadside bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}