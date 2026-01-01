package org.openhab.binding.rachio.internal.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
 * The {@link RachioImageServlet} is responsible for serving irrigation images from Rachio
 *
 * @author Damion Boyett - Refactor contribution
 */
@NonNullByDefault
public class RachioImageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(RachioImageServlet.class);

    private static final String IMAGE_PATH = "/rachioimage";

    private @Nullable RachioImageServletService imageService;

    /**
     * Constructor for RachioImageServlet
     * 
     * @param imageService The image service to use (can be null)
     */
    public RachioImageServlet(@Nullable RachioImageServletService imageService) {
        this.imageService = imageService;
        logger.debug("RachioImageServlet created with imageService: {}", imageService != null ? "present" : "null");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RachioImageServletService localImageService = imageService;

        if (localImageService != null) {
            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.length() > 1) {

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

    /**
     * Set the image service (for cases where constructor injection isn't used)
     * 
     * @param imageService The image service
     */
    public void setImageService(@Nullable RachioImageServletService imageService) {
        this.imageService = imageService;
    }
}
