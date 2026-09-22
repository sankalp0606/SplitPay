package com.splitpay.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitpay.entity.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ====================================================================
 * TEST-ONLY MOCK PAYMENT PROVIDER
 * ====================================================================
 * ATTENTION:
 * This implementation is strictly for automated testing and local sandbox verification.
 * It DOES NOT connect to any real banking institution, NPCI, or authorized payment aggregator.
 *
 * In production environments:
 * 1. Implement PaymentProvider for an authorized entity (e.g. Razorpay, Cashfree, PayU, PhonePe PG).
 * 2. Set 'splitpay.provider.active-provider: authorized_provider_name' in production config.
 * 3. Never deploy with MockPaymentProvider as the authoritative provider.
 * ====================================================================
 */
@Slf4j
@Component("mockPaymentProvider")
@RequiredArgsConstructor
public class MockPaymentProvider implements PaymentProvider {

    private final ObjectMapper objectMapper;

    @Override
    public String getProviderName() {
        return "MOCK_DEVELOPMENT_PROVIDER";
    }

    @Override
    public ProviderPaymentResponse createPaymentRequest(ProviderPaymentRequest request) {
        log.info("[MOCK PROVIDER] Simulating payment request creation for reference: {}", request.getPaymentReference());
        return ProviderPaymentResponse.builder()
                .providerTransactionId("MOCK-TXN-" + UUID.randomUUID())
                .paymentReference(request.getPaymentReference())
                .status(TransactionStatus.PENDING)
                .upiPayload(String.format("upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tr=%s",
                        request.getPayeeVpa(), request.getPayeeName(), request.getAmount(), request.getPaymentReference()))
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public PaymentStatusResponse checkPaymentStatus(String paymentReference) {
        log.info("[MOCK PROVIDER] Simulating status lookup for reference: {}", paymentReference);
        return PaymentStatusResponse.builder()
                .paymentReference(paymentReference)
                .providerTransactionId("MOCK-TXN-" + UUID.randomUUID())
                .status(TransactionStatus.PENDING)
                .build();
    }

    @Override
    public WebhookVerificationResult verifyWebhook(String rawPayload, Map<String, String> headers) {
        log.info("[MOCK PROVIDER] Verifying simulated webhook payload");
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventId = root.has("eventId") ? root.get("eventId").asText() : "EVT-" + UUID.randomUUID();
            String eventType = root.has("eventType") ? root.get("eventType").asText() : "PAYMENT.SUCCESS";
            String paymentRef = root.has("paymentReference") ? root.get("paymentReference").asText() : "";
            String txnId = root.has("providerTransactionId") ? root.get("providerTransactionId").asText() : "MOCK-TXN-" + UUID.randomUUID();
            BigDecimal amount = root.has("amount") ? new BigDecimal(root.get("amount").asText()) : BigDecimal.ZERO;
            String statusStr = root.has("status") ? root.get("status").asText() : "SUCCESS";
            TransactionStatus status = "SUCCESS".equalsIgnoreCase(statusStr) ? TransactionStatus.SUCCESS : TransactionStatus.FAILED;

            return WebhookVerificationResult.builder()
                    .valid(true)
                    .eventId(eventId)
                    .eventType(eventType)
                    .paymentReference(paymentRef)
                    .providerTransactionId(txnId)
                    .amount(amount)
                    .status(status)
                    .build();
        } catch (Exception e) {
            log.warn("[MOCK PROVIDER] Failed to parse webhook payload: {}", e.getMessage());
            return WebhookVerificationResult.builder()
                    .valid(false)
                    .failureMessage("Invalid mock payload format: " + e.getMessage())
                    .build();
        }
    }
}
