package com.pragma.openfinance.consent.domain.exception;

import com.pragma.openfinance.consent.domain.model.ConsentStatus;

import java.util.UUID;

public class InvalidConsentStateException extends RuntimeException {

    public InvalidConsentStateException(UUID consentId, ConsentStatus currentStatus, ConsentStatus targetStatus) {
        super(String.format("Consent %s cannot transition from %s to %s",
                consentId, currentStatus, targetStatus));
    }
}
