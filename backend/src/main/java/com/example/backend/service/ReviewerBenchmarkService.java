package com.example.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.AdjudicateBenchmarkRequest;
import com.example.backend.dto.BenchmarkAdjudicationResponse;
import com.example.backend.dto.BenchmarkProblemRequest;
import com.example.backend.dto.BenchmarkProblemResponse;
import com.example.backend.entity.BenchmarkAdjudication;
import com.example.backend.entity.BenchmarkProblem;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.BenchmarkAdjudicationRepository;
import com.example.backend.repository.BenchmarkProblemRepository;

import lombok.AllArgsConstructor;

/**
 * Service for managing benchmark problems and recording adjudication of annotator disagreements.
 */
@Service
@AllArgsConstructor
public class ReviewerBenchmarkService {

    private final BenchmarkProblemRepository problemRepository;
    private final BenchmarkAdjudicationRepository adjudicationRepository;

    public List<BenchmarkProblemResponse> getBenchmarkProblems(String topic) {
        List<BenchmarkProblem> list;
        if (topic != null && !topic.trim().isEmpty()) {
            list = problemRepository.findByTopic(topic.trim().toLowerCase());
        } else {
            list = problemRepository.findAll();
        }
        return list.stream().map(this::toProblemResponse).toList();
    }

    public BenchmarkProblemResponse getBenchmarkProblemById(UUID id) {
        BenchmarkProblem entity = problemRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy benchmark problem với id: " + id));
        return toProblemResponse(entity);
    }

    @Transactional
    public BenchmarkProblemResponse createBenchmarkProblem(BenchmarkProblemRequest request) {
        BenchmarkProblem entity = new BenchmarkProblem();
        entity.setTopic(request.getTopic().trim().toLowerCase());
        entity.setGradeScope(request.getGradeScope().trim());
        entity.setSourceCategory(request.getSourceCategory().trim());
        entity.setProblemText(request.getProblemText().trim());
        entity.setActive(request.getActive() != null ? request.getActive() : true);

        problemRepository.save(entity);
        return toProblemResponse(entity);
    }

    @Transactional
    public BenchmarkAdjudicationResponse adjudicate(UUID problemId, AdjudicateBenchmarkRequest request) {
        BenchmarkProblem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy benchmark problem với id: " + problemId));

        BenchmarkAdjudication adjudication = new BenchmarkAdjudication();
        adjudication.setBenchmarkProblemId(problem.getId());
        adjudication.setReviewerReference(request.getReviewerReference().trim());
        adjudication.setDisagreementState(request.getDisagreementState().trim());
        adjudication.setResolvedSpecification(request.getResolvedSpecification());

        adjudicationRepository.save(adjudication);
        return toAdjudicationResponse(adjudication);
    }

    public List<BenchmarkAdjudicationResponse> getAdjudicationsByProblem(UUID problemId) {
        return adjudicationRepository.findByBenchmarkProblemId(problemId).stream()
                .map(this::toAdjudicationResponse)
                .toList();
    }

    private BenchmarkProblemResponse toProblemResponse(BenchmarkProblem p) {
        return new BenchmarkProblemResponse(
                p.getId(),
                p.getTopic(),
                p.getGradeScope(),
                p.getSourceCategory(),
                p.getProblemText(),
                p.isActive(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private BenchmarkAdjudicationResponse toAdjudicationResponse(BenchmarkAdjudication a) {
        return new BenchmarkAdjudicationResponse(
                a.getId(),
                a.getBenchmarkProblemId(),
                a.getReviewerReference(),
                a.getDisagreementState(),
                a.getResolvedSpecification(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
