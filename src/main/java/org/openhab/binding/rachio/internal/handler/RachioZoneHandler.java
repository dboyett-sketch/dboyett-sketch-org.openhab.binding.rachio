package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
 *
 * @author Damien Boyett - Initial contribution
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
    public void initialize() {
        logger.debug("Initializing Rachio zone handler for {}", getThing().getUID());
        
        config = getConfigAs(RachioZoneConfiguration.class);
        
        // Get bridge handler to access HTTP client
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            // Get HTTP client from bridge
            rachioHttp = bridgeHandler.getRachioHttp();
            rachioSecurity = bridgeHandler.getRachioSecurity();
            
            // Register with bridge for status updates
            bridgeHandler.registerStatusListener(this);
            
            updateStatus(ThingStatus.UNKNOWN);
            
            // Start initial poll
            scheduler.execute(this::pollStatus);
            
            // Start regular polling
            startPolling();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No bridge available");
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio zone handler for {}", getThing().getUID());
        
        // Unregister from bridge
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            bridgeHandler.unregisterStatusListener(this);
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
        if (localConfig == null || localConfig.getZoneId() == null) {
            logger.warn("Zone configuration not available");
            return;
        }

        RachioHttp localHttpClient = rachioHttp;
        if (localHttpClient == null) {
            logger.warn("HTTP client not available");
            return;
        }

        String channelId = channelUID.getIdWithoutGroup();

        try {
            switch (channelId) {
                case CHANNEL_ZONE_START:
                    if (command instanceof OnOffType) {
                        if (command == OnOffType.ON) {
                            // Use configured default duration or fallback
                            Integer duration = localConfig.getDefaultDuration() != null ? 
                                localConfig.getDefaultDuration() : DEFAULT_ZONE_DURATION;
                            startZone(localConfig.getZoneId(), duration);
                        } else {
                            stopZone(localConfig.getZoneId());
                        }
                    } else if (command instanceof QuantityType) {
                        // Handle duration command
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int minutes = quantity.toUnit(Units.MINUTE).intValue();
                        
                        // Use the provided duration or configured default
                        Integer duration = localConfig.getDefaultDuration() != null ? 
                            localConfig.getDefaultDuration() : DEFAULT_ZONE_DURATION;
                        
                        if (minutes > 0) {
                            duration = minutes;
                        }
                        
                        startZone(localConfig.getZoneId(), duration);
                    }
                    break;

                case CHANNEL_ZONE_STOP:
                    if (command == OnOffType.ON) {
                        stopZone(localConfig.getZoneId());
                    }
                    break;

                case CHANNEL_ZONE_ENABLED:
                    if (command instanceof OnOffType) {
                        boolean enabled = command == OnOffType.ON;
                        localHttpClient.setZoneEnabled(localConfig.getZoneId(), enabled);
                        scheduler.execute(this::pollStatus);
                    }
                    break;

                case CHANNEL_ZONE_DURATION:
                    if (command instanceof DecimalType) {
                        int minutes = ((DecimalType) command).intValue();
                        if (minutes > 0) {
                            startZone(localConfig.getZoneId(), minutes);
                        }
                    } else if (command instanceof QuantityType) {
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int minutes = quantity.toUnit(Units.MINUTE).intValue();
                        if (minutes > 0) {
                            startZone(localConfig.getZoneId(), minutes);
                        }
                    }
                    break;

                case CHANNEL_ZONE_RUNTIME:
                    if (command instanceof DecimalType) {
                        int seconds = ((DecimalType) command).intValue();
                        if (seconds > 0) {
                            startZone(localConfig.getZoneId(), seconds / 60); // Convert seconds to minutes
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
    protected void pollStatus() throws RachioApiException {
        RachioZoneConfiguration localConfig = config;
        if (localConfig == null || localConfig.getZoneId() == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, 
                    "Zone configuration not available");
            return;
        }

        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No bridge available");
            return;
        }

        try {
            RachioZone zone = bridgeHandler.getZone(localConfig.getZoneId());
            if (zone != null) {
                updateZone(zone);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone not found");
            }
        } catch (Exception e) {
            logger.error("Error polling zone status", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Update zone with new data
     *
     * @param zone zone data
     */
    protected void updateZone(RachioZone zone) {
        this.zone = zone;
        updateProperties(zone);
        updateChannels(zone);
    }

    /**
     * Update thing properties from zone data
     *
     * @param zone zone data
     */
    protected void updateProperties(RachioZone zone) {
        Map<String, String> properties = new HashMap<>();
        
        // Basic properties
        properties.put(PROPERTY_ID, zone.getId());
        properties.put(PROPERTY_NAME, zone.getName());
        properties.put("zoneNumber", String.valueOf(zone.getZoneNumber()));
        properties.put("enabled", String.valueOf(zone.isEnabled()));
        properties.put("runtime", String.valueOf(zone.getRuntime()));
        
        // Professional irrigation properties
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
        
        // Update thing properties
        updateProperties(properties);
    }

    /**
     * Update channels from zone data
     *
     * @param zone zone data
     */
    protected void updateChannels(RachioZone zone) {
        // Basic zone status
        updateState(CHANNEL_ZONE_ENABLED, zone.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_ZONE_RUNTIME, new DecimalType(zone.getRuntime()));
        
        // Zone property channels
        updateZonePropertyChannels(zone);
        
        // Update rate limit channels (inherited from RachioHandler)
        updateRateLimitChannels();
    }

    /**
     * Update professional zone property channels
     *
     * @param zone zone data
     */
    private void updateZonePropertyChannels(RachioZone zone) {
        CustomSoil soil = zone.getSoil();
        if (soil != null) {
            if (soil.getType() != null) {
                updateState(CHANNEL_SOIL_TYPE, new DecimalType(0)); // Would need string channel type
            }
            if (soil.getAvailableWater() != null) {
                updateState(CHANNEL_SOIL_AVAILABLE_WATER, new DecimalType(soil.getAvailableWater()));
            }
        }
        
        CustomCrop crop = zone.getCrop();
        if (crop != null) {
            if (crop.getType() != null) {
                updateState(CHANNEL_CROP_TYPE, new DecimalType(0)); // Would need string channel type
            }
            if (crop.getCoefficient() != null) {
                updateState(CHANNEL_CROP_COEFFICIENT, new DecimalType(crop.getCoefficient()));
            }
        }
        
        CustomNozzle nozzle = zone.getNozzle();
        if (nozzle != null) {
            if (nozzle.getType() != null) {
                updateState(CHANNEL_NOZZLE_TYPE, new DecimalType(0)); // Would need string channel type
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
     * Start zone watering
     *
     * @param zoneId zone ID
     * @param duration duration in minutes
     */
    private void startZone(String zoneId, Integer duration) {
        RachioHttp localHttpClient = rachioHttp;
        RachioZoneConfiguration localConfig = config;
        
        if (localHttpClient == null) {
            logger.warn("HTTP client not available");
            return;
        }
        
        // Use configured default duration if available and positive
        if (localConfig != null && localConfig.getDefaultDuration() != null && localConfig.getDefaultDuration() > 0) {
            duration = localConfig.getDefaultDuration();
        }
        
        // Ensure minimum duration
        if (duration <= 0) {
            duration = DEFAULT_ZONE_DURATION;
        }
        
        logger.debug("Starting zone {} for {} minutes", zoneId, duration);
        
        try {
            localHttpClient.startZone(zoneId, duration * 60); // Convert minutes to seconds
            
            // Schedule refresh to get updated status
            scheduler.schedule(() -> {
                scheduler.execute(this::pollStatus);
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
            
        } catch (Exception e) {
            logger.error("Error starting zone", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Stop zone watering
     *
     * @param zoneId zone ID
     */
    private void stopZone(String zoneId) {
        RachioHttp localHttpClient = rachioHttp;
        
        if (localHttpClient == null) {
            logger.warn("HTTP client not available");
            return;
        }
        
        logger.debug("Stopping zone {}", zoneId);
        
        try {
            localHttpClient.stopZone(zoneId);
            
            // Schedule refresh
            scheduler.schedule(() -> {
                scheduler.execute(this::pollStatus);
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
            
        } catch (Exception e) {
            logger.error("Error stopping zone", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Get the current zone data
     *
     * @return zone data or null
     */
    public @Nullable RachioZone getZone() {
        return zone;
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

    // ===== RachioStatusListener interface implementations =====
    
    @Override
    public void onDeviceUpdated(String deviceId) {
        // Zone handler doesn't need to react to device updates directly
    }

    @Override
    public void onZoneUpdated(String zoneId) {
        RachioZoneConfiguration localConfig = config;
        if (localConfig != null && zoneId.equals(localConfig.getZoneId())) {
            logger.debug("Zone {} update received via listener", zoneId);
            scheduler.execute(this::pollStatus);
        }
    }

    @Override
    public void onWebhookEvent(String deviceId, String eventType, 
                               @Nullable String subType, 
                               @Nullable Map<String, Object> eventData) {
        // Zone handler might react to zone-specific webhook events
        logger.debug("Webhook event {} received for device {}", eventType, deviceId);
    }

    @Override
    public void onRateLimitChanged(int remainingRequests, int limit, long resetTime) {
        // Update rate limit channels via parent class
        scheduler.execute(this::updateRateLimitChannels);
    }

    @Override
    public void onConnectionChanged(boolean connected, @Nullable String message) {
        if (!connected) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, 
                    message != null ? message : "Lost connection to Rachio API");
        } else {
            updateStatus(ThingStatus.ONLINE);
            scheduler.execute(this::pollStatus);
        }
    }

    @Override
    public void onError(String errorMessage, @Nullable Throwable exception) {
        logger.error("Error received: {}", errorMessage, exception);
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
    }

    @Override
    public @Nullable String getThingId() {
        return getThing().getUID().getId();
    }

    @Override
    public boolean isForZone(String zoneId) {
        RachioZoneConfiguration localConfig = config;
        return localConfig != null && zoneId.equals(localConfig.getZoneId());
    }
}
