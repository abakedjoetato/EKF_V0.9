package com.deadside.bot.db.models;

import org.bson.types.ObjectId;

/**
 * Represents a bounty placed on a player
 */
public class Bounty {
    private ObjectId id;
    private String targetName;      // Name of the player with the bounty
    private long amount;            // Amount of currency offered
    private long timestamp;         // When the bounty was placed
    private long expiryTimestamp;   // When the bounty expires
    private long guildId;           // Discord guild/server ID
    private long issuerId;          // Discord user ID of who placed the bounty
    private boolean isAiGenerated;  // Whether this was automatically generated
    private boolean completed;      // Whether this bounty has been claimed
    private String completedBy;     // Name of player who completed the bounty (if any)
    
    // Default constructor for MongoDB
    public Bounty() {
    }
    
    /**
     * Create a new player-set bounty
     */
    public Bounty(String targetName, long amount, long guildId, long issuerId) {
        this.id = new ObjectId();
        this.targetName = targetName;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
        this.expiryTimestamp = this.timestamp + (24 * 60 * 60 * 1000); // 24 hours
        this.guildId = guildId;
        this.issuerId = issuerId;
        this.isAiGenerated = false;
        this.completed = false;
    }
    
    /**
     * Create a new AI-generated bounty
     */
    public Bounty(String targetName, long amount, long guildId) {
        this.id = new ObjectId();
        this.targetName = targetName;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
        this.expiryTimestamp = this.timestamp + (60 * 60 * 1000); // 60 minutes
        this.guildId = guildId;
        this.issuerId = 0; // No issuer for AI bounties
        this.isAiGenerated = true;
        this.completed = false;
    }
    
    // Getters and setters
    
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    public String getTargetName() {
        return targetName;
    }
    
    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }
    
    public long getAmount() {
        return amount;
    }
    
    public void setAmount(long amount) {
        this.amount = amount;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getExpiryTimestamp() {
        return expiryTimestamp;
    }
    
    public void setExpiryTimestamp(long expiryTimestamp) {
        this.expiryTimestamp = expiryTimestamp;
    }
    
    public long getGuildId() {
        return guildId;
    }
    
    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }
    
    public long getIssuerId() {
        return issuerId;
    }
    
    public void setIssuerId(long issuerId) {
        this.issuerId = issuerId;
    }
    
    public boolean isAiGenerated() {
        return isAiGenerated;
    }
    
    public void setAiGenerated(boolean aiGenerated) {
        isAiGenerated = aiGenerated;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public String getCompletedBy() {
        return completedBy;
    }
    
    public void setCompletedBy(String completedBy) {
        this.completedBy = completedBy;
    }
    
    /**
     * Check if the bounty has expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTimestamp;
    }
    
    /**
     * Get the time left in milliseconds
     */
    public long getTimeLeft() {
        long currentTime = System.currentTimeMillis();
        if (currentTime > expiryTimestamp) {
            return 0;
        }
        return expiryTimestamp - currentTime;
    }
    
    /**
     * Get the time left in a readable format (hours, minutes)
     */
    public String getTimeLeftFormatted() {
        long timeLeft = getTimeLeft();
        if (timeLeft <= 0) {
            return "Expired";
        }
        
        long hours = timeLeft / (60 * 60 * 1000);
        long minutes = (timeLeft % (60 * 60 * 1000)) / (60 * 1000);
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    /**
     * Complete the bounty with the killer's name
     */
    public void complete(String killerName) {
        this.completed = true;
        this.completedBy = killerName;
    }
    
    @Override
    public String toString() {
        return "Bounty{" +
                "target='" + targetName + '\'' +
                ", amount=" + amount +
                ", expires=" + getTimeLeftFormatted() +
                (isAiGenerated ? ", AI-generated" : ", player-set") +
                '}';
    }
}