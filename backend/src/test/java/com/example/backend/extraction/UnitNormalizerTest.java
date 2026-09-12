package com.example.backend.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class UnitNormalizerTest {

    private final UnitNormalizer normalizer = new UnitNormalizer();

    @Test
    void convertsKilometresPerHourToMetresPerSecond() {
        UnitNormalizer.NormalizedQuantity result = normalizer.normalize(new BigDecimal("36"), "km/h");

        assertThat(result.normalizedValue()).isEqualByComparingTo("10");
        assertThat(result.normalizedUnit()).isEqualTo("m/s");
        assertThat(result.knownUnit()).isTrue();
    }

    @Test
    void convertsGramsToKilograms() {
        UnitNormalizer.NormalizedQuantity result = normalizer.normalize(new BigDecimal("500"), "g");

        assertThat(result.normalizedValue()).isEqualByComparingTo("0.5");
        assertThat(result.normalizedUnit()).isEqualTo("kg");
    }

    @Test
    void preservesUnknownUnitsForHumanConfirmation() {
        UnitNormalizer.NormalizedQuantity result = normalizer.normalize(new BigDecimal("2"), "custom-unit");

        assertThat(result.normalizedValue()).isEqualByComparingTo("2");
        assertThat(result.normalizedUnit()).isEqualTo("custom-unit");
        assertThat(result.knownUnit()).isFalse();
    }

    @Test
    void convertsDegreesToRadians() {
        UnitNormalizer.NormalizedQuantity result = normalizer.normalize(new BigDecimal("30"), "°");

        assertThat(result.normalizedValue()).isEqualByComparingTo("0.52359878");
        assertThat(result.normalizedUnit()).isEqualTo("rad");
        assertThat(result.knownUnit()).isTrue();
    }

    @Test
    void recognizesUnicodeOhmAndSuperscriptAccelerationUnits() {
        UnitNormalizer.NormalizedQuantity resistance = normalizer.normalize(new BigDecimal("10"), "\u03a9");
        UnitNormalizer.NormalizedQuantity acceleration = normalizer.normalize(new BigDecimal("9.8"), "m/s\u00b2");

        assertThat(resistance.normalizedUnit()).isEqualTo("ohm");
        assertThat(resistance.knownUnit()).isTrue();
        assertThat(acceleration.normalizedUnit()).isEqualTo("m/s2");
        assertThat(acceleration.knownUnit()).isTrue();
    }
}
