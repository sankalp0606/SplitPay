package com.splitpay.qr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultQrPaymentService implements QrPaymentService {

    private final UpiPayloadBuilder upiPayloadBuilder;
    private final QrCodeGenerator qrCodeGenerator;

    @Override
    public QrPaymentResult generatePaymentRequest(
            String upiId,
            String recipientName,
            BigDecimal amount,
            String paymentReference,
            String note) {

        log.debug("Generating UPI payment request: [upiId={}, amount={}, ref={}]", upiId, amount, paymentReference);

        String upiUri = upiPayloadBuilder.buildUpiUri(upiId, recipientName, amount, paymentReference, note);
        String qrDataUri = qrCodeGenerator.generateQrDataUri(upiUri);

        return QrPaymentResult.builder()
                .paymentReference(paymentReference)
                .amount(amount)
                .upiUri(upiUri)
                .qrDataUri(qrDataUri)
                .build();
    }

    @Override
    public byte[] generateQrPng(String qrPayload, int width, int height) {
        return qrCodeGenerator.generateQrPngBytes(qrPayload, width, height);
    }

    @Override
    public void validateUpiId(String upiId) {
        upiPayloadBuilder.validateUpiId(upiId);
    }
}
