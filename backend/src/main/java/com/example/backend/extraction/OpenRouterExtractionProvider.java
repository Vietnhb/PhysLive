package com.example.backend.extraction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OpenRouterExtractionProvider implements ExtractionProvider {

    private static final String DEFAULT_MODEL = "nex-agi/nex-n2.5-pro:free";
    private static final String SYSTEM_PROMPT = """
            Extract a Vietnamese or English physics problem into JSON.
            Topics: KINEMATICS for motion; DYNAMICS for forces and mass; CIRCUITS for voltage, current and resistance.
            Use this exact shape:
            {"schemaVersion":"1.0","topic":"KINEMATICS|DYNAMICS|CIRCUITS|null",
            "objects":[{"id":"string","label":"string","type":"string"}],
            "quantities":[{"name":"string","symbol":"string|null","value":0,"originalUnit":"string",
            "normalizedValue":0,"normalizedUnit":"string","confidence":0.0,"sourceText":"string"}],
            "relations":[{"type":"string","subject":"string","object":"string|null","value":null,"unit":null,"sourceText":"string"}],
            "endCondition":{"type":"time_limit|threshold|event|cycle_count","duration":0,"maxTime":0,
            "quantity":"string","operator":">=|<=|>|<|=","value":0,
            "event":{"type":"contact|collision","entities":["string"]},"count":0}|null,
            "confidence":0.0,
            "ambiguities":[{"code":"string","fieldPath":"string","question":"string","options":["string"]}]}.
            Use null when unknown. Do not invent missing values. Return JSON only.
            """;

    private final OpenRouterClient client;
    private final ObjectMapper objectMapper;
    private final UnitNormalizer unitNormalizer;
    private final String model;

    public OpenRouterExtractionProvider(OpenRouterClient client, ObjectMapper objectMapper,
            UnitNormalizer unitNormalizer, Environment environment) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.unitNormalizer = unitNormalizer;
        this.model = environment.getProperty("OPENROUTER_MODEL", DEFAULT_MODEL).trim();
    }

    @Override
    public String providerName() {
        return "openrouter";
    }

    @Override
    public String modelVersion() {
        return model;
    }

    @Override
    public boolean isAvailable() {
        return client.isAvailable() && StringUtils.hasText(model);
    }

    @Override
    public ProviderExtractionResult extract(String text) {
        OpenRouterClient.Completion completion = client.complete(model, List.of(
                client.textMessage("system", SYSTEM_PROMPT),
                client.textMessage("user", text)));
        JsonNode json = client.parseJson(completion.content());
        try {
            SpecificationDocument parsed = objectMapper.treeToValue(json, SpecificationDocument.class);
            return new ProviderExtractionResult(normalize(parsed), completion.rawResponse());
        } catch (Exception exception) {
            throw new IllegalStateException("OpenRouter response does not match Specification v1.", exception);
        }
    }

    private SpecificationDocument normalize(SpecificationDocument document) {
        List<AmbiguityItem> ambiguities = new ArrayList<>(document.ambiguities());
        String topic = normalizeTopic(document.topic());
        if (topic == null && ambiguities.stream().noneMatch(item -> "topic".equals(item.fieldPath()))) {
            ambiguities.add(new AmbiguityItem(
                    "topic.unknown",
                    "topic",
                    "Problem này thuộc Kinematics, Dynamics hay Circuits?",
                    List.of("KINEMATICS", "DYNAMICS", "CIRCUITS")));
        }

        List<PhysicalQuantity> quantities = new ArrayList<>();
        for (int index = 0; index < document.quantities().size(); index++) {
            PhysicalQuantity quantity = document.quantities().get(index);
            if (quantity.value() == null || !StringUtils.hasText(quantity.originalUnit())) {
                ambiguities.add(new AmbiguityItem(
                        "quantity.incomplete",
                        "quantities[" + index + "]",
                        "Đại lượng chưa có đủ giá trị hoặc đơn vị. Vui lòng xác nhận.",
                        List.of()));
                quantities.add(quantity);
                continue;
            }
            UnitNormalizer.NormalizedQuantity normalized = unitNormalizer.normalize(
                    quantity.value(), quantity.originalUnit());
            quantities.add(new PhysicalQuantity(
                    quantity.name(),
                    quantity.symbol(),
                    quantity.value(),
                    quantity.originalUnit(),
                    normalized.normalizedValue(),
                    normalized.normalizedUnit(),
                    clamp(quantity.confidence()),
                    quantity.sourceText()));
            if (!normalized.knownUnit()) {
                ambiguities.add(new AmbiguityItem(
                        "unit.unknown",
                        "quantities[" + index + "].originalUnit",
                        "Đơn vị '" + quantity.originalUnit() + "' chưa được hỗ trợ. Vui lòng xác nhận.",
                        List.of()));
            }
        }

        return new SpecificationDocument(
                SpecificationDocument.CURRENT_SCHEMA_VERSION,
                topic,
                document.objects(),
                quantities,
                document.relations(),
                clamp(document.confidence()),
                ambiguities,
                document.endCondition());
    }

    private String normalizeTopic(String topic) {
        if (!StringUtils.hasText(topic)) {
            return null;
        }
        String normalized = topic.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "KINEMATICS", "DYNAMICS", "CIRCUITS" -> normalized;
            default -> null;
        };
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }
}
