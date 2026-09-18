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
public class SpringOscillationNumericalSolver implements NumericalSolver {
    private static final String VERSION = "spring-oscillation-numerical-v1";
    private final NumericalSolverLimits limits;
    public SpringOscillationNumericalSolver(NumericalSolverLimits limits) { this.limits = limits; }
    @Override public PhysicsArchetype archetype() { return PhysicsArchetype.SPRING_OSCILLATION; }
    @Override public String version() { return VERSION; }

    @Override
    public SolverResult solve(SolverInput input) {
        double amplitude = NumericalSupport.required(input, "amplitude").doubleValue();
        double mass = NumericalSupport.required(input, "mass").doubleValue();
        double stiffness = NumericalSupport.required(input, "springConstant").doubleValue();
        double phase = NumericalSupport.valueOrDefault(input, "phase", "0").doubleValue();
        double omega = Math.sqrt(stiffness / mass);
        double position = amplitude * Math.cos(phase);
        double velocity = -amplitude * omega * Math.sin(phase);
        double acceleration = -(stiffness / mass) * position;
        BigDecimal elapsed = BigDecimal.ZERO;
        List<SimulationFrame> frames = new ArrayList<>();
        frames.add(frame(elapsed, position, velocity, acceleration));
        for (BigDecimal stepValue : limits.stepSizes(input)) {
            double step = stepValue.doubleValue();
            double nextPosition = position + velocity * step + 0.5 * acceleration * step * step;
            double nextAcceleration = -(stiffness / mass) * nextPosition;
            velocity += 0.5 * (acceleration + nextAcceleration) * step;
            position = nextPosition;
            acceleration = nextAcceleration;
            elapsed = elapsed.add(stepValue);
            frames.add(frame(elapsed, position, velocity, acceleration));
        }
        return new SolverResult(SolverMethod.NUMERICAL, VERSION, Map.of(
                "finalPosition", decimal(position), "finalVelocity", decimal(velocity),
                "angularFrequency", decimal(omega)), frames);
    }

    private SimulationFrame frame(BigDecimal time, double position, double velocity, double acceleration) {
        return new SimulationFrame(NumericalSupport.clean(time), Map.of(
                "position", decimal(position), "velocity", decimal(velocity),
                "acceleration", decimal(acceleration)));
    }

    private BigDecimal decimal(double value) { return NumericalSupport.clean(BigDecimal.valueOf(value)); }
}
