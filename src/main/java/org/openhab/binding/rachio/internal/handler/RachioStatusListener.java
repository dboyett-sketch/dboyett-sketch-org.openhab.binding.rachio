package org.openhab.binding.rachio.internal.handler;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;

/**
 * The {@link RachioStatusListener} is notified when a thing status changes or webhook events occur.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public interface RachioStatusListener {
    
    /**
     * This method is called whenever the status of a thing has changed.
     *
     * @param status the thing status
     * @param statusDetail the thing status detail
     * @param description the description
     */
    void onStatusChanged(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description);
    
    /**
     * This method is called when a webhook event is received from Rachio.
     * This allows handlers to react to real-time events without polling.
     *
     * @param deviceId the Rachio device ID that generated the event
     * @param eventType the type of event (e.g., ZONE_STATUS_EVENT, DEVICE_STATUS_EVENT)
     * @param subType the sub-type of the event (e.g., STARTED, STOPPED, COMPLETED)
     * @param eventData additional event data as key-value pairs
     */
    void onWebhookEvent(String deviceId, String eventType, 
                        @Nullable String subType, 
                        @Nullable Map<String, Object> eventData);
    
    /**
     * This method is called when a specific device is updated.
     *
     * @param deviceId the device ID that was updated
     */
    void onDeviceUpdated(String deviceId);
    
    /**
     * This method is called when a specific zone is updated.
     *
     * @param zoneId the zone ID that was updated
     */
    void onZoneUpdated(String zoneId);
    
    /**
     * This method is called when rate limit information changes.
     *
     * @param remainingRequests the number of remaining API requests
     * @param limit the total API request limit
     * @param resetTime the time when the rate limit resets (milliseconds since epoch)
     */
    void onRateLimitChanged(int remainingRequests, int limit, long resetTime);
    
    /**
     * This method is called when connection status changes.
     *
     * @param connected true if connected, false if disconnected
     * @param message optional message describing the connection change
     */
    void onConnectionChanged(boolean connected, @Nullable String message);
    
    /**
     * This method is called when an error occurs.
     *
     * @param errorMessage the error message
     * @param exception the exception that occurred, if any
     */
    void onError(String errorMessage, @Nullable Throwable exception);
    
    /**
     * Returns the thing ID for this listener.
     *
     * @return the thing ID or null if not applicable
     */
    @Nullable String getThingId();
    
    /**
     * Returns true if this listener is for the specified device.
     *
     * @param deviceId the device ID to check
     * @return true if this listener handles the specified device
     */
    boolean isForDevice(String deviceId);
    
    /**
     * Returns true if this listener is active.
     *
     * @return true if the listener is active and should receive notifications
     */
    boolean isActive();
    
    /**
     * Returns a description of this listener for logging purposes.
     *
     * @return listener description
     */
    String getListenerDescription();
    
    /**
     * Returns true if this listener is for the specified zone.
     *
     * @param zoneId the zone ID to check
     * @return true if this listener handles the specified zone
     */
    boolean isForZone(String zoneId);
}
