package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class CustomSoil {
    @Nullable
    public String id;

    @SerializedName("name")
    @Nullable
    public String name;

    @Nullable
    public Double coefficient;

    @Nullable
    public Boolean isDefault;

    // Default constructor for Gson
    public CustomSoil() {
    }

    public CustomSoil(@Nullable String id, @Nullable String name, @Nullable Double coefficient,
            @Nullable Boolean isDefault) {
        this.id = id;
        this.name = name;
        this.coefficient = coefficient;
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
    public Double getCoefficient() {
        return coefficient;
    }

    @Nullable
    public Boolean getIsDefault() {
        return isDefault;
    }
}
