package com.example.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.backend.dto.simulation.CreateSimulationRunRequest;
import com.example.backend.dto.simulation.SimulationRunResponse;
import com.example.backend.dto.simulation.SnapshotResponse;
import com.example.backend.dto.validation.ValidationRunResponse;
import com.example.backend.entity.ConfirmationState;
import com.example.backend.entity.ExtractionOutcome;
import com.example.backend.entity.ExtractionPath;
import com.example.backend.entity.ExtractionRun;
import com.example.backend.entity.ExtractionRunStatus;
import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.entity.ProblemSubmission;
import com.example.backend.entity.Role;
import com.example.backend.entity.SchemaVersion;
import com.example.backend.entity.SimulationRunStatus;
import com.example.backend.entity.SolverType;
import com.example.backend.entity.SolverVersion;
import com.example.backend.entity.SourceMode;
import com.example.backend.entity.Specification;
import com.example.backend.entity.SubmissionStatus;
import com.example.backend.entity.Topic;
import com.example.backend.entity.TopicModuleRelease;
import com.example.backend.entity.User;
import com.example.backend.entity.ValidationRunStatus;
import com.example.backend.extraction.PhysicalQuantity;
import com.example.backend.physics.adapter.SpecificationToSolverInputAdapter;
import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.simulation.SimulationParameterPolicy;
import com.example.backend.physics.simulation.SimulationPayloadBuilder;
import com.example.backend.physics.simulation.EndConditionResolver;
import com.example.backend.physics.solver.NumericalSolverLimits;
import com.example.backend.physics.solver.NumericalSolverRegistry;
import com.example.backend.physics.solver.ReferenceSolverRegistry;
import com.example.backend.physics.solver.numerical.ConstantAccelerationNumericalSolver;
import com.example.backend.physics.solver.numerical.HorizontalForceNumericalSolver;
import com.example.backend.physics.solver.numerical.InclinedPlaneNumericalSolver;
import com.example.backend.physics.solver.numerical.ParallelCircuitNumericalSolver;
import com.example.backend.physics.solver.numerical.SeriesCircuitNumericalSolver;
import com.example.backend.physics.solver.numerical.UniformMotionNumericalSolver;
import com.example.backend.physics.solver.reference.ConstantAccelerationReferenceSolver;
import com.example.backend.physics.solver.reference.HorizontalForceReferenceSolver;
import com.example.backend.physics.solver.reference.InclinedPlaneReferenceSolver;
import com.example.backend.physics.solver.reference.ParallelCircuitReferenceSolver;
import com.example.backend.physics.solver.reference.SeriesCircuitReferenceSolver;
import com.example.backend.physics.solver.reference.UniformMotionReferenceSolver;
import com.example.backend.physics.validation.ArchetypeInputValidator;
import com.example.backend.physics.validation.ResultComparator;
import com.example.backend.physics.validation.ValidationTolerancePolicy;
import com.example.backend.repository.SchemaVersionRepository;
import com.example.backend.repository.SimulationRepository;
import com.example.backend.repository.SimulationRunRepository;
import com.example.backend.repository.SnapshotRepository;
import com.example.backend.repository.SolverVersionRepository;
import com.example.backend.repository.SpecificationRepository;
import com.example.backend.repository.TopicModuleReleaseRepository;
import com.example.backend.repository.TopicRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.ValidationRunRepository;
import com.example.backend.service.CurrentUserService;
import com.example.backend.service.PhysicsSimulationRunService;
import com.example.backend.service.PhysicsSimulationService;
import com.example.backend.service.PhysicsValidationService;
import com.example.backend.service.SpecificationReadinessChecker;
import com.example.backend.service.TopicModuleReleaseService;
import com.fasterxml.jackson.databind.ObjectMapper;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:physics-workflow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PhysicsWorkflowPersistenceIntegrationTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private UserRepository userRepository;
    @Autowired private SpecificationRepository specificationRepository;
    @Autowired private ValidationRunRepository validationRunRepository;
    @Autowired private SchemaVersionRepository schemaVersionRepository;
    @Autowired private SolverVersionRepository solverVersionRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private TopicModuleReleaseRepository releaseRepository;
    @Autowired private SimulationRepository simulationRepository;
    @Autowired private SimulationRunRepository runRepository;
    @Autowired private SnapshotRepository snapshotRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PhysicsValidationService validationService;
    private PhysicsSimulationService simulationService;
    private PhysicsSimulationRunService runService;
    private User owner;

    @BeforeEach
    void setUp() {
        ArchetypeCatalog catalog = new ArchetypeCatalog();
        ArchetypeInputValidator validator = new ArchetypeInputValidator(catalog);
        NumericalSolverLimits limits = new NumericalSolverLimits();
        NumericalSolverRegistry numericalRegistry = new NumericalSolverRegistry(List.of(
                new UniformMotionNumericalSolver(limits),
                new ConstantAccelerationNumericalSolver(limits),
                new HorizontalForceNumericalSolver(limits),
                new InclinedPlaneNumericalSolver(limits),
                new SeriesCircuitNumericalSolver(),
                new ParallelCircuitNumericalSolver()), validator);
        ReferenceSolverRegistry referenceRegistry = new ReferenceSolverRegistry(List.of(
                new UniformMotionReferenceSolver(),
                new ConstantAccelerationReferenceSolver(),
                new HorizontalForceReferenceSolver(),
                new InclinedPlaneReferenceSolver(),
                new SeriesCircuitReferenceSolver(),
                new ParallelCircuitReferenceSolver()), validator);
        SpecificationToSolverInputAdapter adapter = new SpecificationToSolverInputAdapter(objectMapper, validator);
        CurrentUserService currentUserService = new CurrentUserService(userRepository);
        TopicModuleReleaseService releaseService = new TopicModuleReleaseService(
                releaseRepository, topicRepository, schemaVersionRepository, solverVersionRepository,
                currentUserService, numericalRegistry, referenceRegistry);
        validationService = new PhysicsValidationService(
                specificationRepository, validationRunRepository, currentUserService,
                new SpecificationReadinessChecker(adapter), adapter, numericalRegistry, referenceRegistry,
                new ResultComparator(catalog, new ValidationTolerancePolicy()), objectMapper, releaseService);
        simulationService = new PhysicsSimulationService(
                validationRunRepository, simulationRepository, currentUserService);
        runService = new PhysicsSimulationRunService(
                simulationRepository, runRepository, snapshotRepository, currentUserService,
                numericalRegistry, new SimulationParameterPolicy(catalog, validator, limits),
                new SimulationPayloadBuilder(objectMapper), new EndConditionResolver(), objectMapper);

        owner = persistOwner();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner.getEmail(), "ignored", List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readySpecificationFlowsThroughValidationSimulationRunSnapshotAndRetiredReplay() {
        Specification specification = persistReadySpecification(owner);
        TopicModuleRelease release = persistApprovedRelease(owner);
        entityManager.flush();
        entityManager.clear();

        ValidationRunResponse validation = validationService.validate(specification.getId());
        assertThat(validation.status()).isEqualTo(ValidationRunStatus.PASSED);
        assertThat(validation.moduleReleaseId()).isEqualTo(release.getId());
        assertThat(validation.numericalOutput()).isNotNull();
        assertThat(validation.referenceOutput()).isNotNull();

        PhysicsSimulationService.CreationResult creation =
                simulationService.createFromValidation(validation.id());
        assertThat(creation.created()).isTrue();

        SimulationRunResponse run = runService.create(
                creation.simulation().id(), new CreateSimulationRunRequest(Map.of(), Map.of()));
        assertThat(run.status()).isEqualTo(SimulationRunStatus.COMPLETED);
        assertThat(run.frameCount()).isGreaterThan(1);
        List<SnapshotResponse> originalReplay = runService.snapshots(run.id());
        assertThat(originalReplay).hasSize(run.frameCount());
        assertThat(originalReplay.get(0).sequenceNumber()).isZero();
        assertThat(originalReplay.get(originalReplay.size() - 1).simulationTime())
                .isEqualByComparingTo("1");

        TopicModuleRelease storedRelease = releaseRepository.findById(release.getId()).orElseThrow();
        storedRelease.setStatus(ModuleReleaseStatus.RETIRED);
        storedRelease.setRetiredAt(Instant.now());
        storedRelease.setRetiredBy(owner.getId());
        releaseRepository.saveAndFlush(storedRelease);
        entityManager.clear();

        SimulationRunResponse replayedRun = runService.get(run.id());
        List<SnapshotResponse> replayedSnapshots = runService.snapshots(run.id());
        assertThat(replayedRun.outputPayload().path("payloadVersion").asText())
                .isEqualTo(SimulationPayloadBuilder.PAYLOAD_VERSION);
        assertThat(replayedRun.outputPayload().path("summary").path("values").path("finalPosition")
                .decimalValue()).isEqualByComparingTo("2");
        assertThat(replayedSnapshots).hasSize(originalReplay.size());
        SnapshotResponse finalSnapshot = replayedSnapshots.get(replayedSnapshots.size() - 1);
        assertThat(finalSnapshot.simulationTime()).isEqualByComparingTo("1");
        assertThat(finalSnapshot.state().path("values").path("position").decimalValue())
                .isEqualByComparingTo("2");
    }

    private User persistOwner() {
        Role role = new Role();
        role.setName("MANAGER");
        entityManager.persist(role);
        User user = new User();
        user.setEmail("physics-owner@example.com");
        user.setPassword("not-used-in-test");
        user.setFullName("Physics Owner");
        user.setRole(role);
        return entityManager.persist(user);
    }

    private Specification persistReadySpecification(User user) {
        ProblemSubmission submission = new ProblemSubmission();
        submission.setOwner(user);
        submission.setSourceMode(SourceMode.TEXT);
        submission.setOriginalText("Vat chuyen dong voi van toc 2 m/s trong 1 s");
        submission.setEditableText(submission.getOriginalText());
        submission.setStatus(SubmissionStatus.READY_FOR_VALIDATION);
        entityManager.persist(submission);

        ExtractionRun extraction = new ExtractionRun();
        extraction.setSubmission(submission);
        extraction.setExtractionPath(ExtractionPath.RULE_BASED);
        extraction.setProviderName("m09-test-fixture");
        extraction.setModelVersion("fixture-v1");
        extraction.setStatus(ExtractionRunStatus.SUCCEEDED);
        extraction.setOutcome(ExtractionOutcome.API_SUCCESS);
        entityManager.persist(extraction);

        Specification specification = new Specification();
        specification.setSubmission(submission);
        specification.setExtractionRun(extraction);
        specification.setSchemaVersion("1.0");
        specification.setTopic("KINEMATICS");
        specification.setConfidence(new BigDecimal("0.9900"));
        specification.setObjects(objectMapper.createArrayNode());
        specification.setQuantities(objectMapper.valueToTree(List.of(
                quantity("velocity", "v", "2", "m/s"),
                quantity("time", "t", "1", "s"))));
        specification.setRelations(objectMapper.createArrayNode());
        specification.setAmbiguity(objectMapper.createArrayNode());
        specification.setConfirmationState(ConfirmationState.NO_AMBIGUITY);
        entityManager.persist(specification);
        submission.setCurrentSpecification(specification);
        entityManager.persist(submission);
        return specification;
    }

    private TopicModuleRelease persistApprovedRelease(User actor) {
        Topic topic = new Topic();
        topic.setName("Kinematics");
        topic.setSlug("kinematics");
        topic.setEnabled(true);
        topic.setSortOrder(1);
        entityManager.persist(topic);

        SchemaVersion schema = new SchemaVersion();
        schema.setVersion("1.0");
        schema.setDescription("M09 integration schema");
        schema.setActive(true);
        entityManager.persist(schema);

        SolverVersion numerical = solverVersion(SolverType.NUMERICAL, "uniform-motion-numerical-v1");
        SolverVersion reference = solverVersion(SolverType.REFERENCE, "uniform-motion-reference-v1");
        entityManager.persist(numerical);
        entityManager.persist(reference);

        TopicModuleRelease release = new TopicModuleRelease();
        release.setTopic(topic);
        release.setReleaseVersion("uniform-motion-release-v1");
        release.setSchemaVersion(schema);
        release.setNumericalSolverVersion(numerical);
        release.setReferenceSolverVersion(reference);
        release.setStatus(ModuleReleaseStatus.APPROVED);
        release.setCreatedBy(actor.getId());
        release.setApprovedBy(actor.getId());
        release.setApprovedAt(Instant.now());
        return entityManager.persist(release);
    }

    private SolverVersion solverVersion(SolverType type, String version) {
        SolverVersion solver = new SolverVersion();
        solver.setArchetype(PhysicsArchetype.UNIFORM_MOTION);
        solver.setSolverType(type);
        solver.setVersion(version);
        solver.setImplementationClass("m09-test-fixture");
        solver.setActive(true);
        return solver;
    }

    private PhysicalQuantity quantity(String name, String symbol, String value, String unit) {
        return new PhysicalQuantity(name, symbol, new BigDecimal(value), unit,
                new BigDecimal(value), unit, new BigDecimal("0.99"), value + " " + unit);
    }
}
