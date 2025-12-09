package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands for Rachio zones
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends RachioHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);
    
    private @Nullable RachioZone zone;
    private @Nullable RachioZoneConfiguration config;

    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void initializeThing() throws Exception {
        logger.debug("Initializing Rachio zone handler for thing: {}", getThing().getUID());
        
        config = getConfigAs(RachioZoneConfiguration.class);
        if (config == null || config.zoneId == null || config.zoneId.isBlank()) {
            throw new Exception("Zone ID not configured");
        }
        
        if (config.deviceId == null || config.deviceId.isBlank()) {
            throw new Exception("Device ID not configured");
        }
        
        // Get zone from bridge via device
        RachioBridgeHandler localBridgeHandler = getBridgeHandler();
        if (localBridgeHandler == null) {
            throw new Exception("Bridge handler not available");
        }
        
        // Get device and find zone
        org.openhab.binding.rachio.internal.api.dto.RachioDevice device = 
            localBridgeHandler.getDevice(config.deviceId);
        if (device == null) {
            throw new Exception("Device not found with ID: " + config.deviceId);
        }
        
        if (device.zones != null) {
            for (RachioZone z : device.zones) {
                if (z != null && z.id != null && z.id.equals(config.zoneId)) {
                    zone = z;
                    break;
                }
            }
        }
        
        if (zone == null) {
            throw new Exception("Zone not found with ID: " + config.zoneId + " on device: " + config.deviceId);
        }
        
        // Create dynamic channels for zone professional data
        createZoneChannels();
        
        logger.debug("Initialized Rachio zone: {} - {} (Device: {})", zone.id, zone.name, config.deviceId);
    }

    @Override
    protected void handleWebhookEvent(RachioWebHookEvent event) {
        if (event == null || zone == null || config == null || 
            !config.zoneId.equals(event.zoneId) || !config.deviceId.equals(event.deviceId)) {
            return;
        }
        
        logger.debug("Processing webhook event for zone {}: {}", config.zoneId, event.type);
        
        switch (event.type) {
            case "ZONE_STATUS_EVENT":
                updateZoneStatus(event);
                break;
            case "WATER_BUDGET_EVENT":
                updateWaterBudget(event);
                break;
            default:
                logger.debug("Unhandled webhook event type for zone: {}", event.type);
        }
    }

    @Override
    protected void refreshAllChannels() {
        logger.debug("Refreshing all channels for zone: {}", getThing().getUID());
        
        try {
            refreshZoneStatus();
        } catch (Exception e) {
            logger.warn("Error refreshing zone channels: {}", e.getMessage(), e);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshChannel(channelUID.getId());
            return;
        }
        
        String channelId = channelUID.getId();
        RachioHttp localHttp = getRachioHttp();
        
        if (localHttp == null || config == null || config.deviceId == null || config.zoneId == null) {
            logger.warn("Cannot handle command: HTTP client or IDs not available");
            return;
        }
        
        try {
            switch (channelId) {
                case CHANNEL_ZONE_START:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        localHttp.startZone(config.deviceId, config.zoneId, duration);
                        logger.debug("Started zone {} for {} seconds", config.zoneId, duration);
                    }
                    break;
                    
                case CHANNEL_ZONE_STOP:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        localHttp.stopZone(config.deviceId, config.zoneId);
                        logger.debug("Stopped zone {}", config.zoneId);
                    }
                    break;
                    
                case CHANNEL_ZONE_ENABLED:
                    if (command instanceof OnOffType) {
                        boolean enabled = command == OnOffType.ON;
                        localHttp.setZoneEnabled(config.deviceId, config.zoneId, enabled);
                        logger.debug("Set zone {} enabled to {}", config.zoneId, enabled);
                        
                        // Update the state immediately - OnOffType implements both Command and State
                        updateState(channelUID, (OnOffType) command);
                    }
                    break;
                    
                case CHANNEL_ZONE_RUNTIME:
                    if (command instanceof DecimalType) {
                        int runtime = ((DecimalType) command).intValue();
                        // This would typically update the zone runtime configuration
                        // Note: Rachio API might not support direct runtime setting via PUT
                        logger.debug("Set zone {} runtime to {} seconds", config.zoneId, runtime);
                    }
                    break;
                    
                default:
                    logger.debug("Unhandled command for channel: {}", channelId);
            }
        } catch (IOException e) {
            logger.warn("Failed to execute command {} on channel {}: {}", command, channelId, e.getMessage());
        }
    }
    
    /**
     * Refresh a specific channel
     */
    private void refreshChannel(String channelId) {
        try {
            switch (channelId) {
                case CHANNEL_ZONE_NAME:
                case CHANNEL_ZONE_NUMBER:
                case CHANNEL_ZONE_STATUS:
                case CHANNEL_ZONE_ENABLED:
                case CHANNEL_ZONE_RUNTIME:
                case CHANNEL_ZONE_AREA:
                case CHANNEL_ZONE_SOIL_TYPE:
                case CHANNEL_ZONE_CROP_TYPE:
                case CHANNEL_ZONE_NOZZLE_TYPE:
                case CHANNEL_ZONE_SLOPE_TYPE:
                case CHANNEL_ZONE_SHADE_TYPE:
                case CHANNEL_ZONE_ROOT_DEPTH:
                case CHANNEL_ZONE_EFFICIENCY:
                case CHANNEL_ZONE_AVAILABLE_WATER:
                    refreshZoneStatus();
                    break;
                default:
                    if (channelId.startsWith(CHANNEL_ADJUSTMENT_LEVEL_PREFIX)) {
                        refreshZoneStatus();
                    } else {
                        logger.debug("No specific refresh for channel: {}", channelId);
                    }
            }
        } catch (Exception e) {
            logger.warn("Error refreshing channel {}: {}", channelId, e.getMessage());
        }
    }
    
    /**
     * Create dynamic channels for zone professional data
     */
    private void createZoneChannels() {
        if (zone == null) {
            return;
        }
        
        List<Channel> channels = new ArrayList<>(getThing().getChannels());
        
        // Add basic zone channels if not already present
        if (!hasChannel(channels, CHANNEL_ZONE_NAME)) {
            channels.add(createChannel(CHANNEL_ZONE_NAME, "String", "Zone Name", 
                "Name of the zone", null));
        }
        
        if (!hasChannel(channels, CHANNEL_ZONE_NUMBER)) {
            channels.add(createChannel(CHANNEL_ZONE_NUMBER, "Number", "Zone Number", 
                "Zone number on the controller", null));
        }
        
        if (!hasChannel(channels, CHANNEL_ZONE_STATUS)) {
            channels.add(createChannel(CHANNEL_ZONE_STATUS, "String", "Zone Status", 
                "Current status of the zone", null));
        }
        
        // Add professional data channels
        if (zone.area != null) {
            channels.add(createChannel(CHANNEL_ZONE_AREA, "Number:Area", "Zone Area", 
                "Area of the zone in square feet", null));
        }
        
        if (zone.soil != null) {
            channels.add(createChannel(CHANNEL_ZONE_SOIL_TYPE, "String", "Soil Type", 
                "Type of soil in the zone", null));
        }
        
        if (zone.crop != null) {
            channels.add(createChannel(CHANNEL_ZONE_CROP_TYPE, "String", "Crop Type", 
                "Type of crop/plant in the zone", null));
        }
        
        if (zone.nozzle != null) {
            channels.add(createChannel(CHANNEL_ZONE_NOZZLE_TYPE, "String", "Nozzle Type", 
                "Type of sprinkler nozzle", null));
        }
        
        if (zone.slope != null) {
            channels.add(createChannel(CHANNEL_ZONE_SLOPE_TYPE, "String", "Slope Type", 
                "Slope of the zone", null));
        }
        
        if (zone.shade != null) {
            channels.add(createChannel(CHANNEL_ZONE_SHADE_TYPE, "String", "Shade Type", 
                "Amount of shade in the zone", null));
        }
        
        if (zone.rootZoneDepth != null) {
            channels.add(createChannel(CHANNEL_ZONE_ROOT_DEPTH, "Number:Length", "Root Depth", 
                "Root zone depth in inches", null));
        }
        
        if (zone.efficiency != null) {
            channels.add(createChannel(CHANNEL_ZONE_EFFICIENCY, "Number:Dimensionless", "Efficiency", 
                "Irrigation efficiency percentage", null));
        }
        
        if (zone.availableWater != null) {
            channels.add(createChannel(CHANNEL_ZONE_AVAILABLE_WATER, "Number:Dimensionless", "Available Water", 
                "Available water in the soil", null));
        }
        
        // Add watering adjustment level channels
        if (zone.wateringAdjustmentRuntimes != null && !zone.wateringAdjustmentRuntimes.isEmpty()) {
            for (int i = 0; i < Math.min(zone.wateringAdjustmentRuntimes.size(), 5); i++) {
                String channelId = CHANNEL_ADJUSTMENT_LEVEL_PREFIX + (i + 1);
                if (!hasChannel(channels, channelId)) {
                    channels.add(createChannel(channelId, "Number", "Adjustment Level " + (i + 1), 
                        "Watering adjustment runtime for level " + (i + 1), null));
                }
            }
        }
        
        // Update thing with new channels
        updateThing(editThing().withChannels(channels).build());
    }
    
    /**
     * Check if a channel already exists
     */
    private boolean hasChannel(List<Channel> channels, String channelId) {
        for (Channel channel : channels) {
            if (channel.getUID().getId().equals(channelId)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Refresh zone status and data
     */
    private void refreshZoneStatus() throws IOException {
        RachioBridgeHandler localBridgeHandler = getBridgeHandler();
        
        if (localBridgeHandler == null || config == null) {
            return;
        }
        
        // Get updated zone info from bridge via device
        org.openhab.binding.rachio.internal.api.dto.RachioDevice device = 
            localBridgeHandler.getDevice(config.deviceId);
        if (device == null || device.zones == null) {
            return;
        }
        
        // Find our zone in the updated device
        RachioZone updatedZone = null;
        for (RachioZone z : device.zones) {
            if (z != null && z.id != null && z.id.equals(config.zoneId)) {
                updatedZone = z;
                break;
            }
        }
        
        if (updatedZone != null) {
            zone = updatedZone;
            
            // Update basic zone channels
            updateState(CHANNEL_ZONE_NAME, zone.name != null ? zone.name : "Unknown");
            
            if (zone.zoneNumber != null) {
                updateState(CHANNEL_ZONE_NUMBER, zone.zoneNumber);
            }
            
            if (zone.status != null) {
                updateState(CHANNEL_ZONE_STATUS, zone.status);
            }
            
            if (zone.enabled != null) {
                updateState(CHANNEL_ZONE_ENABLED, zone.enabled);
            }
            
            if (zone.runtime != null) {
                updateState(CHANNEL_ZONE_RUNTIME, zone.runtime);
            }
            
            // Update professional data channels
            if (zone.area != null) {
                // Use string unit instead of Units.SQUARE_METRE
                updateState(CHANNEL_ZONE_AREA, new QuantityType<>(zone.area + " m²"));
            }
            
            if (zone.soil != null && zone.soil.name != null) {
                updateState(CHANNEL_ZONE_SOIL_TYPE, zone.soil.name);
            }
            
            if (zone.crop != null && zone.crop.name != null) {
                updateState(CHANNEL_ZONE_CROP_TYPE, zone.crop.name);
            }
            
            if (zone.nozzle != null && zone.nozzle.name != null) {
                updateState(CHANNEL_ZONE_NOZZLE_TYPE, zone.nozzle.name);
            }
            
            if (zone.slope != null && zone.slope.name != null) {
                updateState(CHANNEL_ZONE_SLOPE_TYPE, zone.slope.name);
            }
            
            if (zone.shade != null && zone.shade.name != null) {
                updateState(CHANNEL_ZONE_SHADE_TYPE, zone.shade.name);
            }
            
            if (zone.rootZoneDepth != null) {
                // Use string unit instead of Units.INCH
                updateState(CHANNEL_ZONE_ROOT_DEPTH, new QuantityType<>(zone.rootZoneDepth + " in"));
            }
            
            if (zone.efficiency != null) {
                // Use string unit for percent
                updateState(CHANNEL_ZONE_EFFICIENCY, new QuantityType<>(zone.efficiency + " %"));
            }
            
            if (zone.availableWater != null) {
                updateState(CHANNEL_ZONE_AVAILABLE_WATER, zone.availableWater);
            }
            
            // Update watering adjustment channels
            if (zone.wateringAdjustmentRuntimes != null && !zone.wateringAdjustmentRuntimes.isEmpty()) {
                for (int i = 0; i < Math.min(zone.wateringAdjustmentRuntimes.size(), 5); i++) {
                    String channelId = CHANNEL_ADJUSTMENT_LEVEL_PREFIX + (i + 1);
                    updateState(channelId, zone.wateringAdjustmentRuntimes.get(i));
                }
            }
        }
    }
    
    /**
     * Update zone status from webhook event
     */
    private void updateZoneStatus(RachioWebHookEvent event) {
        if (event.subType != null) {
            updateState(CHANNEL_ZONE_STATUS, event.subType);
        }
        
        if (event.zoneRunStatus != null) {
            updateState(CHANNEL_ZONE_STATUS, event.zoneRunStatus);
        }
        
        if (event.duration != null) {
            updateState(CHANNEL_ZONE_RUNTIME, event.duration);
        }
    }
    
    /**
     * Update water budget from webhook event
     */
    private void updateWaterBudget(RachioWebHookEvent event) {
        logger.debug("Water budget event received for zone {}: {}", config != null ? config.zoneId : "unknown", 
            event.summary != null ? event.summary : "No summary");
        
        if (event.waterBudget != null) {
            // Could update a water budget channel if we had one
        }
    }
}
