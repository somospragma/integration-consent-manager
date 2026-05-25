package com.pragma.openfinance.consent.infrastructure.web.dto;

import com.pragma.openfinance.consent.domain.model.Consent;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ConsentResponseDto {

    private ConsentData data;

    @Data
    @Builder
    public static class ConsentData {
        private UUID consentId;
        private String type;
        private String status;
        private String tppId;
        private List<String> permissions;
        private Instant expiresAt;
        private Instant createdAt;
        private Instant updatedAt;
    }

    public static ConsentResponseDto from(Consent consent) {
        return ConsentResponseDto.builder()
                .data(ConsentData.builder()
                        .consentId(consent.getConsentId())
                        .type(consent.getType().name())
                        .status(consent.getStatus().name())
                        .tppId(consent.getTppId())
                        .permissions(consent.getPermissions())
                        .expiresAt(consent.getExpiresAt())
                        .createdAt(consent.getCreatedAt())
                        .updatedAt(consent.getUpdatedAt())
                        .build())
                .build();
    }
}
