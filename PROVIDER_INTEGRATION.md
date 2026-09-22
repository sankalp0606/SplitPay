# SPLITPAY: Payment Provider Integration Guide

This guide describes how to connect an authorized payment gateway, bank aggregator, or NPCI UPI partner to SPLITPAY for commercial production deployment.

---

## 1. Provider Abstraction Architecture

All external banking rails and payment gateways are isolated behind the `PaymentProvider` interface in:
`backend/src/main/java/com/splitpay/provider/PaymentProvider.java`

```java
public interface PaymentProvider {
    String getProviderName();
    ProviderPaymentResponse createPaymentRequest(ProviderPaymentRequest request);
    PaymentStatusResponse checkPaymentStatus(String paymentReference);
    WebhookVerificationResult verifyWebhook(String rawPayload, Map<String, String> headers);
}
```

Currently, the application includes a sandbox implementation:
`backend/src/main/java/com/splitpay/provider/MockPaymentProvider.java`

---

## 2. Steps to Connect an Authorized Gateway

To connect a commercial gateway (e.g., Razorpay, Cashfree, PayU, PhonePe Payment Gateway, or ICICI Eazypay):

### Step 1: Create the Concrete Implementation
Create a new class implementing `PaymentProvider` in `com.splitpay.provider`:

```java
package com.splitpay.provider;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component("razorpayPaymentProvider")
public class RazorpayPaymentProvider implements PaymentProvider {

    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }

    @Override
    public ProviderPaymentResponse createPaymentRequest(ProviderPaymentRequest request) {
        // Call gateway API (e.g. /v1/orders or /v1/payments/create_upi_qr)
        // Map response to ProviderPaymentResponse
    }

    @Override
    public PaymentStatusResponse checkPaymentStatus(String paymentReference) {
        // Poll gateway status endpoint for authoritative state
    }

    @Override
    public WebhookVerificationResult verifyWebhook(String rawPayload, Map<String, String> headers) {
        // Verify gateway signature (e.g., X-Razorpay-Signature HMAC-SHA256)
        // Extract payment reference, transaction ID, settled amount, and status
    }
}
```

### Step 2: Configure Environment Variables
Set the gateway credentials in `.env`:

```env
# Active Payment Provider Name
ACTIVE_PAYMENT_PROVIDER=RAZORPAY

# Gateway API Credentials
GATEWAY_API_KEY=rzp_live_your_key_here
GATEWAY_API_SECRET=your_api_secret_here
GATEWAY_WEBHOOK_SECRET=your_webhook_signing_secret_here
```

### Step 3: Register Webhook Endpoint at Gateway Dashboard
Configure the gateway webhook URL to deliver real-time settlement notifications:
```
https://api.yourdomain.com/api/payments/webhook
```
Subscribe to:
- `payment.captured` / `payment.success`
- `payment.failed`

---

## 3. Production Launch Checklist for Real Money Settlement

- [ ] Obtain Merchant Account and API credentials from RBI-authorized payment aggregator (PA/PG).
- [ ] Implement `PaymentProvider` for the authorized aggregator.
- [ ] Configure `ACTIVE_PAYMENT_PROVIDER` to match your registered provider bean name.
- [ ] Set `SPLITPAY_PROVIDER_WEBHOOK_SECRET` with the provider's production secret.
- [ ] Ensure HTTPS with TLS 1.3 is enforced on the webhook endpoint.
- [ ] Verify that mock provider is disabled in production environment (`SPRING_PROFILES_ACTIVE=prod`).
- [ ] Test end-to-end sandbox settlement with ₹1 real transaction before going live.
