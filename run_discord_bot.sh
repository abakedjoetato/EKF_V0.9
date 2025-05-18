#!/bin/bash

# Make the target directories if they don't exist
mkdir -p target/classes

# Download the JDA library and its dependencies
echo "Downloading JDA and dependencies..."
mvn dependency:copy-dependencies -DoutputDirectory=target/lib

# Compile the simple bot
echo "Compiling Discord bot..."
javac -cp "target/lib/*" -d target/classes SimpleDiscordBot.java

# Run the bot
echo "Starting Discord bot..."
java -cp "target/classes:target/lib/*" SimpleDiscordBot