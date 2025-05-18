import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deadside Discord Bot
 */
public class SimpleDeadsideBot extends ListenerAdapter {
    
    private static JDA jda;
    
    // Simple in-memory database for storing faction data
    private static final Map<String, Map<String, Object>> factions = new HashMap<>();
    private static final Map<String, Map<String, Object>> players = new HashMap<>();
    
    public static void main(String[] args) {
        try {
            // Get token from environment variable
            String token = System.getenv("DISCORD_TOKEN");
            if (token == null || token.isEmpty()) {
                System.err.println("DISCORD_TOKEN environment variable is not set!");
                System.exit(1);
            }
            
            System.out.println("Starting Deadside Discord Bot...");
            
            // Initialize JDA
            jda = JDABuilder.createDefault(token)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("Deadside"))
                .enableIntents(
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .addEventListeners(new SimpleDeadsideBot())
                .build();
            
            // Wait for the connection to be established
            jda.awaitReady();
            System.out.println("Deadside Bot is now connected to Discord!");
            System.out.println("Connected to " + jda.getGuilds().size() + " servers");
            
            // Register slash commands
            jda.updateCommands().addCommands(
                Commands.slash("ping", "Check the bot's response time"),
                Commands.slash("info", "Get info about the Deadside bot"),
                Commands.slash("server", "Get server information"),
                Commands.slash("help", "Display help for bot commands"),
                
                // Faction commands
                Commands.slash("faction_create", "Create a new faction")
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "name", "Name of the faction", true)
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "tag", "Tag of the faction (3-4 characters)", true)
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "description", "Description of your faction", false),
                
                Commands.slash("faction_info", "Get info about a faction")
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "name", "Name of the faction", true),
                
                Commands.slash("faction_list", "List all factions"),
                
                // Player commands
                Commands.slash("register", "Register as a player")
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "name", "Your in-game name", true),
                
                Commands.slash("player_info", "Get info about a player")
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.USER, "user", "The Discord user to get info for", false)
            ).queue();
            
            System.out.println("Slash commands registered");
            
        } catch (Exception e) {
            System.err.println("Error starting the bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Bot is ready! Connected to " + event.getGuildTotalCount() + " guilds");
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore messages from bots
        if (event.getAuthor().isBot()) return;
        
        Message message = event.getMessage();
        String content = message.getContentRaw();
        
        // Basic text commands
        if (content.equals("!ping")) {
            long time = System.currentTimeMillis();
            event.getChannel().sendMessage("Pong!").queue(response -> {
                response.editMessageFormat("Pong: %d ms", System.currentTimeMillis() - time).queue();
            });
        }
        else if (content.equals("!help")) {
            StringBuilder helpText = new StringBuilder();
            helpText.append("**Deadside Bot Commands:**\n\n");
            helpText.append("**Text Commands:**\n");
            helpText.append("- `!ping` - Check bot response time\n");
            helpText.append("- `!help` - Display this help message\n\n");
            helpText.append("**Slash Commands:**\n");
            helpText.append("- `/ping` - Check bot response time\n");
            helpText.append("- `/info` - Get info about the bot\n");
            helpText.append("- `/server` - Get server information\n");
            helpText.append("- `/help` - Display help information\n");
            helpText.append("- `/faction_create` - Create a new faction\n");
            helpText.append("- `/faction_info` - View information about a faction\n");
            helpText.append("- `/faction_list` - List all factions\n");
            helpText.append("- `/register` - Register as a player\n");
            helpText.append("- `/player_info` - Get info about a player\n");
            
            event.getChannel().sendMessage(helpText.toString()).queue();
        }
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping":
                long time = System.currentTimeMillis();
                event.reply("Pong!").setEphemeral(true).queue(response -> {
                    response.editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time).queue();
                });
                break;
                
            case "info":
                event.reply("**Deadside Discord Bot**\n" +
                        "A robust Discord bot for Deadside game server management, offering " +
                        "comprehensive faction and player engagement tools with enhanced economic interactions.\n\n" +
                        "Connected to: " + jda.getGuilds().size() + " servers")
                        .setEphemeral(true)
                        .queue();
                break;
                
            case "server":
                if (event.getGuild() == null) {
                    event.reply("This command can only be used in a server.").setEphemeral(true).queue();
                    return;
                }
                
                event.reply("**Server Info:**\n" +
                        "Name: " + event.getGuild().getName() + "\n" +
                        "Members: " + event.getGuild().getMemberCount() + "\n" +
                        "Created: " + event.getGuild().getTimeCreated().toString())
                        .setEphemeral(true)
                        .queue();
                break;
                
            case "help":
                StringBuilder helpText = new StringBuilder();
                helpText.append("**Deadside Bot Commands:**\n\n");
                helpText.append("**Text Commands:**\n");
                helpText.append("- `!ping` - Check bot response time\n");
                helpText.append("- `!help` - Display this help message\n\n");
                helpText.append("**Slash Commands:**\n");
                helpText.append("- `/ping` - Check bot response time\n");
                helpText.append("- `/info` - Get info about the bot\n");
                helpText.append("- `/server` - Get server information\n");
                helpText.append("- `/help` - Display help information\n");
                helpText.append("- `/faction_create` - Create a new faction\n");
                helpText.append("- `/faction_info` - View information about a faction\n");
                helpText.append("- `/faction_list` - List all factions\n");
                helpText.append("- `/register` - Register as a player\n");
                helpText.append("- `/player_info` - Get info about a player\n");
                
                event.reply(helpText.toString()).setEphemeral(true).queue();
                break;
                
            case "faction_create":
                handleFactionCreate(event);
                break;
                
            case "faction_info":
                handleFactionInfo(event);
                break;
                
            case "faction_list":
                handleFactionList(event);
                break;
                
            case "register":
                handlePlayerRegister(event);
                break;
                
            case "player_info":
                handlePlayerInfo(event);
                break;
                
            default:
                event.reply("Unknown command").setEphemeral(true).queue();
                break;
        }
    }
    
    private void handleFactionCreate(SlashCommandInteractionEvent event) {
        String name = event.getOption("name").getAsString();
        String tag = event.getOption("tag").getAsString();
        String description = event.getOption("description") != null 
            ? event.getOption("description").getAsString() 
            : "No description provided";
        
        // Check if faction already exists
        if (factions.containsKey(name.toLowerCase())) {
            event.reply("A faction with that name already exists!").setEphemeral(true).queue();
            return;
        }
        
        // Validate tag
        if (tag.length() < 2 || tag.length() > 4) {
            event.reply("Faction tag must be 2-4 characters long!").setEphemeral(true).queue();
            return;
        }
        
        // Create faction
        Map<String, Object> faction = new HashMap<>();
        faction.put("name", name);
        faction.put("tag", tag);
        faction.put("description", description);
        faction.put("leader", event.getUser().getId());
        faction.put("created", System.currentTimeMillis());
        faction.put("members", new ArrayList<>(List.of(event.getUser().getId())));
        
        factions.put(name.toLowerCase(), faction);
        
        // Update player's faction
        String userId = event.getUser().getId();
        if (players.containsKey(userId)) {
            players.get(userId).put("faction", name);
            players.get(userId).put("role", "leader");
        } else {
            Map<String, Object> player = new HashMap<>();
            player.put("name", event.getUser().getName());
            player.put("faction", name);
            player.put("role", "leader");
            player.put("joined", System.currentTimeMillis());
            players.put(userId, player);
        }
        
        event.reply("Faction **" + name + "** [" + tag + "] has been created successfully!").queue();
    }
    
    private void handleFactionInfo(SlashCommandInteractionEvent event) {
        String name = event.getOption("name").getAsString();
        
        // Check if faction exists
        if (!factions.containsKey(name.toLowerCase())) {
            event.reply("No faction found with that name!").setEphemeral(true).queue();
            return;
        }
        
        Map<String, Object> faction = factions.get(name.toLowerCase());
        String factionName = (String) faction.get("name");
        String tag = (String) faction.get("tag");
        String description = (String) faction.get("description");
        long created = (long) faction.get("created");
        @SuppressWarnings("unchecked")
        List<String> members = (List<String>) faction.get("members");
        
        StringBuilder info = new StringBuilder();
        info.append("**Faction: ").append(factionName).append(" [").append(tag).append("]**\n");
        info.append("Description: ").append(description).append("\n");
        info.append("Members: ").append(members.size()).append("\n");
        info.append("Created: ").append(new java.util.Date(created)).append("\n\n");
        
        info.append("**Members:**\n");
        for (String memberId : members) {
            String role = players.containsKey(memberId) ? (String) players.get(memberId).get("role") : "member";
            String roleIcon = role.equals("leader") ? "👑 " : "👤 ";
            String memberName = jda.getUserById(memberId) != null ? jda.getUserById(memberId).getName() : "Unknown";
            info.append(roleIcon).append(memberName).append("\n");
        }
        
        event.reply(info.toString()).queue();
    }
    
    private void handleFactionList(SlashCommandInteractionEvent event) {
        if (factions.isEmpty()) {
            event.reply("No factions have been created yet!").setEphemeral(true).queue();
            return;
        }
        
        StringBuilder list = new StringBuilder();
        list.append("**Available Factions:**\n\n");
        
        for (Map<String, Object> faction : factions.values()) {
            String name = (String) faction.get("name");
            String tag = (String) faction.get("tag");
            String description = (String) faction.get("description");
            @SuppressWarnings("unchecked")
            int memberCount = ((List<String>) faction.get("members")).size();
            
            list.append("**").append(name).append("** [").append(tag).append("] - ");
            list.append(memberCount).append(" members\n");
            list.append(description).append("\n\n");
        }
        
        event.reply(list.toString()).queue();
    }
    
    private void handlePlayerRegister(SlashCommandInteractionEvent event) {
        String name = event.getOption("name").getAsString();
        String userId = event.getUser().getId();
        
        // Create or update player
        if (players.containsKey(userId)) {
            players.get(userId).put("name", name);
            event.reply("Your player name has been updated to **" + name + "**").setEphemeral(true).queue();
        } else {
            Map<String, Object> player = new HashMap<>();
            player.put("name", name);
            player.put("joined", System.currentTimeMillis());
            players.put(userId, player);
            event.reply("You have been registered as **" + name + "**").setEphemeral(true).queue();
        }
    }
    
    private void handlePlayerInfo(SlashCommandInteractionEvent event) {
        String userId = event.getOption("user") != null 
            ? event.getOption("user").getAsUser().getId()
            : event.getUser().getId();
        
        if (!players.containsKey(userId)) {
            event.reply("Player not found! They need to register first with `/register`.").setEphemeral(true).queue();
            return;
        }
        
        Map<String, Object> player = players.get(userId);
        String name = (String) player.get("name");
        String faction = player.containsKey("faction") ? (String) player.get("faction") : "None";
        String role = player.containsKey("role") ? (String) player.get("role") : "None";
        long joined = (long) player.get("joined");
        
        StringBuilder info = new StringBuilder();
        info.append("**Player: ").append(name).append("**\n");
        info.append("Discord: <@").append(userId).append(">\n");
        info.append("Faction: ").append(faction).append("\n");
        if (!faction.equals("None")) {
            info.append("Role: ").append(role).append("\n");
        }
        info.append("Registered: ").append(new java.util.Date(joined)).append("\n");
        
        event.reply(info.toString()).queue();
    }
}