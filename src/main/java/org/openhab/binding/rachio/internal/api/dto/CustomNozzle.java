package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class CustomNozzle {
    @SerializedName("name")
    private String name = "";

    @SerializedName("inchesPerHour")
    private double inchesPerHour = 0.0;

    // Getters
    public String getName() {
        return name;
    }

    public double getInchesPerHour() {
        return inchesPerHour;
    }
}
