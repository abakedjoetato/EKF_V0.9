package com.deadside.bot.db.repositories;

import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.models.GuildConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for GuildConfig operations
 */
public class GuildConfigRepository {
    private static final Logger logger = LoggerFactory.getLogger(GuildConfigRepository.class);
    private final MongoCollection<Document> collection;
    
    /**
     * Constructor
     */
    public GuildConfigRepository() {
        this.collection = MongoDBConnection.getDatabase().getCollection("guild_configs");
    }
    
    /**
     * Find a guild config by ID
     * @param id The ObjectId of the guild config
     * @return The GuildConfig or null if not found
     */
    public GuildConfig findById(ObjectId id) {
        Document doc = collection.find(Filters.eq("_id", id)).first();
        return doc != null ? documentToGuildConfig(doc) : null;
    }
    
    /**
     * Find a guild config by Discord guild ID
     * @param guildId The Discord guild ID
     * @return The GuildConfig or null if not found
     */
    public GuildConfig findByGuildId(long guildId) {
        Document doc = collection.find(Filters.eq("guildId", guildId)).first();
        return doc != null ? documentToGuildConfig(doc) : null;
    }
    
    /**
     * Find all premium guild configs
     * @return List of premium guild configs
     */
    public List<GuildConfig> findAllPremium() {
        List<GuildConfig> result = new ArrayList<>();
        collection.find(Filters.eq("premium", true)).forEach(doc -> result.add(documentToGuildConfig(doc)));
        return result;
    }
    
    /**
     * Save a guild config
     * @param guildConfig The guild config to save
     */
    public void save(GuildConfig guildConfig) {
        Document doc = guildConfigToDocument(guildConfig);
        
        if (guildConfig.getId() == null) {
            guildConfig.setId(new ObjectId());
            doc.put("_id", guildConfig.getId());
        }
        
        collection.replaceOne(
                Filters.eq("_id", guildConfig.getId()),
                doc,
                MongoDBConnection.UPSERT_OPTION
        );
        
        logger.debug("Saved guild config: {}", guildConfig.getId());
    }
    
    /**
     * Update a specific setting for a guild
     * @param guildId The Discord guild ID
     * @param key The setting key
     * @param value The setting value
     */
    public void updateSetting(long guildId, String key, String value) {
        collection.updateOne(
                Filters.eq("guildId", guildId),
                Updates.set("settings." + key, value)
        );
        
        logger.debug("Updated setting {} to {} for guild {}", key, value, guildId);
    }
    
    /**
     * Delete a guild config
     * @param guildConfig The guild config to delete
     */
    public void delete(GuildConfig guildConfig) {
        collection.deleteOne(Filters.eq("_id", guildConfig.getId()));
        logger.debug("Deleted guild config: {}", guildConfig.getId());
    }
    
    /**
     * Delete a guild config by Discord guild ID
     * @param guildId The Discord guild ID
     */
    public void deleteByGuildId(long guildId) {
        collection.deleteOne(Filters.eq("guildId", guildId));
        logger.debug("Deleted guild config for guild: {}", guildId);
    }
    
    /**
     * Convert a MongoDB Document to a GuildConfig object
     * @param doc The MongoDB Document
     * @return The GuildConfig object
     */
    private GuildConfig documentToGuildConfig(Document doc) {
        GuildConfig guildConfig = new GuildConfig();
        
        guildConfig.setId(doc.getObjectId("_id"));
        guildConfig.setGuildId(doc.getLong("guildId"));
        guildConfig.setPremium(doc.getBoolean("premium", false));
        guildConfig.setPremiumUntil(doc.getLong("premiumUntil", 0));
        
        if (doc.containsKey("killfeedChannelId")) {
            guildConfig.setKillfeedChannelId(doc.getString("killfeedChannelId"));
        }
        
        if (doc.containsKey("eventChannelId")) {
            guildConfig.setEventChannelId(doc.getString("eventChannelId"));
        }
        
        if (doc.containsKey("logChannelId")) {
            guildConfig.setLogChannelId(doc.getString("logChannelId"));
        }
        
        guildConfig.setCreatedAt(doc.getLong("createdAt", System.currentTimeMillis()));
        guildConfig.setUpdatedAt(doc.getLong("updatedAt", System.currentTimeMillis()));
        
        // Convert settings
        Map<String, String> settings = new HashMap<>();
        Document settingsDoc = doc.get("settings", Document.class);
        
        if (settingsDoc != null) {
            for (String key : settingsDoc.keySet()) {
                settings.put(key, settingsDoc.getString(key));
            }
        }
        
        guildConfig.setSettings(settings);
        
        return guildConfig;
    }
    
    /**
     * Convert a GuildConfig object to a MongoDB Document
     * @param guildConfig The GuildConfig object
     * @return The MongoDB Document
     */
    private Document guildConfigToDocument(GuildConfig guildConfig) {
        Document doc = new Document();
        
        doc.put("_id", guildConfig.getId());
        doc.put("guildId", guildConfig.getGuildId());
        doc.put("premium", guildConfig.isPremium());
        doc.put("premiumUntil", guildConfig.getPremiumUntil());
        
        if (guildConfig.getKillfeedChannelId() != null) {
            doc.put("killfeedChannelId", guildConfig.getKillfeedChannelId());
        }
        
        if (guildConfig.getEventChannelId() != null) {
            doc.put("eventChannelId", guildConfig.getEventChannelId());
        }
        
        if (guildConfig.getLogChannelId() != null) {
            doc.put("logChannelId", guildConfig.getLogChannelId());
        }
        
        doc.put("createdAt", guildConfig.getCreatedAt());
        doc.put("updatedAt", guildConfig.getUpdatedAt());
        
        // Convert settings
        Document settingsDoc = new Document();
        for (Map.Entry<String, String> entry : guildConfig.getSettings().entrySet()) {
            settingsDoc.put(entry.getKey(), entry.getValue());
        }
        
        doc.put("settings", settingsDoc);
        
        return doc;
    }
}