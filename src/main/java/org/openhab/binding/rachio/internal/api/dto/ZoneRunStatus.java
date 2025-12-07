package org.openhab.binding.rachio.internal.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Zone run status enumeration for Rachio zones
 * 
 * @author Damion Boyett - Initial contribution
 */
public enum ZoneRunStatus {
    
    @SerializedName("STARTED")
    STARTED("STARTED", "Started", "The zone has started watering"),
    
    @SerializedName("STOPPED")
    STOPPED("STOPPED", "Stopped", "The zone has stopped watering"),
    
    @SerializedName("COMPLETED")
    COMPLETED("COMPLETED", "Completed", "The zone has completed watering"),
    
    @SerializedName("PAUSED")
    PAUSED("PAUSED", "Paused", "The zone watering is paused"),
    
    @SerializedName("SCHEDULED")
    SCHEDULED("SCHEDULED", "Scheduled", "The zone is scheduled to run"),
    
    @SerializedName("SKIPPED")
    SKIPPED("SKIPPED", "Skipped", "The zone run was skipped"),
    
    @SerializedName("ABORTED")
    ABORTED("ABORTED", "Aborted", "The zone run was aborted"),
    
    @SerializedName("NOT_RUNNING")
    NOT_RUNNING("NOT_RUNNING", "Not Running", "The zone is not running");
    
    private final String apiValue;
    private final String displayName;
    private final String description;
    
    ZoneRunStatus(String apiValue, String displayName, String description) {
        this.apiValue = apiValue;
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Get the API value for this status
     */
    public String getApiValue() {
        return apiValue;
    }
    
    /**
     * Get the display name for this status
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the description for this status
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Convert from API string value to enum
     */
    public static ZoneRunStatus fromApiValue(String apiValue) {
        if (apiValue == null || apiValue.isEmpty()) {
            return NOT_RUNNING;
        }
        
        for (ZoneRunStatus status : values()) {
            if (status.apiValue.equalsIgnoreCase(apiValue)) {
                return status;
            }
        }
        
        // Try to match without underscore
        String normalized = apiValue.toUpperCase().replace("_", "");
        for (ZoneRunStatus status : values()) {
            if (status.apiValue.replace("_", "").equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        
        return NOT_RUNNING;
    }
    
    /**
     * Check if the zone is currently active (watering)
     */
    public boolean isActive() {
        return this == STARTED || this == PAUSED;
    }
    
    /**
     * Check if the zone has completed its watering cycle
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }
    
    /**
     * Check if the zone is scheduled to run
     */
    public boolean isScheduled() {
        return this == SCHEDULED;
    }
    
    /**
     * Check if the zone is not running
     */
    public boolean isNotRunning() {
        return this == STOPPED || this == NOT_RUNNING || this == SKIPPED || this == ABORTED;
    }
    
    /**
     * Get the OpenHAB string representation
     */
    public String toOpenHABString() {
        switch (this) {
            case STARTED:
                return "STARTED";
            case STOPPED:
                return "STOPPED";
            case COMPLETED:
                return "COMPLETED";
            case PAUSED:
                return "PAUSED";
            case SCHEDULED:
                return "SCHEDULED";
            case SKIPPED:
                return "SKIPPED";
            case ABORTED:
                return "ABORTED";
            case NOT_RUNNING:
            default:
                return "NOT_RUNNING";
        }
    }
    
    /**
     * Parse from OpenHAB string representation
     */
    public static ZoneRunStatus fromOpenHABString(String ohString) {
        if (ohString == null || ohString.isEmpty()) {
            return NOT_RUNNING;
        }
        
        switch (ohString.toUpperCase()) {
            case "STARTED":
                return STARTED;
            case "STOPPED":
                return STOPPED;
            case "COMPLETED":
                return COMPLETED;
            case "PAUSED":
                return PAUSED;
            case "SCHEDULED":
                return SCHEDULED;
            case "SKIPPED":
                return SKIPPED;
            case "ABORTED":
                return ABORTED;
            case "NOT_RUNNING":
            default:
                return NOT_RUNNING;
        }
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
