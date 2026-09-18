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
public class ElasticCollisionReferenceSolver implements ReferenceSolver {
    private static final String VERSION = "elastic-collision-reference-v1";
    @Override public PhysicsArchetype archetype() { return PhysicsArchetype.ELASTIC_COLLISION; }
    @Override public String version() { return VERSION; }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal m1 = ReferenceSupport.required(input, "mass1");
        BigDecimal m2 = ReferenceSupport.required(input, "mass2");
        BigDecimal x1 = ReferenceSupport.required(input, "initialPosition1");
        BigDecimal x2 = ReferenceSupport.required(input, "initialPosition2");
        BigDecimal v1 = ReferenceSupport.required(input, "velocity1");
        BigDecimal v2 = ReferenceSupport.required(input, "velocity2");
        BigDecimal duration = ReferenceSupport.required(input, "duration");
        BigDecimal relative = v1.subtract(v2);
        BigDecimal collision = relative.signum() == 0 ? BigDecimal.ONE.negate()
                : x2.subtract(x1).divide(relative, ReferenceSupport.MATH_CONTEXT);
        if (collision.signum() < 0 || collision.compareTo(duration) > 0) {
            collision = BigDecimal.ONE.negate();
            x1 = x1.add(v1.multiply(duration));
            x2 = x2.add(v2.multiply(duration));
        } else {
            BigDecimal at1 = x1.add(v1.multiply(collision));
            BigDecimal at2 = x2.add(v2.multiply(collision));
            BigDecimal denominator = m1.add(m2);
            BigDecimal next1 = m1.subtract(m2).multiply(v1).add(m2.multiply(v2).multiply(ReferenceSupport.TWO))
                    .divide(denominator, ReferenceSupport.MATH_CONTEXT);
            BigDecimal next2 = m2.subtract(m1).multiply(v2).add(m1.multiply(v1).multiply(ReferenceSupport.TWO))
                    .divide(denominator, ReferenceSupport.MATH_CONTEXT);
            BigDecimal remaining = duration.subtract(collision);
            x1 = at1.add(next1.multiply(remaining)); x2 = at2.add(next2.multiply(remaining));
            v1 = next1; v2 = next2;
        }
        return new SolverResult(SolverMethod.REFERENCE, VERSION, Map.of(
                "finalPosition1", ReferenceSupport.clean(x1), "finalPosition2", ReferenceSupport.clean(x2),
                "finalVelocity1", ReferenceSupport.clean(v1), "finalVelocity2", ReferenceSupport.clean(v2),
                "collisionTime", ReferenceSupport.clean(collision)), List.of());
    }
}
