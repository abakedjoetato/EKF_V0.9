#!/bin/bash

echo "Starting Deadside Discord Bot in Replit environment..."

# Build with Maven first, skipping tests
echo "Building bot with Maven..."
if ! mvn package -DskipTests; then
    echo "Maven build failed. Please check the error messages above."
    exit 1
fi

# Verify the jar exists
if [ ! -f "target/deadside-discord-bot-1.0-SNAPSHOT.jar" ]; then
    echo "JAR file not found after build. Build may have failed silently."
    exit 1
fi

# Start the bot
echo "Starting bot..."
java -jar target/deadside-discord-bot-1.0-SNAPSHOT.jar