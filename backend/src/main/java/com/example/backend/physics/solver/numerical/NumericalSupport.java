package com.example.backend.physics.solver.numerical;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.SolverInput;

final class NumericalSupport {

    static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_EVEN);
    static final BigDecimal TWO = new BigDecimal("2");

    private NumericalSupport() {
    }

    static BigDecimal required(SolverInput input, String key) {
        BigDecimal value = input.values().get(key);
        if (value == null) {
            throw new InvalidPhysicsInputException("Missing required input: " + key);
        }
        return value;
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

    static int sign(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO);
    }
}
