package com.deadside.bot.db.repositories;

import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.models.LinkedPlayer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for LinkedPlayer collection
 */
public class LinkedPlayerRepository {
    private static final Logger logger = LoggerFactory.getLogger(LinkedPlayerRepository.class);
    private static final String COLLECTION_NAME = "linked_players";
    
    private MongoCollection<LinkedPlayer> collection;
    
    public LinkedPlayerRepository() {
        try {
            this.collection = MongoDBConnection.getInstance().getDatabase()
                .getCollection(COLLECTION_NAME, LinkedPlayer.class);
        } catch (IllegalStateException e) {
            // This can happen during early initialization - handle gracefully
            logger.warn("MongoDB connection not initialized yet. Usage will be deferred until initialization.");
        }
    }
    
    /**
     * Get the MongoDB collection, initializing if needed
     */
    private MongoCollection<LinkedPlayer> getCollection() {
        if (collection == null) {
            // Try to get the collection now that MongoDB should be initialized
            this.collection = MongoDBConnection.getInstance().getDatabase()
                .getCollection(COLLECTION_NAME, LinkedPlayer.class);
        }
        return collection;
    }
    
    /**
     * Find a linked player by Discord ID
     */
    public LinkedPlayer findByDiscordId(long discordId) {
        try {
            return getCollection().find(Filters.eq("discordId", discordId)).first();
        } catch (Exception e) {
            logger.error("Error finding linked player by Discord ID: {}", discordId, e);
            return null;
        }
    }
    
    /**
     * Find a linked player by in-game player ID (main or alt)
     */
    public LinkedPlayer findByPlayerId(String playerId) {
        try {
            // Check if it's a main player
            LinkedPlayer mainLink = getCollection().find(Filters.eq("mainPlayerId", playerId)).first();
            if (mainLink != null) {
                return mainLink;
            }
            
            // Check if it's an alt player
            return getCollection().find(Filters.in("altPlayerIds", playerId)).first();
        } catch (Exception e) {
            logger.error("Error finding linked player by player ID: {}", playerId, e);
            return null;
        }
    }
    
    /**
     * Find a linked player by in-game player ID (using ObjectId)
     * This is a convenience method that converts ObjectId to string
     */
    public LinkedPlayer findByPlayerId(org.bson.types.ObjectId playerId) {
        if (playerId == null) {
            return null;
        }
        return findByPlayerId(playerId.toString());
    }
    
    /**
     * Save or update a linked player
     */
    public void save(LinkedPlayer linkedPlayer) {
        try {
            linkedPlayer.setUpdated(System.currentTimeMillis());
            
            if (linkedPlayer.getId() == null) {
                getCollection().insertOne(linkedPlayer);
                logger.debug("Inserted new linked player for Discord ID: {}", linkedPlayer.getDiscordId());
            } else {
                Bson filter = Filters.eq("_id", linkedPlayer.getId());
                getCollection().replaceOne(filter, linkedPlayer);
                logger.debug("Updated linked player for Discord ID: {}", linkedPlayer.getDiscordId());
            }
        } catch (Exception e) {
            logger.error("Error saving linked player: {}", linkedPlayer.getDiscordId(), e);
        }
    }
    
    /**
     * Delete a linked player
     */
    public void delete(LinkedPlayer linkedPlayer) {
        try {
            if (linkedPlayer.getId() != null) {
                Bson filter = Filters.eq("_id", linkedPlayer.getId());
                getCollection().deleteOne(filter);
                logger.debug("Deleted linked player for Discord ID: {}", linkedPlayer.getDiscordId());
            }
        } catch (Exception e) {
            logger.error("Error deleting linked player: {}", linkedPlayer.getDiscordId(), e);
        }
    }
    
    /**
     * Find all linked players
     */
    public List<LinkedPlayer> findAll() {
        try {
            return getCollection().find().into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding all linked players", e);
            return new ArrayList<>();
        }
    }
}