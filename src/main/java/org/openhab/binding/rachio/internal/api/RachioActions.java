package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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

import java.util.List;
import java.util.Map;

/**
 * Interface defining Rachio actions that can be performed from rules or scripts.
 * This provides a clean API for interacting with Rachio devices.
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public interface RachioActions {

    /**
     * Start watering a specific zone
     *
     * @param thingId The OpenHAB thing ID
     * @param zoneId The Rachio zone ID
     * @param duration Duration in seconds
     * @param deviceId The Rachio device ID
     */
    void startZone(String thingId, String zoneId, int duration, String deviceId);

    /**
     * Stop all watering on a device
     *
     * @param thingId The OpenHAB thing ID
     * @param deviceId The Rachio device ID
     */
    void stopWatering(String thingId, String deviceId);

    /**
     * Run all zones sequentially with the same duration
     *
     * @param thingId The OpenHAB thing ID
     * @param duration Duration in seconds for each zone
     * @param deviceId The Rachio device ID
     */
    void runAllZones(String thingId, int duration, String deviceId);

    /**
     * Set a rain delay on a device
     *
     * @param thingId The OpenHAB thing ID
     * @param hours Number of hours to delay watering
     * @param deviceId The Rachio device ID
     */
    void rainDelay(String thingId, int hours, String deviceId);

    /**
     * Run the next available zone (first non-running zone)
     *
     * @param thingId The OpenHAB thing ID
     * @param duration Duration in seconds
     * @param deviceId The Rachio device ID
     */
    void runNextZone(String thingId, int duration, String deviceId);

    /**
     * Enable or disable a zone
     *
     * @param thingId The OpenHAB thing ID
     * @param zoneId The Rachio zone ID
     * @param enabled True to enable, false to disable
     * @param deviceId The Rachio device ID
     */
    void setZoneEnabled(String thingId, String zoneId, boolean enabled, String deviceId);

    /**
     * Get person (user) information
     *
     * @return Person information or null if not available
     * @throws RachioApiException if API call fails
     */
    @Nullable
    RachioPerson getPerson() throws RachioApiException;

    /**
     * Get device information
     *
     * @param deviceId The Rachio device ID
     * @return Device information or null if not found
     * @throws RachioApiException if API call fails
     */
    @Nullable
    RachioDevice getDevice(String deviceId) throws RachioApiException;

    /**
     * Get all devices for the current user
     *
     * @return List of devices
     * @throws RachioApiException if API call fails
     */
    List<RachioDevice> getDevices() throws RachioApiException;

    /**
     * Get events for a device
     *
     * @param deviceId The Rachio device ID
     * @param count Number of events to retrieve
     * @return List of event summaries
     * @throws RachioApiException if API call fails
     */
    List<RachioEventSummary> getEvents(String deviceId, int count) throws RachioApiException;

    /**
     * Get weather forecast for a device
     *
     * @param deviceId The Rachio device ID
     * @return Forecast information or null if not available
     * @throws RachioApiException if API call fails
     */
    @Nullable
    RachioForecast getForecast(String deviceId) throws RachioApiException;

    /**
     * Get water usage data for a device
     *
     * @param deviceId The Rachio device ID
     * @param year The year to get usage for
     * @return Usage information or null if not available
     * @throws RachioApiException if API call fails
     */
    @Nullable
    RachioUsage getUsage(String deviceId, int year) throws RachioApiException;

    /**
     * Get water savings data for a device
     *
     * @param deviceId The Rachio device ID
     * @param year The year to get savings for
     * @return Savings information or null if not available
     * @throws RachioApiException if API call fails
     */
    @Nullable
    RachioUsage getSavings(String deviceId, int year) throws RachioApiException;

    /**
     * Pause or unpause a device
     *
     * @param deviceId The Rachio device ID
     * @param pause True to pause, false to unpause
     * @throws RachioApiException if API call fails
     */
    void pauseDevice(String deviceId, boolean pause) throws RachioApiException;

    /**
     * Get alerts for a device
     *
     * @param deviceId The Rachio device ID
     * @return List of alerts
     * @throws RachioApiException if API call fails
     */
    List<RachioAlert> getAlerts(String deviceId) throws RachioApiException;

    /**
     * Get schedules for a device
     *
     * @param deviceId The Rachio device ID
     * @return List of schedules
     * @throws RachioApiException if API call fails
     */
    List<RachioSchedule> getSchedules(String deviceId) throws RachioApiException;

    /**
     * Get watering history for a zone
     *
     * @param zoneId The Rachio zone ID
     * @param count Number of history entries to retrieve
     * @return List of watering history entries
     * @throws RachioApiException if API call fails
     */
    List<Map<String, Object>> getWateringHistory(String zoneId, int count) throws RachioApiException;

    /**
     * Process a webhook event
     *
     * @param event The webhook event
     */
    void processWebhookEvent(RachioWebhookEvent event);

    /**
     * Clear the device cache
     */
    void clearCache();

    /**
     * Get current cache size
     *
     * @return Number of cached devices
     */
    int getCacheSize();

    /**
     * Get rate limit information
     *
     * @return Map containing rate limit details
     */
    Map<String, Object> getRateLimitInfo();

    /**
     * Get adaptive polling multiplier
     *
     * @return Current polling multiplier (1, 2, or 3)
     */
    int getAdaptivePollingMultiplier();

    /**
     * Validate webhook signature
     *
     * @param payload The webhook payload
     * @param signature The received signature
     * @param secret The webhook secret
     * @return True if signature is valid
     */
    boolean validateWebhookSignature(String payload, String signature, String secret);
}
