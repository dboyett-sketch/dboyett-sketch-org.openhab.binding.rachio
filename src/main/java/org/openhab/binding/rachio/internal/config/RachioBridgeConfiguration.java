package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

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

    @Override
    public String toString() {
        return "RachioBridgeConfiguration [apiKey=" + (apiKey != null ? "[PROTECTED]" : "null") 
            + ", refresh=" + refresh 
            + ", webhookPort=" + webhookPort 
            + ", webhookEnabled=" + webhookEnabled 
            + ", host=" + host 
            + ", port=" + port 
            + ", callbackUrl=" + callbackUrl 
            + ", ipFilter=" + ipFilter 
            + ", clearAllCallbacks=" + clearAllCallbacks 
            + ", defaultRuntime=" + defaultRuntime + "]";
    }
}
