package com.example.backend.physics.solver.reference;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.ReferenceSolver;

@Component
public class ProjectileMotionReferenceSolver implements ReferenceSolver {
    private static final String VERSION = "projectile-motion-reference-v1";
    @Override public PhysicsArchetype archetype() { return PhysicsArchetype.PROJECTILE_MOTION; }
    @Override public String version() { return VERSION; }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal x0 = ReferenceSupport.valueOrDefault(input, "initialPosition", "0");
        BigDecimal y0 = ReferenceSupport.valueOrDefault(input, "initialHeight", "0");
        double speed = ReferenceSupport.required(input, "initialVelocity").doubleValue();
        double angle = ReferenceSupport.required(input, "angle").doubleValue();
        BigDecimal vx = BigDecimal.valueOf(speed * Math.cos(angle));
        BigDecimal vy0 = BigDecimal.valueOf(speed * Math.sin(angle));
        BigDecimal gravity = ReferenceSupport.valueOrDefault(input, "gravity", "9.80665");
        BigDecimal time = ReferenceSupport.required(input, "duration");
        BigDecimal x = x0.add(vx.multiply(time, ReferenceSupport.MATH_CONTEXT));
        BigDecimal y = y0.add(vy0.multiply(time, ReferenceSupport.MATH_CONTEXT))
                .subtract(gravity.multiply(time.pow(2), ReferenceSupport.MATH_CONTEXT)
                        .divide(ReferenceSupport.TWO, ReferenceSupport.MATH_CONTEXT));
        BigDecimal vy = vy0.subtract(gravity.multiply(time, ReferenceSupport.MATH_CONTEXT));
        return new SolverResult(SolverMethod.REFERENCE, VERSION, Map.of(
                "finalPositionX", ReferenceSupport.clean(x),
                "finalPositionY", ReferenceSupport.clean(y),
                "finalVelocityX", ReferenceSupport.clean(vx),
                "finalVelocityY", ReferenceSupport.clean(vy)), List.of());
    }
}
