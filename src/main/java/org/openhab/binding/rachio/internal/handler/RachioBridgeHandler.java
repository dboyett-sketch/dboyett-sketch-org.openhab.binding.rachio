package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioActions;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioWebHookServletService;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioEventSummary;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.binding.rachio.internal.discovery.RachioDiscoveryService;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Michael Lobstein - Initial contribution
 * @author Damion Boyett - Enhanced for OpenHAB 5.x
 */
@NonNullByDefault
@Component(service = RachioBridgeHandler.class)
public class RachioBridgeHandler extends BaseBridgeHandler implements RachioActions {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    private final Gson gson = new Gson();
    private final HttpClientFactory httpClientFactory;
    private final Set<RachioStatusListener> statusListeners = Collections.synchronizedSet(new HashSet<>());

    private @Nullable RachioHttp rachioHttp;
    private @Nullable RachioWebHookServletService webhookService;
    private @Nullable ScheduledFuture<?> pollingJob;
    private @Nullable ScheduledFuture<?> statusJob;
    private RachioBridgeConfiguration config = new RachioBridgeConfiguration();
    private @Nullable RachioPerson person;
    private Map<String, RachioDevice> devices = new ConcurrentHashMap<>();
    private long lastPollTime = 0;
    private long pollInterval = DEFAULT_POLL_INTERVAL_SECONDS;
    private int adaptivePollingMultiplier = 1;
    private boolean discoveryEnabled = false;

    @Activate
    public RachioBridgeHandler(@Reference HttpClientFactory httpClientFactory, Bridge bridge) {
        super(bridge);
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler.");
        config = getConfigAs(RachioBridgeConfiguration.class);

        if (config.apiKey.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "API Key must be configured for Rachio bridge");
            return;
        }

        try {
            // Initialize HTTP client
            rachioHttp = new RachioHttp(httpClientFactory, config.apiKey, this::handleApiException);
            
            // Initialize webhook service
            webhookService = new RachioWebHookServletService(this, scheduler);
            
            // Start polling
            startPolling();
            
            // Start status update job
            startStatusUpdates();
            
            // Update bridge status
            updateStatus(ThingStatus.ONLINE);
            
        } catch (Exception e) {
            logger.error("Failed to initialize Rachio bridge: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Rachio bridge handler.");
        
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null) {
            localPollingJob.cancel(true);
            pollingJob = null;
        }
        
        ScheduledFuture<?> localStatusJob = statusJob;
        if (localStatusJob != null) {
            localStatusJob.cancel(true);
            statusJob = null;
        }
        
        RachioWebHookServletService localWebhookService = webhookService;
        if (localWebhookService != null) {
            localWebhookService.deactivate();
            webhookService = null;
        }
        
        statusListeners.clear();
        devices.clear();
        
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshChannel(channelUID);
        } else {
            switch (channelUID.getIdWithoutGroup()) {
                case CHANNEL_REFRESH:
                    if (command instanceof OnOffType && command == OnOffType.ON) {
                        refreshAllData();
                        updateState(channelUID, OnOffType.OFF);
                    }
                    break;
                case CHANNEL_POLL_INTERVAL:
                    if (command instanceof DecimalType) {
                        int newInterval = ((DecimalType) command).intValue();
                        if (newInterval >= MIN_POLL_INTERVAL_SECONDS && newInterval <= MAX_POLL_INTERVAL_SECONDS) {
                            pollInterval = newInterval;
                            restartPolling();
                            updateState(channelUID, new DecimalType(pollInterval));
                        }
                    }
                    break;
            }
        }
    }

    private void refreshChannel(ChannelUID channelUID) {
        switch (channelUID.getIdWithoutGroup()) {
            case CHANNEL_API_STATUS:
                updateApiStatus();
                break;
            case CHANNEL_RATE_LIMIT_REMAINING:
                updateRateLimitRemaining();
                break;
            case CHANNEL_RATE_LIMIT_PERCENT:
                updateRateLimitPercent();
                break;
            case CHANNEL_RATE_LIMIT_STATUS:
                updateRateLimitStatus();
                break;
            case CHANNEL_RATE_LIMIT_RESET:
                updateRateLimitReset();
                break;
            case CHANNEL_LAST_POLL:
                updateState(channelUID, new DateTimeType(ZonedDateTime.now()));
                break;
        }
    }

    private void startPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null) {
            localPollingJob.cancel(true);
        }
        
        pollingJob = scheduler.scheduleWithFixedDelay(this::pollData, 10, pollInterval * adaptivePollingMultiplier, TimeUnit.SECONDS);
        logger.debug("Started polling with interval {} seconds (multiplier: {})", 
            pollInterval * adaptivePollingMultiplier, adaptivePollingMultiplier);
    }

    private void startStatusUpdates() {
        ScheduledFuture<?> localStatusJob = statusJob;
        if (localStatusJob != null) {
            localStatusJob.cancel(true);
        }
        
        statusJob = scheduler.scheduleWithFixedDelay(this::updateStatusChannels, 30, 30, TimeUnit.SECONDS);
    }

    private void restartPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null) {
            localPollingJob.cancel(true);
        }
        startPolling();
    }

    private void pollData() {
        try {
            logger.debug("Polling Rachio data...");
            
            // Update rate limit info first
            updateRateLimitInfo();
            
            // Get person info
            updatePersonInfo();
            
            // Get devices
            updateDevices();
            
            // Update webhook health
            updateWebhookHealth();
            
            // Update status channels
            updateStatusChannels();
            
            lastPollTime = System.currentTimeMillis();
            updateState(CHANNEL_LAST_POLL, new DateTimeType(ZonedDateTime.now()));
            
        } catch (Exception e) {
            logger.error("Error during polling: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void updatePersonInfo() throws RachioApiException, IOException {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            person = localRachioHttp.getPerson();
            if (person != null) {
                logger.debug("Found person: {} {}", person.firstName, person.lastName);
            }
        }
    }

    private void updateDevices() throws RachioApiException, IOException {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null && person != null) {
            List<RachioDevice> deviceList = localRachioHttp.getDevices(person.id);
            devices.clear();
            
            for (RachioDevice device : deviceList) {
                devices.put(device.id, device);
                logger.debug("Found device: {} ({})", device.name, device.id);
                
                // Notify listeners
                notifyDeviceUpdated(device);
                
                // Update device status
                updateDeviceStatus(device);
            }
            
            // Update child things
            updateChildThings();
        }
    }

    private void updateDeviceStatus(RachioDevice device) {
        // Update device status channel if this is the bridge device
        if (device.id.equals(getThing().getUID().getId())) {
            updateState(CHANNEL_DEVICE_STATUS, new StringType(device.status));
            updateState(CHANNEL_DEVICE_ONLINE, device.status.equals("ONLINE") ? OnOffType.ON : OnOffType.OFF);
        }
    }

    private void updateChildThings() {
        getThing().getThings().forEach(thing -> {
            RachioHandler handler = (RachioHandler) thing.getHandler();
            if (handler != null) {
                handler.refresh();
            }
        });
    }

    private void refreshAllData() {
        logger.debug("Manual refresh requested");
        scheduler.submit(this::pollData);
    }

    private void updateRateLimitInfo() {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                // Get rate limit info from HTTP client
                int remaining = localRachioHttp.getRateLimitRemaining();
                int limit = localRachioHttp.getRateLimitLimit();
                long reset = localRachioHttp.getRateLimitReset();
                
                // Update adaptive polling
                updateAdaptivePolling(remaining, limit);
                
                // Update channels
                updateState(CHANNEL_RATE_LIMIT_REMAINING, new DecimalType(remaining));
                updateState(CHANNEL_RATE_LIMIT_PERCENT, new QuantityType<>(remaining * 100.0 / limit, Units.PERCENT));
                updateState(CHANNEL_RATE_LIMIT_RESET, new DateTimeType(ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(reset), java.time.ZoneId.systemDefault())));
                
                // Update status
                updateRateLimitStatus();
                
            } catch (Exception e) {
                logger.debug("Failed to update rate limit info: {}", e.getMessage());
            }
        }
    }

    private void updateAdaptivePolling(int remaining, int limit) {
        int oldMultiplier = adaptivePollingMultiplier;
        
        if (remaining < limit * 0.1) { // Less than 10% remaining
            adaptivePollingMultiplier = 3; // Slow down to 3x normal interval
        } else if (remaining < limit * 0.25) { // Less than 25% remaining
            adaptivePollingMultiplier = 2; // Slow down to 2x normal interval
        } else {
            adaptivePollingMultiplier = 1; // Normal speed
        }
        
        if (adaptivePollingMultiplier != oldMultiplier) {
            logger.info("Adaptive polling changed from {}x to {}x (remaining: {}/{})", 
                oldMultiplier, adaptivePollingMultiplier, remaining, limit);
            restartPolling();
        }
    }

    private void updateApiStatus() {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                // Simple API call to check status
                localRachioHttp.getPerson();
                updateState(CHANNEL_API_STATUS, new StringType("OK"));
            } catch (Exception e) {
                updateState(CHANNEL_API_STATUS, new StringType("ERROR: " + e.getMessage()));
            }
        }
    }

    private void updateRateLimitRemaining() {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                int remaining = localRachioHttp.getRateLimitRemaining();
                updateState(CHANNEL_RATE_LIMIT_REMAINING, new DecimalType(remaining));
            } catch (Exception e) {
                logger.debug("Failed to get rate limit remaining: {}", e.getMessage());
            }
        }
    }

    private void updateRateLimitPercent() {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                int remaining = localRachioHttp.getRateLimitRemaining();
                int limit = localRachioHttp.getRateLimitLimit();
                updateState(CHANNEL_RATE_LIMIT_PERCENT, new QuantityType<>(remaining * 100.0 / limit, Units.PERCENT));
            } catch (Exception e) {
                logger.debug("Failed to get rate limit percent: {}", e.getMessage());
            }
        }
    }

    private void updateRateLimitStatus() {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                int remaining = localRachioHttp.getRateLimitRemaining();
                int limit = localRachioHttp.getRateLimitLimit();
                
                String status;
                if (remaining == 0) {
                    status = "LIMIT_EXCEEDED";
                } else if (remaining < limit * 0.1) {
                    status = "CRITICAL";
                } else if (remaining < limit * 0.25) {
                    status = "WARNING";
                } else if (remaining < limit * 0.5) {
                    status = "GOOD";
                } else {
                    status = "EXCELLENT";
                }
                
                updateState(CHANNEL_RATE_LIMIT_STATUS, new StringType(status));
            } catch (Exception e) {
                logger.debug("Failed to get rate limit status: {}", e.getMessage());
            }
        }
    }

    private void updateRateLimitReset() {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                long reset = localRachioHttp.getRateLimitReset();
                updateState(CHANNEL_RATE_LIMIT_RESET, new DateTimeType(ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(reset), java.time.ZoneId.systemDefault())));
            } catch (Exception e) {
                logger.debug("Failed to get rate limit reset: {}", e.getMessage());
            }
        }
    }

    private void updateStatusChannels() {
        updateApiStatus();
        updateRateLimitRemaining();
        updateRateLimitPercent();
        updateRateLimitStatus();
        updateRateLimitReset();
        updateWebhookHealth();
    }

    private void updateWebhookHealth() {
        RachioWebHookServletService localWebhookService = webhookService;
        if (localWebhookService != null) {
            String health = localWebhookService.getHealthStatus();
            updateState(CHANNEL_WEBHOOK_HEALTH, new StringType(health));
        }
    }

    public void handleWebhookEvent(RachioWebhookEvent event) {
        logger.debug("Received webhook event: {} for device {}", event.type, event.deviceId);
        
        // Update device status if this is a status event
        if ("DEVICE_STATUS".equals(event.type) && event.deviceId != null) {
            updateState(CHANNEL_DEVICE_STATUS, new StringType(event.subType));
            updateState(CHANNEL_DEVICE_ONLINE, "ONLINE".equals(event.subType) ? OnOffType.ON : OnOffType.OFF);
        }
        
        // Notify listeners
        synchronized (statusListeners) {
            for (RachioStatusListener listener : statusListeners) {
                try {
                    listener.onStatusChanged(event);
                } catch (Exception e) {
                    logger.warn("Error notifying listener: {}", e.getMessage());
                }
            }
        }
        
        // Trigger refresh if needed
        if (shouldRefreshOnEvent(event)) {
            scheduler.submit(this::refreshDeviceData);
        }
    }

    private boolean shouldRefreshOnEvent(RachioWebhookEvent event) {
        return "ZONE_STATUS".equals(event.type) || 
               "DEVICE_STATUS".equals(event.type) ||
               "SCHEDULE_STATUS".equals(event.type);
    }

    private void refreshDeviceData() {
        try {
            updateDevices();
        } catch (Exception e) {
            logger.debug("Failed to refresh device data after webhook: {}", e.getMessage());
        }
    }

    private void handleApiException(Exception e) {
        if (e instanceof RachioApiException) {
            RachioApiException apiEx = (RachioApiException) e;
            if (apiEx.getStatusCode() == 429) { // Rate limit exceeded
                logger.warn("API rate limit exceeded, slowing down polling");
                adaptivePollingMultiplier = Math.min(adaptivePollingMultiplier * 2, 10); // Max 10x slowdown
                restartPolling();
                updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Rate limit exceeded, slowing down");
            } else if (apiEx.getStatusCode() >= 500) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "API server error");
            } else if (apiEx.getStatusCode() == 401) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid API key");
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    public void addStatusListener(RachioStatusListener listener) {
        statusListeners.add(listener);
    }

    public void removeStatusListener(RachioStatusListener listener) {
        statusListeners.remove(listener);
    }

    public @Nullable RachioDevice getDevice(String deviceId) {
        return devices.get(deviceId);
    }

    public @Nullable RachioZone getZone(String deviceId, String zoneId) {
        RachioDevice device = devices.get(deviceId);
        if (device != null && device.zones != null) {
            for (RachioZone zone : device.zones) {
                if (zone.id.equals(zoneId)) {
                    return zone;
                }
            }
        }
        return null;
    }

    public List<RachioDevice> getDevices() {
        return new ArrayList<>(devices.values());
    }

    public @Nullable RachioHttp getRachioHttp() {
        return rachioHttp;
    }

    public @Nullable RachioWebHookServletService getWebhookService() {
        return webhookService;
    }

    public boolean isDiscoveryEnabled() {
        return discoveryEnabled;
    }

    public void setDiscoveryEnabled(boolean enabled) {
        this.discoveryEnabled = enabled;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(RachioDiscoveryService.class);
    }

    // Implementation of RachioActions interface methods
    @Override
    public int getAdaptivePollingMultiplier() {
        return adaptivePollingMultiplier;
    }

    @Override
    public void startZone(String deviceId, String zoneId, int duration, String source) throws RachioApiException {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                localRachioHttp.startZone(deviceId, zoneId, duration, source);
            } catch (IOException e) {
                throw new RachioApiException("Failed to start zone: " + e.getMessage(), e);
            }
        } else {
            throw new RachioApiException("Rachio HTTP client not initialized");
        }
    }

    @Override
    public void stopWatering(String deviceId, String source) throws RachioApiException {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                localRachioHttp.stopWatering(deviceId, source);
            } catch (IOException e) {
                throw new RachioApiException("Failed to stop watering: " + e.getMessage(), e);
            }
        } else {
            throw new RachioApiException("Rachio HTTP client not initialized");
        }
    }

    @Override
    public void runAllZones(String deviceId, int duration, String source) throws RachioApiException {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                localRachioHttp.runAllZones(deviceId, duration, source);
            } catch (IOException e) {
                throw new RachioApiException("Failed to run all zones: " + e.getMessage(), e);
            }
        } else {
            throw new RachioApiException("Rachio HTTP client not initialized");
        }
    }

    @Override
    public void setZoneEnabled(String deviceId, String zoneId, boolean enabled, String source) throws RachioApiException {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                localRachioHttp.setZoneEnabled(deviceId, zoneId, enabled, source);
            } catch (IOException e) {
                throw new RachioApiException("Failed to set zone enabled: " + e.getMessage(), e);
            }
        } else {
            throw new RachioApiException("Rachio HTTP client not initialized");
        }
    }

    @Override
    public void rainDelay(String deviceId, int duration) throws RachioApiException {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                localRachioHttp.rainDelay(deviceId, duration);
            } catch (IOException e) {
                throw new RachioApiException("Failed to set rain delay: " + e.getMessage(), e);
            }
        } else {
            throw new RachioApiException("Rachio HTTP client not initialized");
        }
    }

    @Override
    public void runNextZone(String deviceId, String source) throws RachioApiException {
        RachioHttp localRachioHttp = rachioHttp;
        if (localRachioHttp != null) {
            try {
                localRachioHttp.runNextZone(deviceId, source);
            } catch (IOException e) {
                throw new RachioApiException("Failed to run next zone: " + e.getMessage(), e);
            }
        } else {
            throw new RachioApiException("Rachio HTTP client not initialized");
        }
    }

    private void notifyDeviceUpdated(RachioDevice device) {
        synchronized (statusListeners) {
            for (RachioStatusListener listener : statusListeners) {
                try {
                    listener.onDeviceUpdated(device);
                } catch (Exception e) {
                    logger.warn("Error notifying device update listener: {}", e.getMessage());
                }
            }
        }
    }
}
