package com.splitpay.controller;

import com.splitpay.dto.payment.PaymentPartDto;
import com.splitpay.qr.QrPaymentService;
import com.splitpay.service.PaymentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payment-parts")
@RequiredArgsConstructor
@Tag(name = "Payment Parts", description = "Individual UPI payment part inspection and QR rendering")
public class PaymentPartController {

    private final PaymentOrderService paymentOrderService;
    private final QrPaymentService qrPaymentService;

    @GetMapping("/{id}")
    @Operation(summary = "Get payment part details and UPI payload")
    public ResponseEntity<PaymentPartDto> getPartById(@PathVariable UUID id) {
        PaymentPartDto part = paymentOrderService.getPartById(id);
        return ResponseEntity.ok(part);
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Download or stream the UPI QR code PNG image for a payment part")
    public ResponseEntity<byte[]> getPartQrImage(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "300") int height) {
        PaymentPartDto part = paymentOrderService.getPartById(id);
        byte[] pngBytes = qrPaymentService.generateQrPng(part.getUpiUri(), width, height);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(pngBytes);
    }
}
