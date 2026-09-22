package com.splitpay.qr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrPaymentResult {
    private String paymentReference;
    private BigDecimal amount;
    private String upiUri;
    private String qrDataUri;
}
