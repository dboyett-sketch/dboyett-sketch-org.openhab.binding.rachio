package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
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
 * The {@link RachioZoneHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends RachioHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);

    private @Nullable RachioZoneConfiguration config;
    private @Nullable String deviceId;
    private @Nullable String zoneId;
    private @Nullable ScheduledFuture<?> refreshJob;

    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler for {}", getThing().getUID());
        
        this.config = getConfigAs(RachioZoneConfiguration.class);
        this.deviceId = config != null ? config.deviceId : null;
        this.zoneId = config != null ? config.zoneId : null;
        
        if (deviceId == null || deviceId.isEmpty() || zoneId == null || zoneId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                "Device ID or Zone ID not configured");
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
                String id = channelUID.getId();
                switch (id) {
                    case CHANNEL_ZONE_ENABLED:
                        if (command instanceof OnOffType) {
                            setZoneEnabled(command == OnOffType.ON);
                        }
                        break;
                    case CHANNEL_ZONE_START:
                        if (command instanceof DecimalType) {
                            int duration = ((DecimalType) command).intValue();
                            startZone(duration);
                        } else if (command instanceof OnOffType && command == OnOffType.ON) {
                            // Use default duration
                            startZone(getDefaultDuration());
                        }
                        break;
                    case CHANNEL_ZONE_STOP:
                        if (command instanceof OnOffType && command == OnOffType.ON) {
                            stopZone();
                        }
                        break;
                    case CHANNEL_ZONE_RUNTIME:
                        if (command instanceof DecimalType) {
                            // Update runtime configuration
                            int runtime = ((DecimalType) command).intValue();
                            updateZoneRuntime(runtime);
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
        logger.debug("Polling zone status for {}", getThing().getUID());
        refreshZoneData();
    }

    private void handleRefreshCommand(ChannelUID channelUID) {
        try {
            refreshZoneData();
        } catch (Exception e) {
            logger.warn("Failed to refresh zone data", e);
        }
    }

    private void refreshZoneData() {
        RachioHttp http = rachioHttp;
        String devId = deviceId;
        String zId = zoneId;
        
        if (http != null && devId != null && !devId.isEmpty() && zId != null && !zId.isEmpty()) {
            try {
                RachioZone zone = http.getZone(devId, zId);
                if (zone != null) {
                    updateZoneChannels(zone);
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Zone not found");
                }
            } catch (RachioApiException e) {
                logger.warn("Failed to get zone data: {}", e.getMessage());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            } catch (Exception e) {
                logger.warn("Unexpected error refreshing zone data", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        }
    }

    private void updateZoneChannels(RachioZone zone) {
        // Update zone name
        if (zone.getName != null) {
            updateState(CHANNEL_ZONE_NAME, new StringType(zone.getName));
            updatePropertyIfChanged(Thing.PROPERTY_NAME, zone.getName);
        }
        
        // Update enabled state
        }
        updateState(CHANNEL_ZONE_ENABLED, zone.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        }
        
        // Update runtime
        if (zone.runtime != null) {
            updateState(CHANNEL_ZONE_RUNTIME, new DecimalType(zone.runtime));
        }
        
        // Update last water start time
        if (zone.lastWateredDate != null) {
            long wateredTime = zone.lastWateredDate * 1000; // Convert seconds to milliseconds
            updateState(CHANNEL_ZONE_LAST_WATERED, new DateTimeType(Instant.ofEpochMilli(wateredTime)));
        }
        
        // Update zone number
        if (zone.zoneNumber != null) {
            updateState(CHANNEL_ZONE_NUMBER, new DecimalType(zone.zoneNumber));
        }
        
        // Update professional data if available
        updateProfessionalData(zone);
    }

    private void updateProfessionalData(@Nullable RachioZone zone) {
        if (zone == null) return;
        
        // Soil data
        if (zone.soil != null) {
            updateState(CHANNEL_ZONE_SOIL_TYPE, new StringType(zone.soil.name != null ? zone.soil.name : "UNKNOWN"));
            if (zone.soil.availableWater != null) {
                updateState(CHANNEL_ZONE_SOIL_WATER, new DecimalType(zone.soil.availableWater));
            }
        }
        
        // Crop data
        if (zone.crop != null) {
            updateState(CHANNEL_ZONE_CROP_TYPE, new StringType(zone.crop.name != null ? zone.crop.name : "UNKNOWN"));
            if (zone.crop.coefficient != null) {
                updateState(CHANNEL_ZONE_CROP_COEFFICIENT, new DecimalType(zone.crop.coefficient));
            }
        }
        
        // Nozzle data
        if (zone.nozzle != null) {
            updateState(CHANNEL_ZONE_NOZZLE_TYPE, new StringType(zone.nozzle.name != null ? zone.nozzle.name : "UNKNOWN"));
            if (zone.nozzle.inchesPerHour != null) {
                updateState(CHANNEL_ZONE_NOZZLE_RATE, new DecimalType(zone.nozzle.inchesPerHour));
            }
        }
        
        // Slope data
        if (zone.slope != null && zone.slope.name != null) {
            updateState(CHANNEL_ZONE_SLOPE_TYPE, new StringType(zone.slope.name));
        }
        
        // Shade data
        if (zone.shade != null && zone.shade.name != null) {
            updateState(CHANNEL_ZONE_SHADE_TYPE, new StringType(zone.shade.name));
        }
        
        // Root depth
        if (zone.rootDepth != null) {
            updateState(CHANNEL_ZONE_ROOT_DEPTH, new DecimalType(zone.rootDepth));
        }
        
        // Efficiency
        if (zone.efficiency != null) {
            updateState(CHANNEL_ZONE_EFFICIENCY, new DecimalType(zone.efficiency));
        }
        
        // Area
        if (zone.area != null) {
            updateState(CHANNEL_ZONE_AREA, new DecimalType(zone.area));
        }
        
        // Water adjustments (1-5)
        for (int i = 1; i <= 5; i++) {
            String channelId = CHANNEL_ZONE_WATER_ADJUSTMENT_PREFIX + i;
            // You would need to get adjustment data from zone object
            // This is a placeholder
            // updateState(channelId, new DecimalType(adjustmentValue));
        }
    }

    private void setZoneEnabled(boolean enabled) {
        RachioHttp http = rachioHttp;
        String devId = deviceId;
        String zId = zoneId;
        
        if (http != null && devId != null && !devId.isEmpty() && zId != null && !zId.isEmpty()) {
            try {
                http.setZoneEnabled(devId, zId, enabled);
                logger.info("Set zone {} enabled: {}", zId, enabled);
                scheduleRefresh(3); // Refresh in 3 seconds
            } catch (RachioApiException e) {
                logger.warn("Failed to set zone enabled: {}", e.getMessage());
            }
        }
    }

    private void startZone(int durationSeconds) {
        RachioHttp http = rachioHttp;
        String devId = deviceId;
        String zId = zoneId;
        
        if (http != null && devId != null && !devId.isEmpty() && zId != null && !zId.isEmpty()) {
            try {
                http.startZone(devId, zId, durationSeconds);
                logger.info("Started zone {} for {} seconds", zId, durationSeconds);
                scheduleRefresh(3); // Refresh in 3 seconds
            } catch (RachioApiException e) {
                logger.warn("Failed to start zone: {}", e.getMessage());
            }
        }
    }

    private void stopZone() {
        RachioHttp http = rachioHttp;
        String devId = deviceId;
        String zId = zoneId;
        
        if (http != null && devId != null && !devId.isEmpty() && zId != null && !zId.isEmpty()) {
            try {
                http.stopZone(devId, zId);
                logger.info("Stopped zone {}", zId);
                scheduleRefresh(3); // Refresh in 3 seconds
            } catch (RachioApiException e) {
                logger.warn("Failed to stop zone: {}", e.getMessage());
            }
        }
    }

    private void updateZoneRuntime(int runtimeSeconds) {
        // This would update the zone's configured runtime
        // Implementation depends on Rachio API
        logger.info("Updated zone {} runtime to {} seconds", zoneId, runtimeSeconds);
        scheduleRefresh(3); // Refresh in 3 seconds
    }

    private int getDefaultDuration() {
        // Get default duration from configuration or zone settings
        RachioZoneConfiguration cfg = config;
        if (cfg != null && cfg.defaultDuration > 0) {
            return cfg.defaultDuration;
        }
        return 300; // Default 5 minutes
    }

    // Helper methods for scheduling refreshes
    private void scheduleRefresh(int delaySeconds) {
        cancelRefreshJob();
        
        refreshJob = scheduler.schedule(() -> {
            try {
                refreshZoneData();
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
