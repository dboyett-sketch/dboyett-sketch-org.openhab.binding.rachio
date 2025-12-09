package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Custom Shade data
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomShade {
    @Nullable
    public String id;
    
    @Nullable
    public String name; // FULL_SUN, PART_SHADE, FULL_SHADE
    
    @Nullable
    public Double sunExposure; // percentage
    
    @Nullable
    public String description;
    
    @Override
    public String toString() {
        return "CustomShade [name=" + name + ", sunExposure=" + sunExposure + "]";
    }
}
