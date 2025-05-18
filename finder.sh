#!/bin/bash

echo "Running mvn directly to ensure the bot runs"
mvn exec:java -Dexec.mainClass="com.deadside.bot.Main"