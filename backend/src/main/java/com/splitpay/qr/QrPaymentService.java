package com.splitpay.qr;

import java.math.BigDecimal;

public interface QrPaymentService {

    QrPaymentResult generatePaymentRequest(
            String upiId,
            String recipientName,
            BigDecimal amount,
            String paymentReference,
            String note
    );

    byte[] generateQrPng(String qrPayload, int width, int height);

    void validateUpiId(String upiId);
}
