package com.example.backend.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.entity.*;
import com.example.backend.exception.ApiException;
import com.example.backend.integration.ai.AIClient;
import com.example.backend.service.CurrentUserService;
import com.example.backend.system.simulation.service.AIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.config.import=", "physlive.database.external-enabled=false",
    "SUPABASE_DB_URL=", "DATABASE_URL=", "DIRECT_URL=",
    "spring.datasource.url=jdbc:h2:mem:staff-preview;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never", "spring.flyway.enabled=false",
    "jwt.secret=staff-test-only-signing-key-at-least-32-bytes",
    "physlive.staff.preview-enabled=true",
    "physlive.ai.provider.text-model=test", "physlive.ai.provider.vision-model=test",
    "physlive.ai.visual.model=test", "physlive.ai.jev.api-key=test-only",
    "physlive.ai.jev.model=test", "physlive.ai.jev.base-url=https://router.example.test",
    "logging.level.root=WARN"
})
@AutoConfigureMockMvc
class StaffPreviewIntegrationTest {
    @Autowired MockMvc http;
    @Autowired AIService ai;
    @Autowired ObjectMapper json;
    @Autowired com.example.backend.repository.TopicRepository topics;
    @Autowired com.example.backend.repository.SchemaVersionRepository schemas;
    @MockBean AIClient client;
    @MockBean CurrentUserService currentUser;

    private User staff(String permission) {
        var role = new Role(); role.setName("STAFF");
        var user = new User(); user.setRole(role);
        var grant = new Permission(permission); grant.setRole(role);
        user.getPermissions().add(new PermissionGrant(grant, null, Instant.now()));
        return user;
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void departmentHeadCannotUseUnderstanding() throws Exception {
        when(currentUser.requireCurrentUser()).thenReturn(staff("DEPARTMENT_HEAD_PHYSICS"));
        http.perform(post("/api/simulation/understand").contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"A moving car\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(client);
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void teachingGrantReachesInputValidation() throws Exception {
        when(currentUser.requireCurrentUser()).thenReturn(staff("TEACH"));
        http.perform(post("/api/simulation/understand").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":\"ocr\",\"recognizedText\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(client);
    }

    @Test
    void unsignedPlansCannotGenerateOrCompute() {
        var request = json.createObjectNode().put("schemaId", "motion").put("schemaVersion", "1");
        request.putObject("simulationSpec");
        assertThatThrownBy(() -> ai.generate(request)).isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
        assertThatThrownBy(() -> ai.compute(request)).isInstanceOf(ApiException.class);
        verifyNoInteractions(client);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void approvedModelCanBeUnderstoodGeneratedAndRecomputed() throws Exception {
        var catalogue = com.example.backend.bootstrap.SchemaCatalogCompiler.loadFromClasspath(json);
        assertThat(catalogue.size()).isEqualTo(21);
        var entry = java.util.stream.StreamSupport.stream(catalogue.spliterator(), false)
                .filter(item -> item.path("schemaId").asText().contains("kinematics"))
                .findFirst().orElseThrow();
        var topic = new Topic();
        topic.setName(entry.path("topic").asText()); topic.setSlug("staff-test-motion");
        topics.saveAndFlush(topic);
        var schema = new SchemaVersion();
        schema.setSchemaId(entry.path("schemaId").asText()); schema.setVersion(entry.path("version").asText());
        schema.setName(entry.path("name").asText()); schema.setTopic(topic.getName());
        schema.setDefinition(entry.path("definition")); schemas.saveAndFlush(schema);

        var route = json.createObjectNode();
        route.putObject("topic_schema").put("choice", schema.getSchemaId()).put("confidence", 1)
                .putObject("probabilities").put(schema.getSchemaId(), 1).put("NO_MATCH", 0);
        when(client.route(anyString(), any())).thenReturn(route);
        var fixture = json.readTree("""
            {"status":"EXPLAIN","simulationSpec":{"durationSeconds":2,
             "objects":[{"id":"car","label":"Car"}],
             "parameters":[
               {"name":"m","label":"Mass","value":2,"unit":"kg","min":1,"max":10,"step":1,"role":"mass","of":"car"},
               {"name":"v0","label":"Initial velocity","value":10,"unit":"m/s","min":1,"max":30,"step":1}],
             "model":{"quantities":[
               {"id":"x","label":"Position","unit":"m","role":"position","of":"car","axis":"x","initial":0,"rate":"v"},
               {"id":"v","label":"Velocity","unit":"m/s","role":"velocity","of":"car","axis":"x","initial":"v0","rate":0},
               {"id":"p","label":"Momentum","unit":"kg*m/s","role":"momentum","of":"car","axis":"x","value":["mul","m","v"]},
               {"id":"K","label":"Kinetic energy","unit":"J","role":"kinetic_energy","of":"car","value":["mul",0.5,["mul","m",["pow","v",2]]]}]}}}
            """);
        when(client.text(any(), any())).thenAnswer(call -> fixture.deepCopy());
        var visual = json.createObjectNode();
        visual.putObject("visualProgram").put("code", "({init(){},update(){},destroy(){}})").put("playbackRate", 1);
        when(client.visual(any(), anyString(), any())).thenReturn(visual);

        var plan = ai.understandText("A car moves at constant velocity", null, null, null);
        assertThat(plan.path("stage").asText()).isEqualTo("EXPLAIN");
        assertThat(plan.path("planSignature").asText()).isNotBlank();
        var generated = ai.generate(plan);
        assertThat(generated.path("stage").asText()).isEqualTo("SIMULATION");
        generated.putObject("parameters").put("v0", 15);
        var computed = ai.compute(generated);
        assertThat(computed.path("solverTimeline").path("frames").size()).isGreaterThan(1);
        assertThat(computed.path("presentation").path("parameters").path("v0").asDouble()).isEqualTo(15);
        assertThat(computed.path("validation").path("status").asText()).startsWith("VERIFIED");
        generated.put("description", "Tampered description");
        assertThatThrownBy(() -> ai.compute(generated)).isInstanceOf(ApiException.class);
    }
}
