package com.example.backend.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.backend.entity.ExtractionOutcome;
import com.example.backend.entity.ExtractionPath;
import com.fasterxml.jackson.databind.ObjectMapper;

class ExtractionCoordinatorTest {

    private OpenRouterExtractionProvider openRouter;
    private ExtractionCoordinator coordinator;

    @BeforeEach
    void setUp() {
        openRouter = mock(OpenRouterExtractionProvider.class);
        RuleBasedExtractionProvider fallback = new RuleBasedExtractionProvider(new UnitNormalizer(), new ObjectMapper());
        coordinator = new ExtractionCoordinator(openRouter, fallback);
    }

    @Test
    void usesOpenRouterWhenProviderSucceeds() {
        SpecificationDocument document = new SpecificationDocument(
                "1.0", "KINEMATICS", List.of(), List.of(), List.of(), BigDecimal.ONE, List.of());
        when(openRouter.isAvailable()).thenReturn(true);
        when(openRouter.providerName()).thenReturn("openrouter");
        when(openRouter.modelVersion()).thenReturn("test-model");
        when(openRouter.extract("problem")).thenReturn(new ProviderExtractionResult(document, null));

        ExtractionResult result = coordinator.extract("problem");

        assertThat(result.path()).isEqualTo(ExtractionPath.OPENROUTER);
        assertThat(result.outcome()).isEqualTo(ExtractionOutcome.API_SUCCESS);
        assertThat(result.document()).isSameAs(document);
    }

    @Test
    void fallsBackWhenOpenRouterFails() {
        when(openRouter.isAvailable()).thenReturn(true);
        when(openRouter.extract("Xe chuyển động 10 m/s.")).thenThrow(new IllegalStateException("provider unavailable"));

        ExtractionResult result = coordinator.extract("Xe chuyển động 10 m/s.");

        assertThat(result.path()).isEqualTo(ExtractionPath.RULE_BASED);
        assertThat(result.outcome()).isEqualTo(ExtractionOutcome.RULE_BASED_FALLBACK);
        assertThat(result.document().topic()).isEqualTo("KINEMATICS");
    }
}
