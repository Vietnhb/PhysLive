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
public class HorizontalForceNumericalSolver implements NumericalSolver {

    private static final String VERSION = "horizontal-force-numerical-v1";

    private final NumericalSolverLimits limits;

    public HorizontalForceNumericalSolver(NumericalSolverLimits limits) {
        this.limits = limits;
    }

    @Override
    public PhysicsArchetype archetype() {
        return PhysicsArchetype.HORIZONTAL_FORCE;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal mass = NumericalSupport.required(input, "mass");
        BigDecimal appliedForce = NumericalSupport.required(input, "appliedForce");
        BigDecimal coefficient = NumericalSupport.valueOrDefault(input, "frictionCoefficient", "0");
        BigDecimal gravity = NumericalSupport.valueOrDefault(input, "gravity", "9.80665");
        BigDecimal normalForce = mass.multiply(gravity, NumericalSupport.MATH_CONTEXT);
        BigDecimal frictionLimit = coefficient.multiply(normalForce, NumericalSupport.MATH_CONTEXT);
        BigDecimal position = NumericalSupport.valueOrDefault(input, "initialPosition", "0");
        BigDecimal velocity = NumericalSupport.valueOrDefault(input, "initialVelocity", "0");
        BigDecimal elapsed = BigDecimal.ZERO;
        ForceState forceState = forceState(appliedForce, frictionLimit, velocity);
        List<SimulationFrame> frames = new ArrayList<>();
        frames.add(frame(elapsed, position, velocity, normalForce, forceState, mass));

        for (BigDecimal step : limits.stepSizes(input)) {
            forceState = forceState(appliedForce, frictionLimit, velocity);
            BigDecimal acceleration = forceState.netForce()
                    .divide(mass, NumericalSupport.MATH_CONTEXT);
            BigDecimal nextVelocity = velocity.add(acceleration.multiply(step, NumericalSupport.MATH_CONTEXT));
            if (wouldStopAndReverse(velocity, nextVelocity, appliedForce, frictionLimit)) {
                nextVelocity = BigDecimal.ZERO;
            }
            BigDecimal averageVelocity = velocity.add(nextVelocity)
                    .divide(NumericalSupport.TWO, NumericalSupport.MATH_CONTEXT);
            position = position.add(averageVelocity.multiply(step, NumericalSupport.MATH_CONTEXT));
            velocity = nextVelocity;
            elapsed = elapsed.add(step);
            forceState = forceState(appliedForce, frictionLimit, velocity);
            frames.add(frame(elapsed, position, velocity, normalForce, forceState, mass));
        }

        BigDecimal acceleration = forceState.netForce().divide(mass, NumericalSupport.MATH_CONTEXT);
        return new SolverResult(
                SolverMethod.NUMERICAL,
                VERSION,
                Map.of(
                        "normalForce", NumericalSupport.clean(normalForce),
                        "frictionForce", NumericalSupport.clean(forceState.frictionMagnitude()),
                        "acceleration", NumericalSupport.clean(acceleration),
                        "finalVelocity", NumericalSupport.clean(velocity),
                        "finalPosition", NumericalSupport.clean(position)),
                frames);
    }

    private ForceState forceState(
            BigDecimal appliedForce,
            BigDecimal frictionLimit,
            BigDecimal velocity) {
        int motionDirection = NumericalSupport.sign(velocity);
        if (motionDirection == 0) {
            int appliedDirection = NumericalSupport.sign(appliedForce);
            if (appliedForce.abs().compareTo(frictionLimit) <= 0) {
                return new ForceState(appliedForce.abs(), BigDecimal.ZERO);
            }
            BigDecimal friction = frictionLimit.multiply(BigDecimal.valueOf(appliedDirection));
            return new ForceState(frictionLimit, appliedForce.subtract(friction));
        }
        BigDecimal friction = frictionLimit.multiply(BigDecimal.valueOf(motionDirection));
        return new ForceState(frictionLimit, appliedForce.subtract(friction));
    }

    private boolean wouldStopAndReverse(
            BigDecimal velocity,
            BigDecimal nextVelocity,
            BigDecimal appliedForce,
            BigDecimal frictionLimit) {
        return NumericalSupport.sign(velocity) != 0
                && NumericalSupport.sign(velocity) != NumericalSupport.sign(nextVelocity)
                && appliedForce.abs().compareTo(frictionLimit) <= 0;
    }

    private SimulationFrame frame(
            BigDecimal time,
            BigDecimal position,
            BigDecimal velocity,
            BigDecimal normalForce,
            ForceState forceState,
            BigDecimal mass) {
        BigDecimal acceleration = forceState.netForce().divide(mass, NumericalSupport.MATH_CONTEXT);
        return new SimulationFrame(
                NumericalSupport.clean(time),
                Map.of(
                        "position", NumericalSupport.clean(position),
                        "velocity", NumericalSupport.clean(velocity),
                        "normalForce", NumericalSupport.clean(normalForce),
                        "frictionForce", NumericalSupport.clean(forceState.frictionMagnitude()),
                        "netForce", NumericalSupport.clean(forceState.netForce()),
                        "acceleration", NumericalSupport.clean(acceleration)));
    }

    private record ForceState(BigDecimal frictionMagnitude, BigDecimal netForce) {
    }
}
