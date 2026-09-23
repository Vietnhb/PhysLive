package com.example.backend.physics.solver.reference;

import org.springframework.stereotype.Component;

import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.ReferenceSolver;

@Component
public class RcDischargingReferenceSolver implements ReferenceSolver {
    private static final String VERSION = "rc-discharging-reference-v1";
    @Override public PhysicsArchetype archetype() { return PhysicsArchetype.RC_DISCHARGING; }
    @Override public String version() { return VERSION; }
    @Override public SolverResult solve(SolverInput input) {
        return RcCircuitReferenceSupport.solve(input, archetype(), VERSION);
    }
}
