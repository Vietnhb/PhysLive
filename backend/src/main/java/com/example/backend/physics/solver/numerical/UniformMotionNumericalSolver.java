package com.example.backend.physics.solver.numerical;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SimulationFrame;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.NumericalSolver;
import com.example.backend.physics.solver.NumericalSolverLimits;

@Component
public class UniformMotionNumericalSolver implements NumericalSolver {

    private static final String VERSION = "uniform-motion-numerical-v1";

    private final NumericalSolverLimits limits;

    public UniformMotionNumericalSolver(NumericalSolverLimits limits) {
        this.limits = limits;
    }

    @Override
    public PhysicsArchetype archetype() {
        return PhysicsArchetype.UNIFORM_MOTION;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal position = NumericalSupport.valueOrDefault(input, "initialPosition", "0");
        BigDecimal velocity = NumericalSupport.required(input, "velocity");
        BigDecimal elapsed = BigDecimal.ZERO;
        List<SimulationFrame> frames = new ArrayList<>();
        frames.add(frame(elapsed, position, velocity));

        for (BigDecimal step : limits.stepSizes(input)) {
            position = position.add(velocity.multiply(step, NumericalSupport.MATH_CONTEXT));
            elapsed = elapsed.add(step);
            frames.add(frame(elapsed, position, velocity));
        }

        return new SolverResult(
                SolverMethod.NUMERICAL,
                VERSION,
                Map.of("finalPosition", NumericalSupport.clean(position)),
                frames);
    }

    private SimulationFrame frame(BigDecimal time, BigDecimal position, BigDecimal velocity) {
        return new SimulationFrame(
                NumericalSupport.clean(time),
                Map.of(
                        "position", NumericalSupport.clean(position),
                        "velocity", NumericalSupport.clean(velocity),
                        "acceleration", BigDecimal.ZERO));
    }
}
