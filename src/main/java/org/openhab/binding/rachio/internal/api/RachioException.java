package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Custom exception for Rachio API errors
 */
@NonNullByDefault
public class RachioException extends Exception {
    private final Integer statusCode;
    
    public RachioException(String message) {
        super(message);
        this.statusCode = null;
    }
    
    public RachioException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public RachioException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }
    
    public RachioException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    @Nullable
    public Integer getStatusCode() {
        return statusCode;
    }
}