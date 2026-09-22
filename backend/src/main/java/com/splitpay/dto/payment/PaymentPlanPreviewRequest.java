package com.splitpay.dto.payment;

import com.splitpay.entity.SplittingStrategyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class PaymentPlanPreviewRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least ₹1.00")
    private BigDecimal totalAmount;

    private SplittingStrategyType splittingStrategy;

    @DecimalMin(value = "1.00", message = "Max part amount must be at least ₹1.00")
    private BigDecimal maxPartAmount;

    private List<BigDecimal> customParts;
}
