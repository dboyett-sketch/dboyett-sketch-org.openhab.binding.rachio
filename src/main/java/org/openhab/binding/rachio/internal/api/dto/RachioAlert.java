package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;

@NonNullByDefault
public class RachioAlert {
    public String id;
    public String deviceId;
    public String type; // "OFFLINE", "ONLINE", "RAIN_SENSOR", "FREEZE", "HIGH_WIND", "CYCLE_SOAK"
    public String severity; // "INFO", "WARNING", "ERROR"
    public String message;
    
    @SerializedName("createdAt")
    public Instant createdAt;
    
    @SerializedName("acknowledged")
    public boolean acknowledged;
    
    @SerializedName("acknowledgedAt")
    public @Nullable Instant acknowledgedAt;
    
    @SerializedName("data")
    public @Nullable AlertData data;
    
    public static class AlertData {
        @SerializedName("duration")
        public @Nullable Integer duration;
        
        @SerializedName("zoneId")
        public @Nullable String zoneId;
        
        @SerializedName("temperature")
        public @Nullable Double temperature;
        
        @SerializedName("windSpeed")
        public @Nullable Double windSpeed;
    }
}
