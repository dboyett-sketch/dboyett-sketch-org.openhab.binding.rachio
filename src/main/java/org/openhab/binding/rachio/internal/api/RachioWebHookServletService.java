package org.openhab.binding.rachio.internal.api;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for handling Rachio webhook servlet requests.
 * 
 * @author Damion Boyett - Refactor contribution
 */
@Component(service = { Servlet.class, RachioWebHookServletService.class })
@HttpWhiteboardServletPattern("/rachio/webhook")
@NonNullByDefault
public class RachioWebHookServletService extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServletService.class);

    private @Nullable RachioBridgeHandler bridgeHandler;

    @Activate
    public RachioWebHookServletService() {
        logger.debug("RachioWebHookServletService activated with Http Whiteboard pattern");
    }

    public void setBridgeHandler(RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
        logger.debug("Bridge handler set for webhook servlet");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String payload = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);

            if (payload == null || payload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty payload");
                return;
            }

            processWebhook(payload);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("OK");
        } catch (IOException e) {
            logger.error("Error processing webhook request: {}", e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.getWriter().write("GET not supported, use POST");
    }

    public void processWebhook(String payload) {
        RachioBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            handler.processWebhook(payload);
        } else {
            logger.warn("Received webhook but bridge handler is not available");
        }
    }

    @Deactivate
    public void deactivate() {
        logger.debug("RachioWebHookServletService deactivated");
    }
}
