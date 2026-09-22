package com.splitpay.dto.payment;

import com.splitpay.entity.SplittingStrategyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPlanPreviewResponse {
    private BigDecimal totalAmount;
    private int partCount;
    private List<BigDecimal> parts;
    private SplittingStrategyType splittingStrategy;
    private BigDecimal maxPartAmount;
}
