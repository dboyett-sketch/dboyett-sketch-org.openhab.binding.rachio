package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Custom Crop data
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomCrop {
    @Nullable
    public String id;
    
    @Nullable
    public String name; // GRASS, GARDEN, TREES, etc.
    
    @Nullable
    public Double coefficient; // crop coefficient (Kc)
    
    @Nullable
    public Double rootDepth; // inches
    
    @Nullable
    public String description;
    
    @Override
    public String toString() {
        return "CustomCrop [name=" + name + ", coefficient=" + coefficient + "]";
    }
}
