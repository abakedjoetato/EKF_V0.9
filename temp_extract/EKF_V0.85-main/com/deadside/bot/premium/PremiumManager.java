package com.deadside.bot.premium;

import com.deadside.bot.db.models.GuildConfig;
import com.deadside.bot.db.repositories.GuildConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for premium features and subscriptions
 * Handles enabling/disabling premium features based on guild subscription status
 */
public class PremiumManager {
    private static final Logger logger = LoggerFactory.getLogger(PremiumManager.class);
    private final GuildConfigRepository guildConfigRepository;
    
    public PremiumManager() {
        this.guildConfigRepository = new GuildConfigRepository();
    }
    
    /**
     * Check if a guild has premium status
     * @param guildId The Discord guild ID
     * @return True if the guild has an active premium subscription
     */
    public boolean hasPremium(long guildId) {
        try {
            GuildConfig guildConfig = guildConfigRepository.findByGuildId(guildId);
            
            if (guildConfig == null) {
                return false;
            }
            
            return guildConfig.isPremium();
        } catch (Exception e) {
            logger.error("Error checking premium status for guild ID: {}", guildId, e);
            return false;
        }
    }
    
    /**
     * Enable premium for a guild
     * @param guildId The Discord guild ID
     * @param durationDays Duration in days (0 for unlimited)
     */
    public void enablePremium(long guildId, int durationDays) {
        try {
            GuildConfig guildConfig = guildConfigRepository.findByGuildId(guildId);
            
            if (guildConfig == null) {
                guildConfig = new GuildConfig(guildId);
            }
            
            guildConfig.setPremium(true);
            
            // Set expiration time if duration is specified
            if (durationDays > 0) {
                long expirationTime = System.currentTimeMillis() + (durationDays * 24L * 60L * 60L * 1000L);
                guildConfig.setPremiumUntil(expirationTime);
                logger.info("Premium enabled for guild ID: {} for {} days (until {})", 
                        guildId, durationDays, new java.util.Date(expirationTime));
            } else {
                guildConfig.setPremiumUntil(0); // No expiration
                logger.info("Premium enabled for guild ID: {} with no expiration", guildId);
            }
            
            guildConfigRepository.save(guildConfig);
        } catch (Exception e) {
            logger.error("Error enabling premium for guild ID: {}", guildId, e);
        }
    }
    
    /**
     * Disable premium for a guild
     * @param guildId The Discord guild ID
     */
    public void disablePremium(long guildId) {
        try {
            GuildConfig guildConfig = guildConfigRepository.findByGuildId(guildId);
            
            if (guildConfig != null) {
                guildConfig.setPremium(false);
                guildConfig.setPremiumUntil(0);
                guildConfigRepository.save(guildConfig);
                logger.info("Premium disabled for guild ID: {}", guildId);
            }
        } catch (Exception e) {
            logger.error("Error disabling premium for guild ID: {}", guildId, e);
        }
    }
    
    /**
     * Check and update expired premium subscriptions
     */
    public void checkExpiredSubscriptions() {
        try {
            logger.debug("Checking for expired premium subscriptions");
            
            // Find all premium guilds
            for (GuildConfig config : guildConfigRepository.findAllPremium()) {
                if (config.getPremiumUntil() > 0 && config.getPremiumUntil() < System.currentTimeMillis()) {
                    // Premium has expired
                    config.setPremium(false);
                    guildConfigRepository.save(config);
                    
                    logger.info("Premium subscription expired for guild ID: {}", config.getGuildId());
                }
            }
        } catch (Exception e) {
            logger.error("Error checking expired premium subscriptions", e);
        }
    }
    
    /**
     * Get premium status details for a guild
     * @param guildId The Discord guild ID
     * @return A string with premium status information
     */
    public String getPremiumStatusDetails(long guildId) {
        try {
            GuildConfig guildConfig = guildConfigRepository.findByGuildId(guildId);
            
            if (guildConfig == null || !guildConfig.isPremium()) {
                return "No active premium subscription";
            }
            
            if (guildConfig.getPremiumUntil() == 0) {
                return "Premium subscription active (no expiration)";
            } else {
                long remaining = guildConfig.getPremiumUntil() - System.currentTimeMillis();
                if (remaining <= 0) {
                    return "Premium subscription expired";
                }
                
                // Calculate days remaining
                long daysRemaining = remaining / (24L * 60L * 60L * 1000L);
                return String.format("Premium subscription active (expires in %d days)", daysRemaining);
            }
        } catch (Exception e) {
            logger.error("Error getting premium status details for guild ID: {}", guildId, e);
            return "Error retrieving premium status";
        }
    }
    
    /**
     * Check if Tip4serv payment should be verified
     * Integrates with Tip4serv API to verify premium payments
     * @param guildId The Discord guild ID
     * @param userId The Discord user ID
     * @return True if payment is verified
     */
    public boolean verifyTip4servPayment(long guildId, long userId) {
        try {
            logger.info("Verifying Tip4serv payment for guild ID: {} and user ID: {}", guildId, userId);
            
            // Get configuration from environment or config file
            String tip4servApiKey = System.getenv("TIP4SERV_API_KEY");
            if (tip4servApiKey == null || tip4servApiKey.isEmpty()) {
                logger.warn("Tip4serv API key not configured. Payment verification will be skipped.");
                return false;
            }
            
            // Prepare HTTP client for Tip4serv API call
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().build();
            String apiUrl = "https://tip4serv.com/api/v2/payment/verify";
            
            // Create JSON payload for API request
            String jsonPayload = String.format(
                "{\"api_key\": \"%s\", \"guild_id\": \"%d\", \"user_id\": \"%d\"}", 
                tip4servApiKey, guildId, userId
            );
            
            // Build the request
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
                
            // Send the request and process the response
            java.net.http.HttpResponse<String> response = client.send(
                request, java.net.http.HttpResponse.BodyHandlers.ofString()
            );
            
            // Check response status
            if (response.statusCode() == 200) {
                // Parse JSON response
                // For production use, use a proper JSON parser like Jackson or Gson
                String responseBody = response.body();
                
                // Simple check for success in response (in production, use proper JSON parsing)
                if (responseBody.contains("\"success\":true")) {
                    logger.info("Tip4serv payment verified for guild ID: {} and user ID: {}", guildId, userId);
                    
                    // Enable premium for the guild (default 30 days)
                    enablePremium(guildId, 30);
                    return true;
                } else {
                    logger.info("Tip4serv payment verification failed for guild ID: {} and user ID: {}", 
                            guildId, userId);
                    return false;
                }
            } else {
                logger.error("Tip4serv API request failed with status code: {}", response.statusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error verifying Tip4serv payment", e);
            return false;
        }
    }
    
    /**
     * Process a webhook from Tip4serv
     * @param webhookData JSON data from Tip4serv webhook
     * @return True if webhook was processed successfully
     */
    public boolean processTip4servWebhook(String webhookData) {
        try {
            logger.info("Processing Tip4serv webhook: {}", webhookData);
            
            // For production, use a proper JSON parser like Jackson or Gson
            // Simple example parsing (would use proper JSON parsing in production)
            if (webhookData.contains("\"event\":\"payment_successful\"")) {
                // Extract guild_id and user_id from webhook data
                // This is simplified - use proper JSON parsing in production
                
                // Extract guild_id using regex
                java.util.regex.Pattern guildPattern = java.util.regex.Pattern.compile("\"guild_id\":\"(\\d+)\"");
                java.util.regex.Matcher guildMatcher = guildPattern.matcher(webhookData);
                
                // Extract user_id using regex
                java.util.regex.Pattern userPattern = java.util.regex.Pattern.compile("\"user_id\":\"(\\d+)\"");
                java.util.regex.Matcher userMatcher = userPattern.matcher(webhookData);
                
                if (guildMatcher.find() && userMatcher.find()) {
                    long guildId = Long.parseLong(guildMatcher.group(1));
                    long userId = Long.parseLong(userMatcher.group(1));
                    
                    logger.info("Payment webhook received for guild ID: {} and user ID: {}", guildId, userId);
                    
                    // Extract duration in days (default to 30 if not found)
                    int durationDays = 30;
                    java.util.regex.Pattern durationPattern = java.util.regex.Pattern.compile("\"duration_days\":(\\d+)");
                    java.util.regex.Matcher durationMatcher = durationPattern.matcher(webhookData);
                    
                    if (durationMatcher.find()) {
                        durationDays = Integer.parseInt(durationMatcher.group(1));
                    }
                    
                    // Enable premium for the guild
                    enablePremium(guildId, durationDays);
                    return true;
                } else {
                    logger.error("Could not extract guild or user ID from webhook data");
                    return false;
                }
            } else {
                logger.info("Ignoring non-payment webhook event");
                return true;
            }
        } catch (Exception e) {
            logger.error("Error processing Tip4serv webhook", e);
            return false;
        }
    }
}
