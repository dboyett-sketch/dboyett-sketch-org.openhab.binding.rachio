package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class CustomNozzle {
    @Nullable
    public String id;

    @SerializedName("name")
    @Nullable
    public String name;

    @Nullable
    public Double inchesPerHour;

    @Nullable
    public Boolean isDefault;

    // Default constructor for Gson
    public CustomNozzle() {
    }

    public CustomNozzle(@Nullable String id, @Nullable String name, @Nullable Double inchesPerHour,
            @Nullable Boolean isDefault) {
        this.id = id;
        this.name = name;
        this.inchesPerHour = inchesPerHour;
        this.isDefault = isDefault;
    }

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Double getInchesPerHour() {
        return inchesPerHour;
    }

    @Nullable
    public Boolean getIsDefault() {
        return isDefault;
    }
}
