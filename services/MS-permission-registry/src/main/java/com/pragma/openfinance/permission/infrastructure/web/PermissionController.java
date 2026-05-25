package com.pragma.openfinance.permission.infrastructure.web;

import com.pragma.openfinance.permission.domain.model.Permission;
import com.pragma.openfinance.permission.domain.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<Permission>> listPermissions(
            @RequestParam(required = false) String type) {

        if (type != null) {
            return ResponseEntity.ok(permissionService.getPermissionsByType(type));
        }
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validatePermission(
            @RequestParam String permission,
            @RequestParam String httpMethod,
            @RequestParam String endpoint) {

        boolean valid = permissionService.validatePermission(permission, httpMethod, endpoint);
        String required = permissionService.getRequiredPermission(httpMethod, endpoint).orElse("UNKNOWN");

        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "requestedPermission", permission,
                "requiredPermission", required,
                "endpoint", endpoint
        ));
    }
}
