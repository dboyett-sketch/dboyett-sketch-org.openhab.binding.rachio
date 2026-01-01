package org.openhab.binding.rachio.internal.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for handling Rachio webhook requests.
 * 
 *
 * @author Damion Boyett - Refactor contribution
 * @deprecated Use {@link RachioWebHookServletService} instead
 */
@Deprecated
@NonNullByDefault
public class RachioWebHookServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(RachioWebHookServlet.class);

    private @Nullable RachioBridgeHandler bridgeHandler;

    public RachioWebHookServlet() {
        logger.debug("RachioWebHookServlet created (deprecated)");
    }

    public void setBridgeHandler(RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String payload = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);

            if (payload == null || payload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty payload");
                return;
            }

            RachioBridgeHandler handler = bridgeHandler;
            if (handler != null) {
                handler.processWebhook(payload);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("OK");
            } else {
                logger.warn("Received webhook but bridge handler is not available");
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Bridge handler not available");
            }
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

    @Override
    public void init() throws ServletException {
        logger.debug("RachioWebHookServlet initialized");
        super.init();
    }

    @Override
    public void destroy() {
        logger.debug("RachioWebHookServlet destroyed");
        super.destroy();
    }
}
