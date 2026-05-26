package com.pragma.openfinance.auth.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserInfoController {

    @GetMapping("/userinfo")
    public ResponseEntity<Map<String, Object>> userInfo(
            @RequestHeader("Authorization") String authorization) {

        // En producción: decodificar el access token, extraer sub, consultar user store
        // Para PoC: retornar datos mock
        return ResponseEntity.ok(Map.of(
                "sub", "user-12345",
                "name", "Juan Pérez",
                "email", "juan.perez@example.com",
                "email_verified", true
        ));
    }
}
