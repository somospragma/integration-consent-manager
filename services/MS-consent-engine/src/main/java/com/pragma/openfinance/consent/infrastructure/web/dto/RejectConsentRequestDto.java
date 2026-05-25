package com.pragma.openfinance.consent.infrastructure.web.dto;

import lombok.Data;

@Data
public class RejectConsentRequestDto {
    private String reason;
    private String description;
}
