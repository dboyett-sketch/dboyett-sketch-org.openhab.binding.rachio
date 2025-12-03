package org.openhab.binding.rachio.internal.api;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;

import java.util.List;

public class RachioCloudStatus {
    public boolean online;
    public boolean paused;
    public String username;
    public String fullName;
    public String email;
    public List<RachioDevice> devices; // ← Add this
}