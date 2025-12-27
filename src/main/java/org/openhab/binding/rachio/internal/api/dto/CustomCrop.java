package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class CustomCrop {
    @Nullable
    public String id;

    @SerializedName("name")
    @Nullable
    public String name;

    @Nullable
    public Double coefficient;

    @Nullable
    public Boolean isDefault;

    @Nullable
    public Double inchesPerWeek;

    // Default constructor for Gson
    public CustomCrop() {
    }

    public CustomCrop(@Nullable String id, @Nullable String name, @Nullable Double coefficient,
            @Nullable Boolean isDefault, @Nullable Double inchesPerWeek) {
        this.id = id;
        this.name = name;
        this.coefficient = coefficient;
        this.isDefault = isDefault;
        this.inchesPerWeek = inchesPerWeek;
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
    public Double getCoefficient() {
        return coefficient;
    }

    @Nullable
    public Boolean getIsDefault() {
        return isDefault;
    }

    @Nullable
    public Double getInchesPerWeek() {
        return inchesPerWeek;
    }
}
