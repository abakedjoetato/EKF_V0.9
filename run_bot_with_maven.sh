#!/bin/bash

echo "Starting Deadside Discord Bot with Maven..."

# Make sure resources directory exists and copy images
if [ -d "attached_assets" ]; then
  mkdir -p src/main/resources/images
  echo "Copying logo images to resources directory..."
  cp -f attached_assets/*.png src/main/resources/images/ 2>/dev/null || :
fi

# Compile and run with Maven
echo "Building and running Deadside Discord Bot..."
mvn clean compile exec:java -Dexec.mainClass="com.deadside.bot.Main"