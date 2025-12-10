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


private final Set<RachioStatusListener> statusListeners = ConcurrentHashMap.newKeySet();

public void registerStatusListener(RachioStatusListener listener) {
    if (listener != null) {
        statusListeners.add(listener);
        logger.debug("Registered status listener: {}", listener.getListenerDescription());
    }
}

public void unregisterStatusListener(RachioStatusListener listener) {
    if (listener != null) {
        statusListeners.remove(listener);
        logger.debug("Unregistered status listener: {}", listener.getListenerDescription());
    }
}

protected void notifyStatusListeners(ThingStatus status, ThingStatusDetail detail, @Nullable String message) {
    for (RachioStatusListener listener : statusListeners) {
        if (listener.isActive()) {
            try {
                listener.onStatusChanged(status, detail, message);
            } catch (Exception e) {
                logger.warn("Error notifying status listener {}", listener.getListenerDescription(), e);
            }
        }
    }
}

protected void notifyDeviceUpdated(String deviceId) {
    for (RachioStatusListener listener : statusListeners) {
        if (listener.isActive() && (listener.isForDevice(deviceId) || listener.isForThing(deviceId))) {
            try {
                listener.onDeviceUpdated(deviceId);
            } catch (Exception e) {
                logger.warn("Error notifying device update to listener {}", listener.getListenerDescription(), e);
            }
        }
    }
}

protected void notifyZoneUpdated(String zoneId) {
    for (RachioStatusListener listener : statusListeners) {
        if (listener.isActive() && listener.isForZone(zoneId)) {
            try {
                listener.onZoneUpdated(zoneId);
            } catch (Exception e) {
                logger.warn("Error notifying zone update to listener {}", listener.getListenerDescription(), e);
            }
        }
    }
}
