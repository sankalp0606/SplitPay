package com.splitpay.provider;

import java.util.Map;

/**
 * Payment Provider Abstraction.
 * Isolates real UPI payment gateway/aggregator interactions behind a clean interface.
 * Real bank/gateway integrations (e.g. Razorpay, Cashfree, PayU, PhonePe PG) must implement this interface.
 */
public interface PaymentProvider {

    String getProviderName();

    ProviderPaymentResponse createPaymentRequest(ProviderPaymentRequest request);

    PaymentStatusResponse checkPaymentStatus(String paymentReference);

    WebhookVerificationResult verifyWebhook(String rawPayload, Map<String, String> headers);
}
