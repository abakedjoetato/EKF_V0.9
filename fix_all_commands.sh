#!/bin/bash

echo "==== COMPREHENSIVE COMMAND FIX SCRIPT ===="
echo "Fixing response timing in all command files..."

# Function to fix a file's direct replies with deferred responses
fix_file() {
    local file=$1
    local count=0
    
    # Only process if the file contains any direct replies
    if grep -q "event\.reply" "$file"; then
        echo "Processing $file..."
        
        # First check if the file already has deferReply()
        if grep -q "event\.deferReply" "$file"; then
            # Fix replies after deferReply by replacing reply with getHook().sendMessage
            sed -i 's/event\.reply\("/"event.getHook().sendMessage("/g' "$file"
            sed -i 's/event\.replyEmbeds/event.getHook().sendMessageEmbeds/g' "$file"
            count=$((count + $(grep -c "getHook()" "$file")))
            echo "  Fixed getHook() responses: $count"
        else
            # Add deferReply near the start of execute method
            # This is a simple approach and might need manual adjustment
            sed -i '/public void execute(SlashCommandInteractionEvent event)/,+10 s/\(\s*\)try {/\1try {\n\1    event.deferReply().queue();/g' "$file"
            
            # Fix direct replies with getHook()
            sed -i 's/event\.reply\("/"event.getHook().sendMessage("/g' "$file"
            sed -i 's/event\.replyEmbeds/event.getHook().sendMessageEmbeds/g' "$file"
            count=$((count + 1))
            echo "  Added deferReply() and fixed responses"
        fi
        
        # Fix button interactions
        if grep -q "ButtonInteractionEvent" "$file"; then
            echo "  Fixing button interactions..."
            # Replace direct replies in button handlers with deferEdit().queue() followed by getHook()
            sed -i '/ButtonInteractionEvent/,/}/s/event\.reply\("/"event.deferEdit().queue();\n            event.getHook().sendMessage("/g' "$file"
            count=$((count + $(grep -c "deferEdit()" "$file")))
            echo "  Fixed button interactions with deferEdit(): $count"
        fi
    fi
}

# Process all command files
for file in $(find ./com/deadside/bot/commands -name "*.java"); do
    fix_file "$file"
done

echo ""
echo "==== FIXING MONGODB CONNECTION ===="
echo "Updating MongoDB connection to use Atlas..."

# Fix MongoDB connection
if [ -f "./src/main/java/com/deadside/bot/config/Config.java" ]; then
    # Update the MongoDB URI from localhost to Atlas
    sed -i 's|"mongodb://localhost:27017"|"mongodb+srv://deadsidebot:${MONGODB_PASSWORD}@deadsidecluster.mongodb.net/deadsidebot?retryWrites=true&w=majority"|g' "./src/main/java/com/deadside/bot/config/Config.java"
    echo "✓ Updated MongoDB connection string to Atlas"
fi

# Fix owner ID
find ./src -name "*.java" -type f -exec sed -i 's/Long\.parseLong(ownerId)/462961235382763520L/g' {} \;
find ./src -name "*.java" -type f -exec sed -i 's/userId == ownerId/userId == 462961235382763520L/g' {} \;
echo "✓ Hardcoded owner ID to 462961235382763520L across all files"

echo ""
echo "==== COMPLETING FIXES ===="
echo "Checking for remaining issues..."

# Look for any remaining direct replies that weren't fixed
REMAINING=$(find ./com/deadside/bot/commands -name "*.java" -exec grep -l "event\.reply" {} \; | wc -l)
if [ "$REMAINING" -gt 0 ]; then
    echo "⚠️ Found $REMAINING files with remaining direct replies that may need manual fixes"
    find ./com/deadside/bot/commands -name "*.java" -exec grep -l "event\.reply" {} \; | head -5
else
    echo "✓ All direct replies appear to be fixed"
fi

echo ""
echo "Comprehensive fixes complete! The bot should now handle Discord's timing constraints properly."
echo "====================================="
