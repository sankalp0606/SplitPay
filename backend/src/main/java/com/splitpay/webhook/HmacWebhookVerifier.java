package com.splitpay.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Component
public class HmacWebhookVerifier implements WebhookVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Override
    public boolean verifySignature(String rawPayload, Map<String, String> headers, String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("Webhook secret is not configured; skipping signature verification in development");
            return true;
        }

        String receivedSignature = headers.getOrDefault("X-Webhook-Signature",
                headers.getOrDefault("x-webhook-signature", ""));

        if (receivedSignature.isBlank()) {
            log.warn("Missing X-Webhook-Signature header in webhook request");
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);

            byte[] expectedHash = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(expectedHash);

            // Constant-time comparison to protect against timing attacks
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Failed to compute webhook HMAC signature: {}", e.getMessage());
            return false;
        }
    }
}
