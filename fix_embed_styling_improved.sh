#!/bin/bash

echo "Starting comprehensive embed styling fix..."

# Fix missing import for Instant in EmbedUtils.java
echo "Fixing imports in EmbedUtils.java..."
grep -q "import java.time.Instant;" ./com/deadside/bot/utils/EmbedUtils.java
if [ $? -ne 0 ]; then
  sed -i '/import java.awt.Color;/a import java.time.Instant;' ./com/deadside/bot/utils/EmbedUtils.java
  echo "Added Instant import to EmbedUtils"
fi

# Create a custom script to fix all embeds to use proper logo references
echo "Updating all embeds to use proper logo references..."

# Replace KILLFEED_ICON with KILLFEED_LOGO
find ./com -type f -name "*.java" -exec sed -i 's/KILLFEED_ICON/KILLFEED_LOGO/g' {} \;

# Fix any remaining embeds that don't use ResourceManager
find ./com -type f -name "*.java" -exec grep -l "setThumbnail" {} \; | while read file; do
  # Update direct URL thumbnails to use ResourceManager
  sed -i 's/\.setThumbnail("https:\/\/i\.imgur\.com\/[a-zA-Z0-9]*\.png")/\.setThumbnail(ResourceManager.getAttachmentString(ResourceManager.MAIN_LOGO))/g' "$file"
done

# Ensure all embeds include timestamp
find ./com -type f -name "*.java" -exec grep -l "EmbedBuilder" {} \; | while read file; do
  # Add timestamp to embeds that don't have it (careful with this - only applies where missing)
  grep -q "setTimestamp" "$file" || sed -i 's/\.setFooter(".*")\.build();/\.setFooter("&").setTimestamp(Instant.now()).build();/g' "$file"
done

# Fix footer text to be consistent
echo "Standardizing footer text..."
find ./com -type f -name "*.java" -exec sed -i 's/Powered by Emerald Servers/Powered by Discord.gg\/EmeraldServers/g' {} \;
find ./com -type f -name "*.java" -exec sed -i 's/Powered By Emerald Servers/Powered by Discord.gg\/EmeraldServers/g' {} \;

echo "Embed styling fixes complete!"