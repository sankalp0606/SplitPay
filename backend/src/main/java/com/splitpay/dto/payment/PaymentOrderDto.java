package com.splitpay.dto.payment;

import com.splitpay.entity.PaymentOrderStatus;
import com.splitpay.entity.SplittingStrategyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderDto {
    private UUID id;
    private String orderReference;
    private String recipientName;
    private String upiId;
    private BigDecimal totalAmount;
    private String currency;
    private PaymentOrderStatus status;
    private SplittingStrategyType splittingStrategy;
    private BigDecimal maxPartAmount;
    private String notes;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private int partCount;
    @Builder.Default
    private List<PaymentPartDto> parts = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
