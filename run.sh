#!/bin/bash

# Make sure resource directories exist
mkdir -p com/deadside/bot/resources/images

# Copy logo images if they exist in attached_assets
if [ -d "attached_assets" ]; then
  echo "Copying logo images to resources directory..."
  cp -f attached_assets/*.png com/deadside/bot/resources/images/ 2>/dev/null || :
fi

# Find Java executable - Try multiple approaches
JAVA_CMD=""
if command -v java &> /dev/null; then
  JAVA_CMD="java"
elif command -v /nix/store/*/bin/java &> /dev/null; then
  JAVA_CMD=$(find /nix/store/ -name "java" -type f -executable 2>/dev/null | grep -v "jre" | sort -r | head -n 1)
elif [ -f "/home/runner/.local/share/nix/profiles/replit/profile/bin/java" ]; then
  JAVA_CMD="/home/runner/.local/share/nix/profiles/replit/profile/bin/java"
elif [ -f "/home/runner/.local/bin/java" ]; then
  JAVA_CMD="/home/runner/.local/bin/java"
fi

# Check if we found Java
if [ -z "$JAVA_CMD" ]; then
  echo "Java not found. Using Maven to run instead."
  
  # Compile and run the Discord bot using Maven
  echo "Compiling Deadside Discord Bot..."
  mvn compile

  echo "Starting Deadside Discord Bot..."
  echo "Please ensure you've set the DISCORD_TOKEN environment variable."
  mvn exec:java -Dexec.mainClass="com.deadside.bot.Main"
else
  echo "Using Java at: $JAVA_CMD"
  
  # Compile with Maven
  echo "Compiling Deadside Discord Bot..."
  mvn compile package -DskipTests
  
  # Run with the discovered Java
  echo "Starting Deadside Discord Bot..."
  echo "Please ensure you've set the DISCORD_TOKEN environment variable."
  $JAVA_CMD -jar target/deadside-discord-bot-1.0-SNAPSHOT.jar
fi