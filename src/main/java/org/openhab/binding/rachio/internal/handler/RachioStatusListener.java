package org.openhab.binding.rachio.internal.handler;

import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;

/**
 * Interface for listening to status changes from Rachio devices
 * 
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public interface RachioStatusListener {
    
    /**
     * Called when a device status changes via webhook event
     * 
     * @param event the webhook event
     */
    void onStatusChanged(RachioWebHookEvent event);
    
    /**
     * Called when a device is updated (refreshed from API)
     * 
     * @param device the updated device
     */
    void onDeviceUpdated(RachioDevice device);
    
    /**
     * Get the device ID that this listener is interested in
     * 
     * @return the device ID, or empty string for all devices
     */
    String getDeviceId();
    
    /**
     * Get the last event time for this listener
     * 
     * @return the last event time, or null if no events received
     */
    @Nullable
    Instant getLastEventTime();
}
