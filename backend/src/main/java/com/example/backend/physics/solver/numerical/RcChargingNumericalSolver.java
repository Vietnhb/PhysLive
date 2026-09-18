package com.example.backend.physics.solver.numerical;

import org.springframework.stereotype.Component;

import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.NumericalSolver;
import com.example.backend.physics.solver.NumericalSolverLimits;

@Component
public class RcChargingNumericalSolver implements NumericalSolver {
    private static final String VERSION = "rc-charging-numerical-v1";
    private final NumericalSolverLimits limits;
    public RcChargingNumericalSolver(NumericalSolverLimits limits) { this.limits = limits; }
    @Override public PhysicsArchetype archetype() { return PhysicsArchetype.RC_CHARGING; }
    @Override public String version() { return VERSION; }
    @Override public SolverResult solve(SolverInput input) {
        return RcCircuitNumericalSupport.solve(input, limits, archetype(), VERSION);
    }
}
