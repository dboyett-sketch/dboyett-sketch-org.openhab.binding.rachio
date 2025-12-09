package org.openhab.binding.rachio.internal.api;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.eclipse.jdt.annotation.NonNullByDefault;

import java.io.IOException;
import java.time.Instant;

/**
 * Type adapter for Instant serialization/deserialization with Gson
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class InstantTypeAdapter extends TypeAdapter<Instant> {

    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.toEpochMilli());
        }
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        
        try {
            String value = in.nextString();
            
            // Try parsing as milliseconds first
            try {
                long millis = Long.parseLong(value);
                return Instant.ofEpochMilli(millis);
            } catch (NumberFormatException e1) {
                // Try parsing as seconds
                try {
                    long seconds = Long.parseLong(value);
                    return Instant.ofEpochSecond(seconds);
                } catch (NumberFormatException e2) {
                    // Try parsing as ISO-8601 string
                    return Instant.parse(value);
                }
            }
            
        } catch (Exception e) {
            throw new IOException("Failed to parse Instant: " + e.getMessage(), e);
        }
    }
}
