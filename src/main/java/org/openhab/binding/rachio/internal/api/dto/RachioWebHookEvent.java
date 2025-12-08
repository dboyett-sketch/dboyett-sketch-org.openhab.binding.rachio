package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

@NonNullByDefault
public class RachioWebHookEvent {
    @SerializedName("type")
    private String type = "";

    @SerializedName("subType")
    @Nullable
    private String subType;

    @SerializedName("deviceId")
    private String deviceId = "";

    @SerializedName("zoneId")
    @Nullable
    private String zoneId;

    @SerializedName("eventId")
    @Nullable
    private String eventId;

    @SerializedName("summary")
    @Nullable
    private RachioEventSummary summary;

    // Getters
    public String getType() {
        return type;
    }

    @Nullable
    public String getSubType() {
        return subType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    @Nullable
    public String getZoneId() {
        return zoneId;
    }

    @Nullable
    public String getEventId() {
        return eventId;
    }

    @Nullable
    public RachioEventSummary getSummary() {
        return summary;
    }
}
