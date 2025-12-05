package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
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

/**
 * The {@link RachioZoneHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler implements RachioStatusListener {

    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);
    private final Map<String, String> zoneProperties = new HashMap<>();

    private @Nullable RachioZoneConfiguration config;
    private @Nullable RachioHttp httpHandler;
    private @Nullable ScheduledFuture<?> pollingJob;

    private int pollInterval = 60; // seconds
    private @Nullable RachioZone zoneData;
    private ZonedDateTime lastUpdated;

    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshZoneData();
            return;
        }

        try {
            RachioHttp http = httpHandler;
            RachioZoneConfiguration config = this.config;
            
            if (http == null || config == null) {
                logger.warn("Handler not properly initialized");
                return;
            }

            switch (channelUID.getIdWithoutGroup()) {
                case CHANNEL_ZONE_ENABLED:
                    if (command instanceof OnOffType) {
                        boolean enabled = command == OnOffType.ON;
                        http.setZoneEnabled(config.zoneId, enabled);
                        updateState(CHANNEL_ZONE_ENABLED, (OnOffType) command);
                        logger.info("Zone {} enabled set to {}", config.name, enabled);
                    }
                    break;

                case CHANNEL_ZONE_RUN:
                    if (command instanceof DecimalType || command instanceof QuantityType) {
                        int duration = ((DecimalType) command).intValue();
                        if (duration > 0) {
                            http.startZone(config.zoneId, duration);
                            updateState(CHANNEL_ZONE_STATUS, new StringType("RUNNING"));
                            updateState(CHANNEL_ZONE_LAST_RUN, new DateTimeType(ZonedDateTime.now()));
                            logger.info("Started zone {} for {} seconds", config.name, duration);
                        }
                    }
                    break;
            }
        } catch (RachioApiException e) {
            logger.error("Failed to execute command {}: {}", command, e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler");
        
        config = getConfigAs(RachioZoneConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No bridge assigned");
            return;
        }

        RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) bridge.getHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge handler not available");
            return;
        }

        // Get HTTP handler from bridge
        httpHandler = bridgeHandler.getHttpHandler();
        if (httpHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not properly initialized");
            return;
        }

        // Register as listener to bridge
        bridgeHandler.registerListener(this);

        // Create dynamic channels
        createChannels();

        // Start polling
        startPolling();

        // Initial refresh
        refreshZoneData();
    }

    private void createChannels() {
        logger.debug("Creating channels for zone {}", config != null ? config.name : "unknown");
        
        List<Channel> channels = new ArrayList<>();
        ChannelUID thingUID = getThing().getUID();

        // Basic Zone Channels
        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_ZONE_NAME), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_NAME))
                .withLabel("Zone Name")
                .withDescription("Name of the irrigation zone")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_ZONE_ENABLED), "Switch")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_ENABLED))
                .withLabel("Zone Enabled")
                .withDescription("Whether the zone is enabled for watering")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_ZONE_RUN), "Number")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_RUN))
                .withLabel("Run Zone")
                .withDescription("Run zone for specified duration in seconds")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_ZONE_STATUS), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_STATUS))
                .withLabel("Zone Status")
                .withDescription("Current status of the zone")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_ZONE_LAST_RUN), "DateTime")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_LAST_RUN))
                .withLabel("Last Run")
                .withDescription("When the zone was last run")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_ZONE_RUN_TIME), "Number:Time")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_RUN_TIME))
                .withLabel("Run Time")
                .withDescription("Zone runtime in seconds")
                .build());

        // PROFESSIONAL IRRIGATION DATA CHANNELS
        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_SOIL_TYPE), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_SOIL_TYPE))
                .withLabel("Soil Type")
                .withDescription("Type of soil in this zone")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_CROP_TYPE), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_CROP_TYPE))
                .withLabel("Crop Type")
                .withDescription("Type of crop/vegetation in this zone")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_CROP_COEFFICIENT), "Number:Dimensionless")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_CROP_COEFFICIENT))
                .withLabel("Crop Coefficient")
                .withDescription("Water needs coefficient (0-1)")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_NOZZLE_TYPE), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_NOZZLE_TYPE))
                .withLabel("Nozzle Type")
                .withDescription("Type of irrigation nozzle/sprinkler")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_NOZZLE_RATE), "Number")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_NOZZLE_RATE))
                .withLabel("Nozzle Rate")
                .withDescription("Water application rate (inches/hour)")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_EFFICIENCY), "Number:Dimensionless")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_EFFICIENCY))
                .withLabel("Irrigation Efficiency")
                .withDescription("System efficiency percentage")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_ROOT_DEPTH), "Number:Length")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ROOT_DEPTH))
                .withLabel("Root Depth")
                .withDescription("Plant root zone depth")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_ZONE_AREA), "Number:Area")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_ZONE_AREA))
                .withLabel("Zone Area")
                .withDescription("Zone area in square feet")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_SLOPE_TYPE), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_SLOPE_TYPE))
                .withLabel("Slope Type")
                .withDescription("Type of slope in zone")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_SHADE_TYPE), "String")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_SHADE_TYPE))
                .withLabel("Shade Type")
                .withDescription("Type of shade in zone")
                .build());

        channels.add(ChannelBuilder.create(new ChannelUID(thingUID, CHANNEL_WATER_ADJUSTMENT), "Number")
                .withType(new ChannelTypeUID(BINDING_ID, CHANNEL_WATER_ADJUSTMENT))
                .withLabel("Water Adjustment")
                .withDescription("Watering adjustment level (1-5)")
                .build());

        // Update the thing with all channels
        updateThing(editThing().withChannels(channels).build());
        
        logger.debug("Created {} channels for zone", channels.size());
    }

    private void startPolling() {
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
        }

        this.pollingJob = scheduler.scheduleWithFixedDelay(this::refreshZoneData, 10, pollInterval, TimeUnit.SECONDS);
        logger.debug("Started polling for zone {} every {} seconds", 
                config != null ? config.name : "unknown", pollInterval);
    }

    private void refreshZoneData() {
        try {
            RachioHttp http = httpHandler;
            RachioZoneConfiguration config = this.config;
            
            if (http == null || config == null) {
                logger.debug("Cannot refresh zone data - handler not initialized");
                return;
            }

            // Get device data from bridge to find our zone
            Bridge bridge = getBridge();
            if (bridge == null) {
                return;
            }

            RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) bridge.getHandler();
            if (bridgeHandler == null) {
                return;
            }

            // Get updated zone data from bridge
            RachioZone zone = bridgeHandler.getZoneData(config.zoneId);
            if (zone != null) {
                updateZoneData(zone);
            }

            lastUpdated = ZonedDateTime.now();
            updateStatus(ThingStatus.ONLINE);

        } catch (Exception e) {
            logger.debug("Error refreshing zone data: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void updateZoneData(RachioZone zone) {
        this.zoneData = zone;
        RachioZoneConfiguration config = this.config;
        
        if (config == null || zone == null) {
            return;
        }

        logger.debug("Updating zone data for {} (ID: {})", config.name, config.zoneId);

        // Update basic zone info
        updateState(CHANNEL_ZONE_NAME, new StringType(zone.name));
        updateState(CHANNEL_ZONE_ENABLED, OnOffType.from(zone.enabled));
        updateState(CHANNEL_ZONE_RUN_TIME, new QuantityType<>(zone.runtime, Units.SECOND));

        // Update professional irrigation data
        updateProfessionalData(zone);

        // Update properties for thing
        Map<String, String> properties = new HashMap<>();
        properties.put("zoneId", zone.id);
        properties.put("zoneNumber", String.valueOf(zone.zoneNumber));
        properties.put("deviceId", config.deviceId);
        updateProperties(properties);
    }

    private void updateProfessionalData(RachioZone zone) {
        // Update soil data
        if (zone.customSoil != null) {
            updateState(CHANNEL_SOIL_TYPE, new StringType(zone.customSoil.name));
        }

        // Update crop data
        if (zone.customCrop != null) {
            updateState(CHANNEL_CROP_TYPE, new StringType(zone.customCrop.name));
            updateState(CHANNEL_CROP_COEFFICIENT, new DecimalType(zone.customCrop.coefficient));
        }

        // Update nozzle data
        if (zone.customNozzle != null) {
            updateState(CHANNEL_NOZZLE_TYPE, new StringType(zone.customNozzle.name));
            updateState(CHANNEL_NOZZLE_RATE, new QuantityType<>(zone.customNozzle.inchesPerHour, ImperialUnits.INCH_PER_HOUR));
        }

        // Update slope and shade data
        if (zone.customSlope != null) {
            updateState(CHANNEL_SLOPE_TYPE, new StringType(zone.customSlope.name));
        }
        if (zone.customShade != null) {
            updateState(CHANNEL_SHADE_TYPE, new StringType(zone.customShade.name));
        }

        // Update other professional data
        updateState(CHANNEL_EFFICIENCY, new QuantityType<>(zone.efficiency, Units.PERCENT));
        updateState(CHANNEL_ROOT_DEPTH, new QuantityType<>(zone.rootZoneDepth, ImperialUnits.INCH));
        
        if (zone.yardAreaSquareFeet > 0) {
            updateState(CHANNEL_ZONE_AREA, new QuantityType<>(zone.yardAreaSquareFeet, ImperialUnits.SQUARE_FOOT));
        }

        // Update water adjustment if available
        if (zone.wateringAdjustmentRuntimes != null && zone.wateringAdjustmentRuntimes.length > 0) {
            // Calculate average or use specific level
            int adjustmentLevel = Math.min(5, Math.max(1, (zone.efficiency / 20))); // Estimate from efficiency
            updateState(CHANNEL_WATER_ADJUSTMENT, new DecimalType(adjustmentLevel));
        }
    }

    @Override
    public void zoneStatusChanged(RachioZone zone) {
        if (zone != null && config != null && zone.id.equals(config.zoneId)) {
            logger.debug("Zone status update received for {}", zone.name);
            updateZoneData(zone);
        }
    }

    @Override
    public void deviceStatusChanged(String deviceId, boolean online) {
        // Zone handler doesn't need to react to device status directly
    }

    @Override
    public void webhookEventReceived(String deviceId, String eventType, String eventData) {
        // Zone handler can process webhook events if needed
        if (config != null && deviceId.equals(config.deviceId)) {
            logger.debug("Webhook event received for device {}: {}", deviceId, eventType);
            // Could parse eventData to update specific zone status
        }
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> pollingJob = this.pollingJob;
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
        }
        this.pollingJob = null;

        // Unregister from bridge
        Bridge bridge = getBridge();
        if (bridge != null) {
            RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) bridge.getHandler();
            if (bridgeHandler != null) {
                bridgeHandler.unregisterListener(this);
            }
        }

        super.dispose();
        logger.debug("Disposed zone handler for {}", config != null ? config.name : "unknown");
    }

    public @Nullable RachioZone getZoneData() {
        return zoneData;
    }

    public void setPollInterval(int interval) {
        this.pollInterval = interval;
        startPolling();
    }
}
