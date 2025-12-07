package org.openhab.binding.rachio.internal.handler;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.dto.RachioAlert;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast;
import org.openhab.binding.rachio.internal.api.dto.RachioSchedule;
import org.openhab.binding.rachio.internal.api.dto.RachioUsage;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDeviceHandler} is responsible for handling commands for a Rachio device.
 *
 * @author Michael Lobstein - Initial contribution
 * @author Your Name - Enhanced for OpenHAB 5.x
 */
@NonNullByDefault
public class RachioDeviceHandler extends RachioHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    // Configuration
    private @Nullable RachioDeviceConfiguration config;

    // Data cache
    private @Nullable RachioDevice device;
    private @Nullable RachioForecast forecast;
    private @Nullable RachioUsage usage;
    private final Map<String, RachioZone> zones = new ConcurrentHashMap<>();
    private final List<RachioAlert> alerts = new ArrayList<>();
    private final List<RachioSchedule> schedules = new ArrayList<>();

    // Scheduling
    private @Nullable ScheduledFuture<?> refreshJob;
    private static final int REFRESH_INTERVAL = 300; // seconds

    // Professional feature flags
    private boolean hasProfessionalFeatures = false;

    /**
     * Constructor
     *
     * @param thing the thing
     */
    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler for thing: {}", getThing().getUID());

        // Load configuration
        config = getConfigAs(RachioDeviceConfiguration.class);
        if (config == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Configuration is null");
            return;
        }

        // Validate device ID
        if (config.deviceId == null || config.deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID not configured");
            return;
        }

        // Initialize common handler functionality
        initializeCommon();

        // Check if bridge has professional features enabled
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null && bridgeHandler.getBridgeConfiguration() != null) {
            hasProfessionalFeatures = bridgeHandler.getBridgeConfiguration().hasProfessionalFeatures();
        }

        // Start refresh job
        startRefreshJob();

        // Initial status
        updateStatus(ThingStatus.UNKNOWN);

        // Register with bridge as listener
        if (bridgeHandler != null) {
            bridgeHandler.registerListener(new RachioStatusListener() {
                @Override
                public void onRefreshRequested() {
                    refreshAllChannels();
                }

                @Override
                public void updateDeviceStatus(ThingStatus status) {
                    updateStatus(status);
                }

                @Override
                public void updateZoneStatus(String zoneId, ThingStatus status) {
                    // Not needed for device handler
                }

                @Override
                public void onThingStateChanged(RachioDevice updatedDevice, RachioZone zone) {
                    if (updatedDevice != null && updatedDevice.id.equals(config.deviceId)) {
                        device = updatedDevice;
                        updateChannels();
                    }
                }

                @Override
                public void onDeviceDataUpdated(RachioDevice updatedDevice) {
                    if (updatedDevice != null && updatedDevice.id.equals(config.deviceId)) {
                        device = updatedDevice;
                        updateChannels();
                    }
                }

                @Override
                public void onZoneDataUpdated(RachioZone zone) {
                    // Not needed for device handler
                }

                @Override
                public void onError(String errorMessage, String detail) {
                    logger.error("Error from bridge: {} - {}", errorMessage, detail);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
                }
            });
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio device handler for thing: {}", getThing().getUID());

        // Stop refresh job
        stopRefreshJob();

        // Unregister from bridge
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            // Note: In a real implementation, you'd need to keep track of the listener instance
        }

        // Clear data
        device = null;
        forecast = null;
        usage = null;
        zones.clear();
        alerts.clear();
        schedules.clear();

        super.dispose();
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

    @Override
    protected void refreshChannel(ChannelUID channelUID) {
        String channelId = channelUID.getId();
        
        if (device == null) {
            updateState(channelUID, UnDefType.UNDEF);
            return;
        }

        try {
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_DEVICE_STATUS:
                    updateState(channelUID, new StringType(device.status != null ? device.status : "UNKNOWN"));
                    break;
                case RachioBindingConstants.CHANNEL_DEVICE_ONLINE:
                    updateState(channelUID, device.online ? OnOffType.ON : OnOffType.OFF);
                    break;
                case RachioBindingConstants.CHANNEL_DEVICE_PAUSED:
                    updateState(channelUID, device.paused ? OnOffType.ON : OnOffType.OFF);
                    break;
                case RachioBindingConstants.CHANNEL_DEVICE_ZONES:
                    updateState(channelUID, new DecimalType(zones.size()));
                    break;
                case RachioBindingConstants.CHANNEL_DEVICE_LAST_HEARD:
                    if (device.lastHeardFromDate != null) {
                        try {
                            Instant instant = Instant.parse(device.lastHeardFromDate);
                            updateState(channelUID, new DateTimeType(ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())));
                        } catch (Exception e) {
                            logger.debug("Error parsing last heard date: {}", device.lastHeardFromDate);
                            updateState(channelUID, UnDefType.UNDEF);
                        }
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_RAIN_DELAY:
                    if (device.rainDelay != null) {
                        updateState(channelUID, new QuantityType<>(device.rainDelay, Units.HOUR));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_TEMPERATURE:
                    if (forecast != null && forecast.temperature != null) {
                        updateState(channelUID, new QuantityType<>(forecast.temperature, Units.FAHRENHEIT));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_PRECIPITATION:
                    if (forecast != null && forecast.precipitation != null) {
                        updateState(channelUID, new QuantityType<>(forecast.precipitation, Units.INCH));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_EVAPOTRANSPIRATION:
                    if (forecast != null && forecast.evapotranspiration != null) {
                        updateState(channelUID, new QuantityType<>(forecast.evapotranspiration, Units.INCH));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_WATER_USAGE:
                    if (usage != null && usage.totalUsage != null) {
                        updateState(channelUID, new QuantityType<>(usage.totalUsage, Units.CUBIC_METRE));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_WATER_SAVINGS:
                    if (usage != null && usage.totalSavings != null) {
                        updateState(channelUID, new QuantityType<>(usage.totalSavings, Units.CUBIC_METRE));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                default:
                    logger.debug("Unhandled refresh for channel: {}", channelId);
                    updateState(channelUID, UnDefType.UNDEF);
            }
        } catch (Exception e) {
            logger.error("Error refreshing channel {}: {}", channelId, e.getMessage(), e);
            updateState(channelUID, UnDefType.UNDEF);
        }
    }

    @Override
    protected void handleChannelCommand(ChannelUID channelUID, Command command) {
        String channelId = channelUID.getId();
        
        try {
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_DEVICE_PAUSE:
                    if (command instanceof OnOffType) {
                        boolean pause = command == OnOffType.ON;
                        pauseDevice(pause);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_RAIN_DELAY_SET:
                    if (command instanceof QuantityType) {
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int hours = quantity.intValue();
                        setRainDelay(hours);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_STOP_WATERING:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        stopWatering();
                    }
                    break;
                default:
                    logger.debug("Unhandled command {} for channel {}", command, channelId);
            }
        } catch (Exception e) {
            logger.error("Error handling command {} for channel {}: {}", command, channelId, e.getMessage(), e);
        }
    }

    @Override
    public void refreshAllChannels() {
        logger.debug("Refreshing all channels for device: {}", getThing().getUID());
        
        // Refresh device data
        refreshDeviceData();
        
        // Update all channels
        getThing().getChannels().forEach(channel -> {
            refreshChannel(channel.getUID());
        });
    }

    /**
     * Start the refresh job
     */
    private void startRefreshJob() {
        stopRefreshJob(); // Ensure any existing job is stopped
        
        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshDeviceData();
            } catch (Exception e) {
                logger.error("Error refreshing device data: {}", e.getMessage(), e);
            }
        }, 10, REFRESH_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Stop the refresh job
     */
    private void stopRefreshJob() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
    }

    /**
     * Refresh device data from API
     */
    private void refreshDeviceData() {
        if (getRachioHttp() == null || config == null) {
            logger.debug("RachioHttp or configuration not initialized, skipping refresh");
            return;
        }

        try {
            logger.debug("Refreshing data for device: {}", config.deviceId);
            
            // Get device details
            RachioDevice updatedDevice = getRachioHttp().getDevice(config.deviceId);
            if (updatedDevice != null) {
                device = updatedDevice;
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.warn("Device {} not found", config.deviceId);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Device not found");
                return;
            }

            // Get zones for this device
            List<RachioZone> zoneList = getRachioHttp().getZoneList(config.deviceId);
            if (zoneList != null) {
                zones.clear();
                for (RachioZone zone : zoneList) {
                    zones.put(zone.id, zone);
                }
                logger.debug("Retrieved {} zones for device {}", zones.size(), config.deviceId);
            }

            // Load professional features if enabled
            if (hasProfessionalFeatures) {
                loadProfessionalFeatures();
            }

            // Update channels
            updateChannels();

        } catch (IOException e) {
            logger.error("Error refreshing device data: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error refreshing device data: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, e.getMessage());
        }
    }

    /**
     * Load professional features data
     */
    private void loadProfessionalFeatures() {
        if (getRachioHttp() == null || config == null) {
            return;
        }

        try {
            // Get forecast
            forecast = getRachioHttp().getForecast(config.deviceId);
            
            // Get usage data
            usage = getRachioHttp().getUsage(config.deviceId);
            
            // Get alerts
            alerts.clear();
            List<RachioAlert> alertList = getRachioHttp().getAlerts(config.deviceId);
            if (alertList != null) {
                alerts.addAll(alertList);
            }
            
            // Get schedules
            schedules.clear();
            List<RachioSchedule> scheduleList = getRachioHttp().getSchedules(config.deviceId);
            if (scheduleList != null) {
                schedules.addAll(scheduleList);
            }
            
            logger.debug("Loaded professional features for device {}", config.deviceId);
            
        } catch (Exception e) {
            logger.debug("Error loading professional features: {}", e.getMessage());
            // Don't fail the whole refresh if professional features fail
        }
    }

    /**
     * Update all channels with current data
     */
    private void updateChannels() {
        if (device == null) {
            return;
        }

        // Update device channels
        refreshAllChannels();
        
        // Create dynamic channels for professional features if needed
        createDynamicChannels();
    }

    /**
     * Create dynamic channels for professional features
     */
    private void createDynamicChannels() {
        if (!hasProfessionalFeatures || config == null) {
            return;
        }

        List<Channel> channelsToAdd = new ArrayList<>();
        
        // Add forecast channels
        if (forecast != null) {
            channelsToAdd.add(createForecastChannel(RachioBindingConstants.CHANNEL_TEMPERATURE, "Temperature", "Number:Temperature"));
            channelsToAdd.add(createForecastChannel(RachioBindingConstants.CHANNEL_PRECIPITATION, "Precipitation", "Number:Length"));
            channelsToAdd.add(createForecastChannel(RachioBindingConstants.CHANNEL_EVAPOTRANSPIRATION, "Evapotranspiration", "Number:Length"));
        }
        
        // Add usage channels
        if (usage != null) {
            channelsToAdd.add(createUsageChannel(RachioBindingConstants.CHANNEL_WATER_USAGE, "Water Usage", "Number:Volume"));
            channelsToAdd.add(createUsageChannel(RachioBindingConstants.CHANNEL_WATER_SAVINGS, "Water Savings", "Number:Volume"));
        }
        
        // Add zone status channels
        for (RachioZone zone : zones.values()) {
            if (zone.zoneRunStatus != null && zone.zoneRunStatus.equals(ZoneRunStatus.STARTED)) {
                String channelId = RachioBindingConstants.CHANNEL_ZONE_RUNNING_PREFIX + zone.zoneNumber;
                channelsToAdd.add(createZoneRunningChannel(channelId, "Zone " + zone.zoneNumber + " Running"));
            }
        }
        
        // Update thing with new channels if needed
        if (!channelsToAdd.isEmpty()) {
            ThingBuilder thingBuilder = editThing();
            boolean changed = false;
            
            for (Channel channel : channelsToAdd) {
                if (getThing().getChannel(channel.getUID()) == null) {
                    thingBuilder.withChannel(channel);
                    changed = true;
                    logger.debug("Added dynamic channel: {}", channel.getUID());
                }
            }
            
            if (changed) {
                updateThing(thingBuilder.build());
            }
        }
    }

    /**
     * Create a forecast channel
     */
    private Channel createForecastChannel(String channelId, String label, String itemType) {
        return ChannelBuilder.create(new ChannelUID(getThing().getUID(), channelId), itemType)
                .withType(new ChannelTypeUID(getThing().getThingTypeUID().getBindingId(), channelId))
                .withLabel(label)
                .withKind(ChannelKind.STATE)
                .build();
    }

    /**
     * Create a usage channel
     */
    private Channel createUsageChannel(String channelId, String label, String itemType) {
        return ChannelBuilder.create(new ChannelUID(getThing().getUID(), channelId), itemType)
                .withType(new ChannelTypeUID(getThing().getThingTypeUID().getBindingId(), channelId))
                .withLabel(label)
                .withKind(ChannelKind.STATE)
                .build();
    }

    /**
     * Create a zone running channel
     */
    private Channel createZoneRunningChannel(String channelId, String label) {
        return ChannelBuilder.create(new ChannelUID(getThing().getUID(), channelId), "Switch")
                .withType(new ChannelTypeUID(getThing().getThingTypeUID().getBindingId(), "zone-running"))
                .withLabel(label)
                .withKind(ChannelKind.STATE)
                .build();
    }

    /**
     * Get the bridge handler
     */
    private @Nullable RachioBridgeHandler getBridgeHandler() {
        if (getBridge() != null) {
            return (RachioBridgeHandler) getBridge().getHandler();
        }
        return null;
    }

    /**
     * Pause or unpause the device
     */
    private void pauseDevice(boolean pause) throws IOException {
        if (getRachioHttp() != null && config != null) {
            getRachioHttp().pauseDevice(config.deviceId, pause);
            refreshDeviceData(); // Refresh to get updated state
        }
    }

    /**
     * Set rain delay
     */
    private void setRainDelay(int hours) throws IOException {
        if (getRachioHttp() != null && config != null) {
            getRachioHttp().setRainDelay(config.deviceId, hours);
            refreshDeviceData(); // Refresh to get updated state
        }
    }

    /**
     * Stop all watering
     */
    private void stopWatering() throws IOException {
        if (getRachioHttp() != null && config != null) {
            getRachioHttp().stopWatering(config.deviceId);
            refreshDeviceData(); // Refresh to get updated state
        }
    }

    /**
     * Process a webhook event for this device
     */
    public void processWebhookEvent(RachioWebhookEvent event) {
        if (event == null || config == null || !config.deviceId.equals(event.deviceId)) {
            return;
        }

        logger.debug("Processing webhook event for device: {}", config.deviceId);
        
        try {
            // Update device data based on event
            refreshDeviceData();
            
        } catch (Exception e) {
            logger.error("Error processing webhook event: {}", e.getMessage(), e);
        }
    }

    // Getters
    
    public @Nullable RachioDevice getDevice() {
        return device;
    }

    public @Nullable RachioForecast getForecast() {
        return forecast;
    }

    public @Nullable RachioUsage getUsage() {
        return usage;
    }

    public Map<String, RachioZone> getZones() {
        return zones;
    }

    public List<RachioAlert> getAlerts() {
        return alerts;
    }

    public List<RachioSchedule> getSchedules() {
        return schedules;
    }

    public boolean hasProfessionalFeatures() {
        return hasProfessionalFeatures;
    }
}
