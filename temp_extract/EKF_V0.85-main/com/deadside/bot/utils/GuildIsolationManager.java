package com.deadside.bot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages isolation between different guilds (Discord servers)
 * Ensures that data from one guild doesn't affect another
 */
public class GuildIsolationManager {
    private static final Logger logger = LoggerFactory.getLogger(GuildIsolationManager.class);
    private static GuildIsolationManager instance;
    
    // Map of guild ID to any guild-specific caches or states
    private final Map<Long, GuildCache> guildCaches = new ConcurrentHashMap<>();
    
    private GuildIsolationManager() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance
     */
    public static synchronized GuildIsolationManager getInstance() {
        if (instance == null) {
            instance = new GuildIsolationManager();
        }
        return instance;
    }
    
    /**
     * Get the cache for a guild
     * @param guildId The guild ID
     * @return The guild cache
     */
    public GuildCache getCache(long guildId) {
        return guildCaches.computeIfAbsent(guildId, k -> new GuildCache());
    }
    
    /**
     * Clear the cache for a guild
     * @param guildId The guild ID
     */
    public void clearCache(long guildId) {
        guildCaches.remove(guildId);
        logger.info("Cleared cache for guild {}", guildId);
    }
    
    /**
     * Class representing cached data for a guild
     */
    public static class GuildCache {
        // Example cached data - we can expand this as needed
        private Map<String, Object> serverCache = new ConcurrentHashMap<>();
        private Map<String, Object> playerCache = new ConcurrentHashMap<>();
        
        /**
         * Get an item from the server cache
         * @param key The cache key
         * @return The cached value
         */
        public Object getServerCacheItem(String key) {
            return serverCache.get(key);
        }
        
        /**
         * Put an item in the server cache
         * @param key The cache key
         * @param value The value to cache
         */
        public void putServerCacheItem(String key, Object value) {
            serverCache.put(key, value);
        }
        
        /**
         * Get an item from the player cache
         * @param key The cache key
         * @return The cached value
         */
        public Object getPlayerCacheItem(String key) {
            return playerCache.get(key);
        }
        
        /**
         * Put an item in the player cache
         * @param key The cache key
         * @param value The value to cache
         */
        public void putPlayerCacheItem(String key, Object value) {
            playerCache.put(key, value);
        }
        
        /**
         * Clear the server cache
         */
        public void clearServerCache() {
            serverCache.clear();
        }
        
        /**
         * Clear the player cache
         */
        public void clearPlayerCache() {
            playerCache.clear();
        }
        
        /**
         * Clear all caches
         */
        public void clearAll() {
            clearServerCache();
            clearPlayerCache();
        }
    }
}