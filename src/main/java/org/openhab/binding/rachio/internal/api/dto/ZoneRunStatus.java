package org.openhab.binding.rachio.internal.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Zone run status enumeration
 *
 * @author <author>
 */
public enum ZoneRunStatus {
    @SerializedName("RUNNING")
    RUNNING("RUNNING"),

    @SerializedName("STOPPED")
    STOPPED("STOPPED"),

    @SerializedName("COMPLETED")
    COMPLETED("COMPLETED"),

    @SerializedName("PAUSED")
    PAUSED("PAUSED"),

    @SerializedName("CYCLING")
    CYCLING("CYCLING"),

    @SerializedName("NOT_RUNNING")
    NOT_RUNNING("NOT_RUNNING"),

    @SerializedName("SCHEDULED")
    SCHEDULED("SCHEDULED"),

    @SerializedName("WATERING")
    WATERING("WATERING"),

    @SerializedName("INACTIVE")
    INACTIVE("INACTIVE");

    private final String value;

    ZoneRunStatus(String value) {
        this.value = value;
    }

    /**
     * Get the string value of the enum
     *
     * @return string value
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the name of the enum (matching the Rachio API name)
     *
     * @return enum name as string
     */
    public String getName() {
        return this.name();
    }

    /**
     * Convert from string to enum value
     *
     * @param value string value
     * @return ZoneRunStatus or null if not found
     */
    public static ZoneRunStatus fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (ZoneRunStatus status : ZoneRunStatus.values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        // Try to match without underscores
        String normalized = value.toUpperCase().replace(" ", "_");
        for (ZoneRunStatus status : ZoneRunStatus.values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }

        return null;
    }

    /**
     * Check if this status indicates active watering
     *
     * @return true if active watering status
     */
    public boolean isActive() {
        return this == RUNNING || this == WATERING || this == CYCLING;
    }

    /**
     * Check if this status indicates completed watering
     *
     * @return true if completed
     */
    public boolean isCompleted() {
        return this == COMPLETED || this == NOT_RUNNING || this == INACTIVE;
    }

    /**
     * Check if this status indicates paused watering
     *
     * @return true if paused
     */
    public boolean isPaused() {
        return this == PAUSED;
    }

    /**
     * Check if this status indicates stopped watering
     *
     * @return true if stopped
     */
    public boolean isStopped() {
        return this == STOPPED;
    }
}
