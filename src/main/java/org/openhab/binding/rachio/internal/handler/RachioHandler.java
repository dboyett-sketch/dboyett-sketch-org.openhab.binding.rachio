package org.openhab.binding.rachio.internal.handler;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioWebHookEvent;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
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

/**
 * Abstract base handler for Rachio things (devices and zones)
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public abstract class RachioHandler extends BaseThingHandler {
    protected final Logger logger = LoggerFactory.getLogger(RachioHandler.class);
    
    protected @Nullable RachioBridgeHandler bridgeHandler;
    protected @Nullable RachioHttp http;
    protected @Nullable ScheduledFuture<?> refreshJob;
    
    protected static final int REFRESH_INTERVAL_SECONDS = 60;

    public RachioHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio handler for thing: {}", getThing().getUID());
        
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge not found");
            return;
        }
        
        bridgeHandler = (RachioBridgeHandler) bridge.getHandler();
        if (bridgeHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge handler not available");
            return;
        }
        
        http = bridgeHandler.getRachioHttp();
        if (http == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "HTTP client not available");
            return;
        }
        
        // Register this handler as a status listener
        bridgeHandler.registerStatusListener(new RachioStatusListener() {
            @Override
            public void deviceListUpdated(Collection<org.openhab.binding.rachio.internal.api.dto.RachioDevice> devices) {
                // Default implementation - can be overridden
            }
            
            @Override
            public void deviceUpdated(org.openhab.binding.rachio.internal.api.dto.RachioDevice device) {
                // Default implementation - can be overridden
            }
            
            @Override
            public void webhookEventReceived(RachioWebHookEvent event) {
                handleWebhookEvent(event);
            }
        });
        
        scheduler.submit(() -> {
            try {
                initializeThing();
                startRefreshJob();
                updateStatus(ThingStatus.ONLINE);
            } catch (Exception e) {
                logger.error("Failed to initialize thing: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        });
    }
    
    @Override
    public void dispose() {
        logger.debug("Disposing Rachio handler for thing: {}", getThing().getUID());
        
        stopRefreshJob();
        
        RachioBridgeHandler localBridgeHandler = bridgeHandler;
        if (localBridgeHandler != null) {
            // Unregister status listener if needed
        }
        
        super.dispose();
    }
    
    /**
     * Initialize the specific thing (device or zone)
     */
    protected abstract void initializeThing() throws Exception;
    
    /**
     * Handle a webhook event
     */
    protected abstract void handleWebhookEvent(RachioWebHookEvent event);
    
    /**
     * Refresh all channels
     */
    protected abstract void refreshAllChannels();
    
    /**
     * Start the refresh job
     */
    protected void startRefreshJob() {
        stopRefreshJob(); // Ensure no existing job
        
        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshAllChannels();
            } catch (Exception e) {
                logger.warn("Error refreshing channels: {}", e.getMessage(), e);
            }
        }, 10, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        logger.debug("Started refresh job for thing: {}", getThing().getUID());
    }
    
    /**
     * Stop the refresh job
     */
    protected void stopRefreshJob() {
        ScheduledFuture<?> localRefreshJob = refreshJob;
        if (localRefreshJob != null && !localRefreshJob.isCancelled()) {
            localRefreshJob.cancel(true);
            refreshJob = null;
            logger.debug("Stopped refresh job for thing: {}", getThing().getUID());
        }
    }
    
    /**
     * Get the bridge handler
     */
    protected @Nullable RachioBridgeHandler getBridgeHandler() {
        return bridgeHandler;
    }
    
    /**
     * Get the HTTP client
     */
    protected @Nullable RachioHttp getRachioHttp() {
        return http;
    }
    
    /**
     * Create a channel dynamically
     */
    protected Channel createChannel(String channelId, String itemType, String label, 
                                   @Nullable String description, @Nullable Map<String, String> properties) {
        ChannelUID channelUID = new ChannelUID(getThing().getUID(), channelId);
        ChannelTypeUID channelTypeUID = new ChannelTypeUID("rachio", channelId);
        
        ChannelBuilder channelBuilder = ChannelBuilder.create(channelUID, itemType)
            .withType(channelTypeUID)
            .withLabel(label != null ? label : channelId);
            
        if (description != null) {
            channelBuilder.withDescription(description);
        }
        
        if (properties != null && !properties.isEmpty()) {
            channelBuilder.withProperties(properties);
        }
        
        return channelBuilder.build();
    }
    
    /**
     * Update a channel state with a String value
     */
    protected void updateState(String channelId, String value) {
        updateState(channelId, new StringType(value));
    }
    
    /**
     * Update a channel state with a boolean value
     */
    protected void updateState(String channelId, boolean value) {
        updateState(channelId, value ? OnOffType.ON : OnOffType.OFF);
    }
    
    /**
     * Update a channel state with an int value
     */
    protected void updateState(String channelId, int value) {
        updateState(channelId, new DecimalType(value));
    }
    
    /**
     * Update a channel state with a long value
     */
    protected void updateState(String channelId, long value) {
        updateState(channelId, new DecimalType(value));
    }
    
    /**
     * Update a channel state with a double value
     */
    protected void updateState(String channelId, double value) {
        updateState(channelId, new DecimalType(value));
    }
    
    /**
     * Update a channel state with a float value
     */
    protected void updateState(String channelId, float value) {
        updateState(channelId, new DecimalType(value));
    }
    
    /**
     * Update a channel state with a BigDecimal value
     */
    protected void updateState(String channelId, BigDecimal value) {
        updateState(channelId, new DecimalType(value));
    }
    
    /**
     * Update a channel state with a quantity value (using string unit)
     */
    protected void updateState(String channelId, Number value, String unit) {
        updateState(channelId, new QuantityType<>(value + " " + unit));
    }
    
    /**
     * Update a channel state with a datetime value
     */
    protected void updateState(String channelId, ZonedDateTime dateTime) {
        updateState(channelId, new DateTimeType(dateTime));
    }
    
    /**
     * Get a configuration value as String
     */
    protected @Nullable String getConfigAsString(String key) {
        Object value = getConfig().get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Get a configuration value as Integer
     */
    protected @Nullable Integer getConfigAsInt(String key) {
        Object value = getConfig().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Get a configuration value as Boolean
     */
    protected @Nullable Boolean getConfigAsBoolean(String key) {
        Object value = getConfig().get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
    
    /**
     * Get a configuration value as BigDecimal
     */
    protected @Nullable BigDecimal getConfigAsBigDecimal(String key) {
        Object value = getConfig().get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Update configuration
     */
    protected void updateConfiguration(String key, Object value) {
        Configuration config = editConfiguration();
        config.put(key, value);
        updateConfiguration(config);
    }
}
