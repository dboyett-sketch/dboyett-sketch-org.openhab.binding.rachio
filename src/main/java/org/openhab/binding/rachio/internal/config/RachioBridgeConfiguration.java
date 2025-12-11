package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioBridgeConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Damian S - Initial contribution
 */
@NonNullByDefault
public class RachioBridgeConfiguration {

    public String accessToken = "";
    public String webhookUrl = "";  // ADDED: Missing field causing compilation error
    public int refreshInterval = 60;
    public @Nullable String webhookExternalUrl;
    public boolean verifyWebhookIp = true;
    public @Nullable String webhookAllowedIp;
    public boolean loadAwsIpRanges = true;
    public int webhookTimeout = 30;

    // ADDED: Getter methods for accessing configuration
    public String getAccessToken() {
        return accessToken;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public @Nullable String getWebhookExternalUrl() {
        return webhookExternalUrl;
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
}
