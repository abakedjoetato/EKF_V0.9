package com.deadside.bot.schedulers;

import com.deadside.bot.db.models.Bounty;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.KillRecord;
import com.deadside.bot.db.repositories.BountyRepository;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.KillRecordRepository;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Scheduler for bounty management tasks
 */
public class BountyScheduler {
    private static final Logger logger = LoggerFactory.getLogger(BountyScheduler.class);
    private final BountyRepository bountyRepository;
    private final GameServerRepository gameServerRepository;
    private final KillRecordRepository killRecordRepository;
    private final JDA jda;
    
    // Bounty configuration
    private static final long MIN_BOUNTY_AMOUNT = 500;
    private static final long MAX_BOUNTY_AMOUNT = 10000;
    
    // Kill thresholds for AI bounties
    private static final int[] KILL_THRESHOLDS = {3, 5, 7, 10};
    
    // Difficulty levels for scaling bounty amounts
    private static final String[] DIFFICULTY_LEVELS = {"easy", "moderate", "hard", "elite"};
    
    // Scaling factors for bounty amounts based on difficulty
    private static final double[] DIFFICULTY_SCALING = {0.25, 0.5, 0.75, 1.0};
    
    // Emerald color for bounty notifications
    private static final Color EMERALD_COLOR = new Color(46, 204, 113);
    
    public BountyScheduler(JDA jda) {
        this.jda = jda;
        this.bountyRepository = new BountyRepository();
        this.gameServerRepository = new GameServerRepository();
        this.killRecordRepository = new KillRecordRepository();
    }
    
    /**
     * Process cleanup of expired bounties
     * Runs every 10 minutes
     */
    public void cleanupExpiredBounties() {
        try {
            List<Bounty> expiredBounties = bountyRepository.findExpiredBounties();
            
            if (!expiredBounties.isEmpty()) {
                long deletedCount = bountyRepository.deleteMany(expiredBounties);
                logger.info("Cleaned up {} expired bounties", deletedCount);
                
                // Notify about expired bounties in server channels
                Map<Long, List<Bounty>> guildBounties = groupBountiesByGuild(expiredBounties);
                for (Map.Entry<Long, List<Bounty>> entry : guildBounties.entrySet()) {
                    notifyExpiredBounties(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired bounties", e);
        }
    }
    
    /**
     * Generate AI bounties based on recent kills
     * Runs once per hour
     */
    public void generateAiBounties() {
        try {
            // Get all game servers
            List<GameServer> servers = gameServerRepository.findAll();
            
            for (GameServer server : servers) {
                long guildId = server.getGuildId();
                
                // Skip if no bounty channel configured
                if (server.getBountyChannelId() == 0) {
                    continue;
                }
                
                // Get kills from the last hour
                long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
                List<KillRecord> recentKills = killRecordRepository.findByServerSince(server.getName(), oneHourAgo);
                
                // Count kills by player
                Map<String, Integer> killCounts = new HashMap<>();
                for (KillRecord kill : recentKills) {
                    String killer = kill.getKiller();
                    killCounts.put(killer, killCounts.getOrDefault(killer, 0) + 1);
                }
                
                // Find top killers matching thresholds
                List<Bounty> generatedBounties = new ArrayList<>();
                
                for (Map.Entry<String, Integer> entry : killCounts.entrySet()) {
                    String playerName = entry.getKey();
                    int kills = entry.getValue();
                    
                    // Skip if player already has an active bounty
                    if (bountyRepository.countActiveByTargetName(playerName) > 0) {
                        continue;
                    }
                    
                    // Check against thresholds and generate appropriate bounty
                    for (int i = KILL_THRESHOLDS.length - 1; i >= 0; i--) {
                        if (kills >= KILL_THRESHOLDS[i]) {
                            // Generate bounty amount based on difficulty
                            long bountyAmount = calculateBountyAmount(i);
                            
                            // Create the bounty
                            Bounty bounty = new Bounty(playerName, bountyAmount, guildId);
                            bountyRepository.save(bounty);
                            generatedBounties.add(bounty);
                            
                            // Only generate one bounty per player (highest matching threshold)
                            break;
                        }
                    }
                }
                
                // Send notifications for generated bounties
                if (!generatedBounties.isEmpty()) {
                    logger.info("Generated {} AI bounties for guild {}", generatedBounties.size(), guildId);
                    notifyNewAiBounties(guildId, generatedBounties, server.getBountyChannelId());
                }
            }
        } catch (Exception e) {
            logger.error("Error generating AI bounties", e);
        }
    }
    
    /**
     * Calculate bounty amount based on difficulty level
     */
    private long calculateBountyAmount(int difficultyIndex) {
        double range = MAX_BOUNTY_AMOUNT - MIN_BOUNTY_AMOUNT;
        double scaling = DIFFICULTY_SCALING[difficultyIndex];
        
        // Base amount from min with random portion based on difficulty
        double baseAmount = MIN_BOUNTY_AMOUNT + (range * scaling * 0.7);
        
        // Add some randomness (up to 30% of scaled range)
        double randomPortion = range * scaling * 0.3 * ThreadLocalRandom.current().nextDouble();
        
        return Math.round(baseAmount + randomPortion);
    }
    
    /**
     * Group bounties by guild for notification
     */
    private Map<Long, List<Bounty>> groupBountiesByGuild(List<Bounty> bounties) {
        Map<Long, List<Bounty>> result = new HashMap<>();
        
        for (Bounty bounty : bounties) {
            long guildId = bounty.getGuildId();
            if (!result.containsKey(guildId)) {
                result.put(guildId, new ArrayList<>());
            }
            result.get(guildId).add(bounty);
        }
        
        return result;
    }
    
    /**
     * Notify about expired bounties
     */
    private void notifyExpiredBounties(long guildId, List<Bounty> bounties) {
        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) return;
            
            // Get all game servers for this guild
            List<GameServer> servers = gameServerRepository.findByGuildId(guildId);
            if (servers.isEmpty()) return;
            
            GameServer server = servers.get(0);
            TextChannel bountyChannel = guild.getTextChannelById(server.getBountyChannelId());
            if (bountyChannel == null) return;
            
            // Group AI and player bounties
            List<Bounty> aiBounties = new ArrayList<>();
            List<Bounty> playerBounties = new ArrayList<>();
            
            for (Bounty bounty : bounties) {
                if (bounty.isAiGenerated()) {
                    aiBounties.add(bounty);
                } else {
                    playerBounties.add(bounty);
                }
            }
            
            // Notify about expired AI bounties
            if (!aiBounties.isEmpty()) {
                StringBuilder description = new StringBuilder("The following AI-generated bounties have expired:\n\n");
                for (Bounty bounty : aiBounties) {
                    description.append("• **").append(bounty.getTargetName()).append("** - ")
                            .append(bounty.getAmount()).append(" coins\n");
                }
                
                bountyChannel.sendMessageEmbeds(EmbedUtils.customEmbed(
                        "🏆 Bounty Hunter Update: Targets Escaped",
                        description.toString(),
                        EMERALD_COLOR
                )).queue();
            }
            
            // Notify about expired player bounties
            if (!playerBounties.isEmpty()) {
                StringBuilder description = new StringBuilder("The following player-set bounties have expired:\n\n");
                for (Bounty bounty : playerBounties) {
                    description.append("• **").append(bounty.getTargetName()).append("** - ")
                            .append(bounty.getAmount()).append(" coins (set by <@")
                            .append(bounty.getIssuerId()).append(">)\n");
                }
                
                bountyChannel.sendMessageEmbeds(EmbedUtils.customEmbed(
                        "🏆 Bounty Hunter Update: Contracts Void",
                        description.toString(),
                        EMERALD_COLOR
                )).queue();
            }
        } catch (Exception e) {
            logger.error("Error notifying about expired bounties", e);
        }
    }
    
    /**
     * Notify about new AI-generated bounties
     */
    private void notifyNewAiBounties(long guildId, List<Bounty> bounties, long channelId) {
        try {
            Guild guild = jda.getGuildById(guildId);
            if (guild == null) return;
            
            TextChannel bountyChannel = guild.getTextChannelById(channelId);
            if (bountyChannel == null) return;
            
            StringBuilder description = new StringBuilder("The following players have new bounties on their heads:\n\n");
            
            for (Bounty bounty : bounties) {
                int difficultyIndex = -1;
                long amount = bounty.getAmount();
                
                // Determine the difficulty level based on amount
                for (int i = 0; i < DIFFICULTY_SCALING.length; i++) {
                    double minForLevel = MIN_BOUNTY_AMOUNT + (MAX_BOUNTY_AMOUNT - MIN_BOUNTY_AMOUNT) * DIFFICULTY_SCALING[i] * 0.7;
                    if (amount >= minForLevel) {
                        difficultyIndex = i;
                    }
                }
                
                String difficulty = difficultyIndex >= 0 ? DIFFICULTY_LEVELS[difficultyIndex] : "unknown";
                
                description.append("• **").append(bounty.getTargetName()).append("** - ")
                        .append(bounty.getAmount()).append(" coins (")
                        .append(difficulty).append(" difficulty)\n")
                        .append("  Expires in: ").append(bounty.getTimeLeftFormatted()).append("\n\n");
            }
            
            description.append("*Hunt them down to claim your reward!*");
            
            bountyChannel.sendMessageEmbeds(EmbedUtils.customEmbed(
                    "🏆 New Bounty Contracts Available",
                    description.toString(),
                    EMERALD_COLOR
            )).queue();
        } catch (Exception e) {
            logger.error("Error notifying about new AI bounties", e);
        }
    }
}