package com.pragma.openfinance.consent.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentMetadata {
    private String tppName;
    private String tppLogoUrl;
    private List<String> accountIds;
    private String authenticationMethod;
    private Map<String, String> additionalData;
}
