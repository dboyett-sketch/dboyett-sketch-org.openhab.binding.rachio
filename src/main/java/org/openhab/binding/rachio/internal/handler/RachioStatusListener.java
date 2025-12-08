package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;

/**
 * Interface for listeners that want to receive status updates from Rachio
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public interface RachioStatusListener {
    
    /**
     * Handle a webhook event from Rachio
     *
     * @param event the webhook event
     */
    void handleWebhookEvent(RachioWebhookEvent event);
    
    /**
     * Handle an error that occurred
     *
     * @param errorCode the error code
     * @param errorMessage the error message
     */
    void onError(String errorCode, String errorMessage); // FIXED: Added missing method
    
    /**
     * Handle a status update
     *
     * @param status the status message
     * @param detail the status detail
     */
    default void onStatusUpdate(String status, @Nullable String detail) {
        // Default implementation does nothing
    }
    
    /**
     * Handle device data update
     *
     * @param deviceId the device ID
     * @param dataType the type of data
     * @param data the data value
     */
    default void onDeviceDataUpdate(String deviceId, String dataType, Object data) {
        // Default implementation does nothing
    }
    
    /**
     * Handle zone data update
     *
     * @param zoneId the zone ID
     * @param dataType the type of data
     * @param data the data value
     */
    default void onZoneDataUpdate(String zoneId, String dataType, Object data) {
        // Default implementation does nothing
    }
    
    /**
     * Handle a device status change
     *
     * @param deviceId the device ID
     * @param status the new status
     */
    default void onDeviceStatusChange(String deviceId, String status) {
        // Default implementation does nothing
    }
    
    /**
     * Handle a zone status change
     *
     * @param zoneId the zone ID
     * @param status the new status
     */
    default void onZoneStatusChange(String zoneId, String status) {
        // Default implementation does nothing
    }
    
    /**
     * Handle rate limit update
     *
     * @param remaining remaining requests
     * @param limit total limit
     * @param resetTime reset time
     */
    default void onRateLimitUpdate(int remaining, int limit, @Nullable java.time.Instant resetTime) {
        // Default implementation does nothing
    }
    
    /**
     * Handle webhook health update
     *
     * @param healthy true if webhook is healthy
     */
    default void onWebhookHealthUpdate(boolean healthy) {
        // Default implementation does nothing
    }
    
    /**
     * Handle cache update
     *
     * @param cacheType type of cache
     * @param size cache size
     * @param lastUpdate last update time
     */
    default void onCacheUpdate(String cacheType, int size, @Nullable java.time.Instant lastUpdate) {
        // Default implementation does nothing
    }
}
