package com.example.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.backend.entity.*;
import com.example.backend.entity.school.School;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.*;
import com.example.backend.service.school.LicenseCheckService;
import java.util.*;
import org.junit.jupiter.api.Test;

class B2bLegacyAccessTest {
    private User user(int id) {
        User u = new User(); u.setId(id); u.setEmail(id + "@test.example");
        Role r = new Role(); r.setName("STAFF"); u.setRole(r);
        School s = new School(); s.setId(UUID.randomUUID()); u.setSchool(s); return u;
    }
    @Test void privateLibraryCannotBeClonedByAnotherTeacher() {
        var repository = mock(LibraryItemRepository.class); var users = mock(UserRepository.class);
        var service = new LibraryService(repository, mock(SpecificationRepository.class), users);
        User actor = user(1); when(users.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        UUID id = UUID.randomUUID(); var item = new LibraryItem(); item.setOwnerId(2); item.setVisibility("PERSONAL");
        when(repository.findByIdAndActive(id, true)).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> service.cloneLibraryItem(id, actor.getEmail())).isInstanceOf(ApiException.class);
        verify(repository, never()).save(any());
    }
    @Test void legacyRunHistoryCannotReadAnotherTeachersSimulation() {
        var simulations = mock(SimulationRepository.class); var runs = mock(SimulationRunRepository.class);
        var current = mock(CurrentUserService.class);
        var service = new SimulationAttemptService(simulations, runs, current, new LicenseCheckService());
        when(current.requireCurrentUser()).thenReturn(user(1));
        var simulation = new Simulation(); simulation.setId(UUID.randomUUID()); simulation.setOwner(user(2));
        when(simulations.findById(simulation.getId())).thenReturn(Optional.of(simulation));
        assertThatThrownBy(() -> service.getSimulationRuns(simulation.getId())).isInstanceOf(ApiException.class);
        verifyNoInteractions(runs);
    }
    @Test void classroomSessionsCannotBeReadAcrossSchools() {
        var sessions = mock(ClassSessionRepository.class); var users = mock(UserRepository.class);
        var service = new ClassSessionService(sessions, mock(SpecificationRepository.class), users);
        User actor = user(1); User other = user(2);
        when(users.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        var session = new ClassSession(); session.setId(UUID.randomUUID()); session.setTeacherId(other.getId()); session.setTeacher(other);
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessions.findAll()).thenReturn(List.of(session));
        assertThatThrownBy(() -> service.getSessionById(session.getId(), actor.getEmail())).isInstanceOf(ApiException.class);
        actor.getRole().setName("STUDENT");
        assertThat(service.getSessions(actor.getEmail())).isEmpty();
    }
}
