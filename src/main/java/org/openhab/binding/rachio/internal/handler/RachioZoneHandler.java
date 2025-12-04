package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands for zone things
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite for OpenHAB 5.x
 */
@Component(service = RachioZoneHandler.class, configurationPid = "handler.rachiozone")
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler implements RachioStatusListener {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);

    // Configuration
    private @Nullable RachioZoneConfiguration config;

    // API Client
    private final RachioHttp rachioHttp;

    // Bridge Handler
    private @Nullable RachioBridgeHandler bridgeHandler;

    // Scheduling
    private @Nullable ScheduledFuture<?> pollingJob;
    private static final int POLLING_INTERVAL = 60; // seconds

    // State tracking
    private @Nullable RachioZone lastZoneState;
    private @Nullable ZonedDateTime lastUpdate;

    @Activate
    public RachioZoneHandler(@Reference RachioHttp rachioHttp, Thing thing) {
        super(thing);
        this.rachioHttp = rachioHttp;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler for {}", getThing().getUID());

        config = getConfigAs(RachioZoneConfiguration.class);
        if (config == null || config.deviceId.isEmpty() || config.zoneId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Device ID or Zone ID not configured");
            return;
        }

        // Get bridge handler
        RachioBridgeHandler bridge = getBridgeHandler();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not found");
            return;
        }

        // Create dynamic channels for professional data
        createProfessionalDataChannels();

        // Start polling
        startPolling();

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        stopPolling();
        super.dispose();
    }

    /**
     * Get the bridge handler
     */
    private @Nullable RachioBridgeHandler getBridgeHandler() {
        if (bridgeHandler == null) {
            org.openhab.core.thing.Bridge bridge = getBridge();
            if (bridge != null) {
                bridgeHandler = (RachioBridgeHandler) bridge.getHandler();
            }
        }
        return bridgeHandler;
    }

    /**
     * Get API key from bridge
     */
    private @Nullable String getApiKey() {
        RachioBridgeHandler bridge = getBridgeHandler();
        return bridge != null ? bridge.getApiKey() : null;
    }

    /**
     * Create dynamic channels for professional irrigation data
     */
    private void createProfessionalDataChannels() {
        logger.debug("Creating professional data channels for zone {}", config != null ? config.zoneId : "unknown");

        // Soil Data Channels
        createReadOnlyChannel(CHANNEL_ZONE_SOIL_TYPE, CHANNEL_TYPE_UID_STRING,
                "Soil Type", "Soil composition type", "text");

        // Crop Data Channels
        createReadOnlyChannel(CHANNEL_ZONE_CROP_TYPE, CHANNEL_TYPE_UID_STRING,
                "Crop Type", "Type of vegetation/grass", "text");
        createReadOnlyChannel(CHANNEL_ZONE_CROP_COEFFICIENT, CHANNEL_TYPE_UID_NUMBER,
                "Crop Coefficient", "Evapotranspiration coefficient (0-1)", "number:dimensionless");

        // Nozzle Data Channels
        createReadOnlyChannel(CHANNEL_ZONE_NOZZLE_TYPE, CHANNEL_TYPE_UID_STRING,
                "Nozzle Type", "Sprinkler nozzle model", "text");
        createReadOnlyChannel(CHANNEL_ZONE_NOZZLE_RATE, CHANNEL_TYPE_UID_NUMBER,
                "Nozzle Rate", "Application rate (inches/hour)", "number:velocity");

        // Slope and Shade Channels
        createReadOnlyChannel(CHANNEL_ZONE_SLOPE_TYPE, CHANNEL_TYPE_UID_STRING,
                "Slope", "Ground slope category", "text");
        createReadOnlyChannel(CHANNEL_ZONE_SHADE_TYPE, CHANNEL_TYPE_UID_STRING,
                "Shade", "Shade coverage category", "text");

        // Numeric Professional Data Channels
        createReadOnlyChannel(CHANNEL_ZONE_ROOT_DEPTH, CHANNEL_TYPE_UID_NUMBER,
                "Root Depth", "Plant root zone depth", "number:length");
        createReadOnlyChannel(CHANNEL_ZONE_EFFICIENCY, CHANNEL_TYPE_UID_NUMBER,
                "Efficiency", "Irrigation system efficiency", "number:dimensionless");
        createReadOnlyChannel(CHANNEL_ZONE_AREA, CHANNEL_TYPE_UID_NUMBER,
                "Area", "Zone area", "number:area");

        // Water Adjustment Runtimes (levels 1-5)
        for (int i = 1; i <= 5; i++) {
            String channelId = String.format(CHANNEL_ZONE_WATER_ADJUSTMENT, i);
            createReadOnlyChannel(channelId, CHANNEL_TYPE_UID_NUMBER,
                    String.format("Water Adjustment %d", i),
                    String.format("Watering adjustment runtime level %d", i),
                    "number:time");
        }

        logger.debug("Professional data channels created for zone {}", config != null ? config.zoneId : "unknown");
    }

    /**
     * Create a read-only channel
     */
    private void createReadOnlyChannel(String channelId, ChannelTypeUID channelTypeUID,
            String label, String description, String itemType) {
        ChannelUID uid = new ChannelUID(getThing().getUID(), channelId);

        // Check if channel already exists
        if (getThing().getChannel(uid) != null) {
            return;
        }

        Channel channel = ChannelBuilder.create(uid, itemType)
                .withType(channelTypeUID)
                .withLabel(label)
                .withDescription(description)
                .build();

        updateThing(editThing().withChannel(channel).build());
        logger.trace("Created channel {} for zone {}", channelId, config != null ? config.zoneId : "unknown");
    }

    /**
     * Start polling for zone updates
     */
    private void startPolling() {
        ScheduledFuture<?> job = pollingJob;
        if (job == null || job.isCancelled()) {
            pollingJob = scheduler.scheduleWithFixedDelay(this::pollZoneState, 10, POLLING_INTERVAL, TimeUnit.SECONDS);
            logger.debug("Started polling for zone {}", config != null ? config.zoneId : "unknown");
        }
    }

    /**
     * Stop polling
     */
    private void stopPolling() {
        ScheduledFuture<?> job = pollingJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            pollingJob = null;
            logger.debug("Stopped polling for zone {}", config != null ? config.zoneId : "unknown");
        }
    }

    /**
     * Poll zone state from Rachio API
     */
    private void pollZoneState() {
        try {
            RachioZoneConfiguration localConfig = config;
            String apiKey = getApiKey();

            if (localConfig == null || apiKey == null || apiKey.isEmpty()) {
                logger.debug("Cannot poll zone state: configuration missing");
                return;
            }

            // Get zones for device
            List<RachioZone> zones = rachioHttp.getZones(apiKey, localConfig.deviceId);
            RachioZone zone = zones.stream()
                    .filter(z -> z.getId().equals(localConfig.zoneId))
                    .findFirst()
                    .orElse(null);

            if (zone != null) {
                updateZoneState(zone);
                lastZoneState = zone;
                lastUpdate = ZonedDateTime.now();
            } else {
                logger.warn("Zone {} not found in device {}", localConfig.zoneId, localConfig.deviceId);
            }
        } catch (Exception e) {
            logger.debug("Error polling zone state: {}", e.getMessage(), e);
        }
    }

    /**
     * Update all zone channels with current state
     */
    protected void updateZoneState(RachioZone zone) {
        logger.trace("Updating zone state for {}: {}", zone.getId(), zone.getName());

        // Basic zone information
        updateState(CHANNEL_ZONE_NAME, new StringType(zone.getName()));
        updateState(CHANNEL_ZONE_ENABLED, zone.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_ZONE_RUNTIME, new QuantityType<>(zone.getDuration(), Units.SECOND));

        // Zone status
        ZoneRunStatus status = zone.getStatus();
        if (status != null) {
            updateState(CHANNEL_ZONE_STATUS, new StringType(status.name()));
        }

        // Last run information
        if (zone.getLastWateredDate() != null) {
            updateState(CHANNEL_ZONE_LAST_RUN, new DateTimeType(zone.getLastWateredDate()));
        }

        // Update professional irrigation data
        updateProfessionalData(zone);

        // Update water budget if available
        if (zone.getWaterBudget() != null) {
            updateState(CHANNEL_ZONE_WATER_BUDGET, new QuantityType<>(zone.getWaterBudget(), Units.PERCENT));
        }

        logger.debug("Zone {} state updated: name={}, enabled={}, status={}",
                zone.getId(), zone.getName(), zone.isEnabled(), status);
    }

    /**
     * Update professional irrigation data channels
     */
    private void updateProfessionalData(RachioZone zone) {
        logger.trace("Updating professional data for zone {}", zone.getId());

        // Soil data
        if (zone.getCustomSoil() != null) {
            updateState(CHANNEL_ZONE_SOIL_TYPE, new StringType(zone.getCustomSoil().getName()));
        }

        // Crop data
        if (zone.getCustomCrop() != null) {
            updateState(CHANNEL_ZONE_CROP_TYPE, new StringType(zone.getCustomCrop().getName()));
            if (zone.getCustomCrop().getCoefficient() != null) {
                updateState(CHANNEL_ZONE_CROP_COEFFICIENT,
                        new DecimalType(zone.getCustomCrop().getCoefficient()));
            }
        }

        // Nozzle data
        if (zone.getCustomNozzle() != null) {
            updateState(CHANNEL_ZONE_NOZZLE_TYPE, new StringType(zone.getCustomNozzle().getName()));
            if (zone.getCustomNozzle().getInchesPerHour() != null) {
                updateState(CHANNEL_ZONE_NOZZLE_RATE,
                        new DecimalType(zone.getCustomNozzle().getInchesPerHour()));
            }
        }

        // Slope and shade
        if (zone.getCustomSlope() != null) {
            updateState(CHANNEL_ZONE_SLOPE_TYPE, new StringType(zone.getCustomSlope().getName()));
        }
        if (zone.getCustomShade() != null) {
            updateState(CHANNEL_ZONE_SHADE_TYPE, new StringType(zone.getCustomShade().getName()));
        }

        // Numeric professional data
        if (zone.getRootZoneDepth() != null) {
            updateState(CHANNEL_ZONE_ROOT_DEPTH,
                    new QuantityType<>(zone.getRootZoneDepth(), ImperialUnits.INCH));
        }

        if (zone.getEfficiency() != null) {
            updateState(CHANNEL_ZONE_EFFICIENCY,
                    new QuantityType<>(zone.getEfficiency(), Units.PERCENT));
        }

        if (zone.getYardAreaSquareFeet() != null) {
            updateState(CHANNEL_ZONE_AREA,
                    new QuantityType<>(zone.getYardAreaSquareFeet(), ImperialUnits.SQUARE_FOOT));
        }

        // Water adjustment runtimes
        if (zone.getWateringAdjustmentRuntimes() != null) {
            Integer[] adjustments = zone.getWateringAdjustmentRuntimes();
            for (int i = 0; i < Math.min(adjustments.length, 5); i++) {
                String channelId = String.format(CHANNEL_ZONE_WATER_ADJUSTMENT, i + 1);
                updateState(channelId, new QuantityType<>(adjustments[i], Units.SECOND));
            }
        }

        // Available water if present
        if (zone.getAvailableWater() != null) {
            updateState(CHANNEL_ZONE_AVAILABLE_WATER,
                    new QuantityType<>(zone.getAvailableWater(), ImperialUnits.INCH));
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        RachioZoneConfiguration localConfig = config;
        String apiKey = getApiKey();

        if (localConfig == null || apiKey == null || apiKey.isEmpty()) {
            logger.warn("Cannot handle command: configuration missing");
            return;
        }

        if (command instanceof RefreshType) {
            // Refresh zone state
            scheduler.execute(this::pollZoneState);
            return;
        }

        String channelId = channelUID.getIdWithoutGroup();
        logger.debug("Handling command {} for channel {} on zone {}", command, channelId, localConfig.zoneId);

        try {
            switch (channelId) {
                case CHANNEL_ZONE_ENABLED:
                    if (command instanceof OnOffType) {
                        boolean enabled = command == OnOffType.ON;
                        rachioHttp.setZoneEnabled(getThing().getUID().getId(), localConfig.zoneId,
                                enabled, localConfig.deviceId, apiKey);
                        // Update local state
                        if (lastZoneState != null) {
                            lastZoneState.setEnabled(enabled);
                            updateZoneState(lastZoneState);
                        }
                    }
                    break;

                case CHANNEL_ZONE_START:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        rachioHttp.startZone(getThing().getUID().getId(), localConfig.zoneId,
                                duration, localConfig.deviceId, apiKey);
                    }
                    break;

                case CHANNEL_ZONE_STOP:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        rachioHttp.stopWatering(getThing().getUID().getId(), localConfig.deviceId, apiKey);
                    }
                    break;

                case CHANNEL_ZONE_RUNTIME:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        // This would typically update the default runtime configuration
                        // For now, just log it
                        logger.debug("Zone {} runtime set to {} seconds", localConfig.zoneId, duration);
                    }
                    break;

                default:
                    logger.debug("Unhandled command for channel {}", channelId);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error handling command {} for channel {}: {}", command, channelId, e.getMessage(), e);
        }
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        super.handleConfigurationUpdate(configurationParameters);
        // Re-create channels if configuration changed
        createProfessionalDataChannels();
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.emptyList();
    }

    @Override
    public void onStatusChanged(ThingStatus status) {
        // Bridge status changed
        if (status == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
            startPolling();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            stopPolling();
        }
    }

    @Override
    public void onZoneStateUpdated(RachioZone zone) {
        // Called when webhook or bridge updates zone state
        if (config != null && zone.getId().equals(config.zoneId)) {
            updateZoneState(zone);
            lastZoneState = zone;
            lastUpdate = ZonedDateTime.now();
        }
    }

    @Override
    public void onDeviceStateUpdated() {
        // Refresh zone state when device updates
        scheduler.execute(this::pollZoneState);
    }

    /**
     * Get the current zone state
     */
    public @Nullable RachioZone getZoneState() {
        return lastZoneState;
    }

    /**
     * Get last update time
     */
    public @Nullable ZonedDateTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Get zone configuration
     */
    public @Nullable RachioZoneConfiguration getZoneConfig() {
        return config;
    }
}
