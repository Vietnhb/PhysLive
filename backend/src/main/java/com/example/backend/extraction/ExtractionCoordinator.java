package com.example.backend.extraction;

import org.springframework.stereotype.Service;

import com.example.backend.entity.ExtractionOutcome;
import com.example.backend.entity.ExtractionPath;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExtractionCoordinator {

    private final OpenRouterExtractionProvider openRouter;
    private final RuleBasedExtractionProvider ruleBased;

    public ExtractionResult extract(String text) {
        if (openRouter.isAvailable()) {
            try {
                ProviderExtractionResult result = openRouter.extract(text);
                return new ExtractionResult(
                        result.document(),
                        ExtractionPath.OPENROUTER,
                        ExtractionOutcome.API_SUCCESS,
                        openRouter.providerName(),
                        openRouter.modelVersion(),
                        result.rawResponse(),
                        null);
            } catch (RuntimeException exception) {
                if (exception instanceof com.example.backend.exception.ApiException api
                        && (api.getStatus() == org.springframework.http.HttpStatus.FORBIDDEN
                            || api.getStatus() == org.springframework.http.HttpStatus.UNAUTHORIZED)) throw api;
                return fallback(text, "OpenRouter extraction failed; rule-based fallback was used.");
            }
        }
        return fallback(text, "OpenRouter is not configured; rule-based fallback was used.");
    }

    private ExtractionResult fallback(String text, String reason) {
        ProviderExtractionResult result = ruleBased.extract(text);
        return new ExtractionResult(
                result.document(),
                ExtractionPath.RULE_BASED,
                ExtractionOutcome.RULE_BASED_FALLBACK,
                ruleBased.providerName(),
                ruleBased.modelVersion(),
                result.rawResponse(),
                reason);
    }
}
