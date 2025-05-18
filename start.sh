#!/bin/bash
# Script to build and run the Deadside Discord Bot

echo "Building Deadside Discord Bot..."
mvn package -DskipTests

if [ $? -eq 0 ]; then
    echo "Build successful! Starting the bot..."
    mvn exec:java -Dexec.mainClass="com.deadside.bot.Main" -Dexec.cleanupDaemonThreads=false
else
    echo "Build failed. Please check the errors above."
    exit 1
fi