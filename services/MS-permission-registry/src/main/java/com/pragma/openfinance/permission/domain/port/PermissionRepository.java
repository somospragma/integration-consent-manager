package com.pragma.openfinance.permission.domain.port;

import com.pragma.openfinance.permission.domain.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    List<Permission> findByActiveTrue();

    List<Permission> findByConsentTypeAndActiveTrue(String consentType);
}
