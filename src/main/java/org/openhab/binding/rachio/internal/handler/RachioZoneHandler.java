package org.openhab.binding.rachio.internal.handler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands for a single
 * Rachio irrigation zone.
 *
 * @author Damion Boyett - Refactor contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends RachioHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);

    private @Nullable RachioZoneConfiguration config;
    private @Nullable ScheduledFuture<?> refreshJob;
    private @Nullable RachioZone lastZoneData;

    private boolean channelsCreated = false;

    /**
     * Constructor for manual instantiation by factory
     *
     * @param thing The zone thing
     * @param scheduler Scheduled executor service (injected by factory)
     */
    public RachioZoneHandler(Thing thing, ScheduledExecutorService scheduler) {
        super(thing);
        logger.debug("RachioZoneHandler created with scheduler injection");
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler for thing {}", getThing().getUID());

        config = getConfigAs(RachioZoneConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);

        createZoneChannels();

        Thing bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not available");
            return;
        }

        ThingHandler bridgeHandler = bridge.getHandler();

        if (bridgeHandler instanceof RachioBridgeHandler) {
            RachioBridgeHandler rachioBridgeHandler = (RachioBridgeHandler) bridgeHandler;
            rachioBridgeHandler.registerZoneHandler(this);
            rachioBridgeHandler.registerStatusListener(this);
            logger.debug("Registered with bridge handler");
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    "Bridge handler not available or wrong type");
            return;
        }

        // Schedule initial refresh
        scheduler.schedule(this::refreshZone, 2, TimeUnit.SECONDS);

        logger.debug("Rachio zone handler initialized for thing {}", getThing().getUID());
    }

    @Override
    protected void performRefresh(ChannelUID channelUID) {
        logger.debug("Performing refresh for channel: {}", channelUID);

        // Refresh specific channel based on its ID
        String channelId = channelUID.getId();
        RachioZone zoneData = lastZoneData;

        if (zoneData != null) {
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_ZONE_RUN:
                    updateRunStatusFromZoneData(zoneData);
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_ENABLED:
                    updateState(channelUID, zoneData.enabled ? OnOffType.ON : OnOffType.OFF);
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_RUNTIME:
                    updateState(channelUID, new QuantityType<>(zoneData.runtime, Units.MINUTE));
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_SOIL:
                    if (zoneData.soil != null) {
                        updateState(channelUID, new StringType(zoneData.soil));
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_CROP:
                    if (zoneData.crop != null) {
                        updateState(channelUID, new StringType(zoneData.crop));
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_NOZZLE:
                    if (zoneData.nozzle != null) {
                        updateState(channelUID, new StringType(zoneData.nozzle));
                    }
                    break;
                default:
                    logger.debug("No specific refresh handler for channel: {}", channelId);
                    refreshZone();
            }
        } else {
            refreshZone();
        }
    }

    private void createZoneChannels() {
        if (channelsCreated) {
            logger.debug("Zone channels already created, skipping");
            return;
        }

        logger.debug("Creating zone channels for thing {}", getThing().getUID());

        try {
            ThingBuilder thingBuilder = editThing();
            boolean modified = false;

            // ===== ZONE CONTROL CHANNELS =====

            // 1. Zone Run (Switch) - Start/Stop watering
            ChannelUID zoneRunChannelUID = new ChannelUID(getThing().getUID(), RachioBindingConstants.CHANNEL_ZONE_RUN);
            if (getThing().getChannel(zoneRunChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_ZONE_RUN;
                ChannelBuilder channelBuilder = ChannelBuilder.create(zoneRunChannelUID, "Switch")
                        .withType(channelTypeUID).withLabel("Zone Run")
                        .withDescription("Start or stop watering this zone");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_RUN);
            }

            // 2. Zone Enabled (Switch) - Enable/Disable zone
            ChannelUID zoneEnabledChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_ENABLED);
            if (getThing().getChannel(zoneEnabledChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_ZONE_ENABLED;
                ChannelBuilder channelBuilder = ChannelBuilder.create(zoneEnabledChannelUID, "Switch")
                        .withType(channelTypeUID).withLabel("Zone Enabled")
                        .withDescription("Enable or disable this zone for watering");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_ENABLED);
            }

            // 3. Zone Runtime (Number:Time)
            ChannelUID zoneRuntimeChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_RUNTIME);
            if (getThing().getChannel(zoneRuntimeChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_ZONE_RUNTIME;
                Configuration runtimeConfig = new Configuration();
                runtimeConfig.put("unit", "min");
                ChannelBuilder channelBuilder = ChannelBuilder.create(zoneRuntimeChannelUID, "Number")
                        .withType(channelTypeUID).withLabel("Zone Runtime")
                        .withDescription("Current runtime setting for this zone in minutes")
                        .withConfiguration(runtimeConfig);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_RUNTIME);
            }

            // 4. Zone Duration (Number:Time) - based on last run
            ChannelUID zoneDurationChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_DURATION);
            if (getThing().getChannel(zoneDurationChannelUID) == null) {
                Configuration durationConfig = new Configuration();
                durationConfig.put("unit", "s");
                ChannelBuilder channelBuilder = ChannelBuilder.create(zoneDurationChannelUID, "Number")
                        .withLabel("Zone Duration").withDescription("Set watering duration for this zone in seconds")
                        .withConfiguration(durationConfig);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_DURATION);
            }

            // ===== PROFESSIONAL IRRIGATION DATA CHANNELS =====

            // 5. Soil Type (String)
            ChannelUID soilTypeChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_SOIL);
            if (getThing().getChannel(soilTypeChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_ZONE_SOIL;
                ChannelBuilder channelBuilder = ChannelBuilder.create(soilTypeChannelUID, "String")
                        .withType(channelTypeUID).withLabel("Soil Type")
                        .withDescription("Soil classification for this zone");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_SOIL);
            }

            // 6. Crop Type (String)
            ChannelUID cropTypeChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_CROP);
            if (getThing().getChannel(cropTypeChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_ZONE_CROP;
                ChannelBuilder channelBuilder = ChannelBuilder.create(cropTypeChannelUID, "String")
                        .withType(channelTypeUID).withLabel("Crop Type")
                        .withDescription("Crop or plant type for this zone");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_CROP);
            }

            // 7. Nozzle Type (String)
            ChannelUID nozzleTypeChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_NOZZLE);
            if (getThing().getChannel(nozzleTypeChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_ZONE_NOZZLE;
                ChannelBuilder channelBuilder = ChannelBuilder.create(nozzleTypeChannelUID, "String")
                        .withType(channelTypeUID).withLabel("Nozzle Type")
                        .withDescription("Sprinkler nozzle type for this zone");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_NOZZLE);
            }

            // 8. Shade Type (String)
            ChannelUID shadeTypeChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_SHADE);
            if (getThing().getChannel(shadeTypeChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(shadeTypeChannelUID, "String")
                        .withLabel("Shade Type").withDescription("Shade classification for this zone");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_SHADE);
            }

            // 9. Slope Type (String)
            ChannelUID slopeTypeChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_SLOPE);
            if (getThing().getChannel(slopeTypeChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(slopeTypeChannelUID, "String")
                        .withLabel("Slope Type").withDescription("Slope classification for this zone");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_SLOPE);
            }

            // 10. Irrigation Efficiency (Number:Dimensionless)
            ChannelUID efficiencyChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_IRRIGATION_EFFICIENCY);
            if (getThing().getChannel(efficiencyChannelUID) == null) {
                ChannelTypeUID channelTypeUID = RachioBindingConstants.CHANNEL_TYPE_ZONE_IRRIGATION_EFFICIENCY;
                Configuration efficiencyConfig = new Configuration();
                efficiencyConfig.put("unit", "%");
                ChannelBuilder channelBuilder = ChannelBuilder.create(efficiencyChannelUID, "Number")
                        .withType(channelTypeUID).withLabel("Irrigation Efficiency")
                        .withDescription("Water application efficiency percentage (0-100%)")
                        .withConfiguration(efficiencyConfig);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_IRRIGATION_EFFICIENCY);
            }

            // 11. Zone Efficiency (Number:Dimensionless) - using availableWater field
            ChannelUID zoneEfficiencyChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_EFFICIENCY);
            if (getThing().getChannel(zoneEfficiencyChannelUID) == null) {
                Configuration zoneEfficiencyConfig = new Configuration();
                zoneEfficiencyConfig.put("unit", "%");
                ChannelBuilder channelBuilder = ChannelBuilder.create(zoneEfficiencyChannelUID, "Number")
                        .withLabel("Zone Efficiency").withDescription("Overall zone efficiency factor")
                        .withConfiguration(zoneEfficiencyConfig);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_EFFICIENCY);
            }

            // ===== WATER MANAGEMENT CHANNELS =====

            // 12. Available Water (Number:Length) - ACTUAL FIELD: availableWater
            ChannelUID waterAvailableChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_WATER_AVAILABLE);
            if (getThing().getChannel(waterAvailableChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(waterAvailableChannelUID, "Number")
                        .withLabel("Water Available").withDescription("Water available for this zone in millimeters");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_WATER_AVAILABLE);
            }

            // 13. Water Depth (Number:Length) - ACTUAL FIELD: depthOfWater
            ChannelUID waterDepthChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_WATER_DEPTH);
            if (getThing().getChannel(waterDepthChannelUID) == null) {
                Configuration waterDepthConfig = new Configuration();
                waterDepthConfig.put("unit", "mm");
                ChannelBuilder channelBuilder = ChannelBuilder.create(waterDepthChannelUID, "Number")
                        .withLabel("Water Depth").withDescription("Water depth applied to this zone in millimeters")
                        .withConfiguration(waterDepthConfig);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_WATER_DEPTH);
            }

            // 14. Root Depth (Number:Length) - ACTUAL FIELD: rootZoneDepth
            ChannelUID rootDepthChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_ROOT_DEPTH);
            if (getThing().getChannel(rootDepthChannelUID) == null) {
                Configuration rootDepthConfig = new Configuration();
                rootDepthConfig.put("unit", "mm");
                ChannelBuilder channelBuilder = ChannelBuilder.create(rootDepthChannelUID, "Number")
                        .withLabel("Root Depth").withDescription("Root zone depth for this zone in millimeters")
                        .withConfiguration(rootDepthConfig);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_ROOT_DEPTH);
            }

            // 15. Moisture depletion (Number:Dimensionless) - ACTUAL FIELD: managementAllowedDepletion
            ChannelUID depletionChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_DEPLETION);
            if (getThing().getChannel(depletionChannelUID) == null) {
                Configuration depletionConfig = new Configuration();
                depletionConfig.put("unit", "%");
                ChannelBuilder channelBuilder = ChannelBuilder.create(depletionChannelUID, "Number")
                        .withLabel("Water Depletion").withDescription("Water depletion level for this zone (0-100%)")
                        .withConfiguration(depletionConfig);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_DEPLETION);
            }

            // 16. Zone Area (Number:Area) - ACTUAL FIELD: zoneArea
            ChannelUID areaChannelUID = new ChannelUID(getThing().getUID(), RachioBindingConstants.CHANNEL_ZONE_AREA);
            if (getThing().getChannel(areaChannelUID) == null) {
                Configuration areaConfig = new Configuration();
                areaConfig.put("unit", "m²");
                ChannelBuilder channelBuilder = ChannelBuilder.create(areaChannelUID, "Number").withLabel("Zone Area")
                        .withDescription("Area of this zone in square meters").withConfiguration(areaConfig);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_AREA);
            }

            // ===== USAGE AND MONITORING CHANNELS =====

            // 17. Zone Usage Total (Number:Time) - using runtime field
            ChannelUID usageTotalChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_USAGE_TOTAL);
            if (getThing().getChannel(usageTotalChannelUID) == null) {
                Configuration usageTotalConfig = new Configuration();
                usageTotalConfig.put("unit", "min");
                ChannelBuilder channelBuilder = ChannelBuilder.create(usageTotalChannelUID, "Number")
                        .withLabel("Zone Usage Total").withDescription("Total watering time for this zone in minutes")
                        .withConfiguration(usageTotalConfig);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_USAGE_TOTAL);
            }

            // 18. Zone Last Run Start (DateTime) - using startDate field (Instant)
            ChannelUID lastRunStartChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_LAST_RUN_START);
            if (getThing().getChannel(lastRunStartChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(lastRunStartChannelUID, "DateTime")
                        .withLabel("Zone Last Run Start").withDescription("Start time of the last run for this zone");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_LAST_RUN_START);
            }

            // 19. Zone Last Run End (DateTime) - using endDate field (Instant)
            ChannelUID lastRunEndChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_LAST_RUN_END);
            if (getThing().getChannel(lastRunEndChannelUID) == null) {
                ChannelBuilder channelBuilder = ChannelBuilder.create(lastRunEndChannelUID, "DateTime")
                        .withLabel("Zone Last Run End").withDescription("End time of the last run for this zone");
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_LAST_RUN_END);
            }

            // 20. Zone Last Run Duration (Number:Time) - using duration field
            ChannelUID lastRunDurationChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_LAST_RUN_DURATION);
            if (getThing().getChannel(lastRunDurationChannelUID) == null) {
                Configuration lastRunDurationConfig = new Configuration();
                lastRunDurationConfig.put("unit", "s");
                ChannelBuilder channelBuilder = ChannelBuilder.create(lastRunDurationChannelUID, "Number")
                        .withLabel("Zone Last Run Duration")
                        .withDescription("Duration of the last run for this zone in seconds")
                        .withConfiguration(lastRunDurationConfig);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_LAST_RUN_DURATION);
            }

            // 21. Zone Last Run Water (Number:Volume) - using totalWater field
            ChannelUID lastRunWaterChannelUID = new ChannelUID(getThing().getUID(),
                    RachioBindingConstants.CHANNEL_ZONE_LAST_RUN_WATER);
            if (getThing().getChannel(lastRunWaterChannelUID) == null) {
                Configuration lastRunWaterConfig = new Configuration();
                lastRunWaterConfig.put("unit", "L");
                ChannelBuilder channelBuilder = ChannelBuilder.create(lastRunWaterChannelUID, "Number")
                        .withLabel("Zone Last Run Water")
                        .withDescription("Water used in the last run for this zone in liters")
                        .withConfiguration(lastRunWaterConfig);
                thingBuilder.withChannel(channelBuilder.build());
                modified = true;
                logger.debug("Created channel: {}", RachioBindingConstants.CHANNEL_ZONE_LAST_RUN_WATER);
            }

            if (modified) {
                updateThing(thingBuilder.build());
                channelsCreated = true;
                logger.debug("Successfully created zone channels for thing {}", getThing().getUID());
            } else {
                logger.debug("All zone channels already exist for thing {}", getThing().getUID());
                channelsCreated = true;
            }
        } catch (Exception e) {
            logger.error("Failed to create zone channels for thing {}: {}", getThing().getUID(), e.getMessage(), e);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {}", command, channelUID);

        if (command instanceof RefreshType) {
            super.handleCommand(channelUID, command);
            return;
        }

        // Get configuration
        RachioZoneConfiguration localConfig = config;
        if (localConfig == null) {
            logger.warn("Zone configuration not available");
            return;
        }

        Thing bridge = getBridge();
        if (bridge == null) {
            logger.warn("Bridge not available for command execution");
            return;
        }

        ThingHandler bridgeHandler = bridge.getHandler();

        if (!(bridgeHandler instanceof RachioBridgeHandler)) {
            logger.warn("Bridge handler not available or not a RachioBridgeHandler");
            return;
        }

        RachioBridgeHandler rachioBridgeHandler = (RachioBridgeHandler) bridgeHandler;

        // Execute command based on channel
        String channelId = channelUID.getId();
        try {
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_ZONE_RUN:
                    if (command instanceof OnOffType) {
                        if (command == OnOffType.ON) {
                            // Get default duration from zone runtime or use 5 minutes (300 seconds)
                            RachioZone zoneData = lastZoneData;
                            int duration = zoneData != null ? zoneData.runtime * 60 : 300; // Convert minutes to seconds
                            rachioBridgeHandler.startZone(localConfig.zoneId, duration);
                        } else {
                            rachioBridgeHandler.stopZone(localConfig.zoneId);
                        }
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_ENABLED:
                    if (command instanceof OnOffType) {
                        rachioBridgeHandler.setZoneEnabled(localConfig.zoneId, command == OnOffType.ON);
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_RUNTIME:
                    if (command instanceof DecimalType) {
                        int minutes = ((DecimalType) command).intValue();
                        if (lastZoneData != null) {
                            lastZoneData.runtime = minutes;
                            updateState(channelUID, new QuantityType<>(minutes, Units.MINUTE));
                            logger.debug("Updated zone runtime locally to {} minutes", minutes);
                        }
                    }
                    break;
                case RachioBindingConstants.CHANNEL_ZONE_DURATION:
                    if (command instanceof DecimalType) {
                        int seconds = ((DecimalType) command).intValue();
                        logger.debug("Zone duration set to {} seconds (implementation pending)", seconds);
                        updateState(channelUID, new DecimalType(seconds));
                    }
                    break;
                default:
                    logger.debug("Unhandled command for channel {}", channelId);
            }
        } catch (Exception e) {
            logger.error("Error executing command {} for channel {}: {}", command, channelId, e.getMessage(), e);
        }
    }

    /**
     * Refresh zone data from bridge
     */
    private void refreshZone() {

        Thing bridge = getBridge();
        if (bridge == null) {
            logger.warn("Bridge not available for zone refresh");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not available");
            return;
        }

        ThingHandler bridgeHandler = bridge.getHandler();

        if (bridgeHandler instanceof RachioBridgeHandler) {
            RachioZoneConfiguration localConfig = config;
            if (localConfig != null) {

                RachioZone zoneData = ((RachioBridgeHandler) bridgeHandler).getZoneData(localConfig.zoneId);
                if (zoneData != null) {
                    updateZoneData(zoneData);
                }
            }
        }
    }

    /**
     * Update zone data with new information
     */
    public void updateZoneData(@Nullable RachioZone zoneData) {
        if (zoneData == null) {
            return;
        }

        lastZoneData = zoneData;

        // Update basic zone properties
        updateState(RachioBindingConstants.CHANNEL_ZONE_ENABLED, zoneData.enabled ? OnOffType.ON : OnOffType.OFF);
        updateState(RachioBindingConstants.CHANNEL_ZONE_RUNTIME, new QuantityType<>(zoneData.runtime, Units.MINUTE));

        if (zoneData.soil != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_SOIL, new StringType(zoneData.soil));
        }

        if (zoneData.crop != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_CROP, new StringType(zoneData.crop));
        }

        if (zoneData.nozzle != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_NOZZLE, new StringType(zoneData.nozzle));
        }

        if (zoneData.shade != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_SHADE, new StringType(zoneData.shade));
        }

        if (zoneData.slope != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_SLOPE, new StringType(zoneData.slope));
        }

        updateState(RachioBindingConstants.CHANNEL_ZONE_IRRIGATION_EFFICIENCY, new DecimalType(zoneData.efficiency));

        updateState(RachioBindingConstants.CHANNEL_ZONE_WATER_AVAILABLE,
                new QuantityType<>(zoneData.availableWater, MetricPrefix.MILLI(SIUnits.METRE)));

        // Water Depth - using depthOfWater field
        updateState(RachioBindingConstants.CHANNEL_ZONE_WATER_DEPTH,
                new QuantityType<>(zoneData.depthOfWater, MetricPrefix.MILLI(SIUnits.METRE)));

        // Root Depth - using rootZoneDepth field
        updateState(RachioBindingConstants.CHANNEL_ZONE_ROOT_DEPTH,
                new QuantityType<>(zoneData.rootZoneDepth, MetricPrefix.MILLI(SIUnits.METRE)));

        // Moisture depletion - using managementAllowedDepletion field
        updateState(RachioBindingConstants.CHANNEL_ZONE_DEPLETION,
                new DecimalType(zoneData.managementAllowedDepletion));

        // Zone Area - using zoneArea field
        updateState(RachioBindingConstants.CHANNEL_ZONE_AREA,
                new QuantityType<>(zoneData.zoneArea, SIUnits.SQUARE_METRE));

        updateStatus(ThingStatus.ONLINE);
    }

    /**
     * Update zone run status from ZoneRunStatus enum
     * 
     */
    public void updateZoneRunStatus(@Nullable ZoneRunStatus runStatus) {

        if (runStatus == null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_RUN, OnOffType.OFF);
            return;
        }

        boolean isRunning = runStatus.isActive();
        updateState(RachioBindingConstants.CHANNEL_ZONE_RUN, isRunning ? OnOffType.ON : OnOffType.OFF);

        // Get duration from last zone data if available
        RachioZone zoneData = lastZoneData;
        if (zoneData != null && zoneData.duration > 0) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_DURATION, new DecimalType(zoneData.duration));
        }
    }

    /**
     * Helper method to update run status from zone data
     */
    private void updateRunStatusFromZoneData(RachioZone zoneData) {
        ZoneRunStatus runStatus = zoneData.lastRunStatus;
        if (runStatus != null) {
            updateZoneRunStatus(runStatus);
        } else {
            // Default to not running if no status
            updateState(RachioBindingConstants.CHANNEL_ZONE_RUN, OnOffType.OFF);
        }
    }

    @Override
    public String getListenerId() {
        return getThing().getUID().toString();
    }

    @Override
    public RachioStatusListener.ListenerType getListenerType() {
        return RachioStatusListener.ListenerType.ZONE;
    }

    @Override
    public void onNewDevice(@Nullable String deviceId) {
        // Zone handler doesn't need to handle new devices
    }

    @Override
    public void onNewZone(@Nullable String deviceId, @Nullable String zoneId) {
        // Check if this zone matches our configuration
        RachioZoneConfiguration localConfig = config;
        if (localConfig != null && zoneId != null && zoneId.equals(localConfig.zoneId)) {
            logger.debug("New zone notification received for our zone: {}", zoneId);
            refreshZone();
        }
    }

    @Override
    public void onDeviceStatusChanged(@Nullable RachioDevice device) {
        // Zone handler doesn't need full device updates
    }

    @Override
    public void onDeviceStatusChanged(@Nullable String deviceId, @Nullable String status) {
        // Zone handler doesn't need device status updates by ID
    }

    @Override
    public void onWebhookEventReceived(@Nullable String eventJson) {
        // Webhook events are handled by the bridge, zone handler can ignore
    }

    @Override
    public void onDeviceUpdated(@Nullable RachioDevice device) {
        // Zone handler doesn't need full device updates
    }

    @Override
    public void onZoneUpdated(@Nullable String zoneId, @Nullable RachioZone zone) {
        // Check if this update is for our zone
        RachioZoneConfiguration localConfig = config;
        if (localConfig != null && zoneId != null && zoneId.equals(localConfig.zoneId) && zone != null) {
            logger.debug("Zone update received for our zone: {}", zoneId);
            updateZoneData(zone);
        }
    }

    @Override
    public void onRateLimitStatusChanged(int remaining, int limit, @Nullable String status) {
        // Zone handler doesn't need rate limit updates
    }

    @Override
    public void onZoneStatusChanged(@Nullable String deviceId, @Nullable String zoneId, @Nullable String status) {
        // Check if this status change is for our zone
        RachioZoneConfiguration localConfig = config;
        if (localConfig != null && zoneId != null && zoneId.equals(localConfig.zoneId)) {
            logger.debug("Zone status change received for our zone: {} - {}", zoneId, status);
            // Refresh zone data to get updated run status
            refreshZone();
        }
    }

    protected void onBridgeStatusChanged(ThingStatus status, ThingStatusDetail detail) {
        if (status == ThingStatus.ONLINE) {
            refreshZone();
        } else {
            updateStatus(status, detail);
        }
    }

    @Override
    public void dispose() {
        // Cancel refresh job
        ScheduledFuture<?> localRefreshJob = refreshJob;
        if (localRefreshJob != null && !localRefreshJob.isCancelled()) {
            localRefreshJob.cancel(true);
        }

        Thing bridge = getBridge();
        if (bridge != null) {
            ThingHandler bridgeHandler = bridge.getHandler();
            if (bridgeHandler instanceof RachioBridgeHandler) {
                ((RachioBridgeHandler) bridgeHandler).unregisterZoneHandler(this);
                ((RachioBridgeHandler) bridgeHandler).unregisterStatusListener(this);
            }
        }

        super.dispose();
        logger.debug("Rachio zone handler disposed for thing {}", getThing().getUID());
    }
}
