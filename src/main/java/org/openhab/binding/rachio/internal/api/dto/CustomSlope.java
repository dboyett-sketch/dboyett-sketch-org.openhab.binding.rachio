package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Custom Slope data
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomSlope {
    @Nullable
    public String id;
    
    @Nullable
    public String name; // FLAT, MODERATE, STEEP, etc.
    
    @Nullable
    public Double percent; // slope percentage
    
    @Nullable
    public String description;
    
    @Override
    public String toString() {
        return "CustomSlope [name=" + name + ", percent=" + percent + "]";
    }
}
