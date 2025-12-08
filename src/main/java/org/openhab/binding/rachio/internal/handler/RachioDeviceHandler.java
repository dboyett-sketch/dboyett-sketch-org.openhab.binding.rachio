package org.openhab.binding.rachio.internal.handler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.quantity.Temperature;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.ForecastDay;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
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
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceHandler extends RachioHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);
    
    private final Map<String, Channel> channels = new HashMap<>();
    private @Nullable RachioDeviceConfiguration config;
    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable RachioHttp http;
    private @Nullable ScheduledFuture<?> refreshJob;
    private @Nullable Instant lastEventTime;
    
    private @Nullable RachioDevice device;
    private @Nullable RachioForecast forecast;
    
    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }
    
    @Override
    public void initialize() {
        logger.debug("Initializing Rachio Device handler");
        
        config = getConfigAs(RachioDeviceConfiguration.class);
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
            logger.debug("Refreshing Rachio device {}", getThing().getUID());
            
            RachioBridgeHandler localBridgeHandler = bridgeHandler;
            RachioHttp localHttp = http;
            RachioDeviceConfiguration localConfig = config;
            
            if (localBridgeHandler == null || localHttp == null || localConfig == null) {
                logger.warn("Cannot refresh - handler not properly initialized");
                return;
            }
            
            // Get device info
            String deviceId = localConfig.deviceId;
            if (deviceId == null || deviceId.isEmpty()) {
                logger.warn("Device ID not configured");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID not set");
                return;
            }
            
            device = localHttp.getDeviceInfo(deviceId);
            if (device == null) {
                logger.warn("Device {} not found", deviceId);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Device not found");
                return;
            }
            
            // Update status
            updateStatus(ThingStatus.ONLINE);
            
            // Update channels
            updateDeviceChannels();
            
            // Get forecast if configured
            if (localBridgeHandler.isWeatherEnabled()) {
                forecast = localHttp.getDeviceForecast(deviceId);
                updateForecastChannels();
            }
            
            logger.debug("Refresh completed for device {}", deviceId);
            
        } catch (Exception e) {
            logger.error("Error refreshing device: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }
    
    private void updateDeviceChannels() {
        RachioDevice localDevice = device;
        if (localDevice == null) {
            return;
        }
        
        logger.debug("Updating channels for device: {}", localDevice.getName());
        
        // Basic device info
        updateState(RachioBindingConstants.CHANNEL_DEVICE_NAME, new StringType(localDevice.getName()));
        updateState(RachioBindingConstants.CHANNEL_DEVICE_MODEL, new StringType(localDevice.getModel()));
        updateState(RachioBindingConstants.CHANNEL_DEVICE_STATUS, new StringType(localDevice.getStatus()));
        
        // Online status
        boolean isOnline = "ONLINE".equalsIgnoreCase(localDevice.getStatus());
        updateState(RachioBindingConstants.CHANNEL_DEVICE_ONLINE, isOnline ? OnOffType.ON : OnOffType.OFF);
        
        // Zone count
        List<RachioZone> zones = localDevice.getZones();
        int zoneCount = zones != null ? zones.size() : 0;
        updateState(RachioBindingConstants.CHANNEL_ZONE_COUNT, new DecimalType(zoneCount));
        
        // Check if watering
        boolean isWatering = false;
        int activeZoneCount = 0;
        if (zones != null) {
            for (RachioZone zone : zones) {
                // Simplified check - in reality you'd need to check zone status
                // This is a placeholder - actual implementation would require
                // checking current zone runs via API
                if (zone.isEnabled()) {
                    activeZoneCount++;
                }
            }
        }
        updateState(RachioBindingConstants.CHANNEL_WATERING, isWatering ? OnOffType.ON : OnOffType.OFF);
        updateState(RachioBindingConstants.CHANNEL_ACTIVE_ZONE_COUNT, new DecimalType(activeZoneCount));
        
        // Total zone area (simplified - would need calculation from zones)
        double totalArea = 0.0;
        if (zones != null) {
            for (RachioZone zone : zones) {
                totalArea += zone.getArea();
            }
        }
        // Convert square feet to square meters
        double areaSqm = totalArea * 0.092903;
        updateState(RachioBindingConstants.CHANNEL_TOTAL_ZONE_AREA, 
                   new QuantityType<>(areaSqm, Units.SQUARE_METRE));
        
        // Device location
        updateState(RachioBindingConstants.CHANNEL_LATITUDE, new DecimalType(localDevice.getLatitude()));
        updateState(RachioBindingConstants.CHANNEL_LONGITUDE, new DecimalType(localDevice.getLongitude()));
        updateState(RachioBindingConstants.CHANNEL_ELEVATION, new DecimalType(localDevice.getElevation()));
        updateState(RachioBindingConstants.CHANNEL_TIMEZONE, new StringType(localDevice.getTimeZone()));
        
        // Flex schedule
        updateState(RachioBindingConstants.CHANNEL_FLEX_SCHEDULE, 
                   localDevice.isFlexScheduleRules() ? OnOffType.ON : OnOffType.OFF);
        
        // Rain delay status (simplified - would need actual rain delay API)
        updateState(RachioBindingConstants.CHANNEL_RAIN_DELAY, OnOffType.OFF);
        
        logger.debug("Device channels updated for {}", localDevice.getName());
    }
    
    private void updateForecastChannels() {
        RachioForecast localForecast = forecast;
        if (localForecast == null) {
            return;
        }
        
        List<ForecastDay> forecastDays = localForecast.getForecast();
        if (forecastDays == null || forecastDays.isEmpty()) {
            return;
        }
        
        // Get today's forecast (first day in list)
        ForecastDay today = forecastDays.get(0);
        
        // Update forecast channels
        updateState(RachioBindingConstants.CHANNEL_FORECAST_TEMP, 
                   new QuantityType<Temperature>(today.getTemperatureHigh(), Units.CELSIUS));
        updateState(RachioBindingConstants.CHANNEL_FORECAST_PRECIP, 
                   new QuantityType<>(today.getPrecipIntensity(), Units.MILLIMETRE));
        updateState(RachioBindingConstants.CHANNEL_FORECAST_HUMIDITY, 
                   new DecimalType(today.getHumidity() * 100)); // Convert to percentage
        updateState(RachioBindingConstants.CHANNEL_FORECAST_ET, 
                   new QuantityType<>(today.getPrecipIntensity(), Units.MILLIMETRE)); // Simplified ET
        updateState(RachioBindingConstants.CHANNEL_FORECAST_WIND_SPEED, 
                   new QuantityType<>(today.getWindSpeed(), Units.KILOMETRE_PER_HOUR));
        updateState(RachioBindingConstants.CHANNEL_FORECAST_SUMMARY, 
                   new StringType(today.getSummary()));
        updateState(RachioBindingConstants.CHANNEL_FORECAST_ICON, 
                   new StringType(today.getIcon()));
        
        logger.debug("Forecast channels updated");
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
            RachioDeviceConfiguration localConfig = config;
            
            if (localHttp == null || localConfig == null) {
                logger.warn("Handler not properly initialized");
                return;
            }
            
            String channelId = channelUID.getId();
            String deviceId = localConfig.deviceId;
            
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_RUN_ALL_ZONES:
                    if (command == OnOffType.ON) {
                        localHttp.runAllZones(deviceId, 60); // Default 60 minutes
                        updateState(channelUID, OnOffType.OFF); // Reset switch
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_STOP_WATERING:
                    if (command == OnOffType.ON) {
                        localHttp.stopWatering(deviceId);
                        updateState(channelUID, OnOffType.OFF); // Reset switch
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_RAIN_DELAY:
                    if (command instanceof DecimalType) {
                        int hours = ((DecimalType) command).intValue();
                        localHttp.rainDelay(deviceId, hours);
                        updateState(channelUID, new DecimalType(hours));
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_RUN_NEXT_ZONE:
                    if (command == OnOffType.ON) {
                        localHttp.runNextZone(deviceId);
                        updateState(channelUID, OnOffType.OFF); // Reset switch
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
        logger.debug("Device {} received status change event: {}", getThing().getUID(), event.getType());
        
        // Check if this event is for our device
        RachioDeviceConfiguration localConfig = config;
        if (localConfig == null || !event.getDeviceId().equals(localConfig.deviceId)) {
            return;
        }
        
        lastEventTime = Instant.now();
        
        // Update device status based on event
        String eventType = event.getType();
        switch (eventType) {
            case "DEVICE_STATUS":
                // Device status changed (online/offline)
                refresh();
                break;
                
            case "ZONE_STATUS":
                // Zone started/stopped/completed watering
                refresh();
                break;
                
            case "RAIN_DELAY":
                // Rain delay started/stopped
                refresh();
                break;
                
            case "SCHEDULE_STATUS":
                // Schedule started/stopped
                refresh();
                break;
                
            default:
                logger.debug("Unhandled event type: {}", eventType);
        }
        
        // Update last event time channel
        updateState(RachioBindingConstants.CHANNEL_LAST_EVENT_TIME, 
                   new DateTimeType(ZonedDateTime.now()));
    }
    
    @Override
    public void onDeviceUpdated(RachioDevice updatedDevice) {
        logger.debug("Device {} received device update", getThing().getUID());
        
        RachioDeviceConfiguration localConfig = config;
        if (localConfig == null || !updatedDevice.getId().equals(localConfig.deviceId)) {
            return;
        }
        
        // Update our device cache
        device = updatedDevice;
        
        // Refresh channels
        updateDeviceChannels();
    }
    
    @Override
    public void handleImageCall(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("Image call received for device {}", getThing().getUID());
        
        try {
            // This would handle image requests (like zone maps)
            // For now, just return a simple response
            response.setContentType("text/plain");
            response.getWriter().write("Image endpoint for device " + getThing().getUID());
            response.getWriter().flush();
        } catch (Exception e) {
            logger.error("Error handling image call: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public String getDeviceId() {
        RachioDeviceConfiguration localConfig = config;
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
