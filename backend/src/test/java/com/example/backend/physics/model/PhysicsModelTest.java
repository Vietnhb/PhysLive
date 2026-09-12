package com.example.backend.physics.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PhysicsModelTest {

    @Test
    void archetypesBelongToTheirExpectedTopics() {
        assertThat(PhysicsArchetype.UNIFORM_MOTION.belongsTo("kinematics")).isTrue();
        assertThat(PhysicsArchetype.HORIZONTAL_FORCE.belongsTo("DYNAMICS")).isTrue();
        assertThat(PhysicsArchetype.SERIES_CIRCUIT.belongsTo("CIRCUITS")).isTrue();
        assertThat(PhysicsArchetype.SERIES_CIRCUIT.belongsTo("KINEMATICS")).isFalse();
    }

    @Test
    void solverInputDefensivelyCopiesValuesAndUnits() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("v", new BigDecimal("10"));
        Map<String, String> units = new LinkedHashMap<>();
        units.put("v", "m/s");

        SolverInput input = new SolverInput(null, "1.0", PhysicsArchetype.UNIFORM_MOTION, values, units);
        values.put("t", new BigDecimal("5"));

        assertThat(input.values()).containsOnlyKeys("v");
        assertThatThrownBy(() -> input.values().put("t", BigDecimal.ONE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void solverResultDefensivelyCopiesFrames() {
        List<SimulationFrame> frames = new ArrayList<>();
        frames.add(new SimulationFrame(BigDecimal.ZERO, Map.of("position", BigDecimal.ZERO)));

        SolverResult result = new SolverResult(
                SolverMethod.NUMERICAL,
                "1.0.0",
                Map.of("finalPosition", new BigDecimal("50")),
                frames);
        frames.clear();

        assertThat(result.frames()).hasSize(1);
        assertThatThrownBy(() -> result.frames().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
