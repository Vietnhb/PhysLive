package com.example.backend.physics.solver;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.validation.ArchetypeInputValidator;

@Component
public class NumericalSolverRegistry {

    private final Map<PhysicsArchetype, NumericalSolver> solvers;
    private final ArchetypeInputValidator inputValidator;

    public NumericalSolverRegistry(List<NumericalSolver> solvers, ArchetypeInputValidator inputValidator) {
        EnumMap<PhysicsArchetype, NumericalSolver> configured = new EnumMap<>(PhysicsArchetype.class);
        for (NumericalSolver solver : solvers) {
            NumericalSolver previous = configured.putIfAbsent(solver.archetype(), solver);
            if (previous != null) {
                throw new IllegalStateException("Multiple numerical solvers registered for " + solver.archetype());
            }
        }
        this.solvers = Map.copyOf(configured);
        this.inputValidator = inputValidator;
    }

    public SolverResult solve(SolverInput input) {
        inputValidator.validateOrThrow(input);
        NumericalSolver solver = solvers.get(input.archetype());
        if (solver == null) {
            throw new InvalidPhysicsInputException("No numerical solver registered for " + input.archetype());
        }
        return solver.solve(input);
    }

    public NumericalSolver require(PhysicsArchetype archetype) {
        NumericalSolver solver = solvers.get(archetype);
        if (solver == null) {
            throw new InvalidPhysicsInputException("No numerical solver registered for " + archetype);
        }
        return solver;
    }
}
