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
public class ConstantAccelerationReferenceSolver implements ReferenceSolver {

    private static final String VERSION = "constant-acceleration-reference-v1";

    @Override
    public PhysicsArchetype archetype() {
        return PhysicsArchetype.CONSTANT_ACCELERATION;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal position = ReferenceSupport.valueOrDefault(input, "initialPosition", "0");
        BigDecimal velocity = ReferenceSupport.valueOrDefault(input, "initialVelocity", "0");
        BigDecimal acceleration = ReferenceSupport.required(input, "acceleration");
        BigDecimal duration = ReferenceSupport.required(input, "duration");
        BigDecimal finalVelocity = velocity.add(
                acceleration.multiply(duration, ReferenceSupport.MATH_CONTEXT), ReferenceSupport.MATH_CONTEXT);
        BigDecimal finalPosition = position
                .add(velocity.multiply(duration, ReferenceSupport.MATH_CONTEXT), ReferenceSupport.MATH_CONTEXT)
                .add(acceleration.multiply(duration.pow(2), ReferenceSupport.MATH_CONTEXT)
                        .divide(ReferenceSupport.TWO, ReferenceSupport.MATH_CONTEXT),
                        ReferenceSupport.MATH_CONTEXT);
        return new SolverResult(SolverMethod.REFERENCE, VERSION,
                Map.of("finalPosition", ReferenceSupport.clean(finalPosition),
                        "finalVelocity", ReferenceSupport.clean(finalVelocity)),
                List.of());
    }
}
