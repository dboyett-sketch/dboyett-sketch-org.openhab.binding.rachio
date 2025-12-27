package org.openhab.binding.rachio.internal.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioImageServlet} is responsible for serving irrigation images from Rachio
 *
 * @author Michael Lobstein - Initial contribution
 */
@Component(service = HttpServlet.class, immediate = true)
@NonNullByDefault
public class RachioImageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(RachioImageServlet.class);

    private static final String IMAGE_PATH = "/rachioimage";

    private @Nullable HttpService httpService;
    private @Nullable ServiceRegistration<?> serviceRegistration;
    private @Nullable RachioImageServletService imageService;

    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            logger.debug("Starting Rachio Image servlet at {}", IMAGE_PATH);

            HttpService localHttpService = httpService;
            if (localHttpService != null) {
                Hashtable<String, String> servletParams = new Hashtable<>();
                servletParams.put("alias", IMAGE_PATH);

                localHttpService.registerServlet(IMAGE_PATH, this, servletParams, null);

                // Also register as a service
                serviceRegistration = bundleContext.registerService(HttpServlet.class.getName(), this, null);

                logger.debug("Started Rachio Image servlet at {}", IMAGE_PATH);
            } else {
                logger.error("HTTP Service not available for Rachio Image servlet");
            }
        } catch (NamespaceException | ServletException e) {
            logger.error("Error during Rachio Image servlet startup: {}", e.getMessage(), e);
        }
    }

    @Deactivate
    protected void deactivate() {
        logger.debug("Stopping Rachio Image servlet");

        ServiceRegistration<?> localServiceRegistration = serviceRegistration;
        if (localServiceRegistration != null) {
            localServiceRegistration.unregister();
            serviceRegistration = null;
        }

        HttpService localHttpService = httpService;
        if (localHttpService != null) {
            localHttpService.unregister(IMAGE_PATH);
        }

        logger.debug("Stopped Rachio Image servlet");
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setHttpService(@Nullable HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unsetHttpService(@Nullable HttpService httpService) {
        this.httpService = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setImageService(@Nullable RachioImageServletService imageService) {
        this.imageService = imageService;
    }

    protected void unsetImageService(@Nullable RachioImageServletService imageService) {
        this.imageService = null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RachioImageServletService localImageService = imageService;

        if (localImageService != null) {
            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.length() > 1) {
                // Remove leading slash
                String imageId = pathInfo.substring(1);
                byte[] imageData = localImageService.getImageData(imageId);

                if (imageData != null && imageData.length > 0) {
                    resp.setContentType("image/png");
                    resp.setContentLength(imageData.length);
                    resp.setHeader("Cache-Control", "public, max-age=3600");

                    try (InputStream in = new ByteArrayInputStream(imageData);
                            OutputStream out = resp.getOutputStream()) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    return;
                }
            }
        }

        // If we get here, no image was found
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("text/plain");
        resp.getWriter().write("Image not found");
    }

    public void setBridgeHandler(@Nullable RachioBridgeHandler bridgeHandler) {
        RachioImageServletService localImageService = imageService;
        if (localImageService != null && bridgeHandler != null) {
            localImageService.setBridgeHandler(bridgeHandler);
        }
    }

    public String getImageUrl(@Nullable String imageId) {
        if (imageId == null || imageId.isEmpty()) {
            return "";
        }
        return IMAGE_PATH + "/" + imageId;
    }
}
