package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class RachioPerson {
    public @Nullable String id;
    public @Nullable String username;
    public @Nullable String email;
    public @Nullable List<RachioDevice> devices;
    
    public @Nullable String getId() { return id; }
    public @Nullable String getUsername() { return username; }
    public @Nullable String getEmail() { return email; }
    public @Nullable List<RachioDevice> getDevices() { return devices; }
}
