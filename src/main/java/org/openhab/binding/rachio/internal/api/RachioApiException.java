package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Exception for Rachio API errors
 *
 * @author David Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiException extends Exception {
    
    private final int statusCode;
    
    public RachioApiException(String message) {
        super(message);
        this.statusCode = 0;
    }
    
    public RachioApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }
    
    public RachioApiException(String message, int statusCode) {
        super(message + " (HTTP " + statusCode + ")");
        this.statusCode = statusCode;
    }
    
    public RachioApiException(String message, int statusCode, Throwable cause) {
        super(message + " (HTTP " + statusCode + ")", cause);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public boolean isRateLimitError() {
        return statusCode == 429;
    }
    
    public boolean isAuthError() {
        return statusCode == 401 || statusCode == 403;
    }
}
