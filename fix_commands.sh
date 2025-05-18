#!/bin/bash

# Find all command files in src directory
COMMAND_FILES=$(find ./src/main/java/com/deadside/bot/commands -name "*.java" | grep -v "CommandManager.java\|ICommand.java\|Command.java")

# Create a summary file
echo "Command Response Fix Summary" > fix_summary.txt
echo "=========================" >> fix_summary.txt
echo "" >> fix_summary.txt

# Check each command file
for file in $COMMAND_FILES; do
  echo "Analyzing: $file"
  
  # Check if execute method exists
  if grep -q "public void execute(SlashCommandInteractionEvent event)" "$file"; then
    # Check if file has database operations
    if grep -q "Repository\|findBy\|save\|find.*ById\|deleteBy\|update" "$file"; then
      echo "  - Has database operations ✓"
      
      # Check if deferReply exists
      if grep -q "deferReply()" "$file"; then
        echo "  - Has deferReply() ✓"
      else
        echo "  - Missing deferReply() ×"
        echo "$file: Missing deferReply() but has database operations" >> fix_summary.txt
      fi
      
      # Check if any event.reply calls after potential defer
      REPLIES=$(grep -n "event.reply" "$file" | grep -v "deferReply")
      if [ ! -z "$REPLIES" ]; then
        echo "  - Has reply calls that might need to be changed to getHook() ×"
        echo "$file: Has $(echo "$REPLIES" | wc -l) reply calls that might need to be changed to getHook()" >> fix_summary.txt
        echo "$REPLIES" >> fix_summary.txt
        echo "" >> fix_summary.txt
      fi
    else
      echo "  - No database operations, deferReply not required"
    fi
  else
    echo "  - No execute method, not a command handler"
  fi
  
  echo ""
done

echo "Analysis complete. See fix_summary.txt for a detailed report."
cat fix_summary.txt
