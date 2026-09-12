package com.example.backend.physics.solver.numerical;

import java.math.BigDecimal;

import com.example.backend.exception.InvalidPhysicsInputException;

final class LinearSystemSolver {

    private static final BigDecimal PIVOT_EPSILON = new BigDecimal("1E-18");

    private LinearSystemSolver() {
    }

    static BigDecimal[] solve(BigDecimal[][] coefficients, BigDecimal[] constants) {
        int size = constants.length;
        if (size == 0 || coefficients.length != size) {
            throw new InvalidPhysicsInputException("Linear system dimensions are invalid");
        }
        BigDecimal[][] matrix = new BigDecimal[size][size + 1];
        for (int row = 0; row < size; row++) {
            if (coefficients[row].length != size) {
                throw new InvalidPhysicsInputException("Linear system must be square");
            }
            for (int column = 0; column < size; column++) {
                matrix[row][column] = coefficients[row][column];
            }
            matrix[row][size] = constants[row];
        }

        for (int pivot = 0; pivot < size; pivot++) {
            int bestRow = pivot;
            for (int row = pivot + 1; row < size; row++) {
                if (matrix[row][pivot].abs().compareTo(matrix[bestRow][pivot].abs()) > 0) {
                    bestRow = row;
                }
            }
            if (matrix[bestRow][pivot].abs().compareTo(PIVOT_EPSILON) <= 0) {
                throw new InvalidPhysicsInputException("Circuit equation system is singular or ill-conditioned");
            }
            BigDecimal[] temporary = matrix[pivot];
            matrix[pivot] = matrix[bestRow];
            matrix[bestRow] = temporary;

            for (int row = pivot + 1; row < size; row++) {
                BigDecimal factor = matrix[row][pivot]
                        .divide(matrix[pivot][pivot], NumericalSupport.MATH_CONTEXT);
                for (int column = pivot; column <= size; column++) {
                    matrix[row][column] = matrix[row][column].subtract(
                            factor.multiply(matrix[pivot][column], NumericalSupport.MATH_CONTEXT),
                            NumericalSupport.MATH_CONTEXT);
                }
            }
        }

        BigDecimal[] solution = new BigDecimal[size];
        for (int row = size - 1; row >= 0; row--) {
            BigDecimal remaining = matrix[row][size];
            for (int column = row + 1; column < size; column++) {
                remaining = remaining.subtract(
                        matrix[row][column].multiply(solution[column], NumericalSupport.MATH_CONTEXT),
                        NumericalSupport.MATH_CONTEXT);
            }
            solution[row] = remaining.divide(matrix[row][row], NumericalSupport.MATH_CONTEXT);
        }
        return solution;
    }
}
