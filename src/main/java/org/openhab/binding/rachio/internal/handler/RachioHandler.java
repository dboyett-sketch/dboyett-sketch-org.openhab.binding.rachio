package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioBindingConstants;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link RachioHandler} is a base handler class for Rachio things.
 * Provides common functionality for device and zone handlers.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public abstract class RachioHandler extends BaseThingHandler implements RachioStatusListener {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected @Nullable RachioHttp rachioHttp;
    protected @Nullable RachioBridgeHandler bridgeHandler;
    protected @Nullable ScheduledFuture<?> refreshJob;
    
    // Rate limiting tracking
    protected int rateLimitRemaining = 100;
    protected int rateLimitLimit = 100;
    protected @Nullable Instant rateLimitReset;

    public RachioHandler(Thing thing) {
        super(thing);
    }

    @Override
    public abstract void initialize();

    @Override
    public abstract void dispose();

    @Override
    public abstract void handleCommand(ChannelUID channelUID, Command command);

    @Override
    public abstract void refresh();

    @Override
    public abstract void onWebhookEvent(String eventType, String deviceId, @Nullable String zoneId, 
                                        @Nullable Map<String, Object> data);

    @Override
    public Thing getThing() {
        return super.getThing();
    }

    // Common helper methods
    protected void logDebug(String message, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, args);
        }
    }

    protected void logInfo(String message, Object... args) {
        logger.info(message, args);
    }

    protected void logWarn(String message, Object... args) {
        logger.warn(message, args);
    }

    protected void logError(String message, Throwable t, Object... args) {
        logger.error(message, args, t);
    }

    protected void logError(String message, Object... args) {
        logger.error(message, args);
    }

    protected void scheduleRefresh(int delaySeconds) {
        scheduler.schedule(this::refresh, delaySeconds, TimeUnit.SECONDS);
    }

    protected void cancelRefreshJob() {
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
    }

    protected void createChannelIfMissing(String channelId, String itemType, String category) {
        if (thing.getChannel(channelId) == null) {
            ChannelUID channelUID = new ChannelUID(thing.getUID(), channelId);
            ChannelTypeUID channelTypeUID = new ChannelTypeUID(RachioBindingConstants.BINDING_ID, 
                channelId.toLowerCase());
            
            Channel channel = ChannelBuilder.create(channelUID, itemType)
                .withType(channelTypeUID)
                .withLabel(channelId)
                .withDescription("Dynamic channel for " + channelId)
                .build();
            
            updateThing(editThing().withChannel(channel).build());
            logger.debug("Created dynamic channel: {}", channelId);
        }
    }

    protected void updateRateLimitChannels() {
        if (rachioHttp == null) {
            return;
        }
        
        try {
            Map<String, String> rateLimits = rachioHttp.getRateLimits();
            if (rateLimits != null) {
                String remainingStr = rateLimits.get(RachioBindingConstants.HEADER_RATE_LIMIT_REMAINING);
                String limitStr = rateLimits.get(RachioBindingConstants.HEADER_RATE_LIMIT_LIMIT);
                String resetStr = rateLimits.get(RachioBindingConstants.HEADER_RATE_LIMIT_RESET);
                
                if (remainingStr != null) {
                    rateLimitRemaining = Integer.parseInt(remainingStr);
                    updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_REMAINING, 
                        new DecimalType(rateLimitRemaining));
                }
                
                if (limitStr != null) {
                    rateLimitLimit = Integer.parseInt(limitStr);
                    if (rateLimitLimit > 0) {
                        double percentUsed = ((double) (rateLimitLimit - rateLimitRemaining) / rateLimitLimit) * 100;
                        updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_PERCENT, 
                            new DecimalType(percentUsed));
                    }
                }
                
                if (resetStr != null) {
                    long resetTime = Long.parseLong(resetStr);
                    rateLimitReset = Instant.ofEpochSecond(resetTime);
                    ZonedDateTime resetDateTime = ZonedDateTime.ofInstant(rateLimitReset, ZoneId.systemDefault());
                    updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_RESET, 
                        new DateTimeType(resetDateTime));
                }
                
                // Update status based on rate limits
                String status = getRateLimitStatus();
                updateState(RachioBindingConstants.CHANNEL_RATE_LIMIT_STATUS, new StringType(status));
            }
        } catch (NumberFormatException e) {
            logger.warn("Error parsing rate limit values: {}", e.getMessage());
        }
    }

    protected String getRateLimitStatus() {
        if (rateLimitRemaining <= RachioBindingConstants.RATE_LIMIT_CRITICAL_THRESHOLD) {
            return "CRITICAL";
        } else if (rateLimitRemaining <= RachioBindingConstants.RATE_LIMIT_WARNING_THRESHOLD) {
            return "WARNING";
        } else {
            return "OK";
        }
    }

    protected @Nullable RachioBridgeHandler getBridgeHandler() {
        if (getBridge() == null) {
            return null;
        }
        return (RachioBridgeHandler) getBridge().getHandler();
    }

    protected void handleRefreshCommand(ChannelUID channelUID) {
        logger.debug("Refresh command for channel: {}", channelUID);
        refresh();
    }

    protected void updatePropertyIfChanged(String propertyName, @Nullable String newValue) {
        String currentValue = thing.getProperties().get(propertyName);
        if (newValue != null && !newValue.equals(currentValue)) {
            updateProperty(propertyName, newValue);
        }
    }
}
