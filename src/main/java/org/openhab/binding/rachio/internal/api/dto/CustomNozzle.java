package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Custom Nozzle data
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class CustomNozzle {
    @Nullable
    public String id;
    
    @Nullable
    public String name; // ROTOR, SPRAY, DRIP, etc.
    
    @Nullable
    public Double inchesPerHour; // precipitation rate
    
    @Nullable
    public Double radius; // feet
    
    @Nullable
    public String description;
    
    @Override
    public String toString() {
        return "CustomNozzle [name=" + name + ", inchesPerHour=" + inchesPerHour + "]";
    }
}
