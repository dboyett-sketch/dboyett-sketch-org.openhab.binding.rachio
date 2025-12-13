package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiClient;
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
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceHandler extends RachioHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);
    private @Nullable RachioDeviceConfiguration config;
    private @Nullable RachioApiClient apiClient;
    private @Nullable ScheduledFuture<?> pollingJob;

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler for {}", getThing().getUID());
        config = getConfigAs(RachioDeviceConfiguration.class);
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No bridge available");
            return;
        }
        
        this.apiClient = bridgeHandler.getApiClient();
        if (this.apiClient == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge API client not ready");
            return;
        }
        
        updateStatus(ThingStatus.UNKNOWN);
        // Perform initial poll
        scheduler.execute(this::pollStatus);
        // Start regular polling (e.g., every 5 minutes)
        pollingJob = scheduler.scheduleWithFixedDelay(this::pollStatus, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio device handler for {}", getThing().getUID());
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null && !localPollingJob.isCancelled()) {
            localPollingJob.cancel(true);
            pollingJob = null;
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
        RachioApiClient localApiClient = apiClient;
        
        if (localConfig == null || localConfig.getDeviceId() == null) {
            logger.warn("Device configuration not available");
            return;
        }
        if (localApiClient == null) {
            logger.warn("API client not available");
            return;
        }
        
        String channelId = channelUID.getIdWithoutGroup();
        String deviceId = localConfig.getDeviceId();
        
        try {
            switch (channelId) {
                case CHANNEL_RAIN_DELAY:
                    if (command instanceof DecimalType) {
                        int hours = ((DecimalType) command).intValue();
                        localApiClient.setRainDelay(deviceId, hours);
                        scheduler.execute(this::pollStatus);
                    } else if (command instanceof QuantityType) {
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int hours = quantity.toUnit(Units.HOUR).intValue();
                        localApiClient.setRainDelay(deviceId, hours);
                        scheduler.execute(this::pollStatus);
                    }
                    break;
                case CHANNEL_DEVICE_PAUSE:
                    if (command instanceof OnOffType) {
                        boolean pause = command == OnOffType.ON;
                        localApiClient.pauseDevice(deviceId, pause);
                        scheduler.execute(this::pollStatus);
                    }
                    break;
                case CHANNEL_WATER_BUDGET:
                    if (command instanceof DecimalType) {
                        int percentage = ((DecimalType) command).intValue();
                        if (percentage >= 0 && percentage <= 300) {
                            localApiClient.setWaterBudget(deviceId, percentage);
                            scheduler.execute(this::pollStatus);
                        }
                    }
                    break;
                case CHANNEL_ZONE_ALL:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        localApiClient.runAllZones(deviceId);
                        scheduler.execute(this::pollStatus);
                    }
                    break;
                case CHANNEL_ZONE_NEXT:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        localApiClient.runNextZone(deviceId);
                        scheduler.execute(this::pollStatus);
                    }
                    break;
                case CHANNEL_DEVICE_STATUS:
                    if (command instanceof OnOffType) {
                        boolean turnOn = command == OnOffType.ON;
                        if (turnOn) {
                            localApiClient.turnDeviceOn(deviceId);
                        } else {
                            localApiClient.turnDeviceOff(deviceId);
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
    protected void pollStatus() {
        RachioDeviceConfiguration localConfig = config;
        RachioApiClient localApiClient = apiClient;
        
        if (localConfig == null || localConfig.getDeviceId() == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Device configuration not available");
            return;
        }
        if (localApiClient == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "API client not available");
            return;
        }
        
        try {
            // Get the device data using the new ApiClient
            RachioDevice device = localApiClient.getDevice(localConfig.getDeviceId());
            if (device != null) {
                updateDevice(device);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device not found via API");
            }
        } catch (RachioApiException e) {
            logger.error("Error polling device status via API", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Update device with new data
     * @param device device data
     */
    protected void updateDevice(RachioDevice device) {
        updateProperties(device);
        updateChannels(device);
    }

    /**
     * Update thing properties from device data
     * @param device device data
     */
    protected void updateProperties(RachioDevice device) {
        Map<String, String> properties = new HashMap<>();
        properties.put(PROPERTY_ID, device.getId());
        properties.put(PROPERTY_NAME, device.getName());
        properties.put(PROPERTY_MODEL, device.getModel());
        properties.put(PROPERTY_STATUS, device.getStatus());
        if (device.getSerialNumber() != null) {
            properties.put(PROPERTY_SERIAL_NUMBER, device.getSerialNumber());
        }
        if (device.getMacAddress() != null) {
            properties.put(PROPERTY_MAC_ADDRESS, device.getMacAddress());
        }
        properties.put(PROPERTY_ZONE_COUNT, String.valueOf(device.getZones() != null ? device.getZones().size() : 0));
        updateProperties(properties);
    }

    /**
     * Update channels from device data
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
        // Note: Rate limit channels are no longer updated here as they are managed by the bridge.
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

    // ===== NOTE: RachioStatusListener methods have been REMOVED =====
    // The listener pattern for status updates is no longer used in this version.
    // The bridge's ApiClient handles central polling and event distribution if needed.
}
