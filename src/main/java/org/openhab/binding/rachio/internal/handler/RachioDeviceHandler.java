package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioAlert;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast;
import org.openhab.binding.rachio.internal.api.dto.RachioSchedule;
import org.openhab.binding.rachio.internal.api.dto.RachioUsage;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
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

import com.google.gson.reflect.TypeToken;

/**
 * The {@link RachioDeviceHandler} is responsible for handling commands for Rachio devices
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceHandler extends RachioHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);
    
    private @Nullable RachioDevice device;
    private @Nullable RachioDeviceConfiguration config;
    private @Nullable ScheduledFuture<?> forecastUpdateJob;
    private @Nullable ScheduledFuture<?> usageUpdateJob;
    
    private static final int FORECAST_UPDATE_INTERVAL = 3600; // 1 hour
    private static final int USAGE_UPDATE_INTERVAL = 1800; // 30 minutes

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void initializeThing() throws Exception {
        logger.debug("Initializing Rachio device handler for thing: {}", getThing().getUID());
        
        config = getConfigAs(RachioDeviceConfiguration.class);
        if (config == null || config.deviceId == null || config.deviceId.isBlank()) {
            throw new Exception("Device ID not configured");
        }
        
        // Get device from bridge
        RachioBridgeHandler localBridgeHandler = getBridgeHandler();
        if (localBridgeHandler == null) {
            throw new Exception("Bridge handler not available");
        }
        
        device = localBridgeHandler.getDevice(config.deviceId);
        if (device == null) {
            throw new Exception("Device not found with ID: " + config.deviceId);
        }
        
        // Create dynamic channels for device
        createDeviceChannels();
        
        // Start forecast updates if enabled
        if (localBridgeHandler.isWeatherEnabled()) {
            startForecastUpdates();
        }
        
        // Start usage updates
        startUsageUpdates();
        
        logger.debug("Initialized Rachio device: {} - {}", device.id, device.name);
    }

    @Override
    protected void handleWebhookEvent(RachioWebHookEvent event) {
        if (event == null || device == null || config == null || !config.deviceId.equals(event.deviceId)) {
            return;
        }
        
        logger.debug("Processing webhook event for device {}: {}", config.deviceId, event.type);
        
        switch (event.type) {
            case "DEVICE_STATUS_EVENT":
                updateDeviceStatus(event);
                break;
            case "RAIN_DELAY_EVENT":
                updateRainDelay(event);
                break;
            case "WEATHER_INTELLIGENCE_EVENT":
                updateWeatherIntelligence(event);
                break;
            case "SCHEDULE_STATUS_EVENT":
                updateScheduleStatus(event);
                break;
            case "ALERT_EVENT":
                updateAlert(event);
                break;
            default:
                logger.debug("Unhandled webhook event type: {}", event.type);
        }
    }

    @Override
    protected void refreshAllChannels() {
        logger.debug("Refreshing all channels for device: {}", getThing().getUID());
        
        try {
            // Refresh device status
            refreshDeviceStatus();
            
            // Refresh forecast if enabled
            RachioBridgeHandler bridgeHandler = getBridgeHandler();
            if (bridgeHandler != null && bridgeHandler.isWeatherEnabled()) {
                refreshForecast();
            }
            
            // Refresh usage
            refreshUsage();
            
            // Refresh alerts
            refreshAlerts();
            
        } catch (Exception e) {
            logger.warn("Error refreshing device channels: {}", e.getMessage(), e);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshChannel(channelUID.getId());
            return;
        }
        
        String channelId = channelUID.getId();
        RachioHttp localHttp = getRachioHttp();
        
        if (localHttp == null || config == null || config.deviceId == null) {
            logger.warn("Cannot handle command: HTTP client or device ID not available");
            return;
        }
        
        try {
            switch (channelId) {
                case CHANNEL_RUN_ALL_ZONES:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        localHttp.runAllZones(config.deviceId, duration);
                        logger.debug("Started all zones for {} seconds", duration);
                    }
                    break;
                    
                case CHANNEL_STOP_WATERING:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        localHttp.stopWatering(config.deviceId);
                        logger.debug("Stopped all watering");
                    }
                    break;
                    
                case CHANNEL_RAIN_DELAY:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        localHttp.rainDelay(config.deviceId, duration);
                        logger.debug("Set rain delay for {} hours", duration);
                    }
                    break;
                    
                case CHANNEL_RUN_NEXT_ZONE:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        localHttp.runNextZone(config.deviceId);
                        logger.debug("Started next zone");
                    }
                    break;
                    
                case CHANNEL_PAUSE_DEVICE:
                    if (command instanceof OnOffType) {
                        localHttp.pauseDevice(config.deviceId);
                        logger.debug("Device pause toggled");
                    }
                    break;
                    
                default:
                    logger.debug("Unhandled command for channel: {}", channelId);
            }
        } catch (IOException e) {
            logger.warn("Failed to execute command {} on channel {}: {}", command, channelId, e.getMessage());
        }
    }
    
    /**
     * Refresh a specific channel
     */
    private void refreshChannel(String channelId) {
        try {
            switch (channelId) {
                case CHANNEL_STATUS:
                    refreshDeviceStatus();
                    break;
                case CHANNEL_ONLINE:
                    refreshDeviceStatus();
                    break;
                case CHANNEL_RAIN_DELAY:
                    refreshDeviceStatus();
                    break;
                case CHANNEL_FORECAST_TEMP:
                case CHANNEL_FORECAST_PRECIP:
                case CHANNEL_FORECAST_HUMIDITY:
                case CHANNEL_FORECAST_WIND:
                case CHANNEL_FORECAST_SOLAR:
                    refreshForecast();
                    break;
                case CHANNEL_USAGE_TOTAL:
                case CHANNEL_USAGE_SAVINGS:
                case CHANNEL_USAGE_CURRENT:
                    refreshUsage();
                    break;
                case CHANNEL_ALERTS:
                    refreshAlerts();
                    break;
                default:
                    logger.debug("No specific refresh for channel: {}", channelId);
            }
        } catch (Exception e) {
            logger.warn("Error refreshing channel {}: {}", channelId, e.getMessage());
        }
    }
    
    /**
     * Create dynamic channels for the device
     */
    private void createDeviceChannels() {
        if (device == null) {
            return;
        }
        
        List<Channel> channels = new ArrayList<>(getThing().getChannels());
        
        // Add professional data channels
        if (device.elevation != null) {
            channels.add(createChannel(CHANNEL_ELEVATION, "Number:Length", "Elevation", 
                "Device elevation above sea level", null));
        }
        
        if (device.flexScheduleRules != null) {
            channels.add(createChannel(CHANNEL_FLEX_SCHEDULE, "Switch", "Flex Schedule", 
                "Flex schedule rules enabled", null));
        }
        
        // Add zone summary channels if device has zones
        if (device.zones != null && !device.zones.isEmpty()) {
            channels.add(createChannel(CHANNEL_ZONE_COUNT, "Number", "Zone Count", 
                "Number of zones on this device", null));
            
            channels.add(createChannel(CHANNEL_TOTAL_AREA, "Number:Area", "Total Area", 
                "Total irrigated area", null));
            
            // Calculate total area
            double totalArea = 0;
            for (RachioZone zone : device.zones) {
                if (zone != null && zone.area != null && zone.enabled != null && zone.enabled) {
                    totalArea += zone.area;
                }
            }
            
            if (totalArea > 0) {
                // Use string unit instead of Units.SQUARE_METRE
                updateState(CHANNEL_TOTAL_AREA, new QuantityType<>(totalArea + " m²"));
            }
        }
        
        // Update thing with new channels
        updateThing(editThing().withChannels(channels).build());
    }
    
    /**
     * Refresh device status
     */
    private void refreshDeviceStatus() throws IOException {
        RachioHttp localHttp = getRachioHttp();
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        
        if (localHttp == null || bridgeHandler == null || config == null) {
            return;
        }
        
        // Get updated device info from bridge (it should have the latest from polling)
        RachioDevice updatedDevice = bridgeHandler.getDevice(config.deviceId);
        if (updatedDevice != null) {
            device = updatedDevice;
            
            // Update status channels
            updateState(CHANNEL_STATUS, device.status != null ? device.status : "UNKNOWN");
            updateState(CHANNEL_ONLINE, "ONLINE".equals(device.status));
            
            if (device.rainDelay != null) {
                updateState(CHANNEL_RAIN_DELAY, device.rainDelay > 0);
                if (device.rainDelay > 0) {
                    updateState(CHANNEL_RAIN_DELAY_REMAINING, device.rainDelay);
                }
            }
            
            if (device.serialNumber != null) {
                updateState(CHANNEL_SERIAL_NUMBER, device.serialNumber);
            }
            
            if (device.macAddress != null) {
                updateState(CHANNEL_MAC_ADDRESS, device.macAddress);
            }
            
            if (device.elevation != null) {
                updateState(CHANNEL_ELEVATION, device.elevation, "m");
            }
            
            if (device.flexScheduleRules != null) {
                updateState(CHANNEL_FLEX_SCHEDULE, device.flexScheduleRules);
            }
        }
    }
    
    /**
     * Refresh weather forecast
     */
    private void refreshForecast() throws IOException {
        RachioHttp localHttp = getRachioHttp();
        if (localHttp == null || config == null) {
            return;
        }
        
        String forecastJson = localHttp.getDeviceForecast(config.deviceId);
        if (forecastJson != null && !forecastJson.isEmpty()) {
            RachioForecast forecast = getGson().fromJson(forecastJson, RachioForecast.class);
            if (forecast != null && forecast.days != null && !forecast.days.isEmpty()) {
                // Use first day's forecast (today)
                RachioForecast.ForecastDay today = forecast.days.get(0);
                
                if (today.temp != null) {
                    // Use string unit instead of Units.CELSIUS
                    updateState(CHANNEL_FORECAST_TEMP, new QuantityType<>(today.temp + " °C"));
                }
                
                if (today.precip != null) {
                    // Use string unit instead of Units.MILLIMETRE
                    updateState(CHANNEL_FORECAST_PRECIP, new QuantityType<>(today.precip + " mm"));
                }
                
                if (today.humidity != null) {
                    // Use string unit for percent
                    updateState(CHANNEL_FORECAST_HUMIDITY, new QuantityType<>(today.humidity + " %"));
                }
                
                if (today.wind != null) {
                    // Use string unit instead of Units.KILOMETRE_PER_HOUR
                    updateState(CHANNEL_FORECAST_WIND, new QuantityType<>(today.wind + " km/h"));
                }
                
                if (today.solar != null) {
                    updateState(CHANNEL_FORECAST_SOLAR, new QuantityType<>(today.solar + " W/m²"));
                }
                
                if (today.et != null) {
                    updateState(CHANNEL_FORECAST_ET, new QuantityType<>(today.et + " mm"));
                }
            }
        }
    }
    
    /**
     * Refresh water usage and savings
     */
    private void refreshUsage() throws IOException {
        RachioHttp localHttp = getRachioHttp();
        if (localHttp == null || config == null) {
            return;
        }
        
        String usageJson = localHttp.getDeviceUsage(config.deviceId);
        if (usageJson != null && !usageJson.isEmpty()) {
            RachioUsage usage = getGson().fromJson(usageJson, RachioUsage.class);
            if (usage != null) {
                if (usage.totalUsage != null) {
                    updateState(CHANNEL_USAGE_TOTAL, new QuantityType<>(usage.totalUsage + " gal"));
                }
                
                if (usage.savings != null) {
                    updateState(CHANNEL_USAGE_SAVINGS, new QuantityType<>(usage.savings + " gal"));
                }
                
                if (usage.currentUsage != null) {
                    updateState(CHANNEL_USAGE_CURRENT, new QuantityType<>(usage.currentUsage + " gal"));
                }
            }
        }
        
        // Also get savings data
        String savingsJson = localHttp.getDeviceSavings(config.deviceId);
        if (savingsJson != null && !savingsJson.isEmpty()) {
            // Parse savings data if needed
        }
    }
    
    /**
     * Refresh device alerts
     */
    private void refreshAlerts() throws IOException {
        RachioHttp localHttp = getRachioHttp();
        if (localHttp == null || config == null) {
            return;
        }
        
        String alertsJson = localHttp.getDeviceAlerts(config.deviceId);
        if (alertsJson != null && !alertsJson.isEmpty()) {
            List<RachioAlert> alerts = getGson().fromJson(alertsJson, 
                new TypeToken<List<RachioAlert>>() {}.getType());
            
            if (alerts != null && !alerts.isEmpty()) {
                StringBuilder alertText = new StringBuilder();
                for (RachioAlert alert : alerts) {
                    if (alert != null && alert.message != null) {
                        if (alertText.length() > 0) {
                            alertText.append("; ");
                        }
                        alertText.append(alert.message);
                    }
                }
                updateState(CHANNEL_ALERTS, alertText.toString());
            }
        }
    }
    
    /**
     * Start periodic forecast updates
     */
    private void startForecastUpdates() {
        stopForecastUpdates();
        
        forecastUpdateJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshForecast();
            } catch (Exception e) {
                logger.warn("Error updating forecast: {}", e.getMessage());
            }
        }, 0, FORECAST_UPDATE_INTERVAL, TimeUnit.SECONDS);
    }
    
    /**
     * Stop forecast updates
     */
    private void stopForecastUpdates() {
        ScheduledFuture<?> localForecastUpdateJob = forecastUpdateJob;
        if (localForecastUpdateJob != null && !localForecastUpdateJob.isCancelled()) {
            localForecastUpdateJob.cancel(true);
            forecastUpdateJob = null;
        }
    }
    
    /**
     * Start periodic usage updates
     */
    private void startUsageUpdates() {
        stopUsageUpdates();
        
        usageUpdateJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshUsage();
            } catch (Exception e) {
                logger.warn("Error updating usage: {}", e.getMessage());
            }
        }, 0, USAGE_UPDATE_INTERVAL, TimeUnit.SECONDS);
    }
    
    /**
     * Stop usage updates
     */
    private void stopUsageUpdates() {
        ScheduledFuture<?> localUsageUpdateJob = usageUpdateJob;
        if (localUsageUpdateJob != null && !localUsageUpdateJob.isCancelled()) {
            localUsageUpdateJob.cancel(true);
            usageUpdateJob = null;
        }
    }
    
    /**
     * Update device status from webhook event
     */
    private void updateDeviceStatus(RachioWebHookEvent event) {
        if (event.subType != null) {
            updateState(CHANNEL_STATUS, event.subType);
            updateState(CHANNEL_ONLINE, "ONLINE".equals(event.subType));
        }
    }
    
    /**
     * Update rain delay from webhook event
     */
    private void updateRainDelay(RachioWebHookEvent event) {
        if (event.rainDelay != null) {
            updateState(CHANNEL_RAIN_DELAY, event.rainDelay > 0);
            if (event.rainDelay > 0) {
                updateState(CHANNEL_RAIN_DELAY_REMAINING, event.rainDelay);
            }
        }
    }
    
    /**
     * Update weather intelligence from webhook event
     */
    private void updateWeatherIntelligence(RachioWebHookEvent event) {
        // Could update forecast channels based on event data
        logger.debug("Weather intelligence event received: {}", event.summary);
    }
    
    /**
     * Update schedule status from webhook event
     */
    private void updateScheduleStatus(RachioWebHookEvent event) {
        logger.debug("Schedule status event received: {}", event.summary);
    }
    
    /**
     * Update alert from webhook event
     */
    private void updateAlert(RachioWebHookEvent event) {
        if (event.summary != null) {
            updateState(CHANNEL_ALERTS, event.summary);
        }
    }
    
    /**
     * Get Gson instance from bridge handler
     */
    private com.google.gson.Gson getGson() {
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        return bridgeHandler != null ? bridgeHandler.getGson() : new com.google.gson.Gson();
    }
    
    @Override
    public void dispose() {
        logger.debug("Disposing Rachio device handler for thing: {}", getThing().getUID());
        
        stopForecastUpdates();
        stopUsageUpdates();
        
        super.dispose();
    }
}
