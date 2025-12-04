package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
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
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link RachioBridgeHandler} is responsible for bridge communication with Rachio Cloud API
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite for OpenHAB 5.x
 */
@Component(service = RachioBridgeHandler.class, configurationPid = "handler.rachio")
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    // Configuration
    private @Nullable RachioBridgeConfiguration config;

    // API Client
    private final RachioHttp rachioHttp;
    private final Gson gson;

    // Rate limiting
    private int rateLimitTotal = API_RATE_LIMIT_DAILY;
    private int rateLimitRemaining = API_RATE_LIMIT_DAILY;
    private Instant rateLimitReset = Instant.now().plus(Duration.ofDays(1));
    private boolean rateLimitCritical = false;
    private final Map<String, Integer> endpointUsage = new ConcurrentHashMap<>();

    // Status listeners
    private final List<RachioStatusListener> statusListeners = new CopyOnWriteArrayList<>();

    // Scheduling
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> webhookHealthJob;
    private static final int DEFAULT_POLLING_INTERVAL = 60; // seconds
    private static final int WEBHOOK_HEALTH_CHECK_INTERVAL = 300; // 5 minutes

    // State tracking
    private @Nullable RachioPerson personInfo;
    private final List<RachioDevice> devices = new ArrayList<>();
    private @Nullable String webhookId;
    private boolean webhookRegistered = false;
    private Instant lastApiCall = Instant.now();
    private Instant lastSuccessfulCall = Instant.now();

    @Activate
    public RachioBridgeHandler(@Reference RachioHttp rachioHttp, Bridge bridge) {
        super(bridge);
        this.rachioHttp = rachioHttp;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler for {}", getThing().getUID());

        config = getConfigAs(RachioBridgeConfiguration.class);
        if (config == null || config.apiKey == null || config.apiKey.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API key not configured");
            return;
        }

        // Create monitoring channels
        createMonitoringChannels();

        // Start background jobs
        startPolling();
        startWebhookHealthCheck();

        // Initial validation
        scheduler.execute(this::validateConnection);

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        stopPolling();
        stopWebhookHealthCheck();
        
        // Clean up webhook if configured
        if (webhookRegistered && config != null && config.apiKey != null) {
            unregisterWebhook();
        }
        
        super.dispose();
    }

    /**
     * Create monitoring channels for bridge
     */
    private void createMonitoringChannels() {
        logger.debug("Creating monitoring channels for bridge");

        // Rate limiting channels
        createMonitoringChannel(CHANNEL_BRIDGE_RATE_LIMIT_TOTAL, CHANNEL_TYPE_UID_NUMBER,
                "Rate Limit Total", "Total daily API requests allowed", "number");
        createMonitoringChannel(CHANNEL_BRIDGE_RATE_LIMIT_REMAINING, CHANNEL_TYPE_UID_NUMBER,
                "Rate Limit Remaining", "Remaining API requests today", "number");
        createMonitoringChannel(CHANNEL_BRIDGE_RATE_LIMIT_PERCENT, CHANNEL_TYPE_UID_NUMBER,
                "Rate Limit Percent", "Percentage of API requests used", "number:dimensionless");
        createMonitoringChannel(CHANNEL_BRIDGE_RATE_LIMIT_RESET, CHANNEL_TYPE_UID_DATETIME,
                "Rate Limit Reset", "Time when rate limit resets", "datetime");

        // Status channels
        createMonitoringChannel(CHANNEL_BRIDGE_STATUS, CHANNEL_TYPE_UID_STRING,
                "Bridge Status", "Overall bridge status", "text");
        createMonitoringChannel(CHANNEL_BRIDGE_WEBHOOK_STATUS, CHANNEL_TYPE_UID_STRING,
                "Webhook Status", "Webhook registration status", "text");
        createMonitoringChannel(CHANNEL_BRIDGE_API_STATUS, CHANNEL_TYPE_UID_STRING,
                "API Status", "Last API call status", "text");
        createMonitoringChannel(CHANNEL_BRIDGE_POLLING_STATUS, CHANNEL_TYPE_UID_STRING,
                "Polling Status", "Background polling status", "text");

        // Statistics channels
        createMonitoringChannel(CHANNEL_BRIDGE_LAST_UPDATE, CHANNEL_TYPE_UID_DATETIME,
                "Last Update", "Last successful update", "datetime");
        createMonitoringChannel(CHANNEL_BRIDGE_WEBHOOK_COUNT, CHANNEL_TYPE_UID_NUMBER,
                "Webhook Count", "Number of webhooks received", "number");

        logger.debug("Monitoring channels created for bridge");
    }

    /**
     * Create a monitoring channel
     */
    private void createMonitoringChannel(String channelId, ChannelTypeUID channelTypeUID,
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
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || !localConfig.pollingEnabled) {
            logger.debug("Polling disabled for bridge");
            return;
        }

        ScheduledFuture<?> job = pollingJob;
        if (job == null || job.isCancelled()) {
            int interval = localConfig.refreshInterval > 0 ? localConfig.refreshInterval : DEFAULT_POLLING_INTERVAL;
            pollingJob = scheduler.scheduleWithFixedDelay(this::pollDevices, 10, interval, TimeUnit.SECONDS);
            logger.debug("Started polling with {} second interval", interval);
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
            logger.debug("Stopped polling");
        }
    }

    /**
     * Start webhook health check
     */
    private void startWebhookHealthCheck() {
        ScheduledFuture<?> job = webhookHealthJob;
        if (job == null || job.isCancelled()) {
            webhookHealthJob = scheduler.scheduleWithFixedDelay(this::checkWebhookHealth,
                    60, WEBHOOK_HEALTH_CHECK_INTERVAL, TimeUnit.SECONDS);
            logger.debug("Started webhook health check");
        }
    }

    /**
     * Stop webhook health check
     */
    private void stopWebhookHealthCheck() {
        ScheduledFuture<?> job = webhookHealthJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            webhookHealthJob = null;
            logger.debug("Stopped webhook health check");
        }
    }

    /**
     * Validate API connection and get person info
     */
    private void validateConnection() {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null) {
            return;
        }

        try {
            personInfo = rachioHttp.getPersonInfo(localConfig.apiKey);
            if (personInfo != null) {
                logger.info("Successfully connected to Rachio API for user: {}", personInfo.getUsername());
                updateStatus(ThingStatus.ONLINE);
                
                // Register webhook if configured
                if (localConfig.webhookUrl != null && !localConfig.webhookUrl.isEmpty()) {
                    registerWebhook();
                }
                
                // Initial device poll
                pollDevices();
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Failed to get person info");
            }
        } catch (RachioApiException e) {
            logger.warn("Failed to validate Rachio API connection: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * Poll devices from Rachio API
     */
    private void pollDevices() {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null) {
            return;
        }

        // Check rate limits before polling
        if (rateLimitCritical && rateLimitRemaining < 10) {
            logger.debug("Skipping poll due to critical rate limit: {} remaining", rateLimitRemaining);
            updateState(CHANNEL_BRIDGE_POLLING_STATUS, new StringType("PAUSED - Rate Limit Critical"));
            return;
        }

        try {
            List<RachioDevice> newDevices = rachioHttp.getDevices(localConfig.apiKey);
            devices.clear();
            devices.addAll(newDevices);

            // Update device handlers
            for (RachioDevice device : newDevices) {
                notifyDeviceUpdated(device);
            }

            // Update monitoring channels
            updateMonitoringChannels();
            
            // Update last successful call
            lastSuccessfulCall = Instant.now();
            updateState(CHANNEL_BRIDGE_LAST_UPDATE, new DateTimeType(ZonedDateTime.now()));
            updateState(CHANNEL_BRIDGE_POLLING_STATUS, new StringType("ACTIVE"));

            logger.debug("Successfully polled {} devices", newDevices.size());
        } catch (RachioApiException e) {
            logger.warn("Failed to poll devices: {}", e.getMessage());
            updateState(CHANNEL_BRIDGE_POLLING_STATUS, new StringType("ERROR - " + e.getMessage()));
        }
    }

    /**
     * Update monitoring channels
     */
    private void updateMonitoringChannels() {
        // Rate limiting
        updateState(CHANNEL_BRIDGE_RATE_LIMIT_TOTAL, new DecimalType(rateLimitTotal));
        updateState(CHANNEL_BRIDGE_RATE_LIMIT_REMAINING, new DecimalType(rateLimitRemaining));
        
        if (rateLimitTotal > 0) {
            double percentUsed = ((double) (rateLimitTotal - rateLimitRemaining) / rateLimitTotal) * 100;
            updateState(CHANNEL_BRIDGE_RATE_LIMIT_PERCENT, new QuantityType<>(percentUsed, Units.PERCENT));
        }
        
        updateState(CHANNEL_BRIDGE_RATE_LIMIT_RESET, new DateTimeType(ZonedDateTime.ofInstant(rateLimitReset, null)));

        // Status
        String bridgeStatus = getThing().getStatus().toString();
        updateState(CHANNEL_BRIDGE_STATUS, new StringType(bridgeStatus));
        
        String webhookStatus = webhookRegistered ? "REGISTERED" : "NOT REGISTERED";
        if (webhookId != null) {
            webhookStatus += " (ID: " + webhookId + ")";
        }
        updateState(CHANNEL_BRIDGE_WEBHOOK_STATUS, new StringType(webhookStatus));
        
        updateState(CHANNEL_BRIDGE_API_STATUS, new StringType("LAST: " + 
                DateTimeFormatter.ofPattern("HH:mm:ss").format(lastApiCall)));
    }

    /**
     * Update rate limits from API response headers
     */
    public void updateRateLimits(@Nullable String totalHeader, @Nullable String remainingHeader, 
                                @Nullable String resetHeader) {
        try {
            if (totalHeader != null) {
                rateLimitTotal = Integer.parseInt(totalHeader);
            }
            if (remainingHeader != null) {
                rateLimitRemaining = Integer.parseInt(remainingHeader);
                rateLimitCritical = rateLimitRemaining < 50; // Critical when < 50 remaining
            }
            if (resetHeader != null) {
                rateLimitReset = Instant.ofEpochSecond(Long.parseLong(resetHeader));
            }
            
            lastApiCall = Instant.now();
            
            // Update monitoring channels
            scheduler.execute(this::updateMonitoringChannels);
            
            // Notify listeners if rate limit critical
            if (rateLimitCritical) {
                notifyRateLimitCritical();
            }
            
            logger.trace("Rate limits updated: {}/{} remaining, resets at {}", 
                    rateLimitRemaining, rateLimitTotal, rateLimitReset);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse rate limit headers: total={}, remaining={}, reset={}", 
                    totalHeader, remainingHeader, resetHeader);
        }
    }

    /**
     * Register webhook with Rachio API
     */
    private void registerWebhook() {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null || 
                localConfig.webhookUrl == null || localConfig.webhookUrl.isEmpty()) {
            logger.debug("Webhook not configured, skipping registration");
            return;
        }

        try {
            // Build webhook registration request
            String url = API_BASE_URL + "/webhook";
            
            JsonObject webhookRequest = new JsonObject();
            webhookRequest.addProperty("url", localConfig.webhookUrl);
            webhookRequest.addProperty("eventTypes", "DEVICE_STATUS_EVENT,ZONE_STATUS_EVENT");
            
            if (localConfig.webhookSecret != null && !localConfig.webhookSecret.isEmpty()) {
                webhookRequest.addProperty("secret", localConfig.webhookSecret);
            }
            
            String jsonBody = gson.toJson(webhookRequest);
            
            // Execute request
            HttpResponse<String> response = executeWebhookRequest(url, "POST", jsonBody, localConfig.apiKey);
            
            if (response.statusCode() == 201) {
                JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                webhookId = responseJson.get("id").getAsString();
                webhookRegistered = true;
                
                logger.info("Webhook registered successfully with ID: {}", webhookId);
                updateState(CHANNEL_BRIDGE_WEBHOOK_STATUS, new StringType("REGISTERED (ID: " + webhookId + ")"));
            } else {
                logger.warn("Failed to register webhook (Status {}): {}", response.statusCode(), response.body());
                webhookRegistered = false;
            }
        } catch (Exception e) {
            logger.error("Error registering webhook: {}", e.getMessage(), e);
            webhookRegistered = false;
        }
    }

    /**
     * Unregister webhook
     */
    private void unregisterWebhook() {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null || webhookId == null) {
            return;
        }

        try {
            String url = API_BASE_URL + "/webhook/" + webhookId;
            HttpResponse<String> response = executeWebhookRequest(url, "DELETE", null, localConfig.apiKey);
            
            if (response.statusCode() == 204) {
                logger.info("Webhook {} unregistered successfully", webhookId);
                webhookRegistered = false;
                webhookId = null;
            } else {
                logger.warn("Failed to unregister webhook (Status {}): {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error unregistering webhook: {}", e.getMessage(), e);
        }
    }

    /**
     * Check webhook health and re-register if needed
     */
    private void checkWebhookHealth() {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null || 
                localConfig.webhookUrl == null || localConfig.webhookUrl.isEmpty()) {
            return;
        }

        try {
            // List existing webhooks
            String url = API_BASE_URL + "/webhook";
            HttpResponse<String> response = executeWebhookRequest(url, "GET", null, localConfig.apiKey);
            
            if (response.statusCode() == 200) {
                JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                boolean found = false;
                
                if (responseJson.has("data") && responseJson.get("data").isJsonArray()) {
                    for (var element : responseJson.getAsJsonArray("data")) {
                        JsonObject webhook = element.getAsJsonObject();
                        if (webhook.has("url") && webhook.get("url").getAsString().equals(localConfig.webhookUrl)) {
                            found = true;
                            webhookId = webhook.get("id").getAsString();
                            webhookRegistered = true;
                            break;
                        }
                    }
                }
                
                if (!found && webhookRegistered) {
                    logger.warn("Webhook not found on Rachio, re-registering");
                    registerWebhook();
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking webhook health: {}", e.getMessage());
        }
    }

    /**
     * Execute webhook API request
     */
    private HttpResponse<String> executeWebhookRequest(String url, String method, @Nullable String body, String apiKey) 
            throws IOException, InterruptedException, URISyntaxException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header(AUTHORIZATION, BEARER_PREFIX + apiKey)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .timeout(Duration.ofSeconds(30));

        switch (method) {
            case "POST":
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;
            case "PUT":
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;
            case "DELETE":
                requestBuilder.DELETE();
                break;
            default: // GET
                requestBuilder.GET();
                break;
        }

        HttpRequest request = requestBuilder.build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Get API key from configuration
     */
    public @Nullable String getApiKey() {
        RachioBridgeConfiguration localConfig = config;
        return localConfig != null ? localConfig.apiKey : null;
    }

    /**
     * Get devices managed by this bridge
     */
    public List<RachioDevice> getDevices() {
        return new ArrayList<>(devices);
    }

    /**
     * Get zones for a device
     */
    public List<RachioZone> getZones(String deviceId) throws RachioApiException {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null) {
            throw new RachioApiException("Bridge not configured");
        }
        return rachioHttp.getZones(localConfig.apiKey, deviceId);
    }

    /**
     * Run all zones on a device
     */
    public void runAllZones(String thingId, int duration, String deviceId) {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null) {
            logger.warn("Cannot run all zones: API key not configured");
            return;
        }
        rachioHttp.runAllZones(thingId, duration, deviceId, localConfig.apiKey);
    }

    /**
     * Set rain delay on a device
     */
    public void rainDelay(String thingId, int hours, String deviceId) {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null) {
            logger.warn("Cannot set rain delay: API key not configured");
            return;
        }
        rachioHttp.rainDelay(thingId, hours, deviceId, localConfig.apiKey);
    }

    /**
     * Run next zone on a device
     */
    public void runNextZone(String thingId, int duration, String deviceId) {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null) {
            logger.warn("Cannot run next zone: API key not configured");
            return;
        }
        rachioHttp.runNextZone(thingId, duration, deviceId, localConfig.apiKey);
    }

    /**
     * Get device forecast
     */
    public @Nullable JsonObject getDeviceForecast(String deviceId) throws RachioApiException {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null) {
            throw new RachioApiException("Bridge not configured");
        }
        return rachioHttp.getDeviceForecast(localConfig.apiKey, deviceId);
    }

    /**
     * Get device water usage
     */
    public @Nullable JsonObject getDeviceWaterUsage(String deviceId) throws RachioApiException {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null) {
            throw new RachioApiException("Bridge not configured");
        }
        return rachioHttp.getDeviceWaterUsage(localConfig.apiKey, deviceId);
    }

    /**
     * Get device savings
     */
    public @Nullable JsonObject getDeviceSavings(String deviceId) throws RachioApiException {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null) {
            throw new RachioApiException("Bridge not configured");
        }
        return rachioHttp.getDeviceSavings(localConfig.apiKey, deviceId);
    }

    /**
     * Pause/resume device
     */
    public void pauseDevice(String thingId, boolean pause, String deviceId) {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null) {
            logger.warn("Cannot pause device: API key not configured");
            return;
        }
        rachioHttp.pauseDevice(thingId, pause, deviceId, localConfig.apiKey);
    }

    /**
     * Get device alerts
     */
    public @Nullable JsonArray getDeviceAlerts(String deviceId) throws RachioApiException {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null) {
            throw new RachioApiException("Bridge not configured");
        }
        return rachioHttp.getDeviceAlerts(localConfig.apiKey, deviceId);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channelId = channelUID.getIdWithoutGroup();
        logger.debug("Handling command {} for channel {}", command, channelId);

        if (command instanceof RefreshType) {
            // Refresh monitoring data
            scheduler.execute(this::updateMonitoringChannels);
            return;
        }

        // Bridge channels are mostly read-only, but we can handle some commands
        switch (channelId) {
            case CHANNEL_BRIDGE_WEBHOOK_STATUS:
                if (command instanceof StringType && command.toString().equals("REGISTER")) {
                    scheduler.execute(this::registerWebhook);
                } else if (command instanceof StringType && command.toString().equals("UNREGISTER")) {
                    scheduler.execute(this::unregisterWebhook);
                }
                break;
                
            case CHANNEL_BRIDGE_POLLING_STATUS:
                if (command instanceof StringType) {
                    String cmd = command.toString();
                    if (cmd.equals("START")) {
                        startPolling();
                    } else if (cmd.equals("STOP")) {
                        stopPolling();
                    }
                }
                break;
                
            default:
                logger.debug("Unhandled command for channel {}", channelId);
                break;
        }
    }

    /**
     * Register a status listener
     */
    public void registerStatusListener(RachioStatusListener listener) {
        statusListeners.add(listener);
    }

    /**
     * Unregister a status listener
     */
    public void unregisterStatusListener(RachioStatusListener listener) {
        statusListeners.remove(listener);
    }

    /**
     * Notify all listeners of device update
     */
    private void notifyDeviceUpdated(RachioDevice device) {
        for (RachioStatusListener listener : statusListeners) {
            listener.onDeviceStateUpdated();
        }
    }

    /**
     * Notify all listeners of zone update
     */
    public void notifyZoneUpdated(RachioZone zone) {
        for (RachioStatusListener listener : statusListeners) {
            listener.onZoneStateUpdated(zone);
        }
    }

    /**
     * Notify all listeners of rate limit critical state
     */
    private void notifyRateLimitCritical() {
        for (RachioStatusListener listener : statusListeners) {
            if (listener instanceof RachioBridgeHandler) {
                ((RachioBridgeHandler) listener).onRateLimitCritical();
            }
        }
    }

    /**
     * Handle rate limit critical state
     */
    public void onRateLimitCritical() {
        logger.warn("Rate limit critical: {} requests remaining", rateLimitRemaining);
        // Adjust polling interval
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = scheduler.scheduleWithFixedDelay(this::pollDevices, 60, 300, TimeUnit.SECONDS);
            logger.info("Polling interval increased to 5 minutes due to rate limit");
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.emptyList();
    }

    /**
     * Get bridge configuration
     */
    public @Nullable RachioBridgeConfiguration getBridgeConfig() {
        return config;
    }

    /**
     * Get rate limit information
     */
    public RateLimitInfo getRateLimitInfo() {
        return new RateLimitInfo(rateLimitTotal, rateLimitRemaining, rateLimitReset, rateLimitCritical);
    }

    /**
     * Rate limit information class
     */
    public static class RateLimitInfo {
        public final int total;
        public final int remaining;
        public final Instant reset;
        public final boolean critical;

        public RateLimitInfo(int total, int remaining, Instant reset, boolean critical) {
            this.total = total;
            this.remaining = remaining;
            this.reset = reset;
            this.critical = critical;
        }
    }
}
