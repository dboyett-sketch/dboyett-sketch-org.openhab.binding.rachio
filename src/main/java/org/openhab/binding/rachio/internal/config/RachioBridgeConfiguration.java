package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioBridgeConfiguration} class contains fields mapping thing configuration parameters.
 * @author Damian S - Initial contribution
 */
@NonNullByDefault
public class RachioBridgeConfiguration {

    // API Authentication (Primary)
    public String apiKey = "";

    // Webhook Configuration (Essential for real-time updates)
    public @Nullable String webhookUrl;
    public @Nullable String webhookSecret;

    // Polling & Refresh Intervals
    public int refreshInterval = 300; // Default: 5 minutes (in seconds)
    public int pollInterval = 300;    // Alias for refreshInterval for compatibility

    // Security & IP Filtering (Optional, but recommended)
    public boolean verifyWebhookIp = true;
    public @Nullable String webhookAllowedIp;
    public boolean loadAwsIpRanges = true;
    public int webhookTimeout = 30; // Timeout for webhook validation in seconds

    // Legacy/Deprecated fields (Kept for configuration migration)
    public @Nullable String accessToken;
    public @Nullable String webhookExternalUrl;

    // ==================== Getter Methods ====================

    /**
     * Gets the primary API key for Rachio cloud authentication.
     * This is a REQUIRED configuration parameter.
     */
    public @Nullable String getApiKey() {
        return (apiKey != null && !apiKey.isBlank()) ? apiKey : null;
    }

    /**
     * Gets the publicly accessible URL where Rachio should send webhook events.
     * Format: https://your-openhab-domain:port/rachio/webhook
     */
    public @Nullable String getWebhookUrl() {
        return webhookUrl;
    }

    /**
     * Gets the secret used to validate HMAC signatures of incoming webhooks.
     * This MUST match the secret configured in your Rachio account.
     */
    public @Nullable String getWebhookSecret() {
        return webhookSecret;
    }

    /**
     * Gets the interval (in seconds) for polling device status.
     */
    public int getRefreshInterval() {
        return refreshInterval;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public boolean isVerifyWebhookIp() {
        return verifyWebhookIp;
    }

    public @Nullable String getWebhookAllowedIp() {
        return webhookAllowedIp;
    }

    public boolean isLoadAwsIpRanges() {
        return loadAwsIpRanges;
    }

    public int getWebhookTimeout() {
        return webhookTimeout;
    }

    // ==================== Validation Methods ====================

    /**
     * Checks if the configuration has the minimum required settings for operation.
     */
    public boolean isValid() {
        return getApiKey() != null;
    }

    /**
     * Checks if webhook is fully configured (both URL and secret are provided).
     */
    public boolean isWebhookFullyConfigured() {
        return getWebhookUrl() != null && !getWebhookUrl().isBlank() &&
               getWebhookSecret() != null && !getWebhookSecret().isBlank();
    }

    /**
     * Gets a description of configuration issues for user feedback.
     */
    public @Nullable String getValidationError() {
        if (getApiKey() == null) {
            return "API key is required";
        }
        if (getWebhookUrl() != null && getWebhookSecret() == null) {
            return "Webhook URL is configured but secret is missing";
        }
        if (getWebhookSecret() != null && getWebhookUrl() == null) {
            return "Webhook secret is configured but URL is missing";
        }
        return null;
    }
}
