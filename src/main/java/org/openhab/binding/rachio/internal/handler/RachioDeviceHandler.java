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
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast;
import org.openhab.binding.rachio.internal.api.dto.RachioUsage;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
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
 * Handler for Rachio devices (controllers)
 *
 * @author Dave Boyett - Initial contribution
 */
@Component(service = RachioHandler.class)
@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler implements RachioHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    private @Nullable RachioDeviceConfiguration config;
    private @Nullable RachioActions actions;
    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable ScheduledFuture<?> pollingJob;

    private @Nullable RachioDevice device;
    private @Nullable RachioForecast forecast;
    private @Nullable RachioUsage currentUsage;
    private @Nullable RachioUsage currentSavings;

    @Activate
    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = getConfigAs(RachioDeviceConfiguration.class);
        
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Device must be connected to a Rachio bridge");
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
        
        if (config.deviceId == null || config.deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Device ID is not configured");
            return;
        }
        
        // Create dynamic channels for device data
        createDynamicChannels();
        
        // Register as status listener
        if (bridgeHandler != null) {
            bridgeHandler.addStatusListener(new RachioStatusListener() {
                @Override
                public void deviceListUpdated(List<RachioDevice> devices) {
                    // Find our device in the updated list
                    for (RachioDevice d : devices) {
                        if (d.id != null && d.id.equals(config.deviceId)) {
                            device = d;
                            updateDeviceState(d);
                            updateStatus(ThingStatus.ONLINE);
                            break;
                        }
                    }
                }
                
                @Override
                public void webhookEventReceived(org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent event) {
                    // Handle webhook events for this device
                    if (event.deviceId != null && event.deviceId.equals(config.deviceId)) {
                        handleWebhookEvent(event);
                    }
                }
            });
        }
        
        // Start polling
        startPolling();
        
        updateStatus(ThingStatus.UNKNOWN);
        logger.debug("Rachio device handler initialized for device ID: {}", config.deviceId);
    }

    @Override
    public void dispose() {
        stopPolling();
        
        // Unregister as status listener
        if (bridgeHandler != null) {
            bridgeHandler.removeStatusListener(new RachioStatusListener() {
                @Override
                public void deviceListUpdated(List<RachioDevice> devices) {
                    // Empty implementation for removal
                }
                
                @Override
                public void webhookEventReceived(org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent event) {
                    // Empty implementation for removal
                }
            });
        }
        
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        RachioActions localActions = actions;
        RachioDeviceConfiguration localConfig = config;
        
        if (localActions == null || localConfig == null) {
            logger.warn("Handler not properly initialized");
            return;
        }
        
        if (command instanceof RefreshType) {
            refreshDevice();
            return;
        }
        
        String channelId = channelUID.getIdWithoutGroup();
        String thingId = getThing().getUID().getId();
        String deviceId = localConfig.deviceId;
        
        try {
            switch (channelId) {
                case RachioBindingConstants.CHANNEL_DEVICE_PAUSED:
                    if (command instanceof OnOffType) {
                        boolean pause = command == OnOffType.ON;
                        localActions.pauseDevice(deviceId, pause);
                        // Refresh after pausing/unpausing
                        scheduler.schedule(() -> refreshDevice(), 2, TimeUnit.SECONDS);
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_SET_RAIN_DELAY:
                    if (command instanceof DecimalType) {
                        int hours = ((DecimalType) command).intValue();
                        if (hours >= 0) {
                            localActions.rainDelay(thingId, hours, deviceId);
                            logger.debug("Set rain delay to {} hours for device {}", hours, deviceId);
                        }
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_RUN_ALL_ZONES:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        if (duration > 0) {
                            localActions.runAllZones(thingId, duration, deviceId);
                            logger.debug("Running all zones for {} seconds on device {}", duration, deviceId);
                        }
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_RUN_NEXT_ZONE:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        if (duration > 0) {
                            localActions.runNextZone(thingId, duration, deviceId);
                            logger.debug("Running next zone for {} seconds on device {}", duration, deviceId);
                        }
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_STOP_WATERING:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        localActions.stopWatering(thingId, deviceId);
                        updateState(channelUID, OnOffType.OFF); // Reset the switch
                        logger.debug("Stopped watering on device {}", deviceId);
                    }
                    break;
                    
                case RachioBindingConstants.CHANNEL_REFRESH:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        refreshDevice();
                        updateState(channelUID, OnOffType.OFF); // Reset the switch
                    }
                    break;
                    
                default:
                    logger.debug("Unhandled command for channel {}: {}", channelId, command);
            }
        } catch (Exception e) {
            logger.error("Error handling command for channel {}: {}", channelId, e.getMessage(), e);
        }
    }

    /**
     * Refresh device data from API
     */
    private void refreshDevice() {
        RachioActions localActions = actions;
        RachioDeviceConfiguration localConfig = config;
        
        if (localActions == null || localConfig == null) {
            return;
        }
        
        try {
            // Get device info
            RachioDevice d = localActions.getDevice(localConfig.deviceId);
            if (d != null) {
                device = d;
                updateDeviceState(d);
                updateStatus(ThingStatus.ONLINE);
                
                // Get forecast if available
                try {
                    forecast = localActions.getForecast(localConfig.deviceId);
                    updateForecastState(forecast);
                } catch (Exception e) {
                    logger.debug("Could not get forecast for device {}: {}", localConfig.deviceId, e.getMessage());
                }
                
                // Get usage data for current year
                try {
                    int currentYear = java.time.LocalDate.now().getYear();
                    currentUsage = localActions.getUsage(localConfig.deviceId, currentYear);
                    updateUsageState(currentUsage);
                } catch (Exception e) {
                    logger.debug("Could not get usage data for device {}: {}", localConfig.deviceId, e.getMessage());
                }
                
                // Get savings data for current year
                try {
                    int currentYear = java.time.LocalDate.now().getYear();
                    currentSavings = localActions.getSavings(localConfig.deviceId, currentYear);
                    updateSavingsState(currentSavings);
                } catch (Exception e) {
                    logger.debug("Could not get savings data for device {}: {}", localConfig.deviceId, e.getMessage());
                }
                
            } else {
                logger.warn("Device {} not found", localConfig.deviceId);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Device not found");
            }
            
        } catch (RachioApiException e) {
            logger.error("Error refreshing device {}: {}", localConfig.deviceId, e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error communicating with Rachio API");
        }
    }

    /**
     * Update all channels with device data
     */
    private void updateDeviceState(RachioDevice device) {
        logger.debug("Updating device state for device: {}", device.name);
        
        // Basic device information
        updateState(RachioBindingConstants.CHANNEL_DEVICE_NAME, 
                new StringType(device.name != null ? device.name : ""));
        updateState(RachioBindingConstants.CHANNEL_DEVICE_MODEL, 
                new StringType(device.model != null ? device.model : ""));
        updateState(RachioBindingConstants.CHANNEL_DEVICE_SERIAL, 
                new StringType(device.serialNumber != null ? device.serialNumber : ""));
        
        // Status information
        updateState(RachioBindingConstants.CHANNEL_DEVICE_ONLINE, 
                device.status != null && "ONLINE".equals(device.status) ? OnOffType.ON : OnOffType.OFF);
        updateState(RachioBindingConstants.CHANNEL_DEVICE_STATUS, 
                new StringType(device.status != null ? device.status : "UNKNOWN"));
        updateState(RachioBindingConstants.CHANNEL_DEVICE_PAUSED, 
                device.paused != null && device.paused ? OnOffType.ON : OnOffType.OFF);
        
        // Zone information
        if (device.zones != null) {
            updateState(RachioBindingConstants.CHANNEL_DEVICE_ZONE_COUNT, 
                    new DecimalType(device.zones.size()));
            
            // Count enabled zones
            long enabledZones = device.zones.stream()
                    .filter(z -> z.enabled != null && z.enabled)
                    .count();
            updateState("enabledZones", new DecimalType(enabledZones));
        }
        
        // Rain delay information
        if (device.rainDelayExpirationDate != null) {
            long now = System.currentTimeMillis();
            long expiration = device.rainDelayExpirationDate.toEpochMilli();
            if (expiration > now) {
                long hoursRemaining = (expiration - now) / (1000 * 60 * 60);
                updateState(RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY_HOURS, 
                        new DecimalType(hoursRemaining));
                updateState(RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY, 
                        hoursRemaining > 0 ? OnOffType.ON : OnOffType.OFF);
            } else {
                updateState(RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY_HOURS, 
                        new DecimalType(0));
                updateState(RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY, OnOffType.OFF);
            }
        } else {
            updateState(RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY_HOURS, 
                    new DecimalType(0));
            updateState(RachioBindingConstants.CHANNEL_DEVICE_RAIN_DELAY, OnOffType.OFF);
        }
        
        // Last heard from device
        if (device.lastHeardFromDate != null) {
            updateState(RachioBindingConstants.CHANNEL_DEVICE_LAST_HEARD,
                    new DateTimeType(java.time.ZonedDateTime.ofInstant(
                            device.lastHeardFromDate, java.time.ZoneId.systemDefault())));
        }
        
        // Last updated timestamp
        updateState("lastUpdated", 
                new DateTimeType(java.time.ZonedDateTime.now()));
        
        // Health status
        updateState(RachioBindingConstants.CHANNEL_HEALTH_STATUS, 
                new StringType(calculateHealthStatus(device)));
    }

    /**
     * Update forecast channels
     */
    private void updateForecastState(@Nullable RachioForecast forecast) {
        if (forecast == null) {
            return;
        }
        
        // Update forecast channels if they exist
        // Note: You'll need to add these channels in createDynamicChannels()
        if (forecast.temperature != null) {
            updateState("forecastTemperature", 
                    new QuantityType<>(forecast.temperature, Units.FAHRENHEIT));
        }
        
        if (forecast.precipitation != null) {
            updateState("forecastPrecipitation", 
                    new QuantityType<>(forecast.precipitation, Units.INCH));
        }
        
        if (forecast.evapotranspiration != null) {
            updateState("forecastEvapotranspiration", 
                    new QuantityType<>(forecast.evapotranspiration, Units.INCH));
        }
    }

    /**
     * Update usage channels
     */
    private void updateUsageState(@Nullable RachioUsage usage) {
        if (usage == null) {
            return;
        }
        
        // Update usage channels if they exist
        if (usage.total != null) {
            updateState("waterUsageTotal", 
                    new QuantityType<>(usage.total, Units.CUBIC_METRE));
        }
        
        if (usage.savings != null) {
            updateState("waterSavingsTotal", 
                    new QuantityType<>(usage.savings, Units.CUBIC_METRE));
        }
        
        // You can add more usage metrics as needed
    }

    /**
     * Update savings channels
     */
    private void updateSavingsState(@Nullable RachioUsage savings) {
        if (savings == null) {
            return;
        }
        
        // Update savings channels if they exist
        if (savings.savings != null) {
            updateState("estimatedSavings", 
                    new QuantityType<>(savings.savings, Units.CUBIC_METRE));
            
            // Calculate savings percentage if total usage is available
            if (savings.total != null && savings.total > 0) {
                double percentage = (savings.savings / savings.total) * 100;
                updateState("savingsPercentage", 
                        new QuantityType<>(percentage, Units.PERCENT));
            }
        }
    }

    /**
     * Calculate health status based on device state
     */
    private String calculateHealthStatus(RachioDevice device) {
        if (device.status == null || !"ONLINE".equals(device.status)) {
            return "OFFLINE";
        }
        
        if (device.paused != null && device.paused) {
            return "PAUSED";
        }
        
        if (device.rainDelayExpirationDate != null && 
            device.rainDelayExpirationDate.toEpochMilli() > System.currentTimeMillis()) {
            return "RAIN_DELAY";
        }
        
        // Check if any zones are currently running
        if (device.zones != null) {
            boolean anyZoneRunning = device.zones.stream()
                    .anyMatch(z -> "RUNNING".equals(z.zoneRunStatus));
            if (anyZoneRunning) {
                return "RUNNING";
            }
        }
        
        return "HEALTHY";
    }

    /**
     * Create dynamic channels for device data
     */
    private void createDynamicChannels() {
        List<Channel> channels = new ArrayList<>(getThing().getChannels());
        boolean channelsModified = false;
        
        // Add device-specific channels if they don't exist
        if (channels.stream().noneMatch(c -> "enabledZones".equals(c.getUID().getIdWithoutGroup()))) {
            channels.add(createEnabledZonesChannel());
            channelsModified = true;
        }
        
        if (channels.stream().noneMatch(c -> "forecastTemperature".equals(c.getUID().getIdWithoutGroup()))) {
            channels.addAll(createForecastChannels());
            channelsModified = true;
        }
        
        if (channels.stream().noneMatch(c -> "waterUsageTotal".equals(c.getUID().getIdWithoutGroup()))) {
            channels.addAll(createUsageChannels());
            channelsModified = true;
        }
        
        if (channelsModified) {
            updateThing(editThing().withChannels(channels).build());
            logger.debug("Added dynamic channels to device {}", getThing().getUID());
        }
    }

    /**
     * Create enabled zones channel
     */
    private Channel createEnabledZonesChannel() {
        ChannelBuilder builder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "enabledZones"), "Number")
                .withType(new ChannelTypeUID("rachio", "enabled-zones"))
                .withLabel("Enabled Zones")
                .withDescription("Number of enabled zones on this device")
                .withProperties(Map.of("category", "device", "tags", "Zones,Status"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0))
                        .withMaximum(BigDecimal.valueOf(16))
                        .withStep(BigDecimal.valueOf(1))
                        .withPattern("%d")
                        .build()
                        .toStateDescription());
        return builder.build();
    }

    /**
     * Create forecast channels
     */
    private List<Channel> createForecastChannels() {
        List<Channel> channels = new ArrayList<>();
        
        // Temperature forecast
        ChannelBuilder tempBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "forecastTemperature"), "Number:Temperature")
                .withType(new ChannelTypeUID("rachio", "forecast-temperature"))
                .withLabel("Forecast Temperature")
                .withDescription("Forecast temperature")
                .withProperties(Map.of("category", "weather", "tags", "Weather,Forecast"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(-20))
                        .withMaximum(BigDecimal.valueOf(120))
                        .withStep(BigDecimal.valueOf(1))
                        .withPattern("%.1f %unit%")
                        .build()
                        .toStateDescription());
        channels.add(tempBuilder.build());
        
        // Precipitation forecast
        ChannelBuilder precipBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "forecastPrecipitation"), "Number:Length")
                .withType(new ChannelTypeUID("rachio", "forecast-precipitation"))
                .withLabel("Forecast Precipitation")
                .withDescription("Forecast precipitation in inches")
                .withProperties(Map.of("category", "weather", "tags", "Weather,Forecast"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0))
                        .withMaximum(BigDecimal.valueOf(10))
                        .withStep(BigDecimal.valueOf(0.1))
                        .withPattern("%.2f %unit%")
                        .build()
                        .toStateDescription());
        channels.add(precipBuilder.build());
        
        // Evapotranspiration forecast
        ChannelBuilder etBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "forecastEvapotranspiration"), "Number:Length")
                .withType(new ChannelTypeUID("rachio", "forecast-et"))
                .withLabel("Forecast ET")
                .withDescription("Forecast evapotranspiration")
                .withProperties(Map.of("category", "weather", "tags", "Weather,Forecast"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0))
                        .withMaximum(BigDecimal.valueOf(1))
                        .withStep(BigDecimal.valueOf(0.01))
                        .withPattern("%.2f %unit%")
                        .build()
                        .toStateDescription());
        channels.add(etBuilder.build());
        
        return channels;
    }

    /**
     * Create usage channels
     */
    private List<Channel> createUsageChannels() {
        List<Channel> channels = new ArrayList<>();
        
        // Total water usage
        ChannelBuilder usageBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "waterUsageTotal"), "Number:Volume")
                .withType(new ChannelTypeUID("rachio", "water-usage"))
                .withLabel("Water Usage")
                .withDescription("Total water usage")
                .withProperties(Map.of("category", "usage", "tags", "Water,Usage"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0))
                        .withMaximum(BigDecimal.valueOf(1000))
                        .withStep(BigDecimal.valueOf(1))
                        .withPattern("%.0f %unit%")
                        .build()
                        .toStateDescription());
        channels.add(usageBuilder.build());
        
        // Water savings
        ChannelBuilder savingsBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "waterSavingsTotal"), "Number:Volume")
                .withType(new ChannelTypeUID("rachio", "water-savings"))
                .withLabel("Water Savings")
                .withDescription("Total water savings")
                .withProperties(Map.of("category", "usage", "tags", "Water,Savings"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0))
                        .withMaximum(BigDecimal.valueOf(1000))
                        .withStep(BigDecimal.valueOf(1))
                        .withPattern("%.0f %unit%")
                        .build()
                        .toStateDescription());
        channels.add(savingsBuilder.build());
        
        // Estimated savings
        ChannelBuilder estimatedBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "estimatedSavings"), "Number:Volume")
                .withType(new ChannelTypeUID("rachio", "estimated-savings"))
                .withLabel("Estimated Savings")
                .withDescription("Estimated water savings")
                .withProperties(Map.of("category", "usage", "tags", "Water,Savings"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0))
                        .withMaximum(BigDecimal.valueOf(1000))
                        .withStep(BigDecimal.valueOf(1))
                        .withPattern("%.0f %unit%")
                        .build()
                        .toStateDescription());
        channels.add(estimatedBuilder.build());
        
        // Savings percentage
        ChannelBuilder percentageBuilder = ChannelBuilder
                .create(new ChannelUID(getThing().getUID(), "savingsPercentage"), "Number:Dimensionless")
                .withType(new ChannelTypeUID("rachio", "savings-percentage"))
                .withLabel("Savings Percentage")
                .withDescription("Percentage of water saved")
                .withProperties(Map.of("category", "usage", "tags", "Water,Savings"))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withMinimum(BigDecimal.valueOf(0))
                        .withMaximum(BigDecimal.valueOf(100))
                        .withStep(BigDecimal.valueOf(1))
                        .withPattern("%d %%")
                        .build()
                        .toStateDescription());
        channels.add(percentageBuilder.build());
        
        return channels;
    }

    /**
     * Start polling for device updates
     */
    private void startPolling() {
        stopPolling(); // Ensure no existing polling job
        
        pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshDevice();
            } catch (Exception e) {
                logger.error("Error during device polling: {}", e.getMessage(), e);
            }
        }, 0, 120, TimeUnit.SECONDS); // Poll every 120 seconds for devices
        
        logger.debug("Started polling for device {}", getThing().getUID());
    }

    /**
     * Stop polling
     */
    private void stopPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null) {
            localPollingJob.cancel(true);
            pollingJob = null;
            logger.debug("Stopped polling for device {}", getThing().getUID());
        }
    }

    /**
     * Handle webhook event
     */
    private void handleWebhookEvent(org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent event) {
        logger.debug("Device {} received webhook event: {}", config != null ? config.deviceId : "unknown", event.type);
        
        // Refresh device data when webhook events arrive
        scheduler.schedule(() -> refreshDevice(), 2, TimeUnit.SECONDS);
    }

    /**
     * Get current device data
     */
    public @Nullable RachioDevice getDevice() {
        return device;
    }

    /**
     * Get device configuration
     */
    public @Nullable RachioDeviceConfiguration getDeviceConfig() {
        return config;
    }

    /**
     * Get bridge handler
     */
    public @Nullable RachioBridgeHandler getBridgeHandler() {
        return bridgeHandler;
    }
}
