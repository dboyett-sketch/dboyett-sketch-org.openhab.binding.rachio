package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Rachio Zone information
 */
@NonNullByDefault
public class RachioZone {
    public String id = "";
    public int zoneNumber = 0;
    public String name = "";
    public boolean enabled = true;
    public int runtime = 0;
    public String imageUrl = "";
    public long createDate = -1;
    public String irrigationType = "";
    
    // Add other zone properties as needed based on the older version
}