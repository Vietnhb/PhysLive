package com.example.backend.system.physics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * The law catalogues against models of many different situations (src/test/resources/law-cases): every
 * physically correct model must be accepted by the merged catalogue of all strands, and a model with a law
 * broken must be refused with that law named. A new law that refuses one of these models is too narrow.
 */
class LawCatalogueTest {
    private final ObjectMapper json = new ObjectMapper();
    private final FreeModel models = new FreeModel(json, new SchemaEquationRuntime(json, .02, 3.2e9, 16384, 256, 32, 512));
    private final PathMatchingResourcePatternResolver resources = new PathMatchingResourcePatternResolver();

    private JsonNode catalogue() throws Exception {
        List<JsonNode> strands = new ArrayList<>();
        for (Resource resource : resources.getResources("classpath*:schemas/laws/*.json"))
            try (var input = resource.getInputStream()) { strands.add(json.readTree(input)); }
        assertTrue(strands.size() >= 7, "law catalogues of every strand are on the classpath");
        return LawChecks.merge(json, strands);
    }

    @Test
    void everyCorrectModelIsAcceptedByTheLawsOfAllStrandsTogether() throws Exception {
        JsonNode catalogue = catalogue();
        List<String> refused = new ArrayList<>();
        int cases = 0, checks = 0;
        for (Resource resource : resources.getResources("classpath*:law-cases/*.json")) {
            JsonNode spec;
            try (var input = resource.getInputStream()) { spec = json.readTree(input).path("simulationSpec"); }
            cases++;
            try {
                JsonNode validation = models.compute(catalogue, spec, json.createObjectNode()).path("validation");
                checks += validation.path("lawChecks").size();
                if (!validation.path("status").asText().startsWith("VERIFIED"))
                    refused.add(resource.getFilename() + ": " + validation.path("status").asText() + " " + validation.path("flags"));
            } catch (RuntimeException error) {
                refused.add(resource.getFilename() + ": " + error.getMessage());
            }
        }
        assertTrue(cases >= 50, "the case set is on the classpath");
        assertTrue(checks > 500, "laws were actually applied");
        assertEquals(List.of(), refused);
    }

    @Test
    void aModelThatBreaksALawThroughAJumpOrARandomDrawIsRefused() throws Exception {
        JsonNode catalogue = catalogue();
        int cases = 0;
        for (Resource resource : resources.getResources("classpath*:law-cases-refused/*.json")) {
            JsonNode spec;
            try (var input = resource.getInputStream()) { spec = json.readTree(input).path("simulationSpec"); }
            cases++;
            assertEquals(FreeModel.FLAGGED, models.compute(catalogue, spec, json.createObjectNode()).path("validation").path("status").asText(), resource.getFilename());
        }
        assertTrue(cases >= 2, "the refused cases are on the classpath");
    }

    @Test
    void theStrandsShareOneVocabulary() throws Exception {
        java.util.Map<String, String> roles = new java.util.LinkedHashMap<>();
        java.util.Set<String> laws = new java.util.LinkedHashSet<>();
        List<String> clashes = new ArrayList<>();
        for (Resource resource : resources.getResources("classpath*:schemas/laws/*.json")) {
            JsonNode strand;
            try (var input = resource.getInputStream()) { strand = json.readTree(input); }
            strand.path("roles").fields().forEachRemaining(role -> {
                String known = roles.putIfAbsent(role.getKey(), role.getValue().asText());
                if (known != null && !known.equals(role.getValue().asText())) clashes.add("role " + role.getKey() + " means two things");
            });
            for (JsonNode law : strand.path("laws"))
                if (!laws.add(law.path("id").asText())) clashes.add("law " + law.path("id").asText() + " is defined twice");
        }
        assertEquals(List.of(), clashes);
    }

    @Test
    void aBrokenLawIsNamedInTheRefusal() throws Exception {
        ObjectNode spec;
        try (var input = resources.getResource("classpath:law-cases/mechanics__blind_f01.json").getInputStream()) {
            spec = (ObjectNode) json.readTree(input).path("simulationSpec");
        }
        for (JsonNode quantity : spec.path("model").path("quantities"))
            if (quantity.path("role").asText().equals("force") && quantity.path("kind").asText().equals("friction")) {
                ArrayNode reversed = json.createArrayNode().add("neg");
                reversed.add(quantity.path("value"));
                ((ObjectNode) quantity).set("value", reversed);
            }
        JsonNode validation = models.compute(catalogue(), spec, json.createObjectNode()).path("validation");
        assertEquals(FreeModel.FLAGGED, validation.path("status").asText());
        assertTrue(validation.path("flags").toString().contains("ma sát"), validation.path("flags").toString());
    }
}
