package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Custom Soil data
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomSoil {
    @Nullable
    public String id;
    
    @Nullable
    public String name; // SANDY, CLAY, LOAM, etc.
    
    @Nullable
    public Double availableWater; // inches per foot
    
    @Nullable
    public Double infiltrationRate; // inches per hour
    
    @Nullable
    public String description;
    
    @Override
    public String toString() {
        return "CustomSoil [name=" + name + ", availableWater=" + availableWater + "]";
    }
}
