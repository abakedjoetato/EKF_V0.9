#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Color codes for better readability
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Building and Running Deadside Discord Bot ===${NC}"

# Step 1: Check environment variables
echo -e "${YELLOW}Checking environment variables...${NC}"
if [ -z "$BOT_TOKEN" ]; then
  echo -e "${RED}ERROR: BOT_TOKEN environment variable is not set!${NC}"
  echo -e "${YELLOW}Please provide your Discord bot token.${NC}"
  exit 1
fi

if [ -z "$MONGO_URI" ]; then
  echo -e "${RED}ERROR: MONGO_URI environment variable is not set!${NC}"
  echo -e "${YELLOW}Please provide your MongoDB connection URI.${NC}"
  exit 1
fi

# Step 2: Build the project
echo -e "${YELLOW}Building the project with Maven...${NC}"
# Use -T 1C for parallel builds with 1 thread per core
# Use -Dmaven.test.skip=true to skip tests
mvn clean package -Dmaven.test.skip=true

# Step 3: Run the bot
echo -e "${GREEN}Starting the Deadside Discord Bot...${NC}"

# Check for both JAR file versions
if [ -f "target/deadside-discord-bot-1.0-SNAPSHOT.jar" ]; then
  java -jar target/deadside-discord-bot-1.0-SNAPSHOT.jar
elif [ -f "target/deadside-discord-bot-1.0-SNAPSHOT-shaded.jar" ]; then
  java -jar target/deadside-discord-bot-1.0-SNAPSHOT-shaded.jar
else
  echo -e "${RED}ERROR: Could not find JAR file to run. Make sure Maven build was successful.${NC}"
  ls -la target/
  exit 1
fi