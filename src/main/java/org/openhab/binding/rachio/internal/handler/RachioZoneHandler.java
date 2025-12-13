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
                        Integer duration = localConfig.getDefaultDuration() != null ? 
                            localConfig.getDefaultDuration() : DEFAULT_ZONE_DURATION;
                        localApiClient.startZone(zoneId, duration);
                        scheduler.execute(this::pollStatus);
                    }
                    break;

                case CHANNEL_ZONE_STOP:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        // Stop watering typically stops all zones on the device.
                        // We need the device ID. This highlights a design gap.
                        // For now, we'll assume a method to stop a specific zone or all watering.
                        logger.warn("Single zone stop command may require device context. Implementation needed.");
                        // localApiClient.stopWatering(deviceId); // Would need deviceId
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
                            localApiClient.startZone(zoneId, minutes);
                            scheduler.execute(this::pollStatus);
                        }
                    } else if (command instanceof QuantityType) {
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int minutes = quantity.toUnit(Units.MINUTE).intValue();
                        if (minutes > 0) {
                            localApiClient.startZone(zoneId, minutes);
                            scheduler.execute(this::pollStatus);
                        }
                    }
                    break;

                case CHANNEL_ZONE_RUNTIME:
                    if (command instanceof DecimalType) {
                        int seconds = ((DecimalType) command).intValue();
                        if (seconds > 0) {
                            localApiClient.startZone(zoneId, seconds / 60); // Convert seconds to minutes
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
        
        // CRITICAL ISSUE: The RachioApiClient you created does NOT have a getZone() method.
        // Zone data is typically fetched as part of a device. You have two options:
        // 1. Add a getZone() method to RachioApiClient (requires API endpoint).
        // 2. Fetch the parent device and find the zone within it.
        // For now, this will fail. We are leaving the logic structure but you must implement it.
        
        try {
            // TODO: Implement zone fetching logic.
            // Option 1 (Preferred if API supports): RachioZone zone = localApiClient.getZone(localConfig.getZoneId());
            // Option 2: RachioDevice device = localApiClient.getDevice(deviceId); then find zone in device.getZones()
            logger.error("pollStatus() not fully implemented: Zone data fetching logic is missing in ApiClient.");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone polling not implemented");
            
            // Placeholder for successful update (remove this block when implemented)
            // if (zone != null) {
            //     updateZone(zone);
            //     updateStatus(ThingStatus.ONLINE);
            // } else {
            //     updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone not found");
            // }
            
        } catch (Exception e) {
            logger.error("Error polling zone status", e);
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
        // ... (Keep the existing implementation from your original file for updateZonePropertyChannels)
        // This method can remain largely unchanged as it only deals with the RachioZone DTO object.
        // Ensure you copy the entire method from your original file here.
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
