package com.example.backend.physics.solver.numerical;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.NumericalSolver;

@Component
public class ParallelCircuitNumericalSolver implements NumericalSolver {

    private static final String VERSION = "parallel-circuit-numerical-v1";

    @Override
    public PhysicsArchetype archetype() {
        return PhysicsArchetype.PARALLEL_CIRCUIT;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal voltage = NumericalSupport.required(input, "voltage");
        List<BigDecimal> resistances = resistances(input);
        BigDecimal[] actualSolution = solveSystem(voltage, resistances);
        BigDecimal[] unitSolution = solveSystem(BigDecimal.ONE, resistances);
        int totalIndex = resistances.size();
        BigDecimal totalCurrent = actualSolution[totalIndex];
        BigDecimal unitCurrent = unitSolution[totalIndex];
        if (unitCurrent.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidPhysicsInputException("Equivalent resistance cannot be calculated from zero current");
        }

        List<BigDecimal> branchCurrents = new ArrayList<>(resistances.size());
        for (int index = 0; index < resistances.size(); index++) {
            branchCurrents.add(NumericalSupport.clean(actualSolution[index]));
        }
        BigDecimal equivalentResistance = BigDecimal.ONE.divide(unitCurrent, NumericalSupport.MATH_CONTEXT);

        return new SolverResult(
                SolverMethod.NUMERICAL,
                VERSION,
                Map.of(
                        "equivalentResistance", NumericalSupport.clean(equivalentResistance),
                        "totalCurrent", NumericalSupport.clean(totalCurrent)),
                Map.of("branchCurrents", branchCurrents),
                List.of());
    }

    private BigDecimal[] solveSystem(BigDecimal voltage, List<BigDecimal> resistances) {
        int resistorCount = resistances.size();
        int size = resistorCount + 1;
        BigDecimal[][] coefficients = zeroMatrix(size);
        BigDecimal[] constants = zeroVector(size);

        for (int index = 0; index < resistorCount; index++) {
            coefficients[index][index] = resistances.get(index);
            constants[index] = voltage;
        }
        for (int index = 0; index < resistorCount; index++) {
            coefficients[resistorCount][index] = BigDecimal.ONE.negate();
        }
        coefficients[resistorCount][resistorCount] = BigDecimal.ONE;
        return LinearSystemSolver.solve(coefficients, constants);
    }

    private List<BigDecimal> resistances(SolverInput input) {
        List<BigDecimal> values = input.seriesValues().get("resistances");
        if (values == null || values.isEmpty()) {
            throw new InvalidPhysicsInputException("Missing required input: resistances");
        }
        return values;
    }

    private BigDecimal[][] zeroMatrix(int size) {
        BigDecimal[][] matrix = new BigDecimal[size][size];
        for (int row = 0; row < size; row++) {
            java.util.Arrays.fill(matrix[row], BigDecimal.ZERO);
        }
        return matrix;
    }

    private BigDecimal[] zeroVector(int size) {
        BigDecimal[] vector = new BigDecimal[size];
        java.util.Arrays.fill(vector, BigDecimal.ZERO);
        return vector;
    }
}
