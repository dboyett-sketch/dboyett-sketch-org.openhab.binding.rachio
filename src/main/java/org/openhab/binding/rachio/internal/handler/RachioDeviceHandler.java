package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link RachioDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceHandler extends RachioHandler {

    private RachioDeviceConfiguration config = new RachioDeviceConfiguration();
    private @Nullable RachioDevice deviceData;
    
    // Rate limiting tracking inherited from parent

    public RachioDeviceHandler(Thing thing) {
        super(thing);
        logger.debug("RachioDeviceHandler created for thing: {}", thing.getUID());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {}", command, channelUID);
        
        if (command instanceof RefreshType) {
            handleRefreshCommand(channelUID);
            return;
        }
        
        // Handle specific commands
        String channelId = channelUID.getIdWithoutGroup();
        
        try {
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY:
                    if (command instanceof OnOffType) {
                        handleRainDelayCommand((OnOffType) command);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_DEVICE_SCHEDULE_MODE:
                    if (command instanceof StringType) {
                        handleScheduleModeCommand(command.toString());
                    }
                    break;
                default:
                    logger.warn("Unsupported command {} for channel {}", command, channelUID);
            }
        } catch (Exception e) {
            logger.error("Error handling command {} for channel {}: {}", command, channelUID, e.getMessage(), e);
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler for thing: {}", getThing().getUID());
        
        config = getConfigAs(RachioDeviceConfiguration.class);
        
        if (config.deviceId == null || config.deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                "Device ID is required");
            return;
        }
        
        logger.debug("Device configuration: deviceId={}", config.deviceId);
        
        bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, 
                "No bridge handler found");
            return;
        }
        
        // Register with bridge
        bridgeHandler.registerStatusListener(this);
        
        // Get HTTP client from bridge
        rachioHttp = bridgeHandler.getRachioHttp();
        if (rachioHttp == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                "No HTTP client available");
            return;
        }
        
        // Create dynamic channels if needed
        createDynamicChannels();
        
        // Schedule initial refresh
        scheduleRefresh(2);
        
        updateStatus(ThingStatus.ONLINE);
        logger.info("Rachio device handler initialized for device: {}", config.deviceId);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio device handler");
        
        // Unregister from bridge
        if (bridgeHandler != null) {
            bridgeHandler.unregisterStatusListener(this);
        }
        
        // Stop refresh job
        cancelRefreshJob();
        
        super.dispose();
    }

    @Override
    public void refresh() {
        logger.debug("Refreshing device data for deviceId: {}", config.deviceId);
        
        try {
            if (rachioHttp == null) {
                logger.error("No HTTP client available for refresh");
                return;
            }
            
            // Fetch device data from API
            deviceData = rachioHttp.getDevice(config.deviceId);
            
            if (deviceData != null) {
                updateDeviceChannels(deviceData);
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.warn("No device data returned for deviceId: {}", config.deviceId);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }
            
            // Update rate limiting info
            updateRateLimitChannels();
            
        } catch (Exception e) {
            logger.error("Error refreshing device data: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                "Refresh error: " + e.getMessage());
        }
    }

    @Override
    public void onWebhookEvent(String eventType, String deviceId, @Nullable String zoneId, 
                               @Nullable Map<String, Object> data) {
        if (!config.deviceId.equals(deviceId)) {
            return; // Event not for this device
        }
        
        logger.debug("Received webhook event for device {}: type={}, zoneId={}", 
            deviceId, eventType, zoneId);
        
        // Handle different event types
        switch (eventType) {
            case RachioBindingConstants.EVENT_DEVICE_STATUS:
                handleDeviceStatusEvent(data);
                break;
            case RachioBindingConstants.EVENT_RAIN_DELAY:
                handleRainDelayEvent(data);
                break;
            case RachioBindingConstants.EVENT_SCHEDULE_STATUS:
                handleScheduleStatusEvent(data);
                break;
            default:
                logger.debug("Unhandled event type for device: {}", eventType);
        }
        
        // Trigger refresh to get updated data
        scheduleRefresh(1);
    }

    private void createDynamicChannels() {
        logger.debug("Creating dynamic channels for device");
        
        // Note: In OpenHAB 5.x, channels are typically created programmatically
        // but your thing-types.xml should define them. This method is for
        // any additional dynamic channels not in the XML.
        
        // Example: Create rate limit monitoring channels if not already present
        createChannelIfMissing(RachioBindingConstants.CHANNEL_RATE_LIMIT_REMAINING, 
            "Number", "rateLimit");
        createChannelIfMissing(RachioBindingConstants.CHANNEL_RATE_LIMIT_PERCENT, 
            "Number", "rateLimit");
        createChannelIfMissing(RachioBindingConstants.CHANNEL_RATE_LIMIT_STATUS, 
            "String", "rateLimit");
        createChannelIfMissing(RachioBindingConstants.CHANNEL_RATE_LIMIT_RESET, 
            "DateTime", "rateLimit");
    }

    private void updateDeviceChannels(RachioDevice device) {
        logger.debug("Updating channels for device: {}", device.name);
        
        // Update basic device info
        updatePropertyIfChanged(RachioBindingConstants.PROPERTY_NAME, device.name);
        updatePropertyIfChanged(RachioBindingConstants.PROPERTY_MODEL, device.model);
        
        // Update device status channels
        if (device.status != null) {
            updateState(RachioBindingConstants.CHANNEL_DEVICE_STATUS, 
                new StringType(device.status));
            
            updateState(RachioBindingConstants.CHANNEL_DEVICE_ONLINE, 
                OnOffType.from(RachioBindingConstants.DEVICE_STATUS_ONLINE.equals(device.status)));
        }
        
        // Update rain delay
        boolean rainDelayActive = device.rainDelay != null && device.rainDelay > 0;
        updateState(RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY, 
            OnOffType.from(rainDelayActive));
        
        if (rainDelayActive && device.rainDelayExpiration != null) {
            ZonedDateTime endTime = ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(device.rainDelayExpiration), 
                ZoneId.systemDefault()
            );
            updateState(RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY_END_TIME, 
                new DateTimeType(endTime));
        }
        
        // Update schedule mode if available
        if (device.scheduleMode != null) {
            updateState(RachioBindingConstants.CHANNEL_DEVICE_SCHEDULE_MODE, 
                new StringType(device.scheduleMode));
        }
        
        // Update professional monitoring fields if available
        if (device.waterBudget != null) {
            updateState("waterBudget", new DecimalType(device.waterBudget));
        }
        
        if (device.totalWaterUsage != null) {
            updateState("totalWaterUsage", new DecimalType(device.totalWaterUsage));
        }
        
        if (device.waterSavings != null) {
            updateState("waterSavings", new DecimalType(device.waterSavings));
        }
    }

    private void handleRainDelayCommand(OnOffType command) {
        try {
            if (rachioHttp == null) {
                logger.error("No HTTP client available for rain delay command");
                return;
            }
            
            if (command == OnOffType.ON) {
                // Start rain delay (default 24 hours)
                rachioHttp.startRainDelay(config.deviceId, 24 * 60);
                logger.info("Started rain delay for device: {}", config.deviceId);
            } else {
                // Stop rain delay
                rachioHttp.stopRainDelay(config.deviceId);
                logger.info("Stopped rain delay for device: {}", config.deviceId);
            }
            
            // Refresh after command
            scheduleRefresh(2);
            
        } catch (Exception e) {
            logger.error("Error handling rain delay command: {}", e.getMessage(), e);
        }
    }

    private void handleScheduleModeCommand(String mode) {
        logger.debug("Setting schedule mode to: {}", mode);
        // TODO: Implement schedule mode change via API
        // For now, just update the channel state
        updateState(RachioBindingConstants.CHANNEL_DEVICE_SCHEDULE_MODE, new StringType(mode));
    }

    private void handleDeviceStatusEvent(@Nullable Map<String, Object> data) {
        if (data == null) return;
        
        Object status = data.get("status");
        if (status instanceof String) {
            updateState(RachioBindingConstants.CHANNEL_DEVICE_STATUS, new StringType((String) status));
            updateState(RachioBindingConstants.CHANNEL_DEVICE_ONLINE, 
                OnOffType.from(RachioBindingConstants.DEVICE_STATUS_ONLINE.equals(status)));
            logger.debug("Updated device status from webhook: {}", status);
        }
    }

    private void handleRainDelayEvent(@Nullable Map<String, Object> data) {
        if (data == null) return;
        
        Object rainDelay = data.get("rainDelay");
        if (rainDelay instanceof Number) {
            boolean active = ((Number) rainDelay).intValue() > 0;
            updateState(RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY, OnOffType.from(active));
            logger.debug("Updated rain delay from webhook: {}", active);
        }
    }

    private void handleScheduleStatusEvent(@Nullable Map<String, Object> data) {
        if (data == null) return;
        
        Object scheduleMode = data.get("scheduleMode");
        if (scheduleMode instanceof String) {
            updateState(RachioBindingConstants.CHANNEL_DEVICE_SCHEDULE_MODE, 
                new StringType((String) scheduleMode));
            logger.debug("Updated schedule mode from webhook: {}", scheduleMode);
        }
    }
}
