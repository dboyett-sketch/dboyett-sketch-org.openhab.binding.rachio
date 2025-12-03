package org.openhab.binding.rachio.internal.api;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

public class InstantTypeAdapter extends TypeAdapter<Instant> {
    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.toString()); // ISO-8601 format
        }
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        String str = in.nextString();
        return Instant.parse(str);
    }
}