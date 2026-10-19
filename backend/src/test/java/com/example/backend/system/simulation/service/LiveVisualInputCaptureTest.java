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
class LiveVisualInputCaptureTest {
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, String> settings = new HashMap<>();
    private String setting(String name, String fallback) {
        return System.getenv().getOrDefault(name, settings.getOrDefault(name, fallback));
    }
    private long number(String name, long fallback) { return Long.parseLong(setting(name, String.valueOf(fallback))); }

    @Test
    void captureExactPendulumInput() throws Exception {
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
        Path output=Path.of("..","tmp",setting("CAPTURE_OUTPUT", "pendulum-visual-input")); Files.createDirectories(output);
        var client=spy(new AIClient(ai,jev,json));
        var textCall=new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(call -> {
            int index=textCall.incrementAndGet();
            json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("text-"+index+"-input.json").toFile(),call.getArgument(0));
            json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("text-input.json").toFile(),call.getArgument(0));
            java.util.function.Supplier<String> prompt=call.getArgument(1);
            Files.writeString(output.resolve("text-system-prompt.txt"),prompt.get());
            JsonNode response=(JsonNode)call.callRealMethod();
            json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("text-output.json").toFile(),response);
            json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("text-"+index+"-output.json").toFile(),response);
            return response;
        }).when(client).text(any(),any());
        doAnswer(call -> {
            json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("visual-input.json").toFile(),call.getArgument(0));
            Files.writeString(output.resolve("visual-system-prompt.txt"),(String)call.getArgument(1));
            json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("visual-response-schema.json").toFile(),call.getArgument(2));
            throw new IllegalStateException("VISUAL_INPUT_CAPTURED");
        }).when(client).visual(any(),anyString(),any());
        var service=new AIService(schemas,runtime,mock(GeneratedSimulationStorage.class),jev,json,ai,client,
            new UploadProperties(10485760,Set.of("image/png")),setting("JWT_SECRET",""),(int)number("SIMULATION_MAX_CODE_CHARACTERS",250000));
        if ("true".equals(System.getenv("CAPTURE_REPLAY_ONLY"))) {
            var spec=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(output.resolve("text-output.json").toFile()).path("simulationSpec").deepCopy();
            var laws=(JsonNode)ReflectionTestUtils.invokeMethod(service,"catalogue");
            var replay=json.createObjectNode().put("source","unchanged last captured text-output.json; no new provider call");
            try {
                new FreeModel(json,runtime).tidy(spec,laws);
                new FreeModel(json,runtime).project(spec);
                new FreeModel(json,runtime).compute(laws,spec,json.createObjectNode());
                replay.put("computed",true);
            } catch(Exception failure) {
                replay.put("computed",false).put("error",failure.getMessage());
                var buffer=new java.io.StringWriter(); failure.printStackTrace(new java.io.PrintWriter(buffer));
                Files.writeString(output.resolve("last-model-failure.txt"),buffer.toString());
            }
            json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("last-model-replay.json").toFile(),replay);
            System.out.println("REPLAY_RESULT "+replay); return;
        }
        String description="3 con lắc đơn có chiều dài dây 1.5 m, vật nặng 0.5 kg được kéo lệch góc 30 độ so với phương thẳng đứng rồi thả nhẹ không vận tốc đầu.";
        Files.writeString(output.resolve("description.txt"),description);
        var report=json.createObjectNode().put("description",description).put("textProviderReal",true)
            .put("catalogueSource","checked-in catalogue; database adapter mocked; no writes")
            .put("visualProviderCalled",false).put("visualInputCaptured",false);
        try {
            JsonNode intent=service.understandText(description,null,null,null);
            json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("intent.json").toFile(),intent);
            report.put("stage",intent.path("stage").asText());
            if ("EXPLAIN".equals(intent.path("stage").asText())) service.generate(intent);
            else report.set("question",intent.path("question"));
        } catch(Exception failure) {
            boolean captured="VISUAL_INPUT_CAPTURED".equals(failure.getMessage());
            report.put("visualInputCaptured",captured);
            if(!captured) {
                report.put("errorType",failure.getClass().getSimpleName()).put("error",failure.getMessage());
                var buffer=new java.io.StringWriter(); failure.printStackTrace(new java.io.PrintWriter(buffer));
                Files.writeString(output.resolve("failure.txt"),buffer.toString());
            }
        }
        json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("summary.json").toFile(),report);
        System.out.println("CAPTURE_RESULT "+report);
    }
}
