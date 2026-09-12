package com.example.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.backend.dto.simulation.SimulationRunResponse;
import com.example.backend.dto.simulation.SnapshotResponse;
import com.example.backend.entity.SimulationRunStatus;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.service.PhysicsSimulationRunService;
import com.example.backend.service.PhysicsSimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SimulationRunApiIntegrationTest {

    @Mock private PhysicsSimulationService simulationService;
    @Mock private PhysicsSimulationRunService runService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new SimulationController(simulationService, runService),
                new SimulationRunController(runService)).build();
    }

    @Test
    void exposesCreateGetAndReplayEndpoints() throws Exception {
        UUID simulationId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        SimulationRunResponse run = response(runId, simulationId);
        SnapshotResponse snapshot = new SnapshotResponse(UUID.randomUUID(), 0, BigDecimal.ZERO,
                objectMapper.createObjectNode().put("payloadVersion", "simulation-replay-v1"));
        when(runService.create(eq(simulationId), any())).thenReturn(run);
        when(runService.get(runId)).thenReturn(run);
        when(runService.snapshots(runId)).thenReturn(List.of(snapshot));

        mockMvc.perform(post("/api/simulations/{id}/runs", simulationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":{\"velocity\":4}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(runId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(get("/api/runs/{id}", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solverVersion").value("uniform-motion-numerical-v1"));
        mockMvc.perform(get("/api/runs/{id}/snapshots", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sequenceNumber").value(0))
                .andExpect(jsonPath("$[0].state.payloadVersion").value("simulation-replay-v1"));
    }

    private SimulationRunResponse response(UUID runId, UUID simulationId) {
        return new SimulationRunResponse(runId, simulationId, PhysicsArchetype.UNIFORM_MOTION,
                SimulationRunStatus.COMPLETED, "uniform-motion-numerical-v1",
                objectMapper.createObjectNode(), objectMapper.createObjectNode(), 2,
                null, null, 2L, null);
    }
}
