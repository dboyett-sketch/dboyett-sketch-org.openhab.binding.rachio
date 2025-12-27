package org.openhab.binding.rachio.internal.api;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
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
import org.openhab.binding.rachio.internal.api.dto.RachioSavings;
import org.openhab.binding.rachio.internal.api.dto.RachioSchedule;
import org.openhab.binding.rachio.internal.api.dto.RachioUsage;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.ZoneRunStatus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Client for the Rachio API
 * 
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class RachioApiClient {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantTypeAdapter()).create();

    public RachioApiClient() {
        // API key will be initialized later via the initialize() method
    }

    /**
     * Initialize the client with an API key.
     * This method MUST be called after construction before any API calls are made.
     * 
     * @param apiKey The Rachio API key
     */
    public void initialize(String apiKey) {
        // Set the API key in the static RachioHttp class
        RachioHttp.setApiKey(apiKey);
    }

    /**
     * Get person info for the authenticated user
     * 
     * @return Person object
     */
    public @Nullable RachioPerson getPersonInfo() throws RachioApiException {
        String response = RachioHttp.get(RachioBindingConstants.API_PERSON_ENDPOINT);

        // Handle empty response
        if (response == null || response.isEmpty()) {
            return null;
        }

        return gson.fromJson(response, RachioPerson.class);
    }

    /**
     * Get all devices for the authenticated user
     * 
     * @return List of devices
     */
    @SuppressWarnings("unchecked")
    public @Nullable List<RachioDevice> getDevices() throws RachioApiException {
        String response = RachioHttp.get(RachioBindingConstants.API_DEVICE_ENDPOINT);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<List<RachioDevice>>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get a specific device by ID
     * 
     * @param deviceId Device ID
     * @return Device object
     */
    public @Nullable RachioDevice getDevice(String deviceId) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_DEVICE_ENDPOINT + "/" + deviceId;
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        return gson.fromJson(response, RachioDevice.class);
    }

    /**
     * Get zones for a device
     * 
     * @param deviceId Device ID
     * @return List of zones
     */
    @SuppressWarnings("unchecked")
    public @Nullable List<RachioZone> getZones(String deviceId) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_DEVICE_ENDPOINT + "/" + deviceId + "/zone";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<List<RachioZone>>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Start a zone
     * 
     * @param zoneId Zone ID
     * @param duration Duration in seconds
     */
    public void startZone(String zoneId, int duration) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_ZONE_ENDPOINT + "/" + zoneId + "/start";
        String payload = "{\"duration\":" + duration + "}";
        RachioHttp.put(endpoint, payload);
    }

    /**
     * Stop a zone
     * 
     * @param zoneId Zone ID
     */
    public void stopZone(String zoneId) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_ZONE_ENDPOINT + "/" + zoneId + "/stop";
        RachioHttp.put(endpoint, "");
    }

    /**
     * Start multiple zones
     * 
     * @param zones List of zone IDs and durations
     */
    public void startMultipleZones(List<ZoneRunStatus> zones) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_ZONE_ENDPOINT + "/start_multiple";
        String payload = gson.toJson(zones);
        RachioHttp.put(endpoint, payload);
    }

    /**
     * Get zone run status
     * 
     * @param zoneId Zone ID
     * @return Zone run status
     */
    @SuppressWarnings("unchecked")
    public @Nullable ZoneRunStatus getZoneRunStatus(String zoneId) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_ZONE_ENDPOINT + "/" + zoneId + "/current_schedule";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<ZoneRunStatus>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get device schedule
     * 
     * @param deviceId Device ID
     * @return List of schedules
     */
    @SuppressWarnings("unchecked")
    public @Nullable List<RachioSchedule> getSchedule(String deviceId) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_DEVICE_ENDPOINT + "/" + deviceId + "/schedule";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<List<RachioSchedule>>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get device forecast
     * 
     * @param deviceId Device ID
     * @return Forecast object
     */
    @SuppressWarnings("unchecked")
    public @Nullable RachioForecast getForecast(String deviceId) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_DEVICE_ENDPOINT + "/" + deviceId + "/forecast";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<RachioForecast>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get device usage
     * 
     * @param deviceId Device ID
     * @return Usage object
     */
    @SuppressWarnings("unchecked")
    public @Nullable RachioUsage getUsage(String deviceId) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_DEVICE_ENDPOINT + "/" + deviceId + "/water_usage";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<RachioUsage>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get device savings
     * 
     * @param deviceId Device ID
     * @return Savings object
     */
    @SuppressWarnings("unchecked")
    public @Nullable RachioSavings getSavings(String deviceId) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_DEVICE_ENDPOINT + "/" + deviceId + "/savings";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<RachioSavings>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get device alerts
     * 
     * @param deviceId Device ID
     * @return List of alerts
     */
    @SuppressWarnings("unchecked")
    public @Nullable List<RachioAlert> getAlerts(String deviceId) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_DEVICE_ENDPOINT + "/" + deviceId + "/alert";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<List<RachioAlert>>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get event summary for a device
     * 
     * @param deviceId Device ID
     * @return Event summary
     */
    public @Nullable RachioEventSummary getEventSummary(String deviceId) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_DEVICE_ENDPOINT + "/" + deviceId + "/event";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        return gson.fromJson(response, RachioEventSummary.class);
    }

    /**
     * Get custom soil data
     * 
     * @return List of custom soils
     */
    @SuppressWarnings("unchecked")
    public @Nullable List<CustomSoil> getCustomSoils() throws RachioApiException {
        String endpoint = "/custom_soil";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<List<CustomSoil>>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get custom crop data
     * 
     * @return List of custom crops
     */
    @SuppressWarnings("unchecked")
    public @Nullable List<CustomCrop> getCustomCrops() throws RachioApiException {
        String endpoint = "/custom_crop";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<List<CustomCrop>>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get custom nozzle data
     * 
     * @return List of custom nozzles
     */
    @SuppressWarnings("unchecked")
    public @Nullable List<CustomNozzle> getCustomNozzles() throws RachioApiException {
        String endpoint = "/custom_nozzle";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<List<CustomNozzle>>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get custom slope data
     * 
     * @return List of custom slopes
     */
    @SuppressWarnings("unchecked")
    public @Nullable List<CustomSlope> getCustomSlopes() throws RachioApiException {
        String endpoint = "/custom_slope";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<List<CustomSlope>>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get custom shade data
     * 
     * @return List of custom shades
     */
    @SuppressWarnings("unchecked")
    public @Nullable List<CustomShade> getCustomShades() throws RachioApiException {
        String endpoint = "/custom_shade";
        String response = RachioHttp.get(endpoint);

        if (response == null || response.isEmpty()) {
            return null;
        }

        Type type = new TypeToken<List<CustomShade>>() {
        }.getType();
        return gson.fromJson(response, type);
    }

    /**
     * Get webhook ID for the current binding
     * This method must return @NonNull String according to OpenHAB 5.x patterns
     * 
     * @return Webhook ID string (never null)
     */
    public String getWebHookId() {
        // Always return a non-null string as required by the binding architecture
        return RachioBindingConstants.WEBHOOK_ID;
    }

    // ===== ADDED METHODS TO FIX COMPILATION ERRORS =====

    /**
     * Register a webhook with Rachio
     * 
     * @param url Webhook callback URL
     * @return true if registration was successful
     */
    public boolean registerWebhook(String url) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_WEBHOOK_ENDPOINT;
        String payload = String.format("{\"url\":\"%s\",\"externalId\":\"%s\"}", url,
                RachioBindingConstants.WEBHOOK_ID);
        try {
            RachioHttp.put(endpoint, payload);
            return true;
        } catch (RachioApiException e) {
            // Log the error and rethrow or return false based on your error handling policy
            throw e;
        }
    }

    /**
     * Unregister a webhook by URL
     * 
     * @param url Webhook callback URL
     */
    public void unregisterWebhookByUrl(String url) throws RachioApiException {
        // Implementation for deleting a webhook by URL
        // This would typically involve getting all webhooks, finding the one with the matching URL, and deleting it by
        // ID
        // For now, we'll implement a stub that makes a DELETE request
        // Note: The Rachio API might require the webhook ID, not the URL, for deletion
        String endpoint = RachioBindingConstants.API_WEBHOOK_ENDPOINT;
        // This is a simplified implementation. A real implementation would need the webhook ID.
        // String payload = String.format("{\"url\":\"%s\"}", url);
        // RachioHttp.delete(endpoint + "?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8));
        // For compilation, we'll keep it simple:
        throw new RachioApiException("unregisterWebhookByUrl not fully implemented. Needs webhook ID lookup.");
    }

    /**
     * Set the paused state of a device.
     * CRITICAL: Added to fix "setDevicePaused is undefined" error in RachioDeviceHandler.java
     * 
     * @param deviceId The ID of the device to pause or unpause
     * @param paused true to pause the device, false to resume
     * @throws RachioApiException if the API request fails
     */
    public void setDevicePaused(String deviceId, boolean paused) throws RachioApiException {
        String endpoint = RachioBindingConstants.API_DEVICE_ENDPOINT + "/" + deviceId + "/pause";
        String payload = String.format("{\"paused\":%s}", paused ? "true" : "false");
        RachioHttp.put(endpoint, payload);
    }

    /**
     * Get a specific zone by ID.
     * CRITICAL: Added to fix "getZone is undefined" error referenced in discovery service.
     * 
     * @param zoneId Zone ID
     * @return Zone object
     * @throws RachioApiException if the API request fails
     */
    public @Nullable RachioZone getZone(String zoneId) throws RachioApiException {
        // Note: The Rachio API might not have a direct endpoint for a single zone.
        // A common pattern is to get all zones for a device and filter.
        // This method signature exists to satisfy the interface.
        // A proper implementation would require the device ID context.
        throw new RachioApiException(
                "getZone(String zoneId) requires device context. Use getZones(deviceId) and filter.");
    }
}
