package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.CustomCrop;
import org.openhab.binding.rachio.internal.api.dto.CustomNozzle;
import org.openhab.binding.rachio.internal.api.dto.CustomSoil;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damien Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler implements RachioStatusListener {

    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);

    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable RachioZone zone;

    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler");
        updateStatus(ThingStatus.UNKNOWN);

        bridgeHandler = (RachioBridgeHandler) getBridge().getHandler();
        if (bridgeHandler != null) {
            // Status listeners are handled differently in OpenHAB 5.x
            // The bridge will update us through property changes
            scheduleZoneUpdate();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No bridge available");
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio zone handler");
        
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            // Status listener cleanup handled by OpenHAB framework
        }
        
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Handling command {} for channel {}", command, channelUID);

        if (command instanceof RefreshType) {
            refreshZone();
            return;
        }

        RachioBridgeHandler handler = bridgeHandler;
        RachioZoneConfiguration cfg = getConfigAs(RachioZoneConfiguration.class);

        if (handler == null) {
            logger.warn("Bridge handler not available");
            return;
        }

        String channelId = channelUID.getIdWithoutGroup();

        try {
            switch (channelId) {
                case CHANNEL_ZONE_START:
                    if (command instanceof OnOffType) {
                        if (command == OnOffType.ON) {
                            // Use configured default duration or fallback
                            Integer duration = cfg.getDefaultDuration() != null ? 
                                cfg.getDefaultDuration() : DEFAULT_ZONE_DURATION;
                            startZone(duration);
                        } else {
                            stopZone();
                        }
                    } else if (command instanceof QuantityType) {
                        // Handle duration command
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int minutes = quantity.toUnit(Units.MINUTE).intValue();
                        
                        // Use the provided duration or configured default
                        Integer duration = cfg.getDefaultDuration() != null ? 
                            cfg.getDefaultDuration() : DEFAULT_ZONE_DURATION;
                        
                        if (minutes > 0) {
                            duration = minutes;
                        }
                        
                        startZone(duration);
                    }
                    break;

                case CHANNEL_ZONE_STOP:
                    if (command == OnOffType.ON) {
                        stopZone();
                    }
                    break;

                case CHANNEL_ZONE_ENABLED:
                    if (command instanceof OnOffType) {
                        boolean enabled = command == OnOffType.ON;
                        handler.getHttpClient().setZoneEnabled(cfg.getZoneId(), enabled);
                        scheduleZoneUpdate();
                    }
                    break;

                case CHANNEL_ZONE_DURATION:
                    if (command instanceof DecimalType) {
                        int minutes = ((DecimalType) command).intValue();
                        if (minutes > 0) {
                            startZone(minutes);
                        }
                    } else if (command instanceof QuantityType) {
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int minutes = quantity.toUnit(Units.MINUTE).intValue();
                        if (minutes > 0) {
                            startZone(minutes);
                        }
                    }
                    break;

                case CHANNEL_ZONE_RUNTIME:
                    if (command instanceof DecimalType) {
                        int seconds = ((DecimalType) command).intValue();
                        if (seconds > 0) {
                            startZone(seconds / 60); // Convert seconds to minutes
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

    @Override
    public void onStatusChanged(ThingStatus status, ThingStatusDetail detail, @Nullable String message) {
        updateStatus(status, detail, message);
        
        if (status == ThingStatus.ONLINE) {
            scheduleZoneUpdate();
        }
    }

    /**
     * Schedule a zone update
     */
    private void scheduleZoneUpdate() {
        scheduler.execute(this::refreshZone);
    }

    /**
     * Refresh zone data from bridge
     */
    private void refreshZone() {
        RachioBridgeHandler handler = bridgeHandler;
        RachioZoneConfiguration cfg = getConfigAs(RachioZoneConfiguration.class);

        if (handler == null) {
            logger.debug("Bridge handler not available for refresh");
            return;
        }

        try {
            RachioZone zone = handler.getZone(cfg.getZoneId());
            if (zone != null) {
                updateZone(zone);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone not found");
            }
        } catch (Exception e) {
            logger.error("Error refreshing zone", e);
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
     * @param duration duration in minutes
     */
    private void startZone(Integer duration) {
        RachioBridgeHandler handler = bridgeHandler;
        RachioZoneConfiguration cfg = getConfigAs(RachioZoneConfiguration.class);
        
        if (handler == null) {
            logger.warn("Bridge handler not available");
            return;
        }
        
        // Use configured default duration if available and positive
        if (cfg.getDefaultDuration() != null && cfg.getDefaultDuration() > 0) {
            duration = cfg.getDefaultDuration();
        }
        
        // Ensure minimum duration
        if (duration <= 0) {
            duration = DEFAULT_ZONE_DURATION;
        }
        
        logger.debug("Starting zone {} for {} minutes", cfg.getZoneId(), duration);
        
        try {
            handler.getHttpClient().startZone(cfg.getZoneId(), duration);
            
            // Update zone status immediately
            updateState(CHANNEL_ZONE_STATUS, OnOffType.ON);
            
            // Schedule refresh to get updated status
            scheduler.schedule(() -> {
                refreshZone();
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
            
        } catch (Exception e) {
            logger.error("Error starting zone", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Stop zone watering
     */
    private void stopZone() {
        RachioBridgeHandler handler = bridgeHandler;
        RachioZoneConfiguration cfg = getConfigAs(RachioZoneConfiguration.class);
        
        if (handler == null) {
            logger.warn("Bridge handler not available");
            return;
        }
        
        logger.debug("Stopping zone {}", cfg.getZoneId());
        
        try {
            handler.getHttpClient().stopZone(cfg.getZoneId());
            
            // Update zone status immediately
            updateState(CHANNEL_ZONE_STATUS, OnOffType.OFF);
            
            // Schedule refresh
            scheduler.schedule(() -> {
                refreshZone();
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
     * Force a poll of devices from bridge
     */
    protected void pollDevices() {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            // Call package-private method on bridge
            handler.pollDevices();
        }
    }
}
