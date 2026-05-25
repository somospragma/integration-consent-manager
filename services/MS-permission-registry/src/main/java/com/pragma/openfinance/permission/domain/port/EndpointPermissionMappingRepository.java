package com.pragma.openfinance.permission.domain.port;

import com.pragma.openfinance.permission.domain.model.EndpointPermissionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EndpointPermissionMappingRepository extends JpaRepository<EndpointPermissionMapping, UUID> {

    Optional<EndpointPermissionMapping> findByHttpMethodAndEndpointPattern(
            String httpMethod, String endpointPattern);
}
