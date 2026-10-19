package com.example.backend.system.physics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * A slider or constant given the role of a quantity the model computes (a car's v0 beside its velocity, T0 beside the
 * temperature) is where that quantity starts: the laws check the computed one instead of finding two and checking
 * nothing. Models written by a real provider (live-planning-cases) did exactly this.
 */
class RoleOncePerThingTest {
    private final ObjectMapper json = new ObjectMapper();
    private final FreeModel models = new FreeModel(json, new SchemaEquationRuntime(json, .02, 3.2e9, 16384, 256, 32, 512));
    private final PathMatchingResourcePatternResolver resources = new PathMatchingResourcePatternResolver();

    private JsonNode catalogue() throws Exception {
        List<JsonNode> strands = new ArrayList<>();
        for (Resource resource : resources.getResources("classpath*:schemas/laws/*.json"))
            try (var input = resource.getInputStream()) { strands.add(json.readTree(input)); }
        return LawChecks.merge(json, strands);
    }

    private JsonNode validation(JsonNode spec) throws Exception {
        return models.compute(catalogue(), spec, json.createObjectNode()).path("validation");
    }

    /** A car driven at a wall that bounces back elastically; v0 sets where its velocity starts. */
    private ObjectNode car(boolean startingValueHasRole) throws Exception {
        ObjectNode spec = (ObjectNode) json.readTree("""
                {"durationSeconds": 2,
                 "objects": [{"id": "car", "label": "Xe"}],
                 "parameters": [
                   {"name": "m", "label": "Khối lượng", "value": 1500, "unit": "kg", "min": 500, "max": 3000, "step": 100, "role": "mass", "of": "car"},
                   {"name": "x0", "label": "Vị trí đầu", "value": 10, "unit": "m", "min": 1, "max": 50, "step": 1},
                   {"name": "v0", "label": "Vận tốc đầu", "value": -15, "unit": "m/s", "min": -30, "max": -1, "step": 1}],
                 "model": {"quantities": [
                   {"id": "x", "label": "Vị trí", "unit": "m", "role": "position", "of": "car", "axis": "x", "initial": "x0", "rate": "vx"},
                   {"id": "vx", "label": "Vận tốc", "unit": "m/s", "role": "velocity", "of": "car", "axis": "x", "initial": "v0", "rate": 0,
                    "jumps": [{"when": ["neg", "x"], "to": ["neg", "vx"]}]},
                   {"id": "p", "label": "Động lượng", "unit": "kg*m/s", "role": "momentum", "of": "car", "axis": "x", "value": ["mul", "m", "vx"]},
                   {"id": "KE", "label": "Động năng", "unit": "J", "role": "kinetic_energy", "of": "car", "value": ["mul", 0.5, ["mul", "m", ["pow", "vx", 2]]]}]}}
                """);
        if (startingValueHasRole)
            ((ObjectNode) spec.path("parameters").get(2)).put("role", "velocity").put("of", "car").put("axis", "x");
        return spec;
    }

    @Test
    void aStartingValueWithTheRoleOfWhatItStartsDoesNotHideTheLaws() throws Exception {
        for (boolean hasRole : List.of(true, false)) {
            JsonNode validation = validation(car(hasRole));
            assertEquals(0, validation.path("ambiguousLaws").size(), hasRole + " " + validation.path("ambiguousLaws"));
            assertTrue(validation.path("status").asText().startsWith("VERIFIED"), hasRole + " " + validation.path("status") + " " + validation.path("flags"));
        }
    }

    /** A force set by a slider is a real force all along: it stays in the sum of the forces. */
    @Test
    void aForceSetByASliderStillCountsInNewtonsLaw() throws Exception {
        String box = """
                {"durationSeconds": 2,
                 "objects": [{"id": "box", "label": "Thùng"}],
                 "parameters": [
                   {"name": "m", "label": "Khối lượng", "value": 2, "unit": "kg", "min": 1, "max": 10, "step": 1, "role": "mass", "of": "box"},
                   {"name": "F", "label": "Lực kéo", "value": 10, "unit": "N", "min": 0, "max": 50, "step": 1, "role": "force", "kind": "applied", "of": "box", "axis": "x"},
                   {"name": "b", "label": "Hệ số cản", "value": 1, "unit": "kg/s", "min": 0.1, "max": 5, "step": 0.1}],
                 "model": {"quantities": [
                   {"id": "x", "label": "Vị trí", "unit": "m", "role": "position", "of": "box", "axis": "x", "initial": 0, "rate": "v"},
                   {"id": "v", "label": "Vận tốc", "unit": "m/s", "role": "velocity", "of": "box", "axis": "x", "initial": 0, "rate": %s},
                   {"id": "Fd", "label": "Lực cản", "unit": "N", "role": "force", "kind": "drag", "of": "box", "axis": "x", "value": ["neg", ["mul", "b", "v"]]}]}}
                """;
        JsonNode right = validation(json.readTree(box.formatted("[\"div\", [\"add\", \"F\", \"Fd\"], \"m\"]")));
        assertTrue(right.path("status").asText().startsWith("VERIFIED"), right.path("status") + " " + right.path("flags"));
        // the same box accelerated as if the slider force were not there must be refused, so that force is still counted
        JsonNode wrong = validation(json.readTree(box.formatted("[\"div\", \"Fd\", \"m\"]")));
        assertEquals(FreeModel.FLAGGED, wrong.path("status").asText(), wrong.path("flags").toString());
    }

    /** Projectile (h0, v0), RC discharge (V0) and heating (T0) as gpt-oss-120b wrote them for real. */
    @Test
    void modelsAProviderWroteWithStartingValuesAreChecked() throws Exception {
        List<String> unclear = new ArrayList<>();
        for (Resource resource : resources.getResources("classpath*:live-planning-cases/*.json")) {
            JsonNode spec;
            try (var input = resource.getInputStream()) { spec = json.readTree(input).path("simulationSpec"); }
            JsonNode validation = validation(spec);
            if (!validation.path("ambiguousLaws").isEmpty() || validation.path("status").asText().equals(FreeModel.FLAGGED))
                unclear.add(resource.getFilename() + ": " + validation.path("status") + " " + validation.path("ambiguousLaws") + " " + validation.path("flags"));
        }
        assertEquals(List.of(), unclear);
    }
}
