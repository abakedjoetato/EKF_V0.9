package com.deadside.bot.utils;

import java.awt.Color;

/**
 * Utility class for ensuring embeds meet accessibility standards
 * Implementation of Phase 4 Compliance and Validation
 */
public class AccessibilityUtils {
    // Minimum contrast ratio for accessibility
    private static final double MIN_CONTRAST_RATIO = 4.5;
    
    /**
     * Check if a color has sufficient contrast against Discord dark theme
     * 
     * @param color The color to check
     * @return true if the color has sufficient contrast
     */
    public static boolean hasEnoughContrastOnDark(Color color) {
        // Discord dark theme background approximate color
        Color darkTheme = new Color(54, 57, 63);
        return getContrastRatio(color, darkTheme) >= MIN_CONTRAST_RATIO;
    }
    
    /**
     * Check if a color has sufficient contrast against Discord light theme
     * 
     * @param color The color to check
     * @return true if the color has sufficient contrast
     */
    public static boolean hasEnoughContrastOnLight(Color color) {
        // Discord light theme background approximate color
        Color lightTheme = new Color(255, 255, 255);
        return getContrastRatio(color, lightTheme) >= MIN_CONTRAST_RATIO;
    }
    
    /**
     * Check if a color has sufficient contrast against both Discord themes
     * 
     * @param color The color to check
     * @return true if the color has sufficient contrast on both themes
     */
    public static boolean meetsAccessibilityStandards(Color color) {
        return hasEnoughContrastOnDark(color) || hasEnoughContrastOnLight(color);
    }
    
    /**
     * Get a validated color that meets accessibility standards
     * If the original color doesn't meet standards, adjust it
     * 
     * @param color The original color
     * @return A color that meets accessibility standards
     */
    public static Color getAccessibleColor(Color color) {
        if (meetsAccessibilityStandards(color)) {
            return color;
        }
        
        // Adjust color to meet standards
        float[] hsbValues = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        
        // Try to adjust brightness until it meets standards
        float originalBrightness = hsbValues[2];
        float step = 0.05f;
        
        // Try making it darker first
        for (float brightness = originalBrightness - step; 
             brightness >= 0.0f; 
             brightness -= step) {
            
            Color adjusted = Color.getHSBColor(hsbValues[0], hsbValues[1], brightness);
            if (meetsAccessibilityStandards(adjusted)) {
                return adjusted;
            }
        }
        
        // If making it darker didn't work, try making it lighter
        for (float brightness = originalBrightness + step; 
             brightness <= 1.0f; 
             brightness += step) {
            
            Color adjusted = Color.getHSBColor(hsbValues[0], hsbValues[1], brightness);
            if (meetsAccessibilityStandards(adjusted)) {
                return adjusted;
            }
        }
        
        // If all else fails, return a known accessible color
        return EmbedUtils.EMERALD_GREEN;
    }
    
    /**
     * Calculate the contrast ratio between two colors
     * 
     * @param color1 First color
     * @param color2 Second color
     * @return Contrast ratio between the two colors
     */
    private static double getContrastRatio(Color color1, Color color2) {
        double luminance1 = getLuminance(color1);
        double luminance2 = getLuminance(color2);
        
        // Ensure the lighter color is first
        if (luminance1 < luminance2) {
            double temp = luminance1;
            luminance1 = luminance2;
            luminance2 = temp;
        }
        
        // Calculate contrast ratio
        return (luminance1 + 0.05) / (luminance2 + 0.05);
    }
    
    /**
     * Calculate the relative luminance of a color
     * 
     * @param color The color
     * @return Relative luminance
     */
    private static double getLuminance(Color color) {
        double r = getLinearRGBComponent(color.getRed() / 255.0);
        double g = getLinearRGBComponent(color.getGreen() / 255.0);
        double b = getLinearRGBComponent(color.getBlue() / 255.0);
        
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }
    
    /**
     * Convert sRGB component to linear RGB
     * 
     * @param component sRGB component
     * @return Linear RGB component
     */
    private static double getLinearRGBComponent(double component) {
        if (component <= 0.03928) {
            return component / 12.92;
        } else {
            return Math.pow((component + 0.055) / 1.055, 2.4);
        }
    }
}