package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDeviceHandler} is responsible for handling commands,
 * which are sent to one of the channels.
 *
 * @author Damion
 */
@Component(service = ThingHandler.class, configurationPid = "thing.rachio.device")
@NonNullByDefault
public class RachioDeviceHandler extends RachioHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    public RachioDeviceHandler(Thing thing) {
        super(thing);
        logger.debug("Device handler created for thing: {}", thing.getUID());
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler for thing: {}", getThing().getUID());
        
        // Get device ID FIRST before calling parent initialization
        String localDeviceId = (String) getConfig().get(DEVICE_ID);
        if (localDeviceId == null || localDeviceId.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID is required");
            return;
        }
        
        // Set the deviceId in parent class before initialization
        setDeviceId(localDeviceId);
        logger.debug("Set device ID: {} for thing: {}", localDeviceId, getThing().getUID());

        // FIX: Remove API key registration - devices should use bridge API key
        // The parent initialize() will get the bridge handler and use its API
        
        // Call parent initialization - this will create channels and start status updates
        super.initialize();
        
        logger.debug("Rachio device handler initialization complete for thing: {}", getThing().getUID());
    }

    @Override
    protected void createChannels() {
        logger.debug("Creating device channels for thing: {}", getThing().getUID());
        
        // Create device status channel
        createChannel("device#status", "Device Status", "String", "Status");
        
        // Create control channels
        createChannel(CHANNEL_START_ALL_ZONES, "Start All Zones", "Switch", "Control");
        createChannel(CHANNEL_START_NEXT_ZONE, "Start Next Zone", "Switch", "Control");
        createChannel(CHANNEL_STOP_WATERING, "Stop Watering", "Switch", "Control");
        createChannel(CHANNEL_RAIN_DELAY, "Rain Delay Hours", "Number", "Weather");
        
        logger.debug("All device channels created for thing: {}", getThing().getUID());
    }

    @Override
    protected void updateDeviceStatus() {
        // Device-specific status updates
        String localDeviceId = getDeviceId();
        if (localDeviceId == null) {
            logger.debug("Device ID is null, cannot update device status for thing: {}", getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID not set");
            return;
        }

        try {
            String thingId = getThing().getUID().toString();
            RachioDevice device = rachioHttp.getDevice(thingId, localDeviceId);
            if (device != null) {
                updateDeviceChannels(device);
                updateStatus(ThingStatus.ONLINE);
                logger.debug("Device status updated: {} - {} for thing: {}", device.name, device.status, getThing().getUID());
            } else {
                logger.debug("No device data returned for device ID: {} for thing: {}", localDeviceId, getThing().getUID());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "No device data returned");
            }
        } catch (Exception e) {
            logger.debug("Error updating device status for thing {}: {}", getThing().getUID(), e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void updateDeviceChannels(RachioDevice device) {
        try {
            // Update device status channel
            ChannelUID statusChannel = new ChannelUID(getThing().getUID(), "device#status");
            updateState(statusChannel, new StringType(device.status));
            
            // Update additional device properties if available
            if (device.name != null && !device.name.isEmpty()) {
                logger.trace("Device {} status: {}, online: {}", device.name, device.status, device.on);
            }
            
            logger.debug("Updated device channels for: {} on thing: {}", device.name, getThing().getUID());
        } catch (Exception e) {
            logger.debug("Error updating device channels for thing {}: {}", getThing().getUID(), e.getMessage());
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {} on thing: {}", command, channelUID.getId(), getThing().getUID());
        
        // First call parent to handle refresh commands
        super.handleCommand(channelUID, command);
        
        // Then handle device-specific commands
        String localDeviceId = getDeviceId();
        if (localDeviceId == null) {
            logger.debug("Device ID is null, cannot handle command for thing: {}", getThing().getUID());
            return;
        }

        try {
            String thingId = getThing().getUID().toString();

            switch (channelUID.getId()) {
                case CHANNEL_START_ALL_ZONES:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        // Get default runtime from bridge configuration if available
                        int duration = getDefaultRuntimeFromBridge();
                        rachioHttp.runAllZones(thingId, duration);
                        logger.info("Started all zones on device {} for {} seconds for thing: {}", 
                                   localDeviceId, duration, getThing().getUID());
                        // Reset the switch
                        updateState(channelUID, OnOffType.OFF);
                    }
                    break;
                case CHANNEL_START_NEXT_ZONE:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        // Get default runtime from bridge configuration if available
                        int duration = getDefaultRuntimeFromBridge();
                        rachioHttp.runNextZone(thingId, duration);
                        logger.info("Started next zone on device {} for {} seconds for thing: {}", 
                                   localDeviceId, duration, getThing().getUID());
                        // Reset the switch
                        updateState(channelUID, OnOffType.OFF);
                    }
                    break;
                case CHANNEL_STOP_WATERING:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        rachioHttp.stopWatering(thingId, localDeviceId);
                        logger.info("Stopped watering on device {} for thing: {}", localDeviceId, getThing().getUID());
                        // Reset the switch
                        updateState(channelUID, OnOffType.OFF);
                    }
                    break;
                case CHANNEL_RAIN_DELAY:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        if (duration >= 0 && duration <= 168) { // 0-168 hours (1 week)
                            rachioHttp.rainDelay(thingId, duration);
                            logger.info("Set rain delay on device {} for {} hours for thing: {}", 
                                       localDeviceId, duration, getThing().getUID());
                        } else {
                            logger.warn("Invalid rain delay duration: {} hours for thing: {}", duration, getThing().getUID());
                        }
                    }
                    break;
                default:
                    logger.debug("Unhandled command channel: {} for thing: {}", channelUID.getId(), getThing().getUID());
                    break;
            }
        } catch (Exception e) {
            logger.debug("Failed to execute command {} for thing {}: {}", 
                        channelUID.getId(), getThing().getUID(), e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Get default runtime from bridge configuration if available
     */
    private int getDefaultRuntimeFromBridge() {
        try {
            RachioBridgeHandler bridgeHandler = getRachioBridgeHandler();
            if (bridgeHandler != null) {
                return bridgeHandler.getDefaultRuntime();
            }
        } catch (Exception e) {
            logger.debug("Could not get default runtime from bridge, using constant default: {}", e.getMessage());
        }
        
        // Fallback to constant default
        return DEFAULT_DURATION;
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio device handler for thing: {}", getThing().getUID());
        super.dispose();
    }
}