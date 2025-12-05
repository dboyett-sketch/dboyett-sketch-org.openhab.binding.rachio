package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Configuration class for Rachio Binding
 *
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioConfiguration {

    // API Configuration
    public @Nullable String apiKey;
    
    // Webhook Configuration
    public @Nullable String webhookUrl;
    public boolean webhookEnabled = true;
    public @Nullable String webhookExternalId;
    
    // Polling Configuration
    public int pollInterval = 300; // 5 minutes in seconds
    public int discoveryInterval = 3600; // 1 hour in seconds
    public int refreshInterval = 60; // 1 minute in seconds
    
    // Rate Limiting Configuration
    public int rateLimitThreshold = 10; // Slow down when remaining calls < 10
    public int rateLimitSlowPollInterval = 600; // 10 minutes when rate limited
    public int rateLimitNormalPollInterval = 300; // 5 minutes normal
    
    // Advanced Configuration
    public boolean enableDebugLogging = false;
    public boolean enableProfessionalData = true;
    public boolean autoCreateThings = true;
    public boolean enableRateLimitMonitoring = true;
    
    // Network Configuration
    public int connectionTimeout = 30; // seconds
    public int readTimeout = 30; // seconds
    public int maxRetries = 3;
    public int retryDelay = 5; // seconds
    
    // Security Configuration
    public boolean validateWebhookSignature = true;
    public @Nullable String allowedIPs; // Comma-separated list of IPs/CIDR ranges
    public boolean useAWSIPRanges = false;
    
    // Device Defaults
    public int defaultZoneRuntime = 300; // 5 minutes in seconds
    public int defaultRainDelayHours = 24;
    public int maxZoneRuntime = 10800; // 3 hours in seconds
    
    // Validation Methods
    
    /**
     * Validate the configuration
     * @return true if configuration is valid
     */
    public boolean isValid() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
    
    /**
     * Get API key with null safety
     */
    public String getApiKey() {
        String key = apiKey;
        return key != null ? key.trim() : "";
    }
    
    /**
     * Get webhook URL with null safety
     */
    public String getWebhookUrl() {
        String url = webhookUrl;
        return url != null ? url.trim() : "";
    }
    
    /**
     * Get webhook external ID with null safety
     */
    public String getWebhookExternalId() {
        String id = webhookExternalId;
        if (id != null && !id.trim().isEmpty()) {
            return id.trim();
        }
        // Generate default external ID
        return "openhab-rachio-" + System.currentTimeMillis();
    }
    
    /**
     * Get allowed IPs as array
     */
    public String[] getAllowedIPs() {
        String ips = allowedIPs;
        if (ips == null || ips.trim().isEmpty()) {
            return new String[0];
        }
        return ips.trim().split("\\s*,\\s*");
    }
    
    /**
     * Check if webhook is configured and enabled
     */
    public boolean isWebhookConfigured() {
        return webhookEnabled && getWebhookUrl() != null && !getWebhookUrl().isEmpty();
    }
    
    /**
     * Get poll interval adjusted for rate limiting
     */
    public int getAdjustedPollInterval(boolean rateLimited) {
        return rateLimited ? rateLimitSlowPollInterval : pollInterval;
    }
    
    /**
     * Validate poll interval is within bounds
     */
    public void validatePollInterval() {
        if (pollInterval < 30) {
            pollInterval = 30; // Minimum 30 seconds
        }
        if (pollInterval > 3600) {
            pollInterval = 3600; // Maximum 1 hour
        }
        
        if (rateLimitSlowPollInterval < 60) {
            rateLimitSlowPollInterval = 60; // Minimum 1 minute when rate limited
        }
        if (rateLimitSlowPollInterval > 7200) {
            rateLimitSlowPollInterval = 7200; // Maximum 2 hours when rate limited
        }
        
        if (rateLimitNormalPollInterval < 30) {
            rateLimitNormalPollInterval = 30; // Minimum 30 seconds
        }
        if (rateLimitNormalPollInterval > 3600) {
            rateLimitNormalPollInterval = 3600; // Maximum 1 hour
        }
    }
    
    /**
     * Validate timeouts
     */
    public void validateTimeouts() {
        if (connectionTimeout < 5) {
            connectionTimeout = 5; // Minimum 5 seconds
        }
        if (connectionTimeout > 120) {
            connectionTimeout = 120; // Maximum 2 minutes
        }
        
        if (readTimeout < 5) {
            readTimeout = 5; // Minimum 5 seconds
        }
        if (readTimeout > 120) {
            readTimeout = 120; // Maximum 2 minutes
        }
        
        if (maxRetries < 0) {
            maxRetries = 0; // Minimum 0 retries
        }
        if (maxRetries > 10) {
            maxRetries = 10; // Maximum 10 retries
        }
        
        if (retryDelay < 1) {
            retryDelay = 1; // Minimum 1 second
        }
        if (retryDelay > 60) {
            retryDelay = 60; // Maximum 60 seconds
        }
    }
    
    /**
     * Validate runtime values
     */
    public void validateRuntimes() {
        if (defaultZoneRuntime < 1) {
            defaultZoneRuntime = 1; // Minimum 1 second
        }
        if (defaultZoneRuntime > maxZoneRuntime) {
            defaultZoneRuntime = maxZoneRuntime;
        }
        
        if (maxZoneRuntime < 60) {
            maxZoneRuntime = 60; // Minimum 1 minute
        }
        if (maxZoneRuntime > 28800) {
            maxZoneRuntime = 28800; // Maximum 8 hours
        }
        
        if (defaultRainDelayHours < 0) {
            defaultRainDelayHours = 0; // Minimum 0 hours
        }
        if (defaultRainDelayHours > 168) {
            defaultRainDelayHours = 168; // Maximum 7 days
        }
    }
    
    /**
     * Get configuration as string for logging (without sensitive data)
     */
    @Override
    public String toString() {
        return "RachioConfiguration{" +
                "webhookEnabled=" + webhookEnabled +
                ", pollInterval=" + pollInterval +
                ", discoveryInterval=" + discoveryInterval +
                ", refreshInterval=" + refreshInterval +
                ", rateLimitThreshold=" + rateLimitThreshold +
                ", enableDebugLogging=" + enableDebugLogging +
                ", enableProfessionalData=" + enableProfessionalData +
                ", autoCreateThings=" + autoCreateThings +
                ", enableRateLimitMonitoring=" + enableRateLimitMonitoring +
                ", connectionTimeout=" + connectionTimeout +
                ", readTimeout=" + readTimeout +
                ", maxRetries=" + maxRetries +
                ", retryDelay=" + retryDelay +
                ", validateWebhookSignature=" + validateWebhookSignature +
                ", useAWSIPRanges=" + useAWSIPRanges +
                ", defaultZoneRuntime=" + defaultZoneRuntime +
                ", defaultRainDelayHours=" + defaultRainDelayHours +
                ", maxZoneRuntime=" + maxZoneRuntime +
                '}';
    }
    
    /**
     * Get configuration summary for status display
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("API Key: ").append(apiKey != null && !apiKey.isEmpty() ? "Configured" : "Not Configured");
        sb.append(", Webhook: ").append(isWebhookConfigured() ? "Enabled" : "Disabled");
        sb.append(", Polling: ").append(pollInterval).append("s");
        sb.append(", Professional Data: ").append(enableProfessionalData ? "Enabled" : "Disabled");
        return sb.toString();
    }
    
    /**
     * Check if professional data should be exposed
     */
    public boolean shouldExposeProfessionalData() {
        return enableProfessionalData;
    }
    
    /**
     * Check if rate limit monitoring is enabled
     */
    public boolean isRateLimitMonitoringEnabled() {
        return enableRateLimitMonitoring;
    }
    
    /**
     * Check if auto-creation of things is enabled
     */
    public boolean isAutoCreateThingsEnabled() {
        return autoCreateThings;
    }
    
    /**
     * Check if debug logging is enabled
     */
    public boolean isDebugLoggingEnabled() {
        return enableDebugLogging;
    }
    
    /**
     * Check if webhook signature validation is enabled
     */
    public boolean isWebhookSignatureValidationEnabled() {
        return validateWebhookSignature;
    }
    
    /**
     * Check if AWS IP ranges should be used
     */
    public boolean shouldUseAWSIPRanges() {
        return useAWSIPRanges;
    }
}
