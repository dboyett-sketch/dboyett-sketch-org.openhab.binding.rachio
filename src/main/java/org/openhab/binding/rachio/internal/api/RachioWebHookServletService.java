package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
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
 * The {@link RachioDeviceHandler} is responsible for handling commands for device channels
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler implements RachioStatusListener {

    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable RachioDevice device;

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler");
        updateStatus(ThingStatus.UNKNOWN);

        bridgeHandler = (RachioBridgeHandler) getBridge().getHandler();
        if (bridgeHandler != null) {
            // CORRECTION: Register with bridge handler for status updates
            bridgeHandler.registerStatusListener(this);
            scheduleDeviceUpdate();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No bridge available");
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio device handler");
        
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            // CORRECTION: Unregister from bridge handler
            handler.unregisterStatusListener(this);
        }
        
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Handling command {} for channel {}", command, channelUID);

        if (command instanceof RefreshType) {
            refreshDevice();
            return;
        }

        RachioBridgeHandler handler = bridgeHandler;
        RachioDeviceConfiguration cfg = getConfigAs(RachioDeviceConfiguration.class);

        if (handler == null) {
            logger.warn("Bridge handler not available");
            return;
        }

        String channelId = channelUID.getIdWithoutGroup();

        try {
            switch (channelId) {
                case CHANNEL_RAIN_DELAY:
                    if (command instanceof DecimalType) {
                        int hours = ((DecimalType) command).intValue();
                        if (handler.getHttpClient() != null) {
                            handler.getHttpClient().rainDelay(cfg.getDeviceId(), hours);
                            scheduleDeviceUpdate();
                        }
                    } else if (command instanceof QuantityType) {
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int hours = quantity.toUnit(Units.HOUR).intValue();
                        if (handler.getHttpClient() != null) {
                            handler.getHttpClient().rainDelay(cfg.getDeviceId(), hours);
                            scheduleDeviceUpdate();
                        }
                    }
                    break;

                case CHANNEL_DEVICE_PAUSE:
                    if (command instanceof OnOffType) {
                        boolean pause = command == OnOffType.ON;
                        if (handler.getHttpClient() != null) {
                            handler.getHttpClient().pauseDevice(cfg.getDeviceId(), pause);
                            scheduleDeviceUpdate();
                        }
                    }
                    break;

                case CHANNEL_WATER_BUDGET:
                    if (command instanceof DecimalType) {
                        int percentage = ((DecimalType) command).intValue();
                        if (percentage >= 0 && percentage <= 300) {
                            if (handler.getHttpClient() != null) {
                                handler.getHttpClient().setWaterBudget(cfg.getDeviceId(), percentage);
                                scheduleDeviceUpdate();
                            }
                        }
                    }
                    break;

                case CHANNEL_ZONE_ALL:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        if (handler.getHttpClient() != null) {
                            handler.getHttpClient().runAllZones(cfg.getDeviceId());
                            scheduleDeviceUpdate();
                        }
                    }
                    break;

                case CHANNEL_ZONE_NEXT:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        if (handler.getHttpClient() != null) {
                            handler.getHttpClient().runNextZone(cfg.getDeviceId());
                            scheduleDeviceUpdate();
                        }
                    }
                    break;

                default:
                    logger.debug("Unhandled command for channel {}", channelId);
            }
        } catch (Exception e) {
            logger.error("Error handling command {} for channel {}", command, channelId, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    @Override
    public void onStatusChanged(ThingStatus status, ThingStatusDetail detail, @Nullable String message) {
        // CORRECTION: Implemented the required interface method
        updateStatus(status, detail, message);
        
        if (status == ThingStatus.ONLINE) {
            scheduleDeviceUpdate();
        }
    }

    /**
     * Schedule a device update
     */
    private void scheduleDeviceUpdate() {
        scheduler.execute(this::refreshDevice);
    }

    /**
     * Refresh device data from bridge
     */
    private void refreshDevice() {
        RachioBridgeHandler handler = bridgeHandler;
        RachioDeviceConfiguration cfg = getConfigAs(RachioDeviceConfiguration.class);

        if (handler == null) {
            logger.debug("Bridge handler not available for refresh");
            return;
        }

        try {
            RachioDevice device = handler.getDevice(cfg.getDeviceId());
            if (device != null) {
                updateDevice(device);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device not found");
            }
        } catch (Exception e) {
            logger.error("Error refreshing device", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Update device with new data
     *
     * @param device device data
     */
    protected void updateDevice(RachioDevice device) {
        this.device = device;
        updateProperties(device);
        updateChannels(device);
    }

    /**
     * Update thing properties from device data
     *
     * @param device device data
     */
    protected void updateProperties(RachioDevice device) {
        Map<String, String> properties = new HashMap<>();
        
        // CORRECTION: Using constants from RachioBindingConstants
        // Basic properties
        properties.put(PROPERTY_ID, device.getId());
        properties.put(PROPERTY_NAME, device.getName());
        properties.put(PROPERTY_MODEL, device.getModel());
        properties.put(PROPERTY_STATUS, device.getStatus());
        
        // Network properties
        if (device.getSerialNumber() != null) {
            properties.put(PROPERTY_SERIAL_NUMBER, device.getSerialNumber());
        }
        
        if (device.getMacAddress() != null) {
            properties.put(PROPERTY_MAC_ADDRESS, device.getMacAddress());
        }
        
        // Zone count
        properties.put(PROPERTY_ZONE_COUNT, String.valueOf(device.getZones() != null ? device.getZones().size() : 0));
        
        // Update thing properties
        updateProperties(properties);
    }

    /**
     * Update channels from device data
     *
     * @param device device data
     */
    protected void updateChannels(RachioDevice device) {
        // Device status channels
        updateState(CHANNEL_DEVICE_STATUS, "ONLINE".equals(device.getStatus()) ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_ONLINE, "ONLINE".equals(device.getStatus()) ? OnOffType.ON : OnOffType.OFF);
        
        // CORRECTION: Using CHANNEL_RAIN_DELAY constant
        // Rain delay channel
        if (device.getRainDelay() != null) {
            updateState(CHANNEL_RAIN_DELAY, new DecimalType(device.getRainDelay()));
        }
        
        // Water budget channel
        if (device.getWaterBudget() != null) {
            updateState(CHANNEL_WATER_BUDGET, new DecimalType(device.getWaterBudget()));
        }
        
        // Pause channel
        if (device.isPaused() != null) {
            updateState(CHANNEL_DEVICE_PAUSE, device.isPaused() ? OnOffType.ON : OnOffType.OFF);
        }
        
        // Weather skip channel
        if (device.isWeatherSkip() != null) {
            updateState(CHANNEL_WEATHER_SKIP, device.isWeatherSkip() ? OnOffType.ON : OnOffType.OFF);
        }
        
        // Rain sensor channel
        if (device.isRainSensor() != null) {
            updateState(CHANNEL_RAIN_SENSOR, device.isRainSensor() ? OnOffType.ON : OnOffType.OFF);
        }
        
        // Rate limit monitoring channels (if available from bridge)
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null && handler.getHttpClient() != null) {
            // These would be updated based on rate limit info from the HTTP client
            // updateState(CHANNEL_RATE_LIMIT_REMAINING, new DecimalType(remaining));
            // updateState(CHANNEL_RATE_LIMIT_PERCENTAGE, new DecimalType(percentage));
        }
    }

    /**
     * Get the current device data
     *
     * @return device data or null
     */
    public @Nullable RachioDevice getDevice() {
        return device;
    }

    /**
     * Force a poll of devices from bridge
     * CORRECTION: This method now properly calls the bridge's pollDevices() method
     */
    protected void pollDevices() {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            // Call package-private method on bridge (now fixed to be accessible)
            handler.pollDevices();
        }
    }

    // ===== Additional RachioStatusListener interface methods with default implementations =====
    
    @Override
    public void onDeviceUpdated(String deviceId) {
        RachioDeviceConfiguration cfg = getConfigAs(RachioDeviceConfiguration.class);
        if (deviceId.equals(cfg.getDeviceId())) {
            logger.debug("Device {} update received via listener", deviceId);
            scheduleDeviceUpdate();
        }
    }

    @Override
    public void onZoneUpdated(String zoneId) {
        // Device handler doesn't need to react to zone updates
    }

    @Override
    public void onWebhookEvent(String eventType, @Nullable String deviceId, @Nullable String zoneId) {
        RachioDeviceConfiguration cfg = getConfigAs(RachioDeviceConfiguration.class);
        if (deviceId != null && deviceId.equals(cfg.getDeviceId())) {
            logger.debug("Webhook event {} received for device {}", eventType, deviceId);
            scheduleDeviceUpdate();
        }
    }

    @Override
    public void onRateLimitChanged(int remainingRequests, int limit, long resetTime) {
        // Update rate limit channels if needed
        updateState(CHANNEL_RATE_LIMIT_REMAINING, new DecimalType(remainingRequests));
        if (limit > 0) {
            int percentage = (int) ((remainingRequests / (double) limit) * 100);
            updateState(CHANNEL_RATE_LIMIT_PERCENTAGE, new DecimalType(percentage));
        }
    }

    @Override
    public void onConnectionChanged(boolean connected, @Nullable String message) {
        if (!connected) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                    message != null ? message : "Lost connection to Rachio API");
        } else {
            updateStatus(ThingStatus.ONLINE);
            scheduleDeviceUpdate();
        }
    }

    @Override
    public void onConfigurationUpdated() {
        logger.debug("Configuration updated, refreshing device");
        scheduleDeviceUpdate();
    }

    @Override
    public void onError(String errorMessage, @Nullable Throwable exception) {
        logger.error("Error received: {}", errorMessage, exception);
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
    }

    @Override
    public @Nullable String getThingId() {
        return getThing().getUID().getId();
    }

    @Override
    public boolean isForDevice(String deviceId) {
        RachioDeviceConfiguration cfg = getConfigAs(RachioDeviceConfiguration.class);
        return deviceId.equals(cfg.getDeviceId());
    }

    @Override
    public void onDispose() {
        // Cleanup if needed
    }
}
