#!/bin/bash

echo "Downloading required JDA library..."
mkdir -p lib
wget -O lib/JDA.jar https://github.com/DV8FromTheWorld/JDA/releases/download/v5.0.0-beta.13/JDA-5.0.0-beta.13.jar

echo "Compiling AutocompleteDemoBot..."
javac -cp "lib/*" AutocompleteDemoBot.java

echo "Starting AutocompleteDemoBot..."
echo "Please ensure you've set the DISCORD_TOKEN environment variable."
java -cp ".:lib/*" AutocompleteDemoBot