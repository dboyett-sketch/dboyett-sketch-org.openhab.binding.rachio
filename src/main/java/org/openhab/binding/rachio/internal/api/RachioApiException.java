package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Exception thrown for Rachio API errors
 * 
 * @author Dave Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private final int statusCode;
    private final String errorCode;
    
    public RachioApiException(String message) {
        super(message);
        this.statusCode = 0;
        this.errorCode = "UNKNOWN";
    }
    
    public RachioApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.errorCode = "UNKNOWN";
    }
    
    public RachioApiException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public RachioApiException(int statusCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    /**
     * Get HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Get Rachio error code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Check if this is an authentication error
     */
    public boolean isAuthenticationError() {
        return statusCode == 401 || "UNAUTHORIZED".equals(errorCode);
    }
    
    /**
     * Check if this is a rate limit error
     */
    public boolean isRateLimitError() {
        return statusCode == 429 || "RATE_LIMIT_EXCEEDED".equals(errorCode);
    }
    
    /**
     * Check if this is a not found error
     */
    public boolean isNotFoundError() {
        return statusCode == 404 || "NOT_FOUND".equals(errorCode);
    }
    
    /**
     * Get full error message with code
     */
    public String getFullMessage() {
        if (statusCode > 0 && !"UNKNOWN".equals(errorCode)) {
            return String.format("HTTP %d [%s]: %s", statusCode, errorCode, getMessage());
        }
        return getMessage();
    }
    
    @Override
    public String toString() {
        return getFullMessage();
    }
}
