package com.pragma.openfinance.consent.domain.model;

public enum ConsentStatus {
    CREATED,
    AWAITING_AUTHORIZATION,
    AUTHORIZED,
    REJECTED,
    REVOKED,
    EXPIRED,
    CONSUMED
}
