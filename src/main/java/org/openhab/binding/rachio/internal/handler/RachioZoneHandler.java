package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for Rachio zones with professional irrigation data
 *
 * @author Damion Boyett - Enhanced with professional irrigation features
 */
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler implements RachioHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);
    
    private static final int REFRESH_INTERVAL = 60; // seconds
    private static final int WATERING_UPDATE_INTERVAL = 5; // seconds when watering
    
    private @Nullable RachioZoneConfiguration config;
    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable RachioZone zone;
    private @Nullable ScheduledFuture<?> refreshJob;
    private @Nullable ScheduledFuture<?> wateringUpdateJob;
    
    private boolean watering = false;
    private @Nullable Instant wateringStartTime;
    private int wateringDuration = 0;
    private int wateringRemaining = 0;
    
    public RachioZoneHandler(Thing thing) {
        super(thing);
    }
    
    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler");
        
        config = getConfigAs(RachioZoneConfiguration.class);
        if (config == null || config.zoneId.isEmpty() || config.deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                        "Zone ID or Device ID not configured");
            return;
        }
        
        bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, 
                        "Bridge not found or offline");
            return;
        }
        
        // Create dynamic channels for professional irrigation data
        createProfessionalChannels();
        
        // Schedule refresh
        scheduleRefresh();
        
        // Initial refresh
        scheduler.schedule(this::refresh, 2, TimeUnit.SECONDS);
        
        updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, "Initializing...");
    }
    
    @Override
    public void dispose() {
        logger.debug("Disposing Rachio zone handler");
        
        stopRefresh();
        stopWateringUpdates();
        
        super.dispose();
    }
    
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshChannel(channelUID);
            return;
        }
        
        String channelId = channelUID.getId();
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        
        if (localBridgeHandler == null) {
            logger.error("Bridge handler not available");
            return;
        }
        
        String zoneId = getZoneId();
        String deviceId = getDeviceId();
        
        if (zoneId == null || deviceId == null) {
            logger.error("Zone ID or Device ID not available");
            return;
        }
        
        try {
            switch (channelId) {
                case CHANNEL_ZONE_START:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        localBridgeHandler.startZone(zoneId, deviceId, duration, "OPENHAB");
                        logger.info("Starting zone {} for {} seconds", zoneId, duration);
                    }
                    break;
                    
                case CHANNEL_ZONE_STOP:
                    if (command == OnOffType.OFF) {
                        localBridgeHandler.stopZone(zoneId, deviceId, "OPENHAB");
                        logger.info("Stopping zone {}", zoneId);
                    }
                    break;
                    
                case CHANNEL_ZONE_ENABLED:
                    if (command instanceof OnOffType) {
                        boolean enabled = command == OnOffType.ON;
                        // Fixed: Use runtime from configuration if available
                        RachioZoneConfiguration localConfig = config;
                        int runtime = (localConfig != null && localConfig.runtime > 0) ? localConfig.runtime : 600;
                        localBridgeHandler.setZoneEnabled(zoneId, deviceId, enabled, "OPENHAB");
                        logger.info("{} zone {}", enabled ? "Enabled" : "Disabled", zoneId);
                        
                        // Update runtime if enabled
                        if (enabled) {
                            updateState(CHANNEL_ZONE_RUNTIME, new DecimalType(runtime));
                        }
                    }
                    break;
                    
                case CHANNEL_ZONE_RUNTIME:
                    if (command instanceof DecimalType) {
                        int runtime = ((DecimalType) command).intValue();
                        // Update configuration
                        RachioZoneConfiguration localConfig = config;
                        if (localConfig != null) {
                            localConfig.runtime = runtime;
                        }
                        logger.info("Set zone {} runtime to {} seconds", zoneId, runtime);
                        
                        // Update zone object if available
                        RachioZone localZone = zone;
                        if (localZone != null) {
                            localZone.setRuntime(runtime);
                        }
                    }
                    break;
                    
                default:
                    logger.debug("Unhandled command {} for channel {}", command, channelId);
            }
            
        } catch (Exception e) {
            logger.error("Error handling command {} for channel {}: {}", command, channelId, e.getMessage(), e);
        }
    }
    
    private void createProfessionalChannels() {
        ThingBuilder thingBuilder = editThing();
        boolean channelsModified = false;
        
        // Professional irrigation channels
        if (getThing().getChannel(CHANNEL_SOIL_TYPE) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_SOIL_TYPE), "String")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_SOIL_TYPE))
                    .withLabel("Soil Type")
                    .withDescription("Type of soil in this zone")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_CROP_TYPE) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_CROP_TYPE), "String")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_CROP_TYPE))
                    .withLabel("Crop Type")
                    .withDescription("Type of crop/plant in this zone")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_NOZZLE_TYPE) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_NOZZLE_TYPE), "String")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_NOZZLE_TYPE))
                    .withLabel("Nozzle Type")
                    .withDescription("Type of sprinkler nozzle")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_SLOPE_TYPE) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_SLOPE_TYPE), "String")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_SLOPE_TYPE))
                    .withLabel("Slope Type")
                    .withDescription("Type of slope in this zone")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_SHADE_TYPE) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_SHADE_TYPE), "String")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_SHADE_TYPE))
                    .withLabel("Shade Type")
                    .withDescription("Type of shade in this zone")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_ROOT_DEPTH) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_ROOT_DEPTH), "Number:Length")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ROOT_DEPTH))
                    .withLabel("Root Depth")
                    .withDescription("Depth of plant roots in inches")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_IRRIGATION_EFFICIENCY) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_IRRIGATION_EFFICIENCY), "Number:Dimensionless")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_IRRIGATION_EFFICIENCY))
                    .withLabel("Irrigation Efficiency")
                    .withDescription("Efficiency of irrigation system (0-100%)")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_AVAILABLE_WATER) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_AVAILABLE_WATER), "Number:Dimensionless")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_AVAILABLE_WATER))
                    .withLabel("Available Water")
                    .withDescription("Available water capacity of soil")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        // Water adjustment level channels
        for (int i = 1; i <= 5; i++) {
            String channelId = CHANNEL_ADJUSTMENT_LEVEL_PREFIX + i;
            if (getThing().getChannel(channelId) == null) {
                Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), channelId), "Number:Time")
                        .withType(new ChannelTypeUID(BINDING_ID, channelId))
                        .withLabel("Adjustment Level " + i)
                        .withDescription("Runtime for adjustment level " + i)
                        .build();
                thingBuilder.withChannel(channel);
                channelsModified = true;
            }
        }
        
        if (getThing().getChannel(CHANNEL_ZONE_AREA) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_ZONE_AREA), "Number:Area")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_AREA))
                    .withLabel("Zone Area")
                    .withDescription("Area of the zone in square feet")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_NOZZLE_RATE) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_NOZZLE_RATE), "Number:Speed")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_NOZZLE_RATE))
                    .withLabel("Nozzle Rate")
                    .withDescription("Application rate of nozzle in mm/hour")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        if (getThing().getChannel(CHANNEL_CROP_COEFFICIENT) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_CROP_COEFFICIENT), "Number:Dimensionless")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_CROP_COEFFICIENT))
                    .withLabel("Crop Coefficient")
                    .withDescription("Water requirement coefficient for crop")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        // Water volume calculation channels
        if (getThing().getChannel(CHANNEL_WATER_VOLUME) == null) {
            Channel channel = ChannelBuilder.create(new ChannelUID(getThing().getUID(), CHANNEL_WATER_VOLUME), "Number:Volume")
                    .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_WATER_VOLUME))
                    .withLabel("Water Volume")
                    .withDescription("Estimated water volume for current runtime")
                    .build();
            thingBuilder.withChannel(channel);
            channelsModified = true;
        }
        
        // Update thing if channels were added
        if (channelsModified) {
            updateThing(thingBuilder.build());
            logger.debug("Added professional irrigation channels to zone {}", getZoneId());
        }
    }
    
    private void scheduleRefresh() {
        if (refreshJob == null || refreshJob.isCancelled()) {
            refreshJob = scheduler.scheduleWithFixedDelay(this::refresh, 
                    REFRESH_INTERVAL, REFRESH_INTERVAL, TimeUnit.SECONDS);
            logger.debug("Scheduled zone refresh every {} seconds", REFRESH_INTERVAL);
        }
    }
    
    private void stopRefresh() {
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
            logger.debug("Stopped zone refresh job");
        }
    }
    
    private void startWateringUpdates() {
        if (wateringUpdateJob == null || wateringUpdateJob.isCancelled()) {
            wateringUpdateJob = scheduler.scheduleWithFixedDelay(this::updateWateringStatus, 
                    0, WATERING_UPDATE_INTERVAL, TimeUnit.SECONDS);
            logger.debug("Started watering status updates");
        }
    }
    
    private void stopWateringUpdates() {
        if (wateringUpdateJob != null && !wateringUpdateJob.isCancelled()) {
            wateringUpdateJob.cancel(true);
            wateringUpdateJob = null;
            logger.debug("Stopped watering status updates");
        }
    }
    
    public void refresh() {
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }
        
        String zoneId = getZoneId();
        String deviceId = getDeviceId();
        
        if (zoneId == null || deviceId == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            return;
        }
        
        try {
            // Get device to find zone
            RachioDevice device = localBridgeHandler.getDevice(deviceId);
            if (device == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                            "Device not found");
                return;
            }
            
            // Find zone in device
            RachioZone updatedZone = device.getZoneById(zoneId);
            if (updatedZone == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                            "Zone not found in device");
                return;
            }
            
            // Update zone object
            zone = updatedZone;
            
            // Update channels
            updateChannels(updatedZone);
            
            // Update watering status
            updateWateringStatusFromZone(updatedZone);
            
            // Update thing status
            updateStatus(ThingStatus.ONLINE);
            
            logger.trace("Zone {} refresh completed", zoneId);
            
        } catch (Exception e) {
            logger.debug("Error refreshing zone {}: {}", zoneId, e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }
    
    private void updateChannels(RachioZone zone) {
        // Basic zone info
        updateState(CHANNEL_ZONE_NAME, new StringType(zone.getName()));
        updateState(CHANNEL_ZONE_NUMBER, new DecimalType(zone.getZoneNumber()));
        updateState(CHANNEL_ZONE_ENABLED, zone.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_ZONE_RUNTIME, new DecimalType(zone.getRuntime()));
        updateState(CHANNEL_ZONE_MAX_RUNTIME, new DecimalType(zone.getMaxRuntime()));
        
        // Zone status
        ZoneRunStatus status = zone.getZoneRunStatus();
        if (status != null) {
            updateState(CHANNEL_ZONE_STATUS, new StringType(status.toString()));
            updateState(CHANNEL_ZONE_WATERING, 
                       "STARTED".equals(status.toString()) ? OnOffType.ON : OnOffType.OFF);
        } else {
            updateState(CHANNEL_ZONE_STATUS, new StringType("UNKNOWN"));
            updateState(CHANNEL_ZONE_WATERING, OnOffType.OFF);
        }
        
        // Professional irrigation data
        updateState(CHANNEL_SOIL_TYPE, new StringType(zone.getSoilType()));
        updateState(CHANNEL_CROP_TYPE, new StringType(zone.getCropType()));
        updateState(CHANNEL_NOZZLE_TYPE, new StringType(zone.getNozzleType()));
        updateState(CHANNEL_SLOPE_TYPE, new StringType(zone.getSlopeType()));
        updateState(CHANNEL_SHADE_TYPE, new StringType(zone.getShadeType()));
        
        // Fixed: Use correct unit constant - MILLIMETER not MILLIMETRE
        updateState(CHANNEL_ROOT_DEPTH, new QuantityType<>(zone.getRootZoneDepth(), SIUnits.MILLIMETER));
        updateState(CHANNEL_IRRIGATION_EFFICIENCY, new PercentType(zone.getEfficiency()));
        updateState(CHANNEL_AVAILABLE_WATER, new DecimalType(zone.getAvailableWater()));
        
        // Water adjustment levels
        int[] adjustmentRuntimes = zone.getWateringAdjustmentRuntimes();
        for (int i = 0; i < Math.min(adjustmentRuntimes.length, 5); i++) {
            String channelId = CHANNEL_ADJUSTMENT_LEVEL_PREFIX + (i + 1);
            // Fixed: Use correct unit constant - MILLIMETER not MILLIMETRE
            updateState(channelId, new QuantityType<>(adjustmentRuntimes[i], SIUnits.MILLIMETER));
        }
        
        // Zone area
        updateState(CHANNEL_ZONE_AREA, new QuantityType<>(zone.getArea(), Units.SQUARE_METRE));
        
        // Nozzle application rate (convert inches/hour to mm/hour)
        double nozzleRateInchesPerHour = zone.getNozzleInchesPerHour();
        double nozzleRateMmPerHour = nozzleRateInchesPerHour * 25.4;
        // Fixed: Use correct unit constant - MILLIMETER_PER_HOUR
        updateState(CHANNEL_NOZZLE_RATE, new QuantityType<>(nozzleRateMmPerHour, SIUnits.MILLIMETRE_PER_HOUR));
        
        // Crop coefficient
        updateState(CHANNEL_CROP_COEFFICIENT, new DecimalType(zone.getCropCoefficient()));
        
        // Water volume calculation
        double waterVolume = zone.getCurrentWaterVolume(); // in gallons
        updateState(CHANNEL_WATER_VOLUME, new QuantityType<>(waterVolume, Units.LITRE));
        
        // Last watered date
        if (zone.getLastWateredDate() != null) {
            try {
                ZonedDateTime lastWatered = ZonedDateTime.parse(zone.getLastWateredDate());
                updateState(CHANNEL_LAST_WATERED, new DateTimeType(lastWatered));
            } catch (Exception e) {
                logger.debug("Error parsing last watered date: {}", zone.getLastWateredDate());
            }
        }
        
        // Last run duration
        Integer lastRunDuration = zone.getLastRunDuration();
        if (lastRunDuration != null) {
            updateState(CHANNEL_LAST_RUN_DURATION, new QuantityType<>(lastRunDuration, Units.SECOND));
        }
        
        // Total water used
        Double totalWaterUsed = zone.getTotalWaterUsed();
        if (totalWaterUsed != null) {
            // Fixed: Use correct unit constant - MILLIMETER not MILLIMETRE
            updateState(CHANNEL_TOTAL_WATER_USED, new QuantityType<>(totalWaterUsed, SIUnits.MILLIMETER));
        }
    }
    
    private void updateWateringStatusFromZone(RachioZone zone) {
        ZoneRunStatus status = zone.getZoneRunStatus();
        boolean wasWatering = watering;
        watering = status != null && "STARTED".equals(status.toString());
        
        if (watering && !wasWatering) {
            // Watering just started
            wateringStartTime = Instant.now();
            wateringDuration = zone.getRuntime();
            wateringRemaining = wateringDuration;
            startWateringUpdates();
            logger.info("Zone {} started watering for {} seconds", getZoneId(), wateringDuration);
            
        } else if (!watering && wasWatering) {
            // Watering just stopped
            stopWateringUpdates();
            wateringStartTime = null;
            wateringDuration = 0;
            wateringRemaining = 0;
            logger.info("Zone {} stopped watering", getZoneId());
            
        } else if (watering) {
            // Update remaining time
            if (wateringStartTime != null) {
                long elapsedSeconds = Duration.between(wateringStartTime, Instant.now()).getSeconds();
                wateringRemaining = Math.max(0, wateringDuration - (int) elapsedSeconds);
            }
        }
        
        // Update remaining time channel
        updateState(CHANNEL_ZONE_REMAINING, new QuantityType<>(wateringRemaining, Units.SECOND));
    }
    
    private void updateWateringStatus() {
        if (watering && wateringStartTime != null && wateringRemaining > 0) {
            long elapsedSeconds = Duration.between(wateringStartTime, Instant.now()).getSeconds();
            wateringRemaining = Math.max(0, wateringDuration - (int) elapsedSeconds);
            
            updateState(CHANNEL_ZONE_REMAINING, new QuantityType<>(wateringRemaining, Units.SECOND));
            
            // Update percent complete
            if (wateringDuration > 0) {
                int percentComplete = (int) ((wateringDuration - wateringRemaining) * 100 / wateringDuration);
                updateState(CHANNEL_ZONE_PERCENT, new PercentType(percentComplete));
            }
            
            // Check if watering completed
            if (wateringRemaining <= 0) {
                watering = false;
                stopWateringUpdates();
                updateState(CHANNEL_ZONE_WATERING, OnOffType.OFF);
                updateState(CHANNEL_ZONE_STATUS, new StringType("COMPLETED"));
                logger.info("Zone {} watering completed", getZoneId());
            }
        }
    }
    
    private void refreshChannel(ChannelUID channelUID) {
        String channelId = channelUID.getId();
        RachioZone localZone = zone;
        
        if (localZone == null) {
            return;
        }
        
        // Refresh individual channel based on zone data
        switch (channelId) {
            case CHANNEL_ZONE_STATUS:
                ZoneRunStatus status = localZone.getZoneRunStatus();
                if (status != null) {
                    updateState(channelUID, new StringType(status.toString()));
                }
                break;
            case CHANNEL_ZONE_ENABLED:
                updateState(channelUID, localZone.isEnabled() ? OnOffType.ON : OnOffType.OFF);
                break;
            case CHANNEL_ZONE_RUNTIME:
                updateState(channelUID, new DecimalType(localZone.getRuntime()));
                break;
            case CHANNEL_SOIL_TYPE:
                updateState(channelUID, new StringType(localZone.getSoilType()));
                break;
            case CHANNEL_CROP_TYPE:
                updateState(channelUID, new StringType(localZone.getCropType()));
                break;
            case CHANNEL_NOZZLE_TYPE:
                updateState(channelUID, new StringType(localZone.getNozzleType()));
                break;
            default:
                logger.debug("Unhandled refresh for channel: {}", channelId);
        }
    }
    
    // Event handlers
    
    public void onZoneStarted(int duration) {
        watering = true;
        wateringStartTime = Instant.now();
        wateringDuration = duration;
        wateringRemaining = duration;
        
        updateState(CHANNEL_ZONE_WATERING, OnOffType.ON);
        updateState(CHANNEL_ZONE_STATUS, new StringType("STARTED"));
        updateState(CHANNEL_ZONE_REMAINING, new QuantityType<>(duration, Units.SECOND));
        
        startWateringUpdates();
        logger.info("Zone {} started watering for {} seconds", getZoneId(), duration);
    }
    
    public void onZoneStopped() {
        watering = false;
        wateringStartTime = null;
        wateringDuration = 0;
        wateringRemaining = 0;
        
        updateState(CHANNEL_ZONE_WATERING, OnOffType.OFF);
        updateState(CHANNEL_ZONE_STATUS, new StringType("STOPPED"));
        updateState(CHANNEL_ZONE_REMAINING, new QuantityType<>(0, Units.SECOND));
        updateState(CHANNEL_ZONE_PERCENT, new PercentType(0));
        
        stopWateringUpdates();
        logger.info("Zone {} stopped watering", getZoneId());
    }
    
    public void onZoneEnabledChanged(boolean enabled) {
        updateState(CHANNEL_ZONE_ENABLED, enabled ? OnOffType.ON : OnOffType.OFF);
        
        RachioZone localZone = zone;
        if (localZone != null) {
            localZone.setEnabled(enabled);
        }
        
        logger.info("Zone {} {}", getZoneId(), enabled ? "enabled" : "disabled");
    }
    
    // Utility methods
    
    private @Nullable RachioBridgeHandler getBridgeHandler() {
        return getBridge() != null ? (RachioBridgeHandler) getBridge().getHandler() : null;
    }
    
    public @Nullable String getZoneId() {
        RachioZoneConfiguration localConfig = config;
        return localConfig != null ? localConfig.zoneId : null;
    }
    
    public @Nullable String getDeviceId() {
        RachioZoneConfiguration localConfig = config;
        return localConfig != null ? localConfig.deviceId : null;
    }
    
    public @Nullable RachioZone getZone() {
        return zone;
    }
    
    public boolean isWatering() {
        return watering;
    }
    
    public int getWateringRemaining() {
        return wateringRemaining;
    }
    
    public int getWateringDuration() {
        return wateringDuration;
    }
    
    @Override
    public void handleWebhookEvent(org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent event) {
        // Process webhook events for this zone
        if (event != null && getZoneId() != null) {
            String eventType = event.getType();
            logger.debug("Zone {} received webhook event: {}", getZoneId(), eventType);
            
            // Refresh zone data when webhook event is received
            scheduler.schedule(this::refresh, 1, TimeUnit.SECONDS);
        }
    }
    
    @Override
    public void handleImageCall(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp) {
        // Image handling would be implemented here
        logger.debug("Image call received for zone {}", getZoneId());
    }
}
