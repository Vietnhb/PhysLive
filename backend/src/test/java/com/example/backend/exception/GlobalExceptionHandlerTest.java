package com.example.backend.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @Getter
    @Setter
    public static class SampleValidRequest {
        @NotBlank(message = "Tên không được để trống")
        private String name;
    }

    @RestController
    public static class TestExceptionController {

        @PostMapping("/test/validate")
        public String testValidate(@Valid @RequestBody SampleValidRequest request) {
            return "ok";
        }

        @GetMapping("/test/api-exception")
        public void testApiException() {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Lỗi nghiệp vụ");
        }

        @GetMapping("/test/physics-exception")
        public void testPhysicsException() {
            throw new InvalidPhysicsInputException("Thông số vận tốc âm không hợp lệ");
        }

        @GetMapping("/test/access-denied")
        public void testAccessDenied() {
            throw new AccessDeniedException("Forbidden action");
        }

        @GetMapping("/test/not-found")
        public void testNotFound() {
            throw new NoSuchElementException("Không tìm thấy mục yêu cầu");
        }

        @GetMapping("/test/internal-error")
        public void testInternalError() {
            throw new RuntimeException("Unexpected crash");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleValidationException_Returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").value("Tên không được để trống"))
                .andExpect(jsonPath("$.path").value("/test/validate"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void handleApiException_ReturnsStatusFromException() throws Exception {
        mockMvc.perform(get("/test/api-exception"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Lỗi nghiệp vụ"))
                .andExpect(jsonPath("$.path").value("/test/api-exception"));
    }

    @Test
    void handleInvalidPhysicsInput_Returns422() throws Exception {
        mockMvc.perform(get("/test/physics-exception"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Thông số vận tốc âm không hợp lệ"));
    }

    @Test
    void handleAccessDenied_Returns403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));
    }

    @Test
    void handleNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Không tìm thấy mục yêu cầu"));
    }

    @Test
    void handleAllException_Returns500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/internal-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ, vui lòng thử lại sau"));
    }
}
