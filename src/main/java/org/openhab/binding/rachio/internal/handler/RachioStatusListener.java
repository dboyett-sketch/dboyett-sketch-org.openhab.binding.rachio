package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;

/**
 * Interface for receiving status updates from Rachio bridge
 * 
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public interface RachioStatusListener {
    /**
     * Enum representing the type of listener
     */
    enum ListenerType {
        BRIDGE,
        DEVICE,
        ZONE,
        DISCOVERY
    }

    /**
     * Get unique listener ID
     * 
     * @return listener ID
     */
    String getListenerId();

    /**
     * Get listener type
     * 
     * @return listener type
     */
    ListenerType getListenerType();

    /**
     * Called when a new device is discovered
     * 
     * @param deviceId the device ID
     */
    void onNewDevice(@Nullable String deviceId);

    /**
     * Called when a new zone is discovered
     * 
     * @param deviceId the parent device ID
     * @param zoneId the zone ID
     */
    void onNewZone(@Nullable String deviceId, @Nullable String zoneId);

    /**
     * Called when device status changes (online/offline/running/etc.)
     *
     * @param device the updated device data
     */
    void onDeviceStatusChanged(@Nullable RachioDevice device);

    /**
     * Called when device status changes by ID
     *
     * @param deviceId the device ID
     * @param status the new status
     */
    void onDeviceStatusChanged(@Nullable String deviceId, @Nullable String status);

    /**
     * Called when a webhook event is received
     *
     * @param eventJson the raw webhook event JSON
     */
    void onWebhookEventReceived(@Nullable String eventJson);

    /**
     * Called when device data is updated
     *
     * @param device the updated device data
     */
    void onDeviceUpdated(@Nullable RachioDevice device);

    /**
     * Called when zone data is updated
     *
     * @param zoneId the zone ID
     * @param zone the updated zone data
     */
    void onZoneUpdated(@Nullable String zoneId, @Nullable RachioZone zone);

    /**
     * Called when rate limit status changes
     *
     * @param remaining remaining API calls
     * @param limit total API call limit
     * @param status human-readable status
     */
    void onRateLimitStatusChanged(int remaining, int limit, @Nullable String status);

    // ===== NEW METHOD ADDED BASED ON COMPILATION ERRORS =====

    /**
     * Called when zone status changes (watering started/stopped/completed)
     *
     * @param deviceId the parent device ID
     * @param zoneId the zone ID
     * @param status the new zone status
     */
    void onZoneStatusChanged(@Nullable String deviceId, @Nullable String zoneId, @Nullable String status);
}
