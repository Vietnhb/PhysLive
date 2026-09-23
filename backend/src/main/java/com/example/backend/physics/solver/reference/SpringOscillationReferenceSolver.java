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
public class SpringOscillationReferenceSolver implements ReferenceSolver {
    private static final String VERSION = "spring-oscillation-reference-v1";
    @Override public PhysicsArchetype archetype() { return PhysicsArchetype.SPRING_OSCILLATION; }
    @Override public String version() { return VERSION; }

    @Override
    public SolverResult solve(SolverInput input) {
        double amplitude = ReferenceSupport.required(input, "amplitude").doubleValue();
        double mass = ReferenceSupport.required(input, "mass").doubleValue();
        double stiffness = ReferenceSupport.required(input, "springConstant").doubleValue();
        double phase = ReferenceSupport.valueOrDefault(input, "phase", "0").doubleValue();
        double time = ReferenceSupport.required(input, "duration").doubleValue();
        double omega = Math.sqrt(stiffness / mass);
        double angle = omega * time + phase;
        return new SolverResult(SolverMethod.REFERENCE, VERSION, Map.of(
                "finalPosition", decimal(amplitude * Math.cos(angle)),
                "finalVelocity", decimal(-amplitude * omega * Math.sin(angle)),
                "angularFrequency", decimal(omega)), List.of());
    }

    private BigDecimal decimal(double value) { return ReferenceSupport.clean(BigDecimal.valueOf(value)); }
}
