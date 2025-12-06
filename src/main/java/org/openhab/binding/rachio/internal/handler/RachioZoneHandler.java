package org.openhab.binding.rachio.internal.handler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioActions;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.dto.CustomCrop;
import org.openhab.binding.rachio.internal.api.dto.CustomNozzle;
import org.openhab.binding.rachio.internal.api.dto.CustomShade;
import org.openhab.binding.rachio.internal.api.dto.CustomSlope;
import org.openhab.binding.rachio.internal.api.dto.CustomSoil;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
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
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for Rachio zones
 *
 * @author Dave Boyett - Initial contribution
 */
@Component(service = RachioHandler.class)
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler implements RachioHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);

    private @Nullable RachioZoneConfiguration config;
    private @Nullable RachioActions actions;
    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable ScheduledFuture<?> pollingJob;

    private @Nullable RachioZone zone;
    private @Nullable String deviceId;

    @Activate
    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = getConfigAs(RachioZoneConfiguration.class);
        
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Zone must be connected to a Rachio bridge");
            return;
        }
        
        bridgeHandler = (RachioBridgeHandler) bridge.getHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Cannot get bridge handler");
            return;
        }
        
        actions = bridgeHandler.getActions();
        if (actions == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Cannot get actions from bridge");
            return;
        }
        
        deviceId = config.deviceId;
        if (deviceId == null || deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Device ID is not configured");
            return;
        }
        
        // Create dynamic channels for professional data
        createDynamicChannels();
        
        // Start polling
        startPolling();
        
        updateStatus(ThingStatus.UNKNOWN);
        logger.debug("Rachio zone handler initialized for zone ID: {}", config.zoneId);
    }

    @Override
    public void dispose() {
        stopPolling();
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        RachioActions localActions = actions;
        RachioZoneConfiguration localConfig = config;
        String localDeviceId = deviceId;
        
        if (localActions == null || localConfig == null || localDeviceId == null) {
            logger.warn("Handler not properly initialized");
            return;
        }
        
        if (command instanceof RefreshType) {
            refreshZone();
            return;
        }
        
        String channelId = channelUID.getIdWithoutGroup();
        String zoneId = localConfig.zoneId;
        String thingId = getThing().getUID().getId();
        
        try {
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_ZONE_ENABLED:
                    if (command instanceof OnOffType) {
                        boolean enabled = command == OnOffType.ON;
                        localActions.setZoneEnabled(thingId, zoneId, enabled, localDeviceId);
                        // Refresh after enabling/disabling
                        scheduler.schedule(() -> refreshZone(), 2, TimeUnit.SECONDS);
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_ZONE_START:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        if (duration > 0) {
                            localActions.startZone(thingId, zoneId, duration, localDeviceId);
                            logger.debug("Starting zone {} for {} seconds", zoneId, duration);
                        }
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_ZONE_DURATION:
                    // This is a read-only channel for current duration
                    logger.debug("Zone duration is read-only, ignoring command");
                    break;
                    
                default:
                    logger.debug("Unhandled command for channel {}: {}", channelId, command);
            }
        } catch (Exception e) {
            logger.error("Error handling command for channel {}: {}", channelId, e.getMessage(), e);
        }
    }

    /**
     * Refresh zone data from API
     */
    private void refreshZone() {
        RachioActions localActions = actions;
        RachioZoneConfiguration localConfig = config;
        String localDeviceId = deviceId;
        
        if (localActions == null || localConfig == null || localDeviceId == null) {
            return;
        }
        
        try {
            // Get device to find zone
            var device = localActions.getDevice(localDeviceId);
            if (device != null && device.zones != null) {
                for (RachioZone z : device.zones) {
                    if (z.id != null && z.id.equals(localConfig.zoneId)) {
                        zone = z;
                        updateZoneState(z);
                        updateStatus(ThingStatus.ONLINE);
                        return;
                    }
                }
            }
            
            // Zone not found
            logger.warn("Zone {} not found in device {}", localConfig.zoneId, localDeviceId);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Zone not found in device");
            
        } catch (RachioApiException e) {
            logger.error("Error refreshing zone {}: {}", localConfig.zoneId, e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error communicating with Rachio API");
        }
    }

    /**
     * Update all channels with zone data
     */
    private void updateZoneState(RachioZone zone) {
        logger.debug("Updating zone state for zone: {}", zone.name);
        
        // Basic zone information
        updateState(RachioBindingConstants.CHANNEL_ZONE_NAME, new StringType(zone.name != null ? zone.name : ""));
        updateState(RachioBindingConstants.CHANNEL_ZONE_ENABLED, 
                zone.enabled != null && zone.enabled ? OnOffType.ON : OnOffType.OFF);
        updateState(RachioBindingConstants.CHANNEL_ZONE_NUMBER, new DecimalType(zone.zoneNumber != null ? zone.zoneNumber : 0));
        
        // Runtime information
        if (zone.runtime != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_DURATION, new DecimalType(zone.runtime));
        }
        
        // Run status
        if (zone.zoneRunStatus != null) {
            updateState(RachioBindingConstants.CHANNEL_ZONE_STATUS, 
                    new StringType(zone.zoneRunStatus.toString()));
            
            // Calculate remaining time if zone is running
            if (zone.zoneRunStatus == ZoneRunStatus.STARTED && zone.runtime != null && zone.startDate != null) {
                long elapsed = System.currentTimeMillis() - zone.startDate.toEpochMilli();
                long remaining = Math.max(0, (zone.runtime * 1000) - elapsed);
                updateState(RachioBindingConstants.CHANNEL_ZONE_REMAINING, 
                        new DecimalType(remaining / 1000));
                
                // Calculate end time
                if (remaining > 0) {
                    long endTime = System.currentTimeMillis() + remaining;
                    updateState(RachioBindingConstants.CHANNEL_ZONE_END_TIME, 
                            new DateTimeType(java.time.ZonedDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(endTime), 
                                    java.time.ZoneId.systemDefault())));
                }
            } else {
                updateState(RachioBindingConstants.CHANNEL_ZONE_REMAINING, new DecimalType(0));
                updateState(RachioBindingConstants.CHANNEL_ZONE_END_TIME, 
                        new DateTimeType(java.time.ZonedDateTime.now()));
            }
        }
        
        // Professional irrigation data - SOIL
        if (zone.customSoil != null) {
            CustomSoil soil = zone.customSoil;
            updateState("soilType", new StringType(soil.name != null ? soil.name : "Unknown"));
            if (soil.availableWater != null) {
                updateState("soilAvailableWater", 
                        new QuantityType<>(soil.availableWater, Units.INCH));
            }
        }
        
        // Professional irrigation data - CROP
        if (zone.customCrop != null) {
            CustomCrop crop = zone.customCrop;
            updateState("cropType", new StringType(crop.name != null ? crop.name : "Unknown"));
            if (crop.coefficient != null) {
                updateState("cropCoefficient", 
                        new DecimalType(crop.coefficient.doubleValue()));
            }
        }
        
        // Professional irrigation data - NOZZLE
        if (zone.customNozzle != null) {
            CustomNozzle nozzle = zone.customNozzle;
            updateState("nozzleType", new StringType(nozzle.name != null ? nozzle.name : "Unknown"));
            if (nozzle.inchesPerHour != null) {
                updateState("nozzleRate", 
                        new QuantityType<>(nozzle.inchesPerHour.doubleValue(), Units.INCH_PER_HOUR));
            }
        }
        
        // Professional irrigation data - SLOPE
        if (zone.customSlope != null) {
            CustomSlope slope = zone.customSlope;
            updateState("slopeType", new StringType(slope.name != null ? slope.name : "Unknown"));
        }
        
        // Professional irrigation data - SHADE
        if (zone.customShade != null) {
            CustomShade shade = zone.customShade;
            updateState("shadeType", new StringType(shade.name != null ? shade.name : "Unknown"));
        }
        
        // Professional irrigation data - EFFICIENCY
        if (zone.efficiency != null) {
            updateState("irrigationEfficiency", 
                    new QuantityType<>(zone.efficiency.doubleValue() * 100, Units.PERCENT));
        }
        
        // Professional irrigation data - ROOT DEPTH
        if (zone.rootZoneDepth != null) {
            updateState("rootDepth", 
                    new QuantityType<>(zone.rootZoneDepth.doubleValue(), Units.INCH));
        }
        
        // Professional irrigation data - AREA
        if (zone.yardAreaSquareFeet != null) {
            updateState("zoneArea", 
                    new QuantityType<>(zone.yardAreaSquareFeet.doubleValue(), Units.SQUARE_FOOT));
        }
        
        // Watering adjustments
        if (zone.wateringAdjustmentRuntimes != null && zone.wateringAdjustmentRuntimes.length == 5) {
            for (int i = 0; i < zone.wateringAdjustmentRuntimes.length; i++) {
                updateState("adjustmentLevel" + (i + 1), 
                        new DecimalType(zone.wateringAdjustmentRuntimes[i]));
            }
        }
        
        // Last updated timestamp
        updateState("lastUpdated", 
                new DateTimeType(java.time.ZonedDateTime.now()));
    }

    /**
     * Create dynamic channels for professional irrigation data
     */
    private void createDynamicChannels() {
        List<Channel> channels = new ArrayList<>(getThing().getChannels());
        boolean channelsModified = false;
        
        // Check if professional channels already exist
        boolean hasSoilChannel = channels.stream().anyMatch(c -> "soilType".equals(c.getUID().getIdWithoutGroup()));
        boolean hasCropChannel = channels.stream().anyMatch(c -> "cropCoefficient".equals(c.getUID().getIdWithoutGroup()));
        boolean hasNozzleChannel = channels.stream().anyMatch(c -> "nozzleRate".equals(c.getUID().getIdWithoutGroup()));
        
        // Add professional data channels if they don't exist
        if (!hasSoilChannel) {
            channels.add(createSoilChannels());
            channelsModified = true;
        }
        
        if (!hasCropChannel) {
            channels.add(createCropChannels());
            channelsModified = true;
        }
        
        if (!hasNozzleChannel) {
            channels.add(createNozzleChannels());
            channelsModified = true;
        }
        
        // Add other professional channels
        channels.addAll(createOtherProfessionalChannels());
        channelsModified = true;
        
        if (channelsModified) {
            updateThing(editThing().withChannels(channels).build());
            logger.debug("Added professional data channels to zone {}", getThing().getUID());
        }
    }

    /**
     * Create soil-related channels
     */
    private Channel createSoilChannels() {
        ChannelBuilder builder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "soilType"), "String")
                .withType(new ChannelTypeUID("rachio", "soil-type"))
                .withLabel("Soil Type")
                .withDescription("Soil type for this zone")
                .withProperties(Map.of("category", "irrigation", "tags", "Soil,Irrigation"));
        return builder.build();
    }

    /**
     * Create crop-related channels
     */
    private Channel createCropChannels() {
        ChannelBuilder builder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "cropCoefficient"), "Number")
                .withType(new ChannelTypeUID("rachio", "crop-coefficient"))
                .withLabel("Crop Coefficient")
                .withDescription("Crop coefficient (Kc) - typically 0.4 to 1.2")
                .withProperties(Map.of("category", "irrigation", "tags", "Crop,Irrigation"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0.0))
                        .withMaximum(BigDecimal.valueOf(1.5))
                        .withStep(BigDecimal.valueOf(0.1))
                        .withPattern("%.2f")
                        .build()
                        .toStateDescription());
        return builder.build();
    }

    /**
     * Create nozzle-related channels
     */
    private Channel createNozzleChannels() {
        ChannelBuilder builder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "nozzleRate"), "Number:Length")
                .withType(new ChannelTypeUID("rachio", "nozzle-rate"))
                .withLabel("Nozzle Rate")
                .withDescription("Nozzle application rate in inches per hour")
                .withProperties(Map.of("category", "irrigation", "tags", "Nozzle,Irrigation"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0.0))
                        .withMaximum(BigDecimal.valueOf(5.0))
                        .withStep(BigDecimal.valueOf(0.1))
                        .withPattern("%.2f %unit%")
                        .build()
                        .toStateDescription());
        return builder.build();
    }

    /**
     * Create other professional irrigation channels
     */
    private List<Channel> createOtherProfessionalChannels() {
        List<Channel> channels = new ArrayList<>();
        
        // Efficiency channel
        ChannelBuilder efficiencyBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "irrigationEfficiency"), "Number:Dimensionless")
                .withType(new ChannelTypeUID("rachio", "efficiency"))
                .withLabel("Irrigation Efficiency")
                .withDescription("Irrigation efficiency percentage")
                .withProperties(Map.of("category", "irrigation", "tags", "Efficiency,Irrigation"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0))
                        .withMaximum(BigDecimal.valueOf(100))
                        .withStep(BigDecimal.valueOf(1))
                        .withPattern("%d %%")
                        .build()
                        .toStateDescription());
        channels.add(efficiencyBuilder.build());
        
        // Root depth channel
        ChannelBuilder rootDepthBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "rootDepth"), "Number:Length")
                .withType(new ChannelTypeUID("rachio", "root-depth"))
                .withLabel("Root Zone Depth")
                .withDescription("Root zone depth in inches")
                .withProperties(Map.of("category", "irrigation", "tags", "Root,Irrigation"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0))
                        .withMaximum(BigDecimal.valueOf(48))
                        .withStep(BigDecimal.valueOf(1))
                        .withPattern("%d %unit%")
                        .build()
                        .toStateDescription());
        channels.add(rootDepthBuilder.build());
        
        // Area channel
        ChannelBuilder areaBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "zoneArea"), "Number:Area")
                .withType(new ChannelTypeUID("rachio", "zone-area"))
                .withLabel("Zone Area")
                .withDescription("Zone area in square feet")
                .withProperties(Map.of("category", "irrigation", "tags", "Area,Irrigation"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0))
                        .withMaximum(BigDecimal.valueOf(10000))
                        .withStep(BigDecimal.valueOf(10))
                        .withPattern("%d %unit%")
                        .build()
                        .toStateDescription());
        channels.add(areaBuilder.build());
        
        // Additional soil info
        ChannelBuilder soilWaterBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "soilAvailableWater"), "Number:Length")
                .withType(new ChannelTypeUID("rachio", "available-water"))
                .withLabel("Available Water")
                .withDescription("Available water in inches per foot")
                .withProperties(Map.of("category", "irrigation", "tags", "Soil,Water,Irrigation"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0.0))
                        .withMaximum(BigDecimal.valueOf(2.0))
                        .withStep(BigDecimal.valueOf(0.1))
                        .withPattern("%.2f %unit%")
                        .build()
                        .toStateDescription());
        channels.add(soilWaterBuilder.build());
        
        // Crop type channel
        ChannelBuilder cropTypeBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "cropType"), "String")
                .withType(new ChannelTypeUID("rachio", "crop-type"))
                .withLabel("Crop Type")
                .withDescription("Type of crop/plant")
                .withProperties(Map.of("category", "irrigation", "tags", "Crop,Irrigation"));
        channels.add(cropTypeBuilder.build());
        
        // Nozzle type channel
        ChannelBuilder nozzleTypeBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "nozzleType"), "String")
                .withType(new ChannelTypeUID("rachio", "nozzle-type"))
                .withLabel("Nozzle Type")
                .withDescription("Type of nozzle/sprinkler")
                .withProperties(Map.of("category", "irrigation", "tags", "Nozzle,Irrigation"));
        channels.add(nozzleTypeBuilder.build());
        
        // Slope type channel
        ChannelBuilder slopeTypeBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "slopeType"), "String")
                .withType(new ChannelTypeUID("rachio", "slope-type"))
                .withLabel("Slope Type")
                .withDescription("Slope classification")
                .withProperties(Map.of("category", "irrigation", "tags", "Slope,Irrigation"));
        channels.add(slopeTypeBuilder.build());
        
        // Shade type channel
        ChannelBuilder shadeTypeBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "shadeType"), "String")
                .withType(new ChannelTypeUID("rachio", "shade-type"))
                .withLabel("Shade Type")
                .withDescription("Shade classification")
                .withProperties(Map.of("category", "irrigation", "tags", "Shade,Irrigation"));
        channels.add(shadeTypeBuilder.build());
        
        // Watering adjustment channels
        for (int i = 1; i <= 5; i++) {
            ChannelBuilder adjBuilder = ChannelBuilder
                    .create(new ChannelUID(getThing().getUID(), "adjustmentLevel" + i), "Number")
                    .withType(new ChannelTypeUID("rachio", "adjustment-level"))
                    .withLabel("Adjustment Level " + i)
                    .withDescription("Watering adjustment runtime multiplier for level " + i)
                    .withProperties(Map.of("category", "irrigation", "tags", "Adjustment,Irrigation"))
                    .withStateDescription(StateDescriptionFragmentBuilder.create()
                            .withMinimum(BigDecimal.valueOf(0.0))
                            .withMaximum(BigDecimal.valueOf(3.0))
                            .withStep(BigDecimal.valueOf(0.1))
                            .withPattern("%.1f")
                            .build()
                            .toStateDescription());
            channels.add(adjBuilder.build());
        }
        
        // Last updated timestamp
        ChannelBuilder lastUpdatedBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "lastUpdated"), "DateTime")
                .withType(new ChannelTypeUID("rachio", "last-updated"))
                .withLabel("Last Updated")
                .withDescription("When zone data was last refreshed")
                .withProperties(Map.of("category", "status", "tags", "Status,Timestamp"));
        channels.add(lastUpdatedBuilder.build());
        
        return channels;
    }

    /**
     * Start polling for zone updates
     */
    private void startPolling() {
        stopPolling(); // Ensure no existing polling job
        
        pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshZone();
            } catch (Exception e) {
                logger.error("Error during zone polling: {}", e.getMessage(), e);
            }
        }, 0, 60, TimeUnit.SECONDS); // Poll every 60 seconds
        
        logger.debug("Started polling for zone {}", getThing().getUID());
    }

    /**
     * Stop polling
     */
    private void stopPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null) {
            localPollingJob.cancel(true);
            pollingJob = null;
            logger.debug("Stopped polling for zone {}", getThing().getUID());
        }
    }

    /**
     * Update zone from webhook event
     */
    public void updateFromWebhook(RachioZone updatedZone) {
        if (updatedZone != null && config != null && 
            updatedZone.id != null && updatedZone.id.equals(config.zoneId)) {
            zone = updatedZone;
            updateZoneState(updatedZone);
            updateStatus(ThingStatus.ONLINE);
            
            logger.debug("Zone {} updated from webhook", config.zoneId);
        }
    }

    /**
     * Get current zone data
     */
    public @Nullable RachioZone getZone() {
        return zone;
    }

    /**
     * Get zone configuration
     */
    public @Nullable RachioZoneConfiguration getZoneConfig() {
        return config;
    }
}
