package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.RachioException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.thing.ChannelUID;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands,
 * which are sent to one of the channels.
 *
 * @author Damion
 */
@Component(service = ThingHandler.class, configurationPid = "thing.rachio.zone")
@NonNullByDefault
public class RachioZoneHandler extends RachioHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);
    
    @Nullable
    private String zoneId;

    public RachioZoneHandler(Thing thing) {
        super(thing);
        logger.debug("Zone handler created for thing: {}", thing.getUID());
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler for thing: {}", getThing().getUID());
        
        // Validate configuration FIRST
        String localZoneId = (String) getConfig().get(ZONE_ID);
        if (localZoneId == null || localZoneId.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone ID is required");
            return;
        }

        String localDeviceId = (String) getConfig().get(DEVICE_ID);
        if (localDeviceId == null || localDeviceId.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID is required");
            return;
        }

        // Set device ID and zone ID before parent initialization
        setDeviceId(localDeviceId);
        this.zoneId = localZoneId;
        logger.debug("Set zone device ID: {} for zone: {} on thing: {}", localDeviceId, localZoneId, getThing().getUID());

        // FIX: Remove API key registration - zones should use bridge API key
        // The parent initialize() will get the bridge handler and use its API
        
        // Call parent initialization - this will create channels and start status updates
        super.initialize();
        
        logger.debug("Rachio zone handler initialization complete for thing: {}", getThing().getUID());
    }

    @Override
    protected void createChannels() {
        logger.debug("Creating zone channels for thing: {}", getThing().getUID());
        
        // Create zone property channels
        createChannel("zone#zoneName", "Zone Name", "String", "Status");
        createChannel("zone#zoneEnabled", "Zone Enabled", "Switch", "Status");
        createChannel("zone#zoneRuntime", "Zone Runtime", "Number", "Status");
        
        // Create zone control channel
        createChannel(CHANNEL_START_ZONE, "Start Zone", "Number", "Control");
        
        logger.debug("All zone channels created for thing: {}", getThing().getUID());
    }

    @Override
    protected void updateDeviceStatus() {
        // Zone-specific status updates
        String localDeviceId = getDeviceId();
        String localZoneId = this.zoneId;
        
        if (localDeviceId == null || localZoneId == null) {
            logger.debug("Device ID or Zone ID is null, cannot update zone status for thing: {}", getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID or Zone ID not set");
            return;
        }

        try {
            // Try to get zone data from API for status updates
            RachioZone zoneData = rachioHttp.getZone(localZoneId);
            if (zoneData != null) {
                updateZoneChannels(zoneData);
                updateStatus(ThingStatus.ONLINE);
                logger.debug("Zone status updated for zone: {} on device: {} for thing: {}", 
                            localZoneId, localDeviceId, getThing().getUID());
            } else {
                logger.debug("No zone data returned for zone: {} on device: {} for thing: {}", 
                            localZoneId, localDeviceId, getThing().getUID());
                updateStatus(ThingStatus.ONLINE); // Still online, just no specific zone data
            }
            
        } catch (RachioException e) {
            logger.debug("API error updating zone status for thing {}: {}", getThing().getUID(), e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (Exception e) {
            logger.debug("Error updating zone status for thing {}: {}", getThing().getUID(), e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Update zone-specific channels with zone data
     */
    private void updateZoneChannels(RachioZone zone) {
        try {
            // Update zone name channel
            ChannelUID nameChannel = new ChannelUID(getThing().getUID(), "zone#zoneName");
            updateState(nameChannel, new StringType(zone.name));
            
            // Update zone enabled channel
            ChannelUID enabledChannel = new ChannelUID(getThing().getUID(), "zone#zoneEnabled");
            updateState(enabledChannel, zone.enabled ? OnOffType.ON : OnOffType.OFF);
            
            // Update zone runtime channel
            ChannelUID runtimeChannel = new ChannelUID(getThing().getUID(), "zone#zoneRuntime");
            updateState(runtimeChannel, new DecimalType(zone.runtime));
            
            logger.trace("Updated zone channels for: {} on thing: {}", zone.name, getThing().getUID());
        } catch (Exception e) {
            logger.debug("Error updating zone channels for thing {}: {}", getThing().getUID(), e.getMessage());
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {} on thing: {}", command, channelUID.getId(), getThing().getUID());
        
        // First call parent to handle refresh commands
        super.handleCommand(channelUID, command);
        
        // Then handle zone-specific commands
        String localZoneId = this.zoneId;
        String localDeviceId = getDeviceId();
        
        if (localZoneId == null || localDeviceId == null) {
            logger.debug("Zone ID or Device ID is null, cannot handle command for thing: {}", getThing().getUID());
            return;
        }

        try {
            String thingId = getThing().getUID().toString();

            if (CHANNEL_START_ZONE.equals(channelUID.getId())) {
                handleStartZoneCommand(channelUID, command, thingId, localZoneId);
            } else if ("zone#zoneEnabled".equals(channelUID.getId())) {
                handleZoneEnabledCommand(channelUID, command, thingId, localZoneId);
            } else {
                logger.debug("Unhandled zone command channel: {} on thing: {}", channelUID.getId(), getThing().getUID());
            }
        } catch (Exception e) {
            logger.debug("Failed to execute command {} for thing {}: {}", 
                        channelUID.getId(), getThing().getUID(), e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Handle start zone command with duration validation
     */
    private void handleStartZoneCommand(ChannelUID channelUID, Command command, String thingId, String zoneId) {
        try {
            if (command instanceof DecimalType) {
                int duration = ((DecimalType) command).intValue();
                if (duration > 0 && duration <= 10800) { // 0-3 hours (10800 seconds)
                    rachioHttp.startZone(thingId, zoneId, duration);
                    logger.info("Started zone {} for {} seconds on thing: {}", zoneId, duration, getThing().getUID());
                } else {
                    logger.warn("Invalid duration for zone start: {} seconds on thing: {}", duration, getThing().getUID());
                }
            } else if (command instanceof OnOffType && command == OnOffType.ON) {
                // If it's a switch, use default duration from bridge configuration
                int duration = getDefaultRuntimeFromBridge();
                rachioHttp.startZone(thingId, zoneId, duration);
                logger.info("Started zone {} for {} seconds (default) on thing: {}", zoneId, duration, getThing().getUID());
                // Reset the switch
                updateState(channelUID, OnOffType.OFF);
            }
        } catch (RachioException e) {
            logger.debug("API error starting zone {}: {}", zoneId, e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Handle zone enabled/disabled command
     */
    private void handleZoneEnabledCommand(ChannelUID channelUID, Command command, String thingId, String zoneId) {
        try {
            if (command instanceof OnOffType) {
                boolean enable = (command == OnOffType.ON);
                rachioHttp.setZoneEnabled(thingId, zoneId, enable);
                logger.info("{} zone {} on thing: {}", enable ? "Enabled" : "Disabled", zoneId, getThing().getUID());
                // Keep the channel state as set by the user
                updateState(channelUID, (OnOffType) command);
            }
        } catch (RachioException e) {
            logger.debug("API error setting zone enabled state for {}: {}", zoneId, e.getMessage());
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

    @Nullable
    public String getZoneId() {
        return zoneId;
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio zone handler for thing: {}", getThing().getUID());
        this.zoneId = null;
        super.dispose();
    }
}