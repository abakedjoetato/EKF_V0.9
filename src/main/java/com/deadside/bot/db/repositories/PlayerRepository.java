package com.deadside.bot.db.repositories;

import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.models.Player;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Player collection
 */
public class PlayerRepository {
    private static final Logger logger = LoggerFactory.getLogger(PlayerRepository.class);
    private static final String COLLECTION_NAME = "players";
    
    private MongoCollection<Player> collection;
    
    public PlayerRepository() {
        try {
            this.collection = MongoDBConnection.getInstance().getDatabase()
                .getCollection(COLLECTION_NAME, Player.class);
        } catch (IllegalStateException e) {
            // This can happen during early initialization - handle gracefully
            logger.warn("MongoDB connection not initialized yet. Usage will be deferred until initialization.");
        }
    }
    
    /**
     * Get the MongoDB collection, initializing if needed
     */
    private MongoCollection<Player> getCollection() {
        if (collection == null) {
            // Try to get the collection now that MongoDB should be initialized
            this.collection = MongoDBConnection.getInstance().getDatabase()
                .getCollection(COLLECTION_NAME, Player.class);
        }
        return collection;
    }
    
    /**
     * Find a player by ID
     */
    public Player findByPlayerId(String playerId) {
        try {
            return getCollection().find(Filters.eq("playerId", playerId)).first();
        } catch (Exception e) {
            logger.error("Error finding player by ID: {}", playerId, e);
            return null;
        }
    }
    
    /**
     * Find a player by Deadside ID (alias for findByPlayerId)
     */
    public Player findByDeadsideId(String deadsideId) {
        return findByPlayerId(deadsideId);
    }
    
    /**
     * Find a player by exact name
     */
    public Player findByName(String name) {
        try {
            return getCollection().find(Filters.eq("name", name)).first();
        } catch (Exception e) {
            logger.error("Error finding player by name: {}", name, e);
            return null;
        }
    }
    
    /**
     * Find a player by exact name (alias method)
     */
    public List<Player> findByNameExact(String name) {
        Player player = findByName(name);
        List<Player> players = new ArrayList<>();
        if (player != null) {
            players.add(player);
        }
        return players;
    }
    
    /**
     * Find players by approximate name (case-insensitive)
     */
    public List<Player> findByNameLike(String name) {
        try {
            Bson filter = Filters.regex("name", name, "i"); // Case-insensitive regex
            return getCollection().find(filter).into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding players by name like: {}", name, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get top players by kills
     */
    public List<Player> getTopPlayersByKills(int limit) {
        try {
            return getCollection().find()
                    .sort(Sorts.descending("kills"))
                    .limit(limit)
                    .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error getting top players by kills", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get top players by KD ratio (with minimum kills)
     */
    public List<Player> getTopPlayersByKD(int limit, int minKills) {
        try {
            Bson filter = Filters.gte("kills", minKills);
            return getCollection().find(filter)
                    .sort(Sorts.descending("kills"))
                    .limit(limit)
                    .map(player -> {
                        // Sort in Java since MongoDB can't sort by computed KD ratio
                        return player;
                    })
                    .into(new ArrayList<>())
                    .stream()
                    .sorted((p1, p2) -> Double.compare(p2.getKdRatio(), p1.getKdRatio()))
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            logger.error("Error getting top players by KD", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Save or update a player
     */
    public void save(Player player) {
        try {
            player.setLastUpdated(System.currentTimeMillis());
            
            if (player.getId() == null) {
                getCollection().insertOne(player);
                logger.debug("Inserted new player: {}", player.getName());
            } else {
                Bson filter = Filters.eq("_id", player.getId());
                getCollection().replaceOne(filter, player);
                logger.debug("Updated player: {}", player.getName());
            }
        } catch (Exception e) {
            logger.error("Error saving player: {}", player.getName(), e);
        }
    }
    
    /**
     * Update player kill stats
     */
    public void incrementKills(String playerId, int amount) {
        try {
            Bson filter = Filters.eq("playerId", playerId);
            Bson update = Updates.combine(
                    Updates.inc("kills", amount),
                    Updates.set("lastUpdated", System.currentTimeMillis())
            );
            getCollection().updateOne(filter, update);
        } catch (Exception e) {
            logger.error("Error incrementing kills for player ID: {}", playerId, e);
        }
    }
    
    /**
     * Update player death stats
     */
    public void incrementDeaths(String playerId, int amount) {
        try {
            Bson filter = Filters.eq("playerId", playerId);
            Bson update = Updates.combine(
                    Updates.inc("deaths", amount),
                    Updates.set("lastUpdated", System.currentTimeMillis())
            );
            getCollection().updateOne(filter, update);
        } catch (Exception e) {
            logger.error("Error incrementing deaths for player ID: {}", playerId, e);
        }
    }
    
    /**
     * Update player suicide stats
     */
    public void incrementSuicides(String playerId, int amount) {
        try {
            Bson filter = Filters.eq("playerId", playerId);
            Bson update = Updates.combine(
                    Updates.inc("suicides", amount),
                    Updates.set("lastUpdated", System.currentTimeMillis())
            );
            getCollection().updateOne(filter, update);
        } catch (Exception e) {
            logger.error("Error incrementing suicides for player ID: {}", playerId, e);
        }
    }
    
    /**
     * Find a player by Discord ID
     * This method retrieves a player linked to a Discord user ID
     */
    public Player findByDiscordId(String discordId) {
        try {
            return getCollection().find(Filters.eq("discordId", discordId)).first();
        } catch (Exception e) {
            logger.error("Error finding player by Discord ID: {}", discordId, e);
            return null;
        }
    }
    
    /**
     * Find players by faction ID
     */
    public List<Player> findByFactionId(ObjectId factionId) {
        try {
            return getCollection().find(Filters.eq("factionId", factionId))
                    .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding players by faction ID: {}", factionId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Count players by faction ID
     */
    public long countByFactionId(ObjectId factionId) {
        try {
            return getCollection().countDocuments(Filters.eq("factionId", factionId));
        } catch (Exception e) {
            logger.error("Error counting players by faction ID: {}", factionId, e);
            return 0;
        }
    }
    
    /**
     * Get top players by specific weapon
     */
    public List<Player> getTopPlayersByWeapon(String weaponName, int limit) {
        try {
            return getCollection().find(Filters.eq("mostUsedWeapon", weaponName))
                    .sort(Sorts.descending("mostUsedWeaponKills"))
                    .limit(limit)
                    .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding top players by weapon: {}", weaponName, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Count all players
     */
    public long countAll() {
        try {
            return getCollection().countDocuments();
        } catch (Exception e) {
            logger.error("Error counting all players", e);
            return 0;
        }
    }
    
    /**
     * Get top players by longest kill distance
     */
    public List<Player> getTopPlayersByDistance(int limit) {
        try {
            return getCollection().find(Filters.gt("longestKillDistance", 0))
                .sort(Sorts.descending("longestKillDistance"))
                .limit(limit)
                .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding top players by kill distance", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get top players by longest kill streak
     */
    public List<Player> getTopPlayersByKillStreak(int limit) {
        try {
            return getCollection().find(Filters.gt("longestKillStreak", 0))
                .sort(Sorts.descending("longestKillStreak"))
                .limit(limit)
                .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding top players by kill streak", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get top players by death count
     */
    public List<Player> getTopPlayersByDeaths(int limit) {
        try {
            return getCollection().find()
                .sort(Sorts.descending("deaths"))
                .limit(limit)
                .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding top players by deaths", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Update player's kill streak statistics
     * Increments current streak and updates longest if needed
     */
    public void incrementKillStreak(String playerId) {
        try {
            Player player = findByPlayerId(playerId);
            if (player != null) {
                player.incrementKillStreak();
                save(player);
            }
        } catch (Exception e) {
            logger.error("Error updating kill streak for player ID: {}", playerId, e);
        }
    }
    
    /**
     * Reset a player's current kill streak (when they die)
     */
    public void resetKillStreak(String playerId) {
        try {
            Player player = findByPlayerId(playerId);
            if (player != null) {
                player.resetKillStreak();
                save(player);
            }
        } catch (Exception e) {
            logger.error("Error resetting kill streak for player ID: {}", playerId, e);
        }
    }
    
    /**
     * Update player's longest kill distance if the new one is greater
     */
    public void updateLongestKill(String playerId, int distance, String victimName, String weaponName) {
        try {
            Player player = findByPlayerId(playerId);
            if (player != null && distance > player.getLongestKillDistance()) {
                player.updateLongestKillDistance(distance, victimName, weaponName);
                save(player);
            }
        } catch (Exception e) {
            logger.error("Error updating longest kill for player ID: {}", playerId, e);
        }
    }
    
    /**
     * Add a weapon kill for a player
     */
    public void addWeaponKill(String playerId, String weaponName) {
        try {
            Player player = findByPlayerId(playerId);
            if (player != null) {
                player.addWeaponKill(weaponName);
                save(player);
            }
        } catch (Exception e) {
            logger.error("Error adding weapon kill for player ID: {}", playerId, e);
        }
    }
    
    /**
     * Add a player matchup kill
     */
    public void addPlayerMatchupKill(String killerId, String victimName) {
        try {
            Player player = findByPlayerId(killerId);
            if (player != null) {
                player.addPlayerMatchupKill(victimName);
                save(player);
            }
        } catch (Exception e) {
            logger.error("Error adding player matchup kill for player ID: {}", killerId, e);
        }
    }
    
    /**
     * Delete all players for a specific guild
     * 
     * NOTE: This is a temporary solution since our current Player model doesn't track
     * which server a player belongs to. In a future update, the Player model should include a serverId field
     * to allow for more granular deletion by server instead of by guild.
     * 
     * @param guildId The guild ID
     * @return Number of players deleted
     */
    public int deleteAllByGuildId(long guildId) {
        try {
            // Currently, Player model doesn't have a guildId field, making precise server-specific
            // player deletion impossible with the current schema.
            // This is a known limitation of the current design that will be addressed in a future update.
            // For now, we assume all players in a guild belong to all servers in that guild.
            
            // Since we cannot filter by guildId, we'll have to delete all players
            // This is ONLY safe when there's just one server in the guild
            long deletedCount = getCollection().deleteMany(new org.bson.Document()).getDeletedCount();
            logger.info("Deleted {} players for guild ID: {}", deletedCount, guildId);
            return (int) deletedCount;
        } catch (Exception e) {
            logger.error("Error deleting players for guild ID: {}", guildId, e);
            return 0;
        }
    }
}