package org.openhab.binding.rachio.internal.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link RachioHttp} class handles the HTTP communication with the Rachio API.
 *
 * @author Michael Lobstein - Initial contribution
 * @author Damion Boyett - Enhanced for OpenHAB 5.x
 */
@NonNullByDefault
public class RachioHttp implements RachioActions {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);
    private final Gson gson;

    private static final String BASE_URL = "https://api.rach.io/1/public";
    private static final String CONTENT_TYPE = "application/json";
    private static final String USER_AGENT = "openHAB-Rachio-Binding/5.0";

    private final String apiKey;
    private final ApiExceptionHandler exceptionHandler;
    private @Nullable RachioBridgeHandler bridgeHandler;

    private int rateLimitRemaining = 60;
    private int rateLimitLimit = 60;
    private long rateLimitReset = 0;

    public interface ApiExceptionHandler {
        void handle(Exception e);
    }

    public RachioHttp(String apiKey, ApiExceptionHandler exceptionHandler) {
        this.apiKey = apiKey;
        this.exceptionHandler = exceptionHandler;
        
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Instant.class, new InstantTypeAdapter());
        this.gson = gsonBuilder.create();
    }

    public void setBridgeHandler(@Nullable RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
    }

    public @Nullable RachioPerson getPerson() throws RachioApiException, IOException {
        String url = BASE_URL + "/person/info";
        String response = executeGet(url);
        return gson.fromJson(response, RachioPerson.class);
    }

    public List<RachioDevice> getDevices(String personId) throws RachioApiException, IOException {
        String url = BASE_URL + "/person/" + personId;
        String response = executeGet(url);
        RachioPerson person = gson.fromJson(response, RachioPerson.class);
        return person.devices != null ? person.devices : List.of();
    }

    public void startZone(String deviceId, String zoneId, int duration) throws RachioApiException, IOException {
        startZone(deviceId, zoneId, duration, "openhab");
    }

    public void stopWatering(String deviceId) throws RachioApiException, IOException {
        stopWatering(deviceId, "openhab");
    }

    public void runAllZones(String deviceId, int duration) throws RachioApiException, IOException {
        runAllZones(deviceId, duration, "openhab");
    }

    public void setZoneEnabled(String deviceId, String zoneId, boolean enabled) throws RachioApiException, IOException {
        setZoneEnabled(deviceId, zoneId, enabled, "openhab");
    }

    public void rainDelay(String deviceId, int duration) throws RachioApiException, IOException {
        String url = BASE_URL + "/device/rain_delay";
        String json = String.format("{\"id\":\"%s\",\"duration\":%d}", deviceId, duration);
        executePut(url, json);
    }

    public void runNextZone(String deviceId) throws RachioApiException, IOException {
        runNextZone(deviceId, "openhab");
    }

    private String executeGet(String urlString) throws RachioApiException, IOException {
        return executeRequest(urlString, "GET", null);
    }

    private String executePut(String urlString, String jsonBody) throws RachioApiException, IOException {
        return executeRequest(urlString, "PUT", jsonBody);
    }

    private String executeRequest(String urlString, String method, @Nullable String jsonBody) throws RachioApiException, IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", CONTENT_TYPE);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            // Set request body for PUT/POST
            if (jsonBody != null && (method.equals("PUT") || method.equals("POST"))) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // Read rate limit headers
            readRateLimitHeaders(connection);

            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                try (InputStream inputStream = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            } else {
                String errorMessage = readErrorMessage(connection);
                RachioApiException exception = new RachioApiException("API request failed: " + responseCode + " - " + errorMessage, responseCode);
                handleApiError(responseCode, errorMessage);
                throw exception;
            }

        } catch (IOException e) {
            logger.error("HTTP request failed: {}", e.getMessage());
            exceptionHandler.handle(e);
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void readRateLimitHeaders(HttpURLConnection connection) {
        try {
            String remainingHeader = connection.getHeaderField("X-RateLimit-Remaining");
            String limitHeader = connection.getHeaderField("X-RateLimit-Limit");
            String resetHeader = connection.getHeaderField("X-RateLimit-Reset");

            if (remainingHeader != null) {
                rateLimitRemaining = Integer.parseInt(remainingHeader);
            }
            if (limitHeader != null) {
                rateLimitLimit = Integer.parseInt(limitHeader);
            }
            if (resetHeader != null) {
                try {
                    // Try parsing as ISO instant
                    Instant resetInstant = Instant.parse(resetHeader);
                    rateLimitReset = resetInstant.getEpochSecond();
                } catch (DateTimeParseException e) {
                    try {
                        // Try parsing as Unix timestamp
                        rateLimitReset = Long.parseLong(resetHeader);
                    } catch (NumberFormatException e2) {
                        logger.debug("Failed to parse rate limit reset header: {}", resetHeader);
                    }
                }
            }
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse rate limit headers: {}", e.getMessage());
        }
    }

    private String readErrorMessage(HttpURLConnection connection) {
        try (InputStream errorStream = connection.getErrorStream()) {
            if (errorStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    errorResponse.append(line);
                }
                return errorResponse.toString();
            }
        } catch (IOException e) {
            logger.debug("Failed to read error stream: {}", e.getMessage());
        }
        return connection.getResponseMessage();
    }

    private void handleApiError(int statusCode, String errorMessage) {
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler != null) {
            if (statusCode == 401) {
                localBridgeHandler.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid API key");
            } else if (statusCode == 429) {
                localBridgeHandler.updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Rate limit exceeded");
            } else if (statusCode >= 500) {
                localBridgeHandler.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "API server error");
            } else {
                localBridgeHandler.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "API error: " + statusCode);
            }
        }
    }

    public int getRateLimitRemaining() {
        return rateLimitRemaining;
    }

    public int getRateLimitLimit() {
        return rateLimitLimit;
    }

    public long getRateLimitReset() {
        return rateLimitReset;
    }

    public String getRateLimitResetFormatted() {
        if (rateLimitReset > 0) {
            Instant resetInstant = Instant.ofEpochSecond(rateLimitReset);
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(resetInstant);
        }
        return "N/A";
    }

    // Implementation of RachioActions interface methods
    @Override
    public int getAdaptivePollingMultiplier() {
        return 1; // Default implementation
    }

    @Override
    public void startZone(String deviceId, String zoneId, int duration, String source) throws RachioApiException, IOException {
        String url = BASE_URL + "/zone/start";
        String json = String.format("{\"id\":\"%s\",\"duration\":%d,\"zoneId\":\"%s\"}", zoneId, duration, deviceId);
        executePut(url, json);
    }

    @Override
    public void stopWatering(String deviceId, String source) throws RachioApiException, IOException {
        String url = BASE_URL + "/device/stop_water";
        String json = String.format("{\"id\":\"%s\"}", deviceId);
        executePut(url, json);
    }

    @Override
    public void runAllZones(String deviceId, int duration, String source) throws RachioApiException, IOException {
        String url = BASE_URL + "/device/start_multiple_zone_run";
        String json = String.format("{\"id\":\"%s\",\"duration\":%d}", deviceId, duration);
        executePut(url, json);
    }

    @Override
    public void setZoneEnabled(String deviceId, String zoneId, boolean enabled, String source) throws RachioApiException, IOException {
        String url = BASE_URL + "/zone";
        String json = String.format("{\"id\":\"%s\",\"enabled\":%s}", zoneId, enabled);
        executePut(url, json);
    }

    @Override
    public void rainDelay(String deviceId, int duration) throws RachioApiException, IOException {
        String url = BASE_URL + "/device/rain_delay";
        String json = String.format("{\"id\":\"%s\",\"duration\":%d}", deviceId, duration);
        executePut(url, json);
    }

    @Override
    public void runNextZone(String deviceId, String source) throws RachioApiException, IOException {
        String url = BASE_URL + "/device/start_zone";
        String json = String.format("{\"id\":\"%s\"}", deviceId);
        executePut(url, json);
    }

    @Override
    public boolean validateWebhookSignature(String payload, String signature, String webhookKey) {
        // Simple validation - return true for now
        // TODO: Implement HMAC validation
        return true;
    }
}
