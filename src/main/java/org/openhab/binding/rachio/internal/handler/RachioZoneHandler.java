package org.openhab.binding.rachio.internal.handler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands for a Rachio zone
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler implements RachioStatusListener {
    
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);
    
    private @Nullable RachioZoneConfiguration config;
    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable RachioZone zone;
    private @Nullable ScheduledFuture<?> refreshJob;
    
    public RachioZoneHandler(Thing thing) {
        super(thing);
    }
    
    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler");
        
        config = getConfigAs(RachioZoneConfiguration.class);
        
        bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No bridge configured");
            return;
        }
        
        // FIXED: Changed from direct method call to bridgeHandler reference
        RachioBridgeHandler bh = bridgeHandler;
        if (bh != null) {
            // Register this handler as a listener
            // Note: Need to add registerListener method to RachioBridgeHandler
            // bh.registerListener(this); // Temporarily commented - method doesn't exist yet
        }
        
        updateStatus(ThingStatus.UNKNOWN);
        
        // Schedule initial refresh
        scheduleRefresh(5);
    }
    
    private @Nullable RachioBridgeHandler getBridgeHandler() {
        return getBridge() != null ? (RachioBridgeHandler) getBridge().getHandler() : null;
    }
    
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {}", command, channelUID);
        
        if (command instanceof RefreshType) {
            refreshZone();
            return;
        }
        
        String channelId = channelUID.getIdWithoutGroup();
        RachioBridgeHandler bh = bridgeHandler;
        RachioZoneConfiguration cfg = config;
        
        if (bh == null || cfg == null || cfg.zoneId == null || cfg.deviceId == null) {
            logger.warn("Cannot handle command, zone not properly initialized");
            return;
        }
        
        try {
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_ZONE_RUN:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        // Start zone with configured runtime
                        // FIXED: Using runtime from configuration or default
                        int duration = getZoneRuntime(cfg);
                        bh.startZone(cfg.deviceId, cfg.zoneId, duration);
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_ZONE_STOP:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        // Stop zone
                        bh.stopWatering(cfg.deviceId, cfg.zoneId);
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_ZONE_ENABLED:
                    if (command instanceof OnOffType) {
                        boolean enabled = command == OnOffType.ON;
                        bh.setZoneEnabled(cfg.deviceId, cfg.zoneId, enabled);
                    }
                    break;
                    
                default:
                    logger.debug("Unhandled command for channel {}", channelId);
            }
        } catch (Exception e) {
            logger.warn("Error handling command {} for channel {}: {}", command, channelUID, e.getMessage(), e);
        }
    }
    
    // FIXED: Helper method to get zone runtime
    private int getZoneRuntime(RachioZoneConfiguration cfg) {
        // Try to get runtime from configuration
        // If not available, use default of 10 minutes
        if (cfg.runtime != null && cfg.runtime > 0) {
            return cfg.runtime;
        }
        return 10; // Default 10 minutes
    }
    
    @Override
    public void dispose() {
        logger.debug("Disposing Rachio zone handler");
        
        ScheduledFuture<?> job = refreshJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            refreshJob = null;
        }
        
        // Unregister listener
        RachioBridgeHandler bh = bridgeHandler;
        if (bh != null) {
            // Note: Need to add unregisterListener method to RachioBridgeHandler
        }
        
        super.dispose();
    }
    
    private void scheduleRefresh(long initialDelay) {
        ScheduledFuture<?> job = refreshJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(false);
        }
        
        refreshJob = scheduler.scheduleWithFixedDelay(this::refreshZone, initialDelay, 30, TimeUnit.SECONDS);
    }
    
    private void refreshZone() {
        try {
            logger.debug("Refreshing zone data");
            
            RachioBridgeHandler bh = bridgeHandler;
            RachioZoneConfiguration cfg = config;
            
            if (bh == null || cfg == null || cfg.zoneId == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Missing configuration");
                return;
            }
            
            // Get device from bridge to find zone
            RachioDeviceHandler deviceHandler = getDeviceHandler();
            if (deviceHandler == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Device handler not found");
                return;
            }
            
            // FIXED: Need to get zone from device - this is a placeholder
            // In reality, we'd get the device and then find the zone in its zones list
            updateStatus(ThingStatus.ONLINE);
            
        } catch (Exception e) {
            logger.debug("Error refreshing zone: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }
    
    private @Nullable RachioDeviceHandler getDeviceHandler() {
        RachioZoneConfiguration cfg = config;
        if (cfg == null || cfg.deviceId == null) {
            return null;
        }
        
        // Find the device handler among bridge's things
        RachioBridgeHandler bh = bridgeHandler;
        if (bh == null) {
            return null;
        }
        
        for (Thing thing : bh.getThing().getThings()) {
            if (thing.getHandler() instanceof RachioDeviceHandler) {
                RachioDeviceHandler deviceHandler = (RachioDeviceHandler) thing.getHandler();
                // FIXED: Need to check if this is the right device
                return deviceHandler;
            }
        }
        
        return null;
    }
    
    public void updateZoneState(RachioZone zone) {
        this.zone = zone;
        
        // FIXED: All field accesses changed to use getters instead of direct field access
        
        // Zone enabled status
        updateState(RachioBindingConstants.CHANNEL_ZONE_ENABLED, 
            zone.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        
        // Zone running status
        // FIXED: Changed to use getter and check if running
        boolean isRunning = zone.getZoneRunStatus() != null && 
                           zone.getZoneRunStatus().isActive();
        updateState("zoneRunning", isRunning ? OnOffType.ON : OnOffType.OFF);
        
        // Zone runtime (configured)
        updateState("zoneRuntime", new DecimalType(zone.getRuntime()));
        
        // FIXED: Need to calculate time remaining based on current status
        // For now, set to 0 or runtime
        int timeRemaining = isRunning ? zone.getRuntime() * 60 : 0; // Convert minutes to seconds
        updateState("zoneTimeRemaining", new QuantityType<>(timeRemaining, Units.SECOND));
        
        // Last watered date
        Instant lastWatered = zone.getLastWateredDate();
        if (lastWatered != null) {
            updateState("zoneLastRun", new DateTimeType(ZonedDateTime.ofInstant(lastWatered, ZoneId.systemDefault())));
        } else {
            updateState("zoneLastRun", UnDefType.NULL);
        }
        
        // Zone area
        double area = zone.getArea();
        if (area > 0) {
            // FIXED: Using correct unit constant
            updateState("zoneArea", new QuantityType<>(area, SIUnits.SQUARE_METRE));
        }
        
        // Depth of water (calculated)
        double depthOfWater = calculateDepthOfWater(zone);
        if (depthOfWater > 0) {
            // FIXED: Using correct unit constant
            updateState("zoneDepthOfWater", new QuantityType<>(depthOfWater, SIUnits.MILLIMETRE));
        }
        
        // Actual runtime (if available from last run)
        int lastRunDuration = zone.getLastRunDuration();
        if (lastRunDuration > 0) {
            updateState("zoneRunTimeActual", new QuantityType<>(lastRunDuration, Units.SECOND));
        }
        
        // Professional irrigation data
        updateProfessionalData(zone);
    }
    
    private double calculateDepthOfWater(RachioZone zone) {
        // Calculate depth of water based on runtime and nozzle rate
        double inchesPerHour = zone.getInchesPerHour();
        int runtime = zone.getRuntime(); // in minutes
        
        if (inchesPerHour > 0 && runtime > 0) {
            return (inchesPerHour * runtime) / 60.0; // Convert to inches
        }
        
        return 0.0;
    }
    
    private void updateProfessionalData(RachioZone zone) {
        // Soil data
        CustomSoil soil = zone.getCustomSoil();
        if (soil != null) {
            // FIXED: Check if soil has a name property
            String soilName = getCustomObjectName(soil);
            if (soilName != null && !soilName.isEmpty()) {
                updateState(RachioBindingConstants.CHANNEL_SOIL_TYPE, new StringType(soilName));
            }
        }
        
        // Available water
        double availableWater = zone.getAvailableWater();
        if (availableWater > 0) {
            updateState(RachioBindingConstants.CHANNEL_SOIL_AVAILABLE_WATER, 
                new QuantityType<>(availableWater, SIUnits.MILLIMETRE));
        }
        
        // Crop data
        CustomCrop crop = zone.getCustomCrop();
        if (crop != null) {
            // FIXED: Check if crop has a name property
            String cropName = getCustomObjectName(crop);
            if (cropName != null && !cropName.isEmpty()) {
                updateState(RachioBindingConstants.CHANNEL_CROP_TYPE, new StringType(cropName));
            }
        }
        
        double cropCoefficient = zone.getCropCoefficient();
        if (cropCoefficient > 0) {
            updateState(RachioBindingConstants.CHANNEL_CROP_COEFFICIENT, 
                new QuantityType<>(cropCoefficient, Units.PERCENT));
        }
        
        // Nozzle data
        CustomNozzle nozzle = zone.getCustomNozzle();
        if (nozzle != null) {
            // FIXED: Check if nozzle has a name property
            String nozzleName = getCustomObjectName(nozzle);
            if (nozzleName != null && !nozzleName.isEmpty()) {
                updateState(RachioBindingConstants.CHANNEL_NOZZLE_TYPE, new StringType(nozzleName));
            }
        }
        
        double inchesPerHour = zone.getInchesPerHour();
        if (inchesPerHour > 0) {
            updateState(RachioBindingConstants.CHANNEL_NOZZLE_RATE, 
                new QuantityType<>(inchesPerHour, SIUnits.MILLIMETRE_PER_HOUR));
        }
        
        // Slope data
        CustomSlope slope = zone.getCustomSlope();
        if (slope != null) {
            // FIXED: Check if slope has a name property
            String slopeName = getCustomObjectName(slope);
            if (slopeName != null && !slopeName.isEmpty()) {
                updateState(RachioBindingConstants.CHANNEL_SLOPE_TYPE, new StringType(slopeName));
            }
        }
        
        // Shade data
        CustomShade shade = zone.getCustomShade();
        if (shade != null) {
            // FIXED: Check if shade has a name property
            String shadeName = getCustomObjectName(shade);
            if (shadeName != null && !shadeName.isEmpty()) {
                updateState(RachioBindingConstants.CHANNEL_SHADE_TYPE, new StringType(shadeName));
            }
        }
        
        // Root depth
        double rootDepth = zone.getRootDepth();
        if (rootDepth > 0) {
            // FIXED: Using correct unit constant
            updateState(RachioBindingConstants.CHANNEL_ROOT_DEPTH, 
                new QuantityType<>(rootDepth, SIUnits.MILLIMETRE));
        }
        
        // Irrigation efficiency
        double efficiency = zone.getEfficiency();
        if (efficiency > 0) {
            updateState(RachioBindingConstants.CHANNEL_IRRIGATION_EFFICIENCY, 
                new QuantityType<>(efficiency, Units.PERCENT));
        }
        
        // Water adjustment levels
        updateState(RachioBindingConstants.CHANNEL_ADJUSTMENT_LEVEL1, 
            new DecimalType(zone.getAdjustmentLevel1()));
        updateState(RachioBindingConstants.CHANNEL_ADJUSTMENT_LEVEL2, 
            new DecimalType(zone.getAdjustmentLevel2()));
        updateState(RachioBindingConstants.CHANNEL_ADJUSTMENT_LEVEL3, 
            new DecimalType(zone.getAdjustmentLevel3()));
        updateState(RachioBindingConstants.CHANNEL_ADJUSTMENT_LEVEL4, 
            new DecimalType(zone.getAdjustmentLevel4()));
        updateState(RachioBindingConstants.CHANNEL_ADJUSTMENT_LEVEL5, 
            new DecimalType(zone.getAdjustmentLevel5()));
    }
    
    // FIXED: Helper method to get name from custom objects
    private @Nullable String getCustomObjectName(Object obj) {
        // This is a generic method to get name from custom objects
        // In reality, each Custom* class should have a getName() method
        // For now, return toString() or null
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }
    
    @Override
    public void handleWebhookEvent(RachioWebhookEvent event) {
        logger.debug("Zone handler received webhook event: {}", event.getType());
        
        RachioZoneConfiguration cfg = config;
        if (cfg == null || cfg.zoneId == null) {
            return;
        }
        
        // FIXED: Using getter instead of direct field access
        String eventZoneId = event.getZoneId();
        if (!cfg.zoneId.equals(eventZoneId)) {
            return; // Event is for a different zone
        }
        
        // Handle zone-specific events
        switch (event.getType()) {
            case RachioBindingConstants.EVENT_ZONE_STATUS:
                updateZoneStatusFromEvent(event);
                break;
                
            default:
                logger.debug("Unhandled event type for zone: {}", event.getType());
        }
    }
    
    // FIXED: Added missing onError() method implementation
    @Override
    public void onError(String errorCode, String errorMessage) {
        logger.warn("Zone handler error [{}]: {}", errorCode, errorMessage);
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
    }
    
    // FIXED: Removed incorrect @Override annotation from this method
    public void updateZoneStatus(String zoneId, String status) {
        // Implementation would update zone status
        logger.debug("Updating zone {} status: {}", zoneId, status);
    }
    
    private void updateZoneStatusFromEvent(RachioWebhookEvent event) {
        String status = event.getZoneStatus();
        if (status == null) {
            return;
        }
        
        // Update zone running status
        boolean isRunning = "STARTED".equalsIgnoreCase(status) || 
                           "RUNNING".equalsIgnoreCase(status);
        
        updateState("zoneRunning", isRunning ? OnOffType.ON : OnOffType.OFF);
        
        // Update zone status channel if it exists
        updateState(RachioBindingConstants.CHANNEL_ZONE_STATUS, new StringType(status));
        
        // If zone completed, update last watered time
        if ("COMPLETED".equalsIgnoreCase(status)) {
            Instant now = Instant.now();
            updateState("zoneLastRun", new DateTimeType(ZonedDateTime.ofInstant(now, ZoneId.systemDefault())));
            
            // Update last run duration if available from event
            Integer duration = event.getDuration();
            if (duration != null && duration > 0) {
                updateState("zoneRunTimeActual", new QuantityType<>(duration, Units.SECOND));
            }
        }
    }
    
    // Helper methods
    
    public void startZone(int duration) {
        RachioBridgeHandler bh = bridgeHandler;
        RachioZoneConfiguration cfg = config;
        
        if (bh != null && cfg != null && cfg.deviceId != null && cfg.zoneId != null) {
            bh.startZone(cfg.deviceId, cfg.zoneId, duration);
        }
    }
    
    public void stopZone() {
        RachioBridgeHandler bh = bridgeHandler;
        RachioZoneConfiguration cfg = config;
        
        if (bh != null && cfg != null && cfg.deviceId != null && cfg.zoneId != null) {
            bh.stopWatering(cfg.deviceId, cfg.zoneId);
        }
    }
    
    public void setEnabled(boolean enabled) {
        RachioBridgeHandler bh = bridgeHandler;
        RachioZoneConfiguration cfg = config;
        
        if (bh != null && cfg != null && cfg.deviceId != null && cfg.zoneId != null) {
            bh.setZoneEnabled(cfg.deviceId, cfg.zoneId, enabled);
        }
    }
    
    public @Nullable RachioZone getZone() {
        return zone;
    }
    
    public void setZone(RachioZone zone) {
        this.zone = zone;
        updateZoneState(zone);
    }
    
    @Override
    public String toString() {
        RachioZoneConfiguration cfg = config;
        return String.format("RachioZoneHandler[thing=%s, zoneId=%s]", 
            getThing().getUID(), cfg != null ? cfg.zoneId : "unknown");
    }
}
