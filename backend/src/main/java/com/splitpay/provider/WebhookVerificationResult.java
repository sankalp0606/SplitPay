package com.splitpay.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookVerificationResult {
    private boolean valid;
    private String eventId;
    private String eventType;
    private String paymentReference;
    private String providerTransactionId;
    private java.math.BigDecimal amount;
    private com.splitpay.entity.TransactionStatus status;
    private String failureMessage;
}
