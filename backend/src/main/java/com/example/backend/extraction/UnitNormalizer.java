package com.example.backend.extraction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class UnitNormalizer {

    private record UnitDefinition(String canonical, BigDecimal factor) {
    }

    private static final Map<String, UnitDefinition> UNITS = Map.ofEntries(
            Map.entry("m", new UnitDefinition("m", BigDecimal.ONE)),
            Map.entry("cm", new UnitDefinition("m", new BigDecimal("0.01"))),
            Map.entry("mm", new UnitDefinition("m", new BigDecimal("0.001"))),
            Map.entry("km", new UnitDefinition("m", new BigDecimal("1000"))),
            Map.entry("s", new UnitDefinition("s", BigDecimal.ONE)),
            Map.entry("ms", new UnitDefinition("s", new BigDecimal("0.001"))),
            Map.entry("min", new UnitDefinition("s", new BigDecimal("60"))),
            Map.entry("h", new UnitDefinition("s", new BigDecimal("3600"))),
            Map.entry("kg", new UnitDefinition("kg", BigDecimal.ONE)),
            Map.entry("g", new UnitDefinition("kg", new BigDecimal("0.001"))),
            Map.entry("n", new UnitDefinition("N", BigDecimal.ONE)),
            Map.entry("v", new UnitDefinition("V", BigDecimal.ONE)),
            Map.entry("a", new UnitDefinition("A", BigDecimal.ONE)),
            Map.entry("ohm", new UnitDefinition("ohm", BigDecimal.ONE)),
            Map.entry("ω", new UnitDefinition("ohm", BigDecimal.ONE)),
            Map.entry("m/s", new UnitDefinition("m/s", BigDecimal.ONE)),
            Map.entry("km/h", new UnitDefinition("m/s", new BigDecimal("0.2777777778"))),
            Map.entry("m/s2", new UnitDefinition("m/s2", BigDecimal.ONE)),
            Map.entry("m/s^2", new UnitDefinition("m/s2", BigDecimal.ONE)),
            Map.entry("m/s²", new UnitDefinition("m/s2", BigDecimal.ONE)));

    public NormalizedQuantity normalize(BigDecimal value, String originalUnit) {
        String key = normalizeKey(originalUnit);
        UnitDefinition definition = UNITS.get(key);
        if (definition == null) {
            return new NormalizedQuantity(value, originalUnit, value, originalUnit, false);
        }
        BigDecimal normalizedValue = value.multiply(definition.factor()).setScale(8, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return new NormalizedQuantity(value, originalUnit, normalizedValue, definition.canonical(), true);
    }

    public String normalizeKey(String unit) {
        if (unit == null) {
            return "";
        }
        String normalized = Normalizer.normalize(unit.trim().toLowerCase(), Normalizer.Form.NFKC)
                .replace("°", "")
                .replace(" ", "")
                .replace("²", "2")
                .replace("^2", "2")
                .replace("per", "/");
        if (normalized.equals("mps")) {
            return "m/s";
        }
        if (normalized.equals("kmph")) {
            return "km/h";
        }
        if (normalized.equals("n/m2") || normalized.equals("n/m²")) {
            return "n/m2";
        }
        if (normalized.equals("Ω")) {
            return "ω";
        }
        return normalized;
    }

    public record NormalizedQuantity(
            BigDecimal value,
            String originalUnit,
            BigDecimal normalizedValue,
            String normalizedUnit,
            boolean knownUnit) {
    }
}
