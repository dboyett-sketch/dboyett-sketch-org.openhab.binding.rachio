package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Data transfer object for Rachio zones.
 */
@NonNullByDefault
public class RachioZone {
    private String id = "";
    private String name = "";
    private int zoneNumber;
    private boolean enabled;
    private int runtime;
    
    // Missing fields from compilation errors
    private @Nullable Long lastWateredDate;
    private @Nullable CustomSoil soil;
    private @Nullable CustomCrop crop;
    private @Nullable CustomNozzle nozzle;
    private @Nullable String slope;
    private @Nullable String shade;
    
    // Getters and setters for existing fields
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getZoneNumber() { return zoneNumber; }
    public void setZoneNumber(int zoneNumber) { this.zoneNumber = zoneNumber; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getRuntime() { return runtime; }
    public void setRuntime(int runtime) { this.runtime = runtime; }
    
    // Getters and setters for new fields
    public @Nullable Long getLastWateredDate() { return lastWateredDate; }
    public void setLastWateredDate(@Nullable Long lastWateredDate) { this.lastWateredDate = lastWateredDate; }
    
    public @Nullable CustomSoil getSoil() { return soil; }
    public void setSoil(@Nullable CustomSoil soil) { this.soil = soil; }
    
    public @Nullable CustomCrop getCrop() { return crop; }
    public void setCrop(@Nullable CustomCrop crop) { this.crop = crop; }
    
    public @Nullable CustomNozzle getNozzle() { return nozzle; }
    public void setNozzle(@Nullable CustomNozzle nozzle) { this.nozzle = nozzle; }
    
    public @Nullable String getSlope() { return slope; }
    public void setSlope(@Nullable String slope) { this.slope = slope; }
    
    public @Nullable String getShade() { return shade; }
    public void setShade(@Nullable String shade) { this.shade = shade; }
    
    @Override
    public String toString() {
        return "RachioZone [id=" + id + ", name=" + name + ", zoneNumber=" + zoneNumber + 
               ", enabled=" + enabled + ", runtime=" + runtime + ", lastWateredDate=" + lastWateredDate + 
               ", soil=" + soil + ", crop=" + crop + ", nozzle=" + nozzle + 
               ", slope=" + slope + ", shade=" + shade + "]";
    }
}
