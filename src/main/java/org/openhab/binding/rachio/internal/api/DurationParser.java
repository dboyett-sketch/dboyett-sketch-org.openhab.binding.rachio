package org.openhab.binding.rachio.internal.api;

public class DurationParser {
    
    /**
     * Parse duration string to seconds
     * Supports formats: "10m30s", "5m", "120s", "90" (defaults to seconds)
     */
    public static int parseToSeconds(String durationStr) {
        if (durationStr == null || durationStr.trim().isEmpty()) {
            return 0;
        }
        
        String str = durationStr.trim().toLowerCase();
        int totalSeconds = 0;
        
        // Check for minutes
        int mIndex = str.indexOf('m');
        if (mIndex != -1) {
            try {
                String minutesStr = str.substring(0, mIndex);
                int minutes = Integer.parseInt(minutesStr);
                totalSeconds += minutes * 60;
                str = str.substring(mIndex + 1);
            } catch (NumberFormatException e) {
                // Invalid minutes format, skip
            }
        }
        
        // Check for seconds
        int sIndex = str.indexOf('s');
        if (sIndex != -1) {
            try {
                String secondsStr = str.substring(0, sIndex);
                int seconds = Integer.parseInt(secondsStr);
                totalSeconds += seconds;
            } catch (NumberFormatException e) {
                // Invalid seconds format, skip
            }
        } else if (!str.isEmpty()) {
            // No 's' but has content - assume it's all seconds
            try {
                int seconds = Integer.parseInt(str);
                totalSeconds += seconds;
            } catch (NumberFormatException e) {
                // Not a number
            }
        }
        
        return totalSeconds;
    }
    
    /**
     * Convert seconds to human-readable string "Xm Ys"
     */
    public static String formatSeconds(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        
        if (remainingSeconds == 0) {
            return minutes + "m";
        } else {
            return minutes + "m " + remainingSeconds + "s";
        }
    }
}
