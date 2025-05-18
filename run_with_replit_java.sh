#!/bin/bash

echo "Starting Deadside Discord Bot..."

# Find Java by looking in common Replit paths
for possible_java in "java" "/usr/bin/java" "/nix/store/*/java/bin/java" "/home/runner/.nix-profile/bin/java"; do
  if command -v ${possible_java} &> /dev/null; then
    JAVA_CMD=${possible_java}
    echo "Found Java at: ${JAVA_CMD}"
    break
  fi
done

# If Java not found in path, we'll rely on Replit's built-in Java
if [ -z "${JAVA_CMD}" ]; then
  echo "Java command not found in standard locations. Using mvn exec:java instead..."
  
  # Copy resource files if they exist
  if [ -d "attached_assets" ]; then
    mkdir -p src/main/resources/images
    cp -f attached_assets/*.png src/main/resources/images/ 2>/dev/null || true
  fi
  
  # Run through Maven exec plugin
  mvn exec:java -Dexec.mainClass="com.deadside.bot.Main"
else
  echo "Running with Java: ${JAVA_CMD} version:"
  ${JAVA_CMD} -version
  
  # Ensure the JAR file exists
  if [ -f "target/deadside-discord-bot-1.0-SNAPSHOT.jar" ]; then
    ${JAVA_CMD} -jar target/deadside-discord-bot-1.0-SNAPSHOT.jar
  else
    echo "JAR file not found. Building project first..."
    mvn clean package -DskipTests
    
    if [ -f "target/deadside-discord-bot-1.0-SNAPSHOT.jar" ]; then
      ${JAVA_CMD} -jar target/deadside-discord-bot-1.0-SNAPSHOT.jar
    else
      echo "Failed to create JAR file. Using mvn exec:java as fallback..."
      mvn exec:java -Dexec.mainClass="com.deadside.bot.Main"
    fi
  fi
fi