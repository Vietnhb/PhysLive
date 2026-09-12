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
public class InclinedPlaneNumericalSolver implements NumericalSolver {

    private static final String VERSION = "inclined-plane-numerical-v1";

    private final NumericalSolverLimits limits;

    public InclinedPlaneNumericalSolver(NumericalSolverLimits limits) {
        this.limits = limits;
    }

    @Override
    public PhysicsArchetype archetype() {
        return PhysicsArchetype.INCLINED_PLANE;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal mass = NumericalSupport.required(input, "mass");
        BigDecimal angle = NumericalSupport.required(input, "angle");
        BigDecimal coefficient = NumericalSupport.valueOrDefault(input, "frictionCoefficient", "0");
        BigDecimal gravity = NumericalSupport.valueOrDefault(input, "gravity", "9.80665");
        BigDecimal sin = BigDecimal.valueOf(Math.sin(angle.doubleValue()));
        BigDecimal cos = BigDecimal.valueOf(Math.cos(angle.doubleValue()));
        BigDecimal weight = mass.multiply(gravity, NumericalSupport.MATH_CONTEXT);
        BigDecimal downhillForce = weight.multiply(sin, NumericalSupport.MATH_CONTEXT);
        BigDecimal normalForce = weight.multiply(cos, NumericalSupport.MATH_CONTEXT);
        BigDecimal frictionLimit = coefficient.multiply(normalForce, NumericalSupport.MATH_CONTEXT);
        BigDecimal position = NumericalSupport.valueOrDefault(input, "initialPosition", "0");
        BigDecimal velocity = NumericalSupport.valueOrDefault(input, "initialVelocity", "0");
        BigDecimal elapsed = BigDecimal.ZERO;
        ForceState forceState = forceState(downhillForce, frictionLimit, velocity);
        List<SimulationFrame> frames = new ArrayList<>();
        frames.add(frame(elapsed, position, velocity, normalForce, forceState, mass));

        for (BigDecimal step : limits.stepSizes(input)) {
            forceState = forceState(downhillForce, frictionLimit, velocity);
            BigDecimal acceleration = forceState.netForce().divide(mass, NumericalSupport.MATH_CONTEXT);
            BigDecimal nextVelocity = velocity.add(acceleration.multiply(step, NumericalSupport.MATH_CONTEXT));
            if (wouldStopAndStay(velocity, nextVelocity, downhillForce, frictionLimit)) {
                nextVelocity = BigDecimal.ZERO;
            }
            BigDecimal averageVelocity = velocity.add(nextVelocity)
                    .divide(NumericalSupport.TWO, NumericalSupport.MATH_CONTEXT);
            position = position.add(averageVelocity.multiply(step, NumericalSupport.MATH_CONTEXT));
            velocity = nextVelocity;
            elapsed = elapsed.add(step);
            forceState = forceState(downhillForce, frictionLimit, velocity);
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
            BigDecimal downhillForce,
            BigDecimal frictionLimit,
            BigDecimal velocity) {
        int motionDirection = NumericalSupport.sign(velocity);
        if (motionDirection == 0) {
            if (downhillForce.compareTo(frictionLimit) <= 0) {
                return new ForceState(downhillForce, BigDecimal.ZERO);
            }
            return new ForceState(frictionLimit, downhillForce.subtract(frictionLimit));
        }
        BigDecimal signedFriction = frictionLimit.multiply(BigDecimal.valueOf(motionDirection));
        return new ForceState(frictionLimit, downhillForce.subtract(signedFriction));
    }

    private boolean wouldStopAndStay(
            BigDecimal velocity,
            BigDecimal nextVelocity,
            BigDecimal downhillForce,
            BigDecimal frictionLimit) {
        return NumericalSupport.sign(velocity) < 0
                && NumericalSupport.sign(nextVelocity) >= 0
                && downhillForce.compareTo(frictionLimit) <= 0;
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
