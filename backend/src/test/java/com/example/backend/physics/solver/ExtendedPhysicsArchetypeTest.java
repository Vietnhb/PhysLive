package com.example.backend.physics.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.PhysicsValidationStatus;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.solver.numerical.ElasticCollisionNumericalSolver;
import com.example.backend.physics.solver.numerical.ProjectileMotionNumericalSolver;
import com.example.backend.physics.solver.numerical.RcChargingNumericalSolver;
import com.example.backend.physics.solver.numerical.RcDischargingNumericalSolver;
import com.example.backend.physics.solver.numerical.SpringOscillationNumericalSolver;
import com.example.backend.physics.solver.reference.ElasticCollisionReferenceSolver;
import com.example.backend.physics.solver.reference.ProjectileMotionReferenceSolver;
import com.example.backend.physics.solver.reference.RcChargingReferenceSolver;
import com.example.backend.physics.solver.reference.RcDischargingReferenceSolver;
import com.example.backend.physics.solver.reference.SpringOscillationReferenceSolver;
import com.example.backend.physics.validation.ResultComparator;
import com.example.backend.physics.validation.ValidationTolerancePolicy;

class ExtendedPhysicsArchetypeTest {
    private final NumericalSolverLimits limits = new NumericalSolverLimits();
    private final ResultComparator comparator = new ResultComparator(
            new ArchetypeCatalog(), new ValidationTolerancePolicy());

    @Test
    void projectileNumericalMatchesReference() {
        SolverInput input = input(PhysicsArchetype.PROJECTILE_MOTION, Map.of(
                "initialPosition", d("0"), "initialHeight", d("2"),
                "initialVelocity", d("10"), "angle", d("0.6"),
                "gravity", d("9.80665"), "duration", d("2"), "deltaTime", d("0.01")),
                Map.of("initialPosition", "m", "initialHeight", "m", "initialVelocity", "m/s",
                        "angle", "rad", "gravity", "m/s2", "duration", "s", "deltaTime", "s"));
        assertPassed(input, new ProjectileMotionNumericalSolver(limits), new ProjectileMotionReferenceSolver());
    }

    @Test
    void collisionNumericalMatchesReference() {
        SolverInput input = input(PhysicsArchetype.ELASTIC_COLLISION, Map.of(
                "mass1", d("2"), "mass2", d("3"), "initialPosition1", d("0"),
                "initialPosition2", d("6"), "velocity1", d("3"), "velocity2", d("1"),
                "duration", d("4"), "deltaTime", d("0.01")),
                Map.of("mass1", "kg", "mass2", "kg", "initialPosition1", "m",
                        "initialPosition2", "m", "velocity1", "m/s", "velocity2", "m/s",
                        "duration", "s", "deltaTime", "s"));
        assertPassed(input, new ElasticCollisionNumericalSolver(limits), new ElasticCollisionReferenceSolver());
    }

    @Test
    void springNumericalMatchesReference() {
        SolverInput input = input(PhysicsArchetype.SPRING_OSCILLATION, Map.of(
                "amplitude", d("1"), "mass", d("2"), "springConstant", d("8"),
                "phase", d("0.2"), "duration", d("1"), "deltaTime", d("0.001")),
                Map.of("amplitude", "m", "mass", "kg", "springConstant", "N/m",
                        "phase", "rad", "duration", "s", "deltaTime", "s"));
        assertPassed(input, new SpringOscillationNumericalSolver(limits), new SpringOscillationReferenceSolver());
    }

    @Test
    void rcChargingAndDischargingMatchReferences() {
        Map<String, BigDecimal> values = Map.of("voltage", d("12"), "resistance", d("10"),
                "capacitance", d("0.1"), "duration", d("2"), "deltaTime", d("0.01"));
        Map<String, String> units = Map.of("voltage", "V", "resistance", "ohm", "capacitance", "F",
                "duration", "s", "deltaTime", "s");
        SolverInput charging = input(PhysicsArchetype.RC_CHARGING, values, units);
        SolverInput discharging = input(PhysicsArchetype.RC_DISCHARGING, values, units);
        assertPassed(charging, new RcChargingNumericalSolver(limits), new RcChargingReferenceSolver());
        assertPassed(discharging, new RcDischargingNumericalSolver(limits), new RcDischargingReferenceSolver());
    }

    private void assertPassed(SolverInput input, NumericalSolver numerical, ReferenceSolver reference) {
        assertThat(comparator.compare(input.archetype(), numerical.solve(input), reference.solve(input)).status())
                .isEqualTo(PhysicsValidationStatus.PASSED);
    }

    private SolverInput input(PhysicsArchetype archetype, Map<String, BigDecimal> values,
            Map<String, String> units) {
        return new SolverInput(null, "1.0", archetype, values, Map.of(), units);
    }

    private BigDecimal d(String value) { return new BigDecimal(value); }
}
