package com.pragma.openfinance.consent.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreateConsentRequestDto {

    @NotNull
    @Valid
    private ConsentData data;

    @Data
    public static class ConsentData {
        @NotNull
        private String type;

        @NotNull
        private List<String> permissions;

        private Instant expiresAt;

        private Instant transactionFromDateTime;

        private Instant transactionToDateTime;
    }
}
