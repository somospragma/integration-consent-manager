package com.pragma.openfinance.consent.domain.exception;

import java.util.UUID;

public class ConsentNotFoundException extends RuntimeException {

    public ConsentNotFoundException(UUID consentId) {
        super("Consent not found: " + consentId);
    }
}
