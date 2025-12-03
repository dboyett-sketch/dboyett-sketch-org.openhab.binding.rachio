package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

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