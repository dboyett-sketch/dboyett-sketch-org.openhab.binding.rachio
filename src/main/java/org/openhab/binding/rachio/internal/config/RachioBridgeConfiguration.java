package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Configuration for Rachio bridge
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBridgeConfiguration {

    // API configuration
    @Nullable
    public String apiKey;

    // Webhook settings (CRITICAL ADDITION)
    @Nullable
    public String callbackUrl;

    @Nullable
    public String webhookSecret;

    @Nullable
    public Boolean enableWebhooks;

    // Polling settings (CRITICAL ADDITION - aligns with bridge handler)
    @Nullable
    public Integer pollingInterval;

    // Existing connection settings (renamed for clarity)
    @Nullable
    public Integer refreshInterval;

    @Nullable
    public Integer timeout;

    @Nullable
    public String deviceId;

    // Advanced settings
    @Nullable
    public Boolean enableDiscovery;

    @Nullable
    public Boolean enableImages;

    @Nullable
    public String logLevel;

    public RachioBridgeConfiguration() {
    }

    // === CRITICAL ADDITIONS FOR BRIDGE HANDLER COMPATIBILITY ===

    @Nullable
    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(@Nullable String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    /**
     * Get polling interval in seconds (default: 120)
     * Aligns with RachioBridgeHandler default
     */
    public int getPollingInterval() {
        return pollingInterval != null ? pollingInterval : 120;
    }

    public void setPollingInterval(@Nullable Integer pollingInterval) {
        this.pollingInterval = pollingInterval;
    }
    // ============================================================

    @Nullable
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(@Nullable String apiKey) {
        this.apiKey = apiKey;
    }

    @Nullable
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(@Nullable String deviceId) {
        this.deviceId = deviceId;
    }

    @Nullable
    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(@Nullable String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    /**
     * Get refresh interval in seconds (default: 300)
     * Note: Different from pollingInterval - used for different purposes
     */
    public int getRefreshInterval() {
        return refreshInterval != null ? refreshInterval : 300;
    }

    public void setRefreshInterval(@Nullable Integer refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public int getTimeout() {
        return timeout != null ? timeout : 30;
    }

    public void setTimeout(@Nullable Integer timeout) {
        this.timeout = timeout;
    }

    public boolean isEnableWebhooks() {
        return enableWebhooks != null ? enableWebhooks : true;
    }

    public void setEnableWebhooks(@Nullable Boolean enableWebhooks) {
        this.enableWebhooks = enableWebhooks;
    }

    public boolean isEnableDiscovery() {
        return enableDiscovery != null ? enableDiscovery : true;
    }

    public void setEnableDiscovery(@Nullable Boolean enableDiscovery) {
        this.enableDiscovery = enableDiscovery;
    }

    public boolean isEnableImages() {
        return enableImages != null ? enableImages : false;
    }

    public void setEnableImages(@Nullable Boolean enableImages) {
        this.enableImages = enableImages;
    }

    @Nullable
    public String getLogLevel() {
        return logLevel != null ? logLevel : "INFO";
    }

    public void setLogLevel(@Nullable String logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isValid() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Nullable
    public String getValidationError() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "API key is required";
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RachioBridgeConfiguration[");
        sb.append("apiKey=").append(apiKey != null ? "[SET]" : "[NOT SET]");
        sb.append(", deviceId=").append(deviceId != null ? deviceId : "null");
        sb.append(", callbackUrl=").append(callbackUrl != null ? "[SET]" : "null");
        sb.append(", pollingInterval=").append(getPollingInterval());
        sb.append(", refreshInterval=").append(getRefreshInterval());
        sb.append(", timeout=").append(getTimeout());
        sb.append(", enableWebhooks=").append(isEnableWebhooks());
        sb.append(", enableDiscovery=").append(isEnableDiscovery());
        sb.append(", enableImages=").append(isEnableImages());
        sb.append(", logLevel=").append(getLogLevel());
        sb.append("]");
        return sb.toString();
    }
}
