import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test utility to validate the CSV parsing with the corrected pattern
 */
public class CsvDeathLogParserTest {
    
    // The updated pattern matching our actual CSV format
    private static final Pattern CSV_LINE_PATTERN = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2});([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);?");
    
    public static void main(String[] args) {
        String csvFile = "attached_assets/2025.05.15-00.00.00.csv";
        System.out.println("Testing CSV parser with file: " + csvFile);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int lineCount = 0;
            int matchedLines = 0;
            int playerKills = 0;
            int suicides = 0;
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                line = line.trim();
                if (line.isEmpty()) continue;
                
                Matcher matcher = CSV_LINE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    matchedLines++;
                    
                    // Extract fields from CSV
                    String timestamp = matcher.group(1);
                    String killer = matcher.group(2);
                    String killerId = matcher.group(3);
                    String victim = matcher.group(4);
                    String victimId = matcher.group(5);
                    String weapon = matcher.group(6);
                    String distance = matcher.group(7);
                    
                    // Determine if this is a suicide or player kill
                    boolean isSuicide = killer.equals(victim) || 
                                       weapon.toLowerCase().contains("suicide") || 
                                       weapon.toLowerCase().contains("falling");
                    
                    if (isSuicide) {
                        suicides++;
                        System.out.println("SUICIDE: " + victim + " died by " + weapon);
                    } else {
                        playerKills++;
                        System.out.println("KILL: " + killer + " killed " + victim + " with " + weapon + " at " + distance + "m");
                    }
                } else {
                    System.out.println("Line " + lineCount + " did NOT match pattern: " + line);
                }
            }
            
            // Print summary
            System.out.println("\nParsing Results:");
            System.out.println("Total lines: " + lineCount);
            System.out.println("Matched lines: " + matchedLines);
            System.out.println("Player kills: " + playerKills);
            System.out.println("Suicides: " + suicides);
            System.out.println("Total deaths: " + (playerKills + suicides));
            
        } catch (Exception e) {
            System.err.println("Error processing file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}