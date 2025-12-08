package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class CustomCrop {
    @SerializedName("name")
    private String name = "";

    @SerializedName("coefficient")
    private double coefficient = 0.0;

    // Getters
    public String getName() {
        return name;
    }

    public double getCoefficient() {
        return coefficient;
    }
}
