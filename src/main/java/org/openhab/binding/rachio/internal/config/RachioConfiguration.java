package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Markus Michels - Initial contribution
 */
@NonNullByDefault
public class RachioConfiguration {
    
    // Bridge configuration parameters - ALL PARAMETERS INCLUDED
    public @Nullable String apikey;
    public int pollingInterval = DEFAULT_POLLING_INTERVAL;
    public int defaultRuntime = DEFAULT_DURATION;
    
    // Webhook configuration
    public @Nullable String callbackUrl;
    public boolean clearAllCallbacks = false;
    public @Nullable String ipFilter;

    // Thing configuration (for devices/zones)
    public @Nullable String deviceId;
    public @Nullable String zoneId;
    public @Nullable String personId;

    /**
     * Update configuration from properties
     */
    public void updateConfig(@Nullable Configuration config) {
        if (config != null) {
            apikey = (String) config.get(API_KEY);
            
            // Polling interval with validation
            Object pollingObj = config.get(POLLING_INTERVAL);
            if (pollingObj != null) {
                pollingInterval = ((Number) pollingObj).intValue();
                if (pollingInterval < MIN_POLLING_INTERVAL) {
                    pollingInterval = DEFAULT_POLLING_INTERVAL; // Enforce minimum for rate limiting
                }
            } else {
                pollingInterval = DEFAULT_POLLING_INTERVAL; // default
            }
            
            // Default runtime
            Object runtimeObj = config.get(DEFAULT_RUNTIME);
            if (runtimeObj != null) {
                defaultRuntime = ((Number) runtimeObj).intValue();
                if (defaultRuntime <= 0) {
                    defaultRuntime = DEFAULT_DURATION;
                }
            } else {
                defaultRuntime = DEFAULT_DURATION; // default
            }
            
            // Webhook configuration
            callbackUrl = (String) config.get(CALLBACK_URL);
            
            Object clearCallbacksObj = config.get(CLEAR_ALL_CALLBACKS);
            if (clearCallbacksObj != null) {
                clearAllCallbacks = (Boolean) clearCallbacksObj;
            } else {
                clearAllCallbacks = false; // default
            }
            
            ipFilter = (String) config.get(IP_FILTER);
            deviceId = (String) config.get(DEVICE_ID);
            zoneId = (String) config.get(ZONE_ID);
            personId = (String) config.get("personId"); // Note: personId is not in BindingConstants
        }
    }

    /**
     * Validate configuration
     */
    public boolean isValid() {
        return apikey != null && !apikey.trim().isEmpty();
    }

    /**
     * Validate configuration for bridge
     */
    public boolean isValidBridgeConfig() {
        if (!isValid()) {
            return false;
        }
        
        // Additional bridge-specific validation
        if (pollingInterval < MIN_POLLING_INTERVAL) {
            return false; // Rate limit safety
        }
        
        if (defaultRuntime <= 0 || defaultRuntime > MAX_RUNTIME_SECONDS) {
            return false;
        }
        
        return true;
    }

    /**
     * Validate configuration for device
     */
    public boolean isValidDeviceConfig() {
        return deviceId != null && !deviceId.trim().isEmpty();
    }

    /**
     * Validate configuration for zone
     */
    public boolean isValidZoneConfig() {
        return zoneId != null && !zoneId.trim().isEmpty() && 
               deviceId != null && !deviceId.trim().isEmpty();
    }

    // Getters with proper null safety
    public @Nullable String getApiKey() {
        return apikey;
    }

    public int getPollingInterval() {
        return pollingInterval;
    }

    public int getDefaultRuntime() {
        return defaultRuntime;
    }

    public @Nullable String getCallbackUrl() {
        return callbackUrl;
    }

    public boolean isClearAllCallbacks() {
        return clearAllCallbacks;
    }

    public @Nullable String getIpFilter() {
        return ipFilter;
    }

    public @Nullable String getDeviceId() {
        return deviceId;
    }

    public @Nullable String getZoneId() {
        return zoneId;
    }

    public @Nullable String getPersonId() {
        return personId;
    }

    /**
     * Get configuration summary for logging
     */
    public String getConfigSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("RachioConfiguration{");
        sb.append("apikey='").append(apikey != null ? apikey.substring(0, Math.min(8, apikey.length())) + "..." : "null").append("'");
        sb.append(", pollingInterval=").append(pollingInterval);
        sb.append(", defaultRuntime=").append(defaultRuntime);
        sb.append(", callbackUrl='").append(callbackUrl != null ? "configured" : "null").append("'");
        sb.append(", clearAllCallbacks=").append(clearAllCallbacks);
        sb.append(", ipFilter='").append(ipFilter != null ? ipFilter : "null").append("'");
        sb.append(", deviceId='").append(deviceId != null ? deviceId : "null").append("'");
        sb.append(", zoneId='").append(zoneId != null ? zoneId : "null").append("'");
        sb.append(", personId='").append(personId != null ? personId : "null").append("'");
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String toString() {
        return getConfigSummary();
    }

    /**
     * Create configuration from individual parameters
     */
    public static RachioConfiguration create(String apiKey, int pollingInterval, int defaultRuntime, 
                                           @Nullable String callbackUrl, boolean clearCallbacks, 
                                           @Nullable String ipFilter) {
        RachioConfiguration config = new RachioConfiguration();
        config.apikey = apiKey;
        config.pollingInterval = pollingInterval;
        config.defaultRuntime = defaultRuntime;
        config.callbackUrl = callbackUrl;
        config.clearAllCallbacks = clearCallbacks;
        config.ipFilter = ipFilter;
        return config;
    }

    /**
     * Create device configuration
     */
    public static RachioConfiguration createDeviceConfig(String deviceId) {
        RachioConfiguration config = new RachioConfiguration();
        config.deviceId = deviceId;
        return config;
    }

    /**
     * Create zone configuration
     */
    public static RachioConfiguration createZoneConfig(String deviceId, String zoneId) {
        RachioConfiguration config = new RachioConfiguration();
        config.deviceId = deviceId;
        config.zoneId = zoneId;
        return config;
    }
}