package com.splitpay.qr;

import com.splitpay.exception.InvalidUpiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrPaymentServiceTest {

    private DefaultQrPaymentService qrPaymentService;

    @BeforeEach
    void setUp() {
        UpiPayloadBuilder upiPayloadBuilder = new UpiPayloadBuilder();
        QrCodeGenerator qrCodeGenerator = new QrCodeGenerator();
        qrPaymentService = new DefaultQrPaymentService(upiPayloadBuilder, qrCodeGenerator);
    }

    @Test
    @DisplayName("Generates valid NPCI compliant UPI URI and QR Data URL")
    void generatePaymentRequest_ValidInputs_Success() {
        QrPaymentResult result = qrPaymentService.generatePaymentRequest(
                "abcelectronics@upi",
                "ABC Electronics",
                new BigDecimal("1990.00"),
                "SP-ORD-123-P1-456",
                "SplitPay Order Part 1"
        );

        assertThat(result).isNotNull();
        assertThat(result.getPaymentReference()).isEqualTo("SP-ORD-123-P1-456");
        assertThat(result.getAmount()).isEqualByComparingTo("1990.00");

        // Verify UPI URI scheme and query parameters
        assertThat(result.getUpiUri())
                .startsWith("upi://pay?")
                .contains("pa=abcelectronics@upi")
                .contains("pn=ABC%20Electronics")
                .contains("am=1990.00")
                .contains("cu=INR")
                .contains("tr=SP-ORD-123-P1-456");

        // Verify QR Data URI starts with base64 PNG prefix
        assertThat(result.getQrDataUri()).startsWith("data:image/png;base64,");

        // Verify binary PNG bytes can be generated
        byte[] pngBytes = qrPaymentService.generateQrPng(result.getUpiUri(), 200, 200);
        assertThat(pngBytes).isNotEmpty();
        // PNG magic bytes: 0x89, 'P', 'N', 'G'
        assertThat(pngBytes[0]).isEqualTo((byte) 0x89);
        assertThat(pngBytes[1]).isEqualTo((byte) 'P');
        assertThat(pngBytes[2]).isEqualTo((byte) 'N');
        assertThat(pngBytes[3]).isEqualTo((byte) 'G');
    }

    @ParameterizedTest
    @ValueSource(strings = {"merchant@okaxis", "store.name@icici", "payee-123@hdfcbank", "john_doe@upi"})
    @DisplayName("Validates standard UPI IDs successfully")
    void validUpiIds_PassValidation(String validUpiId) {
        qrPaymentService.validateUpiId(validUpiId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-no-at-sign", "@nohandle", "spaces in upi@bank", "", "bad@domain@double"})
    @DisplayName("Rejects invalid UPI IDs with InvalidUpiException")
    void invalidUpiIds_ThrowException(String invalidUpiId) {
        assertThatThrownBy(() -> qrPaymentService.validateUpiId(invalidUpiId))
                .isInstanceOf(InvalidUpiException.class);
    }
}
