package com.assqp.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Service health check")
public class HealthController {

    @GetMapping
    @Operation(summary = "Basic liveness check - always public, no auth required")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "ASSQP Backend",
                "version", "1.0.0",
                "timestamp", LocalDateTime.now()
        );
    }
}
