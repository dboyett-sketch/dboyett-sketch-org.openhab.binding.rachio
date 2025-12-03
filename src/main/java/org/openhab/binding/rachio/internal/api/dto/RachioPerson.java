package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for Rachio Person information
 */
@NonNullByDefault
public class RachioPerson {
    public String id = "";
    public String username = "";
    public String fullName = "";
    public String email = "";
    public List<Device> devices;

    public static class Device {
        public String id = "";
        public String status = "";
        public String name = "";
        public List<Zone> zones;
        public boolean on = true;
        public String model = "";
        public String serialNumber = "";
        public String macAddress = "";
        public double latitude = 0.0;
        public double longitude = 0.0;
        public boolean deleted = false;
        
        // Network settings
        public Network network;
        
        public static class Network {
            public String ip = "";
            public String nm = "";
            public String gw = "";
            public String dns1 = "";
            public String dns2 = "";
            public String rssi = "";
        }
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