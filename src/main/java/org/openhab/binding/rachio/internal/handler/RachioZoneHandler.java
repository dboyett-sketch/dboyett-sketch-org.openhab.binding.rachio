package org.openhab.binding.rachio.internal.handler;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.dto.CustomCrop;
import org.openhab.binding.rachio.internal.api.dto.CustomNozzle;
import org.openhab.binding.rachio.internal.api.dto.CustomShade;
import org.openhab.binding.rachio.internal.api.dto.CustomSlope;
import org.openhab.binding.rachio.internal.api.dto.CustomSoil;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
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
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands for a Rachio zone.
 *
 * @author Michael Lobstein - Initial contribution
 * @author Your Name - Enhanced for OpenHAB 5.x
 */
@NonNullByDefault
public class RachioZoneHandler extends RachioHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);

    // Configuration
    private @Nullable RachioZoneConfiguration config;

    // Data cache
    private @Nullable RachioZone zone;
    private final Map<String, Object> professionalData = new ConcurrentHashMap<>();

    // Scheduling
    private @Nullable ScheduledFuture<?> refreshJob;
    private static final int REFRESH_INTERVAL = 60; // seconds

    // Run status tracking
    private boolean isRunning = false;
    private @Nullable Long runStartTime;
    private int scheduledDuration = 0;

    /**
     * Constructor
     *
     * @param thing the thing
     */
    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler for thing: {}", getThing().getUID());

        // Load configuration
        config = getConfigAs(RachioZoneConfiguration.class);
        if (config == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Configuration is null");
            return;
        }

        // Validate zone ID
        if (config.zoneId == null || config.zoneId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone ID not configured");
            return;
        }

        // Initialize common handler functionality
        initializeCommon();

        // Start refresh job
        startRefreshJob();

        // Initial status
        updateStatus(ThingStatus.UNKNOWN);

        // Register with bridge as listener
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            bridgeHandler.registerListener(new RachioStatusListener() {
                @Override
                public void onRefreshRequested() {
                    refreshAllChannels();
                }

                @Override
                public void updateDeviceStatus(ThingStatus status) {
                    // Not needed for zone handler
                }

                @Override
                public void updateZoneStatus(String zoneId, ThingStatus status) {
                    if (config != null && config.zoneId.equals(zoneId)) {
                        updateStatus(status);
                    }
                }

                @Override
                public void onThingStateChanged(org.openhab.binding.rachio.internal.api.dto.RachioDevice device, 
                                                org.openhab.binding.rachio.internal.api.dto.RachioZone updatedZone) {
                    if (updatedZone != null && config != null && config.zoneId.equals(updatedZone.id)) {
                        zone = updatedZone;
                        updateZoneState();
                        updateChannels();
                    }
                }

                @Override
                public void onDeviceDataUpdated(org.openhab.binding.rachio.internal.api.dto.RachioDevice device) {
                    // Not needed for zone handler
                }

                @Override
                public void onZoneDataUpdated(org.openhab.binding.rachio.internal.api.dto.RachioZone updatedZone) {
                    if (updatedZone != null && config != null && config.zoneId.equals(updatedZone.id)) {
                        zone = updatedZone;
                        updateZoneState();
                        updateChannels();
                    }
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
        logger.debug("Disposing Rachio zone handler for thing: {}", getThing().getUID());

        // Stop refresh job
        stopRefreshJob();

        // Clear data
        zone = null;
        professionalData.clear();

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
        
        if (zone == null) {
            updateState(channelUID, UnDefType.UNDEF);
            return;
        }

        try {
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_ZONE_ENABLED:
                    updateState(channelUID, zone.enabled ? OnOffType.ON : OnOffType.OFF);
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_RUNNING:
                    updateState(channelUID, isRunning ? OnOffType.ON : OnOffType.OFF);
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_STATUS:
                    String status = getZoneStatus();
                    updateState(channelUID, new StringType(status));
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_DURATION:
                    if (zone.duration != null) {
                        updateState(channelUID, new QuantityType<>(zone.duration, Units.SECOND));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_RUN_TIME:
                    if (runStartTime != null && isRunning) {
                        long elapsedSeconds = (System.currentTimeMillis() - runStartTime) / 1000;
                        updateState(channelUID, new QuantityType<>(elapsedSeconds, Units.SECOND));
                    } else {
                        updateState(channelUID, new QuantityType<>(0, Units.SECOND));
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_TIME_REMAINING:
                    if (runStartTime != null && isRunning && scheduledDuration > 0) {
                        long elapsedSeconds = (System.currentTimeMillis() - runStartTime) / 1000;
                        long remaining = Math.max(0, scheduledDuration - elapsedSeconds);
                        updateState(channelUID, new QuantityType<>(remaining, Units.SECOND));
                    } else {
                        updateState(channelUID, new QuantityType<>(0, Units.SECOND));
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_LAST_RUN:
                    if (zone.lastWateredDate != null) {
                        try {
                            Instant instant = Instant.parse(zone.lastWateredDate);
                            updateState(channelUID, new DateTimeType(ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())));
                        } catch (Exception e) {
                            logger.debug("Error parsing last watered date: {}", zone.lastWateredDate);
                            updateState(channelUID, UnDefType.UNDEF);
                        }
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_AREA:
                    if (zone.area != null) {
                        updateState(channelUID, new QuantityType<>(zone.area, Units.SQUARE_FOOT));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_DEPTH_OF_WATER:
                    if (zone.depthOfWater != null) {
                        updateState(channelUID, new QuantityType<>(zone.depthOfWater, Units.INCH));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_RUN_TIME_ACTUAL:
                    if (zone.runTimeActual != null) {
                        updateState(channelUID, new QuantityType<>(zone.runTimeActual, Units.SECOND));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                // Professional data channels
                case RachioBindingConstants.CHANNEL_ZONE_SOIL_TYPE:
                    if (zone.soil != null) {
                        updateState(channelUID, new StringType(zone.soil.name));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_CROP_TYPE:
                    if (zone.crop != null) {
                        updateState(channelUID, new StringType(zone.crop.name));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_NOZZLE_TYPE:
                    if (zone.nozzle != null) {
                        updateState(channelUID, new StringType(zone.nozzle.name));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_SLOPE_TYPE:
                    if (zone.slope != null) {
                        updateState(channelUID, new StringType(zone.slope.name));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_SHADE_TYPE:
                    if (zone.shade != null) {
                        updateState(channelUID, new StringType(zone.shade.name));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_ROOT_DEPTH:
                    if (zone.rootDepth != null) {
                        updateState(channelUID, new QuantityType<>(zone.rootDepth, Units.INCH));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_EFFICIENCY:
                    if (zone.efficiency != null) {
                        updateState(channelUID, new QuantityType<>(zone.efficiency, Units.PERCENT));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_YARD_AREA_SQFT:
                    if (zone.yardAreaSquareFeet != null) {
                        updateState(channelUID, new QuantityType<>(zone.yardAreaSquareFeet, Units.SQUARE_FOOT));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_AVAILABLE_WATER:
                    if (zone.availableWater != null) {
                        updateState(channelUID, new QuantityType<>(zone.availableWater, Units.INCH));
                    } else {
                        updateState(channelUID, UnDefType.UNDEF);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_APPLICATION_RATE:
                    if (zone.applicationRate != null) {
                        updateState(channelUID, new QuantityType<>(zone.applicationRate, Units.INCH_PER_HOUR));
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
                case RachioBindingConstants.CHANNEL_ZONE_START:
                    if (command instanceof QuantityType) {
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int duration = quantity.intValue();
                        startZone(duration);
                    } else if (command instanceof OnOffType && command == OnOffType.ON) {
                        startZone(zone != null && zone.duration != null ? zone.duration : 300);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_STOP:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        stopZone();
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_ENABLE:
                    if (command instanceof OnOffType) {
                        boolean enable = command == OnOffType.ON;
                        setZoneEnabled(enable);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_DURATION_SET:
                    if (command instanceof QuantityType) {
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int duration = quantity.intValue();
                        setZoneDuration(duration);
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
        logger.debug("Refreshing all channels for zone: {}", getThing().getUID());
        
        // Refresh zone data
        refreshZoneData();
        
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
                refreshZoneData();
            } catch (Exception e) {
                logger.error("Error refreshing zone data: {}", e.getMessage(), e);
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
     * Refresh zone data from API
     */
    private void refreshZoneData() {
        if (getRachioHttp() == null || config == null || deviceId == null) {
            logger.debug("RachioHttp, configuration or deviceId not initialized, skipping refresh");
            return;
        }

        try {
            logger.debug("Refreshing data for zone: {}", config.zoneId);
            
            // Get zone details from device handler
            RachioDeviceHandler deviceHandler = getDeviceHandler();
            if (deviceHandler != null) {
                RachioZone updatedZone = deviceHandler.getZones().get(config.zoneId);
                if (updatedZone != null) {
                    zone = updatedZone;
                    updateZoneState();
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    logger.warn("Zone {} not found in device {}", config.zoneId, deviceId);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone not found");
                    return;
                }
            } else {
                // Fallback: get zone from API directly
                RachioZone updatedZone = getRachioHttp().getZone(deviceId, config.zoneId);
                if (updatedZone != null) {
                    zone = updatedZone;
                    updateZoneState();
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    logger.warn("Zone {} not found", config.zoneId);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Zone not found");
                    return;
                }
            }

            // Update channels
            updateChannels();

        } catch (IOException e) {
            logger.error("Error refreshing zone data: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error refreshing zone data: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, e.getMessage());
        }
    }

    /**
     * Update zone running state based on current data
     */
    private void updateZoneState() {
        if (zone == null) {
            isRunning = false;
            runStartTime = null;
            return;
        }

        // Update running state based on zone status
        if (zone.zoneRunStatus != null) {
            if (zone.zoneRunStatus.equals(ZoneRunStatus.STARTED)) {
                if (!isRunning) {
                    isRunning = true;
                    runStartTime = System.currentTimeMillis();
                    scheduledDuration = zone.duration != null ? zone.duration : 0;
                    logger.debug("Zone {} started running", config != null ? config.zoneId : "unknown");
                }
            } else {
                if (isRunning) {
                    isRunning = false;
                    runStartTime = null;
                    scheduledDuration = 0;
                    logger.debug("Zone {} stopped running", config != null ? config.zoneId : "unknown");
                }
            }
        }
    }

    /**
     * Get the zone status as a string
     */
    private String getZoneStatus() {
        if (zone == null) {
            return "UNKNOWN";
        }
        
        if (zone.zoneRunStatus != null) {
            return zone.zoneRunStatus.toString();
        }
        
        return zone.enabled ? "IDLE" : "DISABLED";
    }

    /**
     * Update all channels with current data
     */
    private void updateChannels() {
        if (zone == null) {
            return;
        }

        // Update all channels
        refreshAllChannels();
        
        // Create dynamic channels for professional data if needed
        createDynamicChannels();
    }

    /**
     * Create dynamic channels for professional data
     */
    private void createDynamicChannels() {
        if (zone == null || config == null) {
            return;
        }

        // Check if we need to add professional data channels
        boolean needsUpdate = false;
        ThingBuilder thingBuilder = editThing();
        
        // Soil data channels
        if (zone.soil != null && getThing().getChannel(RachioBindingConstants.CHANNEL_ZONE_SOIL_TYPE) == null) {
            thingBuilder.withChannel(createProfessionalChannel(RachioBindingConstants.CHANNEL_ZONE_SOIL_TYPE, "Soil Type", "String"));
            needsUpdate = true;
        }
        
        // Crop data channels
        if (zone.crop != null && getThing().getChannel(RachioBindingConstants.CHANNEL_ZONE_CROP_TYPE) == null) {
            thingBuilder.withChannel(createProfessionalChannel(RachioBindingConstants.CHANNEL_ZONE_CROP_TYPE, "Crop Type", "String"));
            needsUpdate = true;
        }
        
        // Nozzle data channels
        if (zone.nozzle != null && getThing().getChannel(RachioBindingConstants.CHANNEL_ZONE_NOZZLE_TYPE) == null) {
            thingBuilder.withChannel(createProfessionalChannel(RachioBindingConstants.CHANNEL_ZONE_NOZZLE_TYPE, "Nozzle Type", "String"));
            needsUpdate = true;
        }
        
        // Slope data channels
        if (zone.slope != null && getThing().getChannel(RachioBindingConstants.CHANNEL_ZONE_SLOPE_TYPE) == null) {
            thingBuilder.withChannel(createProfessionalChannel(RachioBindingConstants.CHANNEL_ZONE_SLOPE_TYPE, "Slope Type", "String"));
            needsUpdate = true;
        }
        
        // Shade data channels
        if (zone.shade != null && getThing().getChannel(RachioBindingConstants.CHANNEL_ZONE_SHADE_TYPE) == null) {
            thingBuilder.withChannel(createProfessionalChannel(RachioBindingConstants.CHANNEL_ZONE_SHADE_TYPE, "Shade Type", "String"));
            needsUpdate = true;
        }
        
        // Update thing if needed
        if (needsUpdate) {
            updateThing(thingBuilder.build());
            logger.debug("Added professional data channels for zone {}", config.zoneId);
        }
    }

    /**
     * Create a professional data channel
     */
    private Channel createProfessionalChannel(String channelId, String label, String itemType) {
        return ChannelBuilder.create(new ChannelUID(getThing().getUID(), channelId), itemType)
                .withType(new ChannelTypeUID(getThing().getThingTypeUID().getBindingId(), channelId))
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
     * Get the device handler
     */
    private @Nullable RachioDeviceHandler getDeviceHandler() {
        if (getBridge() != null && getBridge().getHandler() instanceof RachioBridgeHandler) {
            RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) getBridge().getHandler();
            // In a real implementation, you'd get the device handler from the bridge
            // For now, return null and fall back to API calls
        }
        return null;
    }

    /**
     * Start the zone
     */
    private void startZone(int duration) throws IOException {
        if (getRachioHttp() != null && config != null && deviceId != null) {
            getRachioHttp().startZone(deviceId, config.zoneId, duration);
            refreshZoneData(); // Refresh to get updated state
        }
    }

    /**
     * Stop the zone
     */
    private void stopZone() throws IOException {
        if (getRachioHttp() != null && config != null && deviceId != null) {
            getRachioHttp().stopWatering(deviceId);
            refreshZoneData(); // Refresh to get updated state
        }
    }

    /**
     * Enable or disable the zone
     */
    private void setZoneEnabled(boolean enabled) throws IOException {
        if (getRachioHttp() != null && config != null && deviceId != null) {
            getRachioHttp().setZoneEnabled(deviceId, config.zoneId, enabled);
            refreshZoneData(); // Refresh to get updated state
        }
    }

    /**
     * Set zone duration
     */
    private void setZoneDuration(int duration) {
        if (zone != null) {
            // This would typically update the zone configuration via API
            // For now, just update the local cache
            zone.duration = duration;
            logger.debug("Set zone {} duration to {} seconds", config != null ? config.zoneId : "unknown", duration);
            refreshAllChannels();
        }
    }

    /**
     * Process a webhook event for this zone
     */
    public void processWebhookEvent(RachioWebhookEvent event) {
        if (event == null || config == null || !config.zoneId.equals(event.zoneId)) {
            return;
        }

        logger.debug("Processing webhook event for zone: {}", config.zoneId);
        
        try {
            // Update zone state based on event
            updateZoneState();
            
            // Refresh zone data
            refreshZoneData();
            
        } catch (Exception e) {
            logger.error("Error processing webhook event: {}", e.getMessage(), e);
        }
    }

    // Getters
    
    public @Nullable RachioZone getZone() {
        return zone;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public @Nullable Long getRunStartTime() {
        return runStartTime;
    }

    public int getScheduledDuration() {
        return scheduledDuration;
    }

    public Map<String, Object> getProfessionalData() {
        return professionalData;
    }
}
