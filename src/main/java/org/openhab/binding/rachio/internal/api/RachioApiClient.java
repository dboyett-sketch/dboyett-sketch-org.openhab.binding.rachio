package org.openhab.binding.rachio.internal.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.*;
import org.openhab.core.common.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Business logic layer for Rachio API interactions
 */
public class RachioApiClient {
    private final Logger logger = LoggerFactory.getLogger(RachioApiClient.class);

    private static final String BASE_URL = "https://api.rach.io/1/public";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final RachioHttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    
    // Caching implementation for Option A
    private final Map<String, RachioDevice> deviceCache = new ConcurrentHashMap<>();
    private final Map<String, String> zoneToDeviceMap = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    public RachioApiClient(String apiKey) {
        this.httpClient = new RachioHttpClient(apiKey);
        this.scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
            new NamedThreadFactory("rachio-polling", true)
        );

        // Start periodic status updates
        startPolling();
    }

    public void dispose() {
        scheduler.shutdown();
    }

    // ==================== WEBHOOK CACHE CLEARING ====================

    /**
     * Process webhook events from Rachio
     * This should be called from RachioWebHookServlet when events arrive
     */
    public void processWebhookEvent(RachioWebHookEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }
        
        String eventType = event.getEventType();
        logger.debug("Processing webhook event: {}", eventType);
        
        switch (eventType) {
            case "ZONE_COMPLETED_EVENT":
            case "ZONE_STARTED_EVENT":
            case "ZONE_STOPPED_EVENT":
                // Zone state changed - clear affected device cache
                if (event.getZoneId() != null) {
                    clearDeviceCacheForZone(event.getZoneId());
                    logger.debug("Cleared cache for zone {} after {}", event.getZoneId(), eventType);
                }
                break;
                
            case "DEVICE_STATUS_EVENT":
            case "DEVICE_CONNECTED_EVENT":
            case "DEVICE_DISCONNECTED_EVENT":
                // Device state changed - clear device cache
                if (event.getDeviceId() != null) {
                    clearDeviceCache(event.getDeviceId());
                    logger.debug("Cleared cache for device {} after {}", event.getDeviceId(), eventType);
                }
                break;
                
            case "RAIN_DELAY_EVENT":
            case "WATER_BUDGET_EVENT":
            case "DEVICE_PAUSED_EVENT":
            case "DEVICE_RESUMED_EVENT":
                // Device settings changed
                if (event.getDeviceId() != null) {
                    clearDeviceCache(event.getDeviceId());
                    logger.debug("Cleared cache for device {} after {}", event.getDeviceId(), eventType);
                }
                break;
                
            case "SCHEDULE_COMPLETED_EVENT":
            case "SCHEDULE_STARTED_EVENT":
            case "SCHEDULE_STOPPED_EVENT":
                // Schedule events affect devices
                if (event.getDeviceId() != null) {
                    clearDeviceCache(event.getDeviceId());
                }
                break;
                
            default:
                logger.debug("Unhandled webhook event type: {}", eventType);
                // For any unhandled event, clear all cache to be safe
                clearCache();
        }
        
        // Also check for event summary which might contain multiple device/zone IDs
        if (event.getEventSummary() != null) {
            processEventSummary(event.getEventSummary());
        }
    }

    /**
     * Process event summary which may contain multiple affected entities
     */
    private void processEventSummary(RachioEventSummary summary) {
        if (summary.getDeviceIds() != null) {
            for (String deviceId : summary.getDeviceIds()) {
                clearDeviceCache(deviceId);
            }
        }
        if (summary.getZoneIds() != null) {
            for (String zoneId : summary.getZoneIds()) {
                clearDeviceCacheForZone(zoneId);
            }
        }
    }

    /**
     * Get recent events for a device (useful for debugging)
     */
    public List<RachioWebHookEvent> getDeviceEvents(String deviceId, int limit) throws RachioApiException {
        String response = httpClient.get(BASE_URL + "/device/" + deviceId + "/event?limit=" + limit);
        try {
            return mapper.readValue(response, new TypeReference<List<RachioWebHookEvent>>() {});
        } catch (Exception e) {
            throw new RachioApiException("Failed to parse device events", e);
        }
    }

    // ==================== FLEXIBLE DURATION SUPPORT ====================

    /**
     * Start zone with duration in seconds
     */
    public void startZoneSeconds(String zoneId, int seconds) throws RachioApiException {
        if (seconds <= 0) {
            throw new RachioApiException("Duration must be positive");
        }
        
        String body = String.format("{\"id\":\"%s\",\"duration\":%d}", zoneId, seconds);
        httpClient.put(BASE_URL + "/zone/start", body);
        
        // Clear cache for the parent device since zone state changed
        clearDeviceCacheForZone(zoneId);
    }

    /**
     * Start zone with duration in minutes
     */
    public void startZoneMinutes(String zoneId, int minutes) throws RachioApiException {
        startZoneSeconds(zoneId, minutes * 60);
    }

    /**
     * Start zone with flexible duration string (e.g., "10m30s", "5m", "120s")
     */
    public void startZone(String zoneId, String durationStr) throws RachioApiException {
        int seconds = DurationParser.parseToSeconds(durationStr);
        if (seconds <= 0) {
            throw new RachioApiException("Invalid duration format: " + durationStr);
        }
        startZoneSeconds(zoneId, seconds);
    }

    // ==================== EXISTING METHODS (UPDATED) ====================

    // Device operations
    public List<RachioDevice> getDevices() throws RachioApiException {
        String response = httpClient.get(BASE_URL + "/person/info");
        try {
            RachioPerson person = mapper.readValue(response, RachioPerson.class);
            List<RachioDevice> devices = person.getDevices();

            // Update cache with fresh device data
            if (devices != null) {
                for (RachioDevice device : devices) {
                    String deviceId = device.getId();
                    deviceCache.put(deviceId, device);
                    cacheTimestamps.put(deviceId, System.currentTimeMillis());

                    // Update zone-to-device mappings
                    if (device.getZones() != null) {
                        for (RachioZone zone : device.getZones()) {
                            zoneToDeviceMap.put(zone.getId(), deviceId);
                        }
                    }
                }
            }

            return devices;
        } catch (Exception e) {
            throw new RachioApiException("Failed to parse devices", e);
        }
    }

    /**
     * Get device with caching
     */
    public @Nullable RachioDevice getDevice(String deviceId) throws RachioApiException {
        // Check cache first (if not expired)
        RachioDevice cached = deviceCache.get(deviceId);
        Long cacheTime = cacheTimestamps.get(deviceId);

        if (cached != null && cacheTime != null && 
            (System.currentTimeMillis() - cacheTime) < CACHE_TTL_MS) {
            return cached;
        }

        // Cache miss or expired - fetch from API
        String response = httpClient.get(BASE_URL + "/device/" + deviceId);
        try {
            RachioDevice device = mapper.readValue(response, RachioDevice.class);

            // Update cache
            deviceCache.put(deviceId, device);
            cacheTimestamps.put(deviceId, System.currentTimeMillis());

            // Update zone-to-device mappings
            if (device.getZones() != null) {
                for (RachioZone zone : device.getZones()) {
                    zoneToDeviceMap.put(zone.getId(), deviceId);
                }
            }

            return device;
        } catch (Exception e) {
            throw new RachioApiException("Failed to parse device", e);
        }
    }

    /**
     * Get zone data by zone ID
     * Fetches the parent device and extracts the specific zone
     */
    public @Nullable RachioZone getZone(String zoneId) throws RachioApiException {
        // Check cache first
        String deviceId = zoneToDeviceMap.get(zoneId);
        if (deviceId == null) {
            // We don't know which device this zone belongs to
            // Need to search through user's devices
            deviceId = findDeviceIdForZone(zoneId);
            if (deviceId == null) {
                logger.debug("Zone {} not found on any device", zoneId);
                return null;
            }
        }

        // Get device (with caching)
        RachioDevice device = getDevice(deviceId);
        if (device == null || device.getZones() == null) {
            logger.warn("Device {} not found or has no zones", deviceId);
            return null;
        }

        // Find the zone in device's zones
        return device.getZones().stream()
            .filter(zone -> zoneId.equals(zone.getId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Find which device contains the specified zone
     */
    private @Nullable String findDeviceIdForZone(String zoneId) throws RachioApiException {
        List<RachioDevice> devices = getDevices();
        for (RachioDevice device : devices) {
            if (device.getZones() != null) {
                for (RachioZone zone : device.getZones()) {
                    if (zoneId.equals(zone.getId())) {
                        // Cache the mapping for future use
                        zoneToDeviceMap.put(zoneId, device.getId());
                        logger.debug("Mapped zone {} to device {}", zoneId, device.getId());
                        return device.getId();
                    }
                }
            }
        }
        return null;
    }

    // NOTE: The original startZone(int duration) method has been replaced with the new flexible versions above.

    public void stopWatering(String deviceId) throws RachioApiException {
        httpClient.put(BASE_URL + "/device/" + deviceId + "/stop_water", "{}");

        // Clear cache for this device
        clearDeviceCache(deviceId);
    }

    // Device control methods needed for DeviceHandler
    public void setRainDelay(String deviceId, int hours) throws RachioApiException {
        String body = String.format("{\"hours\":%d}", hours);
        httpClient.put(BASE_URL + "/device/" + deviceId + "/rain_delay", body);
        clearDeviceCache(deviceId);
    }

    public void pauseDevice(String deviceId, boolean pause) throws RachioApiException {
        String action = pause ? "pause" : "resume";
        httpClient.put(BASE_URL + "/device/" + deviceId + "/" + action, "{}");
        clearDeviceCache(deviceId);
    }

    public void setWaterBudget(String deviceId, int percentage) throws RachioApiException {
        String body = String.format("{\"percentage\":%d}", percentage);
        httpClient.put(BASE_URL + "/device/" + deviceId + "/water_budget", body);
        clearDeviceCache(deviceId);
    }

    public void runAllZones(String deviceId) throws RachioApiException {
        httpClient.put(BASE_URL + "/device/" + deviceId + "/start", "{}");
        clearDeviceCache(deviceId);
    }

    public void runNextZone(String deviceId) throws RachioApiException {
        httpClient.put(BASE_URL + "/device/" + deviceId + "/start_next", "{}");
        clearDeviceCache(deviceId);
    }

    public void turnDeviceOn(String deviceId) throws RachioApiException {
        httpClient.put(BASE_URL + "/device/" + deviceId + "/on", "{}");
        clearDeviceCache(deviceId);
    }

    public void turnDeviceOff(String deviceId) throws RachioApiException {
        httpClient.put(BASE_URL + "/device/" + deviceId + "/off", "{}");
        clearDeviceCache(deviceId);
    }

    public void setZoneEnabled(String zoneId, boolean enabled) throws RachioApiException {
        String body = String.format("{\"enabled\":%b}", enabled);
        httpClient.put(BASE_URL + "/zone/" + zoneId, body);
        clearDeviceCacheForZone(zoneId);
    }

    // Webhook operations
    public void registerWebhook(String url, String externalId) throws RachioApiException {
        String body = String.format(
            "{\"url\":\"%s\",\"externalId\":\"%s\",\"eventTypes\":[\"DEVICE_STATUS_EVENT\"]}",
            url, externalId
        );
        httpClient.post(BASE_URL + "/webhook", body);
    }

    public void unregisterWebhook(String webhookId) throws RachioApiException {
        httpClient.delete(BASE_URL + "/webhook/" + webhookId);
    }

    // Weather integration (uses existing DTOs)
    public RachioForecast getForecast(String deviceId) throws RachioApiException {
        String response = httpClient.get(BASE_URL + "/device/" + deviceId + "/forecast");
        try {
            return mapper.readValue(response, RachioForecast.class);
        } catch (Exception e) {
            throw new RachioApiException("Failed to parse forecast", e);
        }
    }

    // Cache management methods
    public void clearCache() {
        deviceCache.clear();
        zoneToDeviceMap.clear();
        cacheTimestamps.clear();
        logger.debug("Cleared all API client caches");
    }

    public void clearDeviceCache(String deviceId) {
        deviceCache.remove(deviceId);
        cacheTimestamps.remove(deviceId);
        // Remove all zones for this device from the mapping
        zoneToDeviceMap.entrySet().removeIf(entry -> deviceId.equals(entry.getValue()));
        logger.debug("Cleared cache for device {}", deviceId);
    }

    private void clearDeviceCacheForZone(String zoneId) {
        String deviceId = zoneToDeviceMap.get(zoneId);
        if (deviceId != null) {
            clearDeviceCache(deviceId);
        }
    }

    public void invalidateCache() {
        cacheTimestamps.clear(); // Force refresh on next getDevice() call
        logger.debug("Invalidated cache timestamps");
    }

    // Polling for status updates
    private void startPolling() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                // Periodically invalidate cache to ensure fresh data
                invalidateCache();
                logger.debug("Periodic cache invalidation completed");
            } catch (Exception e) {
                logger.debug("Cache invalidation failed: {}", e.getMessage());
            }
        }, 10, 10, TimeUnit.MINUTES); // Invalidate cache every 10 minutes
    }
}
