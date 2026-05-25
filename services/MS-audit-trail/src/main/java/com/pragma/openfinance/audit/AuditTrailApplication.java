package com.pragma.openfinance.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuditTrailApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditTrailApplication.class, args);
    }
}
