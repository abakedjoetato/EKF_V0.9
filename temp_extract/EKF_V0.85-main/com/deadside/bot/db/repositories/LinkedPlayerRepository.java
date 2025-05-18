package com.deadside.bot.db.repositories;

import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.models.LinkedPlayer;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for linked player operations
 */
public class LinkedPlayerRepository {
    private static final Logger logger = LoggerFactory.getLogger(LinkedPlayerRepository.class);
    private final MongoCollection<LinkedPlayer> linkedPlayerCollection;

    public LinkedPlayerRepository() {
        this.linkedPlayerCollection = MongoDBConnection.getInstance()
                .getDatabase()
                .getCollection("linked_players", LinkedPlayer.class);
    }

    /**
     * Save a new linked player
     */
    public void save(LinkedPlayer linkedPlayer) {
        try {
            linkedPlayerCollection.insertOne(linkedPlayer);
            logger.info("Saved new linked player: {}", linkedPlayer);
        } catch (Exception e) {
            logger.error("Error saving linked player", e);
            throw e;
        }
    }

    /**
     * Update an existing linked player
     */
    public void update(LinkedPlayer linkedPlayer) {
        try {
            linkedPlayerCollection.replaceOne(
                    Filters.eq("_id", linkedPlayer.getId()),
                    linkedPlayer
            );
            logger.info("Updated linked player: {}", linkedPlayer);
        } catch (Exception e) {
            logger.error("Error updating linked player", e);
            throw e;
        }
    }

    /**
     * Find by ID
     */
    public LinkedPlayer findById(ObjectId id) {
        try {
            return linkedPlayerCollection.find(Filters.eq("_id", id)).first();
        } catch (Exception e) {
            logger.error("Error finding linked player by ID", e);
            return null;
        }
    }

    /**
     * Find by Discord ID and Guild ID
     */
    public LinkedPlayer findByDiscordIdAndGuildId(long discordId, long guildId) {
        try {
            Bson filter = Filters.and(
                    Filters.eq("discordId", discordId),
                    Filters.eq("guildId", guildId)
            );
            return linkedPlayerCollection.find(filter).first();
        } catch (Exception e) {
            logger.error("Error finding linked player by Discord ID and Guild ID", e);
            return null;
        }
    }

    /**
     * Find by game name (case-insensitive)
     */
    public LinkedPlayer findByGameName(String gameName) {
        try {
            // Use a regex for case-insensitive search
            Bson filter = Filters.regex("gameName", "^" + gameName + "$", "i");
            return linkedPlayerCollection.find(filter).first();
        } catch (Exception e) {
            logger.error("Error finding linked player by game name", e);
            return null;
        }
    }

    /**
     * Find all links for a guild
     */
    public List<LinkedPlayer> findByGuildId(long guildId) {
        try {
            FindIterable<LinkedPlayer> links = linkedPlayerCollection.find(
                    Filters.eq("guildId", guildId)
            );
            
            List<LinkedPlayer> result = new ArrayList<>();
            for (LinkedPlayer link : links) {
                result.add(link);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error finding linked players for guild {}", guildId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Delete a linked player
     */
    public void delete(LinkedPlayer linkedPlayer) {
        try {
            linkedPlayerCollection.deleteOne(Filters.eq("_id", linkedPlayer.getId()));
            logger.info("Deleted linked player: {}", linkedPlayer);
        } catch (Exception e) {
            logger.error("Error deleting linked player", e);
            throw e;
        }
    }

    /**
     * Delete by Discord ID and Guild ID
     */
    public boolean deleteByDiscordIdAndGuildId(long discordId, long guildId) {
        try {
            Bson filter = Filters.and(
                    Filters.eq("discordId", discordId),
                    Filters.eq("guildId", guildId)
            );
            return linkedPlayerCollection.deleteOne(filter).getDeletedCount() > 0;
        } catch (Exception e) {
            logger.error("Error deleting linked player by Discord ID and Guild ID", e);
            return false;
        }
    }
}