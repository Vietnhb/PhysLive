package com.example.backend.physics.solver;

import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverResult;

public interface NumericalSolver {
    PhysicsArchetype archetype();

    String version();

    SolverResult solve(SolverInput input);
}
