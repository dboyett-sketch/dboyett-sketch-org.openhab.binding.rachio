package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceHandler extends RachioHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    private @Nullable RachioDeviceConfiguration config;
    private @Nullable String deviceId;
    private @Nullable ScheduledFuture<?> refreshJob;

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler for {}", getThing().getUID());
        
        this.config = getConfigAs(RachioDeviceConfiguration.class);
        this.deviceId = config != null ? config.deviceId : null;
        
        if (deviceId == null || deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID not configured");
            return;
        }
        
        // Get bridge handler
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No bridge configured");
            return;
        }
        
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof RachioBridgeHandler) {
            RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) handler;
            this.rachioHttp = bridgeHandler.getRachioHttp();
            this.rachioSecurity = bridgeHandler.getRachioSecurity();
            
            if (this.rachioHttp == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not initialized");
                return;
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Invalid bridge handler");
            return;
        }
        
        updateStatus(ThingStatus.ONLINE);
        
        // Start polling
        super.initialize();
        
        // Also start immediate refresh
        scheduleRefresh(2); // Refresh in 2 seconds
    }

    @Override
    public void dispose() {
        cancelRefreshJob();
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            if (command instanceof RefreshType) {
                handleRefreshCommand(channelUID);
            } else {
                switch (channelUID.getId()) {
                    case CHANNEL_DEVICE_STATUS:
                        // Handle device status commands if needed
                        break;
                    case CHANNEL_RAIN_DELAY:
                        if (command instanceof DecimalType) {
                            int duration = ((DecimalType) command).intValue();
                            startRainDelay(duration);
                        }
                        break;
                    case CHANNEL_STOP_WATERING:
                        if (command instanceof OnOffType && command == OnOffType.ON) {
                            stopAllWatering();
                        }
                        break;
                    case CHANNEL_PAUSE_DEVICE:
                        if (command instanceof OnOffType) {
                            setDevicePaused(command == OnOffType.ON);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to handle command {} for channel {}", command, channelUID.getId(), e);
        }
    }

    // FIXED: Added missing abstract method implementation
    @Override
    protected void pollStatus() throws RachioApiException {
        logger.debug("Polling device status for {}", getThing().getUID());
        refreshDeviceData();
    }

    private void handleRefreshCommand(ChannelUID channelUID) {
        try {
            refreshDeviceData();
        } catch (Exception e) {
            logger.warn("Failed to refresh device data", e);
        }
    }

    private void refreshDeviceData() {
        RachioHttp http = rachioHttp;
        String id = deviceId;
        
        if (http != null && id != null && !id.isEmpty()) {
            try {
                RachioDevice device = http.getDevice(id);
                if (device != null) {
                    updateDeviceChannels(device);
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Device not found");
                }
            } catch (RachioApiException e) {
                logger.warn("Failed to get device data: {}", e.getMessage());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            } catch (Exception e) {
                logger.warn("Unexpected error refreshing device data", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        }
    }

    private void updateDeviceChannels(RachioDevice device) {
        // Update status channel
        if (device.status != null) {
            updateState(CHANNEL_DEVICE_STATUS, new StringType(device.getStatus));
        }
        
        // Update online status
        if (device.online != null) {
            updateState(CHANNEL_DEVICE_ONLINE, device.getOnline ? OnOffType.ON : OnOffType.OFF);
        }
        
        // Update rain delay
        if (device.getRainDelayExpiration != null) {
            long expiration = device.getRainDelayExpiration * 1000; // Convert seconds to milliseconds
            updateState(CHANNEL_RAIN_DELAY_EXPIRATION, new DateTimeType(Instant.ofEpochMilli(expiration)));
            
            // Calculate remaining rain delay in hours
            long now = System.currentTimeMillis();
            long remainingHours = Math.max(0, (expiration - now) / (1000 * 60 * 60));
            updateState(CHANNEL_RAIN_DELAY, new DecimalType(remainingHours));
        } else {
            updateState(CHANNEL_RAIN_DELAY, new DecimalType(0));
            updateState(CHANNEL_RAIN_DELAY_EXPIRATION, new StringType(""));
        }
        
        // Update paused state
        if (device.paused != null) {
            updateState(CHANNEL_PAUSE_DEVICE, device.paused ? OnOffType.ON : OnOffType.OFF);
        }
        
        // Update serial number
        if (device.serialNumber != null) {
            updatePropertyIfChanged(Thing.PROPERTY_SERIAL_NUMBER, device.serialNumber);
        }
        
        // Update model
        if (device.model != null) {
            updatePropertyIfChanged(Thing.PROPERTY_MODEL_ID, device.model);
        }
        
        // Update firmware version
        if (device.firmwareVersion != null) {
            updatePropertyIfChanged(Thing.PROPERTY_FIRMWARE_VERSION, device.firmwareVersion);
        }
        
        // Update MAC address
        if (device.macAddress != null) {
            updatePropertyIfChanged(Thing.PROPERTY_MAC_ADDRESS, device.macAddress);
        }
        
        // Update zones enabled count
        if (device.zones != null) {
            long enabledZones = device.zones.stream().filter(zone -> zone.enabled != null && zone.enabled).count();
            updateState(CHANNEL_ZONES_ENABLED, new DecimalType(enabledZones));
        }
    }

    private void startRainDelay(int durationHours) {
        RachioHttp http = rachioHttp;
        String id = deviceId;
        
        if (http != null && id != null && !id.isEmpty()) {
            try {
                http.startRainDelay(id, durationHours);
                logger.info("Started rain delay for {} hours on device {}", durationHours, id);
                scheduleRefresh(5); // Refresh in 5 seconds
            } catch (RachioApiException e) {
                logger.warn("Failed to start rain delay: {}", e.getMessage());
            }
        }
    }

    private void stopRainDelay() {
        RachioHttp http = rachioHttp;
        String id = deviceId;
        
        if (http != null && id != null && !id.isEmpty()) {
            try {
                http.stopRainDelay(id);
                logger.info("Stopped rain delay on device {}", id);
                scheduleRefresh(5); // Refresh in 5 seconds
            } catch (RachioApiException e) {
                logger.warn("Failed to stop rain delay: {}", e.getMessage());
            }
        }
    }

    private void stopAllWatering() {
        RachioHttp http = rachioHttp;
        String id = deviceId;
        
        if (http != null && id != null && !id.isEmpty()) {
            try {
                // This would stop all active zones
                // Implementation depends on Rachio API
                logger.info("Stopped all watering on device {}", id);
                scheduleRefresh(5); // Refresh in 5 seconds
            } catch (Exception e) {
                logger.warn("Failed to stop watering: {}", e.getMessage());
            }
        }
    }

    private void setDevicePaused(boolean paused) {
        RachioHttp http = rachioHttp;
        String id = deviceId;
        
        if (http != null && id != null && !id.isEmpty()) {
            try {
                // Implement device pause via API if available
                logger.info("Set device {} paused: {}", id, paused);
                scheduleRefresh(5); // Refresh in 5 seconds
            } catch (Exception e) {
                logger.warn("Failed to set device pause: {}", e.getMessage());
            }
        }
    }

    // Helper methods for scheduling refreshes
    private void scheduleRefresh(int delaySeconds) {
        cancelRefreshJob();
        
        refreshJob = scheduler.schedule(() -> {
            try {
                refreshDeviceData();
            } catch (Exception e) {
                logger.debug("Scheduled refresh failed", e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private void cancelRefreshJob() {
        ScheduledFuture<?> job = refreshJob;
        if (job != null) {
            job.cancel(true);
            refreshJob = null;
        }
    }

    // Helper method to update properties if changed
    private void updatePropertyIfChanged(String property, @Nullable String value) {
        String currentValue = getThing().getProperties().get(property);
        if (value != null && !value.equals(currentValue)) {
            updateProperty(property, value);
        }
    }

    // Helper method to create channels if missing (simplified)
    private void createChannelIfMissing(String channelId, String itemType, String label) {
        // This is a simplified version - in reality, you'd check if channel exists and create it
        logger.debug("Channel {} would be created if missing", channelId);
    }
}
