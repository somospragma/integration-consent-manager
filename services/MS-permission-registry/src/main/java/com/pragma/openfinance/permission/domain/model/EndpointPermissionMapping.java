package com.pragma.openfinance.permission.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "endpoint_permission_mappings", indexes = {
    @Index(name = "idx_epm_endpoint", columnList = "httpMethod, endpointPattern")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointPermissionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID mappingId;

    @Column(nullable = false, length = 10)
    private String httpMethod;

    @Column(nullable = false, length = 200)
    private String endpointPattern;

    @Column(nullable = false, length = 50)
    private String requiredPermission;

    @Column(length = 200)
    private String description;
}
