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
public class InclinedPlaneReferenceSolver implements ReferenceSolver {

    private static final String VERSION = "inclined-plane-reference-v1";

    @Override
    public PhysicsArchetype archetype() {
        return PhysicsArchetype.INCLINED_PLANE;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal mass = ReferenceSupport.required(input, "mass");
        BigDecimal angle = ReferenceSupport.required(input, "angle");
        BigDecimal coefficient = ReferenceSupport.valueOrDefault(input, "frictionCoefficient", "0");
        BigDecimal gravity = ReferenceSupport.valueOrDefault(input, "gravity", "9.80665");
        BigDecimal weight = mass.multiply(gravity, ReferenceSupport.MATH_CONTEXT);
        BigDecimal downhillForce = weight.multiply(
                BigDecimal.valueOf(Math.sin(angle.doubleValue())), ReferenceSupport.MATH_CONTEXT);
        BigDecimal normalForce = weight.multiply(
                BigDecimal.valueOf(Math.cos(angle.doubleValue())), ReferenceSupport.MATH_CONTEXT);
        BigDecimal frictionLimit = coefficient.multiply(normalForce, ReferenceSupport.MATH_CONTEXT);
        ClosedFormDryFrictionMotion.Outcome outcome = ClosedFormDryFrictionMotion.solve(
                ReferenceSupport.valueOrDefault(input, "initialPosition", "0"),
                ReferenceSupport.valueOrDefault(input, "initialVelocity", "0"),
                ReferenceSupport.required(input, "duration"), mass, downhillForce, frictionLimit);

        return new SolverResult(SolverMethod.REFERENCE, VERSION,
                Map.of(
                        "normalForce", ReferenceSupport.clean(normalForce),
                        "frictionForce", ReferenceSupport.clean(outcome.frictionMagnitude()),
                        "acceleration", ReferenceSupport.clean(outcome.acceleration()),
                        "finalVelocity", ReferenceSupport.clean(outcome.velocity()),
                        "finalPosition", ReferenceSupport.clean(outcome.position())),
                List.of());
    }
}
