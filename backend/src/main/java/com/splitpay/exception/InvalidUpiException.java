package com.splitpay.exception;

import org.springframework.http.HttpStatus;

public class InvalidUpiException extends AppException {
    public InvalidUpiException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_UPI_ID");
    }
}
