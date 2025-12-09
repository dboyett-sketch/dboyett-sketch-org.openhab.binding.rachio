package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;

import java.util.Map;

/**
 * The {@link RachioStatusListener} is an interface for things that want to
 * receive status updates from the Rachio bridge via webhooks or polling.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public interface RachioStatusListener {

    /**
     * Get the thing associated with this listener
     */
    Thing getThing();

    /**
     * Refresh the thing's state from the API
     */
    void refresh();

    /**
     * Handle a webhook event from the Rachio API
     * 
     * @param eventType the type of event (e.g., ZONE_STATUS, DEVICE_STATUS)
     * @param deviceId the device ID the event is for
     * @param zoneId the zone ID (if applicable, null for device events)
     * @param data additional event data
     */
    void onWebhookEvent(String eventType, String deviceId, @Nullable String zoneId, 
                        @Nullable Map<String, Object> data);
}
