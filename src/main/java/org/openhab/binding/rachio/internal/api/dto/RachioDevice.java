package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Rachio Device information
 */
@NonNullByDefault
public class RachioDevice {
    public String id = "";
    public String status = "";
    public String name = "";
    public boolean on = true;
    public String model = "";
    public String serialNumber = "";
    public String macAddress = "";
    public double latitude = 0.0;
    public double longitude = 0.0;
    public boolean deleted = false;
    public long createDate = -1;
    public long rainDelayExpirationDate = 0;
    public boolean homeKitCompatible = false;
    public String scheduleModeType = "";
    public boolean cycleSoak = false;
    
    // Network settings
    public Network network;
    
    // Zones
    public List<Zone> zones;

    public static class Network {
        public String ip = "";
        public String nm = "";
        public String gw = "";
        public String dns1 = "";
        public String dns2 = "";
        public String rssi = "";
    }

    public static class Zone {
        public String id = "";
        public int zoneNumber = 0;
        public String name = "";
        public boolean enabled = true;
        public int runtime = 0;
        public String imageUrl = "";
        // Add other zone properties as needed
    }
}