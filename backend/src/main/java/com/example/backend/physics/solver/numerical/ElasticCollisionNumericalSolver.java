package com.example.backend.physics.solver.numerical;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SimulationFrame;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.NumericalSolver;
import com.example.backend.physics.solver.NumericalSolverLimits;

@Component
public class ElasticCollisionNumericalSolver implements NumericalSolver {
    private static final String VERSION = "elastic-collision-numerical-v1";
    private final NumericalSolverLimits limits;
    public ElasticCollisionNumericalSolver(NumericalSolverLimits limits) { this.limits = limits; }
    @Override public PhysicsArchetype archetype() { return PhysicsArchetype.ELASTIC_COLLISION; }
    @Override public String version() { return VERSION; }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal m1 = NumericalSupport.required(input, "mass1");
        BigDecimal m2 = NumericalSupport.required(input, "mass2");
        BigDecimal x1 = NumericalSupport.required(input, "initialPosition1");
        BigDecimal x2 = NumericalSupport.required(input, "initialPosition2");
        BigDecimal v1 = NumericalSupport.required(input, "velocity1");
        BigDecimal v2 = NumericalSupport.required(input, "velocity2");
        BigDecimal collisionTime = collisionTime(x1, x2, v1, v2);
        BigDecimal duration = NumericalSupport.required(input, "duration");
        if (collisionTime.compareTo(duration) > 0) collisionTime = BigDecimal.ONE.negate();
        BigDecimal elapsed = BigDecimal.ZERO;
        boolean collided = false;
        List<SimulationFrame> frames = new ArrayList<>();
        frames.add(frame(elapsed, x1, x2, v1, v2));
        for (BigDecimal step : limits.stepSizes(input)) {
            BigDecimal end = elapsed.add(step);
            if (!collided && collisionTime.signum() >= 0 && collisionTime.compareTo(end) <= 0) {
                BigDecimal before = collisionTime.subtract(elapsed).max(BigDecimal.ZERO);
                x1 = x1.add(v1.multiply(before, NumericalSupport.MATH_CONTEXT));
                x2 = x2.add(v2.multiply(before, NumericalSupport.MATH_CONTEXT));
                BigDecimal denominator = m1.add(m2);
                BigDecimal nextV1 = m1.subtract(m2).multiply(v1).add(m2.multiply(v2).multiply(NumericalSupport.TWO))
                        .divide(denominator, NumericalSupport.MATH_CONTEXT);
                BigDecimal nextV2 = m2.subtract(m1).multiply(v2).add(m1.multiply(v1).multiply(NumericalSupport.TWO))
                        .divide(denominator, NumericalSupport.MATH_CONTEXT);
                BigDecimal after = step.subtract(before);
                v1 = nextV1; v2 = nextV2;
                x1 = x1.add(v1.multiply(after, NumericalSupport.MATH_CONTEXT));
                x2 = x2.add(v2.multiply(after, NumericalSupport.MATH_CONTEXT));
                collided = true;
            } else {
                x1 = x1.add(v1.multiply(step, NumericalSupport.MATH_CONTEXT));
                x2 = x2.add(v2.multiply(step, NumericalSupport.MATH_CONTEXT));
            }
            elapsed = end;
            frames.add(frame(elapsed, x1, x2, v1, v2));
        }
        return new SolverResult(SolverMethod.NUMERICAL, VERSION, Map.of(
                "finalPosition1", NumericalSupport.clean(x1), "finalPosition2", NumericalSupport.clean(x2),
                "finalVelocity1", NumericalSupport.clean(v1), "finalVelocity2", NumericalSupport.clean(v2),
                "collisionTime", NumericalSupport.clean(collisionTime)), frames);
    }

    private BigDecimal collisionTime(BigDecimal x1, BigDecimal x2, BigDecimal v1, BigDecimal v2) {
        BigDecimal relativeVelocity = v1.subtract(v2);
        if (relativeVelocity.signum() == 0) return BigDecimal.ONE.negate();
        BigDecimal value = x2.subtract(x1).divide(relativeVelocity, NumericalSupport.MATH_CONTEXT);
        return value.signum() < 0 ? BigDecimal.ONE.negate() : value;
    }

    private SimulationFrame frame(BigDecimal t, BigDecimal x1, BigDecimal x2, BigDecimal v1, BigDecimal v2) {
        return new SimulationFrame(NumericalSupport.clean(t), Map.of(
                "position1", NumericalSupport.clean(x1), "position2", NumericalSupport.clean(x2),
                "velocity1", NumericalSupport.clean(v1), "velocity2", NumericalSupport.clean(v2)));
    }
}
