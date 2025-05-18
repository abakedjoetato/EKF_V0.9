import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test utility to directly test the Historical CSV Parser functionality
 */
public class TestHistoricalCsvParser {
    // The updated pattern matching our actual CSV format
    private static final Pattern CSV_LINE_PATTERN = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2});([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);?");
    
    // Date format for parsing timestamps
    private static final SimpleDateFormat CSV_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
    
    public static void main(String[] args) {
        String csvFile = "attached_assets/2025.05.15-00.00.00.csv";
        System.out.println("Testing Historical CSV Parser with file: " + csvFile);
        
        // Simulating different timestamp scenarios
        long currentTimestamp = System.currentTimeMillis();
        long pastTimestamp = currentTimestamp - (7 * 24 * 60 * 60 * 1000); // 7 days ago
        
        System.out.println("Current system time: " + new Date(currentTimestamp));
        System.out.println("Simulated 'last processed' time: " + new Date(pastTimestamp));
        System.out.println("-----------------------------------------------------\n");
        
        // Run with normal mode (should filter old entries)
        System.out.println("RUNNING IN NORMAL MODE (should filter old entries):");
        processFile(csvFile, pastTimestamp, false);
        
        System.out.println("\n-----------------------------------------------------\n");
        
        // Run with historical mode (should process all entries)
        System.out.println("RUNNING IN HISTORICAL MODE (should process all entries):");
        processFile(csvFile, pastTimestamp, true);
    }
    
    private static void processFile(String csvFile, long lastProcessedTimestamp, boolean isHistoricalMode) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int lineCount = 0;
            int matchedLines = 0;
            int processedLines = 0;
            int skippedDueToTime = 0;
            
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
                    
                    try {
                        // Parse the timestamp to check if it's older than last processed
                        Date deathTime = CSV_DATE_FORMAT.parse(timestamp);
                        
                        boolean shouldSkip = false;
                        
                        // Apply the same logic our bot uses for timestamp filtering
                        if (!isHistoricalMode && lastProcessedTimestamp > 0 && 
                            deathTime.getTime() < lastProcessedTimestamp) {
                            skippedDueToTime++;
                            shouldSkip = true;
                            System.out.println("SKIPPED (timestamp): " + killer + " killed " + victim + 
                                               " on " + timestamp);
                            continue;
                        }
                        
                        // This death would be processed
                        processedLines++;
                        boolean isSuicide = killer.equals(victim) || 
                                           weapon.toLowerCase().contains("suicide") || 
                                           weapon.toLowerCase().contains("falling");
                        
                        if (isSuicide) {
                            System.out.println("PROCESSED (suicide): " + victim + " died by " + weapon + 
                                              " on " + timestamp);
                        } else {
                            System.out.println("PROCESSED (kill): " + killer + " killed " + victim + 
                                              " with " + weapon + " at " + distance + "m on " + timestamp);
                        }
                        
                    } catch (ParseException e) {
                        System.err.println("Error parsing timestamp: " + timestamp);
                    }
                }
            }
            
            // Print summary
            System.out.println("\nProcessing Results:");
            System.out.println("Total lines: " + lineCount);
            System.out.println("Matched lines: " + matchedLines);
            System.out.println("Processed deaths: " + processedLines);
            System.out.println("Skipped due to timestamp: " + skippedDueToTime);
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}