package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioHandler} is the abstract base class for Rachio thing handlers.
 *
 * @author Michael Lobstein - Initial contribution
 * @author Your Name - Enhanced for OpenHAB 5.x
 */
@NonNullByDefault
public abstract class RachioHandler extends BaseThingHandler implements RachioStatusListener {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected @Nullable RachioHttp rachioHttp;
    protected @Nullable String deviceId;

    /**
     * Constructor
     *
     * @param thing the thing
     */
    public RachioHandler(Thing thing) {
        super(thing);
    }

    /**
     * Initialize common handler functionality
     */
    protected void initializeCommon() {
        try {
            logger.debug("Initializing Rachio handler for thing: {}", getThing().getUID());
            
            // Get device ID from configuration
            Object configDeviceId = getThing().getConfiguration().get("deviceId");
            if (configDeviceId instanceof String) {
                deviceId = (String) configDeviceId;
            }
            
            // Get bridge handler to access RachioHttp
            Bridge bridge = getBridge();
            if (bridge != null) {
                RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) bridge.getHandler();
                if (bridgeHandler != null) {
                    rachioHttp = bridgeHandler.getRachioHttp();
                }
            }
            
            if (rachioHttp == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Cannot connect to Rachio bridge");
                return;
            }
            
            if (deviceId == null || deviceId.isEmpty()) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Device ID not configured");
                return;
            }
            
            // Initial status
            updateStatus(ThingStatus.UNKNOWN);
            
        } catch (Exception e) {
            logger.error("Error initializing Rachio handler: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            if (command instanceof RefreshType) {
                logger.debug("Refresh command received for channel: {}", channelUID.getId());
                refreshChannel(channelUID);
            } else {
                logger.debug("Command {} received for channel: {}", command, channelUID.getId());
                handleChannelCommand(channelUID, command);
            }
        } catch (Exception e) {
            logger.error("Error handling command: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle a refresh command for a specific channel
     *
     * @param channelUID the channel UID
     */
    protected abstract void refreshChannel(ChannelUID channelUID);

    /**
     * Handle a command for a specific channel
     *
     * @param channelUID the channel UID
     * @param command the command
     */
    protected abstract void handleChannelCommand(ChannelUID channelUID, Command command);

    /**
     * Refresh all channels
     */
    public abstract void refreshAllChannels();

    /**
     * Get the Rachio HTTP client
     *
     * @return the RachioHttp instance
     */
    public @Nullable RachioHttp getRachioHttp() {
        return rachioHttp;
    }

    /**
     * Get the device ID
     *
     * @return the device ID
     */
    public @Nullable String getDeviceId() {
        return deviceId;
    }

    // RachioStatusListener implementation
    
    @Override
    public void onRefreshRequested() {
        logger.debug("Refresh requested for thing: {}", getThing().getUID());
        refreshAllChannels();
    }
    
    @Override
    public void updateDeviceStatus(ThingStatus status) {
        updateStatus(status);
    }
    
    @Override
    public void updateZoneStatus(String zoneId, ThingStatus status) {
        // Implement in child classes if needed
    }
    
    @Override
    public void onThingStateChanged(org.openhab.binding.rachio.internal.api.dto.RachioDevice device, 
                                    org.openhab.binding.rachio.internal.api.dto.RachioZone zone) {
        // Implement in child classes
    }
    
    @Override
    public void onDeviceDataUpdated(org.openhab.binding.rachio.internal.api.dto.RachioDevice device) {
        // Implement in child classes
    }
    
    @Override
    public void onZoneDataUpdated(org.openhab.binding.rachio.internal.api.dto.RachioZone zone) {
        // Implement in child classes
    }
    
    @Override
    public void onError(String errorMessage, String detail) {
        logger.error("Error in Rachio handler: {} - {}", errorMessage, detail);
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
    }
}
