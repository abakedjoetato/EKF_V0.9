#!/bin/bash

# Simple CSV parser test without needing Java compilation
echo "Testing CSV pattern matching on sample file"

# Define our sample file
CSV_FILE="./attached_assets/2025.05.15-00.00.00.csv"

# Function to check if a line contains a kill (not suicide or relocation)
is_kill() {
  local line="$1"
  local killer=$(echo "$line" | cut -d';' -f2)
  local victim=$(echo "$line" | cut -d';' -f4)
  local weapon=$(echo "$line" | cut -d';' -f6)
  
  if [ "$killer" = "$victim" ]; then
    return 1  # It's a suicide
  fi
  
  if [ "$weapon" = "suicide_by_relocation" ] || [ "$weapon" = "falling" ]; then
    return 1  # It's a relocation or falling death
  fi
  
  return 0  # It's a kill
}

# Count total lines, kills, and special deaths
TOTAL=0
KILLS=0
SUICIDES=0
RELOCATIONS=0
FALLING=0

# Process each line
while IFS= read -r line; do
  TOTAL=$((TOTAL + 1))
  
  killer=$(echo "$line" | cut -d';' -f2)
  victim=$(echo "$line" | cut -d';' -f4)
  weapon=$(echo "$line" | cut -d';' -f6)
  distance=$(echo "$line" | cut -d';' -f7)
  
  if [ "$killer" = "$victim" ]; then
    if [ "$weapon" = "falling" ]; then
      FALLING=$((FALLING + 1))
      echo "FALL: $victim died from falling"
    elif [ "$weapon" = "suicide_by_relocation" ]; then
      RELOCATIONS=$((RELOCATIONS + 1))
      echo "RELOC: $victim relocated"
    else
      SUICIDES=$((SUICIDES + 1))
      echo "SUICIDE: $victim died by $weapon"
    fi
  else
    KILLS=$((KILLS + 1))
    echo "KILL: $killer killed $victim with $weapon from ${distance}m"
  fi
  
done < "$CSV_FILE"

# Print summary
echo ""
echo "Summary:"
echo "Total lines: $TOTAL"
echo "Normal kills: $KILLS"
echo "Suicides: $SUICIDES"
echo "Relocations: $RELOCATIONS"
echo "Falling deaths: $FALLING"