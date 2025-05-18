package com.deadside.bot.db.models;

import org.bson.types.ObjectId;

/**
 * Represents a link between a Discord user and a Deadside player
 */
public class LinkedPlayer {
    private ObjectId id;
    private String gameName;      // Deadside player name
    private long discordId;       // Discord user ID
    private long guildId;         // Discord guild/server ID
    private long timestamp;       // When the link was created

    // Default constructor for MongoDB
    public LinkedPlayer() {
    }

    /**
     * Create a new player-discord link
     */
    public LinkedPlayer(String gameName, long discordId, long guildId) {
        this.id = new ObjectId();
        this.gameName = gameName;
        this.discordId = discordId;
        this.guildId = guildId;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    public String getGameName() {
        return gameName;
    }
    
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
    
    public long getDiscordId() {
        return discordId;
    }
    
    public void setDiscordId(long discordId) {
        this.discordId = discordId;
    }
    
    public long getGuildId() {
        return guildId;
    }
    
    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "LinkedPlayer{" +
                "gameName='" + gameName + '\'' +
                ", discordId=" + discordId +
                ", guildId=" + guildId +
                '}';
    }
}