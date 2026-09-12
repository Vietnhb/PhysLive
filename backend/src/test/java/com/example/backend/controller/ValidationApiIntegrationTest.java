package com.example.backend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.backend.dto.validation.ValidationRunResponse;
import com.example.backend.entity.ValidationRunStatus;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.service.PhysicsValidationService;
import com.example.backend.service.PhysicsSimulationService;
import com.example.backend.service.SpecificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ValidationApiIntegrationTest {

    @Mock private SpecificationService specificationService;
    @Mock private PhysicsValidationService physicsValidationService;
    @Mock private PhysicsSimulationService physicsSimulationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new SpecificationController(specificationService, physicsValidationService),
                new ValidationController(physicsValidationService, physicsSimulationService)).build();
    }

    @Test
    void postCreatesValidationRun() throws Exception {
        UUID specificationId = UUID.randomUUID();
        UUID validationId = UUID.randomUUID();
        when(physicsValidationService.validate(specificationId))
                .thenReturn(response(validationId, specificationId));

        mockMvc.perform(post("/api/specifications/{id}/validations", specificationId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(validationId.toString()))
                .andExpect(jsonPath("$.status").value("PASSED"))
                .andExpect(jsonPath("$.archetype").value("UNIFORM_MOTION"));
    }

    @Test
    void getReturnsOwnedValidationRepresentation() throws Exception {
        UUID specificationId = UUID.randomUUID();
        UUID validationId = UUID.randomUUID();
        when(physicsValidationService.get(validationId)).thenReturn(response(validationId, specificationId));

        mockMvc.perform(get("/api/validations/{id}", validationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specificationId").value(specificationId.toString()))
                .andExpect(jsonPath("$.numericalSolverVersion").value("uniform-motion-numerical-v1"))
                .andExpect(jsonPath("$.referenceSolverVersion").value("uniform-motion-reference-v1"));
    }

    private ValidationRunResponse response(UUID id, UUID specificationId) {
        ObjectMapper mapper = new ObjectMapper();
        return new ValidationRunResponse(id, specificationId, PhysicsArchetype.UNIFORM_MOTION,
                ValidationRunStatus.PASSED,
                mapper.createObjectNode().put("schemaVersion", "1.0"),
                mapper.createObjectNode().put("finalPosition", 6),
                mapper.createObjectNode().put("finalPosition", 6),
                mapper.createObjectNode().put("finalPosition", 0),
                mapper.createObjectNode().put("finalPosition", 0),
                mapper.createObjectNode(), "1.0",
                "uniform-motion-numerical-v1", "uniform-motion-reference-v1",
                UUID.randomUUID(), "kinematics-uniform-v1",
                Instant.parse("2026-09-12T00:00:00Z"), Instant.parse("2026-09-12T00:00:00.010Z"),
                10L, null);
    }
}
