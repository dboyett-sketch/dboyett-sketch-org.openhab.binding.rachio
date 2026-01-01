package org.openhab.binding.rachio.internal.handler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for Rachio thing handlers
 * 
 * @author Damion Boyett refactor contribution
 */
@NonNullByDefault
public abstract class RachioHandler extends BaseThingHandler implements RachioStatusListener {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected @Nullable ScheduledFuture<?> refreshJob;

    protected RachioHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            handleRefreshCommand(channelUID, () -> performRefresh(channelUID));
        }
    }

    /**
     * Handle refresh command with custom action
     * 
     * @param channelUID channel UID
     * @param refreshAction action to perform on refresh
     */
    protected void handleRefreshCommand(ChannelUID channelUID, Runnable refreshAction) {
        if (scheduler.isShutdown()) {
            logger.debug("Scheduler shutdown, skipping refresh for {}", channelUID);
            return;
        }

        scheduler.submit(() -> {
            try {
                refreshAction.run();
            } catch (Exception e) {
                logger.debug("Error executing refresh for {}: {}", channelUID, e.getMessage(), e);
            }
        });
    }

    /**
     * Perform refresh for a specific channel
     * 
     * @param channelUID the channel to refresh
     */
    protected abstract void performRefresh(ChannelUID channelUID);

    /**
     * Schedule a refresh with delay
     * 
     * @param delay delay in seconds
     */
    protected void scheduleRefresh(int delay) {
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
        }
        refreshJob = scheduler.schedule(this::refresh, delay, TimeUnit.SECONDS);
    }

    protected void refresh() {
        logger.trace("Base refresh() called - child classes should implement performRefresh()");
    }

    /**
     * Stop any scheduled refresh jobs
     */
    protected void stopRefresh() {
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
    }

    /**
     * Update thing status with detailed information
     * 
     * @param status thing status
     * @param detail status detail
     * @param message status message
     */
    protected void updateStatus(ThingStatus status, ThingStatusDetail detail, @Nullable String message) {
        if (message != null) {
            super.updateStatus(status, detail, message);
        } else {
            super.updateStatus(status, detail);
        }
    }

    /**
     * Convert string to StringType, handling null values
     * 
     * @param value string value
     * @return StringType or null
     */
    protected @Nullable StringType toSafeStringType(@Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            return new StringType(value);
        } catch (Exception e) {
            logger.debug("Error creating StringType from value: {}", value, e);
            return null;
        }
    }

    // ===== RachioStatusListener Interface Implementation =====

    @Override
    public abstract String getListenerId();

    @Override
    public abstract RachioStatusListener.ListenerType getListenerType();

    @Override
    public void onNewDevice(@Nullable String deviceId) {
        logger.debug("Base handler received new device: {}", deviceId);
    }

    @Override
    public void onNewZone(@Nullable String deviceId, @Nullable String zoneId) {
        logger.debug("Base handler received new zone: device={}, zone={}", deviceId, zoneId);
    }

    @Override
    public void onDeviceStatusChanged(@Nullable RachioDevice device) {
        logger.debug("Base handler received device status changed: {}", device != null ? device.id : "null");
    }

    @Override
    public void onDeviceStatusChanged(@Nullable String deviceId, @Nullable String status) {
        logger.debug("Base handler received device status changed: id={}, status={}", deviceId, status);
    }

    @Override
    public void onWebhookEventReceived(@Nullable String eventJson) {
        logger.debug("Base handler received webhook event (length: {})", eventJson != null ? eventJson.length() : 0);
    }

    @Override
    public void onDeviceUpdated(@Nullable RachioDevice device) {
        logger.debug("Base handler received device updated: {}", device != null ? device.id : "null");
    }

    @Override
    public void onZoneUpdated(@Nullable String zoneId, @Nullable RachioZone zone) {
        logger.debug("Base handler received zone updated: zoneId={}", zoneId);
    }

    @Override
    public void onRateLimitStatusChanged(int remaining, int limit, @Nullable String status) {
        logger.debug("Base handler received rate limit status: {}/{}, status={}", remaining, limit, status);
    }

    @Override
    public void onZoneStatusChanged(@Nullable String deviceId, @Nullable String zoneId, @Nullable String status) {
        logger.debug("Base handler received zone status changed: device={}, zone={}, status={}", deviceId, zoneId,
                status);
    }
}
