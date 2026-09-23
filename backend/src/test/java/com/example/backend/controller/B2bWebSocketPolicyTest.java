package com.example.backend.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.backend.dto.simulation.ParameterAdjustmentRequest;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.entity.school.School;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CurrentUserService;
import com.example.backend.service.PhysicsSimulationRunService;
import com.example.backend.service.school.LicenseCheckService;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class B2bWebSocketPolicyTest {
    @Test void expiryAfterHandshakeBlocksAdjustmentAndRestoresContext() {
        var users = mock(UserRepository.class); var runs = mock(PhysicsSimulationRunService.class);
        var controller = new SimulationWebSocketController(runs, new CurrentUserService(users), new LicenseCheckService());
        var school = new School(); school.setLicenseStart(LocalDate.now().minusDays(10)); school.setLicenseEnd(LocalDate.now().minusDays(1));
        var role = new Role(); role.setName("TEACHER");
        var user = new User(); user.setRole(role); user.setSchool(school);
        when(users.findByEmail("teacher@test.example")).thenReturn(Optional.of(user));
        var principal = new UsernamePasswordAuthenticationToken("teacher@test.example", null, List.of());
        var previous = SecurityContextHolder.getContext();
        assertThatThrownBy(() -> controller.adjust(new ParameterAdjustmentRequest(UUID.randomUUID(), Map.of(), Map.of()), principal))
                .isInstanceOf(ApiException.class).hasMessageContaining("license");
        assertThat(SecurityContextHolder.getContext()).isSameAs(previous);
        verifyNoInteractions(runs);
        SecurityContextHolder.clearContext();
    }
    @Test void anonymousSocketCannotAdjustSimulation() {
        var runs = mock(PhysicsSimulationRunService.class);
        var controller = new SimulationWebSocketController(runs, mock(CurrentUserService.class), new LicenseCheckService());
        assertThatThrownBy(() -> controller.adjust(new ParameterAdjustmentRequest(UUID.randomUUID(), Map.of(), Map.of()), null))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(runs);
    }
}
