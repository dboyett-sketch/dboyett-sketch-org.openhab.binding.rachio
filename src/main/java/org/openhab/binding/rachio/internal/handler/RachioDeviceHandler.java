package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDeviceHandler} is responsible for handling commands and status
 * updates for Rachio Controller Devices.
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler implements RachioStatusListener {

    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    private @Nullable RachioDeviceConfiguration config;
    private @Nullable RachioDevice deviceData;
    private @Nullable ScheduledFuture<?> pollingJob;
    
    private int pollInterval = 120; // 2 minutes default
    private ZonedDateTime lastUpdated;
    private boolean deviceOnline = false;

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshDeviceData();
            return;
        }

        // Device channels are mostly read-only, but can handle some commands
        String channelId = channelUID.getIdWithoutGroup();
        
        switch (channelId) {
            case CHANNEL_DEVICE_ONLINE:
                if (command instanceof OnOffType) {
                    // This is a status channel, not a control channel
                    logger.debug("Device online status is read-only");
                }
                break;
                
            case CHANNEL_STATUS:
                if (command instanceof RefreshType) {
                    refreshDeviceData();
                }
                break;
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler");
        
        config = getConfigAs(RachioDeviceConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No bridge assigned");
            return;
        }

        RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) bridge.getHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge handler not available");
            return;
        }

        // Register as listener to bridge
        bridgeHandler.registerListener(this);

        // Create dynamic channels
        createDeviceChannels();

        // Start polling
        startPolling();

        // Initial refresh
        refreshDeviceData();
    }

    private void createDeviceChannels() {
        logger.debug("Creating device channels");
        
        List<Channel> channels = new ArrayList<>();
        ChannelUID thingUID = getThing().getUID();

        // Device Information Channels
        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_DEVICE_NAME), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_DEVICE_NAME))
                .withLabel("Device Name")
                .withDescription("Name of the Rachio controller")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_DEVICE_STATUS), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_DEVICE_STATUS))
                .withLabel("Device Status")
                .withDescription("Current status of the device")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_DEVICE_ONLINE), "Switch")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_DEVICE_ONLINE))
                .withLabel("Device Online")
                .withDescription("Whether the device is online")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_DEVICE_SERIAL), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_DEVICE_SERIAL))
                .withLabel("Serial Number")
                .withDescription("Device serial number")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_DEVICE_MODEL), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_DEVICE_MODEL))
                .withLabel("Device Model")
                .withDescription("Device model information")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_DEVICE_ZONE_COUNT), "Number")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_DEVICE_ZONE_COUNT))
                .withLabel("Zone Count")
                .withDescription("Number of zones on this device")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_DEVICE_LAST_HEARTBEAT), "DateTime")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_DEVICE_LAST_HEARTBEAT))
                .withLabel("Last Heartbeat")
                .withDescription("When device last reported status")
                .build());

        // Device Control Channels (delegated to bridge)
        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_STATUS), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_STATUS))
                .withLabel("Status")
                .withDescription("Current device status")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_LAST_UPDATED), "DateTime")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_LAST_UPDATED))
                .withLabel("Last Updated")
                .withDescription("When device data was last updated")
                .build());

        // Update the thing with all channels
        updateThing(editThing().withChannels(channels).build());
        
        logger.debug("Created {} device channels", channels.size());
    }

    private void startPolling() {
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
        }

        this.pollingJob = scheduler.scheduleWithFixedDelay(this::refreshDeviceData, 10, pollInterval, TimeUnit.SECONDS);
        logger.debug("Started device polling every {} seconds", pollInterval);
    }

    private void refreshDeviceData() {
        try {
            Bridge bridge = getBridge();
            if (bridge == null) {
                logger.debug("Cannot refresh device data - no bridge");
                return;
            }

            RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) bridge.getHandler();
            if (bridgeHandler == null) {
                logger.debug("Cannot refresh device data - bridge handler not available");
                return;
            }

            RachioDeviceConfiguration config = this.config;
            if (config == null || config.deviceId == null || config.deviceId.isEmpty()) {
                logger.debug("Cannot refresh device data - missing device ID");
                return;
            }

            // Get device data from bridge cache
            RachioDevice device = bridgeHandler.getDeviceData(config.deviceId);
            if (device != null) {
                updateDeviceData(device);
            } else {
                // Device not in cache, try to get it directly
                logger.debug("Device not in cache, will refresh on next bridge poll");
            }

            lastUpdated = ZonedDateTime.now();
            updateState(CHANNEL_LAST_UPDATED, new DateTimeType(lastUpdated));
            updateStatus(ThingStatus.ONLINE);

        } catch (Exception e) {
            logger.debug("Error refreshing device data: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void updateDeviceData(RachioDevice device) {
        this.deviceData = device;
        RachioDeviceConfiguration config = this.config;
        
        if (config == null || device == null) {
            return;
        }

        logger.debug("Updating device data for {} (ID: {})", device.name, device.id);

        // Update basic device info
        updateState(CHANNEL_DEVICE_NAME, new StringType(device.name));
        updateState(CHANNEL_DEVICE_STATUS, new StringType(device.status));
        updateState(CHANNEL_DEVICE_ONLINE, OnOffType.from("ONLINE".equals(device.status)));
        updateState(CHANNEL_DEVICE_SERIAL, new StringType(device.serialNumber));
        updateState(CHANNEL_DEVICE_MODEL, new StringType(device.model));
        
        if (device.zones != null) {
            updateState(CHANNEL_DEVICE_ZONE_COUNT, new DecimalType(device.zones.size()));
        }
        
        // Update heartbeat if available
        if (device.lastHeartbeat != null) {
            updateState(CHANNEL_DEVICE_LAST_HEARTBEAT, new DateTimeType(device.lastHeartbeat));
        }
        
        // Update overall status
        updateState(CHANNEL_STATUS, new StringType(device.status));

        // Update device online status
        boolean newOnlineStatus = "ONLINE".equals(device.status);
        if (newOnlineStatus != deviceOnline) {
            deviceOnline = newOnlineStatus;
            logger.info("Device {} is now {}", device.name, deviceOnline ? "ONLINE" : "OFFLINE");
        }

        // Update properties for thing
        updateDeviceProperties(device);
    }

    private void updateDeviceProperties(RachioDevice device) {
        Map<String, String> properties = getThing().getProperties();
        
        // Only update if changed
        boolean needsUpdate = false;
        
        if (!device.id.equals(properties.get(PROPERTY_DEVICE_ID))) {
            needsUpdate = true;
        }
        if (!device.name.equals(properties.get(PROPERTY_DEVICE_NAME))) {
            needsUpdate = true;
        }
        if (!device.serialNumber.equals(properties.get(PROPERTY_DEVICE_SERIAL))) {
            needsUpdate = true;
        }
        if (!device.model.equals(properties.get(PROPERTY_DEVICE_MODEL))) {
            needsUpdate = true;
        }
        
        if (needsUpdate) {
            Map<String, String> newProperties = new java.util.HashMap<>();
            newProperties.put(PROPERTY_DEVICE_ID, device.id);
            newProperties.put(PROPERTY_DEVICE_NAME, device.name);
            newProperties.put(PROPERTY_DEVICE_SERIAL, device.serialNumber);
            newProperties.put(PROPERTY_DEVICE_MODEL, device.model);
            newProperties.put(PROPERTY_ZONES, String.valueOf(device.zones != null ? device.zones.size() : 0));
            newProperties.put(PROPERTY_ONLINE, String.valueOf(deviceOnline));
            
            // Add MAC address if available
            if (device.macAddress != null && !device.macAddress.isEmpty()) {
                newProperties.put(PROPERTY_MAC, device.macAddress);
            }
            
            updateProperties(newProperties);
            logger.debug("Updated device properties for {}", device.name);
        }
    }

    @Override
    public void zoneStatusChanged(RachioZone zone) {
        // Device handler doesn't need to react to zone status directly
        // But we can update if it's a zone from this device
        if (deviceData != null && deviceData.zones != null) {
            for (RachioZone deviceZone : deviceData.zones) {
                if (deviceZone.id.equals(zone.id)) {
                    logger.debug("Zone status update for device {}", deviceData.name);
                    // Could update device status if needed
                    break;
                }
            }
        }
    }

    @Override
    public void deviceStatusChanged(String deviceId, boolean online) {
        RachioDeviceConfiguration config = this.config;
        if (config != null && config.deviceId != null && config.deviceId.equals(deviceId)) {
            logger.debug("Device status changed: {} is {}", deviceId, online ? "online" : "offline");
            
            // Update device online status
            deviceOnline = online;
            updateState(CHANNEL_DEVICE_ONLINE, OnOffType.from(online));
            updateState(CHANNEL_DEVICE_STATUS, new StringType(online ? STATUS_ONLINE : STATUS_OFFLINE));
            updateState(CHANNEL_STATUS, new StringType(online ? STATUS_ONLINE : STATUS_OFFLINE));
            
            if (online) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Device offline");
            }
        }
    }

    @Override
    public void webhookEventReceived(String deviceId, String eventType, String eventData) {
        RachioDeviceConfiguration config = this.config;
        if (config != null && config.deviceId != null && config.deviceId.equals(deviceId)) {
            logger.debug("Webhook event received for device {}: {}", deviceId, eventType);
            
            // Update last heartbeat on any event
            updateState(CHANNEL_DEVICE_LAST_HEARTBEAT, new DateTimeType(ZonedDateTime.now()));
            
            // Handle specific event types
            switch (eventType) {
                case EVENT_DEVICE_STATUS:
                    // Update device status from webhook
                    if (eventData.contains("\"status\":\"ONLINE\"")) {
                        updateState(CHANNEL_DEVICE_STATUS, new StringType(STATUS_ONLINE));
                        updateState(CHANNEL_DEVICE_ONLINE, OnOffType.ON);
                    } else if (eventData.contains("\"status\":\"OFFLINE\"")) {
                        updateState(CHANNEL_DEVICE_STATUS, new StringType(STATUS_OFFLINE));
                        updateState(CHANNEL_DEVICE_ONLINE, OnOffType.OFF);
                    }
                    break;
                    
                case EVENT_RAIN_DELAY:
                    // Update rain delay status
                    logger.info("Rain delay event received for device {}", deviceId);
                    break;
                    
                case EVENT_WEATHER_INTEL:
                    // Weather intelligence event
                    logger.debug("Weather intelligence event received");
                    break;
            }
        }
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
        }
        this.pollingJob = null;

        // Unregister from bridge
        Bridge bridge = getBridge();
        if (bridge != null) {
            RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) bridge.getHandler();
            if (bridgeHandler != null) {
                bridgeHandler.unregisterListener(this);
            }
        }

        super.dispose();
        logger.debug("Disposed device handler");
    }

    // Getters for other handlers
    public @Nullable RachioDevice getDeviceData() {
        return deviceData;
    }

    public @Nullable RachioDeviceConfiguration getDeviceConfiguration() {
        return config;
    }

    public boolean isDeviceOnline() {
        return deviceOnline;
    }

    public void setPollInterval(int interval) {
        this.pollInterval = interval;
        startPolling();
    }
    
    /**
     * Get a specific zone from this device
     */
    public @Nullable RachioZone getZone(String zoneId) {
        RachioDevice device = deviceData;
        if (device != null && device.zones != null) {
            for (RachioZone zone : device.zones) {
                if (zone.id.equals(zoneId)) {
                    return zone;
                }
            }
        }
        return null;
    }
    
    /**
     * Get all zones from this device
     */
    public @Nullable List<RachioZone> getAllZones() {
        RachioDevice device = deviceData;
        return device != null ? device.zones : null;
    }
}
