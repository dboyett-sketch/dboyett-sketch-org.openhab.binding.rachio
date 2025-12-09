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
    public @Nullable String accessToken;
    public @Nullable String secretKey;
    public @Nullable String deviceId;
    public @Nullable String webhookUrl;
    public @Nullable Boolean webhookEnabled;
    public @Nullable Integer webhookCheckInterval;
    public @Nullable String allowedIps;
    public @Nullable Boolean allowAwsIps;
    public @Nullable Boolean weatherEnabled;
    
    // Getters for compatibility
    public @Nullable String getAccessToken() { return accessToken; }
    public @Nullable String getSecretKey() { return secretKey; }
    public @Nullable String getDeviceId() { return deviceId; }
    public @Nullable String getWebhookUrl() { return webhookUrl; }
    public @Nullable Boolean isWebhookEnabled() { return webhookEnabled; }
    public @Nullable Integer getWebhookCheckInterval() { return webhookCheckInterval; }
    public @Nullable String getAllowedIps() { return allowedIps; }
    public @Nullable Boolean isAllowAwsIps() { return allowAwsIps; }
    public @Nullable Boolean isWeatherEnabled() { return weatherEnabled; }
}
