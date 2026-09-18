package com.example.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.backend.dto.problem.ResolveAmbiguityRequest;
import com.example.backend.dto.problem.SpecificationResponse;
import com.example.backend.dto.problem.ValidationReadinessResponse;
import com.example.backend.dto.problem.UpdateEndConditionRequest;
import com.example.backend.entity.AmbiguityCase;
import com.example.backend.entity.AmbiguityStatus;
import com.example.backend.entity.ConfirmationState;
import com.example.backend.entity.ReviewerDecision;
import com.example.backend.entity.Specification;
import com.example.backend.entity.SubmissionStatus;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.simulation.EndConditionResolver;
import com.example.backend.repository.AmbiguityCaseRepository;
import com.example.backend.repository.ReviewerDecisionRepository;
import com.example.backend.repository.SpecificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpecificationService {

    private final SpecificationRepository specificationRepository;
    private final AmbiguityCaseRepository ambiguityRepository;
    private final ReviewerDecisionRepository decisionRepository;
    private final CurrentUserService currentUserService;
    private final ProblemResponseMapper mapper;
    private final SpecificationReadinessChecker readinessChecker;
    private final EndConditionResolver endConditionResolver;

    @Transactional(readOnly = true)
    public SpecificationResponse get(UUID id) {
        return mapper.toSpecification(requireOwnedSpecification(id));
    }

    @Transactional
    public SpecificationResponse resolve(UUID specificationId, UUID ambiguityId, ResolveAmbiguityRequest request) {
        if (request == null || !StringUtils.hasText(request.answer())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ambiguity answer is required");
        }

        User actor = currentUserService.requireCurrentUser();
        Specification specification = specificationRepository.findByIdAndSubmissionOwner(specificationId, actor)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Specification not found"));
        AmbiguityCase ambiguity = ambiguityRepository.findByIdAndSpecification(ambiguityId, specification)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ambiguity case not found"));
        if (ambiguity.getStatus() != AmbiguityStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "Ambiguity case is already resolved");
        }

        Instant now = Instant.now();
        ambiguity.setResolution(request.answer().trim());
        ambiguity.setStatus(AmbiguityStatus.RESOLVED);
        ambiguity.setResolvedAt(now);

        ReviewerDecision decision = new ReviewerDecision();
        decision.setAmbiguityCase(ambiguity);
        decision.setActorId(actor.getId());
        decision.setActorRole(actor.getRole().getName());
        decision.setDecisionState("CONFIRMED");
        decision.setAnswer(request.answer().trim());
        decision.setComment(trimToNull(request.comment()));
        decision.setDecidedAt(now);
        decisionRepository.save(decision);

        boolean hasOpenCases = specification.getAmbiguityCases().stream()
                .anyMatch(item -> item.getStatus() == AmbiguityStatus.OPEN);
        if (!hasOpenCases) {
            specification.setConfirmationState(ConfirmationState.CONFIRMED);
            specification.getSubmission().setStatus(SubmissionStatus.READY_FOR_VALIDATION);
        }
        return mapper.toSpecification(specification);
    }

    @Transactional(readOnly = true)
    public ValidationReadinessResponse readiness(UUID id) {
        Specification specification = requireOwnedSpecification(id);
        List<String> blockers = readinessChecker.blockers(specification);
        return new ValidationReadinessResponse(specification.getId(), blockers.isEmpty(), List.copyOf(blockers));
    }

    @Transactional
    public SpecificationResponse updateEndCondition(UUID id, UpdateEndConditionRequest request) {
        if (request == null || request.endCondition() == null || request.endCondition().isNull()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End condition is required");
        }
        Specification specification = requireOwnedSpecification(id);
        try {
            endConditionResolver.requestedHorizon(request.endCondition(), java.math.BigDecimal.ONE);
        } catch (InvalidPhysicsInputException exception) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        }
        specification.setEndCondition(request.endCondition().deepCopy());
        return mapper.toSpecification(specificationRepository.save(specification));
    }

    private Specification requireOwnedSpecification(UUID id) {
        User owner = currentUserService.requireCurrentUser();
        return specificationRepository.findByIdAndSubmissionOwner(id, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Specification not found"));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
