package com.example.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.backend.dto.health.SystemHealthResponse;
import com.example.backend.service.HealthService;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private HealthService healthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController(healthService)).build();
    }

    @Test
    void getHealth_WhenSystemIsUp_ReturnsOkWithDetails() throws Exception {
        SystemHealthResponse response = SystemHealthResponse.builder()
                .status("UP")
                .timestamp("2026-09-12T20:30:00Z")
                .uptimeMs(15000)
                .database(SystemHealthResponse.DatabaseHealth.builder()
                        .status("UP")
                        .database("PostgreSQL 16.1")
                        .latencyMs(3)
                        .build())
                .diskSpace(SystemHealthResponse.DiskHealth.builder()
                        .status("UP")
                        .freeGb("45.50 GB")
                        .build())
                .memory(SystemHealthResponse.MemoryHealth.builder()
                        .status("UP")
                        .usedMb("120.0 MB")
                        .maxMb("2048.0 MB")
                        .build())
                .physicsEngine(SystemHealthResponse.PhysicsEngineHealth.builder()
                        .status("UP")
                        .supportedArchetypes(List.of("UNIFORM_MOTION", "CONSTANT_ACCELERATION"))
                        .solverTypes(List.of("CLOSED_FORM", "NUMERICAL_ODE"))
                        .build())
                .build();

        when(healthService.checkHealth()).thenReturn(response);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database.status").value("UP"))
                .andExpect(jsonPath("$.database.database").value("PostgreSQL 16.1"))
                .andExpect(jsonPath("$.diskSpace.status").value("UP"))
                .andExpect(jsonPath("$.physicsEngine.supportedArchetypes[0]").value("UNIFORM_MOTION"));
    }

    @Test
    void getHealth_WhenSystemIsDown_ReturnsServiceUnavailable() throws Exception {
        SystemHealthResponse response = SystemHealthResponse.builder()
                .status("DOWN")
                .timestamp("2026-09-12T20:30:00Z")
                .database(SystemHealthResponse.DatabaseHealth.builder()
                        .status("DOWN")
                        .error("Connection refused")
                        .build())
                .build();

        when(healthService.checkHealth()).thenReturn(response);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database.status").value("DOWN"));
    }
}
