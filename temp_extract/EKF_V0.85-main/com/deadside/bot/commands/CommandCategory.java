package com.deadside.bot.commands;

/**
 * Enum representing different command categories for organization and help display
 */
public enum CommandCategory {
    ADMIN("Admin Commands", "Commands for server administration"),
    ECONOMY("Economy Commands", "Commands for currency and trading"),
    STATS("Statistics Commands", "Commands for viewing player and server stats"),
    FACTION("Faction Commands", "Commands for faction management"),
    PLAYER("Player Commands", "Commands for player interaction"),
    INFO("Information Commands", "General information commands"),
    MISC("Miscellaneous", "Other miscellaneous commands");
    
    private final String title;
    private final String description;
    
    CommandCategory(String title, String description) {
        this.title = title;
        this.description = description;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
}