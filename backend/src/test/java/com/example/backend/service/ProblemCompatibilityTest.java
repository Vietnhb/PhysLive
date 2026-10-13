package com.example.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.backend.dto.problem.UpdateSpecificationRequest;
import com.example.backend.entity.*;
import com.example.backend.exception.ApiException;
import com.example.backend.extraction.*;
import com.example.backend.physics.simulation.EndConditionResolver;
import com.example.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProblemCompatibilityTest {
    private final ObjectMapper json = new ObjectMapper();
    private final ProblemSubmissionRepository problems = mock(ProblemSubmissionRepository.class);
    private final SpecificationRepository specifications = mock(SpecificationRepository.class);
    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final SpecificationService confirmations = mock(SpecificationService.class);
    private final User owner = new User();
    private final ProblemSubmission problem = new ProblemSubmission();
    private final Specification original = new Specification();
    private ProblemService service;

    @BeforeEach
    void setup() {
        owner.setId(5);
        problem.setId(UUID.randomUUID());
        problem.setOwner(owner);
        original.setId(UUID.randomUUID());
        original.setSubmission(problem);
        original.setObjects(json.createArrayNode());
        original.setQuantities(json.createArrayNode());
        original.setRelations(json.createArrayNode());
        original.setAmbiguity(json.createArrayNode());
        original.setValidationStatus("PASSED");
        original.setContractVersion("1.0");
        problem.setCurrentSpecification(original);
        when(currentUser.requireCurrentUser()).thenReturn(owner);
        when(problems.findByIdAndOwner(problem.getId(), owner)).thenReturn(Optional.of(problem));
        service = new ProblemService(problems, mock(SourceAssetRepository.class), mock(ExtractionRunRepository.class),
                specifications, mock(LessonRepository.class), currentUser, new ProblemResponseMapper(),
                mock(ExtractionCoordinator.class), mock(OcrProvider.class), json, confirmations, new EndConditionResolver());
    }

    @Test
    void editingCreatesUnvalidatedRevisionAndKeepsOpenAmbiguities() throws Exception {
        var ambiguity = new AmbiguityCase();
        ambiguity.setCode("direction");
        original.addAmbiguityCase(ambiguity);
        when(specifications.save(any())).thenAnswer(call -> call.getArgument(0));
        var quantities = json.readTree("[{\"name\":\"velocity\",\"normalizedValue\":10,\"normalizedUnit\":\"m/s\"}]");
        service.updateSpecification(problem.getId(), new UpdateSpecificationRequest(
                json.createArrayNode(), quantities, json.createArrayNode(), null));
        var revision = problem.getCurrentSpecification();
        assertThat(revision).isNotSameAs(original);
        assertThat(original.getQuantities()).isEmpty();
        assertThat(original.getValidationStatus()).isEqualTo("PASSED");
        assertThat(revision.getValidationStatus()).isEqualTo("NOT_VALIDATED");
        assertThat(revision.getValidationResult()).isNull();
        assertThat(revision.getConfirmationState()).isEqualTo(ConfirmationState.UNRESOLVED);
        assertThat(revision.getAmbiguityCases()).hasSize(1);
        assertThat(revision.getAmbiguityCases().getFirst()).isNotSameAs(ambiguity);
        assertThat(revision.getContractVersion()).isEqualTo("1.0");
    }

    @Test
    void invalidQuantitiesDoNotModifyTheSpecification() throws Exception {
        var request = new UpdateSpecificationRequest(json.createArrayNode(),
                json.readTree("[{\"name\":\"velocity\",\"normalizedValue\":\"bad\"}]"), json.createArrayNode(), null);
        assertThatThrownBy(() -> service.updateSpecification(problem.getId(), request)).isInstanceOf(ApiException.class);
        assertThat(problem.getCurrentSpecification()).isSameAs(original);
        verifyNoInteractions(specifications);
    }

    @Test
    void incompleteAnswersDoNotPartiallyConfirm() {
        for (String code : new String[]{"direction", "unit"}) {
            var ambiguity = new AmbiguityCase();
            ambiguity.setCode(code);
            original.addAmbiguityCase(ambiguity);
        }
        assertThatThrownBy(() -> service.confirm(problem.getId(), Map.of("direction", "left")))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(confirmations);
    }

    @Test
    void confirmationUsesExistingAuditedResolutionService() {
        var ambiguity = new AmbiguityCase();
        ambiguity.setId(UUID.randomUUID());
        ambiguity.setCode("direction");
        original.addAmbiguityCase(ambiguity);
        service.confirm(problem.getId(), Map.of("direction", "left"));
        verify(confirmations).resolve(original.getId(), ambiguity.getId(),
                new com.example.backend.dto.problem.ResolveAmbiguityRequest("left", null));
    }

    @Test
    void anotherOwnersProblemIsNotAccessible() {
        when(problems.findByIdAndOwner(problem.getId(), owner)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirm(problem.getId(), Map.of())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.updateSpecification(problem.getId(), null)).isInstanceOf(ApiException.class);
        verifyNoInteractions(specifications, confirmations);
    }

    @Test
    void responseKeepsContractAndValidationMetadata() {
        original.setSchemaId("kinematics");
        original.setValidationResult(json.createObjectNode().put("passed", true));
        var response = new ProblemResponseMapper().toSpecification(original);
        assertThat(response.contractVersion()).isEqualTo("1.0");
        assertThat(response.schemaId()).isEqualTo("kinematics");
        assertThat(response.validationStatus()).isEqualTo("PASSED");
        assertThat(response.validationResult().path("passed").asBoolean()).isTrue();
    }
}
