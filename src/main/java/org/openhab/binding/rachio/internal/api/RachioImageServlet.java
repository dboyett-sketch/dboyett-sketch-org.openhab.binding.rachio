package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for handling Rachio image requests (zone maps, device images, etc.)
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioImageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private final Logger logger = LoggerFactory.getLogger(RachioImageServlet.class);
    private final RachioHandler handler;

    public RachioImageServlet(RachioHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("Received image request: {}", req.getPathInfo());
        
        try {
            // Parse the request path to determine what image is being requested
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"No image path specified\"}");
                return;
            }
            
            // Remove leading slash if present
            if (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            
            // Handle different types of image requests
            byte[] imageData = handleImageRequest(pathInfo, req);
            
            if (imageData == null || imageData.length == 0) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\":\"Image not found\"}");
                return;
            }
            
            // Determine content type based on file extension or data
            String contentType = determineContentType(pathInfo);
            
            // Set response headers
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType(contentType);
            resp.setContentLength(imageData.length);
            resp.setHeader("Cache-Control", "public, max-age=3600"); // Cache for 1 hour
            
            // Write image data to response
            try (OutputStream out = resp.getOutputStream()) {
                out.write(imageData);
                out.flush();
            }
            
            logger.debug("Successfully served image: {} ({} bytes)", pathInfo, imageData.length);
            
        } catch (Exception e) {
            logger.error("Error processing image request: {}", e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Internal server error\"}");
        }
    }

    /**
     * Handle different types of image requests
     */
    private @Nullable byte[] handleImageRequest(String path, HttpServletRequest req) {
        // Parse the path to determine what's being requested
        // Format could be: device/{id}/image, zone/{id}/map, etc.
        
        String[] pathParts = path.split("/");
        if (pathParts.length < 3) {
            logger.warn("Invalid image path format: {}", path);
            return null;
        }
        
        String entityType = pathParts[0]; // "device" or "zone"
        String entityId = pathParts[1];   // ID of the device or zone
        String imageType = pathParts[2];  // "image", "map", etc.
        
        logger.debug("Image request - Type: {}, ID: {}, Image: {}", entityType, entityId, imageType);
        
        // For now, return a placeholder image
        // In a real implementation, this would fetch the actual image from Rachio API
        // or generate it based on device/zone data
        
        return generatePlaceholderImage(entityType, entityId, imageType);
    }
    
    /**
     * Generate a placeholder image for testing
     */
    private byte[] generatePlaceholderImage(String entityType, String entityId, String imageType) {
        // Create a simple SVG placeholder image
        String svg = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<svg width=\"400\" height=\"300\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
            "  <rect width=\"100%%\" height=\"100%%\" fill=\"#f0f0f0\"/>\n" +
            "  <rect x=\"50\" y=\"50\" width=\"300\" height=\"200\" fill=\"#ffffff\" stroke=\"#cccccc\" stroke-width=\"2\"/>\n" +
            "  <text x=\"200\" y=\"120\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"24\" fill=\"#333333\">%s</text>\n" +
            "  <text x=\"200\" y=\"150\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"18\" fill=\"#666666\">%s</text>\n" +
            "  <text x=\"200\" y=\"180\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"16\" fill=\"#888888\">%s</text>\n" +
            "  <text x=\"200\" y=\"220\" text-anchor=\"middle\" font-family=\"Arial\" font-size=\"14\" fill=\"#999999\">Placeholder Image</text>\n" +
            "</svg>",
            entityType.toUpperCase(), entityId, imageType
        );
        
        return svg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * Determine content type based on file extension or image type
     */
    private String determineContentType(String path) {
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (path.endsWith(".png")) {
            return "image/png";
        } else if (path.endsWith(".gif")) {
            return "image/gif";
        } else if (path.endsWith(".svg")) {
            return "image/svg+xml";
        } else {
            // Default to SVG for placeholder images
            return "image/svg+xml";
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Image servlet typically only supports GET requests
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.setHeader("Allow", "GET");
        resp.getWriter().write("{\"error\":\"POST method not allowed for image requests\"}");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.setHeader("Allow", "GET");
        resp.getWriter().write("{\"error\":\"PUT method not allowed for image requests\"}");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        resp.setHeader("Allow", "GET");
        resp.getWriter().write("{\"error\":\"DELETE method not allowed for image requests\"}");
    }
}
