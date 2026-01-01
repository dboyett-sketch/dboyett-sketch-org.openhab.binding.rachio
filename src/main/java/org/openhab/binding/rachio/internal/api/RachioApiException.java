package org.openhab.binding.rachio.internal.api;

/**
 * Exception class for Rachio API errors
 * Includes HTTP status codes and detailed error information
 * 
 * @author dboyett-sketch
 */
public class RachioApiException extends Exception {

    // ===== CRITICAL PATCH: Added missing serialVersionUID =====
    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String apiErrorCode;
    private final String requestUrl;
    private final String responseBody;

    /**
     * Constructor with message
     */
    public RachioApiException(String message) {
        super(message);
        this.statusCode = 0;
        this.apiErrorCode = null;
        this.requestUrl = null;
        this.responseBody = null;
    }

    /**
     * Constructor with message and cause
     */
    public RachioApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.apiErrorCode = null;
        this.requestUrl = null;
        this.responseBody = null;
    }

    /**
     * Constructor with HTTP status code
     */
    public RachioApiException(String message, int statusCode) {
        super(message + " (HTTP " + statusCode + ")");
        this.statusCode = statusCode;
        this.apiErrorCode = null;
        this.requestUrl = null;
        this.responseBody = null;
    }

    /**
     * Constructor with HTTP status code and cause
     */
    public RachioApiException(String message, int statusCode, Throwable cause) {
        super(message + " (HTTP " + statusCode + ")", cause);
        this.statusCode = statusCode;
        this.apiErrorCode = null;
        this.requestUrl = null;
        this.responseBody = null;
    }

    /**
     * Constructor with full details
     */
    public RachioApiException(String message, int statusCode, String apiErrorCode, String requestUrl,
            String responseBody) {
        super(buildMessage(message, statusCode, apiErrorCode, requestUrl, responseBody));
        this.statusCode = statusCode;
        this.apiErrorCode = apiErrorCode;
        this.requestUrl = requestUrl;
        this.responseBody = responseBody;
    }

    /**
     * Constructor with full details and cause
     */
    public RachioApiException(String message, int statusCode, String apiErrorCode, String requestUrl,
            String responseBody, Throwable cause) {
        super(buildMessage(message, statusCode, apiErrorCode, requestUrl, responseBody), cause);
        this.statusCode = statusCode;
        this.apiErrorCode = apiErrorCode;
        this.requestUrl = requestUrl;
        this.responseBody = responseBody;
    }

    /**
     * Build detailed error message
     */
    private static String buildMessage(String message, int statusCode, String apiErrorCode, String requestUrl,
            String responseBody) {
        StringBuilder sb = new StringBuilder();
        sb.append(message);

        if (statusCode > 0) {
            sb.append(" (HTTP ").append(statusCode).append(")");
        }

        if (apiErrorCode != null && !apiErrorCode.isEmpty()) {
            sb.append(" [").append(apiErrorCode).append("]");
        }

        if (requestUrl != null && !requestUrl.isEmpty()) {
            sb.append(" - URL: ").append(requestUrl);
        }

        if (responseBody != null && !responseBody.isEmpty()) {
            // Truncate long response bodies for readability
            String truncatedBody = responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
            sb.append(" - Response: ").append(truncatedBody);
        }

        return sb.toString();
    }

    /**
     * Get HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Get API-specific error code
     */
    public String getApiErrorCode() {
        return apiErrorCode;
    }

    /**
     * Get the request URL that caused the error
     */
    public String getRequestUrl() {
        return requestUrl;
    }

    /**
     * Get the response body (if any)
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Check if this is an authentication error (401)
     */
    public boolean isAuthenticationError() {
        return statusCode == 401;
    }

    /**
     * Check if this is an authorization error (403)
     */
    public boolean isAuthorizationError() {
        return statusCode == 403;
    }

    /**
     * Check if this is a not found error (404)
     */
    public boolean isNotFoundError() {
        return statusCode == 404;
    }

    /**
     * Check if this is a rate limit error (429)
     */
    public boolean isRateLimitError() {
        return statusCode == 429;
    }

    /**
     * Check if this is a server error (5xx)
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * Check if this is a client error (4xx)
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Check if this is a network/connection error
     */
    public boolean isNetworkError() {
        return statusCode == 0 && getCause() != null;
    }

    /**
     * Check if this error should trigger a retry
     */
    public boolean shouldRetry() {
        // Retry on server errors, rate limits, and network errors
        return isServerError() || isRateLimitError() || isNetworkError();
    }

    /**
     * Get suggested retry delay in milliseconds
     */
    public long getSuggestedRetryDelay() {
        if (isRateLimitError()) {
            return 60000; // 1 minute for rate limits
        } else if (isServerError()) {
            return 5000; // 5 seconds for server errors
        } else if (isNetworkError()) {
            return 2000; // 2 seconds for network errors
        }
        return 0; // No retry suggested
    }

    /**
     * Create a user-friendly error message
     */
    public String getUserFriendlyMessage() {
        if (isAuthenticationError()) {
            return "Authentication failed. Please check your API key.";
        } else if (isAuthorizationError()) {
            return "Access denied. You don't have permission to perform this action.";
        } else if (isNotFoundError()) {
            return "Resource not found. The device or zone may have been removed.";
        } else if (isRateLimitError()) {
            return "Rate limit exceeded. Please wait a minute before trying again.";
        } else if (isServerError()) {
            return "Rachio server error. Please try again later.";
        } else if (isNetworkError()) {
            return "Network connection failed. Please check your internet connection.";
        } else if (isClientError()) {
            return "Invalid request. Please check your configuration.";
        } else {
            return getMessage();
        }
    }

    /**
     * Get error severity level
     */
    public ErrorSeverity getSeverity() {
        if (isAuthenticationError() || isAuthorizationError()) {
            return ErrorSeverity.CRITICAL;
        } else if (isServerError() || isNetworkError()) {
            return ErrorSeverity.HIGH;
        } else if (isRateLimitError()) {
            return ErrorSeverity.MEDIUM;
        } else if (isClientError()) {
            return ErrorSeverity.LOW;
        } else {
            return ErrorSeverity.UNKNOWN;
        }
    }

    /**
     * Create a simplified version for logging (without full response body)
     */
    public String toLogString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RachioApiException: ").append(getMessage());

        if (statusCode > 0) {
            sb.append(" [Status: ").append(statusCode).append("]");
        }

        if (apiErrorCode != null) {
            sb.append(" [Code: ").append(apiErrorCode).append("]");
        }

        if (requestUrl != null) {
            // Just show the endpoint, not full URL
            String endpoint = extractEndpoint(requestUrl);
            sb.append(" [Endpoint: ").append(endpoint).append("]");
        }

        return sb.toString();
    }

    /**
     * Extract endpoint from full URL
     */
    private String extractEndpoint(String url) {
        if (url == null)
            return "";

        // Remove protocol and domain
        String endpoint = url.replaceFirst("^https?://[^/]+", "");

        // Truncate if too long
        if (endpoint.length() > 50) {
            endpoint = endpoint.substring(0, 47) + "...";
        }

        return endpoint;
    }

    @Override
    public String toString() {
        return toLogString();
    }

    /**
     * Error severity levels
     */
    public enum ErrorSeverity {
        CRITICAL, // Authentication/authorization failures
        HIGH, // Server errors, network issues
        MEDIUM, // Rate limits, temporary issues
        LOW, // Client errors, validation issues
        UNKNOWN // Other errors
    }

    /**
     * Factory method for common error types
     */
    public static RachioApiException authenticationError(String message, String requestUrl) {
        return new RachioApiException(message, 401, "AUTH_FAILED", requestUrl, null);
    }

    public static RachioApiException notFoundError(String message, String requestUrl) {
        return new RachioApiException(message, 404, "NOT_FOUND", requestUrl, null);
    }

    public static RachioApiException rateLimitError(String message, String requestUrl, String responseBody) {
        return new RachioApiException(message, 429, "RATE_LIMIT", requestUrl, responseBody);
    }

    public static RachioApiException serverError(String message, String requestUrl, String responseBody) {
        return new RachioApiException(message, 500, "SERVER_ERROR", requestUrl, responseBody);
    }

    public static RachioApiException networkError(String message, Throwable cause) {
        return new RachioApiException(message, 0, "NETWORK_ERROR", null, null, cause);
    }

    public static RachioApiException validationError(String message, String apiErrorCode) {
        return new RachioApiException(message, 400, apiErrorCode, null, null);
    }
}
