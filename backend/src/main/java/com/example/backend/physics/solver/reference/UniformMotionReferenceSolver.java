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
public class UniformMotionReferenceSolver implements ReferenceSolver {

    private static final String VERSION = "uniform-motion-reference-v1";

    @Override
    public PhysicsArchetype archetype() {
        return PhysicsArchetype.UNIFORM_MOTION;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal initialPosition = ReferenceSupport.valueOrDefault(input, "initialPosition", "0");
        BigDecimal velocity = ReferenceSupport.required(input, "velocity");
        BigDecimal duration = ReferenceSupport.required(input, "duration");
        BigDecimal finalPosition = initialPosition.add(
                velocity.multiply(duration, ReferenceSupport.MATH_CONTEXT), ReferenceSupport.MATH_CONTEXT);
        return new SolverResult(SolverMethod.REFERENCE, VERSION,
                Map.of("finalPosition", ReferenceSupport.clean(finalPosition)), List.of());
    }
}
