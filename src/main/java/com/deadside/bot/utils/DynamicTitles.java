package com.deadside.bot.utils;

import java.util.Random;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to provide dynamic titles and descriptions for killfeed and other embeds
 */
public class DynamicTitles {
    private static final Random random = new Random();
    
    // Killfeed titles - No emojis
    private static final String[] KILLFEED_TITLES = {
        "Deadside Killfeed",
        "Combat Report",
        "Battlefield Update",
        "Elimination Feed",
        "Deadside Elimination"
    };
    
    // Suicide titles - No emojis
    private static final String[] SUICIDE_TITLES = {
        "Deadside Suicide",
        "Self-Elimination",
        "Early Departure",
        "Unfortunate Accident"
    };
    
    // Falling death titles - No emojis
    private static final String[] FALLING_TITLES = {
        "Fatal Fall",
        "Gravity Wins Again",
        "Falling Mishap",
        "Deadly Descent"
    };
    
    // Leaderboard titles - No emojis
    private static final String[] LEADERBOARD_TITLES = {
        "Deadside Leaderboard",
        "Top Survivors",
        "Elite Survivors",
        "Deadside Elite"
    };
    
    // Airdrop titles - No emojis
    private static final String[] AIRDROP_TITLES = {
        "Airdrop Inbound",
        "Supply Drop",
        "Care Package",
        "Emergency Supplies"
    };
    
    // Mission titles - No emojis
    private static final String[] MISSION_TITLES = {
        "Mission Alert",
        "Special Assignment",
        "Contract Available",
        "Elite Operation"
    };
    
    // Helicrash titles - No emojis
    private static final String[] HELICRASH_TITLES = {
        "Helicopter Crash",
        "Crash Site Located",
        "Downed Aircraft",
        "Wreckage Spotted"
    };
    
    // Dynamic descriptions for killfeed
    private static final String[] KILLFEED_DESCRIPTIONS = {
        "{killer} eliminated {victim} with {weapon} from {distance}m",
        "{killer} took down {victim} using {weapon} at {distance}m",
        "{killer} dispatched {victim} with a {weapon} from {distance}m away",
        "{killer} neutralized {victim} with precision ({weapon}, {distance}m)",
        "{victim} was eliminated by {killer} ({weapon}, {distance}m)"
    };
    
    // Dynamic descriptions for suicides
    private static final String[] SUICIDE_DESCRIPTIONS = {
        "{player} decided to check out early",
        "{player} chose a different path",
        "{player} had an unfortunate accident",
        "{player} took matters into their own hands",
        "{player} found a quick way back to the menu",
        "{player} decided to respawn elsewhere"
    };
    
    // Dynamic descriptions for falling deaths
    private static final String[] FALLING_DESCRIPTIONS = {
        "{player} didn't stick the landing ({height}m)",
        "{player} discovered gravity is still working ({height}m fall)",
        "{player} misjudged the height ({height}m)",
        "{player} took a leap of faith... it didn't work out ({height}m)",
        "Gravity: 1, {player}: 0 (Fell {height}m)"
    };
    
    // Dynamic descriptions for leaderboards
    private static final String[] LEADERBOARD_DESCRIPTIONS = {
        "The most feared survivors in the wasteland",
        "Deadside's most dangerous players",
        "Those who've proven their worth in combat",
        "The elite of Deadside's brutal environment"
    };
    
    // Dynamic descriptions for bounty kills - No emojis
    private static final String[] BOUNTY_TITLES = {
        "Bounty Collected",
        "Target Eliminated",
        "Contract Complete",
        "Bounty Claimed"
    };
    
    /**
     * Get a random killfeed title
     */
    public static String getKillfeedTitle() {
        return KILLFEED_TITLES[random.nextInt(KILLFEED_TITLES.length)];
    }
    
    /**
     * Get a random suicide title
     */
    public static String getSuicideTitle() {
        return SUICIDE_TITLES[random.nextInt(SUICIDE_TITLES.length)];
    }
    
    /**
     * Get a random falling death title
     */
    public static String getFallingTitle() {
        return FALLING_TITLES[random.nextInt(FALLING_TITLES.length)];
    }
    
    /**
     * Get a random leaderboard title
     */
    public static String getLeaderboardTitle() {
        return LEADERBOARD_TITLES[random.nextInt(LEADERBOARD_TITLES.length)];
    }
    
    /**
     * Get a random bounty title
     */
    public static String getBountyTitle() {
        return BOUNTY_TITLES[random.nextInt(BOUNTY_TITLES.length)];
    }
    
    /**
     * Get a formatted killfeed description with player and weapon details
     */
    public static String getKillfeedDescription(String killer, String victim, String weapon, int distance) {
        String template = KILLFEED_DESCRIPTIONS[random.nextInt(KILLFEED_DESCRIPTIONS.length)];
        return template
            .replace("{killer}", "**" + killer + "**")
            .replace("{victim}", "**" + victim + "**")
            .replace("{weapon}", "**" + weapon + "**")
            .replace("{distance}", String.valueOf(distance));
    }
    
    /**
     * Get a formatted suicide description
     */
    public static String getSuicideDescription(String player) {
        String template = SUICIDE_DESCRIPTIONS[random.nextInt(SUICIDE_DESCRIPTIONS.length)];
        return template.replace("{player}", "**" + player + "**");
    }
    
    /**
     * Get a formatted falling death description
     */
    public static String getFallingDescription(String player, int height) {
        String template = FALLING_DESCRIPTIONS[random.nextInt(FALLING_DESCRIPTIONS.length)];
        return template
            .replace("{player}", "**" + player + "**")
            .replace("{height}", String.valueOf(height));
    }
    
    /**
     * Get a random leaderboard description
     */
    public static String getLeaderboardDescription() {
        return LEADERBOARD_DESCRIPTIONS[random.nextInt(LEADERBOARD_DESCRIPTIONS.length)];
    }
    
    /**
     * Get a random airdrop title
     */
    public static String getAirdropTitle() {
        return AIRDROP_TITLES[random.nextInt(AIRDROP_TITLES.length)];
    }
    
    /**
     * Get a random mission title
     */
    public static String getMissionTitle() {
        return MISSION_TITLES[random.nextInt(MISSION_TITLES.length)];
    }
    
    /**
     * Get a random helicrash title
     */
    public static String getHelicrashTitle() {
        return HELICRASH_TITLES[random.nextInt(HELICRASH_TITLES.length)];
    }
}