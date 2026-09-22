package com.splitpay.provider;

import com.splitpay.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPaymentResponse {
    private String providerTransactionId;
    private String paymentReference;
    private TransactionStatus status;
    private String upiPayload;
    private Instant createdAt;
}
