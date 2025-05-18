import re
import os

# The updated pattern matching our actual CSV format
CSV_LINE_PATTERN = r"(\d{4}\.\d{2}\.\d{2}-\d{2}\.\d{2}\.\d{2});([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);"

def test_csv_parser():
    """Test the CSV parser with our sample file"""
    csv_file = "attached_assets/2025.05.15-00.00.00.csv"
    
    if not os.path.exists(csv_file):
        print(f"File not found: {csv_file}")
        
        # Check if there's a similar file in the attached_assets directory
        for filename in os.listdir("attached_assets"):
            if filename.endswith(".csv"):
                print(f"Found alternative CSV file: {filename}")
                csv_file = os.path.join("attached_assets", filename)
                break
    
    print(f"Testing CSV parser with file: {csv_file}")
    
    try:
        with open(csv_file, 'r') as f:
            lines = f.readlines()
            
        line_count = 0
        matched_lines = 0
        player_kills = 0
        suicides = 0
        
        for line in lines:
            line_count += 1
            line = line.strip()
            if not line:
                continue
                
            match = re.match(CSV_LINE_PATTERN, line)
            if match:
                matched_lines += 1
                
                # Extract fields from CSV
                timestamp = match.group(1)
                killer = match.group(2)
                killer_id = match.group(3)
                victim = match.group(4)
                victim_id = match.group(5)
                weapon = match.group(6)
                distance = match.group(7)
                
                # Determine if this is a suicide or player kill
                is_suicide = killer == victim or \
                           "suicide" in weapon.lower() or \
                           "falling" in weapon.lower()
                
                if is_suicide:
                    suicides += 1
                    print(f"SUICIDE: {victim} died by {weapon}")
                else:
                    player_kills += 1
                    print(f"KILL: {killer} killed {victim} with {weapon} at {distance}m")
            else:
                print(f"Line {line_count} did NOT match pattern: {line}")
                # Print the first 50 characters for easier diagnosis
                print(f"First 50 chars: {line[:50]}...")
        
        # Print summary
        print("\nParsing Results:")
        print(f"Total lines: {line_count}")
        print(f"Matched lines: {matched_lines}")
        print(f"Player kills: {player_kills}")
        print(f"Suicides: {suicides}")
        print(f"Total deaths: {player_kills + suicides}")
        
    except Exception as e:
        print(f"Error processing file: {str(e)}")

if __name__ == "__main__":
    test_csv_parser()