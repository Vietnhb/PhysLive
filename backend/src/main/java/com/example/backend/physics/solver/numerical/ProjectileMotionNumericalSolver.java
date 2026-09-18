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
public class ProjectileMotionNumericalSolver implements NumericalSolver {

    private static final String VERSION = "projectile-motion-numerical-v1";
    private final NumericalSolverLimits limits;

    public ProjectileMotionNumericalSolver(NumericalSolverLimits limits) {
        this.limits = limits;
    }

    @Override public PhysicsArchetype archetype() { return PhysicsArchetype.PROJECTILE_MOTION; }
    @Override public String version() { return VERSION; }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal x = NumericalSupport.valueOrDefault(input, "initialPosition", "0");
        BigDecimal y = NumericalSupport.valueOrDefault(input, "initialHeight", "0");
        double speed = NumericalSupport.required(input, "initialVelocity").doubleValue();
        double angle = NumericalSupport.required(input, "angle").doubleValue();
        BigDecimal vx = BigDecimal.valueOf(speed * Math.cos(angle));
        BigDecimal vy = BigDecimal.valueOf(speed * Math.sin(angle));
        BigDecimal gravity = NumericalSupport.valueOrDefault(input, "gravity", "9.80665");
        BigDecimal elapsed = BigDecimal.ZERO;
        List<SimulationFrame> frames = new ArrayList<>();
        frames.add(frame(elapsed, x, y, vx, vy, gravity));
        for (BigDecimal step : limits.stepSizes(input)) {
            x = x.add(vx.multiply(step, NumericalSupport.MATH_CONTEXT));
            y = y.add(vy.multiply(step, NumericalSupport.MATH_CONTEXT))
                    .subtract(gravity.multiply(step.pow(2), NumericalSupport.MATH_CONTEXT)
                            .divide(NumericalSupport.TWO, NumericalSupport.MATH_CONTEXT));
            vy = vy.subtract(gravity.multiply(step, NumericalSupport.MATH_CONTEXT));
            elapsed = elapsed.add(step);
            frames.add(frame(elapsed, x, y, vx, vy, gravity));
        }
        return new SolverResult(SolverMethod.NUMERICAL, VERSION, Map.of(
                "finalPositionX", NumericalSupport.clean(x),
                "finalPositionY", NumericalSupport.clean(y),
                "finalVelocityX", NumericalSupport.clean(vx),
                "finalVelocityY", NumericalSupport.clean(vy)), frames);
    }

    private SimulationFrame frame(BigDecimal time, BigDecimal x, BigDecimal y,
            BigDecimal vx, BigDecimal vy, BigDecimal gravity) {
        return new SimulationFrame(NumericalSupport.clean(time), Map.of(
                "positionX", NumericalSupport.clean(x), "positionY", NumericalSupport.clean(y),
                "velocityX", NumericalSupport.clean(vx), "velocityY", NumericalSupport.clean(vy),
                "accelerationX", BigDecimal.ZERO, "accelerationY", NumericalSupport.clean(gravity.negate())));
    }
}
