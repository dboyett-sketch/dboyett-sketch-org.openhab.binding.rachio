package org.openhab.binding.rachio.internal.api;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class SafeReflectiveTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        // Block all java.lang.ThreadLocal and subclasses
        if (ThreadLocal.class.isAssignableFrom(type.getRawType())) {
            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    out.nullValue(); // or throw if you want to fail fast
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    in.skipValue();
                    return null;
                }
            };
        }

        return gson.getDelegateAdapter(this, type);
    }
}