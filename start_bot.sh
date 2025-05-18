#!/bin/bash

# Make sure config file can be found
mkdir -p src/main/resources
cp -f config.properties src/main/resources/
cp -f .env src/main/resources/

# Ensure image resources are available
mkdir -p src/main/resources/images
cp -f attached_assets/*.png src/main/resources/images/ 2>/dev/null

# Build and run the bot
echo "Building and starting Deadside Discord Bot..."
mvn compile exec:java -Dexec.mainClass="com.deadside.bot.Main" -DskipTests=true -Dexec.cleanupDaemonThreads=false