package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class RachioUsage {
    public @Nullable Double totalUsage; // Total water usage in gallons
    public @Nullable Double savings; // Water savings in gallons
    public @Nullable Double currentUsage; // Current period usage in gallons
    
    public @Nullable Double getTotalUsage() { return totalUsage; }
    public @Nullable Double getSavings() { return savings; }
    public @Nullable Double getCurrentUsage() { return currentUsage; }
}
