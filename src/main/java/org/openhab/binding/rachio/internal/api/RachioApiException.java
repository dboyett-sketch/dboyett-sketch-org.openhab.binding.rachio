package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Rachio API Exception
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApiException extends Exception {
    private static final long serialVersionUID = 1L;

    public RachioApiException(String message) {
        super(message);
    }

    public RachioApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public RachioApiException(Throwable cause) {
        super(cause);
    }
}