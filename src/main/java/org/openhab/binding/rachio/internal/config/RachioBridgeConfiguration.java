package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.ParameterOption;

/**
 * The {@link RachioBridgeConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBridgeConfiguration {

    @Nullable
    public String apiKey;
    
    @Nullable
    public String personId;
    
    @Nullable
    public String secretKey;
    
    @Nullable
    public Integer refresh;
    
    @Nullable
    public Integer webhookPort;
    
    @Nullable
    public Boolean webhookEnabled;
    
    @Nullable
    public String host;
    
    @Nullable
    public Integer port;
    
    @Nullable
    public String callbackUrl;
    
    @Nullable
    public String ipFilter;
    
    @Nullable
    public Boolean clearAllCallbacks;
    
    @Nullable
    public Integer defaultRuntime;
    
    @Nullable
    public Boolean useAwsIpRanges; // ADDED: Optional AWS IP range verification
    
    @Nullable
    public String customIpRanges;  // ADDED: Custom IP ranges for filtering

    @Override
    public String toString() {
        return "RachioBridgeConfiguration [apiKey=" + (apiKey != null ? "[PROTECTED]" : "null") 
            + ", personId=" + personId
            + ", secretKey=" + (secretKey != null ? "[PROTECTED]" : "null")
            + ", refresh=" + refresh 
            + ", webhookPort=" + webhookPort 
            + ", webhookEnabled=" + webhookEnabled 
            + ", host=" + host 
            + ", port=" + port 
            + ", callbackUrl=" + callbackUrl 
            + ", ipFilter=" + ipFilter 
            + ", clearAllCallbacks=" + clearAllCallbacks 
            + ", defaultRuntime=" + defaultRuntime 
            + ", useAwsIpRanges=" + useAwsIpRanges
            + ", customIpRanges=" + customIpRanges + "]";
    }
}
