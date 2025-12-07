package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioBridgeConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Michael Lobstein - Initial contribution
 * @author Your Name - Enhanced for OpenHAB 5.x
 */
@NonNullByDefault
public class RachioBridgeConfiguration {

    // Required configuration
    public String apiKey = "";
    public String personId = "";

    // Webhook configuration
    public boolean webhookEnabled = false;
    public String webhookExternalUrl = "";
    public int webhookPort = 8080;
    public String webhookPath = "/rachio/webhook";
    public String webhookSecret = "";

    // Advanced configuration
    public int refreshInterval = 60; // seconds
    public int timeout = 30; // seconds
    public boolean useHttps = true;
    public boolean validateCertificates = true;

    // Rate limiting configuration
    public boolean adaptivePolling = true;
    public int lowRateThreshold = 50; // requests
    public int criticalRateThreshold = 10; // requests

    // Security configuration
    public boolean ipFilteringEnabled = false;
    public String allowedIpRanges = "";
    public boolean awsIpVerification = false;

    // Professional features
    public boolean enableForecast = true;
    public boolean enableUsageAnalytics = true;
    public boolean enableSavingsData = true;
    public boolean enableAlertSystem = true;

    // Debug configuration
    public boolean debugLogging = false;
    public boolean logApiResponses = false;
    public boolean logWebhookEvents = false;

    /**
     * Validate the configuration
     *
     * @return true if configuration is valid
     */
    public boolean isValid() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }

        // Validate webhook configuration if enabled
        if (webhookEnabled) {
            if (webhookExternalUrl == null || webhookExternalUrl.trim().isEmpty()) {
                return false;
            }
            if (webhookPort < 1 || webhookPort > 65535) {
                return false;
            }
        }

        // Validate refresh interval
        if (refreshInterval < 10 || refreshInterval > 3600) {
            return false;
        }

        // Validate timeout
        if (timeout < 5 || timeout > 120) {
            return false;
        }

        return true;
    }

    /**
     * Get the full webhook URL
     *
     * @return the complete webhook URL
     */
    public @Nullable String getWebhookUrl() {
        if (!webhookEnabled || webhookExternalUrl.isEmpty()) {
            return null;
        }

        String url = webhookExternalUrl;
        if (!url.endsWith("/")) {
            url += "/";
        }
        
        // Remove leading slash from path if present
        String path = webhookPath;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        url += path;
        
        // Ensure proper protocol
        if (useHttps && !url.startsWith("https://")) {
            if (url.startsWith("http://")) {
                url = "https://" + url.substring(7);
            } else {
                url = "https://" + url;
            }
        } else if (!useHttps && !url.startsWith("http://")) {
            url = "http://" + url;
        }
        
        return url;
    }

    /**
     * Get the base API URL
     *
     * @return the Rachio API URL
     */
    public String getApiUrl() {
        return useHttps ? "https://api.rach.io" : "http://api.rach.io";
    }

    /**
     * Check if professional features are enabled
     *
     * @return true if any professional feature is enabled
     */
    public boolean hasProfessionalFeatures() {
        return enableForecast || enableUsageAnalytics || enableSavingsData || enableAlertSystem;
    }

    /**
     * Get configuration summary for logging
     *
     * @return configuration summary string
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("API Key: ").append(apiKey != null && !apiKey.isEmpty() ? "Configured" : "Missing").append("\n");
        summary.append("Person ID: ").append(personId != null && !personId.isEmpty() ? personId : "Not set").append("\n");
        summary.append("Webhook: ").append(webhookEnabled ? "Enabled" : "Disabled").append("\n");
        
        if (webhookEnabled) {
            summary.append("Webhook URL: ").append(getWebhookUrl()).append("\n");
            summary.append("Webhook Port: ").append(webhookPort).append("\n");
        }
        
        summary.append("Refresh Interval: ").append(refreshInterval).append("s\n");
        summary.append("Timeout: ").append(timeout).append("s\n");
        summary.append("Adaptive Polling: ").append(adaptivePolling ? "Enabled" : "Disabled").append("\n");
        summary.append("Professional Features: ").append(hasProfessionalFeatures() ? "Enabled" : "Disabled").append("\n");
        
        return summary.toString();
    }

    /**
     * Sanitize configuration values
     */
    public void sanitize() {
        // Trim string values
        if (apiKey != null) {
            apiKey = apiKey.trim();
        }
        if (personId != null) {
            personId = personId.trim();
        }
        if (webhookExternalUrl != null) {
            webhookExternalUrl = webhookExternalUrl.trim();
        }
        if (webhookPath != null) {
            webhookPath = webhookPath.trim();
            // Ensure path starts with slash
            if (!webhookPath.startsWith("/")) {
                webhookPath = "/" + webhookPath;
            }
        }
        if (webhookSecret != null) {
            webhookSecret = webhookSecret.trim();
        }
        if (allowedIpRanges != null) {
            allowedIpRanges = allowedIpRanges.trim();
        }

        // Validate numeric ranges
        refreshInterval = Math.max(10, Math.min(refreshInterval, 3600));
        timeout = Math.max(5, Math.min(timeout, 120));
        webhookPort = Math.max(1, Math.min(webhookPort, 65535));
        lowRateThreshold = Math.max(1, Math.min(lowRateThreshold, 300));
        criticalRateThreshold = Math.max(1, Math.min(criticalRateThreshold, lowRateThreshold - 1));
    }
}
