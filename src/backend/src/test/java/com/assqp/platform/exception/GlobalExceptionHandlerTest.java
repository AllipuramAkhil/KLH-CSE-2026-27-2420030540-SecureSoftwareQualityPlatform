package com.assqp.platform.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404WithStructuredBody() {
        when(request.getRequestURI()).thenReturn("/api/projects/999");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Project not found with id: 999"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).contains("Project not found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/projects/999");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleBadRequest_returns400() {
        when(request.getRequestURI()).thenReturn("/api/tests/1/execute");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(
                new BadRequestException("Invalid execution status: BOGUS"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
    }

    @Test
    void handleValidation_includesFieldLevelErrors() {
        when(request.getRequestURI()).thenReturn("/api/projects");

        ResponseEntity<ErrorResponse> response = handler.handleValidation(
                new ValidationException("Validation failed", Map.of("name", "Project name is required")),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getValidationErrors()).containsEntry("name", "Project name is required");
    }

    @Test
    void handleAiService_returns503_notInternalServerError() {
        when(request.getRequestURI()).thenReturn("/api/projects/1/ai/analyze");

        ResponseEntity<ErrorResponse> response = handler.handleAiService(
                new AiServiceException("AI provider unreachable"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void handleGeneric_neverLeaksStackTrace() {
        when(request.getRequestURI()).thenReturn("/api/health");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(
                new RuntimeException("Some internal detail that should not leak"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("Some internal detail");
    }
}
