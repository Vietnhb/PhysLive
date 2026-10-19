package com.example.backend.system.simulation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.problem.model.entity.ExtractionRun;
import com.example.backend.system.problem.model.entity.ProblemSubmission;
import com.example.backend.system.problem.model.entity.Specification;
import com.example.backend.system.simulation.model.entity.Simulation;
import com.example.backend.system.simulation.model.entity.SimulationRun;
import com.example.backend.system.simulation.repository.SimulationRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** A clone is the teacher's own chain of records, and changing it leaves the shared original as it was. */
class SimulationCopierTest {
    private final ObjectMapper json = new ObjectMapper();
    private final SimulationRunRepository runs = mock(SimulationRunRepository.class);
    private final SimulationCopier copier = new SimulationCopier(mock(EntityManager.class), runs);

    @Test
    void theCopyBelongsToTheTeacherAndIsIndependentOfTheOriginal() throws Exception {
        User author = new User(), teacher = new User();
        var submission = new ProblemSubmission();
        submission.setOwner(author);
        submission.setOriginalText("Hai xe va chạm");
        var specification = new Specification();
        specification.setSubmission(submission);
        specification.setExtractionRun(new ExtractionRun());
        specification.setSchemaId("thpt_momentum");
        specification.setQuantities(json.readTree("[{\"name\":\"m1\",\"value\":1}]"));
        var source = new Simulation();
        source.setId(UUID.randomUUID());
        source.setOwner(author);
        source.setSpecification(specification);
        source.setSchemaId("thpt_momentum");
        var latest = new SimulationRun();
        latest.setSimulation(source);
        latest.setValidationPassed(true);
        latest.setResult(json.readTree("{\"simulationSpec\":{\"visualProgram\":{\"code\":\"original\"}}}"));
        when(runs.findFirstBySimulationIdOrderByCreatedAtDesc(any())).thenReturn(Optional.of(latest));

        Simulation copy = copier.copyFor(teacher, source);

        assertSame(teacher, copy.getOwner());
        assertSame(teacher, copy.getSpecification().getSubmission().getOwner());
        assertNotSame(specification, copy.getSpecification());
        assertEquals("Hai xe va chạm", copy.getSpecification().getSubmission().getOriginalText());
        assertSame(copy, copy.getLatestRun().getSimulation());

        // the teacher changes the scene of the copy: the original keeps its own
        ((ObjectNode) copy.getLatestRun().getResult().path("simulationSpec").path("visualProgram")).put("code", "changed");
        ((ObjectNode) copy.getSpecification().getQuantities().get(0)).put("value", 2);
        assertEquals("original", latest.getResult().path("simulationSpec").path("visualProgram").path("code").asText());
        assertEquals(1, specification.getQuantities().get(0).path("value").asInt());
    }
}
