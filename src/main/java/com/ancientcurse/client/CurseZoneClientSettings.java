package com.ancientcurse.client;

/**
 * Client-side settings for curse zone visualization
 */
public class CurseZoneClientSettings {
    private static boolean showAllZoneBorders = true;
    
    public static boolean shouldShowAllZoneBorders() {
        return showAllZoneBorders;
    }
    
    public static void setShowAllZoneBorders(boolean show) {
        showAllZoneBorders = show;
    }
    
    public static void toggleShowAllZoneBorders() {
        showAllZoneBorders = !showAllZoneBorders;
    }
}