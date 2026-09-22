package com.splitpay.exception;

import org.springframework.http.HttpStatus;

public class PaymentValidationException extends AppException {
    public PaymentValidationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_VALIDATION_ERROR");
    }
}
