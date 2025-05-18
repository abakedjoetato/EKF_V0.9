package com.deadside.bot.db.repositories;

import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.models.Bounty;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for bounty operations
 */
public class BountyRepository {
    private static final Logger logger = LoggerFactory.getLogger(BountyRepository.class);
    private final MongoCollection<Bounty> bountyCollection;
    
    public BountyRepository() {
        this.bountyCollection = MongoDBConnection.getInstance()
                .getDatabase()
                .getCollection("bounties", Bounty.class);
    }
    
    /**
     * Save a new bounty
     */
    public void save(Bounty bounty) {
        try {
            bountyCollection.insertOne(bounty);
            logger.info("Saved new bounty on player {}", bounty.getTargetName());
        } catch (Exception e) {
            logger.error("Error saving bounty", e);
            throw e;
        }
    }
    
    /**
     * Update an existing bounty
     */
    public void update(Bounty bounty) {
        try {
            bountyCollection.replaceOne(
                    Filters.eq("_id", bounty.getId()),
                    bounty
            );
            logger.info("Updated bounty {}", bounty.getId());
        } catch (Exception e) {
            logger.error("Error updating bounty", e);
            throw e;
        }
    }
    
    /**
     * Find by ID
     */
    public Bounty findById(ObjectId id) {
        try {
            return bountyCollection.find(Filters.eq("_id", id)).first();
        } catch (Exception e) {
            logger.error("Error finding bounty by ID", e);
            return null;
        }
    }
    
    /**
     * Find all bounties for a guild
     */
    public List<Bounty> findByGuildId(long guildId) {
        try {
            FindIterable<Bounty> bounties = bountyCollection.find(
                    Filters.eq("guildId", guildId)
            );
            
            List<Bounty> result = new ArrayList<>();
            for (Bounty bounty : bounties) {
                result.add(bounty);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error finding bounties for guild {}", guildId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find active bounties for a target player
     */
    public List<Bounty> findActiveByTargetName(String targetName) {
        try {
            Bson filter = Filters.and(
                    Filters.eq("targetName", targetName),
                    Filters.eq("completed", false),
                    Filters.gt("expiryTimestamp", System.currentTimeMillis())
            );
            
            FindIterable<Bounty> bounties = bountyCollection.find(filter);
            
            List<Bounty> result = new ArrayList<>();
            for (Bounty bounty : bounties) {
                result.add(bounty);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error finding active bounties for player {}", targetName, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find active bounties for a guild
     */
    public List<Bounty> findActiveByGuildId(long guildId) {
        try {
            Bson filter = Filters.and(
                    Filters.eq("guildId", guildId),
                    Filters.eq("completed", false),
                    Filters.gt("expiryTimestamp", System.currentTimeMillis())
            );
            
            FindIterable<Bounty> bounties = bountyCollection.find(filter);
            
            List<Bounty> result = new ArrayList<>();
            for (Bounty bounty : bounties) {
                result.add(bounty);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error finding active bounties for guild {}", guildId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find expired bounties
     */
    public List<Bounty> findExpiredBounties() {
        try {
            Bson filter = Filters.and(
                    Filters.eq("completed", false),
                    Filters.lt("expiryTimestamp", System.currentTimeMillis())
            );
            
            FindIterable<Bounty> bounties = bountyCollection.find(filter);
            
            List<Bounty> result = new ArrayList<>();
            for (Bounty bounty : bounties) {
                result.add(bounty);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error finding expired bounties", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Delete a bounty
     */
    public void delete(Bounty bounty) {
        try {
            bountyCollection.deleteOne(Filters.eq("_id", bounty.getId()));
            logger.info("Deleted bounty {}", bounty.getId());
        } catch (Exception e) {
            logger.error("Error deleting bounty", e);
            throw e;
        }
    }
    
    /**
     * Delete many bounties
     */
    public long deleteMany(List<Bounty> bounties) {
        try {
            List<ObjectId> ids = new ArrayList<>();
            for (Bounty bounty : bounties) {
                ids.add(bounty.getId());
            }
            
            return bountyCollection.deleteMany(Filters.in("_id", ids))
                    .getDeletedCount();
        } catch (Exception e) {
            logger.error("Error deleting multiple bounties", e);
            return 0;
        }
    }
    
    /**
     * Mark bounty as completed
     */
    public boolean completeBounty(Bounty bounty, String killerName) {
        try {
            bounty.complete(killerName);
            bountyCollection.updateOne(
                    Filters.eq("_id", bounty.getId()),
                    Updates.combine(
                            Updates.set("completed", true),
                            Updates.set("completedBy", killerName)
                    )
            );
            return true;
        } catch (Exception e) {
            logger.error("Error completing bounty", e);
            return false;
        }
    }
    
    /**
     * Count active bounties for a target
     */
    public long countActiveByTargetName(String targetName) {
        try {
            Bson filter = Filters.and(
                    Filters.eq("targetName", targetName),
                    Filters.eq("completed", false),
                    Filters.gt("expiryTimestamp", System.currentTimeMillis())
            );
            
            return bountyCollection.countDocuments(filter);
        } catch (Exception e) {
            logger.error("Error counting active bounties for player {}", targetName, e);
            return 0;
        }
    }
    
    /**
     * Count active bounties for a guild
     */
    public long countActiveByGuildId(long guildId) {
        try {
            Bson filter = Filters.and(
                    Filters.eq("guildId", guildId),
                    Filters.eq("completed", false),
                    Filters.gt("expiryTimestamp", System.currentTimeMillis())
            );
            
            return bountyCollection.countDocuments(filter);
        } catch (Exception e) {
            logger.error("Error counting active bounties for guild {}", guildId, e);
            return 0;
        }
    }
}