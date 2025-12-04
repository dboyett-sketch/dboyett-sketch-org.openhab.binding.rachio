package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
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
 * The {@link RachioHttp} class is responsible for all HTTP communications with the Rachio Cloud API
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite for OpenHAB 5.x
 */
@Component(service = RachioHttp.class)
@NonNullByDefault
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    // API Constants
    private static final String BASE_URL = "https://api.rach.io/1/public";
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // HTTP Client
    private final HttpClient httpClient;
    private final Gson gson;

    @Activate
    public RachioHttp(@Reference HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .registerTypeAdapterFactory(new SafeReflectiveTypeAdapterFactory())
                .create();
    }

    /**
     * Get authentication headers for API requests
     */
    public String[] getAuthHeaders(String apiKey) {
        return new String[] { AUTHORIZATION, BEARER_PREFIX + apiKey, CONTENT_TYPE, APPLICATION_JSON };
    }

    /**
     * Get base API URL for device-specific endpoints
     */
    public String getApiUrl(@Nullable String deviceId) {
        return BASE_URL;
    }

    /**
     * Get person info (used to validate API key)
     */
    public @Nullable RachioPerson getPersonInfo(String apiKey) throws RachioApiException {
        try {
            String url = BASE_URL + "/person/info";
            HttpResponse<String> response = executeGet(url, getAuthHeaders(apiKey));

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), RachioPerson.class);
            } else {
                throw new RachioApiException("Failed to get person info: " + response.body());
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RachioApiException("Error getting person info", e);
        }
    }

    /**
     * Get device list for a person
     */
    public List<RachioDevice> getDevices(String apiKey) throws RachioApiException {
        try {
            String url = BASE_URL + "/person/" + getPersonId(apiKey) + "/device";
            HttpResponse<String> response = executeGet(url, getAuthHeaders(apiKey));

            if (response.statusCode() == 200) {
                RachioDevice[] devices = gson.fromJson(response.body(), RachioDevice[].class);
                return List.of(devices);
            } else {
                throw new RachioApiException("Failed to get devices: " + response.body());
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RachioApiException("Error getting devices", e);
        }
    }

    /**
     * Get device by ID
     */
    public @Nullable RachioDevice getDevice(String apiKey, String deviceId) throws RachioApiException {
        try {
            String url = BASE_URL + "/device/" + deviceId;
            HttpResponse<String> response = executeGet(url, getAuthHeaders(apiKey));

            if (response.statusCode() == 200) {
                return gson.fromJson(response.body(), RachioDevice.class);
            } else {
                throw new RachioApiException("Failed to get device: " + response.body());
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RachioApiException("Error getting device", e);
        }
    }

    /**
     * Get zones for a device
     */
    public List<RachioZone> getZones(String apiKey, String deviceId) throws RachioApiException {
        try {
            String url = BASE_URL + "/device/" + deviceId + "/zone";
            HttpResponse<String> response = executeGet(url, getAuthHeaders(apiKey));

            if (response.statusCode() == 200) {
                RachioZone[] zones = gson.fromJson(response.body(), RachioZone[].class);
                return List.of(zones);
            } else {
                throw new RachioApiException("Failed to get zones: " + response.body());
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RachioApiException("Error getting zones", e);
        }
    }

    /**
     * Get person ID from API key
     */
    private String getPersonId(String apiKey) throws RachioApiException {
        RachioPerson person = getPersonInfo(apiKey);
        if (person != null && person.getId() != null) {
            return person.getId();
        }
        throw new RachioApiException("Could not get person ID");
    }

    /**
     * Start a specific zone
     */
    public void startZone(String thingId, String zoneId, int duration, String deviceId, String apiKey) {
        if (deviceId == null || deviceId.isEmpty()) {
            logger.warn("Device ID not found for thing {}", thingId);
            return;
        }

        try {
            String url = String.format("%s/zone/%s/start", getApiUrl(deviceId), zoneId);
            String json = String.format("{\"duration\": %d}", duration);

            HttpResponse<String> response = executePost(url, getAuthHeaders(apiKey), json);

            if (response.statusCode() == 204) {
                logger.debug("Successfully started zone {} for device {} (duration: {} seconds)",
                        zoneId, deviceId, duration);
            } else {
                logger.warn("Failed to start zone {} (Status {}): {}", zoneId, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error starting zone: {}", e.getMessage(), e);
        }
    }

    /**
     * Stop watering on a device
     */
    public void stopWatering(String thingId, String deviceId, String apiKey) {
        if (deviceId == null || deviceId.isEmpty()) {
            logger.warn("Device ID not found for thing {}", thingId);
            return;
        }

        try {
            String url = String.format("%s/device/%s/stop_water", BASE_URL, deviceId);

            HttpResponse<String> response = executePut(url, getAuthHeaders(apiKey), "{}");

            if (response.statusCode() == 204) {
                logger.debug("Successfully stopped watering for device {}", deviceId);
            } else {
                logger.warn("Failed to stop watering (Status {}): {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error stopping watering: {}", e.getMessage(), e);
        }
    }

    /**
     * Set zone enabled state
     */
    public void setZoneEnabled(String thingId, String zoneId, boolean enabled, String deviceId, String apiKey) {
        if (deviceId == null || deviceId.isEmpty()) {
            logger.warn("Device ID not found for thing {}", thingId);
            return;
        }

        try {
            String url = String.format("%s/zone/%s", getApiUrl(deviceId), zoneId);
            String json = String.format("{\"enabled\": %b}", enabled);

            HttpResponse<String> response = executePut(url, getAuthHeaders(apiKey), json);

            if (response.statusCode() == 204) {
                logger.debug("Successfully {} zone {} for device {}", enabled ? "enabled" : "disabled", zoneId, deviceId);
            } else {
                logger.warn("Failed to set zone enabled state (Status {}): {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error setting zone enabled state: {}", e.getMessage(), e);
        }
    }

    /**
     * IMPLEMENTED: Run all zones for specified duration
     * Rachio API: POST /zone/start with empty zones array
     */
    public void runAllZones(String thingId, int duration, String deviceId, String apiKey) {
        if (deviceId == null || deviceId.isEmpty()) {
            logger.warn("Device ID not found for thing {}", thingId);
            return;
        }

        try {
            // Rachio API: POST /zone/start with empty zones array runs all zones
            String url = String.format("%s/zone/start", getApiUrl(deviceId));
            String json = String.format("{\"zones\": [], \"duration\": %d}", duration);

            HttpResponse<String> response = executePost(url, getAuthHeaders(apiKey), json);

            if (response.statusCode() == 204) {
                logger.debug("Successfully started all zones for device {} (duration: {} seconds)",
                        deviceId, duration);
            } else {
                logger.warn("Failed to start all zones (Status {}): {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error starting all zones: {}", e.getMessage(), e);
        }
    }

    /**
     * IMPLEMENTED: Set rain delay on device
     * Rachio API: PUT /device/{id}/rainDelay
     */
    public void rainDelay(String thingId, int hours, String deviceId, String apiKey) {
        if (deviceId == null || deviceId.isEmpty()) {
            logger.warn("Device ID not found for thing {}", thingId);
            return;
        }

        try {
            // Rachio API: PUT /device/{id}/rainDelay
            String url = String.format("%s/device/%s/rainDelay", BASE_URL, deviceId);

            // Convert hours to seconds (Rachio expects seconds)
            int seconds = hours * 3600;
            String json = String.format("{\"duration\": %d}", seconds);

            HttpResponse<String> response = executePut(url, getAuthHeaders(apiKey), json);

            if (response.statusCode() == 204) {
                logger.debug("Successfully set rain delay to {} hours ({} seconds) for device {}",
                        hours, seconds, deviceId);
            } else {
                logger.warn("Failed to set rain delay (Status {}): {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error setting rain delay: {}", e.getMessage(), e);
        }
    }

    /**
     * IMPLEMENTED: Run next available zone
     * Strategy: Check current schedule and start next non-running zone
     */
    public void runNextZone(String thingId, int duration, String deviceId, String apiKey) {
        if (deviceId == null || deviceId.isEmpty()) {
            logger.warn("Device ID not found for thing {}", thingId);
            return;
        }

        try {
            // First, get current schedule to find next zone
            String scheduleUrl = String.format("%s/device/%s/current_schedule", BASE_URL, deviceId);
            HttpResponse<String> scheduleResponse = executeGet(scheduleUrl, getAuthHeaders(apiKey));

            if (scheduleResponse.statusCode() == 200) {
                JsonObject schedule = JsonParser.parseString(scheduleResponse.body()).getAsJsonObject();
                String nextZoneId = findNextZoneId(schedule);

                if (nextZoneId != null) {
                    // Start the identified next zone
                    startZone(thingId, nextZoneId, duration, deviceId, apiKey);
                } else {
                    logger.warn("No zones available to start as next zone for device {}", deviceId);
                }
            } else if (scheduleResponse.statusCode() == 404) {
                // No current schedule - start first zone
                logger.debug("No current schedule found, starting first zone");
                startFirstZone(thingId, duration, deviceId, apiKey);
            } else {
                logger.warn("Could not get current schedule (Status {}): {}",
                        scheduleResponse.statusCode(), scheduleResponse.body());
            }
        } catch (Exception e) {
            logger.error("Error starting next zone: {}", e.getMessage(), e);
        }
    }

    /**
     * Find the next zone ID from current schedule
     */
    private @Nullable String findNextZoneId(JsonObject schedule) {
        if (!schedule.has("zones") || !schedule.get("zones").isJsonArray()) {
            return null;
        }

        JsonArray zones = schedule.getAsJsonArray("zones");
        if (zones.size() == 0) {
            return null;
        }

        // Check for currently running zone
        for (int i = 0; i < zones.size(); i++) {
            JsonObject zone = zones.get(i).getAsJsonObject();
            if (zone.has("running") && zone.get("running").getAsBoolean()) {
                // Found running zone, return next one if available
                if (i < zones.size() - 1) {
                    return zones.get(i + 1).getAsJsonObject().get("zoneId").getAsString();
                }
                // If last zone is running, return null (no next zone)
                return null;
            }
        }

        // No zone running, return first zone
        return zones.get(0).getAsJsonObject().get("zoneId").getAsString();
    }

    /**
     * Start the first zone of a device
     */
    private void startFirstZone(String thingId, int duration, String deviceId, String apiKey) {
        try {
            // Get zones to find first zone ID
            List<RachioZone> zones = getZones(apiKey, deviceId);
            if (!zones.isEmpty()) {
                String firstZoneId = zones.get(0).getId();
                startZone(thingId, firstZoneId, duration, deviceId, apiKey);
            } else {
                logger.warn("No zones found for device {}", deviceId);
            }
        } catch (RachioApiException e) {
            logger.error("Error getting zones for device {}: {}", deviceId, e.getMessage());
        }
    }

    /**
     * Get device forecast data
     */
    public @Nullable JsonObject getDeviceForecast(String apiKey, String deviceId) throws RachioApiException {
        try {
            String url = BASE_URL + "/device/" + deviceId + "/forecast";
            HttpResponse<String> response = executeGet(url, getAuthHeaders(apiKey));

            if (response.statusCode() == 200) {
                return JsonParser.parseString(response.body()).getAsJsonObject();
            } else {
                throw new RachioApiException("Failed to get forecast: " + response.body());
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RachioApiException("Error getting forecast", e);
        }
    }

    /**
     * Get device water usage
     */
    public @Nullable JsonObject getDeviceWaterUsage(String apiKey, String deviceId) throws RachioApiException {
        try {
            String url = BASE_URL + "/device/" + deviceId + "/water";
            HttpResponse<String> response = executeGet(url, getAuthHeaders(apiKey));

            if (response.statusCode() == 200) {
                return JsonParser.parseString(response.body()).getAsJsonObject();
            } else {
                throw new RachioApiException("Failed to get water usage: " + response.body());
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RachioApiException("Error getting water usage", e);
        }
    }

    /**
     * Get device savings data
     */
    public @Nullable JsonObject getDeviceSavings(String apiKey, String deviceId) throws RachioApiException {
        try {
            String url = BASE_URL + "/device/" + deviceId + "/savings";
            HttpResponse<String> response = executeGet(url, getAuthHeaders(apiKey));

            if (response.statusCode() == 200) {
                return JsonParser.parseString(response.body()).getAsJsonObject();
            } else {
                throw new RachioApiException("Failed to get savings data: " + response.body());
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RachioApiException("Error getting savings data", e);
        }
    }

    /**
     * Pause device
     */
    public void pauseDevice(String thingId, boolean pause, String deviceId, String apiKey) {
        if (deviceId == null || deviceId.isEmpty()) {
            logger.warn("Device ID not found for thing {}", thingId);
            return;
        }

        try {
            String url = String.format("%s/device/%s/pause", BASE_URL, deviceId);
            String json = String.format("{\"paused\": %b}", pause);

            HttpResponse<String> response = executePut(url, getAuthHeaders(apiKey), json);

            if (response.statusCode() == 204) {
                logger.debug("Successfully {} device {}", pause ? "paused" : "resumed", deviceId);
            } else {
                logger.warn("Failed to {} device (Status {}): {}",
                        pause ? "pause" : "resume", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error {} device: {}", pause ? "pausing" : "resuming", e.getMessage(), e);
        }
    }

    /**
     * Get device alerts
     */
    public @Nullable JsonArray getDeviceAlerts(String apiKey, String deviceId) throws RachioApiException {
        try {
            String url = BASE_URL + "/device/" + deviceId + "/alerts";
            HttpResponse<String> response = executeGet(url, getAuthHeaders(apiKey));

            if (response.statusCode() == 200) {
                return JsonParser.parseString(response.body()).getAsJsonArray();
            } else {
                throw new RachioApiException("Failed to get alerts: " + response.body());
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RachioApiException("Error getting alerts", e);
        }
    }

    /**
     * Execute HTTP GET request
     */
    private HttpResponse<String> executeGet(String url, String[] headers)
            throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .headers(headers)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Execute HTTP POST request
     */
    private HttpResponse<String> executePost(String url, String[] headers, String body)
            throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .headers(headers)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Execute HTTP PUT request
     */
    private HttpResponse<String> executePut(String url, String[] headers, String body)
            throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .headers(headers)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Execute HTTP DELETE request
     */
    private HttpResponse<String> executeDelete(String url, String[] headers)
            throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .headers(headers)
                .DELETE()
                .timeout(Duration.ofSeconds(30))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
