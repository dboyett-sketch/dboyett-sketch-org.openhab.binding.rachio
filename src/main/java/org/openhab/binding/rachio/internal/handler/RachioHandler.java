package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioSecurity;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioHandler} is the base handler for Rachio things.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public abstract class RachioHandler extends BaseThingHandler implements RachioStatusListener {
    protected final Logger logger = LoggerFactory.getLogger(RachioHandler.class);

    protected @Nullable RachioHttp rachioHttp;
    protected @Nullable RachioSecurity rachioSecurity;
    protected @Nullable ScheduledFuture<?> pollingJob;
    protected @Nullable ScheduledFuture<?> statusUpdateJob;

    public RachioHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio handler for {}", getThing().getUID());
        updateStatus(ThingStatus.UNKNOWN);
        startPolling();
    }

    @Override
    public void dispose() {
        stopPolling();
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Base implementation does nothing
        // Subclasses should override for specific commands
    }

    protected void startPolling() {
        stopPolling(); // Ensure any existing polling is stopped
        
        pollingJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                pollStatus();
            } catch (Exception e) {
                logger.warn("Polling failed for {}", getThing().getUID(), e);
            }
        }, 0, getPollingInterval(), TimeUnit.SECONDS);
        
        // Also schedule rate limit updates more frequently
        statusUpdateJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                updateRateLimitChannels();
            } catch (Exception e) {
                logger.debug("Failed to update rate limit channels", e);
            }
        }, 0, 30, TimeUnit.SECONDS); // Update every 30 seconds
    }

    protected void stopPolling() {
        ScheduledFuture<?> job = pollingJob;
        if (job != null) {
            job.cancel(true);
            pollingJob = null;
        }
        
        ScheduledFuture<?> statusJob = statusUpdateJob;
        if (statusJob != null) {
            statusJob.cancel(true);
            statusUpdateJob = null;
        }
    }

    protected int getPollingInterval() {
        // Default polling interval (can be overridden by subclasses or configuration)
        return 60; // 60 seconds
    }

    protected abstract void pollStatus() throws RachioApiException;

    // FIXED: Removed @Override annotation - this is not overriding a method
    protected void updateRateLimitChannels() {
        RachioHttp http = rachioHttp;
        if (http != null) {
            try {
                // Get rate limits - returns Map<String, Integer>
                Map<String, Integer> rateLimits = http.getRateLimits();
                
                if (!rateLimits.isEmpty()) {
                    // Get the first entry or use a default key
                    Integer remaining = rateLimits.values().iterator().next();
                    
                    // Convert Integer to String for StringType channels
                    updateState(CHANNEL_RATE_LIMIT_REMAINING, new StringType(remaining.toString()));
                    
                    // Calculate percentage (assuming 1000 is max)
                    int percentage = (remaining * 100) / 1000;
                    updateState(CHANNEL_RATE_LIMIT_PERCENT, new StringType(String.valueOf(percentage)));
                    
                    // Determine status
                    String status;
                    if (remaining > 100) {
                        status = "GOOD";
                    } else if (remaining > 20) {
                        status = "WARNING";
                    } else {
                        status = "CRITICAL";
                    }
                    updateState(CHANNEL_RATE_LIMIT_STATUS, new StringType(status));
                    
                    // Update reset time if available
                    String endpoint = rateLimits.keySet().iterator().next();
                    java.time.Instant resetTime = http.getRateLimitReset(endpoint);
                    if (resetTime != null) {
                        updateState(CHANNEL_RATE_LIMIT_RESET, new StringType(resetTime.toString()));
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to update rate limit channels", e);
            }
        }
    }

    @Override
    public void onStatusChanged(ThingStatus status, ThingStatusDetail detail, @Nullable String message) {
        updateStatus(status, detail, message);
    }

    @Override
    public void onWebhookEvent(String deviceId, String eventType, 
                               @Nullable String subType, 
                               @Nullable Map<String, Object> eventData) {
        // Base implementation - subclasses can override
        logger.debug("Base webhook event: {} for device {}", eventType, deviceId);
    }

    // Getters for subclasses
    protected @Nullable RachioHttp getRachioHttp() {
        return rachioHttp;
    }

    protected @Nullable RachioSecurity getRachioSecurity() {
        return rachioSecurity;
    }
}
