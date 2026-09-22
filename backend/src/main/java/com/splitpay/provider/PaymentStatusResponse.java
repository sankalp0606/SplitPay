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
public class PaymentStatusResponse {
    private String providerTransactionId;
    private String paymentReference;
    private BigDecimal amount;
    private TransactionStatus status;
    private Instant settledAt;
    private String failureReason;
}
