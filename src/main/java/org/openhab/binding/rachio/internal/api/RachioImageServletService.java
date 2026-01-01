package org.openhab.binding.rachio.internal.api;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { Servlet.class, RachioImageServletService.class })
@HttpWhiteboardServletPattern("/rachio/image/*")
public class RachioImageServletService extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(RachioImageServletService.class);

    // Bridge handler field for servlet
    private @Nullable RachioBridgeHandler bridgeHandler;

    @Activate
    public RachioImageServletService() {
        logger.debug("RachioImageServletService activated with Http Whiteboard pattern");
    }

    @Nullable
    public BufferedImage getImage(@NonNull String imageUrlString) {
        if (imageUrlString.isEmpty()) {
            return null;
        }
        BufferedImage image = fetchImageFromUrl(imageUrlString);
        if (image == null) {
            logger.debug("Failed to fetch image from URL: {}", imageUrlString);
        }
        return image;
    }

    @Nullable
    private BufferedImage fetchImageFromUrl(@NonNull String imageUrlString) {
        if (imageUrlString.isEmpty()) {
            return null;
        }
        try {
            URI imageUri = new URI(imageUrlString);
            URL imageUrl = imageUri.toURL();

            HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                byte[] imageData = connection.getInputStream().readAllBytes();
                return ImageIO.read(new ByteArrayInputStream(imageData));
            } else {
                logger.debug("Failed to fetch image, HTTP code: {}", responseCode);
            }
        } catch (URISyntaxException e) {
            logger.debug("Invalid image URL syntax: {} - {}", imageUrlString, e.getMessage());
        } catch (MalformedURLException e) {
            logger.debug("Malformed image URL: {} - {}", imageUrlString, e.getMessage());
        } catch (IOException e) {
            logger.debug("Error fetching image: {}", e.getMessage());
        }
        return null;
    }

    // ========== ADD METHODS CALLED BY RachioImageServlet ==========

    /**
     * Get image data as byte array for servlet response.
     * Called by RachioImageServlet.doGet() at line 117
     * 
     * @param imageUrlString URL of the image to fetch
     * @return byte array of image data or null if not found
     */
    public byte @Nullable [] getImageData(@NonNull String imageUrlString) {
        BufferedImage image = getImage(imageUrlString);
        if (image == null) {
            return null;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Use PNG format for consistency
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            logger.debug("Error converting image to bytes: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Set the bridge handler for this service.
     * Called by RachioImageServlet.init() at line 146
     * 
     * @param bridgeHandler The bridge handler to set
     */
    public void setBridgeHandler(@Nullable RachioBridgeHandler bridgeHandler) {
        this.bridgeHandler = bridgeHandler;
    }

    /**
     * Get the bridge handler for this service.
     * 
     * @return The bridge handler or null if not set
     */
    @Nullable
    public RachioBridgeHandler getBridgeHandler() {
        return bridgeHandler;
    }

    // ========== HttpServlet Methods ==========

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // This servlet now handles its own requests via Http Whiteboard
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Image URL required");
            return;
        }

        // Extract image URL from path (remove leading slash)
        String imageUrlString = pathInfo.substring(1);

        byte[] imageData = getImageData(imageUrlString);
        if (imageData == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
            return;
        }

        resp.setContentType("image/png");
        resp.setContentLength(imageData.length);
        resp.getOutputStream().write(imageData);
    }

    @Deactivate
    public void deactivate() {
        logger.debug("RachioImageServletService deactivated");
    }
}
