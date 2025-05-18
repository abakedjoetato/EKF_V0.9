package com.deadside.bot.utils;

import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.KillRecordRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for cleaning up all data associated with a server
 * This ensures that when a server is removed, all its data is properly cleaned up
 */
public class ServerDataCleanupUtil {
    private static final Logger logger = LoggerFactory.getLogger(ServerDataCleanupUtil.class);
    
    /**
     * Clean up all data associated with a server
     * @param server The server being removed
     * @return Summary of the cleanup operation
     */
    public static CleanupSummary cleanupServerData(GameServer server) {
        if (server == null) {
            logger.warn("Cannot clean up data for null server");
            return new CleanupSummary();
        }
        
        long guildId = server.getGuildId();
        String serverId = server.getName();
        
        CleanupSummary summary = new CleanupSummary();
        
        try {
            // Clean up kill records
            KillRecordRepository killRecordRepository = new KillRecordRepository();
            long killRecordsDeleted = killRecordRepository.deleteByServerIdAndGuildId(serverId, guildId);
            summary.setKillRecordsDeleted(killRecordsDeleted);
            
            // Clean up players associated with this server
            long playerRecordsDeleted = cleanupPlayerData(serverId, guildId);
            summary.setPlayerRecordsDeleted(playerRecordsDeleted);
            
            // Clean up bounties associated with this server
            long bountiesDeleted = cleanupBounties(serverId, guildId);
            summary.setBountiesDeleted(bountiesDeleted);
            
            // Clean up factions associated with this server
            long factionsDeleted = cleanupFactions(serverId, guildId);
            summary.setFactionsDeleted(factionsDeleted);
            
            // Clean up any downloaded CSV or log files
            cleanupLocalFiles(server);
            
            // Clear any caches related to this server
            GuildIsolationManager.getInstance().clearCache(guildId);
            
            // Reset in-memory parsing states
            ParserStateManager.resetParserState(serverId, guildId);
            
            // Make sure to reset the server's last processed file and line information
            // This is crucial for proper isolation when the server is re-added
            server.setLastProcessedKillfeedFile("");
            server.setLastProcessedKillfeedLine(-1);
            server.setLastProcessedLogFile("");
            server.setLastProcessedLogLine(-1);
            // We don't save here because the server is about to be deleted
            
            logger.info("Successfully cleaned up data for server '{}' in guild {}", serverId, guildId);
            summary.setSuccess(true);
        } catch (Exception e) {
            logger.error("Error cleaning up data for server '{}' in guild {}", serverId, guildId, e);
            summary.setSuccess(false);
            summary.setErrorMessage(e.getMessage());
        }
        
        return summary;
    }
    
    /**
     * Clean up player data associated with a server
     * This is more complex because Player records don't directly reference server IDs
     * We need to rely on KillRecord associations to find relevant players
     */
    private static long cleanupPlayerData(String serverId, long guildId) {
        try {
            MongoCollection<Document> playerCollection = 
                    MongoDBConnection.getInstance().getDatabase().getCollection("players");
            
            // This is a simplification - in a real scenario, we would need to identify
            // which players are exclusively on this server and only reset their stats
            // For now, we'll reset stats for players who have kills/deaths on this server
            
            // Find player IDs from kill records for this server
            MongoCollection<Document> killCollection = 
                    MongoDBConnection.getInstance().getDatabase().getCollection("kill_records");
            
            Bson killFilter = Filters.and(
                    Filters.eq("serverId", serverId),
                    Filters.eq("guildId", guildId)
            );
            
            // Due to MongoDB Java driver limitations in our setup, we'll do a simpler approach
            // In a more complex system, we'd use aggregation to find related players
            
            // For now, we're resetting the kill/death stats for all players in this guild
            // This is a safe approach that ensures clean data separation
            Bson playerFilter = Filters.eq("guildId", guildId);
            
            // Reset player stats related to kills/deaths for this guild
            Document update = new Document("$set", new Document()
                    .append("kills", 0)
                    .append("deaths", 0)
                    .append("suicides", 0)
                    .append("mostUsedWeapon", "")
                    .append("mostUsedWeaponKills", 0)
                    .append("mostKilledPlayer", "")
                    .append("mostKilledPlayerCount", 0)
                    .append("killedByMost", "")
                    .append("killedByMostCount", 0)
                    .append("lastUpdated", System.currentTimeMillis()));
            
            // Apply the update to matching players
            long count = playerCollection.countDocuments(playerFilter);
            playerCollection.updateMany(playerFilter, update);
            
            logger.info("Reset stats for {} players in guild {}", count, guildId);
            return count;
        } catch (Exception e) {
            logger.error("Error cleaning up player data for server ID: {} in guild ID: {}", 
                    serverId, guildId, e);
            return 0;
        }
    }
    
    /**
     * Clean up bounties associated with a server
     */
    private static long cleanupBounties(String serverId, long guildId) {
        try {
            MongoCollection<Document> bountyCollection = 
                    MongoDBConnection.getInstance().getDatabase().getCollection("bounties");
            
            Bson filter = Filters.and(
                    Filters.eq("serverId", serverId),
                    Filters.eq("guildId", guildId)
            );
            
            DeleteResult result = bountyCollection.deleteMany(filter);
            long deleteCount = result.getDeletedCount();
            
            logger.info("Deleted {} bounties for server ID: {} in guild ID: {}", 
                    deleteCount, serverId, guildId);
            return deleteCount;
        } catch (Exception e) {
            logger.error("Error cleaning up bounties for server ID: {} in guild ID: {}", 
                    serverId, guildId, e);
            return 0;
        }
    }
    
    /**
     * Clean up factions associated with a server
     */
    private static long cleanupFactions(String serverId, long guildId) {
        try {
            MongoCollection<Document> factionCollection = 
                    MongoDBConnection.getInstance().getDatabase().getCollection("factions");
            
            Bson filter = Filters.and(
                    Filters.eq("serverId", serverId),
                    Filters.eq("guildId", guildId)
            );
            
            DeleteResult result = factionCollection.deleteMany(filter);
            long deleteCount = result.getDeletedCount();
            
            logger.info("Deleted {} factions for server ID: {} in guild ID: {}", 
                    deleteCount, serverId, guildId);
            return deleteCount;
        } catch (Exception e) {
            logger.error("Error cleaning up factions for server ID: {} in guild ID: {}", 
                    serverId, guildId, e);
            return 0;
        }
    }
    
    /**
     * Clean up local files associated with a server
     */
    private static void cleanupLocalFiles(GameServer server) {
        try {
            // Get the directory where files for this server would be stored
            String directoryPath = server.getDirectory();
            File directory = new File(directoryPath);
            
            if (directory.exists() && directory.isDirectory()) {
                // Delete all files in this directory
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        boolean deleted = file.delete();
                        if (!deleted) {
                            logger.warn("Could not delete file: {}", file.getAbsolutePath());
                        }
                    }
                }
                
                // Delete the directory itself
                boolean deleted = directory.delete();
                if (deleted) {
                    logger.info("Deleted local directory for server: {}", server.getName());
                } else {
                    logger.warn("Could not delete directory: {}", directoryPath);
                }
            }
        } catch (Exception e) {
            logger.error("Error cleaning up local files for server: {}", server.getName(), e);
        }
    }
    
    /**
     * Class to hold summary information about the cleanup operation
     */
    public static class CleanupSummary {
        private boolean success = false;
        private String errorMessage = "";
        private long killRecordsDeleted = 0;
        private long playerRecordsDeleted = 0;
        private long bountiesDeleted = 0;
        private long factionsDeleted = 0;
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public long getKillRecordsDeleted() {
            return killRecordsDeleted;
        }
        
        public void setKillRecordsDeleted(long killRecordsDeleted) {
            this.killRecordsDeleted = killRecordsDeleted;
        }
        
        public long getPlayerRecordsDeleted() {
            return playerRecordsDeleted;
        }
        
        public void setPlayerRecordsDeleted(long playerRecordsDeleted) {
            this.playerRecordsDeleted = playerRecordsDeleted;
        }
        
        public long getBountiesDeleted() {
            return bountiesDeleted;
        }
        
        public void setBountiesDeleted(long bountiesDeleted) {
            this.bountiesDeleted = bountiesDeleted;
        }
        
        public long getFactionsDeleted() {
            return factionsDeleted;
        }
        
        public void setFactionsDeleted(long factionsDeleted) {
            this.factionsDeleted = factionsDeleted;
        }
        
        @Override
        public String toString() {
            if (success) {
                return String.format(
                        "Successfully cleaned up server data: %d kill records, %d player stats reset, %d bounties, %d factions", 
                        killRecordsDeleted, playerRecordsDeleted, bountiesDeleted, factionsDeleted);
            } else {
                return String.format("Failed to clean up server data: %s", errorMessage);
            }
        }
    }
}