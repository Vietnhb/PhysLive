package com.example.backend.physics.solver.reference;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.SolverInput;

final class ReferenceSupport {

    static final MathContext MATH_CONTEXT = new MathContext(24, RoundingMode.HALF_EVEN);
    static final BigDecimal TWO = new BigDecimal("2");

    private ReferenceSupport() {
    }

    static BigDecimal required(SolverInput input, String key) {
        BigDecimal value = input.values().get(key);
        if (value == null) {
            throw new InvalidPhysicsInputException("Missing required input: " + key);
        }
        return value;
    }

    static List<BigDecimal> requiredSeries(SolverInput input, String key) {
        List<BigDecimal> values = input.seriesValues().get(key);
        if (values == null || values.isEmpty()) {
            throw new InvalidPhysicsInputException("Missing required input: " + key);
        }
        return values;
    }

    static BigDecimal valueOrDefault(SolverInput input, String key, String defaultValue) {
        return input.values().getOrDefault(key, new BigDecimal(defaultValue));
    }

    static BigDecimal clean(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.round(MATH_CONTEXT).stripTrailingZeros();
    }
}
