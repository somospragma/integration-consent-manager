package com.pragma.openfinance.consent.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AuthorizeConsentRequestDto {

    @NotBlank
    private String userId;

    @NotEmpty
    private List<String> accountIds;

    private String authenticationMethod;
}
