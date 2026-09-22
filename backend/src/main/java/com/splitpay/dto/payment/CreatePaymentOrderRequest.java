package com.splitpay.dto.payment;

import com.splitpay.entity.SplittingStrategyType;
import jakarta.validation.constraints.*;
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
public class CreatePaymentOrderRequest {

    @NotBlank(message = "Recipient or merchant name is required")
    @Size(min = 2, max = 100, message = "Recipient name must be between 2 and 100 characters")
    private String recipientName;

    @NotBlank(message = "UPI ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$", message = "The provided UPI ID is invalid")
    private String upiId;

    @NotNull(message = "Total payment amount is required")
    @DecimalMin(value = "1.00", message = "Payment amount must be at least ₹1.00")
    @Digits(integer = 12, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal totalAmount;

    private SplittingStrategyType splittingStrategy;

    @DecimalMin(value = "1.00", message = "Max part amount must be at least ₹1.00")
    @Digits(integer = 12, fraction = 2, message = "Max part amount must have at most 2 decimal places")
    private BigDecimal maxPartAmount;

    @Size(max = 255, message = "Notes cannot exceed 255 characters")
    private String notes;

    private List<BigDecimal> customParts;
}
