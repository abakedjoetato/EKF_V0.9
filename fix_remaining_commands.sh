#!/bin/bash

echo "==== TARGETED COMMAND FIX SCRIPT - PHASE 2 ===="
echo "Fixing remaining direct replies in command files..."

# Fix the ServerCommand
echo "Fixing ServerCommand..."
grep -n "event.reply" ./com/deadside/bot/commands/admin/ServerCommand.java
sed -i 's/event\.reply(/event.getHook().sendMessage(/g' ./com/deadside/bot/commands/admin/ServerCommand.java

# Fix the BankCommand
echo "Fixing BankCommand..."
grep -n "event.reply" ./com/deadside/bot/commands/economy/BankCommand.java
sed -i 's/event\.reply(/event.getHook().sendMessage(/g' ./com/deadside/bot/commands/economy/BankCommand.java

# Fix the SlotCommand
echo "Fixing SlotCommand..."
grep -n "event.reply" ./com/deadside/bot/commands/economy/SlotCommand.java
sed -i 's/event\.reply(/event.getHook().sendMessage(/g' ./com/deadside/bot/commands/economy/SlotCommand.java

# Fix the BlackjackCommand
echo "Fixing BlackjackCommand..."
grep -n "event.reply" ./com/deadside/bot/commands/economy/BlackjackCommand.java
sed -i 's/event\.reply(/event.getHook().sendMessage(/g' ./com/deadside/bot/commands/economy/BlackjackCommand.java

# Fix the LinkCommand
echo "Fixing LinkCommand..."
grep -n "event.reply" ./com/deadside/bot/commands/player/LinkCommand.java
sed -i 's/event\.reply(/event.getHook().sendMessage(/g' ./com/deadside/bot/commands/player/LinkCommand.java

echo ""
echo "==== VERIFYING FIXES ===="
echo "Re-checking for any remaining direct replies..."

# Recheck for direct replies
REMAINING=$(find ./com/deadside/bot/commands -name "*.java" -exec grep -l "event\.reply" {} \; | wc -l)
if [ "$REMAINING" -gt 0 ]; then
    echo "⚠️ Found $REMAINING files with remaining direct replies that need manual fixes"
    find ./com/deadside/bot/commands -name "*.java" -exec grep -l "event\.reply" {} \; | head -5
else
    echo "✓ All direct replies appear to be fixed"
fi

echo ""
echo "Phase 2 fixes complete!"
echo "====================================="
