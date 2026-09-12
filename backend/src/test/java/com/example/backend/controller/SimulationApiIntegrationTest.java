package com.example.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.backend.dto.simulation.SimulationResponse;
import com.example.backend.entity.SimulationStatus;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.service.PhysicsSimulationService;
import com.example.backend.service.PhysicsSimulationRunService;
import com.example.backend.service.PhysicsSimulationService.CreationResult;
import com.example.backend.service.PhysicsValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SimulationApiIntegrationTest {

    @Mock private PhysicsValidationService physicsValidationService;
    @Mock private PhysicsSimulationService physicsSimulationService;
    @Mock private PhysicsSimulationRunService simulationRunService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ValidationController(physicsValidationService, physicsSimulationService),
                new SimulationController(physicsSimulationService, simulationRunService)).build();
    }

    @Test
    void postReturnsCreatedForFirstRequestAndOkForRetry() throws Exception {
        UUID validationId = UUID.randomUUID();
        SimulationResponse response = response(validationId);
        when(physicsSimulationService.createFromValidation(validationId))
                .thenReturn(new CreationResult(response, true))
                .thenReturn(new CreationResult(response, false));

        mockMvc.perform(post("/api/validations/{id}/simulation", validationId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.status").value("READY"));
        mockMvc.perform(post("/api/validations/{id}/simulation", validationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @Test
    void getReturnsSimulation() throws Exception {
        UUID validationId = UUID.randomUUID();
        SimulationResponse response = response(validationId);
        when(physicsSimulationService.get(response.id())).thenReturn(response);

        mockMvc.perform(get("/api/simulations/{id}", response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationRunId").value(validationId.toString()))
                .andExpect(jsonPath("$.archetype").value("UNIFORM_MOTION"));
    }

    private SimulationResponse response(UUID validationId) {
        return new SimulationResponse(UUID.randomUUID(), validationId, UUID.randomUUID(),
                PhysicsArchetype.UNIFORM_MOTION, SimulationStatus.READY,
                new ObjectMapper().createObjectNode().put("duration", 3), null, null, null);
    }
}
