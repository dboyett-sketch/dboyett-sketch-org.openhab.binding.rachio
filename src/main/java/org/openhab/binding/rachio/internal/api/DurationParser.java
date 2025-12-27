package org.openhab.binding.rachio.internal.api;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and formatting duration strings
 * Supports various duration formats for Rachio API
 * 
 * @author dboyett-sketch
 */
public class DurationParser {

    // ISO 8601 duration pattern (PT1H30M, PT5M, etc.)
    private static final Pattern ISO_PATTERN = Pattern.compile("^PT(?:([0-9]+)H)?(?:([0-9]+)M)?(?:([0-9]+)S)?$");

    // Simple duration patterns
    private static final Pattern SIMPLE_PATTERN = Pattern.compile("^([0-9]+)([hHmMsS])$");

    // Seconds-only pattern
    private static final Pattern SECONDS_PATTERN = Pattern.compile("^([0-9]+)(?:s|sec|seconds?)?$",
            Pattern.CASE_INSENSITIVE);

    // Minutes pattern
    private static final Pattern MINUTES_PATTERN = Pattern.compile("^([0-9]+)(?:m|min|minutes?)$",
            Pattern.CASE_INSENSITIVE);

    // Hours pattern
    private static final Pattern HOURS_PATTERN = Pattern.compile("^([0-9]+)(?:h|hr|hours?)$", Pattern.CASE_INSENSITIVE);

    // Human-readable pattern (1h30m, 5m30s, etc.)
    private static final Pattern HUMAN_PATTERN = Pattern.compile("^(?:([0-9]+)h)?(?:([0-9]+)m)?(?:([0-9]+)s)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Parse a duration string into seconds
     */
    public static int parseToSeconds(String durationString) {
        if (durationString == null || durationString.trim().isEmpty()) {
            return 0;
        }

        String trimmed = durationString.trim();

        // Try ISO 8601 format first
        Integer seconds = parseIsoDuration(trimmed);
        if (seconds != null) {
            return seconds;
        }

        // Try simple format (e.g., "90s", "5m", "2h")
        seconds = parseSimpleDuration(trimmed);
        if (seconds != null) {
            return seconds;
        }

        // Try human-readable format (e.g., "1h30m", "5m30s")
        seconds = parseHumanDuration(trimmed);
        if (seconds != null) {
            return seconds;
        }

        // Try as plain number (assume seconds)
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot parse duration: " + durationString);
        }
    }

    /**
     * Parse ISO 8601 duration format (PT1H30M, PT5M, etc.)
     */
    private static Integer parseIsoDuration(String input) {
        Matcher matcher = ISO_PATTERN.matcher(input);
        if (matcher.matches()) {
            int hours = parseGroup(matcher.group(1));
            int minutes = parseGroup(matcher.group(2));
            int seconds = parseGroup(matcher.group(3));

            return hours * 3600 + minutes * 60 + seconds;
        }
        return null;
    }

    /**
     * Parse simple duration format (e.g., "90s", "5m", "2h")
     */
    private static Integer parseSimpleDuration(String input) {
        Matcher matcher = SIMPLE_PATTERN.matcher(input);
        if (matcher.matches()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();

            switch (unit) {
                case "h":
                    return value * 3600;
                case "m":
                    return value * 60;
                case "s":
                    return value;
                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * Parse human-readable duration (e.g., "1h30m", "5m30s")
     */
    private static Integer parseHumanDuration(String input) {
        Matcher matcher = HUMAN_PATTERN.matcher(input);
        if (matcher.matches()) {
            int hours = parseGroup(matcher.group(1));
            int minutes = parseGroup(matcher.group(2));
            int seconds = parseGroup(matcher.group(3));

            return hours * 3600 + minutes * 60 + seconds;
        }
        return null;
    }

    /**
     * Parse regex group to integer (handles null/empty)
     */
    private static int parseGroup(String group) {
        if (group == null || group.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(group);
    }

    /**
     * Parse duration string to java.time.Duration
     */
    public static Duration parseDuration(String durationString) {
        int seconds = parseToSeconds(durationString);
        return Duration.ofSeconds(seconds);
    }

    /**
     * Format seconds to ISO 8601 duration string
     */
    public static String formatIsoDuration(int seconds) {
        if (seconds <= 0) {
            return "PT0S";
        }

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        StringBuilder sb = new StringBuilder("PT");

        if (hours > 0) {
            sb.append(hours).append("H");
        }

        if (minutes > 0) {
            sb.append(minutes).append("M");
        }

        if (secs > 0 || (hours == 0 && minutes == 0)) {
            sb.append(secs).append("S");
        }

        return sb.toString();
    }

    /**
     * Format seconds to human-readable string
     */
    public static String formatHumanDuration(int seconds) {
        if (seconds <= 0) {
            return "0s";
        }

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append("h");
        }

        if (minutes > 0) {
            sb.append(minutes).append("m");
        }

        if (secs > 0 || (hours == 0 && minutes == 0)) {
            sb.append(secs).append("s");
        }

        return sb.toString();
    }

    /**
     * Format seconds to short string (e.g., "1:30:00", "5:30")
     */
    public static String formatShortDuration(int seconds) {
        if (seconds <= 0) {
            return "0:00";
        }

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
    }

    /**
     * Format seconds to descriptive string (e.g., "1 hour 30 minutes")
     */
    public static String formatDescriptiveDuration(int seconds) {
        if (seconds <= 0) {
            return "0 seconds";
        }

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        StringBuilder sb = new StringBuilder();

        if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
            if (minutes > 0 || secs > 0) {
                sb.append(" ");
            }
        }

        if (minutes > 0) {
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
            if (secs > 0) {
                sb.append(" ");
            }
        }

        if (secs > 0 || (hours == 0 && minutes == 0)) {
            sb.append(secs).append(secs == 1 ? " second" : " seconds");
        }

        return sb.toString();
    }

    /**
     * Check if string is a valid duration
     */
    public static boolean isValidDuration(String durationString) {
        try {
            parseToSeconds(durationString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse with default value (returns default on error)
     */
    public static int parseToSeconds(String durationString, int defaultValue) {
        try {
            return parseToSeconds(durationString);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Parse duration with bounds checking
     */
    public static int parseToSeconds(String durationString, int minSeconds, int maxSeconds) {
        int seconds = parseToSeconds(durationString);

        if (seconds < minSeconds) {
            return minSeconds;
        }

        if (seconds > maxSeconds) {
            return maxSeconds;
        }

        return seconds;
    }

    /**
     * Convert minutes to seconds
     */
    public static int minutesToSeconds(int minutes) {
        return minutes * 60;
    }

    /**
     * Convert hours to seconds
     */
    public static int hoursToSeconds(int hours) {
        return hours * 3600;
    }

    /**
     * Convert seconds to minutes (rounded)
     */
    public static int secondsToMinutes(int seconds) {
        return (int) Math.round(seconds / 60.0);
    }

    /**
     * Convert seconds to hours (rounded)
     */
    public static int secondsToHours(int seconds) {
        return (int) Math.round(seconds / 3600.0);
    }

    /**
     * Get duration for watering (with sensible defaults)
     */
    public static int getWateringDuration(String input, int defaultMinutes) {
        if (input == null || input.trim().isEmpty()) {
            return minutesToSeconds(defaultMinutes);
        }

        try {
            int seconds = parseToSeconds(input);

            // Validate watering duration (30 seconds to 4 hours)
            if (seconds < 30) {
                return 30;
            }

            if (seconds > 14400) { // 4 hours
                return 14400;
            }

            return seconds;
        } catch (Exception e) {
            return minutesToSeconds(defaultMinutes);
        }
    }

    /**
     * Get duration for rain delay (with sensible defaults)
     */
    public static int getRainDelayDuration(String input, int defaultHours) {
        if (input == null || input.trim().isEmpty()) {
            return hoursToSeconds(defaultHours);
        }

        try {
            // Try to parse as hours first
            Matcher hoursMatcher = HOURS_PATTERN.matcher(input.trim());
            if (hoursMatcher.matches()) {
                int hours = Integer.parseInt(hoursMatcher.group(1));
                return hoursToSeconds(Math.min(Math.max(hours, 0), 168)); // 0-168 hours (7 days)
            }

            // Try as simple number (assume hours)
            int hours = Integer.parseInt(input.trim());
            return hoursToSeconds(Math.min(Math.max(hours, 0), 168));

        } catch (Exception e) {
            return hoursToSeconds(defaultHours);
        }
    }

    /**
     * Calculate end time from start time and duration
     */
    public static java.time.Instant calculateEndTime(java.time.Instant startTime, int durationSeconds) {
        return startTime.plusSeconds(durationSeconds);
    }

    /**
     * Calculate remaining time from start time and duration
     */
    public static int calculateRemainingSeconds(java.time.Instant startTime, int durationSeconds) {
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant endTime = startTime.plusSeconds(durationSeconds);

        if (now.isAfter(endTime)) {
            return 0;
        }

        return (int) (endTime.getEpochSecond() - now.getEpochSecond());
    }

    /**
     * Check if duration is within valid range
     */
    public static boolean isInRange(int seconds, int minSeconds, int maxSeconds) {
        return seconds >= minSeconds && seconds <= maxSeconds;
    }

    /**
     * Parse group with unit detection
     */
    public static Integer parseWithUnit(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim().toLowerCase();

        // Try seconds pattern
        Matcher secondsMatcher = SECONDS_PATTERN.matcher(trimmed);
        if (secondsMatcher.matches()) {
            return Integer.parseInt(secondsMatcher.group(1));
        }

        // Try minutes pattern
        Matcher minutesMatcher = MINUTES_PATTERN.matcher(trimmed);
        if (minutesMatcher.matches()) {
            return Integer.parseInt(minutesMatcher.group(1)) * 60;
        }

        // Try hours pattern
        Matcher hoursMatcher = HOURS_PATTERN.matcher(trimmed);
        if (hoursMatcher.matches()) {
            return Integer.parseInt(hoursMatcher.group(1)) * 3600;
        }

        return null;
    }
}
