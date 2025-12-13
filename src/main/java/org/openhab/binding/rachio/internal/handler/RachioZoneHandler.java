package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiClient;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.dto.CustomCrop;
import org.openhab.binding.rachio.internal.api.dto.CustomNozzle;
import org.openhab.binding.rachio.internal.api.dto.CustomSoil;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands for zone channels
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends RachioHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);
    private @Nullable RachioZoneConfiguration config;
    private @Nullable RachioApiClient apiClient;
    private @Nullable ScheduledFuture<?> pollingJob;

    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler for {}", getThing().getUID());
        config = getConfigAs(RachioZoneConfiguration.class);
        RachioBridgeHandler bridgeHandler = getBridgeHandler();

        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No bridge available");
            return;
        }

        this.apiClient = bridgeHandler.getApiClient();
        if (this.apiClient == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge API client not ready");
            return;
        }

        updateStatus(ThingStatus.UNKNOWN);
        // Perform initial poll
        scheduler.execute(this::pollStatus);
        // Start regular polling (e.g., every 5 minutes)
        pollingJob = scheduler.scheduleWithFixedDelay(this::pollStatus, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio zone handler for {}", getThing().getUID());
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null && !localPollingJob.isCancelled()) {
            localPollingJob.cancel(true);
            pollingJob = null;
        }
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Handling command {} for channel {}", command, channelUID);
        if (command instanceof RefreshType) {
            scheduler.execute(this::pollStatus);
            return;
        }

        RachioZoneConfiguration localConfig = config;
        RachioApiClient localApiClient = apiClient;

        if (localConfig == null || localConfig.getZoneId() == null) {
            logger.warn("Zone configuration not available");
            return;
        }
        if (localApiClient == null) {
            logger.warn("API client not available");
            return;
        }

        String channelId = channelUID.getIdWithoutGroup();
        String zoneId = localConfig.getZoneId();

        try {
            switch (channelId) {
                case CHANNEL_ZONE_START:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        // Use configured default duration or fallback
                        Integer durationMinutes = (localConfig.getDefaultDuration() != null) ? 
                            localConfig.getDefaultDuration() : DEFAULT_ZONE_DURATION;
                        // Call the new method that accepts minutes
                        localApiClient.startZoneMinutes(zoneId, durationMinutes);
                        scheduler.execute(this::pollStatus);
                    } else if (command instanceof QuantityType) {
                        // Handle QuantityType command - supports minutes or seconds via unit
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        if (quantity.getUnit() == Units.SECOND) {
                            int seconds = quantity.intValue();
                            localApiClient.startZoneSeconds(zoneId, seconds);
                        } else {
                            // Default to minutes if no unit or different unit specified
                            int minutes = quantity.toUnit(Units.MINUTE).intValue();
                            localApiClient.startZoneMinutes(zoneId, minutes);
                        }
                        scheduler.execute(this::pollStatus);
                    } else if (command instanceof StringType) {
                        // Handle string duration like "10m30s" using the new flexible method
                        String durationStr = ((StringType) command).toString();
                        localApiClient.startZone(zoneId, durationStr);
                        scheduler.execute(this::pollStatus);
                    }
                    break;

                case CHANNEL_ZONE_STOP:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        // To stop a specific zone, we need its device context.
                        // For now, this is a placeholder. You might need to fetch the parent device ID.
                        logger.warn("Single zone stop command not fully implemented. Requires parent device ID.");
                        // Example future implementation:
                        // String deviceId = getParentDeviceId(); // You need to implement this
                        // localApiClient.stopWatering(deviceId);
                    }
                    break;

                case CHANNEL_ZONE_ENABLED:
                    if (command instanceof OnOffType) {
                        boolean enabled = command == OnOffType.ON;
                        localApiClient.setZoneEnabled(zoneId, enabled);
                        scheduler.execute(this::pollStatus);
                    }
                    break;

                case CHANNEL_ZONE_DURATION:
                    if (command instanceof DecimalType) {
                        int minutes = ((DecimalType) command).intValue();
                        if (minutes > 0) {
                            localApiClient.startZoneMinutes(zoneId, minutes);
                            scheduler.execute(this::pollStatus);
                        }
                    } else if (command instanceof QuantityType) {
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int minutes = quantity.toUnit(Units.MINUTE).intValue();
                        if (minutes > 0) {
                            localApiClient.startZoneMinutes(zoneId, minutes);
                            scheduler.execute(this::pollStatus);
                        }
                    } else if (command instanceof StringType) {
                        // Also support string format on the duration channel
                        String durationStr = ((StringType) command).toString();
                        localApiClient.startZone(zoneId, durationStr);
                        scheduler.execute(this::pollStatus);
                    }
                    break;

                case CHANNEL_ZONE_RUNTIME:
                    if (command instanceof DecimalType) {
                        int seconds = ((DecimalType) command).intValue();
                        if (seconds > 0) {
                            localApiClient.startZoneSeconds(zoneId, seconds);
                            scheduler.execute(this::pollStatus);
                        }
                    }
                    break;

                default:
                    logger.debug("Unhandled command for channel {}", channelId);
            }
        } catch (Exception e) {
            logger.error("Error handling command {} for channel {}", command, channelId, e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Implementation of abstract method from RachioHandler
     */
    @Override
    protected void pollStatus() {
        RachioZoneConfiguration localConfig = config;
        RachioApiClient localApiClient = apiClient;

        if (localConfig == null || localConfig.getZoneId() == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Zone configuration not available");
            return;
        }
        if (localApiClient == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "API client not available");
            return;
        }

        try {
            // FIXED: Now uses the getZone() method from the patched RachioApiClient
            RachioZone zone = localApiClient.getZone(localConfig.getZoneId());
            if (zone != null) {
                updateZone(zone);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone not found via API");
            }
        } catch (RachioApiException e) {
            logger.error("Error polling zone status via API", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Update zone with new data
     * @param zone zone data
     */
    protected void updateZone(RachioZone zone) {
        updateProperties(zone);
        updateChannels(zone);
    }

    /**
     * Update thing properties from zone data
     * @param zone zone data
     */
    protected void updateProperties(RachioZone zone) {
        Map<String, String> properties = new HashMap<>();
        properties.put(PROPERTY_ID, zone.getId());
        properties.put(PROPERTY_NAME, zone.getName());
        properties.put("zoneNumber", String.valueOf(zone.getZoneNumber()));
        properties.put("enabled", String.valueOf(zone.isEnabled()));
        properties.put("runtime", String.valueOf(zone.getRuntime()));

        CustomSoil soil = zone.getSoil();
        if (soil != null && soil.getType() != null) {
            properties.put(PROPERTY_SOIL_TYPE, soil.getType());
        }
        CustomCrop crop = zone.getCrop();
        if (crop != null && crop.getType() != null) {
            properties.put(PROPERTY_CROP_TYPE, crop.getType());
        }
        CustomNozzle nozzle = zone.getNozzle();
        if (nozzle != null && nozzle.getType() != null) {
            properties.put(PROPERTY_NOZZLE_TYPE, nozzle.getType());
        }
        updateProperties(properties);
    }

    /**
     * Update channels from zone data
     * @param zone zone data
     */
    protected void updateChannels(RachioZone zone) {
        // Basic zone status
        updateState(CHANNEL_ZONE_ENABLED, zone.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_ZONE_RUNTIME, new DecimalType(zone.getRuntime()));
        // Zone property channels
        updateZonePropertyChannels(zone);
        // Note: Rate limit channels are no longer updated here.
    }

    /**
     * Update professional zone property channels
     * @param zone zone data
     */
    private void updateZonePropertyChannels(RachioZone zone) {
        CustomSoil soil = zone.getSoil();
        if (soil != null) {
            if (soil.getType() != null) {
                // For string channels, you might need to create a StringType channel
                // For now, using DecimalType as placeholder
                updateState(CHANNEL_SOIL_TYPE, new DecimalType(0));
            }
            if (soil.getAvailableWater() != null) {
                updateState(CHANNEL_SOIL_AVAILABLE_WATER, new DecimalType(soil.getAvailableWater()));
            }
        }

        CustomCrop crop = zone.getCrop();
        if (crop != null) {
            if (crop.getType() != null) {
                updateState(CHANNEL_CROP_TYPE, new DecimalType(0));
            }
            if (crop.getCoefficient() != null) {
                updateState(CHANNEL_CROP_COEFFICIENT, new DecimalType(crop.getCoefficient()));
            }
        }

        CustomNozzle nozzle = zone.getNozzle();
        if (nozzle != null) {
            if (nozzle.getType() != null) {
                updateState(CHANNEL_NOZZLE_TYPE, new DecimalType(0));
            }
            if (nozzle.getRate() != null) {
                updateState(CHANNEL_NOZZLE_RATE, new DecimalType(nozzle.getRate()));
            }
        }

        // Additional properties
        if (zone.getRootDepth() != null) {
            updateState(CHANNEL_ROOT_DEPTH, new DecimalType(zone.getRootDepth()));
        }

        if (zone.getEfficiency() != null) {
            updateState(CHANNEL_IRRIGATION_EFFICIENCY, new DecimalType(zone.getEfficiency()));
        }

        if (zone.getArea() != null) {
            updateState(CHANNEL_ZONE_AREA, new DecimalType(zone.getArea()));
        }
    }

    /**
     * Get the bridge handler
     */
    private @Nullable RachioBridgeHandler getBridgeHandler() {
        Thing bridge = getBridge();
        if (bridge != null && bridge.getHandler() instanceof RachioBridgeHandler) {
            return (RachioBridgeHandler) bridge.getHandler();
        }
        return null;
    }
    // ===== NOTE: RachioStatusListener methods have been REMOVED =====
}
