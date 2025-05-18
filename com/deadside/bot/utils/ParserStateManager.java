package com.deadside.bot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to manage parser states and control parser behavior
 * across different servers and guilds.
 */
public class ParserStateManager {
    private static final Logger logger = LoggerFactory.getLogger(ParserStateManager.class);
    
    // Use server name + guild ID as the key to track parser states by server
    private static final Map<String, ParserState> parserStates = new ConcurrentHashMap<>();
    
    /**
     * Get a parser state for a specific server and guild
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     * @return The parser state object (creates a new one if none exists)
     */
    public static ParserState getParserState(String serverName, long guildId) {
        String key = getStateKey(serverName, guildId);
        return parserStates.computeIfAbsent(key, k -> new ParserState(serverName, guildId));
    }
    
    /**
     * Check if killfeed parser is paused for a server
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     * @return true if paused, false otherwise
     */
    public static boolean isKillfeedParserPaused(String serverName, long guildId) {
        return getParserState(serverName, guildId).isKillfeedParserPaused();
    }
    
    /**
     * Check if CSV parser is paused for a server
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     * @return true if paused, false otherwise
     */
    public static boolean isCSVParserPaused(String serverName, long guildId) {
        return getParserState(serverName, guildId).isCSVParserPaused();
    }
    
    /**
     * Check if log parser is paused for a server
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     * @return true if paused, false otherwise
     */
    public static boolean isLogParserPaused(String serverName, long guildId) {
        return getParserState(serverName, guildId).isLogParserPaused();
    }
    
    /**
     * Check if historical parsing is enabled for a server
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     * @return true if enabled, false otherwise
     */
    public static boolean isHistoricalParsingEnabled(String serverName, long guildId) {
        return getParserState(serverName, guildId).isHistoricalParsingEnabled();
    }
    
    /**
     * Enable historical parsing for a server
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     */
    public static void enableHistoricalParsing(String serverName, long guildId) {
        ParserState state = getParserState(serverName, guildId);
        state.setHistoricalParsingEnabled(true);
        logger.info("Historical parsing enabled for server: {} in guild: {}", serverName, guildId);
    }
    
    /**
     * Disable historical parsing for a server
     * Also resets any related tracking state to ensure fresh start after historical processing
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     */
    public static void disableHistoricalParsing(String serverName, long guildId) {
        ParserState state = getParserState(serverName, guildId);
        state.setHistoricalParsingEnabled(false);
        
        // Reset any parser-specific state to ensure clean state after historical processing
        // This ensures the regular parser will start fresh after historical parsing completes
        state.clearParserMemory();
        
        logger.info("Historical parsing disabled for server: {} in guild: {}", serverName, guildId);
    }
    
    /**
     * Pause the killfeed parser for a server
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     * @param reason The reason for pausing
     */
    public static void pauseKillfeedParser(String serverName, long guildId, String reason) {
        ParserState state = getParserState(serverName, guildId);
        state.setKillfeedParserPaused(true);
        state.setKillfeedPauseReason(reason);
        logger.info("Killfeed parser paused for server: {} in guild: {}, reason: {}", 
                serverName, guildId, reason);
    }
    
    /**
     * Resume the killfeed parser for a server
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     */
    public static void resumeKillfeedParser(String serverName, long guildId) {
        ParserState state = getParserState(serverName, guildId);
        state.setKillfeedParserPaused(false);
        state.setKillfeedPauseReason("");
        logger.info("Killfeed parser resumed for server: {} in guild: {}", serverName, guildId);
    }
    
    /**
     * Pause the CSV parser for a server
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     * @param reason The reason for pausing
     */
    public static void pauseCSVParser(String serverName, long guildId, String reason) {
        ParserState state = getParserState(serverName, guildId);
        state.setCSVParserPaused(true);
        state.setCSVPauseReason(reason);
        logger.info("CSV parser paused for server: {} in guild: {}, reason: {}", 
                serverName, guildId, reason);
    }
    
    /**
     * Resume the CSV parser for a server
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     */
    public static void resumeCSVParser(String serverName, long guildId) {
        ParserState state = getParserState(serverName, guildId);
        state.setCSVParserPaused(false);
        state.setCSVPauseReason("");
        logger.info("CSV parser resumed for server: {} in guild: {}", serverName, guildId);
    }
    
    /**
     * Pause the log parser for a server
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     * @param reason The reason for pausing
     */
    public static void pauseLogParser(String serverName, long guildId, String reason) {
        ParserState state = getParserState(serverName, guildId);
        state.setLogParserPaused(true);
        state.setLogPauseReason(reason);
        logger.info("Log parser paused for server: {} in guild: {}, reason: {}", 
                serverName, guildId, reason);
    }
    
    /**
     * Resume the log parser for a server
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     */
    public static void resumeLogParser(String serverName, long guildId) {
        ParserState state = getParserState(serverName, guildId);
        state.setLogParserPaused(false);
        state.setLogPauseReason("");
        logger.info("Log parser resumed for server: {} in guild: {}", serverName, guildId);
    }
    
    /**
     * Create a state key from server name and guild ID
     * 
     * @param serverName The game server name
     * @param guildId The Discord guild ID
     * @return A unique key for storing parser state
     */
    private static String getStateKey(String serverName, long guildId) {
        return serverName + "_" + guildId;
    }
    
    /**
     * Nested class to store parser state for a specific server and guild
     */
    public static class ParserState {
        private final String serverName;
        private final long guildId;
        
        private boolean killfeedParserPaused;
        private String killfeedPauseReason;
        
        private boolean csvParserPaused;
        private String csvPauseReason;
        
        private boolean logParserPaused;
        private String logPauseReason;
        
        private boolean historicalParsingEnabled;
        
        // Additional memory state tracking - allows parsers to store/retrieve last positions
        private Map<String, Object> parserMemory = new HashMap<>();
        
        public ParserState(String serverName, long guildId) {
            this.serverName = serverName;
            this.guildId = guildId;
            this.killfeedParserPaused = false;
            this.killfeedPauseReason = "";
            this.csvParserPaused = false;
            this.csvPauseReason = "";
            this.logParserPaused = false;
            this.logPauseReason = "";
            this.historicalParsingEnabled = false;
        }
        
        public String getServerName() {
            return serverName;
        }
        
        public long getGuildId() {
            return guildId;
        }
        
        public boolean isKillfeedParserPaused() {
            return killfeedParserPaused;
        }
        
        public void setKillfeedParserPaused(boolean killfeedParserPaused) {
            this.killfeedParserPaused = killfeedParserPaused;
        }
        
        public String getKillfeedPauseReason() {
            return killfeedPauseReason;
        }
        
        public void setKillfeedPauseReason(String killfeedPauseReason) {
            this.killfeedPauseReason = killfeedPauseReason;
        }
        
        public boolean isCSVParserPaused() {
            return csvParserPaused;
        }
        
        public void setCSVParserPaused(boolean csvParserPaused) {
            this.csvParserPaused = csvParserPaused;
        }
        
        public String getCSVPauseReason() {
            return csvPauseReason;
        }
        
        public void setCSVPauseReason(String csvPauseReason) {
            this.csvPauseReason = csvPauseReason;
        }
        
        public boolean isLogParserPaused() {
            return logParserPaused;
        }
        
        public void setLogParserPaused(boolean logParserPaused) {
            this.logParserPaused = logParserPaused;
        }
        
        public String getLogPauseReason() {
            return logPauseReason;
        }
        
        public void setLogPauseReason(String logPauseReason) {
            this.logPauseReason = logPauseReason;
        }
        
        public boolean isHistoricalParsingEnabled() {
            return historicalParsingEnabled;
        }
        
        public void setHistoricalParsingEnabled(boolean historicalParsingEnabled) {
            this.historicalParsingEnabled = historicalParsingEnabled;
        }
        
        /**
         * Clear all parser memory for this server
         * This is used when transitioning between historical and regular parsing
         * to ensure a clean state.
         */
        public void clearParserMemory() {
            parserMemory.clear();
        }
        
        /**
         * Store a value in parser memory
         * @param key Memory key
         * @param value Memory value
         */
        public void storeInMemory(String key, Object value) {
            parserMemory.put(key, value);
        }
        
        /**
         * Retrieve a value from parser memory
         * @param key Memory key
         * @return Value or null if not found
         */
        public Object retrieveFromMemory(String key) {
            return parserMemory.get(key);
        }
    }
}