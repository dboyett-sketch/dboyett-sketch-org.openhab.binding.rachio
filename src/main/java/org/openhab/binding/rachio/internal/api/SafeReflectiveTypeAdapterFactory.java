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
import java.util.HashMap;
import java.util.Map;

/**
 * Safe reflective type adapter factory for Gson that prevents
 * reflection-based attacks by limiting field access.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class SafeReflectiveTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();
        
        // Only create adapters for our DTO classes in the rachio package
        if (!rawType.getName().startsWith("org.openhab.binding.rachio")) {
            return null;
        }
        
        // Create a safe adapter that only allows known fields
        return (TypeAdapter<T>) new SafeTypeAdapter<>(gson, rawType);
    }

    private static class SafeTypeAdapter<T> extends TypeAdapter<T> {
        private final Gson gson;
        private final Class<T> clazz;
        private final Map<String, Field> allowedFields;
        
        @SuppressWarnings("unchecked")
        SafeTypeAdapter(Gson gson, Class<?> clazz) {
            this.gson = gson;
            this.clazz = (Class<T>) clazz;
            this.allowedFields = new HashMap<>();
            
            // Only allow fields that are explicitly marked with @Nullable or have public access
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(org.eclipse.jdt.annotation.Nullable.class) || 
                    java.lang.reflect.Modifier.isPublic(field.getModifiers())) {
                    field.setAccessible(true);
                    allowedFields.put(field.getName(), field);
                }
            }
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            
            out.beginObject();
            for (Map.Entry<String, Field> entry : allowedFields.entrySet()) {
                try {
                    Object fieldValue = entry.getValue().get(value);
                    if (fieldValue != null) {
                        out.name(entry.getKey());
                        gson.toJson(fieldValue, fieldValue.getClass(), out);
                    }
                } catch (IllegalAccessException e) {
                    // Skip this field
                }
            }
            out.endObject();
        }

        @Override
        public T read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            
            try {
                T instance = clazz.getDeclaredConstructor().newInstance();
                in.beginObject();
                while (in.hasNext()) {
                    String name = in.nextName();
                    Field field = allowedFields.get(name);
                    if (field != null) {
                        Object value = gson.fromJson(in, field.getType());
                        field.set(instance, value);
                    } else {
                        // Skip unknown field for safety
                        in.skipValue();
                    }
                }
                in.endObject();
                return instance;
            } catch (Exception e) {
                throw new IOException("Failed to deserialize " + clazz.getName() + ": " + e.getMessage(), e);
            }
        }
    }
}
