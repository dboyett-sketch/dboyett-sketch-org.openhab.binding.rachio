package org.openhab.binding.rachio.internal.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
 * Service for handling Rachio image servlet requests.
 *
 * @author Brian Higginbotham - Initial contribution
 */
@Component(service = RachioImageServletService.class)
@NonNullByDefault
public class RachioImageServletService {

    private final Logger logger = LoggerFactory.getLogger(RachioImageServletService.class);
    private final HttpService httpService;

    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable String servletAlias;

    @Activate
    public RachioImageServletService(@Reference HttpService httpService) {
        this.httpService = httpService;
    }

    public void setBridgeHandler(RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        try {
            RachioImageServlet servlet = new RachioImageServlet(this);
            Dictionary<String, String> servletParams = new Hashtable<>();
            servletParams.put("alias", "/rachio/image");
            servletAlias = "/rachio/image";

            httpService.registerServlet(servletAlias, servlet, servletParams, null);
            logger.debug("Rachio image servlet registered at {}", servletAlias);
        } catch (ServletException | NamespaceException e) {
            logger.error("Error registering Rachio image servlet: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error registering Rachio image servlet: {}", e.getMessage(), e);
        }
    }

    @Deactivate
    public void deactivate() {
        if (servletAlias != null) {
            httpService.unregister(servletAlias);
            logger.debug("Rachio image servlet unregistered");
        }
    }

    public byte @Nullable [] getImageData(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        try (InputStream in = new URL(imageUrl).openStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        } catch (IOException e) {
            logger.debug("Error downloading image from {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Internal servlet class for handling image requests.
     */
    public static class RachioImageServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private final RachioImageServletService service;

        public RachioImageServlet(RachioImageServletService service) {
            this.service = service;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Image ID required");
                return;
            }

            String imageId = pathInfo.substring(1); // Remove leading slash
            RachioBridgeHandler bridgeHandler = service.bridgeHandler;

            if (bridgeHandler == null) {
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Bridge handler not available");
                return;
            }

            String imageUrl = bridgeHandler.getImageUrl(imageId);
            if (imageUrl == null || imageUrl.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }

            byte[] imageData = service.getImageData(imageUrl);
            if (imageData == null || imageData.length == 0) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image data not available");
                return;
            }

            resp.setContentType("image/jpeg");
            resp.setContentLength(imageData.length);
            resp.setHeader("Cache-Control", "public, max-age=3600");
            resp.getOutputStream().write(imageData);
        }
    }
}
