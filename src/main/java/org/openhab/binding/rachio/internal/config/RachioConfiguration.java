package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioConfiguration} class contains binding-wide configuration parameters.
 *
 * @author Daniel B. - Major rewrite for OpenHAB 5.x
 */
@NonNullByDefault
public class RachioConfiguration {
    // Binding-wide settings
    public boolean autoDiscoveryEnabled = true;
    public int discoveryTimeout = 30; // seconds
    public boolean backgroundDiscovery = true;

    // Default settings for new things
    public int defaultPollingInterval = 60; // seconds
    public int defaultRefreshInterval = 300; // seconds
    public boolean defaultPollingEnabled = true;

    // Professional features defaults
    public boolean defaultEnableForecast = true;
    public boolean defaultEnableWaterAnalytics = true;
    public boolean defaultEnableSavings = true;
    public boolean defaultEnableAlerts = true;

    // Security defaults
    public boolean defaultValidateWebhooks = true;
    public boolean defaultUseAWSIPs = false;
    public @Nullable String defaultAllowedIPs;

    // Logging defaults
    public boolean enableAPILogging = false;
    public boolean enableWebhookLogging = true;
    public boolean enableRateLimitLogging = true;
    public String logLevel = "INFO";

    // Performance settings
    public int httpConnectionTimeout = 30; // seconds
    public int httpReadTimeout = 30; // seconds
    public int maxHttpConnections = 10;
    public int connectionPoolSize = 5;
    public boolean enableConnectionPooling = true;

    // Advanced settings
    public boolean useCompression = true;
    public boolean followRedirects = true;
    public boolean validateCertificates = true;
    public @Nullable String proxyHost;
    public @Nullable Integer proxyPort;
    public @Nullable String proxyUsername;
    public @Nullable String proxyPassword;

    // UI/UX settings
    public boolean showProfessionalData = true;
    public boolean groupChannelsByType = true;
    public String defaultUnits = "imperial"; // "imperial" or "metric"
    public boolean enableTooltips = true;
    public boolean showAdvancedOptions = false;

    // Automation settings
    public boolean enableRuleTemplates = true;
    public boolean enableSceneSupport = true;
    public boolean enablePresenceDetection = false;

    @Override
    public String toString() {
        return "RachioConfiguration [autoDiscoveryEnabled=" + autoDiscoveryEnabled +
                ", defaultPollingInterval=" + defaultPollingInterval +
                ", defaultEnableForecast=" + defaultEnableForecast +
                ", enableAPILogging=" + enableAPILogging +
                "]";
    }

    /**
     * Check if proxy is configured
     */
    public boolean isProxyConfigured() {
        return proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && proxyPort > 0;
    }

    /**
     * Get proxy settings as a string
     */
    public String getProxyString() {
        if (!isProxyConfigured()) {
            return "";
        }
        String auth = "";
        if (proxyUsername != null && !proxyUsername.isEmpty()) {
            auth = proxyUsername + ":" + (proxyPassword != null ? proxyPassword : "") + "@";
        }
        return "http://" + auth + proxyHost + ":" + proxyPort;
    }
}
