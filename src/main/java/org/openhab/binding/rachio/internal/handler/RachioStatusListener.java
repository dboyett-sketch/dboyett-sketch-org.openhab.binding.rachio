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
}
