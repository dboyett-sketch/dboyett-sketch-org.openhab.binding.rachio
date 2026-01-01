package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Gson TypeAdapter for java.time.Instant
 * Handles various timestamp formats from Rachio API
 * 
 * @author dboyett-sketch
 */
public class InstantTypeAdapter extends TypeAdapter<Instant> {

    // Rachio API uses ISO 8601 timestamps (with milliseconds)
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    // Alternative format for timestamps without timezone
    private static final DateTimeFormatter LOCAL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // For epoch seconds (sometimes used in webhooks)
    private static final DateTimeFormatter EPOCH_FORMATTER = null; // Special handling

    /**
     * Write Instant to JSON
     */
    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        // Write as ISO 8601 string
        String formatted = ISO_FORMATTER.format(value);
        out.value(formatted);
    }

    /**
     * Read Instant from JSON
     */
    @Override
    public Instant read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        try {
            String jsonValue = in.nextString();
            return parseInstant(jsonValue);
        } catch (DateTimeParseException e) {
            throw new JsonParseException("Failed to parse timestamp: " + in.nextString(), e);
        } catch (NumberFormatException e) {
            throw new JsonParseException("Failed to parse numeric timestamp: " + in.nextString(), e);
        }
    }

    /**
     * Parse Instant from string with multiple format support
     */
    private Instant parseInstant(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String trimmed = value.trim();

        // Try ISO 8601 format first (most common)
        try {
            return Instant.from(ISO_FORMATTER.parse(trimmed));
        } catch (DateTimeParseException e1) {
            // Try ISO format without timezone
            try {
                TemporalAccessor accessor = ISO_FORMATTER.parse(trimmed + "Z");
                return Instant.from(accessor);
            } catch (DateTimeParseException e2) {
                // Try as epoch seconds (numeric)
                try {
                    long epochSeconds = Long.parseLong(trimmed);
                    return Instant.ofEpochSecond(epochSeconds);
                } catch (NumberFormatException e3) {
                    // Try as epoch milliseconds
                    try {
                        long epochMillis = Long.parseLong(trimmed);
                        // If value is large, assume milliseconds
                        if (epochMillis > 1000000000000L) { // > year 2001 in ms
                            return Instant.ofEpochMilli(epochMillis);
                        } else {
                            // Otherwise assume seconds
                            return Instant.ofEpochSecond(epochMillis);
                        }
                    } catch (NumberFormatException e4) {
                        // Try local date time format
                        try {
                            TemporalAccessor accessor = LOCAL_FORMATTER.parse(trimmed);
                            return Instant.from(accessor);
                        } catch (DateTimeParseException e5) {
                            throw new DateTimeParseException(
                                    "Cannot parse timestamp: " + trimmed
                                            + " (tried ISO 8601, epoch seconds, epoch milliseconds, local datetime)",
                                    trimmed, 0);
                        }
                    }
                }
            }
        }
    }

    /**
     * Parse Instant with lenient error handling (returns null on failure)
     */
    public Instant parseLenient(String value) {
        try {
            return parseInstant(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Format Instant to ISO 8601 string
     */
    public String format(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ISO_FORMATTER.format(instant);
    }

    /**
     * Format Instant to epoch seconds
     */
    public Long formatEpochSeconds(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.getEpochSecond();
    }

    /**
     * Format Instant to epoch milliseconds
     */
    public Long formatEpochMillis(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.toEpochMilli();
    }

    /**
     * Check if string is a valid timestamp
     */
    public boolean isValidTimestamp(String value) {
        try {
            parseInstant(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get current timestamp as ISO string
     */
    public String nowAsIso() {
        return format(Instant.now());
    }

    /**
     * Get current timestamp as epoch seconds
     */
    public long nowAsEpochSeconds() {
        return Instant.now().getEpochSecond();
    }

    /**
     * Get current timestamp as epoch milliseconds
     */
    public long nowAsEpochMillis() {
        return Instant.now().toEpochMilli();
    }

    /**
     * Parse ISO string to Instant (convenience method)
     */
    public static Instant fromIso(String isoString) {
        InstantTypeAdapter adapter = new InstantTypeAdapter();
        return adapter.parseInstant(isoString);
    }

    /**
     * Format Instant to ISO string (convenience method)
     */
    public static String toIso(Instant instant) {
        InstantTypeAdapter adapter = new InstantTypeAdapter();
        return adapter.format(instant);
    }

    /**
     * Calculate time difference in seconds
     */
    public long differenceInSeconds(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0;
        }
        return end.getEpochSecond() - start.getEpochSecond();
    }

    /**
     * Calculate time difference in minutes
     */
    public long differenceInMinutes(Instant start, Instant end) {
        return differenceInSeconds(start, end) / 60;
    }

    /**
     * Calculate time difference in hours
     */
    public long differenceInHours(Instant start, Instant end) {
        return differenceInMinutes(start, end) / 60;
    }

    /**
     * Check if Instant is in the past
     */
    public boolean isInPast(Instant instant) {
        if (instant == null) {
            return false;
        }
        return instant.isBefore(Instant.now());
    }

    /**
     * Check if Instant is in the future
     */
    public boolean isInFuture(Instant instant) {
        if (instant == null) {
            return false;
        }
        return instant.isAfter(Instant.now());
    }

    /**
     * Get age of timestamp in seconds
     */
    public long getAgeInSeconds(Instant instant) {
        if (instant == null) {
            return Long.MAX_VALUE;
        }
        return differenceInSeconds(instant, Instant.now());
    }

    /**
     * Check if timestamp is recent (within specified seconds)
     */
    public boolean isRecent(Instant instant, int seconds) {
        return getAgeInSeconds(instant) <= seconds;
    }
}
