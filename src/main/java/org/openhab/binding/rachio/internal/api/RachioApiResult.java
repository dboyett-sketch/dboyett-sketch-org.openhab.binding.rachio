package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Rachio API Result
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiResult {
    private final boolean success;
    private final String message;
    private final Object data;

    public RachioApiResult(boolean success, String message) {
        this(success, message, null);
    }

    public RachioApiResult(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}