#!/bin/bash

# Script to fix ICON references in embeds that might be causing thumbnail issues
echo "Fixing icon references in EmbedUtils.java..."

# Replace references to KILLFEED_ICON (which doesn't exist) with KILLFEED_LOGO
sed -i 's/KILLFEED_ICON/KILLFEED_LOGO/g' ./com/deadside/bot/utils/EmbedUtils.java

echo "Adding timestamps to embeds that don't have them..."
sed -i 's/.setFooter("Server: " + serverName + " | discord.gg\/EmeraldServers | " + time)/.setFooter("Server: " + serverName + " | discord.gg\/EmeraldServers | " + time).setTimestamp(Instant.now())/' ./com/deadside/bot/utils/EmbedUtils.java

echo "Fixes applied!"