package com.pragma.openfinance.permission.domain.service;

import com.pragma.openfinance.permission.domain.model.EndpointPermissionMapping;
import com.pragma.openfinance.permission.domain.model.Permission;
import com.pragma.openfinance.permission.domain.port.EndpointPermissionMappingRepository;
import com.pragma.openfinance.permission.domain.port.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final EndpointPermissionMappingRepository mappingRepository;

    @Cacheable(value = "permissions", key = "'all'")
    public List<Permission> getAllPermissions() {
        return permissionRepository.findByActiveTrue();
    }

    @Cacheable(value = "permissions", key = "#consentType")
    public List<Permission> getPermissionsByType(String consentType) {
        return permissionRepository.findByConsentTypeAndActiveTrue(consentType);
    }

    /**
     * Valida si un permiso es suficiente para acceder a un endpoint.
     * Usado por el API Gateway en cada request.
     */
    @Cacheable(value = "endpoint-mappings", key = "#httpMethod + ':' + #endpointPattern")
    public Optional<String> getRequiredPermission(String httpMethod, String endpointPattern) {
        return mappingRepository.findByHttpMethodAndEndpointPattern(httpMethod, endpointPattern)
                .map(EndpointPermissionMapping::getRequiredPermission);
    }

    public boolean validatePermission(String permissionCode, String httpMethod, String endpoint) {
        Optional<String> required = getRequiredPermission(httpMethod, normalizeEndpoint(endpoint));
        if (required.isEmpty()) {
            log.warn("No permission mapping found for {} {}", httpMethod, endpoint);
            return false;
        }
        return required.get().equals(permissionCode);
    }

    /**
     * Normaliza un endpoint concreto a su patrón.
     * Ej: /accounts/12345/balances → /accounts/{id}/balances
     */
    private String normalizeEndpoint(String endpoint) {
        return endpoint.replaceAll("/[0-9a-fA-F-]{36}", "/{id}")
                       .replaceAll("/\\d+", "/{id}");
    }
}
