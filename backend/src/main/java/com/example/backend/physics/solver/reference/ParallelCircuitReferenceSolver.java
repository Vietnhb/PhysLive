package com.example.backend.physics.solver.reference;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.ReferenceSolver;

@Component
public class ParallelCircuitReferenceSolver implements ReferenceSolver {

    private static final String VERSION = "parallel-circuit-reference-v1";

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
        BigDecimal voltage = ReferenceSupport.required(input, "voltage");
        List<BigDecimal> resistances = ReferenceSupport.requiredSeries(input, "resistances");
        BigDecimal conductance = BigDecimal.ZERO;
        List<BigDecimal> branchCurrents = new ArrayList<>(resistances.size());
        BigDecimal totalCurrent = BigDecimal.ZERO;
        for (BigDecimal resistance : resistances) {
            conductance = conductance.add(
                    BigDecimal.ONE.divide(resistance, ReferenceSupport.MATH_CONTEXT),
                    ReferenceSupport.MATH_CONTEXT);
            BigDecimal branchCurrent = voltage.divide(resistance, ReferenceSupport.MATH_CONTEXT);
            branchCurrents.add(ReferenceSupport.clean(branchCurrent));
            totalCurrent = totalCurrent.add(branchCurrent, ReferenceSupport.MATH_CONTEXT);
        }
        BigDecimal equivalentResistance = BigDecimal.ONE.divide(conductance, ReferenceSupport.MATH_CONTEXT);
        return new SolverResult(SolverMethod.REFERENCE, VERSION,
                Map.of(
                        "equivalentResistance", ReferenceSupport.clean(equivalentResistance),
                        "totalCurrent", ReferenceSupport.clean(totalCurrent)),
                Map.of("branchCurrents", branchCurrents), List.of());
    }
}
