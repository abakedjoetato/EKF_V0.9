#!/bin/bash

echo "Starting Discord bot using direct Java execution instead of Maven..."

# Check if DISCORD_TOKEN is set
if [ -z "$DISCORD_TOKEN" ]; then
  echo "ERROR: DISCORD_TOKEN environment variable is not set."
  echo "Please set it in the Secrets tab in Replit."
  exit 1
fi

# Create a simple main class that uses JDA directly
cat > QuickBot.java << 'EOF'
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class QuickBot extends ListenerAdapter {
    public static void main(String[] args) throws Exception {
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isEmpty()) {
            System.err.println("Discord token not provided");
            System.exit(1);
        }
        
        System.out.println("Starting up Discord bot with Replit button...");
        
        JDA jda = JDABuilder.createDefault(token)
            .setStatus(OnlineStatus.ONLINE)
            .setActivity(Activity.playing("Deadside"))
            .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .setChunkingFilter(ChunkingFilter.ALL)
            .addEventListeners(new QuickBot())
            .build();
            
        jda.awaitReady();
        System.out.println("Bot is ready and connected to Discord!");
        System.out.println("Connected to " + jda.getGuilds().size() + " servers");
        
        // Register slash commands
        jda.updateCommands().addCommands(
            Commands.slash("ping", "Test bot response"),
            Commands.slash("server", "Get server info"),
            Commands.slash("status", "Check the bot's status")
        ).queue();
    }
    
    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Bot is fully connected to Discord!");
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        
        String message = event.getMessage().getContentRaw();
        
        if (message.equalsIgnoreCase("!ping")) {
            event.getChannel().sendMessage("Pong! Bot is running with Replit button.").queue();
        }
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping":
                long time = System.currentTimeMillis();
                event.reply("Pong!").queue(response -> {
                    response.editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time).queue();
                });
                break;
                
            case "server":
                event.reply("Server: " + event.getGuild().getName() + "\n" +
                           "Members: " + event.getGuild().getMemberCount()).queue();
                break;
                
            case "status":
                event.reply("Deadside Bot is online and operational!").queue();
                break;
        }
    }
}
EOF

# Compile and run the bot
echo "Compiling the Java code..."
/usr/bin/env java -version
javac -cp "target/dependency/*" QuickBot.java

echo "Starting the Discord bot..."
java -cp ".:target/dependency/*" QuickBot