package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class CustomNozzle {
    public @Nullable String id;
    public @Nullable String name;
    public @Nullable Double inchesPerHour;
    
    public @Nullable String getId() { return id; }
    public @Nullable String getName() { return name; }
    public @Nullable Double getInchesPerHour() { return inchesPerHour; }
}
