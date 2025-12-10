package org.openhab.binding.rachio.internal.handler;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.CustomCrop;
import org.openhab.binding.rachio.internal.api.dto.CustomNozzle;
import org.openhab.binding.rachio.internal.api.dto.CustomSoil;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler implements RachioStatusListener {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);

    private @Nullable RachioZoneConfiguration config;
    private @Nullable RachioBridgeHandler bridgeHandler;

    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler");
        
        this.config = getConfigAs(RachioZoneConfiguration.class);
        this.bridgeHandler = getBridgeHandler();
        
        if (this.config == null || this.config.zoneId == null || this.config.zoneId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone ID not configured");
            return;
        }
        
        if (this.bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not found");
            return;
        }
        
        // Register with bridge
        this.bridgeHandler.registerStatusListener(this);
        
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            handler.unregisterStatusListener(this);
        }
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {}", command, channelUID.getId());
        
        if (command instanceof RefreshType) {
            refreshZone();
            return;
        }
        
        // Handle zone-specific commands
        switch (channelUID.getId()) {
            case CHANNEL_ZONE_START:
                if (command instanceof OnOffType && command == OnOffType.ON) {
                    startZone();
                }
                break;
            case CHANNEL_ZONE_STOP:
                if (command instanceof OnOffType && command == OnOffType.ON) {
                    stopZone();
                }
                break;
            case CHANNEL_ZONE_RUNTIME:
                if (command instanceof DecimalType) {
                    int minutes = ((DecimalType) command).intValue();
                    setZoneRuntime(minutes);
                }
                break;
        }
    }

    @Override
    public void onZoneUpdated(@Nullable RachioZone zone) {
        if (zone == null) {
            return;
        }
        
        String zoneId = config != null ? config.zoneId : null;
        if (zoneId == null || !zoneId.equals(zone.getId())) {
            return; // Not our zone
        }
        
        logger.debug("Updating zone: {}", zone.getName());
        
        // Update zone name
        if (zone.getName() != null) {
            updateState(CHANNEL_ZONE_NAME, new StringType(zone.getName()));
            updateProperty(Thing.PROPERTY_NAME, zone.getName());
        }
        
        // Update enabled status
        updateState(CHANNEL_ZONE_ENABLED, zone.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        
        // Update runtime
        updateState(CHANNEL_ZONE_RUNTIME, new DecimalType(zone.getRuntime()));
        
        // Update last watered date
        if (zone.getLastWateredDate() != null) {
            updateState(CHANNEL_ZONE_LAST_WATERED, 
                new DateTimeType(java.time.Instant.ofEpochMilli(zone.getLastWateredDate())));
        }
        
        // Update zone number
        updateState(CHANNEL_ZONE_NUMBER, new DecimalType(zone.getZoneNumber()));
        updateProperty(PROPERTY_ZONE_NUMBER, String.valueOf(zone.getZoneNumber()));
        
        // Update soil data
        CustomSoil soil = zone.getSoil();
        if (soil != null) {
            updateState(CHANNEL_ZONE_SOIL_TYPE, new StringType(soil.getType() != null ? soil.getType() : ""));
            if (soil.getAvailableWater() != null) {
                updateState(CHANNEL_ZONE_SOIL_WATER, new DecimalType(soil.getAvailableWater()));
            }
        }
        
        // Update crop data
        CustomCrop crop = zone.getCrop();
        if (crop != null) {
            updateState(CHANNEL_ZONE_CROP_TYPE, new StringType(crop.getType() != null ? crop.getType() : ""));
            if (crop.getCoefficient() != null) {
                updateState(CHANNEL_ZONE_CROP_COEFFICIENT, new DecimalType(crop.getCoefficient()));
            }
        }
        
        // Update nozzle data
        CustomNozzle nozzle = zone.getNozzle();
        if (nozzle != null) {
            updateState(CHANNEL_ZONE_NOZZLE_TYPE, new StringType(nozzle.getType() != null ? nozzle.getType() : ""));
            if (nozzle.getRate() != null) {
                updateState(CHANNEL_ZONE_NOZZLE_RATE, new DecimalType(nozzle.getRate()));
            }
        }
        
        // Update slope
        if (zone.getSlope() != null) {
            updateState(CHANNEL_ZONE_SLOPE_TYPE, new StringType(zone.getSlope()));
        }
        
        // Update shade
        if (zone.getShade() != null) {
            updateState(CHANNEL_ZONE_SHADE_TYPE, new StringType(zone.getShade()));
        }
        
        // Update other professional properties would go here
        // CHANNEL_ZONE_EFFICIENCY, CHANNEL_ZONE_WATER_ADJUSTMENT_PREFIX, etc.
    }

    @Override
    public void onWebhookEvent(String deviceId, String eventType, @Nullable String subType, @Nullable Map<String, Object> eventData) {
        // Check if this webhook is for our zone
        // This would need to parse eventData to see if it contains our zoneId
        logger.debug("Received webhook event {} for device {}", eventType, deviceId);
        
        // Trigger refresh on webhook events
        scheduler.submit(() -> {
            try {
                refreshZone();
            } catch (Exception e) {
                logger.debug("Failed to refresh after webhook", e);
            }
        });
    }

    private void refreshZone() {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            // Request bridge to refresh zone data
            handler.pollDevices();
        }
    }

    private void startZone() {
        logger.debug("Starting zone");
        RachioZoneConfiguration cfg = config;
        if (cfg != null) {
            int duration = cfg.defaultDuration > 0 ? cfg.defaultDuration : 10; // Default 10 minutes
            // Implementation would call Rachio API via bridge
            logger.debug("Starting zone {} for {} minutes", cfg.zoneId, duration);
        }
    }

    private void stopZone() {
        logger.debug("Stopping zone");
        // Implementation would call Rachio API via bridge
    }

    private void setZoneRuntime(int minutes) {
        logger.debug("Setting zone runtime to {} minutes", minutes);
        RachioZoneConfiguration cfg = config;
        if (cfg != null) {
            cfg.defaultDuration = minutes;
            // Would need to persist configuration
        }
    }

    private @Nullable RachioBridgeHandler getBridgeHandler() {
        return getBridge() != null && getBridge().getHandler() instanceof RachioBridgeHandler 
            ? (RachioBridgeHandler) getBridge().getHandler() 
            : null;
    }
}
