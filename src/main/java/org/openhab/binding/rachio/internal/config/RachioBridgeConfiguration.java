package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioBridgeConfiguration} class contains fields mapping bridge configuration parameters.
 *
 * @author Brian G. - Initial contribution (from 2.5 binding)
 * @author Daniel B. - Major rewrite for OpenHAB 5.x
 */
@NonNullByDefault
public class RachioBridgeConfiguration {
    // Required: API key for Rachio Cloud
    public String apiKey = "";

    // Optional: Webhook configuration
    public @Nullable String webhookUrl;
    public @Nullable String webhookSecret;
    public @Nullable String webhookId;

    // Optional: IP security
    public @Nullable String allowedIPs;
    public boolean useAWSIPs = false;

    // Optional: Polling configuration
    public int refreshInterval = 60; // seconds
    public boolean pollingEnabled = true;

    // Optional: Rate limiting
    public int rateLimitWarningThreshold = 100;
    public int rateLimitCriticalThreshold = 10;
    public boolean adaptivePolling = true;

    // Optional: Professional features
    public boolean enableForecast = true;
    public boolean enableWaterAnalytics = true;
    public boolean enableSavingsData = true;
    public boolean enableAlerts = true;
    public boolean enableScheduleMonitoring = true;

    // Optional: Logging
    public boolean debugLogging = false;
    public boolean webhookLogging = true;
    public boolean rateLimitLogging = true;

    // Optional: Advanced settings
    public int httpTimeout = 30; // seconds
    public int maxRetries = 3;
    public int retryDelay = 5; // seconds
    public boolean validateSSL = true;

    @Override
    public String toString() {
        return "RachioBridgeConfiguration [apiKey=" + (apiKey.isEmpty() ? "not set" : "set") +
                ", webhookUrl=" + webhookUrl +
                ", refreshInterval=" + refreshInterval +
                ", pollingEnabled=" + pollingEnabled +
                ", enableForecast=" + enableForecast +
                ", enableWaterAnalytics=" + enableWaterAnalytics +
                "]";
    }
}

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * Legacy bridge configuration - consider using RachioConfiguration instead
 * @deprecated Use {@link RachioConfiguration} for full configuration support
 */
@NonNullByDefault
@Deprecated
public class RachioBridgeConfiguration {
    public String apiKey = "";
    public String webhookId = "";
    public int refreshInterval = DEFAULT_POLLING_INTERVAL;
    public String webhookUrl = "";

    /**
     * Validate configuration
     */
    public boolean isValid() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Get configuration summary for logging
     */
    public String getConfigSummary() {
        return String.format("RachioBridgeConfiguration{apiKey='%s', webhookId='%s', refreshInterval=%d, webhookUrl='%s'}", 
                           apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) + "..." : "null",
                           webhookId, refreshInterval, webhookUrl);
    }

    @Override
    public String toString() {
        return getConfigSummary();
    }

}
