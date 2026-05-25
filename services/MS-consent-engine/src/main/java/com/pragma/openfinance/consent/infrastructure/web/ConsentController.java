package com.pragma.openfinance.consent.infrastructure.web;

import com.pragma.openfinance.consent.domain.model.Consent;
import com.pragma.openfinance.consent.domain.model.ConsentEvent.ActorType;
import com.pragma.openfinance.consent.domain.model.ConsentType;
import com.pragma.openfinance.consent.domain.service.ConsentService;
import com.pragma.openfinance.consent.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping
    public ResponseEntity<ConsentResponseDto> createConsent(
            @Valid @RequestBody CreateConsentRequestDto request,
            @RequestHeader("X-Fapi-Interaction-Id") String interactionId,
            @RequestHeader("X-Tpp-Id") String tppId) {

        Consent consent = consentService.createConsent(
                ConsentType.valueOf(request.getData().getType()),
                tppId,
                request.getData().getPermissions(),
                request.getData().getExpiresAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Fapi-Interaction-Id", interactionId)
                .body(ConsentResponseDto.from(consent));
    }

    @GetMapping("/{consentId}")
    public ResponseEntity<ConsentResponseDto> getConsent(
            @PathVariable UUID consentId,
            @RequestHeader("X-Fapi-Interaction-Id") String interactionId) {

        Consent consent = consentService.getConsent(consentId);

        return ResponseEntity.ok()
                .header("X-Fapi-Interaction-Id", interactionId)
                .body(ConsentResponseDto.from(consent));
    }

    @DeleteMapping("/{consentId}")
    public ResponseEntity<Void> revokeConsent(
            @PathVariable UUID consentId,
            @RequestHeader("X-Fapi-Interaction-Id") String interactionId,
            @RequestHeader("X-Tpp-Id") String tppId) {

        consentService.revokeConsent(consentId, tppId, ActorType.TPP);

        return ResponseEntity.noContent()
                .header("X-Fapi-Interaction-Id", interactionId)
                .build();
    }

    @PostMapping("/{consentId}/authorize")
    public ResponseEntity<ConsentResponseDto> authorizeConsent(
            @PathVariable UUID consentId,
            @Valid @RequestBody AuthorizeConsentRequestDto request,
            @RequestHeader("X-Correlation-Id") String correlationId) {

        Consent consent = consentService.authorizeConsent(
                consentId,
                request.getUserId(),
                request.getAccountIds(),
                request.getAuthenticationMethod()
        );

        return ResponseEntity.ok(ConsentResponseDto.from(consent));
    }

    @PostMapping("/{consentId}/reject")
    public ResponseEntity<ConsentResponseDto> rejectConsent(
            @PathVariable UUID consentId,
            @RequestBody(required = false) RejectConsentRequestDto request,
            @RequestHeader("X-Correlation-Id") String correlationId) {

        String reason = request != null ? request.getReason() : "USER_REJECTED";
        Consent consent = consentService.rejectConsent(consentId, reason);

        return ResponseEntity.ok(ConsentResponseDto.from(consent));
    }

    @GetMapping("/{consentId}/active")
    public ResponseEntity<ConsentActiveResponseDto> checkActive(
            @PathVariable UUID consentId,
            @RequestParam(required = false) String requiredPermission) {

        boolean active = consentService.isConsentActive(consentId);
        boolean hasPermission = requiredPermission != null
                ? consentService.hasPermission(consentId, requiredPermission)
                : true;

        Consent consent = consentService.getConsent(consentId);

        return ResponseEntity.ok(ConsentActiveResponseDto.builder()
                .active(active)
                .consentId(consentId)
                .permissions(consent.getPermissions())
                .hasPermission(hasPermission)
                .expiresAt(consent.getExpiresAt())
                .build());
    }

    @PostMapping("/{consentId}/consume")
    public ResponseEntity<ConsentResponseDto> consumeConsent(
            @PathVariable UUID consentId,
            @RequestHeader("X-Correlation-Id") String correlationId) {

        Consent consent = consentService.consumeConsent(consentId);
        return ResponseEntity.ok(ConsentResponseDto.from(consent));
    }
}
