package com.splitpay.service;

import com.splitpay.audit.AuditService;
import com.splitpay.entity.*;
import com.splitpay.exception.BadRequestException;
import com.splitpay.exception.ResourceNotFoundException;
import com.splitpay.provider.PaymentProviderService;
import com.splitpay.provider.WebhookVerificationResult;
import com.splitpay.repository.PaymentEventRepository;
import com.splitpay.repository.PaymentPartRepository;
import com.splitpay.repository.PaymentTransactionRepository;
import com.splitpay.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentEventRepository paymentEventRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentPartRepository paymentPartRepository;
    private final PaymentReconciliationService reconciliationService;
    private final PaymentProviderService paymentProviderService;
    private final WebhookVerifier webhookVerifier;
    private final AuditService auditService;

    @Value("${splitpay.provider.webhook-secret:}")
    private String webhookSecret;

    @Transactional
    public void processWebhook(String rawPayload, Map<String, String> headers, String clientIp) {
        log.info("Processing inbound webhook from IP: {}", clientIp);

        // 1. Signature Verification
        boolean isValidSignature = webhookVerifier.verifySignature(rawPayload, headers, webhookSecret);
        if (!isValidSignature) {
            log.warn("Webhook signature verification failed from IP: {}", clientIp);
            throw new BadRequestException("Invalid webhook signature", "INVALID_WEBHOOK_SIGNATURE");
        }

        // 2. Parse & Verify through active payment provider
        WebhookVerificationResult result = paymentProviderService.getActiveProvider().verifyWebhook(rawPayload, headers);
        if (!result.isValid()) {
            log.warn("Provider webhook validation rejected: {}", result.getFailureMessage());
            throw new BadRequestException(result.getFailureMessage(), "INVALID_WEBHOOK_PAYLOAD");
        }

        String eventId = result.getEventId();
        if (eventId == null || eventId.isBlank()) {
            throw new BadRequestException("Missing eventId in webhook payload", "MISSING_EVENT_ID");
        }

        // 3. Idempotency Check on Event
        if (paymentEventRepository.existsByEventId(eventId)) {
            log.warn("Duplicate webhook event received [eventId={}]. Discarding to ensure idempotency.", eventId);
            return; // Idempotent success: already recorded, do not credit again!
        }

        // 4. Persist Inbound Event
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .eventId(eventId)
                .eventType(result.getEventType() != null ? result.getEventType() : "PAYMENT.UPDATE")
                .providerName(paymentProviderService.getActiveProvider().getProviderName())
                .payload(rawPayload)
                .processed(false)
                .build();
        paymentEvent = paymentEventRepository.save(paymentEvent);

        // 5. Look up PaymentPart
        String paymentRef = result.getPaymentReference();
        if (paymentRef == null || paymentRef.isBlank()) {
            log.error("Webhook event {} missing paymentReference", eventId);
            paymentEvent.setErrorMessage("Missing paymentReference in event");
            paymentEventRepository.save(paymentEvent);
            throw new BadRequestException("Payment reference is missing in webhook", "MISSING_PAYMENT_REF");
        }

        PaymentPart part = paymentPartRepository.findByPaymentReference(paymentRef)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentPart", "paymentReference", paymentRef));

        // 6. Transaction Deduplication Check
        String providerTxnId = result.getProviderTransactionId();
        if (providerTxnId != null && paymentTransactionRepository.existsByPaymentReferenceAndProviderTransactionId(paymentRef, providerTxnId)) {
            log.warn("Transaction already exists for ref={} and txnId={}. Skipping duplicate credit.", paymentRef, providerTxnId);
            paymentEvent.setProcessed(true);
            paymentEvent.setProcessedAt(Instant.now());
            paymentEventRepository.save(paymentEvent);
            return;
        }

        // 7. Persist Authoritative Transaction Record
        PaymentTransaction transaction = PaymentTransaction.builder()
                .paymentPart(part)
                .paymentReference(paymentRef)
                .providerTransactionId(providerTxnId)
                .providerName(paymentProviderService.getActiveProvider().getProviderName())
                .amount(result.getAmount() != null ? result.getAmount() : part.getAmount())
                .currency(part.getCurrency())
                .status(result.getStatus())
                .rawPayload(rawPayload)
                .build();
        paymentTransactionRepository.save(transaction);

        // 8. Transition PaymentPart Status
        if (result.getStatus() == TransactionStatus.SUCCESS) {
            part.setStatus(PaymentPartStatus.SUCCESS);
            log.info("PaymentPart [{}] transitioned to SUCCESS via authoritative webhook", paymentRef);
        } else if (result.getStatus() == TransactionStatus.FAILED) {
            part.setStatus(PaymentPartStatus.FAILED);
            log.warn("PaymentPart [{}] transitioned to FAILED via authoritative webhook", paymentRef);
        }
        paymentPartRepository.save(part);

        // 9. Reconcile PaymentOrder
        PaymentOrder order = part.getPaymentOrder();
        if (order != null) {
            reconciliationService.reconcileOrder(order);
        }

        // 10. Audit Logging
        auditService.recordAction(
                order != null && order.getUser() != null ? order.getUser().getId() : null,
                "PAYMENT_WEBHOOK_PROCESSED",
                "PaymentPart",
                part.getId().toString(),
                clientIp,
                String.format("Ref: %s, Txn: %s, Status: %s, Amount: %s", paymentRef, providerTxnId, result.getStatus(), result.getAmount())
        );

        // 11. Mark Event Processed
        paymentEvent.setProcessed(true);
        paymentEvent.setProcessedAt(Instant.now());
        paymentEventRepository.save(paymentEvent);

        log.info("Webhook event [{}] successfully processed and reconciled", eventId);
    }
}
