#!/bin/bash

echo "=== Setting up and starting Deadside Discord Bot ==="

# Install Java if not already installed
if ! command -v java &> /dev/null; then
  echo "Java not found. Installing Java..."
  # Install Java using nix-env which is available in Replit
  nix-env -i openjdk-17
  
  # Add Java to the PATH
  export PATH=$PATH:$(dirname $(nix-env -q --out-path openjdk-17 | cut -d' ' -f3))/bin
fi

# Verify Java is installed
if ! command -v java &> /dev/null; then
  echo "Failed to install Java. Please check the Replit configuration."
  exit 1
fi

echo "Java version:"
java -version

# Check environment variables
echo "Checking environment variables..."

if [ -z "$BOT_TOKEN" ]; then
  echo "ERROR: BOT_TOKEN environment variable is not set!"
  echo "Please add your Discord bot token in Replit Secrets."
  exit 1
fi

if [ -z "$MONGO_URI" ]; then
  echo "ERROR: MONGO_URI environment variable is not set!"
  echo "Please add your MongoDB connection string in Replit Secrets."
  exit 1
fi

# Make sure resource directories exist
mkdir -p com/deadside/bot/resources/images

# Copy logo images if they exist in attached_assets
if [ -d "attached_assets" ]; then
  echo "Copying logo images to resources directory..."
  cp -f attached_assets/*.png com/deadside/bot/resources/images/ 2>/dev/null || :
fi

# Build with Maven
echo "Building with Maven..."
mvn clean package -DskipTests

# Run the bot
echo "Starting the bot..."
if [ -f "target/deadside-discord-bot-1.0-SNAPSHOT.jar" ]; then
  java -jar target/deadside-discord-bot-1.0-SNAPSHOT.jar
elif [ -f "target/deadside-discord-bot-1.0-SNAPSHOT-shaded.jar" ]; then
  java -jar target/deadside-discord-bot-1.0-SNAPSHOT-shaded.jar
else
  echo "ERROR: Could not find JAR file to run. Make sure Maven build was successful."
  ls -la target/
  exit 1
fi