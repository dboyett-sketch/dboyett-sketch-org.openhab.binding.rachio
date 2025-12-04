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
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link RachioDeviceHandler} is responsible for handling commands for device things
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite for OpenHAB 5.x
 */
@Component(service = RachioDeviceHandler.class, configurationPid = "handler.rachio.device")
@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler implements RachioStatusListener {
    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    // Configuration
    private @Nullable RachioDeviceConfiguration config;

    // Bridge Handler
    private @Nullable RachioBridgeHandler bridgeHandler;

    // Scheduling
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> forecastJob;
    private @Nullable ScheduledFuture<?> analyticsJob;
    private static final int POLLING_INTERVAL = 120; // 2 minutes
    private static final int FORECAST_INTERVAL = 3600; // 1 hour
    private static final int ANALYTICS_INTERVAL = 7200; // 2 hours

    // State tracking
    private @Nullable RachioDevice lastDeviceState;
    private @Nullable ZonedDateTime lastUpdate;
    private @Nullable JsonObject lastForecast;
    private @Nullable JsonObject lastWaterUsage;
    private @Nullable JsonObject lastSavings;
    private @Nullable JsonArray lastAlerts;
    private int rainDelayHours = 0;
    private boolean devicePaused = false;

    // JSON parser
    private final Gson gson;

    @Activate
    public RachioDeviceHandler(Thing thing) {
        super(thing);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler for {}", getThing().getUID());

        config = getConfigAs(RachioDeviceConfiguration.class);
        if (config == null || config.deviceId.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Device ID not configured");
            return;
        }

        // Get bridge handler
        RachioBridgeHandler bridge = getBridgeHandler();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not found");
            return;
        }

        // Register with bridge
        bridge.registerStatusListener(this);

        // Create professional channels
        createProfessionalChannels();

        // Start background jobs
        startPolling();
        startForecastUpdates();
        startAnalyticsUpdates();

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        stopPolling();
        stopForecastUpdates();
        stopAnalyticsUpdates();

        // Unregister from bridge
        RachioBridgeHandler bridge = getBridgeHandler();
        if (bridge != null) {
            bridge.unregisterStatusListener(this);
        }

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
     * Create dynamic channels for professional features
     */
    private void createProfessionalChannels() {
        logger.debug("Creating professional channels for device {}", config != null ? config.deviceId : "unknown");

        // Control Channels
        createControlChannel(CHANNEL_DEVICE_STOP_WATER, CHANNEL_TYPE_UID_SWITCH,
                "Stop Watering", "Immediately stop all watering", "switch");
        createControlChannel(CHANNEL_DEVICE_RUN_ALL_ZONES, CHANNEL_TYPE_UID_NUMBER,
                "Run All Zones", "Run all zones for specified duration (seconds)", "number:time");
        createControlChannel(CHANNEL_DEVICE_RUN_NEXT_ZONE, CHANNEL_TYPE_UID_NUMBER,
                "Run Next Zone", "Run next available zone (seconds)", "number:time");
        createControlChannel(CHANNEL_DEVICE_SET_RAIN_DELAY, CHANNEL_TYPE_UID_NUMBER,
                "Set Rain Delay", "Set rain delay in hours", "number:time");
        createControlChannel(CHANNEL_DEVICE_PAUSE, CHANNEL_TYPE_UID_SWITCH,
                "Pause Device", "Pause/resume device operation", "switch");

        // Weather & Forecast Channels
        createReadOnlyChannel(CHANNEL_DEVICE_FORECAST, CHANNEL_TYPE_UID_STRING,
                "Weather Forecast", "Current weather forecast summary", "text");
        createReadOnlyChannel(CHANNEL_DEVICE_WEATHER_INTEL, CHANNEL_TYPE_UID_STRING,
                "Weather Intel", "Smart watering intelligence status", "text");

        // Water Analytics Channels
        createReadOnlyChannel(CHANNEL_DEVICE_WATER_USAGE, CHANNEL_TYPE_UID_STRING,
                "Water Usage", "Water usage analytics", "text");
        createReadOnlyChannel(CHANNEL_DEVICE_WATER_SAVINGS, CHANNEL_TYPE_UID_STRING,
                "Water Savings", "Water savings analytics", "text");

        // Alert Channels
        createReadOnlyChannel(CHANNEL_DEVICE_ALERTS, CHANNEL_TYPE_UID_STRING,
                "Device Alerts", "Active device alerts", "text");

        // Schedule Channels
        createReadOnlyChannel(CHANNEL_DEVICE_SCHEDULE_STATUS, CHANNEL_TYPE_UID_STRING,
                "Schedule Status", "Current schedule execution status", "text");

        logger.debug("Professional channels created for device {}", config != null ? config.deviceId : "unknown");
    }

    /**
     * Create a read-only channel
     */
    private void createReadOnlyChannel(String channelId, ChannelTypeUID channelTypeUID,
            String label, String description, String itemType) {
        ChannelUID uid = new ChannelUID(getThing().getUID(), channelId);

        if (getThing().getChannel(uid) != null) {
            return;
        }

        Channel channel = ChannelBuilder.create(uid, itemType)
                .withType(channelTypeUID)
                .withLabel(label)
                .withDescription(description)
                .build();

        updateThing(editThing().withChannel(channel).build());
    }

    /**
     * Create a control channel
     */
    private void createControlChannel(String channelId, ChannelTypeUID channelTypeUID,
            String label, String description, String itemType) {
        ChannelUID uid = new ChannelUID(getThing().getUID(), channelId);

        if (getThing().getChannel(uid) != null) {
            return;
        }

        Channel channel = ChannelBuilder.create(uid, itemType)
                .withType(channelTypeUID)
                .withLabel(label)
                .withDescription(description)
                .build();

        updateThing(editThing().withChannel(channel).build());
    }

    /**
     * Start polling for device updates
     */
    private void startPolling() {
        ScheduledFuture<?> job = pollingJob;
        if (job == null || job.isCancelled()) {
            pollingJob = scheduler.scheduleWithFixedDelay(this::pollDeviceState, 10, POLLING_INTERVAL, TimeUnit.SECONDS);
            logger.debug("Started device polling for {}", config != null ? config.deviceId : "unknown");
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
            logger.debug("Stopped device polling");
        }
    }

    /**
     * Start forecast updates
     */
    private void startForecastUpdates() {
        ScheduledFuture<?> job = forecastJob;
        if (job == null || job.isCancelled()) {
            forecastJob = scheduler.scheduleWithFixedDelay(this::updateForecast, 30, FORECAST_INTERVAL, TimeUnit.SECONDS);
            logger.debug("Started forecast updates");
        }
    }

    /**
     * Stop forecast updates
     */
    private void stopForecastUpdates() {
        ScheduledFuture<?> job = forecastJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            forecastJob = null;
            logger.debug("Stopped forecast updates");
        }
    }

    /**
     * Start analytics updates
     */
    private void startAnalyticsUpdates() {
        ScheduledFuture<?> job = analyticsJob;
        if (job == null || job.isCancelled()) {
            analyticsJob = scheduler.scheduleWithFixedDelay(this::updateAnalytics, 60, ANALYTICS_INTERVAL, TimeUnit.SECONDS);
            logger.debug("Started analytics updates");
        }
    }

    /**
     * Stop analytics updates
     */
    private void stopAnalyticsUpdates() {
        ScheduledFuture<?> job = analyticsJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            analyticsJob = null;
            logger.debug("Stopped analytics updates");
        }
    }

    /**
     * Poll device state from bridge
     */
    private void pollDeviceState() {
        RachioBridgeHandler bridge = getBridgeHandler();
        RachioDeviceConfiguration localConfig = config;

        if (bridge == null || localConfig == null) {
            logger.debug("Cannot poll device state: bridge or config missing");
            return;
        }

        try {
            // Get device from bridge
            List<RachioDevice> devices = bridge.getDevices();
            RachioDevice device = devices.stream()
                    .filter(d -> d.getId().equals(localConfig.deviceId))
                    .findFirst()
                    .orElse(null);

            if (device != null) {
                updateDeviceState(device);
                lastDeviceState = device;
                lastUpdate = ZonedDateTime.now();
            } else {
                logger.warn("Device {} not found in bridge", localConfig.deviceId);
            }
        } catch (Exception e) {
            logger.debug("Error polling device state: {}", e.getMessage(), e);
        }
    }

    /**
     * Update forecast data
     */
    private void updateForecast() {
        RachioBridgeHandler bridge = getBridgeHandler();
        RachioDeviceConfiguration localConfig = config;

        if (bridge == null || localConfig == null) {
            return;
        }

        try {
            JsonObject forecast = bridge.getDeviceForecast(localConfig.deviceId);
            if (forecast != null) {
                lastForecast = forecast;
                updateForecastChannels(forecast);
                logger.debug("Updated forecast for device {}", localConfig.deviceId);
            }
        } catch (RachioApiException e) {
            logger.debug("Error updating forecast: {}", e.getMessage());
        }
    }

    /**
     * Update analytics data (water usage and savings)
     */
    private void updateAnalytics() {
        RachioBridgeHandler bridge = getBridgeHandler();
        RachioDeviceConfiguration localConfig = config;

        if (bridge == null || localConfig == null) {
            return;
        }

        try {
            // Update water usage
            JsonObject waterUsage = bridge.getDeviceWaterUsage(localConfig.deviceId);
            if (waterUsage != null) {
                lastWaterUsage = waterUsage;
                updateWaterUsageChannels(waterUsage);
            }

            // Update savings
            JsonObject savings = bridge.getDeviceSavings(localConfig.deviceId);
            if (savings != null) {
                lastSavings = savings;
                updateSavingsChannels(savings);
            }

            // Update alerts
            JsonArray alerts = bridge.getDeviceAlerts(localConfig.deviceId);
            if (alerts != null) {
                lastAlerts = alerts;
                updateAlertChannels(alerts);
            }

            logger.debug("Updated analytics for device {}", localConfig.deviceId);
        } catch (RachioApiException e) {
            logger.debug("Error updating analytics: {}", e.getMessage());
        }
    }

    /**
     * Update all device channels with current state
     */
    protected void updateDeviceState(RachioDevice device) {
        logger.trace("Updating device state for {}: {}", device.getId(), device.getName());

        // Basic device information
        updateState(CHANNEL_DEVICE_NAME, new StringType(device.getName()));
        updateState(CHANNEL_DEVICE_ONLINE, device.isOnline() ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_DEVICE_PAUSED, device.isPaused() ? OnOffType.ON : OnOffType.OFF);
        updateState(CHANNEL_DEVICE_STATUS, new StringType(device.getStatus()));

        // Device properties
        updateState(CHANNEL_DEVICE_ZONE_COUNT, new DecimalType(device.getZones().size()));
        updateState(CHANNEL_DEVICE_SERIAL_NUMBER, new StringType(device.getSerialNumber()));
        updateState(CHANNEL_DEVICE_MODEL, new StringType(device.getModel()));
        updateState(CHANNEL_DEVICE_MAC_ADDRESS, new StringType(device.getMacAddress()));
        
        if (device.getCreatedDate() != null) {
            updateState(CHANNEL_DEVICE_CREATED_DATE, new DateTimeType(device.getCreatedDate()));
        }
        
        if (device.getUpdatedDate() != null) {
            updateState(CHANNEL_DEVICE_UPDATED_DATE, new DateTimeType(device.getUpdatedDate()));
        }
        
        if (device.getLastHeardFrom() != null) {
            updateState(CHANNEL_DEVICE_LAST_HEARD_FROM, new DateTimeType(device.getLastHeardFrom()));
        }

        // Rain delay
        if (device.getRainDelayExpirationDate() != null) {
            long remainingSeconds = java.time.Duration.between(ZonedDateTime.now(), device.getRainDelayExpirationDate()).getSeconds();
            rainDelayHours = (int) Math.max(0, remainingSeconds / 3600);
            updateState(CHANNEL_DEVICE_RAIN_DELAY, new QuantityType<>(rainDelayHours, Units.HOUR));
            updateState(CHANNEL_DEVICE_RAIN_DELAY_HOURS, new DecimalType(rainDelayHours));
        } else {
            rainDelayHours = 0;
            updateState(CHANNEL_DEVICE_RAIN_DELAY, new QuantityType<>(0, Units.HOUR));
            updateState(CHANNEL_DEVICE_RAIN_DELAY_HOURS, new DecimalType(0));
        }

        // Update device paused state
        devicePaused = device.isPaused();
        updateState(CHANNEL_DEVICE_PAUSED, devicePaused ? OnOffType.ON : OnOffType.OFF);

        logger.debug("Device {} state updated: name={}, online={}, zones={}, rainDelay={}h",
                device.getId(), device.getName(), device.isOnline(), device.getZones().size(), rainDelayHours);
    }

    /**
     * Update forecast channels
     */
    private void updateForecastChannels(JsonObject forecast) {
        if (forecast.has("summary") && forecast.get("summary").isJsonObject()) {
            JsonObject summary = forecast.getAsJsonObject("summary");
            if (summary.has("condition") && summary.has("temperature") && summary.has("precipitation")) {
                String condition = summary.get("condition").getAsString();
                double temperature = summary.get("temperature").getAsDouble();
                double precipitation = summary.get("precipitation").getAsDouble();
                
                String forecastText = String.format("%s, %.0f°F, %.2f\" rain", condition, temperature, precipitation);
                updateState(CHANNEL_DEVICE_FORECAST, new StringType(forecastText));
            }
        }

        // Weather intelligence (smart skip)
        if (forecast.has("weatherIntel") && forecast.get("weatherIntel").isJsonObject()) {
            JsonObject weatherIntel = forecast.getAsJsonObject("weatherIntel");
            if (weatherIntel.has("status")) {
                String status = weatherIntel.get("status").getAsString();
                updateState(CHANNEL_DEVICE_WEATHER_INTEL, new StringType(status));
            }
        }
    }

    /**
     * Update water usage channels
     */
    private void updateWaterUsageChannels(JsonObject waterUsage) {
        StringBuilder usageText = new StringBuilder();
        
        if (waterUsage.has("total") && waterUsage.get("total").isJsonObject()) {
            JsonObject total = waterUsage.getAsJsonObject("total");
            if (total.has("gallons") && total.has("cost")) {
                double gallons = total.get("gallons").getAsDouble();
                double cost = total.get("cost").getAsDouble();
                usageText.append(String.format("Total: %.0f gal ($%.2f)", gallons, cost));
            }
        }
        
        if (waterUsage.has("average") && waterUsage.get("average").isJsonObject()) {
            JsonObject average = waterUsage.getAsJsonObject("average");
            if (average.has("gallons") && average.has("cost")) {
                double gallons = average.get("gallons").getAsDouble();
                double cost = average.get("cost").getAsDouble();
                if (usageText.length() > 0) {
                    usageText.append(" | ");
                }
                usageText.append(String.format("Avg: %.0f gal ($%.2f)", gallons, cost));
            }
        }

        if (usageText.length() > 0) {
            updateState(CHANNEL_DEVICE_WATER_USAGE, new StringType(usageText.toString()));
        }
    }

    /**
     * Update savings channels
     */
    private void updateSavingsChannels(JsonObject savings) {
        StringBuilder savingsText = new StringBuilder();
        
        if (savings.has("total") && savings.get("total").isJsonObject()) {
            JsonObject total = savings.getAsJsonObject("total");
            if (total.has("gallons") && total.has("cost")) {
                double gallons = total.get("gallons").getAsDouble();
                double cost = total.get("cost").getAsDouble();
                savingsText.append(String.format("Total: %.0f gal ($%.2f)", gallons, cost));
            }
        }
        
        if (savings.has("percentage") && savings.has("comparedTo")) {
            double percentage = savings.get("percentage").getAsDouble();
            String comparedTo = savings.get("comparedTo").getAsString();
            if (savingsText.length() > 0) {
                savingsText.append(" | ");
            }
            savingsText.append(String.format("%.1f%% vs %s", percentage, comparedTo));
        }

        if (savingsText.length() > 0) {
            updateState(CHANNEL_DEVICE_WATER_SAVINGS, new StringType(savingsText.toString()));
        }
    }

    /**
     * Update alert channels
     */
    private void updateAlertChannels(JsonArray alerts) {
        if (alerts.size() == 0) {
            updateState(CHANNEL_DEVICE_ALERTS, new StringType("No active alerts"));
            return;
        }

        StringBuilder alertsText = new StringBuilder();
        for (int i = 0; i < Math.min(alerts.size(), 3); i++) { // Show max 3 alerts
            JsonObject alert = alerts.get(i).getAsJsonObject();
            if (alert.has("type") && alert.has("message")) {
                if (alertsText.length() > 0) {
                    alertsText.append("; ");
                }
                alertsText.append(alert.get("message").getAsString());
            }
        }

        if (alertsText.length() > 0) {
            updateState(CHANNEL_DEVICE_ALERTS, new StringType(alertsText.toString()));
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        RachioBridgeHandler bridge = getBridgeHandler();
        RachioDeviceConfiguration localConfig = config;

        if (bridge == null || localConfig == null) {
            logger.warn("Cannot handle command: bridge or config missing");
            return;
        }

        if (command instanceof RefreshType) {
            // Refresh device state
            scheduler.execute(this::pollDeviceState);
            return;
        }

        String channelId = channelUID.getIdWithoutGroup();
        logger.debug("Handling command {} for channel {} on device {}", command, channelId, localConfig.deviceId);

        try {
            switch (channelId) {
                case CHANNEL_DEVICE_STOP_WATER:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        bridge.stopWatering(getThing().getUID().getId(), localConfig.deviceId);
                        // Turn switch back off after command
                        scheduler.schedule(() -> updateState(channelUID, OnOffType.OFF), 1, TimeUnit.SECONDS);
                    }
                    break;

                case CHANNEL_DEVICE_RUN_ALL_ZONES:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        bridge.runAllZones(getThing().getUID().getId(), duration, localConfig.deviceId);
                    }
                    break;

                case CHANNEL_DEVICE_RUN_NEXT_ZONE:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        bridge.runNextZone(getThing().getUID().getId(), duration, localConfig.deviceId);
                    }
                    break;

                case CHANNEL_DEVICE_SET_RAIN_DELAY:
                    if (command instanceof DecimalType) {
                        int hours = ((DecimalType) command).intValue();
                        bridge.rainDelay(getThing().getUID().getId(), hours, localConfig.deviceId);
                        // Update local state
                        rainDelayHours = hours;
                        updateState(CHANNEL_DEVICE_RAIN_DELAY, new QuantityType<>(hours, Units.HOUR));
                        updateState(CHANNEL_DEVICE_RAIN_DELAY_HOURS, new DecimalType(hours));
                    }
                    break;

                case CHANNEL_DEVICE_PAUSE:
                    if (command instanceof OnOffType) {
                        boolean pause = command == OnOffType.ON;
                        bridge.pauseDevice(getThing().getUID().getId(), pause, localConfig.deviceId);
                        // Update local state
                        devicePaused = pause;
                        updateState(CHANNEL_DEVICE_PAUSED, pause ? OnOffType.ON : OnOffType.OFF);
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
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.emptyList();
    }

    @Override
    public void onStatusChanged(ThingStatus status) {
        // Bridge status changed
        if (status == ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
            startPolling();
            startForecastUpdates();
            startAnalyticsUpdates();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            stopPolling();
            stopForecastUpdates();
            stopAnalyticsUpdates();
        }
    }

    @Override
    public void onZoneStateUpdated(RachioZone zone) {
        // Zone state updated via webhook - update device if zone belongs to this device
        if (lastDeviceState != null && lastDeviceState.getZones().stream()
                .anyMatch(z -> z.getId().equals(zone.getId()))) {
            // Refresh device state to get updated zone status
            scheduler.execute(this::pollDeviceState);
        }
    }

    @Override
    public void onDeviceStateUpdated() {
        // Device state updated via bridge - refresh our state
        scheduler.execute(this::pollDeviceState);
    }

    /**
     * Get the current device state
     */
    public @Nullable RachioDevice getDeviceState() {
        return lastDeviceState;
    }

    /**
     * Get last update time
     */
    public @Nullable ZonedDateTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Get device configuration
     */
    public @Nullable RachioDeviceConfiguration getDeviceConfig() {
        return config;
    }

    /**
     * Get current forecast data
     */
    public @Nullable JsonObject getForecast() {
        return lastForecast;
    }

    /**
     * Get current water usage data
     */
    public @Nullable JsonObject getWaterUsage() {
        return lastWaterUsage;
    }

    /**
     * Get current savings data
     */
    public @Nullable JsonObject getSavings() {
        return lastSavings;
    }

    /**
     * Get current alerts
     */
    public @Nullable JsonArray getAlerts() {
        return lastAlerts;
    }
}
