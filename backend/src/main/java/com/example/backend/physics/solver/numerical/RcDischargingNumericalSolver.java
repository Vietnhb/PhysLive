package com.example.backend.physics.solver.numerical;

import org.springframework.stereotype.Component;

import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.NumericalSolver;
import com.example.backend.physics.solver.NumericalSolverLimits;

@Component
public class RcDischargingNumericalSolver implements NumericalSolver {
    private static final String VERSION = "rc-discharging-numerical-v1";
    private final NumericalSolverLimits limits;
    public RcDischargingNumericalSolver(NumericalSolverLimits limits) { this.limits = limits; }
    @Override public PhysicsArchetype archetype() { return PhysicsArchetype.RC_DISCHARGING; }
    @Override public String version() { return VERSION; }
    @Override public SolverResult solve(SolverInput input) {
        return RcCircuitNumericalSupport.solve(input, limits, archetype(), VERSION);
    }
}
