package com.example.backend.system.simulation.service;

import com.example.backend.exception.ApiException;
import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.problem.model.entity.ExtractionRun;
import com.example.backend.system.problem.model.entity.ProblemSubmission;
import com.example.backend.system.problem.model.entity.Specification;
import com.example.backend.system.simulation.model.entity.Simulation;
import com.example.backend.system.simulation.model.entity.SimulationRun;
import com.example.backend.system.simulation.repository.SimulationRunRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A teacher's own copy of a shared simulation: the whole saved chain (submission, specification, simulation and its
 * latest run) belongs to the teacher, so the copy opens, changes and exports as theirs and never touches the original.
 */
@Service
@RequiredArgsConstructor
public class SimulationCopier {
    private final EntityManager entities;
    private final SimulationRunRepository runs;

    @Transactional
    public Simulation copyFor(User owner, Simulation source) {
        Specification from = source.getSpecification();
        ProblemSubmission original = from.getSubmission();

        var submission = new ProblemSubmission();
        submission.setOwner(owner);
        submission.setLesson(original.getLesson());
        submission.setSourceMode(original.getSourceMode());
        submission.setOriginalText(original.getOriginalText());
        submission.setEditableText(original.getEditableText());
        submission.setStatus(original.getStatus());
        entities.persist(submission);

        ExtractionRun originalExtraction = from.getExtractionRun();
        var extraction = new ExtractionRun();
        extraction.setSubmission(submission);
        if (originalExtraction != null) {
            extraction.setExtractionPath(originalExtraction.getExtractionPath());
            extraction.setProviderName(originalExtraction.getProviderName());
            extraction.setModelVersion(originalExtraction.getModelVersion());
            extraction.setStatus(originalExtraction.getStatus());
            extraction.setOutcome(originalExtraction.getOutcome());
        }
        entities.persist(extraction);

        var specification = new Specification();
        specification.setSubmission(submission);
        specification.setExtractionRun(extraction);
        specification.setSchemaId(from.getSchemaId());
        specification.setSchemaVersion(from.getSchemaVersion());
        specification.setContractVersion(from.getContractVersion());
        specification.setTopic(from.getTopic());
        specification.setConfidence(from.getConfidence());
        specification.setObjects(copy(from.getObjects()));
        specification.setQuantities(copy(from.getQuantities()));
        specification.setRelations(copy(from.getRelations()));
        specification.setEndCondition(copy(from.getEndCondition()));
        specification.setAmbiguity(copy(from.getAmbiguity()));
        specification.setConfirmationState(from.getConfirmationState());
        specification.setValidationStatus(from.getValidationStatus());
        specification.setValidationResult(copy(from.getValidationResult()));
        entities.persist(specification);
        submission.setCurrentSpecification(specification);

        var simulation = new Simulation();
        simulation.setOwner(owner);
        simulation.setSpecification(specification);
        simulation.setSchemaId(source.getSchemaId());
        simulation.setSolverVersion(source.getSolverVersion());
        simulation.setStatus(source.getStatus());
        entities.persist(simulation);

        SimulationRun latest = runs.findFirstBySimulationIdOrderByCreatedAtDesc(source.getId())
                .orElseThrow(() -> ApiException.notFound("Mô phỏng chia sẻ này chưa có dữ liệu đã lưu"));
        var run = new SimulationRun();
        run.setSimulation(simulation);
        run.setRunType(latest.getRunType());
        run.setSchemaId(latest.getSchemaId());
        run.setSchemaVersion(latest.getSchemaVersion());
        run.setBindingVersion(latest.getBindingVersion());
        run.setNumericalSolverId(latest.getNumericalSolverId());
        run.setReferenceSolverId(latest.getReferenceSolverId());
        run.setOutputContractVersion(latest.getOutputContractVersion());
        run.setOutputContractChecksum(latest.getOutputContractChecksum());
        run.setValidationPassed(latest.isValidationPassed());
        run.setValidationCheckpoints(copy(latest.getValidationCheckpoints()));
        run.setValidationError(latest.getValidationError());
        run.setDurationSeconds(latest.getDurationSeconds());
        run.setResult(copy(latest.getResult()));
        entities.persist(run);
        simulation.setLatestRun(run);
        entities.flush();
        return simulation;
    }

    private static JsonNode copy(JsonNode node) {
        return node == null ? null : node.deepCopy();
    }
}
