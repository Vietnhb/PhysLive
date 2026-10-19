package com.example.backend.system.simulation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.bootstrap.SchemaCatalogCompiler;
import com.example.backend.config.AIProperties;
import com.example.backend.config.JevProperties;
import com.example.backend.config.UploadProperties;
import com.example.backend.integration.ai.AIClient;
import com.example.backend.system.physics.model.entity.SchemaVersion;
import com.example.backend.system.physics.service.SchemaDefinitionService;
import com.example.backend.system.physics.service.SchemaEquationRuntime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A slider recomputation of a signed plan: the same request returns the very same result from the kept results without
 * solving again, the topic version and catalogue are read once while the approved laws stay the same, and a change to
 * the approved laws solves anew.
 */
class ComputeCachingTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void theSameRecomputationIsServedFromTheKeptResultUntilTheLawsChange() throws Exception {
        var schemas = mock(SchemaDefinitionService.class);
        List<SchemaVersion> catalogue = new ArrayList<>();
        for (JsonNode item : SchemaCatalogCompiler.loadFromClasspath(json)) {
            var schema = new SchemaVersion();
            schema.setSchemaId(item.path("schemaId").asText()); schema.setVersion(item.path("version").asText());
            schema.setName(item.path("name").asText()); schema.setTopic(item.path("topic").asText()); schema.setDefinition(item.path("definition"));
            catalogue.add(schema);
        }
        var mark = new AtomicReference<>("laws-v1");
        when(schemas.approvedFingerprint()).thenAnswer(call -> mark.get());
        when(schemas.approvedSchemas()).thenReturn(catalogue);
        when(schemas.requireCurrentApproved(anyString(), anyString())).thenAnswer(call -> catalogue.stream()
                .filter(s -> s.getSchemaId().equals(call.getArgument(0)) && s.getVersion().equals(call.getArgument(1))).findFirst().orElseThrow());
        var ai = new AIProperties(new AIProperties.Provider("x", URI.create("https://example.invalid"), "m", "m", "classpath:prompts/physics-ocr-system.txt",
                Duration.ofSeconds(1), Duration.ofSeconds(1), .2),
                new AIProperties.Visual("x", "openai_compatible", URI.create("https://example.invalid"), "m", .4, Duration.ofSeconds(1), true, true, 1000, "low"));
        var jev = new JevProperties("x", URI.create("https://example.invalid"), "x", Duration.ofSeconds(1), .25, .08, 20000, 80000);
        var service = new AIService(schemas, new SchemaEquationRuntime(json, .02, 3.2e9, 16384, 256, 32, 512), mock(GeneratedSimulationStorage.class),
                jev, json, ai, new AIClient(ai, jev, json), new UploadProperties(10485760, Set.of("image/png")), "test-signing-key-test-signing-key", 250000);
        ReflectionTestUtils.setField(service, "computed", new ComputeResultCache(64L << 20));

        ObjectNode request = json.createObjectNode();
        JsonNode spec;
        try (var stream = getClass().getResourceAsStream("/law-cases/flex__decay.json")) { spec = json.readTree(stream).path("simulationSpec"); }
        var schema = catalogue.stream().filter(s -> s.getSchemaId().equals("thpt_nuclear_physics")).findFirst().orElseThrow();
        request.put("stage", "EXPLAIN").put("schemaId", schema.getSchemaId()).put("schemaVersion", schema.getVersion()).put("description", "phân rã");
        request.set("simulationSpec", spec.deepCopy());
        request.put("planSignature", (String) ReflectionTestUtils.invokeMethod(service, "signPlan", request, true));
        request.putObject("parameters");

        long t0 = System.nanoTime();
        JsonNode first = service.compute(request.deepCopy());
        long firstMs = (System.nanoTime() - t0) / 1_000_000;
        long t1 = System.nanoTime();
        JsonNode second = service.compute(request.deepCopy());
        long secondMs = (System.nanoTime() - t1) / 1_000_000;
        System.out.println("CACHE first " + firstMs + " ms, repeated " + secondMs + " ms");

        assertEquals(first, second, "a kept result is the very same result");
        assertTrue(secondMs * 5 < firstMs, "the repeated request is served without solving again: " + firstMs + " ms then " + secondMs + " ms");
        verify(schemas, times(1)).requireCurrentApproved(anyString(), anyString());

        mark.set("laws-v2"); // a reviewer approves a change to the laws: nothing kept for the old laws is served
        long t2 = System.nanoTime();
        JsonNode third = service.compute(request.deepCopy());
        long thirdMs = (System.nanoTime() - t2) / 1_000_000;
        System.out.println("CACHE after a law change " + thirdMs + " ms");
        assertEquals(first, third, "same laws content, same result");
        assertTrue(thirdMs * 5 > firstMs, "solved anew after the change");
        verify(schemas, times(2)).requireCurrentApproved(anyString(), anyString());
    }
}
