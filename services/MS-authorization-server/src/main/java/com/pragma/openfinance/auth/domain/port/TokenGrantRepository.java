package com.pragma.openfinance.auth.domain.port;

import com.pragma.openfinance.auth.domain.model.TokenGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenGrantRepository extends JpaRepository<TokenGrant, String> {
    Optional<TokenGrant> findByAuthorizationCodeValue(String code);
    Optional<TokenGrant> findByRefreshTokenValue(String refreshToken);
    Optional<TokenGrant> findByAccessTokenValue(String accessToken);
    List<TokenGrant> findByConsentId(String consentId);
    List<TokenGrant> findByRegisteredClientIdAndPrincipalName(String clientId, String principal);
}
