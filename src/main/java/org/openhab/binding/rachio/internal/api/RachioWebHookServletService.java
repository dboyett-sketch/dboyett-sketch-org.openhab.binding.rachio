package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for handling Rachio webhook servlet requests.
 * 
 * @author Brian Higginbotham - Initial contribution
 */
@Component(service = RachioWebHookServletService.class)
@NonNullByDefault
public class RachioWebHookServletService {

    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServletService.class);
    private final HttpService httpService;

    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable String servletAlias;

    @Activate
    public RachioWebHookServletService(@Reference HttpService httpService) {
        this.httpService = httpService;
    }

    public void setBridgeHandler(RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        try {
            RachioWebHookServlet servlet = new RachioWebHookServlet(this);
            Dictionary<String, String> servletParams = new Hashtable<>();
            servletParams.put("alias", "/rachio/webhook");
            servletAlias = "/rachio/webhook";

            httpService.registerServlet(servletAlias, servlet, servletParams, null);
            logger.debug("Rachio webhook servlet registered at {}", servletAlias);
        } catch (ServletException | NamespaceException e) {
            logger.error("Error registering Rachio webhook servlet: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error registering Rachio webhook servlet: {}", e.getMessage(), e);
        }
    }

    @Deactivate
    public void deactivate() {
        if (servletAlias != null) {
            httpService.unregister(servletAlias);
            logger.debug("Rachio webhook servlet unregistered");
        }
    }

    public void processWebhook(String payload) {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            handler.processWebhook(payload);
        } else {
            logger.warn("Received webhook but bridge handler is not available");
        }
    }

    /**
     * Internal servlet class for handling webhook requests.
     */
    public static class RachioWebHookServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private final RachioWebHookServletService service;
        // ==== SURGICAL PATCH START ====
        // FIXED: Changed logger from static reference to instance variable
        // This resolves: "Cannot make a static reference to the non-static field logger"
        private final Logger servletLogger = LoggerFactory.getLogger(RachioWebHookServlet.class);
        // ==== SURGICAL PATCH END ====

        public RachioWebHookServlet(RachioWebHookServletService service) {
            this.service = service;
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                String payload = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);

                if (payload == null || payload.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty payload");
                    return;
                }

                service.processWebhook(payload);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("OK");
            } catch (IOException e) {
                // ==== SURGICAL PATCH: Updated logger reference ====
                servletLogger.error("Error processing webhook request: {}", e.getMessage(), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            resp.getWriter().write("GET not supported, use POST");
        }
    }
}
