package org.openhab.binding.rachio.internal.handler;

import java.time.Instant;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler implements RachioStatusListener {
    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    private @Nullable RachioDeviceConfiguration config;
    private @Nullable RachioBridgeHandler bridgeHandler;

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler");
        
        this.config = getConfigAs(RachioDeviceConfiguration.class);
        this.bridgeHandler = getBridgeHandler();
        
        if (this.config == null || this.config.deviceId == null || this.config.deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID not configured");
            return;
        }
        
        if (this.bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not found");
            return;
        }
        
        // Register with bridge
        this.bridgeHandler.registerStatusListener(this);
        
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            handler.unregisterStatusListener(this);
        }
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {}", command, channelUID.getId());
        
        if (command instanceof RefreshType) {
            refreshDevice();
            return;
        }
        
        // Handle device-specific commands
        switch (channelUID.getId()) {
            case CHANNEL_STOP_WATERING:
                if (command instanceof OnOffType && command == OnOffType.ON) {
                    stopWatering();
                }
                break;
            case CHANNEL_PAUSE_DEVICE:
                if (command instanceof OnOffType) {
                    pauseDevice(command == OnOffType.ON);
                }
                break;
            case CHANNEL_RAIN_DELAY:
                if (command instanceof DecimalType) {
                    int hours = ((DecimalType) command).intValue();
                    setRainDelay(hours);
                }
                break;
        }
    }

    @Override
    public void onDeviceUpdated(@Nullable RachioDevice device) {
        if (device == null) {
            return;
        }
        
        String deviceId = config != null ? config.deviceId : null;
        if (deviceId == null || !deviceId.equals(device.getId())) {
            return; // Not our device
        }
        
        logger.debug("Updating device: {}", device.getName());
        
        // Update status
        if (device.getStatus() != null) {
            updateState(CHANNEL_DEVICE_STATUS, new StringType(device.getStatus()));
        }
        
        // Update online status
        if (device.getOnline() != null) {
            updateState(CHANNEL_DEVICE_ONLINE, device.getOnline() ? OnOffType.ON : OnOffType.OFF);
        }
        
        // Update rain delay expiration
        if (device.getRainDelayExpiration() != null) {
            updateState(CHANNEL_DEVICE_RAIN_DELAY_END_TIME, new DateTimeType(Instant.ofEpochMilli(device.getRainDelayExpiration())));
            
            // Calculate if rain delay is active
            boolean rainDelayActive = device.getRainDelayExpiration() > System.currentTimeMillis();
            updateState(CHANNEL_RAIN_DELAY, rainDelayActive ? OnOffType.ON : OnOffType.OFF);
        } else {
            updateState(CHANNEL_RAIN_DELAY, OnOffType.OFF);
            updateState(CHANNEL_DEVICE_RAIN_DELAY_END_TIME, new StringType(""));
        }
        
        // Update pause status
        if (device.getPaused() != null) {
            updateState(CHANNEL_PAUSE_DEVICE, device.getPaused() ? OnOffType.ON : OnOffType.OFF);
        }
        
        // Update serial number
        if (device.getSerialNumber() != null) {
            updateProperty(PROPERTY_SERIAL_NUMBER, device.getSerialNumber());
        }
        
        // Update model
        if (device.getModel() != null) {
            updateProperty(PROPERTY_MODEL, device.getModel());
        }
        
        // Update firmware version
        if (device.getFirmwareVersion() != null) {
            updateState(CHANNEL_FIRMWARE_VERSION, new StringType(device.getFirmwareVersion()));
        }
        
        // Update MAC address
        if (device.getMacAddress() != null) {
            updateProperty(PROPERTY_MAC_ADDRESS, device.getMacAddress());
        }
        
        // Update zones enabled count
        if (device.getZones() != null) {
            long enabledZones = device.getZones().stream()
                .filter(zone -> zone.isEnabled())
                .count();
            updateState(CHANNEL_ZONES_ENABLED, new DecimalType(enabledZones));
        }
    }

    @Override
    public void onWebhookEvent(String deviceId, String eventType, @Nullable String subType, @Nullable Map<String, Object> eventData) {
        if (config == null || !deviceId.equals(config.deviceId)) {
            return; // Not our device
        }
        
        logger.debug("Received webhook event {} for device {}", eventType, deviceId);
        
        // Trigger refresh on webhook events
        scheduler.submit(() -> {
            try {
                refreshDevice();
            } catch (Exception e) {
                logger.debug("Failed to refresh after webhook", e);
            }
        });
    }

    private void refreshDevice() {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            // Request bridge to refresh device data
            handler.pollDevices();
        }
    }

    private void stopWatering() {
        logger.debug("Stopping all watering");
        // Implementation would call Rachio API via bridge
    }

    private void pauseDevice(boolean pause) {
        logger.debug("{} device", pause ? "Pausing" : "Resuming");
        // Implementation would call Rachio API via bridge
    }

    private void setRainDelay(int hours) {
        logger.debug("Setting rain delay to {} hours", hours);
        // Implementation would call Rachio API via bridge
    }

    private @Nullable RachioBridgeHandler getBridgeHandler() {
        return getBridge() != null && getBridge().getHandler() instanceof RachioBridgeHandler 
            ? (RachioBridgeHandler) getBridge().getHandler() 
            : null;
    }
}
