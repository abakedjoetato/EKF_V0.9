package com.deadside.bot.utils;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.GuildConfig;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.GuildConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class that enforces guild isolation and data separation
 * This ensures that data from one Discord server/guild cannot be accessed by another
 */
public class GuildIsolationManager {
    private static final Logger logger = LoggerFactory.getLogger(GuildIsolationManager.class);
    private static final GuildIsolationManager INSTANCE = new GuildIsolationManager();
    
    private final GuildConfigRepository guildConfigRepository;
    private final GameServerRepository gameServerRepository;
    
    // Cache guild configs to reduce database queries
    private final ConcurrentHashMap<Long, GuildConfig> guildConfigCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> lastGuildDataAccess = new ConcurrentHashMap<>();
    
    private GuildIsolationManager() {
        this.guildConfigRepository = new GuildConfigRepository();
        this.gameServerRepository = new GameServerRepository();
    }
    
    /**
     * Get the singleton instance
     */
    public static GuildIsolationManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initialize a guild if it doesn't exist
     * @param guildId The Discord guild ID
     * @return The guild configuration
     */
    public GuildConfig initializeGuild(long guildId) {
        GuildConfig guildConfig = guildConfigRepository.findByGuildId(guildId);
        
        if (guildConfig == null) {
            guildConfig = new GuildConfig(guildId);
            guildConfigRepository.save(guildConfig);
            logger.info("Initialized new guild with ID: {}", guildId);
        }
        
        // Update cache
        guildConfigCache.put(guildId, guildConfig);
        lastGuildDataAccess.put(guildId, System.currentTimeMillis());
        
        return guildConfig;
    }
    
    /**
     * Get guild configuration
     * @param guildId The Discord guild ID
     * @return The guild configuration
     */
    public GuildConfig getGuildConfig(long guildId) {
        // Check cache first
        GuildConfig cachedConfig = guildConfigCache.get(guildId);
        if (cachedConfig != null) {
            // Update last access time
            lastGuildDataAccess.put(guildId, System.currentTimeMillis());
            return cachedConfig;
        }
        
        // Get from database
        GuildConfig guildConfig = guildConfigRepository.findByGuildId(guildId);
        
        if (guildConfig == null) {
            // Initialize if it doesn't exist
            return initializeGuild(guildId);
        }
        
        // Update cache
        guildConfigCache.put(guildId, guildConfig);
        lastGuildDataAccess.put(guildId, System.currentTimeMillis());
        
        return guildConfig;
    }
    
    /**
     * Get servers for a guild
     * @param guildId The Discord guild ID
     * @return List of game servers for this guild
     */
    public List<GameServer> getServersForGuild(long guildId) {
        // Update last access time
        lastGuildDataAccess.put(guildId, System.currentTimeMillis());
        
        // Get servers from database
        return gameServerRepository.findAllByGuildId(guildId);
    }
    
    /**
     * Check if a server belongs to a guild
     * @param guildId The Discord guild ID
     * @param serverId The game server ID
     * @return True if the server belongs to this guild
     */
    public boolean isServerInGuild(long guildId, String serverId) {
        GameServer server = gameServerRepository.findById(serverId);
        return server != null && server.getGuildId() == guildId;
    }
    
    /**
     * Save guild configuration
     * @param guildConfig The guild configuration to save
     */
    public void saveGuildConfig(GuildConfig guildConfig) {
        guildConfigRepository.save(guildConfig);
        
        // Update cache
        guildConfigCache.put(guildConfig.getGuildId(), guildConfig);
        lastGuildDataAccess.put(guildConfig.getGuildId(), System.currentTimeMillis());
    }
    
    /**
     * Clear cache for a guild
     * @param guildId The Discord guild ID
     */
    public void clearCache(long guildId) {
        guildConfigCache.remove(guildId);
        lastGuildDataAccess.remove(guildId);
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        guildConfigCache.clear();
        lastGuildDataAccess.clear();
    }
    
    /**
     * Cleanup old cache entries
     * This should be run periodically to free memory
     * @param maxAgeMs Maximum age in milliseconds
     */
    public void cleanupCaches(long maxAgeMs) {
        long now = System.currentTimeMillis();
        lastGuildDataAccess.forEach((guildId, lastAccess) -> {
            if (now - lastAccess > maxAgeMs) {
                guildConfigCache.remove(guildId);
                lastGuildDataAccess.remove(guildId);
            }
        });
    }
    
    /**
     * Verify that a user can access guild data
     * This should be used to prevent users from accessing data from other guilds
     * @param guildId The Discord guild ID
     * @param userId The Discord user ID
     * @return True if the user is in the guild
     */
    public boolean canUserAccessGuild(long guildId, long userId) {
        // In a real implementation, this would check if the user is in the guild
        // For now, we'll assume they are if the guild exists
        return guildConfigRepository.findByGuildId(guildId) != null;
    }
}