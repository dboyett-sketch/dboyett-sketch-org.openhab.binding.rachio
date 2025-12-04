package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Rachio Custom Nozzle Configuration
 *
 * @author Daniel B. - Professional irrigation data
 */
@NonNullByDefault
public class CustomNozzle {
    private String id = "";
    private String name = "";
    private @Nullable Double inchesPerHour;
    private @Nullable Double precipitationRate; // same as inchesPerHour
    
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
    
    public @Nullable Double getInchesPerHour() {
        return inchesPerHour;
    }
    
    public void setInchesPerHour(@Nullable Double inchesPerHour) {
        this.inchesPerHour = inchesPerHour;
    }
    
    public @Nullable Double getPrecipitationRate() {
        return precipitationRate;
    }
    
    public void setPrecipitationRate(@Nullable Double precipitationRate) {
        this.precipitationRate = precipitationRate;
    }
    
    @Override
    public String toString() {
        return "CustomNozzle{id='" + id + "', name='" + name + "', inchesPerHour=" + inchesPerHour + "}";
    }
}
