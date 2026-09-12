package com.example.backend.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class RuleBasedExtractionProviderTest {

    private RuleBasedExtractionProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RuleBasedExtractionProvider(new UnitNormalizer(), new ObjectMapper());
    }

    @Test
    void extractsAndNormalizesKinematicsProblem() {
        SpecificationDocument document = provider
                .extract("Một xe chuyển động với vận tốc 36 km/h trong 5 s.")
                .document();

        assertThat(document.topic()).isEqualTo("KINEMATICS");
        assertThat(document.quantities()).hasSize(2);
        assertThat(document.quantities())
                .anySatisfy(quantity -> {
                    assertThat(quantity.normalizedUnit()).isEqualTo("m/s");
                    assertThat(quantity.normalizedValue()).isEqualByComparingTo("10");
                });
        assertThat(document.ambiguities()).isEmpty();
    }

    @Test
    void identifiesDynamicsAndCircuits() {
        SpecificationDocument dynamics = provider.extract("Một vật khối lượng 2 kg chịu lực 10 N.").document();
        SpecificationDocument circuits = provider.extract("Mạch có điện áp 12 V và dòng điện 2 A.").document();

        assertThat(dynamics.topic()).isEqualTo("DYNAMICS");
        assertThat(circuits.topic()).isEqualTo("CIRCUITS");
    }

    @Test
    void createsAmbiguitiesWhenTopicAndQuantityAreMissing() {
        SpecificationDocument document = provider.extract("Hãy phân tích hiện tượng này.").document();

        assertThat(document.topic()).isNull();
        assertThat(document.ambiguities())
                .extracting(AmbiguityItem::code)
                .containsExactlyInAnyOrder("topic.unknown", "quantity.missing");
    }
}
