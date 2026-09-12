package com.example.backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.AmbiguityCaseResponse;
import com.example.backend.dto.ReviewDecisionRequest;
import com.example.backend.dto.ReviewerDecisionResponse;
import com.example.backend.entity.AmbiguityCase;
import com.example.backend.entity.ReviewerDecision;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.AmbiguityCaseRepository;
import com.example.backend.repository.ReviewerDecisionRepository;
import com.example.backend.repository.UserRepository;

import lombok.AllArgsConstructor;

/**
 * Service for Physics Content Reviewer to inspect and adjudicate flagged ambiguities
 * escalated from the problem understanding pipeline.
 */
@Service
@AllArgsConstructor
public class ReviewerAmbiguityService {

    private final AmbiguityCaseRepository ambiguityCaseRepository;
    private final ReviewerDecisionRepository reviewerDecisionRepository;
    private final UserRepository userRepository;

    public List<AmbiguityCaseResponse> getAmbiguityCases(String status) {
        List<AmbiguityCase> list;
        if (status != null && !status.trim().isEmpty()) {
            list = ambiguityCaseRepository.findByStatus(status.trim().toUpperCase());
        } else {
            list = ambiguityCaseRepository.findAllByOrderByCreatedAtDesc();
        }
        return list.stream().map(this::toCaseResponse).toList();
    }

    public AmbiguityCaseResponse getAmbiguityCaseById(UUID id) {
        AmbiguityCase entity = ambiguityCaseRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy ambiguity case với id: " + id));
        return toCaseResponse(entity);
    }

    @Transactional
    public ReviewerDecisionResponse adjudicate(UUID id, ReviewDecisionRequest request, String currentEmail) {
        AmbiguityCase ambiguityCase = ambiguityCaseRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy ambiguity case với id: " + id));

        User actor = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));

        String state = request.getDecisionState().trim().toUpperCase();
        if (!state.equals("RESOLVED") && !state.equals("REJECTED")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Quyết định phải là RESOLVED hoặc REJECTED");
        }

        // 1. Create ReviewerDecision record
        ReviewerDecision decision = new ReviewerDecision();
        decision.setAmbiguityCaseId(ambiguityCase.getId());
        decision.setActorId(actor.getId());
        decision.setActorRole(actor.getRole() != null ? actor.getRole().getName() : "REVIEWER");
        decision.setAnswer(request.getAnswer().trim());
        decision.setComment(request.getComment() != null ? request.getComment().trim() : null);
        decision.setDecisionState(state);
        decision.setDecidedAt(OffsetDateTime.now());

        reviewerDecisionRepository.save(decision);

        // 2. Update AmbiguityCase status and resolution
        ambiguityCase.setStatus(state);
        ambiguityCase.setResolution(request.getAnswer().trim());
        ambiguityCase.setResolvedAt(OffsetDateTime.now());
        ambiguityCaseRepository.save(ambiguityCase);

        return toDecisionResponse(decision);
    }

    public List<ReviewerDecisionResponse> getDecisionsByCase(UUID ambiguityCaseId) {
        return reviewerDecisionRepository.findByAmbiguityCaseIdOrderByDecidedAtDesc(ambiguityCaseId).stream()
                .map(this::toDecisionResponse)
                .toList();
    }

    private AmbiguityCaseResponse toCaseResponse(AmbiguityCase c) {
        return new AmbiguityCaseResponse(
                c.getId(),
                c.getSpecificationId(),
                c.getCode(),
                c.getFieldPath(),
                c.getQuestion(),
                c.getOptions(),
                c.getResolution(),
                c.getResolvedAt(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }

    private ReviewerDecisionResponse toDecisionResponse(ReviewerDecision d) {
        return new ReviewerDecisionResponse(
                d.getId(),
                d.getAmbiguityCaseId(),
                d.getActorId(),
                d.getActorRole(),
                d.getAnswer(),
                d.getComment(),
                d.getDecisionState(),
                d.getDecidedAt(),
                d.getCreatedAt());
    }
}
