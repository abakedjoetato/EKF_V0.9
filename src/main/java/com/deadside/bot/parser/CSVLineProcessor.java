package com.deadside.bot.parser;

import com.deadside.bot.data.MongoDBHandler;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes individual CSV lines from Deadside server logs.
 * Extracts kill information and updates database statistics.
 */
public class CSVLineProcessor {
    private static final Logger LOGGER = Logger.getLogger(CSVLineProcessor.class.getName());
    private final MongoDBHandler mongoDBHandler;
    
    // Common patterns for parsing kill log lines
    private static final Pattern KILL_PATTERN = Pattern.compile("(.*?) killed (.*?) with (.*)");
    
    public CSVLineProcessor(MongoDBHandler mongoDBHandler) {
        this.mongoDBHandler = mongoDBHandler;
    }
    
    /**
     * Process a single line from the CSV log file.
     * 
     * @param line The raw CSV line
     * @param host The server host
     * @param serverId The server ID
     * @param guildId The Discord guild ID
     * @param sendEmbeds Whether to send Discord embeds (false for historical parsing)
     * @return true if the line was successfully processed, false otherwise
     */
    public boolean processLine(String line, String host, String serverId, String guildId, boolean sendEmbeds) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Split the CSV line into components
            // Format is typically: timestamp,event_type,data
            String[] parts = line.split(",", 3);
            
            if (parts.length < 3) {
                LOGGER.fine("Invalid line format (not enough parts): " + line);
                return false;
            }
            
            String timestamp = parts[0].trim();
            String eventType = parts[1].trim();
            String eventData = parts[2].trim();
            
            // Process based on event type
            switch (eventType.toLowerCase()) {
                case "kill":
                    return processKillEvent(timestamp, eventData, host, serverId, guildId, sendEmbeds);
                case "death":
                    return processDeathEvent(timestamp, eventData, host, serverId, guildId, sendEmbeds);
                default:
                    LOGGER.fine("Unrecognized event type: " + eventType);
                    return false;
            }
        } catch (Exception e) {
            LOGGER.warning("Error processing line: " + line + " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Process a kill event from the log.
     */
    private boolean processKillEvent(String timestamp, String eventData, String host, String serverId, 
                                   String guildId, boolean sendEmbeds) {
        Matcher matcher = KILL_PATTERN.matcher(eventData);
        
        if (!matcher.matches()) {
            LOGGER.fine("Kill line doesn't match expected pattern: " + eventData);
            return false;
        }
        
        String killer = matcher.group(1).trim();
        String victim = matcher.group(2).trim();
        String weapon = matcher.group(3).trim();
        
        // Skip system or invalid entries
        if (shouldSkipKillEvent(killer, victim, weapon)) {
            return false;
        }
        
        // Store kill information in database
        mongoDBHandler.recordKill(guildId, host + "_" + serverId, timestamp, killer, victim, weapon);
        
        // Only send Discord embed for live events, not historical
        if (sendEmbeds) {
            sendKillEmbed(guildId, host, serverId, timestamp, killer, victim, weapon);
        }
        
        return true;
    }
    
    /**
     * Process a death event from the log.
     */
    private boolean processDeathEvent(String timestamp, String eventData, String host, String serverId, 
                                   String guildId, boolean sendEmbeds) {
        // Process death events (suicides, falls, etc.)
        // Implementation would depend on the format of death events
        
        // For now, just log and return true
        LOGGER.fine("Processing death event: " + eventData);
        return true;
    }
    
    /**
     * Determine if a kill event should be skipped based on data.
     */
    private boolean shouldSkipKillEvent(String killer, String victim, String weapon) {
        // Skip invalid or system events
        if (killer == null || victim == null || weapon == null) {
            return true;
        }
        
        if (killer.isEmpty() || victim.isEmpty()) {
            return true;
        }
        
        // Skip certain system messages or invalid entries
        if (killer.equalsIgnoreCase("system") || 
            victim.equalsIgnoreCase("system") || 
            killer.equals(victim)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Send a kill event to Discord (only for live events, not historical)
     */
    private void sendKillEmbed(String guildId, String host, String serverId, 
                            String timestamp, String killer, String victim, String weapon) {
        // This would call the Discord embed sender
        // For historical parsing, this is never called
        LOGGER.fine("Would send kill embed for: " + killer + " killed " + victim);
    }
}
