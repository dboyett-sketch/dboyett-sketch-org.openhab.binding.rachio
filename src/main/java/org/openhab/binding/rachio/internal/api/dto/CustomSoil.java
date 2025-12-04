package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Rachio Custom Soil Configuration
 *
 * @author Daniel B. - Professional irrigation data
 */
@NonNullByDefault
public class CustomSoil {
    private String id = "";
    private String name = "";
    private @Nullable Double availableWater; // inches per foot
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public @Nullable Double getAvailableWater() {
        return availableWater;
    }
    
    public void setAvailableWater(@Nullable Double availableWater) {
        this.availableWater = availableWater;
    }
    
    @Override
    public String toString() {
        return "CustomSoil{id='" + id + "', name='" + name + "', availableWater=" + availableWater + "}";
    }
}
