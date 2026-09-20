package com.example.backend.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class ExternalDatabaseSafetyTest {
    @ParameterizedTest
    @ValueSource(strings = {"create", "create-drop"})
    void refusesDestructiveSchemaBeforeReadingExternalConnection(String action) {
        var environment = new MockEnvironment().withProperty("spring.jpa.hibernate.ddl-auto", action);
        assertThatThrownBy(() -> new DatabaseConfig().dataSource(environment))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Destructive Hibernate");
    }
}
