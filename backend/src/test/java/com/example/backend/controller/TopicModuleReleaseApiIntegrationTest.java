package com.example.backend.controller;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.backend.dto.release.TopicModuleReleaseResponse;
import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.service.TopicModuleReleaseService;

@ExtendWith(MockitoExtension.class)
class TopicModuleReleaseApiIntegrationTest {

    @Mock private TopicModuleReleaseService releaseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TopicModuleReleaseController(releaseService)).build();
    }

    @Test
    void exposesDraftApproveRetireAndGetEndpoints() throws Exception {
        UUID releaseId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        TopicModuleReleaseResponse draft = response(releaseId, topicId, ModuleReleaseStatus.DRAFT);
        TopicModuleReleaseResponse approved = response(releaseId, topicId, ModuleReleaseStatus.APPROVED);
        TopicModuleReleaseResponse retired = response(releaseId, topicId, ModuleReleaseStatus.RETIRED);
        when(releaseService.createDraft(any())).thenReturn(draft);
        when(releaseService.approve(releaseId)).thenReturn(approved);
        when(releaseService.retire(releaseId)).thenReturn(retired);
        when(releaseService.get(releaseId)).thenReturn(retired);

        mockMvc.perform(post("/api/module-releases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicId":"%s","releaseVersion":"uniform-v1",
                                 "schemaVersion":"1.0","archetype":"UNIFORM_MOTION"}
                                """.formatted(topicId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
        mockMvc.perform(post("/api/module-releases/{id}/approve", releaseId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/api/module-releases/{id}/retire", releaseId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RETIRED"));
        mockMvc.perform(get("/api/module-releases/{id}", releaseId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.releaseVersion").value("uniform-v1"));
    }

    private TopicModuleReleaseResponse response(
            UUID id, UUID topicId, ModuleReleaseStatus status) {
        return new TopicModuleReleaseResponse(id, topicId, "kinematics", PhysicsArchetype.UNIFORM_MOTION,
                "uniform-v1", "1.0", "uniform-motion-numerical-v1",
                "uniform-motion-reference-v1", status, 10, 10, 11,
                null, null, null, null);
    }
}
