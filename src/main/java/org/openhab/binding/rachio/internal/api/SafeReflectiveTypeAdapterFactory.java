package org.openhab.binding.rachio.internal.api;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.eclipse.jdt.annotation.NonNullByDefault;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Safe TypeAdapterFactory that handles null values gracefully
 * Prevents NullPointerException when deserializing JSON
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class SafeReflectiveTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        // Get the default reflective type adapter
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                try {
                    return delegate.read(in);
                } catch (Exception e) {
                    // Log error but don't crash
                    System.err.println("Error deserializing " + type.getRawType().getName() + ": " + e.getMessage());
                    // Skip the value
                    in.skipValue();
                    return null;
                }
            }
        };
    }
    
    /**
     * Helper method to safely set field values
     */
    public static void safeSetField(Object object, Field field, Object value) {
        if (field != null) {
            try {
                field.setAccessible(true);
                field.set(object, value);
            } catch (IllegalAccessException e) {
                // Ignore - field might be final or inaccessible
            }
        }
    }
    
    /**
     * Helper method to safely get field values
     */
    public static Object safeGetField(Object object, Field field) {
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.get(object);
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }
}
