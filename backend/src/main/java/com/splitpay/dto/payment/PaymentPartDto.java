package com.splitpay.dto.payment;

import com.splitpay.entity.PaymentPartStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPartDto {
    private UUID id;
    private UUID paymentOrderId;
    private int partNumber;
    private String paymentReference;
    private BigDecimal amount;
    private String currency;
    private PaymentPartStatus status;
    private String upiUri;
    private String qrDataUri;
    private Instant createdAt;
    private Instant updatedAt;
}
