package org.openhab.binding.rachio.internal.handler;

import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingStatus;

/**
 * The {@link RachioStatusListener} is notified when a Rachio device or zone status changes.
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
public interface RachioStatusListener {
    /**
     * Called when a refresh of device/zone data is requested
     */
    void onRefreshRequested();
    
    /**
     * Called when the overall device status changes
     * 
     * @param status the new thing status
     */
    void updateDeviceStatus(ThingStatus status);

    /**
     * Called when a specific zone status changes
     * 
     * @param zoneId the ID of the zone that changed
     * @param status the new thing status
     */
    void updateZoneStatus(String zoneId, ThingStatus status);

    /**
     * Called when thing state changes for a device and zone
     * 
     * @param device the device that changed
     * @param zone the zone that changed (can be null for device-only changes)
     */
    void onThingStateChanged(RachioDevice device, RachioZone zone);
    
    /**
     * Called when device data is successfully retrieved and processed
     * 
     * @param device the device that was updated
     */
    void onDeviceDataUpdated(RachioDevice device);
    
    /**
     * Called when zone data is successfully retrieved and processed
     * 
     * @param zone the zone that was updated
     */
    void onZoneDataUpdated(RachioZone zone);
    
    /**
     * Called when an error occurs during data retrieval or processing
     * 
     * @param errorMessage description of the error
     * @param detail additional error details
     */
    void onError(String errorMessage, String detail);
}