package com.example.backend.extraction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RuleBasedExtractionProvider implements ExtractionProvider {

    private static final Pattern NUMBER_WITH_UNIT = Pattern.compile(
            "(?<![\\p{L}\\d])([-+]?\\d+(?:[.,]\\d+)?)\\s*(km/h|m/s\\s*2|m/s\\s*\\^\\s*2|m/s|cm|mm|km|ms|min|kg|g|ohm|Ω|n|v|a|m|s)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ASSIGNMENT_SYMBOL = Pattern.compile("(?i)(?:\\b([a-z][a-z0-9_]*)\\s*=\\s*)$");
    private static final Pattern OBJECT_PATTERN = Pattern.compile(
            "(?iu)(?:vật|xe|khối|mạch|điện trở|dây)\\s+([\\p{L}\\d_-]+)");

    private final UnitNormalizer unitNormalizer;
    private final ObjectMapper objectMapper;

    @Override
    public String providerName() {
        return "rule-based";
    }

    @Override
    public String modelVersion() {
        return "rule-based-v1";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ProviderExtractionResult extract(String text) {
        String normalizedText = text == null ? "" : text.trim();
        if (normalizedText.isBlank()) {
            throw new IllegalArgumentException("Problem text must not be blank");
        }

        String topic = detectTopic(normalizedText);
        List<PhysicalQuantity> quantities = extractQuantities(normalizedText);
        List<PhysicalObject> objects = extractObjects(normalizedText);
        List<AmbiguityItem> ambiguities = new ArrayList<>();

        if (topic == null) {
            ambiguities.add(new AmbiguityItem(
                    "topic.unknown",
                    "topic",
                    "Problem này thuộc Kinematics, Dynamics hay Circuits?",
                    List.of("KINEMATICS", "DYNAMICS", "CIRCUITS")));
        }
        if (quantities.isEmpty()) {
            ambiguities.add(new AmbiguityItem(
                    "quantity.missing",
                    "quantities",
                    "Chưa nhận diện được đại lượng và đơn vị. Vui lòng bổ sung hoặc xác nhận dữ liệu.",
                    List.of()));
        }

        BigDecimal confidence = calculateConfidence(topic, quantities, ambiguities);
        SpecificationDocument document = new SpecificationDocument(
                SpecificationDocument.CURRENT_SCHEMA_VERSION,
                topic,
                objects,
                quantities,
                inferRelations(normalizedText, quantities),
                confidence,
                ambiguities);
        return new ProviderExtractionResult(document, objectMapper.valueToTree(document));
    }

    private String detectTopic(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "mạch", "điện áp", "dòng điện", "điện trở", "ohm", "volt", "ampe")) {
            return "CIRCUITS";
        }
        if (containsAny(lower, "lực", "ma sát", "khối lượng", "newton", "trọng lực")) {
            return "DYNAMICS";
        }
        if (containsAny(lower, "chuyển động", "vận tốc", "tốc độ", "gia tốc", "quãng đường", "thời gian")) {
            return "KINEMATICS";
        }
        return null;
    }

    private List<PhysicalQuantity> extractQuantities(String text) {
        Map<String, PhysicalQuantity> quantities = new LinkedHashMap<>();
        Matcher matcher = NUMBER_WITH_UNIT.matcher(text);
        while (matcher.find()) {
            BigDecimal value = new BigDecimal(matcher.group(1).replace(',', '.'));
            String originalUnit = matcher.group(2).replaceAll("\\s+", "");
            UnitNormalizer.NormalizedQuantity normalized = unitNormalizer.normalize(value, originalUnit);
            String sourceText = matcher.group();
            String symbol = findSymbol(text, matcher.start());
            String name = nameForUnit(normalized.normalizedUnit(), symbol);
            String key = name + ":" + normalized.normalizedValue() + ":" + normalized.normalizedUnit();
            quantities.putIfAbsent(key, new PhysicalQuantity(
                    name,
                    symbol,
                    value,
                    originalUnit,
                    normalized.normalizedValue(),
                    normalized.normalizedUnit(),
                    normalized.knownUnit() ? new BigDecimal("0.95") : new BigDecimal("0.60"),
                    sourceText));
        }
        return List.copyOf(quantities.values());
    }

    private List<PhysicalObject> extractObjects(String text) {
        Map<String, PhysicalObject> objects = new LinkedHashMap<>();
        Matcher matcher = OBJECT_PATTERN.matcher(text);
        while (matcher.find()) {
            String label = matcher.group(1);
            String id = label.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-");
            objects.putIfAbsent(id, new PhysicalObject(id, label, "physical_object"));
        }
        return List.copyOf(objects.values());
    }

    private List<PhysicalRelation> inferRelations(String text, List<PhysicalQuantity> quantities) {
        if (quantities.size() >= 2 && text.toLowerCase(Locale.ROOT).contains("tỉ lệ")) {
            PhysicalQuantity first = quantities.get(0);
            PhysicalQuantity second = quantities.get(1);
            return List.of(new PhysicalRelation(
                    "PROPORTIONAL",
                    first.name(),
                    second.name(),
                    null,
                    null,
                    "tỉ lệ"));
        }
        return List.of();
    }

    private String findSymbol(String text, int valueStart) {
        String prefix = text.substring(Math.max(0, valueStart - 24), valueStart);
        Matcher matcher = ASSIGNMENT_SYMBOL.matcher(prefix);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String nameForUnit(String unit, String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            return symbol;
        }
        return switch (unit) {
            case "m" -> "distance";
            case "s" -> "time";
            case "kg" -> "mass";
            case "N" -> "force";
            case "V" -> "voltage";
            case "A" -> "current";
            case "ohm" -> "resistance";
            case "m/s" -> "velocity";
            case "m/s2" -> "acceleration";
            default -> "quantity";
        };
    }

    private BigDecimal calculateConfidence(String topic, List<PhysicalQuantity> quantities,
            List<AmbiguityItem> ambiguities) {
        BigDecimal confidence = new BigDecimal("0.35");
        if (topic != null) {
            confidence = confidence.add(new BigDecimal("0.30"));
        }
        if (!quantities.isEmpty()) {
            confidence = confidence.add(new BigDecimal("0.30"));
        }
        if (ambiguities.isEmpty()) {
            confidence = confidence.add(new BigDecimal("0.05"));
        }
        return confidence.min(BigDecimal.ONE).max(BigDecimal.ZERO);
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
