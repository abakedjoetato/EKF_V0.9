#!/bin/bash

# Set color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting Deadside Discord Bot...${NC}"

# Check for required environment variables
if [ -z "$BOT_TOKEN" ]; then
  echo -e "${RED}ERROR: BOT_TOKEN environment variable is not set!${NC}"
  exit 1
fi

if [ -z "$MONGO_URI" ]; then
  echo -e "${RED}ERROR: MONGO_URI environment variable is not set!${NC}"
  exit 1
fi

# Find Java path
JAVA_PATH=$(which java)

if [ -z "$JAVA_PATH" ]; then
  echo -e "${YELLOW}Java not found in PATH, searching for installation...${NC}"
  # Try to find Java from other common locations
  if [ -f "/nix/store/2vwkssqpzykk37r996cafq7x63imf4sp-openjdk-21+35/bin/java" ]; then
    JAVA_PATH="/nix/store/2vwkssqpzykk37r996cafq7x63imf4sp-openjdk-21+35/bin/java"
  elif [ -f "/usr/bin/java" ]; then
    JAVA_PATH="/usr/bin/java"
  elif [ -f "/usr/local/bin/java" ]; then
    JAVA_PATH="/usr/local/bin/java"
  fi
fi

if [ -z "$JAVA_PATH" ]; then
  echo -e "${RED}ERROR: Could not find Java installation. Please install Java.${NC}"
  exit 1
fi

echo -e "${GREEN}Using Java at: ${JAVA_PATH}${NC}"

# Check if the JAR file exists
if [ ! -f "target/deadside-discord-bot-1.0-SNAPSHOT.jar" ]; then
  echo -e "${YELLOW}JAR file not found. Running Maven build...${NC}"
  mvn clean package -Dmaven.test.skip=true
fi

echo -e "${GREEN}Running Deadside Discord Bot...${NC}"

# Check for both JAR file versions
if [ -f "target/deadside-discord-bot-1.0-SNAPSHOT.jar" ]; then
  $JAVA_PATH -jar target/deadside-discord-bot-1.0-SNAPSHOT.jar
elif [ -f "target/deadside-discord-bot-1.0-SNAPSHOT-shaded.jar" ]; then
  $JAVA_PATH -jar target/deadside-discord-bot-1.0-SNAPSHOT-shaded.jar
else
  echo -e "${RED}ERROR: Could not find JAR file to run. Make sure Maven build was successful.${NC}"
  exit 1
fi