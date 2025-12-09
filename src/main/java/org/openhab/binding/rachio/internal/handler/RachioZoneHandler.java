package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.CustomCrop;
import org.openhab.binding.rachio.internal.api.dto.CustomNozzle;
import org.openhab.binding.rachio.internal.api.dto.CustomShade;
import org.openhab.binding.rachio.internal.api.dto.CustomSlope;
import org.openhab.binding.rachio.internal.api.dto.CustomSoil;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler implements RachioStatusListener {

    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);
    
    private RachioZoneConfiguration config = new RachioZoneConfiguration();
    private @Nullable RachioHttp rachioHttp;
    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable RachioZone zoneData;
    
    // Runtime tracking
    private @Nullable Integer currentRuntime;
    private boolean isRunning = false;

    public RachioZoneHandler(Thing thing) {
        super(thing);
        logger.debug("RachioZoneHandler created for thing: {}", thing.getUID());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {}", command, channelUID);
        
        if (command instanceof RefreshType) {
            refresh();
            return;
        }
        
        String channelId = channelUID.getIdWithoutGroup();
        
        try {
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_ZONE_RUN:
                    handleRunCommand(command);
                    break;
                case RachioZoneHandler.CHANNEL_ZONE_ENABLED:
                    handleEnabledCommand(command);
                    break;
                case RachioZoneHandler.CHANNEL_ZONE_RUN_TIME:
                    handleRuntimeCommand(command);
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
        logger.debug("Initializing Rachio zone handler for thing: {}", getThing().getUID());
        
        config = getConfigAs(RachioZoneConfiguration.class);
        
        if (config.zoneId == null || config.zoneId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                "Zone ID is required");
            return;
        }
        
        if (config.deviceId == null || config.deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                "Device ID is required");
            return;
        }
        
        logger.debug("Zone configuration: zoneId={}, deviceId={}", config.zoneId, config.deviceId);
        
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
        
        // Create dynamic channels for professional data
        createDynamicChannels();
        
        // Schedule initial refresh
        scheduler.schedule(this::refresh, 3, TimeUnit.SECONDS);
        
        updateStatus(ThingStatus.ONLINE);
        logger.info("Rachio zone handler initialized for zone: {}", config.zoneId);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio zone handler");
        
        // Unregister from bridge
        if (bridgeHandler != null) {
            bridgeHandler.unregisterStatusListener(this);
        }
        
        super.dispose();
    }

    @Override
    public void refresh() {
        logger.debug("Refreshing zone data for zoneId: {}", config.zoneId);
        
        try {
            if (rachioHttp == null) {
                logger.error("No HTTP client available for refresh");
                return;
            }
            
            // Fetch zone data from API
            zoneData = rachioHttp.getZone(config.deviceId, config.zoneId);
            
            if (zoneData != null) {
                updateZoneChannels(zoneData);
                updateStatus(ThingStatus.ONLINE);
            } else {
                logger.warn("No zone data returned for zoneId: {}", config.zoneId);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }
            
        } catch (Exception e) {
            logger.error("Error refreshing zone data: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                "Refresh error: " + e.getMessage());
        }
    }

    @Override
    public void onWebhookEvent(String eventType, String deviceId, @Nullable String zoneId, 
                               @Nullable Map<String, Object> data) {
        if (!config.deviceId.equals(deviceId) || !config.zoneId.equals(zoneId)) {
            return; // Event not for this zone
        }
        
        logger.debug("Received webhook event for zone {}: type={}", zoneId, eventType);
        
        // Handle zone-specific events
        switch (eventType) {
            case RachioBindingConstants.EVENT_ZONE_STATUS:
                handleZoneStatusEvent(data);
                break;
            default:
                logger.debug("Unhandled event type for zone: {}", eventType);
        }
        
        // Trigger refresh to get updated data
        scheduler.schedule(this::refresh, 1, TimeUnit.SECONDS);
    }

    private void createDynamicChannels() {
        logger.debug("Creating dynamic channels for zone");
        
        // Create professional irrigation data channels if not already present
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_SOIL_TYPE, "String", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_SOIL_AVAILABLE_WATER, "Number", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_CROP_TYPE, "String", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_CROP_COEFFICIENT, "Number", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_NOZZLE_TYPE, "String", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_NOZZLE_RATE, "Number", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_SLOPE_TYPE, "String", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_SHADE_TYPE, "String", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_ROOT_DEPTH, "Number", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_IRRIGATION_EFFICIENCY, "Number", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_1, "Number", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_2, "Number", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_3, "Number", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_4, "Number", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_5, "Number", "sprinkler");
        createProfessionalChannelIfMissing(RachioBindingConstants.CHANNEL_ZONE_AREA, "Number", "sprinkler");
    }

    private void createProfessionalChannelIfMissing(String channelId, String itemType, String category) {
        if (thing.getChannel(channelId) == null) {
            ChannelUID channelUID = new ChannelUID(thing.getUID(), channelId);
            ChannelTypeUID channelTypeUID = new ChannelTypeUID(RachioBindingConstants.BINDING_ID, 
                channelId.toLowerCase());
            
            Channel channel = ChannelBuilder.create(channelUID, itemType)
                .withType(channelTypeUID)
                .withLabel(getChannelLabel(channelId))
                .withDescription(getChannelDescription(channelId))
                .withCategory(category)
                .build();
            
            updateThing(editThing().withChannel(channel).build());
            logger.debug("Created professional channel: {}", channelId);
        }
    }

    private String getChannelLabel(String channelId) {
        switch (channelId) {
            case RachioBindingConstants.CHANNEL_ZONE_SOIL_TYPE: return "Soil Type";
            case RachioBindingConstants.CHANNEL_ZONE_SOIL_AVAILABLE_WATER: return "Soil Available Water";
            case RachioBindingConstants.CHANNEL_ZONE_CROP_TYPE: return "Crop Type";
            case RachioBindingConstants.CHANNEL_ZONE_CROP_COEFFICIENT: return "Crop Coefficient";
            case RachioBindingConstants.CHANNEL_ZONE_NOZZLE_TYPE: return "Nozzle Type";
            case RachioBindingConstants.CHANNEL_ZONE_NOZZLE_RATE: return "Nozzle Rate";
            case RachioBindingConstants.CHANNEL_ZONE_SLOPE_TYPE: return "Slope Type";
            case RachioBindingConstants.CHANNEL_ZONE_SHADE_TYPE: return "Shade Type";
            case RachioBindingConstants.CHANNEL_ZONE_ROOT_DEPTH: return "Root Depth";
            case RachioBindingConstants.CHANNEL_ZONE_IRRIGATION_EFFICIENCY: return "Irrigation Efficiency";
            case RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_1: return "Adjustment Level 1";
            case RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_2: return "Adjustment Level 2";
            case RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_3: return "Adjustment Level 3";
            case RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_4: return "Adjustment Level 4";
            case RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_5: return "Adjustment Level 5";
            case RachioBindingConstants.CHANNEL_ZONE_AREA: return "Zone Area";
            default: return channelId;
        }
    }

    private String getChannelDescription(String channelId) {
        switch (channelId) {
            case RachioBindingConstants.CHANNEL_ZONE_SOIL_TYPE: return "Type of soil in the zone";
            case RachioBindingConstants.CHANNEL_ZONE_SOIL_AVAILABLE_WATER: return "Available water capacity in soil";
            case RachioBindingConstants.CHANNEL_ZONE_CROP_TYPE: return "Type of crop or vegetation";
            case RachioBindingConstants.CHANNEL_ZONE_CROP_COEFFICIENT: return "Crop coefficient value";
            case RachioBindingConstants.CHANNEL_ZONE_NOZZLE_TYPE: return "Type of irrigation nozzle";
            case RachioBindingConstants.CHANNEL_ZONE_NOZZLE_RATE: return "Nozzle rate in inches per hour";
            case RachioBindingConstants.CHANNEL_ZONE_SLOPE_TYPE: return "Type of slope in the zone";
            case RachioBindingConstants.CHANNEL_ZONE_SHADE_TYPE: return "Type of shade in the zone";
            case RachioBindingConstants.CHANNEL_ZONE_ROOT_DEPTH: return "Root depth in inches";
            case RachioBindingConstants.CHANNEL_ZONE_IRRIGATION_EFFICIENCY: return "Irrigation efficiency percentage";
            case RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_1: return "Water adjustment level 1";
            case RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_2: return "Water adjustment level 2";
            case RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_3: return "Water adjustment level 3";
            case RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_4: return "Water adjustment level 4";
            case RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_5: return "Water adjustment level 5";
            case RachioBindingConstants.CHANNEL_ZONE_AREA: return "Zone area in square feet";
            default: return "Professional irrigation data";
        }
    }

    private void updateZoneChannels(RachioZone zone) {
        logger.debug("Updating channels for zone: {}", zone.name);
        
        // Update zone properties
        updateProperty(RachioBindingConstants.PROPERTY_NAME, zone.name);
        updateProperty(RachioBindingConstants.PROPERTY_ZONE_NUMBER, String.valueOf(zone.zoneNumber));
        
        // Update basic zone channels
        updateState(RachioBindingConstants.CHANNEL_ZONE_ENABLED, OnOffType.from(zone.enabled));
        updateState(RachioBindingConstants.CHANNEL_ZONE_RUN_TIME, new DecimalType(zone.runtime));
        
        // Update zone status (from last known or default)
        if (zone.status != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_STATUS, new StringType(zone.status));
            isRunning = RachioBindingConstants.ZONE_STATUS_STARTED.equals(zone.status);
        } else {
            updateState(RachioBindingConstants.CHANNEL_ZONE_STATUS, 
                new StringType(isRunning ? RachioBindingConstants.ZONE_STATUS_STARTED : RachioBindingConstants.ZONE_STATUS_STOPPED));
        }
        
        updateState(RachioBindingConstants.CHANNEL_ZONE_RUN, OnOffType.from(isRunning));
        
        // Update professional irrigation data channels
        updateProfessionalChannels(zone);
    }

    private void updateProfessionalChannels(RachioZone zone) {
        // Soil data
        if (zone.customSoil != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_SOIL_TYPE, 
                new StringType(zone.customSoil.name));
            if (zone.customSoil.availableWater != null) {
                updateState(RachioBindingConstants.CHANNEL_ZONE_SOIL_AVAILABLE_WATER, 
                    new DecimalType(zone.customSoil.availableWater));
            }
        }
        
        // Crop data
        if (zone.customCrop != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_CROP_TYPE, 
                new StringType(zone.customCrop.name));
            if (zone.customCrop.coefficient != null) {
                updateState(RachioBindingConstants.CHANNEL_ZONE_CROP_COEFFICIENT, 
                    new DecimalType(zone.customCrop.coefficient));
            }
        }
        
        // Nozzle data
        if (zone.customNozzle != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_NOZZLE_TYPE, 
                new StringType(zone.customNozzle.name));
            if (zone.customNozzle.inchesPerHour != null) {
                updateState(RachioBindingConstants.CHANNEL_ZONE_NOZZLE_RATE, 
                    new DecimalType(zone.customNozzle.inchesPerHour));
            }
        }
        
        // Slope data
        if (zone.customSlope != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_SLOPE_TYPE, 
                new StringType(zone.customSlope.name));
        }
        
        // Shade data
        if (zone.customShade != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_SHADE_TYPE, 
                new StringType(zone.customShade.name));
        }
        
        // Root depth
        if (zone.rootDepth != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_ROOT_DEPTH, 
                new DecimalType(zone.rootDepth));
        }
        
        // Irrigation efficiency
        if (zone.efficiency != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_IRRIGATION_EFFICIENCY, 
                new DecimalType(zone.efficiency));
        }
        
        // Water adjustment levels
        if (zone.waterAdjustment != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_1, 
                new DecimalType(zone.waterAdjustment.getOrDefault(1, 0.0)));
            updateState(RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_2, 
                new DecimalType(zone.waterAdjustment.getOrDefault(2, 0.0)));
            updateState(RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_3, 
                new DecimalType(zone.waterAdjustment.getOrDefault(3, 0.0)));
            updateState(RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_4, 
                new DecimalType(zone.waterAdjustment.getOrDefault(4, 0.0)));
            updateState(RachioBindingConstants.CHANNEL_ZONE_ADJUSTMENT_LEVEL_5, 
                new DecimalType(zone.waterAdjustment.getOrDefault(5, 0.0)));
        }
        
        // Zone area
        if (zone.area != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_AREA, 
                new DecimalType(zone.area));
        }
    }

    private void handleRunCommand(Command command) {
        try {
            if (rachioHttp == null) {
                logger.error("No HTTP client available for run command");
                return;
            }
            
            if (command instanceof OnOffType) {
                if (command == OnOffType.ON) {
                    // Start zone with default runtime or configured runtime
                    int runtime = currentRuntime != null ? currentRuntime : 
                        (config.defaultRuntime != null ? config.defaultRuntime : 300);
                    
                    rachioHttp.startZone(config.deviceId, config.zoneId, runtime);
                    isRunning = true;
                    updateState(RachioBindingConstants.CHANNEL_ZONE_STATUS, 
                        new StringType(RachioBindingConstants.ZONE_STATUS_STARTED));
                    logger.info("Started zone {} for {} seconds", config.zoneId, runtime);
                } else {
                    // Stop zone
                    rachioHttp.stopZone(config.deviceId, config.zoneId);
                    isRunning = false;
                    updateState(RachioBindingConstants.CHANNEL_ZONE_STATUS, 
                        new StringType(RachioBindingConstants.ZONE_STATUS_STOPPED));
                    logger.info("Stopped zone {}", config.zoneId);
                }
                
                updateState(RachioBindingConstants.CHANNEL_ZONE_RUN, (OnOffType) command);
                
                // Refresh after command
                scheduler.schedule(this::refresh, 2, TimeUnit.SECONDS);
            }
            
        } catch (Exception e) {
            logger.error("Error handling run command: {}", e.getMessage(), e);
        }
    }

    private void handleEnabledCommand(Command command) {
        if (command instanceof OnOffType && rachioHttp != null) {
            try {
                boolean enabled = command == OnOffType.ON;
                rachioHttp.setZoneEnabled(config.deviceId, config.zoneId, enabled);
                logger.info("Set zone {} enabled to {}", config.zoneId, enabled);
                
                // Refresh after command
                scheduler.schedule(this::refresh, 2, TimeUnit.SECONDS);
                
            } catch (Exception e) {
                logger.error("Error handling enabled command: {}", e.getMessage(), e);
            }
        }
    }

    private void handleRuntimeCommand(Command command) {
        if (command instanceof DecimalType && rachioHttp != null) {
            try {
                int runtime = ((DecimalType) command).intValue();
                currentRuntime = runtime;
                logger.debug("Set zone runtime to {} seconds", runtime);
                
                // Note: This just stores the runtime for next start
                // Actual API call happens when zone is started
                
            } catch (Exception e) {
                logger.error("Error handling runtime command: {}", e.getMessage(), e);
            }
        }
    }

    private void handleZoneStatusEvent(@Nullable Map<String, Object> data) {
        if (data == null) return;
        
        Object status = data.get("status");
        if (status instanceof String) {
            String statusStr = (String) status;
            updateState(RachioBindingConstants.CHANNEL_ZONE_STATUS, new StringType(statusStr));
            
            isRunning = RachioBindingConstants.ZONE_STATUS_STARTED.equals(statusStr);
            updateState(RachioBindingConstants.CHANNEL_ZONE_RUN, OnOffType.from(isRunning));
            
            logger.debug("Updated zone status from webhook: {}", statusStr);
            
            // If zone completed, update run time if available
            if (RachioBindingConstants.ZONE_STATUS_COMPLETED.equals(statusStr)) {
                Object duration = data.get("duration");
                if (duration instanceof Number) {
                    updateState(RachioBindingConstants.CHANNEL_ZONE_RUN_TIME, 
                        new DecimalType(((Number) duration).intValue()));
                }
            }
        }
    }

    private @Nullable RachioBridgeHandler getBridgeHandler() {
        if (getBridge() == null) {
            return null;
        }
        return (RachioBridgeHandler) getBridge().getHandler();
    }

    @Override
    public Thing getThing() {
        return super.getThing();
    }
}
