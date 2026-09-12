package com.example.backend.physics.solver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.SolverInput;

@Component
public class NumericalSolverLimits {

    public static final BigDecimal DEFAULT_DELTA_TIME = new BigDecimal("0.01");
    public static final BigDecimal MAX_DURATION = new BigDecimal("300");
    public static final int MAX_FRAMES = 10_001;

    public List<BigDecimal> stepSizes(SolverInput input) {
        BigDecimal duration = required(input, "duration");
        BigDecimal deltaTime = input.values().getOrDefault("deltaTime", DEFAULT_DELTA_TIME);
        if (duration.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPhysicsInputException("Duration must not be negative");
        }
        if (duration.compareTo(MAX_DURATION) > 0) {
            throw new InvalidPhysicsInputException("Duration must not exceed " + MAX_DURATION + " seconds");
        }
        if (deltaTime.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPhysicsInputException("Delta time must be greater than zero");
        }

        BigDecimal stepCount = duration.divide(deltaTime, 0, RoundingMode.CEILING);
        if (stepCount.compareTo(BigDecimal.valueOf(MAX_FRAMES - 1L)) > 0) {
            throw new InvalidPhysicsInputException("Numerical run must not exceed " + MAX_FRAMES + " frames");
        }

        int numberOfSteps = stepCount.intValueExact();
        List<BigDecimal> steps = new ArrayList<>(numberOfSteps);
        BigDecimal elapsed = BigDecimal.ZERO;
        for (int index = 0; index < numberOfSteps; index++) {
            BigDecimal remaining = duration.subtract(elapsed);
            BigDecimal step = remaining.min(deltaTime);
            steps.add(step);
            elapsed = elapsed.add(step);
        }
        return List.copyOf(steps);
    }

    private BigDecimal required(SolverInput input, String key) {
        BigDecimal value = input.values().get(key);
        if (value == null) {
            throw new InvalidPhysicsInputException("Missing required input: " + key);
        }
        return value;
    }
}
