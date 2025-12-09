package org.openhab.binding.rachio.internal.handler;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;

/**
 * Listener interface for Rachio status updates
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public interface RachioStatusListener {
    /**
     * Called when the device list is updated
     */
    void deviceListUpdated(Collection<RachioDevice> devices);
    
    /**
     * Called when a specific device is updated
     */
    void deviceUpdated(RachioDevice device);
    
    /**
     * Called when a webhook event is received
     */
    void webhookEventReceived(RachioWebHookEvent event);
}
