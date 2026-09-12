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
public class ConstantAccelerationNumericalSolver implements NumericalSolver {

    private static final String VERSION = "constant-acceleration-numerical-v1";

    private final NumericalSolverLimits limits;

    public ConstantAccelerationNumericalSolver(NumericalSolverLimits limits) {
        this.limits = limits;
    }

    @Override
    public PhysicsArchetype archetype() {
        return PhysicsArchetype.CONSTANT_ACCELERATION;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal position = NumericalSupport.valueOrDefault(input, "initialPosition", "0");
        BigDecimal velocity = NumericalSupport.valueOrDefault(input, "initialVelocity", "0");
        BigDecimal acceleration = NumericalSupport.required(input, "acceleration");
        BigDecimal elapsed = BigDecimal.ZERO;
        List<SimulationFrame> frames = new ArrayList<>();
        frames.add(frame(elapsed, position, velocity, acceleration));

        for (BigDecimal step : limits.stepSizes(input)) {
            BigDecimal nextVelocity = velocity.add(
                    acceleration.multiply(step, NumericalSupport.MATH_CONTEXT));
            BigDecimal averageVelocity = velocity.add(nextVelocity)
                    .divide(NumericalSupport.TWO, NumericalSupport.MATH_CONTEXT);
            position = position.add(averageVelocity.multiply(step, NumericalSupport.MATH_CONTEXT));
            velocity = nextVelocity;
            elapsed = elapsed.add(step);
            frames.add(frame(elapsed, position, velocity, acceleration));
        }

        return new SolverResult(
                SolverMethod.NUMERICAL,
                VERSION,
                Map.of(
                        "finalPosition", NumericalSupport.clean(position),
                        "finalVelocity", NumericalSupport.clean(velocity)),
                frames);
    }

    private SimulationFrame frame(
            BigDecimal time,
            BigDecimal position,
            BigDecimal velocity,
            BigDecimal acceleration) {
        return new SimulationFrame(
                NumericalSupport.clean(time),
                Map.of(
                        "position", NumericalSupport.clean(position),
                        "velocity", NumericalSupport.clean(velocity),
                        "acceleration", NumericalSupport.clean(acceleration)));
    }
}
