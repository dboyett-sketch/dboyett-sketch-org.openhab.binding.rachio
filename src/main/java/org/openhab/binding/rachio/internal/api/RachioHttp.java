package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.CustomCrop;
import org.openhab.binding.rachio.internal.api.dto.CustomNozzle;
import org.openhab.binding.rachio.internal.api.dto.CustomShade;
import org.openhab.binding.rachio.internal.api.dto.CustomSlope;
import org.openhab.binding.rachio.internal.api.dto.CustomSoil;
import org.openhab.binding.rachio.internal.api.dto.RachioAlert;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioEventSummary;
import org.openhab.binding.rachio.internal.api.dto.RachioForecast;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioSchedule;
import org.openhab.binding.rachio.internal.api.dto.RachioUsage;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Handles HTTP communication with the Rachio API
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    // API Constants
    private static final String RACHIO_API_URL = "https://api.rach.io/1/public";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);
    private static final String USER_AGENT = "OpenHAB-Rachio-Binding/5.0";

    // Rate limiting tracking
    private static final int RATE_LIMIT_WARNING_THRESHOLD = 5;
    private static final int RATE_LIMIT_CRITICAL_THRESHOLD = 2;

    private final HttpClient client;
    private final Gson gson;
    private final RachioBridgeHandler bridgeHandler;
    private @Nullable RachioBridgeConfiguration config;

    // Rate limiting state
    private int remainingRequests = 100; // Conservative default
    private int rateLimitTotal = 100;
    private @Nullable Instant rateLimitReset;
    private RateLimitState rateLimitState = RateLimitState.NORMAL;
    private int adaptivePollingMultiplier = 1;

    // Cache
    private final Map<String, RachioDevice> deviceCache = new HashMap<>();
    private @Nullable Instant cacheExpiry;

    enum RateLimitState {
        NORMAL,
        WARNING,
        CRITICAL
    }

    /**
     * Constructor
     */
    public RachioHttp(RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
        
        // Create HTTP client with proper configuration
        this.client = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        
        // Configure GSON with custom type adapters
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .registerTypeAdapterFactory(new SafeReflectiveTypeAdapterFactory())
                .create();
    }

    /**
     * Initialize with configuration
     */
    public void initialize(RachioBridgeConfiguration config) {
        this.config = config;
        logger.debug("RachioHttp initialized with API key: {}", maskApiKey(config.apiKey));
    }

    /**
     * Get current person (user) information
     */
    public @Nullable RachioPerson getPerson() throws RachioApiException {
        try {
            String response = makeGetRequest("/person/info");
            return gson.fromJson(response, RachioPerson.class);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to get person info: {}", e.getMessage());
            throw new RachioApiException("Failed to get person info", e);
        }
    }

    /**
     * Get device by ID
     */
    public @Nullable RachioDevice getDevice(String deviceId) throws RachioApiException {
        // Check cache first
        if (deviceCache.containsKey(deviceId) && cacheExpiry != null 
                && Instant.now().isBefore(cacheExpiry)) {
            logger.debug("Returning cached device {}", deviceId);
            return deviceCache.get(deviceId);
        }

        try {
            String response = makeGetRequest("/device/" + deviceId);
            RachioDevice device = gson.fromJson(response, RachioDevice.class);
            
            // Cache the result (5 minute cache)
            if (device != null) {
                deviceCache.put(deviceId, device);
                cacheExpiry = Instant.now().plusSeconds(300);
                logger.debug("Cached device {} for 5 minutes", deviceId);
            }
            
            return device;
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to get device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get device", e);
        }
    }

    /**
     * Get all devices for the current user
     */
    public List<RachioDevice> getDevices() throws RachioApiException {
        try {
            String response = makeGetRequest("/person/" + getPersonId() + "/device");
            RachioDevice[] devices = gson.fromJson(response, RachioDevice[].class);
            return Arrays.asList(devices != null ? devices : new RachioDevice[0]);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to get devices: {}", e.getMessage());
            throw new RachioApiException("Failed to get devices", e);
        }
    }

    /**
     * Get person ID from configuration or API
     */
    private String getPersonId() throws RachioApiException {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig != null && localConfig.personId != null && !localConfig.personId.isEmpty()) {
            return localConfig.personId;
        }
        
        // Fetch from API
        RachioPerson person = getPerson();
        if (person != null && person.id != null) {
            // Update config if possible
            if (localConfig != null) {
                localConfig.personId = person.id;
            }
            return person.id;
        }
        
        throw new RachioApiException("Could not determine person ID");
    }

    /**
     * Start a zone
     */
    public void startZone(String thingId, String zoneId, int duration, String deviceId) {
        try {
            String url = RACHIO_API_URL + "/1/public/zone/start";
            String payload = String.format("{\"id\":\"%s\",\"duration\":%d}", zoneId, duration);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            
            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            updateRateLimits(response.headers().map());
            
            if (response.statusCode() == 200) {
                logger.debug("Zone {} started for {} seconds", zoneId, duration);
                bridgeHandler.updateZoneStatus(zoneId, ZoneRunStatus.STARTED, duration);
            } else {
                logger.error("Failed to start zone {}: {} - {}", zoneId, 
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error starting zone {}: {}", zoneId, e.getMessage(), e);
        }
    }

    /**
     * Stop watering on all zones
     */
    public void stopWatering(String thingId, String deviceId) {
        try {
            String url = RACHIO_API_URL + "/1/public/device/" + deviceId + "/stop_water";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + getApiKey())
                    .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            
            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            updateRateLimits(response.headers().map());
            
            if (response.statusCode() == 200) {
                logger.debug("Stopped watering on device {}", deviceId);
                bridgeHandler.updateDeviceStatus(deviceId, "STOPPED");
            } else {
                logger.error("Failed to stop watering on device {}: {} - {}", deviceId,
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error stopping watering: {}", e.getMessage(), e);
        }
    }

    /**
     * Run all zones sequentially
     */
    public void runAllZones(String thingId, int duration, String deviceId) {
        if (thingId == null || deviceId == null) {
            logger.error("runAllZones: thingId or deviceId is null");
            return;
        }
        
        try {
            String url = RACHIO_API_URL + "/1/public/device/" + deviceId + "/quick_run";
            String payload = String.format("{\"duration\": %d}", duration);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            
            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            updateRateLimits(response.headers().map());
            
            if (response.statusCode() == 200) {
                logger.debug("runAllZones successful for device {}", deviceId);
                bridgeHandler.updateDeviceStatus(deviceId, "RUNNING_ALL_ZONES");
            } else {
                logger.error("runAllZones failed: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error running all zones: {}", e.getMessage(), e);
        }
    }

    /**
     * Set rain delay
     */
    public void rainDelay(String thingId, int hours, String deviceId) {
        if (thingId == null || deviceId == null) {
            logger.error("rainDelay: thingId or deviceId is null");
            return;
        }
        
        try {
            String url = RACHIO_API_URL + "/1/public/device/" + deviceId + "/rain_delay";
            String payload = String.format("{\"duration\": %d}", hours * 3600); // Convert to seconds
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + getApiKey())
                    .PUT(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            
            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            updateRateLimits(response.headers().map());
            
            if (response.statusCode() == 200) {
                logger.debug("Rain delay set to {} hours for device {}", hours, deviceId);
                bridgeHandler.updateRainDelay(deviceId, hours);
            } else {
                logger.error("Rain delay failed: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error setting rain delay: {}", e.getMessage(), e);
        }
    }

    /**
     * Run next available zone
     */
    public void runNextZone(String thingId, int duration, String deviceId) {
        if (thingId == null || deviceId == null) {
            logger.error("runNextZone: thingId or deviceId is null");
            return;
        }
        
        try {
            // Get current device state
            RachioDevice device = getDevice(deviceId);
            if (device != null && device.zones != null) {
                // Find first zone that's not running
                for (RachioZone zone : device.zones) {
                    if (zone.id != null && !"RUNNING".equals(zone.zoneRunStatus)) {
                        startZone(thingId, zone.id, duration, deviceId);
                        return;
                    }
                }
                logger.warn("All zones are already running or no zones available");
            } else {
                logger.error("Could not get device {} for runNextZone", deviceId);
            }
        } catch (Exception e) {
            logger.error("Error running next zone: {}", e.getMessage(), e);
        }
    }

    /**
     * Enable or disable a zone
     */
    public void setZoneEnabled(String thingId, String zoneId, boolean enabled, String deviceId) {
        try {
            String url = RACHIO_API_URL + "/1/public/zone/" + zoneId;
            String payload = String.format("{\"enabled\":%s}", enabled);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + getApiKey())
                    .PUT(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            
            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            updateRateLimits(response.headers().map());
            
            if (response.statusCode() == 200) {
                logger.debug("Zone {} enabled set to {}", zoneId, enabled);
                // Clear cache since zone data changed
                deviceCache.remove(deviceId);
            } else {
                logger.error("Failed to set zone enabled: {} - {}", 
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error setting zone enabled: {}", e.getMessage(), e);
        }
    }

    /**
     * Get events for a device
     */
    public List<RachioEventSummary> getEvents(String deviceId, int count) throws RachioApiException {
        try {
            String response = makeGetRequest(
                    String.format("/device/%s/event?count=%d", deviceId, count));
            RachioEventSummary[] events = gson.fromJson(response, RachioEventSummary[].class);
            return Arrays.asList(events != null ? events : new RachioEventSummary[0]);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to get events for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get events", e);
        }
    }

    /**
     * Get forecast for a device
     */
    public @Nullable RachioForecast getForecast(String deviceId) throws RachioApiException {
        try {
            String response = makeGetRequest("/device/" + deviceId + "/forecast");
            return gson.fromJson(response, RachioForecast.class);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to get forecast for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get forecast", e);
        }
    }

    /**
     * Get usage data for a device
     */
    public @Nullable RachioUsage getUsage(String deviceId, int year) throws RachioApiException {
        try {
            String response = makeGetRequest(
                    String.format("/device/%s/water/%d", deviceId, year));
            return gson.fromJson(response, RachioUsage.class);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to get usage for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get usage", e);
        }
    }

    /**
     * Get savings data for a device
     */
    public @Nullable RachioUsage getSavings(String deviceId, int year) throws RachioApiException {
        try {
            String response = makeGetRequest(
                    String.format("/device/%s/savings/%d", deviceId, year));
            return gson.fromJson(response, RachioUsage.class);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to get savings for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get savings", e);
        }
    }

    /**
     * Pause device
     */
    public void pauseDevice(String deviceId, boolean pause) throws RachioApiException {
        try {
            String url = RACHIO_API_URL + "/1/public/device/" + deviceId + "/pause";
            String payload = String.format("{\"paused\":%s}", pause);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + getApiKey())
                    .PUT(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            
            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            updateRateLimits(response.headers().map());
            
            if (response.statusCode() == 200) {
                logger.debug("Device {} pause set to {}", deviceId, pause);
            } else {
                logger.error("Failed to pause device: {} - {}", 
                        response.statusCode(), response.body());
                throw new RachioApiException("Failed to pause device: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error pausing device: {}", e.getMessage());
            throw new RachioApiException("Error pausing device", e);
        }
    }

    /**
     * Get alerts for a device
     */
    public List<RachioAlert> getAlerts(String deviceId) throws RachioApiException {
        try {
            String response = makeGetRequest("/device/" + deviceId + "/alerts");
            RachioAlert[] alerts = gson.fromJson(response, RachioAlert[].class);
            return Arrays.asList(alerts != null ? alerts : new RachioAlert[0]);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to get alerts for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get alerts", e);
        }
    }

    /**
     * Get schedules for a device
     */
    public List<RachioSchedule> getSchedules(String deviceId) throws RachioApiException {
        try {
            String response = makeGetRequest("/device/" + deviceId + "/schedule");
            RachioSchedule[] schedules = gson.fromJson(response, RachioSchedule[].class);
            return Arrays.asList(schedules != null ? schedules : new RachioSchedule[0]);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to get schedules for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get schedules", e);
        }
    }

    /**
     * Get watering history for a zone
     */
    public List<Map<String, Object>> getWateringHistory(String zoneId, int count) 
            throws RachioApiException {
        try {
            String response = makeGetRequest(
                    String.format("/zone/%s/watering?count=%d", zoneId, count));
            // Parse as generic JSON since structure isn't defined in DTOs yet
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> history = gson.fromJson(response, List.class);
            return history != null ? history : new ArrayList<>();
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to get watering history for zone {}: {}", zoneId, e.getMessage());
            throw new RachioApiException("Failed to get watering history", e);
        }
    }

    /**
     * Make a GET request to the Rachio API
     */
    private String makeGetRequest(String endpoint) throws IOException, RachioApiException {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null || localConfig.apiKey.isEmpty()) {
            throw new RachioApiException("API key not configured");
        }

        checkRateLimits();
        
        try {
            String url = RACHIO_API_URL + endpoint;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + localConfig.apiKey)
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            
            logger.debug("Making GET request to: {}", endpoint);
            
            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            updateRateLimits(response.headers().map());
            
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                return response.body();
            } else if (statusCode == 429) {
                logger.warn("Rate limit exceeded for endpoint: {}", endpoint);
                handleRateLimitExceeded();
                throw new RachioApiException("Rate limit exceeded");
            } else if (statusCode == 401) {
                logger.error("Unauthorized - check API key");
                bridgeHandler.updateStatus(ThingStatus.OFFLINE, 
                        ThingStatusDetail.CONFIGURATION_ERROR, "Invalid API key");
                throw new RachioApiException("Unauthorized - invalid API key");
            } else {
                logger.error("API request failed: {} - {}", statusCode, response.body());
                throw new RachioApiException("API request failed: " + statusCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Update rate limits from response headers
     */
    private void updateRateLimits(Map<String, List<String>> headers) {
        try {
            // X-RateLimit-Remaining
            List<String> remainingHeader = headers.get("X-RateLimit-Remaining");
            if (remainingHeader != null && !remainingHeader.isEmpty()) {
                remainingRequests = Integer.parseInt(remainingHeader.get(0));
            }
            
            // X-RateLimit-Limit
            List<String> limitHeader = headers.get("X-RateLimit-Limit");
            if (limitHeader != null && !limitHeader.isEmpty()) {
                rateLimitTotal = Integer.parseInt(limitHeader.get(0));
            }
            
            // X-RateLimit-Reset
            List<String> resetHeader = headers.get("X-RateLimit-Reset");
            if (resetHeader != null && !resetHeader.isEmpty()) {
                try {
                    // Parse ISO 8601 timestamp
                    String resetTime = resetHeader.get(0);
                    rateLimitReset = Instant.parse(resetTime);
                } catch (DateTimeParseException e) {
                    logger.debug("Could not parse rate limit reset time: {}", e.getMessage());
                }
            }
            
            // Update rate limit state
            updateRateLimitState();
            
            // Log rate limit info at debug level
            logger.debug("Rate limits - Remaining: {}/{}, Reset: {}, State: {}", 
                    remainingRequests, rateLimitTotal, rateLimitReset, rateLimitState);
            
        } catch (NumberFormatException e) {
            logger.debug("Error parsing rate limit headers: {}", e.getMessage());
        }
    }

    /**
     * Update rate limit state based on remaining requests
     */
    private void updateRateLimitState() {
        RateLimitState oldState = rateLimitState;
        
        if (remainingRequests <= RATE_LIMIT_CRITICAL_THRESHOLD) {
            rateLimitState = RateLimitState.CRITICAL;
            adaptivePollingMultiplier = 3; // Slow down polling significantly
        } else if (remainingRequests <= RATE_LIMIT_WARNING_THRESHOLD) {
            rateLimitState = RateLimitState.WARNING;
            adaptivePollingMultiplier = 2; // Slow down polling
        } else {
            rateLimitState = RateLimitState.NORMAL;
            adaptivePollingMultiplier = 1;
        }
        
        if (oldState != rateLimitState) {
            logger.info("Rate limit state changed: {} -> {}", oldState, rateLimitState);
            bridgeHandler.updateRateLimitStatus(rateLimitState.toString(), 
                    remainingRequests, rateLimitTotal, rateLimitReset);
        }
    }

    /**
     * Check if we can make a request based on rate limits
     */
    private void checkRateLimits() throws RachioApiException {
        if (remainingRequests <= 0) {
            Instant reset = rateLimitReset;
            if (reset != null && Instant.now().isBefore(reset)) {
                throw new RachioApiException("Rate limit exceeded. Reset at: " + reset);
            } else {
                // Reset time has passed, reset counter
                remainingRequests = rateLimitTotal;
                rateLimitState = RateLimitState.NORMAL;
                adaptivePollingMultiplier = 1;
            }
        }
    }

    /**
     * Handle rate limit exceeded
     */
    private void handleRateLimitExceeded() {
        rateLimitState = RateLimitState.CRITICAL;
        adaptivePollingMultiplier = 3;
        remainingRequests = 0;
        
        bridgeHandler.updateRateLimitStatus(rateLimitState.toString(), 
                remainingRequests, rateLimitTotal, rateLimitReset);
        
        logger.warn("Rate limit exceeded. Polling slowed down 3x until reset.");
    }

    /**
     * Get adaptive polling multiplier
     */
    public int getAdaptivePollingMultiplier() {
        return adaptivePollingMultiplier;
    }

    /**
     * Get API key from configuration
     */
    private String getApiKey() throws RachioApiException {
        RachioBridgeConfiguration localConfig = config;
        if (localConfig == null || localConfig.apiKey == null || localConfig.apiKey.isEmpty()) {
            throw new RachioApiException("API key not configured");
        }
        return localConfig.apiKey;
    }

    /**
     * Mask API key for logging
     */
    private String maskApiKey(@Nullable String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Get rate limit information for channels
     */
    public Map<String, Object> getRateLimitInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("remaining", remainingRequests);
        info.put("total", rateLimitTotal);
        info.put("state", rateLimitState.toString());
        info.put("multiplier", adaptivePollingMultiplier);
        info.put("reset", rateLimitReset != null ? rateLimitReset.toString() : "Unknown");
        return info;
    }

    /**
     * Clear device cache
     */
    public void clearCache() {
        deviceCache.clear();
        cacheExpiry = null;
        logger.debug("Device cache cleared");
    }

    /**
     * Get cached device count
     */
    public int getCacheSize() {
        return deviceCache.size();
    }

    /**
     * Process webhook event
     */
    public void processWebhookEvent(RachioWebhookEvent event) {
        if (event == null || event.deviceId == null) {
            logger.warn("Invalid webhook event received");
            return;
        }
        
        logger.debug("Processing webhook event: {} for device {}", 
                event.type, event.deviceId);
        
        // Update device cache
        deviceCache.remove(event.deviceId);
        
        // Notify bridge handler
        bridgeHandler.handleWebhookEvent(event);
    }

    /**
     * Validate webhook signature
     */
    public boolean validateWebhookSignature(String payload, String signature, String secret) {
        // Implementation moved to RachioSecurity class
        // This method kept for backward compatibility
        RachioSecurity security = new RachioSecurity();
        return security.validateWebhookSignature(payload, signature, secret);
    }
}
