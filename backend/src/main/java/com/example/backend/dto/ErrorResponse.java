package com.example.backend.dto;

import java.time.Instant;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Chuẩn định dạng phản hồi lỗi HTTP (Standardized API Error Response).
 * Tương thích ngược 100% với các trường cũ (status, message).
 * Bổ sung: error, timestamp, path, traceId, validationErrors.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private String timestamp;
    private String traceId;
    private Map<String, String> validationErrors;

    /**
     * Constructor tương thích ngược cho các class/service hiện hữu gọi new ErrorResponse(status, message)
     */
    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.error = resolveStatusReason(status);
        this.timestamp = Instant.now().toString();
        this.traceId = MDC.get("traceId");
    }

    public ErrorResponse(int status, String message, String path) {
        this(status, message);
        this.path = path;
    }

    public ErrorResponse(int status, String message, String path, Map<String, String> validationErrors) {
        this(status, message, path);
        this.validationErrors = validationErrors;
    }

    private static String resolveStatusReason(int status) {
        try {
            return HttpStatus.valueOf(status).getReasonPhrase();
        } catch (IllegalArgumentException ex) {
            return "Error";
        }
    }
}
