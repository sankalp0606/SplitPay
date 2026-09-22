package com.splitpay.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPaymentRequest {
    private String paymentReference;
    private BigDecimal amount;
    private String currency;
    private String payeeVpa;
    private String payeeName;
    private String note;
}
