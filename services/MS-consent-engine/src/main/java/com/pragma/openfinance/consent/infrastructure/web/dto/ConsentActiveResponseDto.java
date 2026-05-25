package com.pragma.openfinance.consent.infrastructure.web.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ConsentActiveResponseDto {
    private boolean active;
    private UUID consentId;
    private List<String> permissions;
    private boolean hasPermission;
    private Instant expiresAt;
}
