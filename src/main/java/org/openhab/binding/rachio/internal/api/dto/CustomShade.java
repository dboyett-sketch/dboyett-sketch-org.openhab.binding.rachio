package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class CustomShade {
    public @Nullable String id;
    public @Nullable String name;
    
    public @Nullable String getId() { return id; }
    public @Nullable String getName() { return name; }
}
