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
public class ReferenceSolverRegistry {

    private final Map<PhysicsArchetype, ReferenceSolver> solvers;
    private final ArchetypeInputValidator inputValidator;

    public ReferenceSolverRegistry(List<ReferenceSolver> solvers, ArchetypeInputValidator inputValidator) {
        EnumMap<PhysicsArchetype, ReferenceSolver> configured = new EnumMap<>(PhysicsArchetype.class);
        for (ReferenceSolver solver : solvers) {
            ReferenceSolver previous = configured.putIfAbsent(solver.archetype(), solver);
            if (previous != null) {
                throw new IllegalStateException("Multiple reference solvers registered for " + solver.archetype());
            }
        }
        this.solvers = Map.copyOf(configured);
        this.inputValidator = inputValidator;
    }

    public SolverResult solve(SolverInput input) {
        inputValidator.validateOrThrow(input);
        ReferenceSolver solver = solvers.get(input.archetype());
        if (solver == null) {
            throw new InvalidPhysicsInputException("No reference solver registered for " + input.archetype());
        }
        return solver.solve(input);
    }

    public ReferenceSolver require(PhysicsArchetype archetype) {
        ReferenceSolver solver = solvers.get(archetype);
        if (solver == null) {
            throw new InvalidPhysicsInputException("No reference solver registered for " + archetype);
        }
        return solver;
    }
}
