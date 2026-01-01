package org.openhab.binding.rachio.internal.api;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioSecurity} class provides cryptographic utilities for the Rachio binding,
 * specifically for validating webhook signatures.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioSecurity {

    private final Logger logger = LoggerFactory.getLogger(RachioSecurity.class);
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * Constructor for RachioSecurity.
     */
    public RachioSecurity() {
        logger.debug("RachioSecurity created.");
    }

    /**
     * Validates a Rachio webhook signature.
     *
     * @param payload the raw JSON payload from the webhook request body
     * @param signatureHeader the value of the 'X-Rachio-Signature' header (Base64 encoded)
     * @param webhookSecret the webhook secret configured for the bridge
     * @return true if the signature is valid, false otherwise
     */
    public boolean validateWebhookSignature(String payload, @Nullable String signatureHeader, String webhookSecret) {
        if (signatureHeader == null || signatureHeader.isEmpty()) {
            logger.error("Signature header is null or empty.");
            return false;
        }

        if (webhookSecret == null || webhookSecret.isEmpty()) {
            logger.error("Webhook secret is not configured.");
            return false;
        }

        try {
            // Calculate the expected HMAC
            String expectedSignature = calculateHmacSha256(payload, webhookSecret);

            // Compare the expected signature with the provided header
            // Use a constant-time comparison to avoid timing attacks
            return constantTimeEquals(expectedSignature, signatureHeader);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Cryptographic error during signature validation: {}", e.getMessage(), e);
            return false;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid Base64 encoding in signature header: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Calculates the HMAC SHA-256 signature of a message.
     *
     * @param message the message to sign
     * @param secret the secret key
     * @return the Base64-encoded HMAC SHA-256 signature
     * @throws NoSuchAlgorithmException if HmacSHA256 is not available
     * @throws InvalidKeyException if the secret key is invalid
     */
    private String calculateHmacSha256(String message, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    /**
     * Compares two strings in constant time to prevent timing attacks.
     *
     * @param a the first string
     * @param b the second string
     * @return true if the strings are equal, false otherwise
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Utility method to generate a webhook secret.
     * Can be used during bridge installation/configuration.
     *
     * @return a base64-encoded cryptographically random string suitable for use as a webhook secret
     */
    public String generateWebhookSecret() {
        // Generate 32 random bytes (256 bits) - a strong secret for HMAC-SHA256
        byte[] randomBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }
}
