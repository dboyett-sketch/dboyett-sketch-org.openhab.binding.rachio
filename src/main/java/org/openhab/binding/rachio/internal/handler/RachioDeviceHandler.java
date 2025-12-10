package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
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
public class RachioDeviceHandler extends RachioHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    private @Nullable RachioDevice device;
    private @Nullable RachioDeviceConfiguration config;

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler for {}", getThing().getUID());
        
        config = getConfigAs(RachioDeviceConfiguration.class);
        
        // Get bridge handler to access HTTP client
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            // Get HTTP client from bridge
            rachioHttp = bridgeHandler.getRachioHttp();
            rachioSecurity = bridgeHandler.getRachioSecurity();
            
            // Register with bridge for status updates
            bridgeHandler.registerStatusListener(this);
            
            updateStatus(ThingStatus.UNKNOWN);
            
            // Start initial poll
            scheduler.execute(this::pollStatus);
            
            // Start regular polling
            startPolling();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No bridge available");
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio device handler for {}", getThing().getUID());
        
        // Unregister from bridge
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            bridgeHandler.unregisterStatusListener(this);
        }
        
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Handling command {} for channel {}", command, channelUID);

        if (command instanceof RefreshType) {
            scheduler.execute(this::pollStatus);
            return;
        }

        RachioDeviceConfiguration localConfig = config;
        if (localConfig == null || localConfig.getDeviceId() == null) {
            logger.warn("Device configuration not available");
            return;
        }

        RachioHttp localHttpClient = rachioHttp;
        if (localHttpClient == null) {
            logger.warn("HTTP client not available");
            return;
        }

        String channelId = channelUID.getIdWithoutGroup();

        try {
            switch (channelId) {
                case CHANNEL_RAIN_DELAY:
                    if (command instanceof DecimalType) {
                        int hours = ((DecimalType) command).intValue();
                        localHttpClient.rainDelay(localConfig.getDeviceId(), hours);
                        scheduler.execute(this::pollStatus);
                    } else if (command instanceof QuantityType) {
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int hours = quantity.toUnit(Units.HOUR).intValue();
                        localHttpClient.rainDelay(localConfig.getDeviceId(), hours);
                        scheduler.execute(this::pollStatus);
                    }
                    break;

                case CHANNEL_DEVICE_PAUSE:
                    if (command instanceof OnOffType) {
                        boolean pause = command == OnOffType.ON;
                        localHttpClient.pauseDevice(localConfig.getDeviceId(), pause);
                        scheduler.execute(this::pollStatus);
                    }
                    break;

                case CHANNEL_WATER_BUDGET:
                    if (command instanceof DecimalType) {
                        int percentage = ((DecimalType) command).intValue();
                        if (percentage >= 0 && percentage <= 300) {
                            localHttpClient.setWaterBudget(localConfig.getDeviceId(), percentage);
                            scheduler.execute(this::pollStatus);
                        }
                    }
                    break;

                case CHANNEL_ZONE_ALL:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        localHttpClient.runAllZones(localConfig.getDeviceId());
                        scheduler.execute(this::pollStatus);
                    }
                    break;

                case CHANNEL_ZONE_NEXT:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        localHttpClient.runNextZone(localConfig.getDeviceId());
                        scheduler.execute(this::pollStatus);
                    }
                    break;

                case CHANNEL_DEVICE_STATUS:
                    if (command instanceof OnOffType) {
                        boolean turnOn = command == OnOffType.ON;
                        if (turnOn) {
                            localHttpClient.turnDeviceOn(localConfig.getDeviceId());
                        } else {
                            localHttpClient.turnDeviceOff(localConfig.getDeviceId());
                        }
                        scheduler.execute(this::pollStatus);
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

    /**
     * Implementation of abstract method from RachioHandler
     */
    @Override
    protected void pollStatus() throws RachioApiException {
        RachioDeviceConfiguration localConfig = config;
        if (localConfig == null || localConfig.getDeviceId() == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                    "Device configuration not available");
            return;
        }

        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No bridge available");
            return;
        }

        try {
            RachioDevice device = bridgeHandler.getDevice(localConfig.getDeviceId());
            if (device != null) {
                updateDevice(device);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device not found");
            }
        } catch (Exception e) {
            logger.error("Error polling device status", e);
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
        
        // Update rate limit channels (inherited from RachioHandler)
        updateRateLimitChannels();
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
     * Get the bridge handler
     */
    private @Nullable RachioBridgeHandler getBridgeHandler() {
        Thing bridge = getBridge();
        if (bridge != null && bridge.getHandler() instanceof RachioBridgeHandler) {
            return (RachioBridgeHandler) bridge.getHandler();
        }
        return null;
    }

    // ===== RachioStatusListener interface implementations =====
    
    @Override
    public void onDeviceUpdated(String deviceId) {
        RachioDeviceConfiguration localConfig = config;
        if (localConfig != null && deviceId.equals(localConfig.getDeviceId())) {
            logger.debug("Device {} update received via listener", deviceId);
            scheduler.execute(this::pollStatus);
        }
    }

    @Override
    public void onZoneUpdated(String zoneId) {
        // Device handler doesn't need to react to zone updates
    }

    @Override
    public void onWebhookEvent(String deviceId, String eventType, 
                               @Nullable String subType, 
                               @Nullable Map<String, Object> eventData) {
        RachioDeviceConfiguration localConfig = config;
        if (localConfig != null && deviceId.equals(localConfig.getDeviceId())) {
            logger.debug("Webhook event {} received for device {}", eventType, deviceId);
            scheduler.execute(this::pollStatus);
        }
    }

    @Override
    public void onRateLimitChanged(int remainingRequests, int limit, long resetTime) {
        // Update rate limit channels via parent class
        scheduler.execute(this::updateRateLimitChannels);
    }

    @Override
    public void onConnectionChanged(boolean connected, @Nullable String message) {
        if (!connected) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                    message != null ? message : "Lost connection to Rachio API");
        } else {
            updateStatus(ThingStatus.ONLINE);
            scheduler.execute(this::pollStatus);
        }
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
        RachioDeviceConfiguration localConfig = config;
        return localConfig != null && deviceId.equals(localConfig.getDeviceId());
    }
    
    @Override
    public boolean isActive() {
        return getThing().getStatus() != ThingStatus.UNINITIALIZED;
    }
    
    @Override
    public String getListenerDescription() {
        return "RachioDeviceHandler[" + getThing().getUID() + "]";
    }
    
    @Override
    public boolean isForZone(String zoneId) {
        // Device handlers don't handle zones
        return false;
    }
}
