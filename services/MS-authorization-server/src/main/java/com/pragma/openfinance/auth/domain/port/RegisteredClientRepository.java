package com.pragma.openfinance.auth.domain.port;

import com.pragma.openfinance.auth.domain.model.RegisteredClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegisteredClientRepository extends JpaRepository<RegisteredClient, String> {
    Optional<RegisteredClient> findByClientId(String clientId);
    Optional<RegisteredClient> findBySoftwareId(String softwareId);
    boolean existsByClientId(String clientId);
}
