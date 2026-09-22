package com.splitpay.webhook;

import java.util.Map;

public interface WebhookVerifier {

    boolean verifySignature(String rawPayload, Map<String, String> headers, String secret);
}
