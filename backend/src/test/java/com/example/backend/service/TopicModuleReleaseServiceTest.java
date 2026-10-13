package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.backend.dto.release.CreateTopicModuleReleaseRequest;
import com.example.backend.dto.release.TopicModuleReleaseResponse;
import com.example.backend.entity.ModuleReleaseStatus;
import com.example.backend.entity.Role;
import com.example.backend.entity.SchemaVersion;
import com.example.backend.entity.SolverType;
import com.example.backend.entity.SolverVersion;
import com.example.backend.entity.Topic;
import com.example.backend.entity.TopicModuleRelease;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.repository.SchemaVersionRepository;
import com.example.backend.repository.SolverVersionRepository;
import com.example.backend.repository.TopicModuleReleaseRepository;
import com.example.backend.repository.TopicRepository;
import com.example.backend.physics.solver.NumericalSolver;
import com.example.backend.physics.solver.NumericalSolverRegistry;
import com.example.backend.physics.solver.ReferenceSolver;
import com.example.backend.physics.solver.ReferenceSolverRegistry;

@ExtendWith(MockitoExtension.class)
class TopicModuleReleaseServiceTest {

    @Mock private TopicModuleReleaseRepository releaseRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private SchemaVersionRepository schemaVersionRepository;
    @Mock private SolverVersionRepository solverVersionRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private NumericalSolverRegistry numericalRegistry;
    @Mock private ReferenceSolverRegistry referenceRegistry;
    @Mock private NumericalSolver numericalImplementation;
    @Mock private ReferenceSolver referenceImplementation;

    private TopicModuleReleaseService service;
    private User reviewer;
    private User admin;
    private Topic topic;
    private SchemaVersion schema;
    private SolverVersion numerical;
    private SolverVersion reference;

    @BeforeEach
    void setUp() {
        service = new TopicModuleReleaseService(releaseRepository, topicRepository,
                schemaVersionRepository, solverVersionRepository, currentUserService,
                numericalRegistry, referenceRegistry);
        reviewer = user(10, "REVIEWER");
        admin = user(11, "MANAGER");
        topic = new Topic();
        topic.setId(UUID.randomUUID());
        topic.setName("KINEMATICS");
        topic.setSlug("kinematics");
        topic.setEnabled(true);
        schema = new SchemaVersion();
        schema.setId(UUID.randomUUID());
        schema.setVersion("1.0");
        schema.setActive(true);
        numerical = solver(SolverType.NUMERICAL, "uniform-motion-numerical-v1");
        reference = solver(SolverType.REFERENCE, "uniform-motion-reference-v1");
    }

    @Test
    void createsDraftWithVersionBindingsAndActorAudit() {
        CreateTopicModuleReleaseRequest request = request();
        when(currentUserService.requireAnyRole("REVIEWER", "MANAGER")).thenReturn(reviewer);
        when(topicRepository.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(releaseRepository.findByTopicAndReleaseVersion(topic, "uniform-v1")).thenReturn(Optional.empty());
        when(numericalRegistry.require(PhysicsArchetype.UNIFORM_MOTION)).thenReturn(numericalImplementation);
        when(referenceRegistry.require(PhysicsArchetype.UNIFORM_MOTION)).thenReturn(referenceImplementation);
        when(numericalImplementation.version()).thenReturn(numerical.getVersion());
        when(referenceImplementation.version()).thenReturn(reference.getVersion());
        when(schemaVersionRepository.findByVersion("1.0")).thenReturn(Optional.of(schema));
        when(solverVersionRepository.findByArchetypeAndSolverTypeAndVersion(
                PhysicsArchetype.UNIFORM_MOTION, SolverType.NUMERICAL, numerical.getVersion()))
                .thenReturn(Optional.of(numerical));
        when(solverVersionRepository.findByArchetypeAndSolverTypeAndVersion(
                PhysicsArchetype.UNIFORM_MOTION, SolverType.REFERENCE, reference.getVersion()))
                .thenReturn(Optional.of(reference));
        when(releaseRepository.save(any(TopicModuleRelease.class))).thenAnswer(call -> {
            TopicModuleRelease release = call.getArgument(0);
            release.setId(UUID.randomUUID());
            return release;
        });

        TopicModuleReleaseResponse response = service.createDraft(request);

        assertThat(response.status()).isEqualTo(ModuleReleaseStatus.DRAFT);
        assertThat(response.createdBy()).isEqualTo(reviewer.getId());
        assertThat(response.archetype()).isEqualTo(PhysicsArchetype.UNIFORM_MOTION);
        assertThat(response.schemaVersion()).isEqualTo("1.0");
    }

    @Test
    void bootstrapsSchemaAndSolverCatalogOnFreshDatabase() {
        when(currentUserService.requireAnyRole("REVIEWER", "MANAGER")).thenReturn(reviewer);
        when(topicRepository.findById(topic.getId())).thenReturn(Optional.of(topic));
        when(releaseRepository.findByTopicAndReleaseVersion(topic, "uniform-v1")).thenReturn(Optional.empty());
        when(numericalRegistry.require(PhysicsArchetype.UNIFORM_MOTION)).thenReturn(numericalImplementation);
        when(referenceRegistry.require(PhysicsArchetype.UNIFORM_MOTION)).thenReturn(referenceImplementation);
        when(numericalImplementation.version()).thenReturn(numerical.getVersion());
        when(referenceImplementation.version()).thenReturn(reference.getVersion());
        when(schemaVersionRepository.findByVersion("1.0")).thenReturn(Optional.empty());
        when(schemaVersionRepository.save(any(SchemaVersion.class))).thenAnswer(call -> call.getArgument(0));
        when(solverVersionRepository.findByArchetypeAndSolverTypeAndVersion(
                PhysicsArchetype.UNIFORM_MOTION, SolverType.NUMERICAL, numerical.getVersion()))
                .thenReturn(Optional.empty());
        when(solverVersionRepository.findByArchetypeAndSolverTypeAndVersion(
                PhysicsArchetype.UNIFORM_MOTION, SolverType.REFERENCE, reference.getVersion()))
                .thenReturn(Optional.empty());
        when(solverVersionRepository.save(any(SolverVersion.class))).thenAnswer(call -> call.getArgument(0));
        when(releaseRepository.save(any(TopicModuleRelease.class))).thenAnswer(call -> {
            TopicModuleRelease release = call.getArgument(0);
            release.setId(UUID.randomUUID());
            return release;
        });

        TopicModuleReleaseResponse response = service.createDraft(request());

        assertThat(response.schemaVersion()).isEqualTo("1.0");
        assertThat(response.numericalSolverVersion()).isEqualTo(numerical.getVersion());
        assertThat(response.referenceSolverVersion()).isEqualTo(reference.getVersion());
        verify(schemaVersionRepository).save(any(SchemaVersion.class));
        verify(solverVersionRepository, times(2)).save(any(SolverVersion.class));
    }

    @Test
    void transitionsDraftToApprovedAsReviewer() {
        TopicModuleRelease release = release(ModuleReleaseStatus.DRAFT);
        when(currentUserService.requireAnyRole("REVIEWER", "MANAGER")).thenReturn(reviewer);
        when(releaseRepository.findByIdForUpdate(release.getId())).thenReturn(Optional.of(release));
        when(topicRepository.findByIdForReleaseApproval(topic.getId())).thenReturn(Optional.of(topic));
        when(releaseRepository.existsByTopicAndStatusAndNumericalSolverVersionArchetype(
                topic, ModuleReleaseStatus.APPROVED, PhysicsArchetype.UNIFORM_MOTION)).thenReturn(false);
        when(releaseRepository.save(release)).thenReturn(release);

        TopicModuleReleaseResponse response = service.approve(release.getId());

        assertThat(response.status()).isEqualTo(ModuleReleaseStatus.APPROVED);
        assertThat(response.approvedBy()).isEqualTo(reviewer.getId());
        assertThat(response.approvedAt()).isNotNull();
    }

    @Test
    void transitionsApprovedToRetiredOnlyAsAdmin() {
        TopicModuleRelease release = release(ModuleReleaseStatus.APPROVED);
        when(currentUserService.requireAnyRole("MANAGER")).thenReturn(admin);
        when(releaseRepository.findByIdForUpdate(release.getId())).thenReturn(Optional.of(release));
        when(releaseRepository.save(release)).thenReturn(release);

        TopicModuleReleaseResponse response = service.retire(release.getId());

        assertThat(response.status()).isEqualTo(ModuleReleaseStatus.RETIRED);
        assertThat(response.retiredBy()).isEqualTo(admin.getId());
        assertThat(response.retiredAt()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = ModuleReleaseStatus.class, names = {"APPROVED", "RETIRED"})
    void rejectsApproveUnlessCurrentStateIsDraft(ModuleReleaseStatus status) {
        TopicModuleRelease release = release(status);
        when(currentUserService.requireAnyRole("REVIEWER", "MANAGER")).thenReturn(reviewer);
        when(releaseRepository.findByIdForUpdate(release.getId())).thenReturn(Optional.of(release));

        assertThatThrownBy(() -> service.approve(release.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("DRAFT");
        verify(releaseRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ModuleReleaseStatus.class, names = {"DRAFT", "RETIRED"})
    void rejectsRetireUnlessCurrentStateIsApproved(ModuleReleaseStatus status) {
        TopicModuleRelease release = release(status);
        when(currentUserService.requireAnyRole("MANAGER")).thenReturn(admin);
        when(releaseRepository.findByIdForUpdate(release.getId())).thenReturn(Optional.of(release));

        assertThatThrownBy(() -> service.retire(release.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("APPROVED");
        verify(releaseRepository, never()).save(any());
    }

    @Test
    void refusesSecondApprovedReleaseForSameTopicAndArchetype() {
        TopicModuleRelease release = release(ModuleReleaseStatus.DRAFT);
        when(currentUserService.requireAnyRole("REVIEWER", "MANAGER")).thenReturn(reviewer);
        when(releaseRepository.findByIdForUpdate(release.getId())).thenReturn(Optional.of(release));
        when(topicRepository.findByIdForReleaseApproval(topic.getId())).thenReturn(Optional.of(topic));
        when(releaseRepository.existsByTopicAndStatusAndNumericalSolverVersionArchetype(
                topic, ModuleReleaseStatus.APPROVED, PhysicsArchetype.UNIFORM_MOTION)).thenReturn(true);

        assertThatThrownBy(() -> service.approve(release.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Retire");
    }

    @Test
    void retiredReleaseCannotReturnToApproved() {
        TopicModuleRelease retired = release(ModuleReleaseStatus.RETIRED);
        when(currentUserService.requireAnyRole("REVIEWER", "MANAGER")).thenReturn(reviewer);
        when(releaseRepository.findByIdForUpdate(retired.getId())).thenReturn(Optional.of(retired));

        assertThatThrownBy(() -> service.approve(retired.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void resolvesOnlyExactApprovedCompatibleRelease() {
        TopicModuleRelease approved = release(ModuleReleaseStatus.APPROVED);
        when(releaseRepository.findApprovedCompatibleReleases(
                "KINEMATICS", "1.0", PhysicsArchetype.UNIFORM_MOTION,
                numerical.getVersion(), reference.getVersion())).thenReturn(List.of(approved));

        assertThat(service.requireApprovedCompatible(
                "KINEMATICS", "1.0", PhysicsArchetype.UNIFORM_MOTION,
                numerical.getVersion(), reference.getVersion())).isSameAs(approved);
    }

    @Test
    void reviewerCannotRetireRelease() {
        UUID releaseId = UUID.randomUUID();
        when(currentUserService.requireAnyRole("MANAGER"))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "Required role: MANAGER"));

        assertThatThrownBy(() -> service.retire(releaseId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("MANAGER");
        verify(releaseRepository, never()).findByIdForUpdate(any());
    }

    private CreateTopicModuleReleaseRequest request() {
        return new CreateTopicModuleReleaseRequest(
                topic.getId(), "uniform-v1", "1.0", PhysicsArchetype.UNIFORM_MOTION);
    }

    private TopicModuleRelease release(ModuleReleaseStatus status) {
        TopicModuleRelease release = new TopicModuleRelease();
        release.setId(UUID.randomUUID());
        release.setTopic(topic);
        release.setReleaseVersion("uniform-v1");
        release.setSchemaVersion(schema);
        release.setNumericalSolverVersion(numerical);
        release.setReferenceSolverVersion(reference);
        release.setStatus(status);
        release.setCreatedBy(reviewer.getId());
        return release;
    }

    private SolverVersion solver(SolverType type, String version) {
        SolverVersion solver = new SolverVersion();
        solver.setId(UUID.randomUUID());
        solver.setSolverType(type);
        solver.setArchetype(PhysicsArchetype.UNIFORM_MOTION);
        solver.setVersion(version);
        solver.setActive(true);
        return solver;
    }

    private User user(int id, String roleName) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
