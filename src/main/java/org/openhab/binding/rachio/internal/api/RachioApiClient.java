package org.openhab.binding.rachio.internal.api;

import org.openhab.binding.rachio.internal.api.dto.*;
import org.openhab.binding.rachio.internal.api.exception.RachioApiException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Rachio API Client - Main interface to Rachio Cloud API
 * Uses RachioHttp service which provides OpenHAB 5.x compatible HTTP client
 */
@Component(service = RachioApiClient.class, immediate = true)
public class RachioApiClient {
    private final Logger logger = LoggerFactory.getLogger(RachioApiClient.class);
    
    // JSON parser with custom type adapters
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .registerTypeAdapterFactory(new SafeReflectiveTypeAdapterFactory())
            .create();
    
    // Injected HTTP service (OpenHAB 5.x compatible)
    private RachioHttp rachioHttp;
    
    @Reference
    public void setRachioHttp(RachioHttp rachioHttp) {
        this.rachioHttp = rachioHttp;
        logger.debug("RachioHttp service injected into RachioApiClient");
    }
    
    public void unsetRachioHttp(RachioHttp rachioHttp) {
        this.rachioHttp = null;
    }
    
    @Activate
    protected void activate() {
        logger.debug("RachioApiClient service activated");
    }
    
    @Deactivate
    protected void deactivate() {
        logger.debug("RachioApiClient service deactivated");
    }
    
    @Modified
    protected void modified() {
        logger.debug("RachioApiClient service configuration modified");
    }
    
    /**
     * Validate API key by attempting to fetch person info
     */
    public boolean validateApiKey(String apiKey) throws RachioApiException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RachioApiException("API key cannot be null or empty");
        }
        
        try {
            RachioPerson person = rachioHttp.getPerson(apiKey);
            return person != null && person.id != null && !person.id.isEmpty();
        } catch (RachioApiException e) {
            logger.debug("API key validation failed: {}", e.getMessage());
            throw new RachioApiException("Invalid API key: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get person information
     */
    public RachioPerson getPerson(String apiKey) throws RachioApiException {
        validateApiKey(apiKey);
        return rachioHttp.getPerson(apiKey);
    }
    
    /**
     * Get all devices for the authenticated person
     */
    public List<RachioDevice> getDevices(String apiKey) throws RachioApiException {
        validateApiKey(apiKey);
        
        try {
            List<RachioDevice> devices = rachioHttp.getDevices(apiKey);
            logger.debug("Retrieved {} devices from Rachio API", devices != null ? devices.size() : 0);
            return devices != null ? devices : Collections.emptyList();
        } catch (RachioApiException e) {
            logger.error("Failed to retrieve devices: {}", e.getMessage());
            throw new RachioApiException("Failed to get devices: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get specific device by ID
     */
    public RachioDevice getDevice(String apiKey, String deviceId) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new RachioApiException("Device ID cannot be null or empty");
        }
        
        try {
            return rachioHttp.getDevice(apiKey, deviceId);
        } catch (RachioApiException e) {
            logger.error("Failed to retrieve device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get device " + deviceId + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Start watering a zone
     */
    public void startZone(String apiKey, String zoneId, int durationSeconds) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (zoneId == null || zoneId.trim().isEmpty()) {
            throw new RachioApiException("Zone ID cannot be null or empty");
        }
        
        if (durationSeconds <= 0) {
            throw new RachioApiException("Duration must be positive");
        }
        
        if (durationSeconds > 10800) { // 3 hours max per Rachio API
            throw new RachioApiException("Duration cannot exceed 10800 seconds (3 hours)");
        }
        
        try {
            rachioHttp.startZone(apiKey, zoneId, durationSeconds);
            logger.info("Started zone {} for {} seconds", zoneId, durationSeconds);
        } catch (RachioApiException e) {
            logger.error("Failed to start zone {}: {}", zoneId, e.getMessage());
            throw new RachioApiException("Failed to start zone " + zoneId + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Stop watering a zone
     */
    public void stopZone(String apiKey, String zoneId) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (zoneId == null || zoneId.trim().isEmpty()) {
            throw new RachioApiException("Zone ID cannot be null or empty");
        }
        
        try {
            rachioHttp.stopZone(apiKey, zoneId);
            logger.info("Stopped zone {}", zoneId);
        } catch (RachioApiException e) {
            logger.error("Failed to stop zone {}: {}", zoneId, e.getMessage());
            throw new RachioApiException("Failed to stop zone " + zoneId + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Get current run status for a zone
     */
    public ZoneRunStatus getZoneRunStatus(String apiKey, String zoneId) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (zoneId == null || zoneId.trim().isEmpty()) {
            throw new RachioApiException("Zone ID cannot be null or empty");
        }
        
        try {
            ZoneRunStatus status = rachioHttp.getZoneRunStatus(apiKey, zoneId);
            return status != null ? status : new ZoneRunStatus();
        } catch (RachioApiException e) {
            logger.debug("Failed to get zone status for {}: {}", zoneId, e.getMessage());
            // Return empty status instead of throwing for offline/bad zones
            return new ZoneRunStatus();
        }
    }
    
    /**
     * Check if a zone is currently running
     */
    public boolean isZoneRunning(String apiKey, String zoneId) throws RachioApiException {
        ZoneRunStatus status = getZoneRunStatus(apiKey, zoneId);
        return status != null && status.status != null && "ACTIVE".equals(status.status);
    }
    
    /**
     * Get device schedule
     */
    public List<RachioSchedule> getDeviceSchedule(String apiKey, String deviceId) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new RachioApiException("Device ID cannot be null or empty");
        }
        
        try {
            List<RachioSchedule> schedule = rachioHttp.getDeviceSchedule(apiKey, deviceId);
            return schedule != null ? schedule : Collections.emptyList();
        } catch (RachioApiException e) {
            logger.error("Failed to get schedule for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get schedule: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get device water usage
     */
    public RachioUsage getDeviceUsage(String apiKey, String deviceId, int year, int month) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new RachioApiException("Device ID cannot be null or empty");
        }
        
        if (year < 2000 || year > 2100) {
            throw new RachioApiException("Invalid year");
        }
        
        if (month < 1 || month > 12) {
            throw new RachioApiException("Invalid month");
        }
        
        try {
            return rachioHttp.getDeviceUsage(apiKey, deviceId, year, month);
        } catch (RachioApiException e) {
            logger.error("Failed to get usage for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get water usage: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get device savings data
     */
    public RachioSavings getDeviceSavings(String apiKey, String deviceId) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new RachioApiException("Device ID cannot be null or empty");
        }
        
        try {
            return rachioHttp.getDeviceSavings(apiKey, deviceId);
        } catch (RachioApiException e) {
            logger.error("Failed to get savings for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get savings data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get device alerts
     */
    public List<RachioAlert> getDeviceAlerts(String apiKey, String deviceId) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new RachioApiException("Device ID cannot be null or empty");
        }
        
        try {
            List<RachioAlert> alerts = rachioHttp.getDeviceAlerts(apiKey, deviceId);
            return alerts != null ? alerts : Collections.emptyList();
        } catch (RachioApiException e) {
            logger.error("Failed to get alerts for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get alerts: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get forecast for device
     */
    public RachioForecast getForecast(String apiKey, String deviceId) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new RachioApiException("Device ID cannot be null or empty");
        }
        
        try {
            return rachioHttp.getForecast(apiKey, deviceId);
        } catch (RachioApiException e) {
            logger.error("Failed to get forecast for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get forecast: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get event summary for device
     */
    public RachioEventSummary getEventSummary(String apiKey, String deviceId) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new RachioApiException("Device ID cannot be null or empty");
        }
        
        try {
            return rachioHttp.getEventSummary(apiKey, deviceId);
        } catch (RachioApiException e) {
            logger.error("Failed to get event summary for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to get event summary: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create webhook for device
     */
    public void createWebhook(String apiKey, String deviceId, String externalId, String url) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new RachioApiException("Device ID cannot be null or empty");
        }
        
        if (externalId == null || externalId.trim().isEmpty()) {
            throw new RachioApiException("External ID cannot be null or empty");
        }
        
        if (url == null || url.trim().isEmpty()) {
            throw new RachioApiException("Webhook URL cannot be null or empty");
        }
        
        try {
            rachioHttp.createWebhook(apiKey, deviceId, externalId, url);
            logger.info("Created webhook for device {} with externalId {}", deviceId, externalId);
        } catch (RachioApiException e) {
            logger.error("Failed to create webhook for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to create webhook: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete webhook
     */
    public void deleteWebhook(String apiKey, String webhookId) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (webhookId == null || webhookId.trim().isEmpty()) {
            throw new RachioApiException("Webhook ID cannot be null or empty");
        }
        
        try {
            rachioHttp.deleteWebhook(apiKey, webhookId);
            logger.info("Deleted webhook {}", webhookId);
        } catch (RachioApiException e) {
            logger.error("Failed to delete webhook {}: {}", webhookId, e.getMessage());
            throw new RachioApiException("Failed to delete webhook: " + e.getMessage(), e);
        }
    }
    
    /**
     * List webhooks for device
     */
    public List<String> listWebhooks(String apiKey, String deviceId) throws RachioApiException {
        validateApiKey(apiKey);
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new RachioApiException("Device ID cannot be null or empty");
        }
        
        try {
            List<String> webhooks = rachioHttp.listWebhooks(apiKey, deviceId);
            return webhooks != null ? webhooks : Collections.emptyList();
        } catch (RachioApiException e) {
            logger.error("Failed to list webhooks for device {}: {}", deviceId, e.getMessage());
            throw new RachioApiException("Failed to list webhooks: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse webhook event from JSON string
     */
    public RachioWebHookEvent parseWebhookEvent(String json) throws RachioApiException {
        if (json == null || json.trim().isEmpty()) {
            throw new RachioApiException("Webhook JSON cannot be null or empty");
        }
        
        try {
            return rachioHttp.parseWebhookEvent(json);
        } catch (RachioApiException e) {
            logger.error("Failed to parse webhook event: {}", e.getMessage());
            throw new RachioApiException("Failed to parse webhook event: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test API connection
     */
    public boolean testConnection(String apiKey) {
        try {
            return rachioHttp.testConnection(apiKey);
        } catch (Exception e) {
            logger.debug("Connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get zone by ID from device
     */
    public RachioZone getZone(String apiKey, String deviceId, String zoneId) throws RachioApiException {
        RachioDevice device = getDevice(apiKey, deviceId);
        
        if (device != null && device.zones != null) {
            for (RachioZone zone : device.zones) {
                if (zone.id != null && zone.id.equals(zoneId)) {
                    return zone;
                }
            }
        }
        
        throw new RachioApiException("Zone " + zoneId + " not found in device " + deviceId);
    }
    
    /**
     * Get all zones for a device
     */
    public List<RachioZone> getZones(String apiKey, String deviceId) throws RachioApiException {
        RachioDevice device = getDevice(apiKey, deviceId);
        
        if (device != null && device.zones != null) {
            return device.zones;
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Run multiple zones in sequence (Rachio API limitation: one at a time)
     */
    public void runZonesSequence(String apiKey, List<String> zoneIds, int durationPerZone) throws RachioApiException {
        if (zoneIds == null || zoneIds.isEmpty()) {
            throw new RachioApiException("Zone IDs list cannot be null or empty");
        }
        
        for (String zoneId : zoneIds) {
            startZone(apiKey, zoneId, durationPerZone);
            // Note: Rachio API doesn't support true sequencing - this starts them all
            // but zones will run sequentially based on Rachio hardware capabilities
        }
        
        logger.info("Started {} zones in sequence, {} seconds each", zoneIds.size(), durationPerZone);
    }
    
    /**
     * Parse JSON string to object
     */
    public <T> T fromJson(String json, Class<T> classOfT) throws RachioApiException {
        try {
            return gson.fromJson(json, classOfT);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse JSON: {}", json, e);
            throw new RachioApiException("Invalid JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert object to JSON string
     */
    public String toJson(Object object) {
        return gson.toJson(object);
    }
}
