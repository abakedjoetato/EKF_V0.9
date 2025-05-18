#!/bin/bash

# Check if DISCORD_TOKEN is available
if [ -z "$DISCORD_TOKEN" ]; then
    echo "ERROR: DISCORD_TOKEN environment variable is not set."
    echo "Please add your Discord token in the Secrets tab (lock icon) in Replit."
    exit 1
fi

echo "Discord token is available in the environment."
echo "Starting Discord bot in simplified mode..."

# Use Maven to compile and run a simplified bot
mvn compile exec:java -Dexec.mainClass="SimpleBotLoader" -DskipTests=true -Dexec.cleanupDaemonThreads=false

# If the bot fails to start, show an error message
if [ $? -ne 0 ]; then
    echo "Error: The Discord bot failed to start."
    echo "Please check the logs above for more details."
    exit 1
fi