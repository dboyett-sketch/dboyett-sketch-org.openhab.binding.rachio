package org.openhab.binding.rachio.internal.config;

import org.openhab.core.config.core.Configuration;

/**
 * General configuration class for Rachio Binding
 * Contains system-wide settings and defaults
 * 
 * @author dboyett-sketch
 */
public class RachioConfiguration {

    // System Configuration
    public Integer threadPoolSize = 5;
    public Integer maxConcurrentRequests = 10;
    public Integer requestQueueSize = 100;

    // API Configuration
    public String apiBaseUrl = "https://api.rach.io/1/public";
    public Integer apiTimeout = 10000; // milliseconds
    public Integer apiRetryCount = 3;
    public Integer apiRetryDelay = 1000; // milliseconds

    // Cache Configuration
    public Boolean enableCaching = true;
    public Integer defaultCacheTTL = 300; // seconds
    public Integer maxCacheSize = 1000;

    // Discovery Configuration
    public Boolean enableAutoDiscovery = true;
    public Integer discoveryDelay = 10; // seconds
    public Integer discoveryTimeout = 30; // seconds

    // Webhook Configuration
    public String webhookBasePath = "/rachio/webhook";
    public Boolean webhookEnabled = true;
    public Integer webhookPort = 8080;
    public Boolean webhookSSL = false;

    // Image Configuration
    public Boolean enableImageProxy = true;
    public Integer imageCacheSize = 50; // MB
    public Integer imageTimeout = 30000; // milliseconds

    // Logging Configuration
    public String logLevel = "INFO";
    public Boolean logApiRequests = false;
    public Boolean logApiResponses = false;
    public Integer logMaxSize = 10; // MB

    // Performance Configuration
    public Boolean compressResponses = true;
    public Integer connectionPoolSize = 20;
    public Integer keepAliveTimeout = 30000; // milliseconds

    // Security Configuration
    public Boolean validateCertificates = true;
    public Integer sslHandshakeTimeout = 10000; // milliseconds
    public String[] allowedCiphers = null; // Null means use defaults

    /**
     * Default constructor
     */
    public RachioConfiguration() {
        // Default values already set above
    }

    /**
     * Validate configuration
     */
    public boolean isValid() {
        // Validate thread pool size
        if (threadPoolSize != null && (threadPoolSize < 1 || threadPoolSize > 50)) {
            return false;
        }

        // Validate concurrent requests
        if (maxConcurrentRequests != null && (maxConcurrentRequests < 1 || maxConcurrentRequests > 100)) {
            return false;
        }

        // Validate queue size
        if (requestQueueSize != null && (requestQueueSize < 10 || requestQueueSize > 1000)) {
            return false;
        }

        // Validate API timeout
        if (apiTimeout != null && (apiTimeout < 1000 || apiTimeout > 60000)) {
            return false;
        }

        // Validate retry count
        if (apiRetryCount != null && (apiRetryCount < 0 || apiRetryCount > 10)) {
            return false;
        }

        // Validate retry delay
        if (apiRetryDelay != null && (apiRetryDelay < 100 || apiRetryDelay > 10000)) {
            return false;
        }

        // Validate cache TTL
        if (defaultCacheTTL != null && (defaultCacheTTL < 10 || defaultCacheTTL > 86400)) {
            return false;
        }

        // Validate cache size
        if (maxCacheSize != null && (maxCacheSize < 10 || maxCacheSize > 10000)) {
            return false;
        }

        // Validate discovery settings
        if (discoveryDelay != null && (discoveryDelay < 0 || discoveryDelay > 300)) {
            return false;
        }

        if (discoveryTimeout != null && (discoveryTimeout < 10 || discoveryTimeout > 300)) {
            return false;
        }

        // Validate webhook port
        if (webhookPort != null && (webhookPort < 1 || webhookPort > 65535)) {
            return false;
        }

        // Validate image settings
        if (imageCacheSize != null && (imageCacheSize < 1 || imageCacheSize > 1000)) {
            return false;
        }

        if (imageTimeout != null && (imageTimeout < 1000 || imageTimeout > 120000)) {
            return false;
        }

        // Validate logging
        if (logMaxSize != null && (logMaxSize < 1 || logMaxSize > 100)) {
            return false;
        }

        // Validate performance settings
        if (connectionPoolSize != null && (connectionPoolSize < 1 || connectionPoolSize > 100)) {
            return false;
        }

        if (keepAliveTimeout != null && (keepAliveTimeout < 1000 || keepAliveTimeout > 300000)) {
            return false;
        }

        // Validate security settings
        if (sslHandshakeTimeout != null && (sslHandshakeTimeout < 1000 || sslHandshakeTimeout > 30000)) {
            return false;
        }

        return true;
    }

    /**
     * Get validation errors
     */
    public String getValidationErrors() {
        StringBuilder errors = new StringBuilder();

        if (threadPoolSize != null && (threadPoolSize < 1 || threadPoolSize > 50)) {
            errors.append("Thread pool size must be between 1 and 50. ");
        }

        if (maxConcurrentRequests != null && (maxConcurrentRequests < 1 || maxConcurrentRequests > 100)) {
            errors.append("Max concurrent requests must be between 1 and 100. ");
        }

        if (requestQueueSize != null && (requestQueueSize < 10 || requestQueueSize > 1000)) {
            errors.append("Request queue size must be between 10 and 1000. ");
        }

        if (apiTimeout != null && (apiTimeout < 1000 || apiTimeout > 60000)) {
            errors.append("API timeout must be between 1000 and 60000 milliseconds. ");
        }

        if (apiRetryCount != null && (apiRetryCount < 0 || apiRetryCount > 10)) {
            errors.append("API retry count must be between 0 and 10. ");
        }

        if (apiRetryDelay != null && (apiRetryDelay < 100 || apiRetryDelay > 10000)) {
            errors.append("API retry delay must be between 100 and 10000 milliseconds. ");
        }

        if (defaultCacheTTL != null && (defaultCacheTTL < 10 || defaultCacheTTL > 86400)) {
            errors.append("Default cache TTL must be between 10 and 86400 seconds. ");
        }

        if (maxCacheSize != null && (maxCacheSize < 10 || maxCacheSize > 10000)) {
            errors.append("Max cache size must be between 10 and 10000 entries. ");
        }

        if (discoveryDelay != null && (discoveryDelay < 0 || discoveryDelay > 300)) {
            errors.append("Discovery delay must be between 0 and 300 seconds. ");
        }

        if (discoveryTimeout != null && (discoveryTimeout < 10 || discoveryTimeout > 300)) {
            errors.append("Discovery timeout must be between 10 and 300 seconds. ");
        }

        if (webhookPort != null && (webhookPort < 1 || webhookPort > 65535)) {
            errors.append("Webhook port must be between 1 and 65535. ");
        }

        if (imageCacheSize != null && (imageCacheSize < 1 || imageCacheSize > 1000)) {
            errors.append("Image cache size must be between 1 and 1000 MB. ");
        }

        if (imageTimeout != null && (imageTimeout < 1000 || imageTimeout > 120000)) {
            errors.append("Image timeout must be between 1000 and 120000 milliseconds. ");
        }

        if (logMaxSize != null && (logMaxSize < 1 || logMaxSize > 100)) {
            errors.append("Log max size must be between 1 and 100 MB. ");
        }

        if (connectionPoolSize != null && (connectionPoolSize < 1 || connectionPoolSize > 100)) {
            errors.append("Connection pool size must be between 1 and 100. ");
        }

        if (keepAliveTimeout != null && (keepAliveTimeout < 1000 || keepAliveTimeout > 300000)) {
            errors.append("Keep-alive timeout must be between 1000 and 300000 milliseconds. ");
        }

        if (sslHandshakeTimeout != null && (sslHandshakeTimeout < 1000 || sslHandshakeTimeout > 30000)) {
            errors.append("SSL handshake timeout must be between 1000 and 30000 milliseconds. ");
        }

        return errors.toString().trim();
    }

    /**
     * Check if caching is enabled
     */
    public boolean isCachingEnabled() {
        return enableCaching != null ? enableCaching : true;
    }

    /**
     * Check if auto discovery is enabled
     */
    public boolean isAutoDiscoveryEnabled() {
        return enableAutoDiscovery != null ? enableAutoDiscovery : true;
    }

    /**
     * Check if webhooks are enabled
     */
    public boolean isWebhookEnabled() {
        return webhookEnabled != null ? webhookEnabled : true;
    }

    /**
     * Check if image proxy is enabled
     */
    public boolean isImageProxyEnabled() {
        return enableImageProxy != null ? enableImageProxy : true;
    }

    /**
     * Check if responses should be compressed
     */
    public boolean shouldCompressResponses() {
        return compressResponses != null ? compressResponses : true;
    }

    /**
     * Check if certificates should be validated
     */
    public boolean shouldValidateCertificates() {
        return validateCertificates != null ? validateCertificates : true;
    }

    /**
     * Get effective thread pool size
     */
    public int getEffectiveThreadPoolSize() {
        if (threadPoolSize != null && threadPoolSize >= 1) {
            return threadPoolSize;
        }
        return 5;
    }

    /**
     * Get effective API timeout
     */
    public int getEffectiveApiTimeout() {
        if (apiTimeout != null && apiTimeout >= 1000) {
            return apiTimeout;
        }
        return 10000;
    }

    /**
     * Get effective cache TTL
     */
    public int getEffectiveCacheTTL() {
        if (defaultCacheTTL != null && defaultCacheTTL >= 10) {
            return defaultCacheTTL;
        }
        return 300;
    }

    /**
     * Get effective image timeout
     */
    public int getEffectiveImageTimeout() {
        if (imageTimeout != null && imageTimeout >= 1000) {
            return imageTimeout;
        }
        return 30000;
    }

    /**
     * Get effective webhook port
     */
    public int getEffectiveWebhookPort() {
        if (webhookPort != null && webhookPort >= 1) {
            return webhookPort;
        }
        return 8080;
    }

    /**
     * Get webhook base URL
     */
    public String getWebhookBaseUrl() {
        String protocol = isWebhookSSL() ? "https" : "http";
        return protocol + "://localhost:" + getEffectiveWebhookPort() + webhookBasePath;
    }

    /**
     * Check if webhook SSL is enabled
     */
    public boolean isWebhookSSL() {
        return webhookSSL != null ? webhookSSL : false;
    }

    /**
     * Get log level as enum
     */
    public java.util.logging.Level getLogLevel() {
        if (logLevel == null) {
            return java.util.logging.Level.INFO;
        }

        switch (logLevel.toUpperCase()) {
            case "TRACE":
                return java.util.logging.Level.FINEST;
            case "DEBUG":
                return java.util.logging.Level.FINE;
            case "INFO":
                return java.util.logging.Level.INFO;
            case "WARN":
                return java.util.logging.Level.WARNING;
            case "ERROR":
                return java.util.logging.Level.SEVERE;
            default:
                return java.util.logging.Level.INFO;
        }
    }

    /**
     * Update configuration from OpenHAB Configuration object
     */
    public void updateFromConfiguration(Configuration config) {
        if (config == null) {
            return;
        }

        // System Configuration
        updateIntConfig(config, "threadPoolSize", v -> threadPoolSize = v);
        updateIntConfig(config, "maxConcurrentRequests", v -> maxConcurrentRequests = v);
        updateIntConfig(config, "requestQueueSize", v -> requestQueueSize = v);

        // API Configuration
        apiBaseUrl = (String) config.get("apiBaseUrl");
        updateIntConfig(config, "apiTimeout", v -> apiTimeout = v);
        updateIntConfig(config, "apiRetryCount", v -> apiRetryCount = v);
        updateIntConfig(config, "apiRetryDelay", v -> apiRetryDelay = v);

        // Cache Configuration
        updateBoolConfig(config, "enableCaching", v -> enableCaching = v);
        updateIntConfig(config, "defaultCacheTTL", v -> defaultCacheTTL = v);
        updateIntConfig(config, "maxCacheSize", v -> maxCacheSize = v);

        // Discovery Configuration
        updateBoolConfig(config, "enableAutoDiscovery", v -> enableAutoDiscovery = v);
        updateIntConfig(config, "discoveryDelay", v -> discoveryDelay = v);
        updateIntConfig(config, "discoveryTimeout", v -> discoveryTimeout = v);

        // Webhook Configuration
        webhookBasePath = (String) config.get("webhookBasePath");
        updateBoolConfig(config, "webhookEnabled", v -> webhookEnabled = v);
        updateIntConfig(config, "webhookPort", v -> webhookPort = v);
        updateBoolConfig(config, "webhookSSL", v -> webhookSSL = v);

        // Image Configuration
        updateBoolConfig(config, "enableImageProxy", v -> enableImageProxy = v);
        updateIntConfig(config, "imageCacheSize", v -> imageCacheSize = v);
        updateIntConfig(config, "imageTimeout", v -> imageTimeout = v);

        // Logging Configuration
        logLevel = (String) config.get("logLevel");
        updateBoolConfig(config, "logApiRequests", v -> logApiRequests = v);
        updateBoolConfig(config, "logApiResponses", v -> logApiResponses = v);
        updateIntConfig(config, "logMaxSize", v -> logMaxSize = v);

        // Performance Configuration
        updateBoolConfig(config, "compressResponses", v -> compressResponses = v);
        updateIntConfig(config, "connectionPoolSize", v -> connectionPoolSize = v);
        updateIntConfig(config, "keepAliveTimeout", v -> keepAliveTimeout = v);

        // Security Configuration
        updateBoolConfig(config, "validateCertificates", v -> validateCertificates = v);
        updateIntConfig(config, "sslHandshakeTimeout", v -> sslHandshakeTimeout = v);

        // Allowed ciphers (array handling)
        Object ciphersObj = config.get("allowedCiphers");
        if (ciphersObj instanceof String) {
            String ciphersStr = (String) ciphersObj;
            if (!ciphersStr.trim().isEmpty()) {
                allowedCiphers = ciphersStr.split(",");
            }
        }
    }

    /**
     * Convert to OpenHAB Configuration object
     */
    public Configuration toConfiguration() {
        Configuration config = new Configuration();

        // System Configuration
        config.put("threadPoolSize", threadPoolSize);
        config.put("maxConcurrentRequests", maxConcurrentRequests);
        config.put("requestQueueSize", requestQueueSize);

        // API Configuration
        config.put("apiBaseUrl", apiBaseUrl);
        config.put("apiTimeout", apiTimeout);
        config.put("apiRetryCount", apiRetryCount);
        config.put("apiRetryDelay", apiRetryDelay);

        // Cache Configuration
        config.put("enableCaching", enableCaching);
        config.put("defaultCacheTTL", defaultCacheTTL);
        config.put("maxCacheSize", maxCacheSize);

        // Discovery Configuration
        config.put("enableAutoDiscovery", enableAutoDiscovery);
        config.put("discoveryDelay", discoveryDelay);
        config.put("discoveryTimeout", discoveryTimeout);

        // Webhook Configuration
        config.put("webhookBasePath", webhookBasePath);
        config.put("webhookEnabled", webhookEnabled);
        config.put("webhookPort", webhookPort);
        config.put("webhookSSL", webhookSSL);

        // Image Configuration
        config.put("enableImageProxy", enableImageProxy);
        config.put("imageCacheSize", imageCacheSize);
        config.put("imageTimeout", imageTimeout);

        // Logging Configuration
        config.put("logLevel", logLevel);
        config.put("logApiRequests", logApiRequests);
        config.put("logApiResponses", logApiResponses);
        config.put("logMaxSize", logMaxSize);

        // Performance Configuration
        config.put("compressResponses", compressResponses);
        config.put("connectionPoolSize", connectionPoolSize);
        config.put("keepAliveTimeout", keepAliveTimeout);

        // Security Configuration
        config.put("validateCertificates", validateCertificates);
        config.put("sslHandshakeTimeout", sslHandshakeTimeout);

        // Allowed ciphers
        if (allowedCiphers != null) {
            config.put("allowedCiphers", String.join(",", allowedCiphers));
        }

        return config;
    }

    @Override
    public String toString() {
        return "RachioConfiguration{" + "threadPoolSize=" + threadPoolSize + ", maxConcurrentRequests="
                + maxConcurrentRequests + ", enableCaching=" + enableCaching + ", defaultCacheTTL=" + defaultCacheTTL
                + ", enableAutoDiscovery=" + enableAutoDiscovery + ", webhookEnabled=" + webhookEnabled
                + ", webhookPort=" + webhookPort + ", enableImageProxy=" + enableImageProxy + ", logLevel='" + logLevel
                + '\'' + ", compressResponses=" + compressResponses + ", validateCertificates=" + validateCertificates
                + '}';
    }

    // Helper methods for configuration updates

    private interface IntConsumer {
        void accept(int value);
    }

    private interface BoolConsumer {
        void accept(boolean value);
    }

    private void updateIntConfig(Configuration config, String key, IntConsumer setter) {
        Object obj = config.get(key);
        if (obj instanceof Number) {
            setter.accept(((Number) obj).intValue());
        } else if (obj instanceof String) {
            try {
                setter.accept(Integer.parseInt((String) obj));
            } catch (NumberFormatException e) {
                // Keep default
            }
        }
    }

    private void updateBoolConfig(Configuration config, String key, BoolConsumer setter) {
        Object obj = config.get(key);
        if (obj instanceof Boolean) {
            setter.accept((Boolean) obj);
        } else if (obj instanceof String) {
            setter.accept(Boolean.parseBoolean((String) obj));
        }
    }
}
