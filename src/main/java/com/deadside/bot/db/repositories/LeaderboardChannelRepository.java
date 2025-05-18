package com.deadside.bot.db.repositories;

import com.deadside.bot.db.MongoDBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for managing auto-updating leaderboard channels
 */
public class LeaderboardChannelRepository {
    private static final Logger logger = LoggerFactory.getLogger(LeaderboardChannelRepository.class);
    private static final String COLLECTION_NAME = "leaderboard_channels";
    
    private MongoCollection<Document> collection;
    
    public LeaderboardChannelRepository() {
        try {
            this.collection = MongoDBConnection.getInstance().getDatabase()
                .getCollection(COLLECTION_NAME);
        } catch (IllegalStateException e) {
            // This can happen during early initialization - handle gracefully
            logger.warn("MongoDB connection not initialized yet. Usage will be deferred until initialization.");
        }
    }
    
    /**
     * Get the MongoDB collection, initializing if needed
     */
    private MongoCollection<Document> getCollection() {
        if (collection == null) {
            // Try to get the collection now that MongoDB should be initialized
            this.collection = MongoDBConnection.getInstance().getDatabase()
                .getCollection(COLLECTION_NAME);
        }
        return collection;
    }
    
    /**
     * Save or update a leaderboard channel configuration
     */
    public void saveLeaderboardChannel(long guildId, long channelId) {
        try {
            Bson filter = Filters.eq("guildId", guildId);
            Bson update = Updates.combine(
                Updates.set("channelId", channelId),
                Updates.set("lastUpdated", System.currentTimeMillis())
            );
            
            UpdateOptions options = new UpdateOptions().upsert(true);
            getCollection().updateOne(filter, update, options);
            
            logger.info("Saved leaderboard channel: Guild={}, Channel={}", guildId, channelId);
        } catch (Exception e) {
            logger.error("Error saving leaderboard channel", e);
        }
    }
    
    /**
     * Get a leaderboard channel for a specific guild
     */
    public Long getLeaderboardChannel(long guildId) {
        try {
            Document doc = getCollection().find(Filters.eq("guildId", guildId)).first();
            return doc != null ? doc.getLong("channelId") : null;
        } catch (Exception e) {
            logger.error("Error getting leaderboard channel for guild: {}", guildId, e);
            return null;
        }
    }
    
    /**
     * Get all leaderboard channel configurations
     */
    public List<Map<String, Object>> getAllLeaderboardChannels() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            getCollection().find().forEach(doc -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("guildId", doc.getLong("guildId"));
                entry.put("channelId", doc.getLong("channelId"));
                entry.put("lastUpdated", doc.getLong("lastUpdated"));
                result.add(entry);
            });
        } catch (Exception e) {
            logger.error("Error getting all leaderboard channels", e);
        }
        
        return result;
    }
    
    /**
     * Delete a leaderboard channel configuration
     */
    public void deleteLeaderboardChannel(long guildId) {
        try {
            getCollection().deleteOne(Filters.eq("guildId", guildId));
            logger.info("Deleted leaderboard channel for guild: {}", guildId);
        } catch (Exception e) {
            logger.error("Error deleting leaderboard channel for guild: {}", guildId, e);
        }
    }
}