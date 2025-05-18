#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Color codes for better readability
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Starting Deadside Discord Bot ===${NC}"

# Step 1: Find Java executable
echo -e "${YELLOW}Locating Java...${NC}"
JAVA_CMD=""

# Try different locations for Java
for possible_java in $(which java 2>/dev/null) \
                     /usr/bin/java \
                     /usr/local/bin/java \
                     /home/runner/.local/share/nix/profiles/replit/profile/bin/java \
                     /home/runner/.local/bin/java \
                     /nix/store/*/bin/java; do
  if [ -x "$possible_java" ] 2>/dev/null; then
    JAVA_CMD="$possible_java"
    break
  fi
done

if [ -z "$JAVA_CMD" ]; then
  echo -e "${RED}Java not found. Please make sure Java is installed.${NC}"
  exit 1
fi

echo -e "${GREEN}Found Java at: $JAVA_CMD${NC}"

# Step 2: Check environment variables
echo -e "${YELLOW}Checking environment variables...${NC}"
if [ -z "$BOT_TOKEN" ]; then
  echo -e "${RED}BOT_TOKEN environment variable is not set!${NC}"
  echo -e "${YELLOW}Please add your Discord bot token in Replit Secrets.${NC}"
  exit 1
fi

if [ -z "$MONGO_URI" ]; then
  echo -e "${RED}MONGO_URI environment variable is not set!${NC}"
  echo -e "${YELLOW}Please add your MongoDB URI in Replit Secrets.${NC}"
  exit 1
fi

# Step 3: Make sure resource directories exist
mkdir -p com/deadside/bot/resources/images

# Copy logo images if they exist in attached_assets
if [ -d "attached_assets" ]; then
  echo -e "${YELLOW}Copying logo images to resources directory...${NC}"
  cp -f attached_assets/*.png com/deadside/bot/resources/images/ 2>/dev/null || :
fi

# Step 4: Build the project
echo -e "${YELLOW}Building the project with Maven...${NC}"
mvn clean package -DskipTests

# Step 5: Run the bot
echo -e "${GREEN}Starting the Deadside Discord Bot...${NC}"

# Check for both JAR file versions
if [ -f "target/deadside-discord-bot-1.0-SNAPSHOT.jar" ]; then
  "$JAVA_CMD" -jar target/deadside-discord-bot-1.0-SNAPSHOT.jar
elif [ -f "target/deadside-discord-bot-1.0-SNAPSHOT-shaded.jar" ]; then
  "$JAVA_CMD" -jar target/deadside-discord-bot-1.0-SNAPSHOT-shaded.jar
else
  echo -e "${RED}ERROR: Could not find JAR file to run. Make sure Maven build was successful.${NC}"
  ls -la target/
  exit 1
fi