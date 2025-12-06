package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service component for Rachio Webhook Servlet
 * Manages servlet lifecycle and bridge handler registration
 * 
 * @author Dave Boyett - Initial contribution
 */
@Component(service = RachioWebHookServletService.class, immediate = true)
@NonNullByDefault
public class RachioWebHookServletService {
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServletService.class);
    
    private final Map<String, RachioBridgeHandler> bridgeHandlers = new ConcurrentHashMap<>();
    private @Nullable RachioWebHookServlet webhookServlet;
    private @Nullable HttpService httpService;
    
    @Activate
    public void activate() {
        logger.info("Rachio WebHook Servlet Service activated");
        // Servlet will be created when HttpService becomes available
    }
    
    @Deactivate
    public void deactivate() {
        logger.info("Rachio WebHook Servlet Service deactivated");
        
        // Clean up servlet
        RachioWebHookServlet servlet = webhookServlet;
        if (servlet != null) {
            servlet.deactivate();
            webhookServlet = null;
        }
        
        bridgeHandlers.clear();
    }
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addBridgeHandler(RachioBridgeHandler handler) {
        String thingId = handler.getThing().getUID().getId();
        bridgeHandlers.put(thingId, handler);
        logger.debug("Registered bridge handler: {}", thingId);
        
        // Pass to servlet if it exists
        RachioWebHookServlet servlet = webhookServlet;
        if (servlet != null) {
            servlet.addBridgeHandler(handler);
        }
    }
    
    public void removeBridgeHandler(RachioBridgeHandler handler) {
        String thingId = handler.getThing().getUID().getId();
        bridgeHandlers.remove(thingId);
        logger.debug("Unregistered bridge handler: {}", thingId);
        
        // Pass to servlet if it exists
        RachioWebHookServlet servlet = webhookServlet;
        if (servlet != null) {
            servlet.removeBridgeHandler(handler);
        }
    }
    
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
        logger.info("HttpService available, creating webhook servlet");
        
        // Create servlet now that HttpService is available
        createWebhookServlet();
    }
    
    public void unsetHttpService(HttpService httpService) {
        if (this.httpService == httpService) {
            this.httpService = null;
            
            // Clean up servlet
            RachioWebHookServlet servlet = webhookServlet;
            if (servlet != null) {
                servlet.deactivate();
                webhookServlet = null;
            }
        }
    }
    
    private void createWebhookServlet() {
        HttpService localHttpService = httpService;
        if (localHttpService != null) {
            try {
                webhookServlet = new RachioWebHookServlet(localHttpService);
                logger.info("Rachio webhook servlet created");
                
                // Register existing bridge handlers with servlet
                for (RachioBridgeHandler handler : bridgeHandlers.values()) {
                    webhookServlet.addBridgeHandler(handler);
                }
                
            } catch (Exception e) {
                logger.error("Failed to create webhook servlet: {}", e.getMessage(), e);
            }
        }
    }
    
    // Getters for monitoring
    
    public boolean isServletActive() {
        RachioWebHookServlet servlet = webhookServlet;
        return servlet != null && servlet.isServletRegistered();
    }
    
    public int getRegisteredHandlerCount() {
        return bridgeHandlers.size();
    }
    
    public @Nullable RachioWebHookServlet getWebhookServlet() {
        return webhookServlet;
    }
}
