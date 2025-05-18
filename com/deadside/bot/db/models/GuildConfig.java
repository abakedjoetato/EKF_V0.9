package com.deadside.bot.db.models;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Model for storing Discord guild/server configuration
 */
public class GuildConfig {
    private static final Logger logger = LoggerFactory.getLogger(GuildConfig.class);
    
    private ObjectId id;
    private long guildId;
    private boolean premium;
    private long premiumUntil;
    private String killfeedChannelId;
    private String eventChannelId;
    private String logChannelId;
    private Map<String, String> settings;
    private long createdAt;
    private long updatedAt;
    
    /**
     * Constructor for creating a new guild config
     * @param guildId The Discord guild ID
     */
    public GuildConfig(long guildId) {
        this.id = new ObjectId();
        this.guildId = guildId;
        this.premium = false;
        this.premiumUntil = 0;
        this.settings = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Default constructor (required for MongoDB)
     */
    public GuildConfig() {
        this.settings = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Getters and setters
    
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    public long getGuildId() {
        return guildId;
    }
    
    public void setGuildId(long guildId) {
        this.guildId = guildId;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean isPremium() {
        return premium;
    }
    
    public void setPremium(boolean premium) {
        this.premium = premium;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getPremiumUntil() {
        return premiumUntil;
    }
    
    public void setPremiumUntil(long premiumUntil) {
        this.premiumUntil = premiumUntil;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getKillfeedChannelId() {
        return killfeedChannelId;
    }
    
    public void setKillfeedChannelId(String killfeedChannelId) {
        this.killfeedChannelId = killfeedChannelId;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getEventChannelId() {
        return eventChannelId;
    }
    
    public void setEventChannelId(String eventChannelId) {
        this.eventChannelId = eventChannelId;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getLogChannelId() {
        return logChannelId;
    }
    
    public void setLogChannelId(String logChannelId) {
        this.logChannelId = logChannelId;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Map<String, String> getSettings() {
        return settings;
    }
    
    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Get a specific setting
     * @param key The setting key
     * @param defaultValue The default value if the setting is not found
     * @return The setting value or the default value
     */
    public String getSetting(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }
    
    /**
     * Set a specific setting
     * @param key The setting key
     * @param value The setting value
     */
    public void setSetting(String key, String value) {
        settings.put(key, value);
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "GuildConfig{" +
                "id=" + id +
                ", guildId=" + guildId +
                ", premium=" + premium +
                ", premiumUntil=" + premiumUntil +
                ", settings=" + settings +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}