package com.example.backend.system.simulation.service;

import com.example.backend.bootstrap.SchemaCatalogCompiler;
import com.example.backend.config.*;
import com.example.backend.integration.ai.AIClient;
import com.example.backend.system.physics.model.entity.SchemaVersion;
import com.example.backend.system.physics.service.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Explicit opt-in: real configured AI providers, checked-in catalogues, no database writes. */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_SIMULATION", matches = "true")
class LiveSimulationProviderTest {
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, String> settings = new HashMap<>();
    private String setting(String name, String fallback) {
        return System.getenv().getOrDefault(name, settings.getOrDefault(name, fallback));
    }
    private long number(String name, long fallback) { return Long.parseLong(setting(name, String.valueOf(fallback))); }

    @Test
    void realDescriptionsReachTheConfiguredVisualProvider() throws Exception {
        for (String line : Files.readAllLines(Path.of(".env.local"))) {
            int equals = line.indexOf('=');
            if (equals < 1 || line.stripLeading().startsWith("#")) continue;
            String value = line.substring(equals + 1).trim();
            if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))))
                value = value.substring(1, value.length() - 1);
            settings.put(line.substring(0, equals).trim(), value);
        }
        var ai = new AIProperties(new AIProperties.Provider(setting("AI_API_KEY", ""), URI.create(setting("AI_BASE_URL", "")),
                setting("AI_TEXT_MODEL", ""), setting("AI_VISION_MODEL", ""), setting("AI_OCR_PROMPT_RESOURCE", "classpath:prompts/physics-ocr-system.txt"),
                Duration.ofMillis(number("AI_CONNECT_TIMEOUT_MS", 5000)), Duration.ofMillis(number("AI_READ_TIMEOUT_MS", 30000)),
                Double.parseDouble(setting("AI_UNDERSTANDING_TEMPERATURE", ".2"))),
                new AIProperties.Visual(setting("AI_VISUAL_API_KEY", ""), setting("AI_VISUAL_PROVIDER", "openai_compatible"),
                URI.create(setting("AI_VISUAL_BASE_URL", "")), setting("AI_VISUAL_MODEL", ""), Double.parseDouble(setting("AI_VISUAL_TEMPERATURE", ".4")),
                Duration.ofMillis(number("AI_VISUAL_READ_TIMEOUT_MS", 240000)), Boolean.parseBoolean(setting("AI_VISUAL_STRICT_STRUCTURED_OUTPUT", "true")),
                Boolean.parseBoolean(setting("AI_VISUAL_SUPPORTS_RESPONSE_FORMAT", "true")), (int) number("AI_VISUAL_MAX_COMPLETION_TOKENS", 32000),
                setting("AI_VISUAL_REASONING_EFFORT", "low")));
        String timeout = setting("JEV_TIMEOUT", "60s");
        var jev = new JevProperties(setting("JEV_API_KEY", ""), URI.create(setting("JEV_BASE_URL", "")), setting("JEV_MODEL", ""),
                timeout.endsWith("s") ? Duration.ofSeconds(Long.parseLong(timeout.substring(0, timeout.length() - 1))) : Duration.parse(timeout),
                Double.parseDouble(setting("JEV_MINIMUM_CONFIDENCE", ".25")), Double.parseDouble(setting("JEV_MINIMUM_MARGIN", ".08")),
                (int) number("JEV_MAXIMUM_QUERY_CHARACTERS", 20000), (int) number("JEV_MAXIMUM_PROMPT_CHARACTERS", 80000));
        var schemas = mock(SchemaDefinitionService.class);
        List<SchemaVersion> catalogue = new ArrayList<>();
        for (JsonNode item : SchemaCatalogCompiler.loadFromClasspath(json)) {
            var schema = new SchemaVersion(); schema.setSchemaId(item.path("schemaId").asText()); schema.setVersion(item.path("version").asText());
            schema.setName(item.path("name").asText()); schema.setTopic(item.path("topic").asText()); schema.setDefinition(item.path("definition"));
            catalogue.add(schema);
        }
        when(schemas.approvedSchemas()).thenReturn(catalogue);
        when(schemas.requireCurrentApproved(anyString(), anyString())).thenAnswer(call -> catalogue.stream()
                .filter(s -> s.getSchemaId().equals(call.getArgument(0)) && s.getVersion().equals(call.getArgument(1))).findFirst().orElseThrow());
        var runtime = new SchemaEquationRuntime(json, .02, 3.2e9, 16384, 256, 32, 512);
        var service = new AIService(schemas, runtime, mock(GeneratedSimulationStorage.class), jev, json, ai, new AIClient(ai, jev, json),
                new UploadProperties(10485760, Set.of("image/png")), setting("JWT_SECRET", ""), (int) number("SIMULATION_MAX_CODE_CHARACTERS", 250000));
        Path output = Path.of("..", "tmp", "live-group-field"); Files.createDirectories(output);
        boolean fixturesOnly = "true".equals(System.getenv("LIVE_VISUAL_FIXTURES_ONLY"));
        var summary = fixturesOnly && Files.exists(output.resolve("summary.json"))
                ? (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(output.resolve("summary.json").toFile())
                : json.createObjectNode().put("textModel", ai.provider().textModel()).put("visualModel", ai.visual().model())
                .put("usesRealProviders", true).put("catalogueSource", "checked-in resources; database adapter mocked; no writes");
        var cases = fixturesOnly ? (com.fasterxml.jackson.databind.node.ArrayNode) summary.path("cases") : summary.putArray("cases");
        String[] descriptions = fixturesOnly ? new String[0] : new String[]{"100 hạt phân rã ngẫu nhiên", "giao thoa hai nguồn"};
        for (int i = 0; i < descriptions.length; i++) {
            String id = i == 0 ? "decay" : "interference";
            var report = cases.addObject().put("id", id).put("description", descriptions[i]);
            long start = System.nanoTime();
            try {
                System.out.println("LIVE_BEGIN " + id);
                JsonNode intent = service.understandText(descriptions[i], null, null, null);
                json.writerWithDefaultPrettyPrinter().writeValue(output.resolve(id + "-intent.json").toFile(), intent);
                report.put("stage", intent.path("stage").asText()); report.set("validation", intent.path("validation"));
                if ("EXPLAIN".equals(intent.path("stage").asText())) {
                    JsonNode generated = service.generate(intent);
                    json.writerWithDefaultPrettyPrinter().writeValue(output.resolve(id + "-generated.json").toFile(), generated);
                    Files.writeString(output.resolve(id + "-visual.js"), generated.path("simulationSpec").path("visualProgram").path("code").asText());
                    report.put("generated", true).put("frames", generated.path("simulationSpec").path("solverTimeline").path("frames").size());
                    report.set("objects", generated.path("simulationSpec").path("objects"));
                    report.set("validation", generated.path("validation"));
                } else {report.put("generated", false).set("question", intent.path("question"));}
            } catch (Exception failure) {
                report.put("generated", false).put("errorType", failure.getClass().getSimpleName()).put("error", failure.getMessage());
                System.out.println("LIVE_FAILURE " + id + " " + failure.getClass().getSimpleName());
            }
            report.put("elapsedSeconds", (System.nanoTime() - start) / 1e9);
            json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("summary.json").toFile(), summary);
            System.out.println("LIVE_END " + id + " generated=" + report.path("generated").asBoolean());
        }
        var fixtures = summary.putArray("visualFixtures");
        for (String id : List.of("decay", "waves")) {
            var report = fixtures.addObject().put("id", id).put("source", "law-cases/flex__" + id + ".json; verified fixture, not free-text interpretation");
            long start = System.nanoTime();
            try {
                System.out.println("LIVE_VISUAL_BEGIN " + id);
                JsonNode spec;
                try (var stream = getClass().getResourceAsStream("/law-cases/flex__" + id + ".json")) { spec = json.readTree(stream).path("simulationSpec").deepCopy(); }
                var projection = new FreeModel(json, runtime);
                projection.project((com.fasterxml.jackson.databind.node.ObjectNode) spec);
                var schema = catalogue.stream().filter(s -> s.getSchemaId().equals("decay".equals(id) ? "thpt_nuclear_physics" : "thpt_waves")).findFirst().orElseThrow();
                var intent = json.createObjectNode().put("stage", "EXPLAIN").put("schemaId", schema.getSchemaId()).put("schemaVersion", schema.getVersion())
                        .put("description", "decay".equals(id) ? "Nhóm 200 hạt phân rã ngẫu nhiên; dùng đúng mô hình kiểm thử đính kèm" : "Giao thoa hai nguồn sóng nước; dùng đúng mô hình kiểm thử đính kèm");
                intent.set("simulationSpec", spec);
                intent.put("planSignature", (String) ReflectionTestUtils.invokeMethod(service, "signPlan", intent));
                JsonNode generated = service.generate(intent);
                json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("fixture-" + id + "-generated.json").toFile(), generated);
                Files.writeString(output.resolve("fixture-" + id + "-visual.js"), generated.path("simulationSpec").path("visualProgram").path("code").asText());
                report.put("generated", true).put("frames", generated.path("simulationSpec").path("solverTimeline").path("frames").size());
                report.set("objects", generated.path("simulationSpec").path("objects")); report.set("validation", generated.path("validation"));
            } catch (Exception failure) {
                report.put("generated", false).put("errorType", failure.getClass().getSimpleName()).put("error", failure.getMessage());
            }
            report.put("elapsedSeconds", (System.nanoTime() - start) / 1e9);
            json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("summary.json").toFile(), summary);
            System.out.println("LIVE_VISUAL_END " + id + " generated=" + report.path("generated").asBoolean());
        }
    }
}
