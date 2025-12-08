package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class CustomShade {
    @SerializedName("name")
    private String name = "";

    // Getter
    public String getName() {
        return name;
    }
}
