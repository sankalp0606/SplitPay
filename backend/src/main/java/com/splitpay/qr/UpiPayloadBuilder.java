package com.splitpay.qr;

import com.splitpay.exception.InvalidUpiException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Component
public class UpiPayloadBuilder {

    private static final Pattern UPI_ID_PATTERN =
            Pattern.compile("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$");

    public void validateUpiId(String upiId) {
        if (upiId == null || !UPI_ID_PATTERN.matcher(upiId.trim()).matches()) {
            throw new InvalidUpiException(String.format("The provided UPI ID '%s' is invalid", upiId));
        }
    }

    /**
     * Constructs an NPCI-compliant UPI deep-link URI.
     * Format: upi://pay?pa={upiId}&pn={payeeName}&am={amount}&cu=INR&tr={ref}&tn={note}
     */
    public String buildUpiUri(String upiId, String payeeName, BigDecimal amount, String paymentReference, String note) {
        validateUpiId(upiId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be strictly positive");
        }
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new IllegalArgumentException("Payment reference must not be blank");
        }

        String safePayeeName = payeeName != null && !payeeName.isBlank() ? payeeName.trim() : "Merchant";
        String encodedPayeeName = urlEncode(safePayeeName);

        String safeNote = note != null && !note.isBlank() ? note.trim() : "SplitPay " + paymentReference;
        String encodedNote = urlEncode(safeNote);

        String formattedAmount = String.format("%.2f", amount);

        return String.format(
                "upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tr=%s&tn=%s",
                upiId.trim(),
                encodedPayeeName,
                formattedAmount,
                paymentReference.trim(),
                encodedNote
        );
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
