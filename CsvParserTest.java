import com.deadside.bot.parsers.DeadsideCsvParser;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple test class to verify CSV parsing works with real data
 */
public class CsvParserTest {
    private static final Logger logger = LoggerFactory.getLogger(CsvParserTest.class);
    
    // Pattern for CSV line matching
    private static final Pattern CSV_LINE_PATTERN = Pattern.compile(
        "(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2});([^;]+);([^;]+);([^;]+);([^;]+);([^;]+);(\\d+);([^;]+);([^;]+);?"
    );

    public static void main(String[] args) throws IOException {
        String csvFilePath = "./attached_assets/2025.05.15-00.00.00.csv";
        System.out.println("Testing CSV parsing with file: " + csvFilePath);
        
        String content = new String(Files.readAllBytes(Paths.get(csvFilePath)));
        String[] lines = content.split("\\n");
        
        System.out.println("Found " + lines.length + " lines in file");
        
        int matchedLines = 0;
        int unmatchedLines = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            Matcher matcher = CSV_LINE_PATTERN.matcher(line);
            if (matcher.matches()) {
                matchedLines++;
                
                // Extract values
                String timestamp = matcher.group(1);
                String killer = matcher.group(2);
                String killerId = matcher.group(3);
                String victim = matcher.group(4);
                String victimId = matcher.group(5);
                String weapon = matcher.group(6);
                int distance = Integer.parseInt(matcher.group(7));
                String killerPlatform = matcher.group(8);
                String victimPlatform = matcher.group(9);
                
                // Print when a real kill (not suicide or relocation) is found
                boolean isSuicide = killer.equals(victim);
                boolean isRelocationSuicide = "suicide_by_relocation".equalsIgnoreCase(weapon);
                boolean isFalling = "falling".equalsIgnoreCase(weapon);
                
                if (!isSuicide && !isRelocationSuicide && !isFalling) {
                    System.out.println("KILL: " + killer + " killed " + victim + " with " + weapon + " from " + distance + "m");
                } else if (isFalling) {
                    System.out.println("FALL: " + victim + " died from falling");
                } else if (isRelocationSuicide) {
                    System.out.println("RELOC: " + victim + " relocated");
                }
            } else {
                unmatchedLines++;
                System.out.println("Line doesn't match: " + line);
            }
        }
        
        System.out.println("Results:");
        System.out.println("  - Matched: " + matchedLines);
        System.out.println("  - Unmatched: " + unmatchedLines);
    }
}