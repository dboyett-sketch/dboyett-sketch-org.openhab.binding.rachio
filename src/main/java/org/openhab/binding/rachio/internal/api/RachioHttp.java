package org.openhab.binding.rachio.internal.api;

// REMOVE these incompatible imports:
// import java.io.BufferedReader;
// import java.io.InputStreamReader;
// import java.io.OutputStream;
// import java.net.HttpURLConnection;
// import java.net.URL;

// ADD these imports:
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = RachioHttp.class)
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);
    
    private RachioHttpClient rachioHttpClient;
    
    @Reference
    public void setRachioHttpClient(RachioHttpClient rachioHttpClient) {
        this.rachioHttpClient = rachioHttpClient;
    }
    
    public void unsetRachioHttpClient(RachioHttpClient rachioHttpClient) {
        this.rachioHttpClient = null;
    }
    
    // Update all methods to use rachioHttpClient instead of HttpURLConnection
    public String httpGet(String url, String apiKey) throws RachioApiException {
        return rachioHttpClient.get(url, apiKey);
    }
    
    public String httpPost(String url, String apiKey, String body) throws RachioApiException {
        return rachioHttpClient.post(url, apiKey, body);
    }
    
    // ... rest of methods updated similarly
}
