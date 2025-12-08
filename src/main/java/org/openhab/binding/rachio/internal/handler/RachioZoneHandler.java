package org.openhab.binding.rachio.internal.handler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.quantity.Length;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands for individual zones.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends RachioHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);
    
    private final Map<String, Channel> channels = new HashMap<>();
    private @Nullable RachioZoneConfiguration config;
    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable RachioHttp http;
    private @Nullable ScheduledFuture<?> refreshJob;
    private @Nullable Instant lastEventTime;
    
    private @Nullable RachioZone zone;
    private @Nullable RachioDevice device;
    
    public RachioZoneHandler(Thing thing) {
        super(thing);
    }
    
    @Override
    public void initialize() {
        logger.debug("Initializing Rachio Zone handler");
        
        config = getConfigAs(RachioZoneConfiguration.class);
        bridgeHandler = getBridgeHandler();
        
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED, "No bridge configured");
            return;
        }
        
        http = bridgeHandler.getHttp();
        if (http == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "HTTP client not available");
            return;
        }
        
        // Cache channels for faster access
        for (Channel channel : getThing().getChannels()) {
            channels.put(channel.getUID().getId(), channel);
        }
        
        // Start refresh job
        scheduleRefresh();
        
        updateStatus(ThingStatus.UNKNOWN);
    }
    
    @Override
    public void dispose() {
        ScheduledFuture<?> localRefreshJob = refreshJob;
        if (localRefreshJob != null) {
            localRefreshJob.cancel(true);
            refreshJob = null;
        }
        super.dispose();
    }
    
    private @Nullable RachioBridgeHandler getBridgeHandler() {
        ThingHandler handler = getBridge().getHandler();
        if (handler instanceof RachioBridgeHandler) {
            return (RachioBridgeHandler) handler;
        }
        return null;
    }
    
    private void scheduleRefresh() {
        ScheduledFuture<?> localRefreshJob = refreshJob;
        if (localRefreshJob != null) {
            localRefreshJob.cancel(false);
        }
        
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler != null) {
            int refreshInterval = localBridgeHandler.getRefreshInterval();
            refreshJob = scheduler.scheduleWithFixedDelay(this::refresh, 10, refreshInterval, TimeUnit.SECONDS);
            logger.debug("Scheduled refresh every {} seconds", refreshInterval);
        }
    }
    
    @Override
    public void refresh() {
        try {
            logger.debug("Refreshing Rachio zone {}", getThing().getUID());
            
            RachioBridgeHandler localBridgeHandler = bridgeHandler;
            RachioHttp localHttp = http;
            RachioZoneConfiguration localConfig = config;
            
            if (localBridgeHandler == null || localHttp == null || localConfig == null) {
                logger.warn("Cannot refresh - handler not properly initialized");
                return;
            }
            
            // Get device info first (to get zone info)
            String deviceId = localConfig.deviceId;
            String zoneId = localConfig.zoneId;
            
            if (deviceId == null || deviceId.isEmpty() || zoneId == null || zoneId.isEmpty()) {
                logger.warn("Device ID or Zone ID not configured");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "ID not set");
                return;
            }
            
            // Get device to find zone
            device = localHttp.getDeviceInfo(deviceId);
            if (device == null) {
                logger.warn("Device {} not found", deviceId);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Device not found");
                return;
            }
            
            // Find zone in device
            zone = findZoneInDevice(device, zoneId);
            if (zone == null) {
                logger.warn("Zone {} not found in device {}", zoneId, deviceId);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone not found");
                return;
            }
            
            // Update status
            updateStatus(ThingStatus.ONLINE);
            
            // Update channels
            updateZoneChannels();
            
            logger.debug("Refresh completed for zone {}", zoneId);
            
        } catch (Exception e) {
            logger.error("Error refreshing zone: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }
    
    private @Nullable RachioZone findZoneInDevice(RachioDevice device, String zoneId) {
        if (device.getZones() == null) {
            return null;
        }
        
        for (RachioZone zone : device.getZones()) {
            if (zoneId.equals(zone.getId())) {
                return zone;
            }
        }
        return null;
    }
    
    private void updateZoneChannels() {
        RachioZone localZone = zone;
        if (localZone == null) {
            return;
        }
        
        logger.debug("Updating channels for zone: {}", localZone.getName());
        
        // Basic zone info
        updateState(RachioBindingConstants.CHANNEL_ZONE_NAME, new StringType(localZone.getName()));
        updateState(RachioBindingConstants.CHANNEL_ZONE_NUMBER, new DecimalType(localZone.getZoneNumber()));
        updateState(RachioBindingConstants.CHANNEL_ZONE_ENABLED, localZone.isEnabled() ? OnOffType.ON : OnOffType.OFF);
        updateState(RachioBindingConstants.CHANNEL_ZONE_STATUS, new StringType(localZone.isEnabled() ? "ENABLED" : "DISABLED"));
        
        // Runtime info
        updateState(RachioBindingConstants.CHANNEL_ZONE_RUNTIME, new DecimalType(localZone.getRuntime()));
        updateState(RachioBindingConstants.CHANNEL_ZONE_MAX_RUNTIME, new DecimalType(localZone.getMaxRuntime()));
        
        // Area (convert square feet to square meters)
        double areaSqm = localZone.getArea() * 0.092903;
        updateState(RachioBindingConstants.CHANNEL_ZONE_AREA, 
                   new QuantityType<>(areaSqm, Units.SQUARE_METRE));
        
        // Professional irrigation data
        updateState(RachioBindingConstants.CHANNEL_SOIL_TYPE, new StringType(localZone.getSoilType()));
        updateState(RachioBindingConstants.CHANNEL_CROP_TYPE, new StringType(localZone.getCropType()));
        updateState(RachioBindingConstants.CHANNEL_NOZZLE_TYPE, new StringType(localZone.getNozzleType()));
        updateState(RachioBindingConstants.CHANNEL_SLOPE_TYPE, new StringType(localZone.getSlopeType()));
        updateState(RachioBindingConstants.CHANNEL_SHADE_TYPE, new StringType(localZone.getShadeType()));
        
        updateState(RachioBindingConstants.CHANNEL_ROOT_DEPTH, 
                   new QuantityType<Length>(localZone.getRootZoneDepth(), Units.INCH));
        updateState(RachioBindingConstants.CHANNEL_IRRIGATION_EFFICIENCY, 
                   new DecimalType(localZone.getEfficiency()));
        updateState(RachioBindingConstants.CHANNEL_SOIL_AVAILABLE_WATER, 
                   new DecimalType(localZone.getAvailableWater()));
        
        // Watering adjustment runtimes
        if (localZone.getWateringAdjustmentRuntimes() != null) {
            int[] adjustments = localZone.getWateringAdjustmentRuntimes().stream()
                    .mapToInt(Integer::intValue)
                    .toArray();
            
            for (int i = 0; i < Math.min(adjustments.length, 5); i++) {
                String channelId = RachioBindingConstants.CHANNEL_ADJUSTMENT_LEVEL_PREFIX + (i + 1);
                updateState(channelId, new DecimalType(adjustments[i]));
            }
        }
        
        logger.debug("Zone channels updated for {}", localZone.getName());
    }
    
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received command {} for channel {}", command, channelUID);
        
        if (command instanceof RefreshType) {
            refresh();
            return;
        }
        
        try {
            RachioHttp localHttp = http;
            RachioZoneConfiguration localConfig = config;
            
            if (localHttp == null || localConfig == null) {
                logger.warn("Handler not properly initialized");
                return;
            }
            
            String channelId = channelUID.getId();
            String deviceId = localConfig.deviceId;
            String zoneId = localConfig.zoneId;
            
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_START_ZONE:
                    if (command == OnOffType.ON) {
                        int duration = 30; // Default 30 minutes
                        localHttp.startZone(deviceId, zoneId, duration);
                        updateState(channelUID, OnOffType.OFF); // Reset switch
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_STOP_ZONE:
                    if (command == OnOffType.ON) {
                        localHttp.stopZone(deviceId, zoneId);
                        updateState(channelUID, OnOffType.OFF); // Reset switch
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_ZONE_ENABLED:
                    if (command instanceof OnOffType) {
                        boolean enabled = command == OnOffType.ON;
                        localHttp.setZoneEnabled(deviceId, zoneId, enabled);
                        updateState(channelUID, command);
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_ZONE_RUNTIME:
                    if (command instanceof DecimalType) {
                        int minutes = ((DecimalType) command).intValue();
                        // Note: This would typically require updating zone configuration
                        // For now, we'll just update the channel state
                        updateState(channelUID, command);
                    }
                    break;
                    
                default:
                    logger.debug("Unhandled command for channel {}", channelId);
            }
            
        } catch (Exception e) {
            logger.error("Error handling command: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void onStatusChanged(RachioWebHookEvent event) {
        logger.debug("Zone {} received status change event: {}", getThing().getUID(), event.getType());
        
        // Check if this event is for our zone
        RachioZoneConfiguration localConfig = config;
        if (localConfig == null || !event.getDeviceId().equals(localConfig.deviceId)) {
            return;
        }
        
        String eventZoneId = event.getZoneId();
        if (eventZoneId == null || !eventZoneId.equals(localConfig.zoneId)) {
            return;
        }
        
        lastEventTime = Instant.now();
        
        // Update zone status based on event
        String eventType = event.getType();
        switch (eventType) {
            case "ZONE_STATUS":
                // Zone started/stopped/completed watering
                refresh();
                break;
                
            case "ZONE_COMPLETED":
                // Zone completed watering
                refresh();
                break;
                
            default:
                logger.debug("Unhandled event type for zone: {}", eventType);
        }
        
        // Update last event time channel
        updateState(RachioBindingConstants.CHANNEL_LAST_EVENT_TIME, 
                   new DateTimeType(ZonedDateTime.now()));
    }
    
    @Override
    public void onDeviceUpdated(RachioDevice updatedDevice) {
        logger.debug("Zone {} received device update", getThing().getUID());
        
        RachioZoneConfiguration localConfig = config;
        if (localConfig == null || !updatedDevice.getId().equals(localConfig.deviceId)) {
            return;
        }
        
        // Update our device cache
        device = updatedDevice;
        
        // Find and update zone
        zone = findZoneInDevice(updatedDevice, localConfig.zoneId);
        if (zone != null) {
            updateZoneChannels();
        }
    }
    
    @Override
    public void handleImageCall(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("Image call received for zone {}", getThing().getUID());
        
        try {
            // This would handle image requests (like zone maps)
            // For now, just return a simple response
            response.setContentType("text/plain");
            response.getWriter().write("Image endpoint for zone " + getThing().getUID());
            response.getWriter().flush();
        } catch (Exception e) {
            logger.error("Error handling image call: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public String getDeviceId() {
        RachioZoneConfiguration localConfig = config;
        return localConfig != null ? localConfig.deviceId : "";
    }
    
    @Override
    public @Nullable Instant getLastEventTime() {
        return lastEventTime;
    }
    
    @Override
    public void updateStatusFromEvent(RachioWebHookEvent event) {
        onStatusChanged(event);
    }
    
    private void updateState(String channelId, State state) {
        Channel channel = channels.get(channelId);
        if (channel != null) {
            updateState(channel.getUID(), state);
        }
    }
}
