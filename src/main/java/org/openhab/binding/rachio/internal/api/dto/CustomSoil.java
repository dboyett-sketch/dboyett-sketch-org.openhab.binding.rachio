package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class CustomSoil {
    @SerializedName("name")
    private String name = "";

    @SerializedName("availableWater")
    private double availableWater = 0.0;

    // Getters
    public String getName() {
        return name;
    }

    public double getAvailableWater() {
        return availableWater;
    }
}
