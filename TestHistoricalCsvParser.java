import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Simple test to validate the CSV line pattern matching for the historical parser
 */
public class TestHistoricalCsvParser {
    // Original pattern that's failing to match lines
    private static final Pattern ORIGINAL_PATTERN = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2});([^;]+);([^;]+);([^;]+);([^;]+);([^;]+);(\\d+);([^;]+);([^;]+);?");
    
    // Updated pattern that should be more flexible with CSV formats
    private static final Pattern UPDATED_PATTERN = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2});([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);?");
    
    public static void main(String[] args) {
        String csvFile = "attached_assets/2025.05.15-00.00.00.csv";
        
        System.out.println("Testing CSV pattern matching for historical parser");
        System.out.println("File: " + csvFile);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int lineNumber = 0;
            int originalMatches = 0;
            int updatedMatches = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Test with original pattern
                Matcher originalMatcher = ORIGINAL_PATTERN.matcher(line);
                boolean originalMatched = originalMatcher.matches();
                if (originalMatched) originalMatches++;
                
                // Test with updated pattern
                Matcher updatedMatcher = UPDATED_PATTERN.matcher(line);
                boolean updatedMatched = updatedMatcher.matches();
                if (updatedMatched) updatedMatches++;
                
                // Print results for this line
                System.out.printf("Line %d: Original pattern: %s, Updated pattern: %s%n", 
                        lineNumber, originalMatched ? "MATCH" : "NO MATCH", updatedMatched ? "MATCH" : "NO MATCH");
                
                // If updated pattern matches but original doesn't, show details
                if (updatedMatched && !originalMatched) {
                    System.out.println("  Line content: " + line);
                    if (updatedMatched) {
                        System.out.println("  Timestamp: " + updatedMatcher.group(1));
                        System.out.println("  Killer: " + updatedMatcher.group(2));
                        System.out.println("  Victim: " + updatedMatcher.group(4));
                        System.out.println("  Weapon: " + updatedMatcher.group(6));
                        System.out.println("  Distance: " + updatedMatcher.group(7));
                    }
                    System.out.println();
                }
            }
            
            // Print summary
            System.out.println("\nSummary:");
            System.out.println("Total lines: " + lineNumber);
            System.out.println("Original pattern matches: " + originalMatches);
            System.out.println("Updated pattern matches: " + updatedMatches);
            System.out.println("Improvement: " + (updatedMatches - originalMatches) + " additional matches");
            
            double originalPercentage = (double)originalMatches / lineNumber * 100;
            double updatedPercentage = (double)updatedMatches / lineNumber * 100;
            System.out.printf("Original match rate: %.1f%%%n", originalPercentage);
            System.out.printf("Updated match rate: %.1f%%%n", updatedPercentage);
            
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
    }
}