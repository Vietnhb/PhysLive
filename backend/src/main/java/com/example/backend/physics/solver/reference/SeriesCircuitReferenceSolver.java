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
public class SeriesCircuitReferenceSolver implements ReferenceSolver {

    private static final String VERSION = "series-circuit-reference-v1";

    @Override
    public PhysicsArchetype archetype() {
        return PhysicsArchetype.SERIES_CIRCUIT;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public SolverResult solve(SolverInput input) {
        BigDecimal voltage = ReferenceSupport.required(input, "voltage");
        List<BigDecimal> resistances = ReferenceSupport.requiredSeries(input, "resistances");
        BigDecimal equivalentResistance = resistances.stream()
                .reduce(BigDecimal.ZERO,
                        (left, right) -> left.add(right, ReferenceSupport.MATH_CONTEXT));
        BigDecimal current = voltage.divide(equivalentResistance, ReferenceSupport.MATH_CONTEXT);
        List<BigDecimal> voltageDrops = new ArrayList<>(resistances.size());
        for (BigDecimal resistance : resistances) {
            voltageDrops.add(ReferenceSupport.clean(
                    current.multiply(resistance, ReferenceSupport.MATH_CONTEXT)));
        }
        return new SolverResult(SolverMethod.REFERENCE, VERSION,
                Map.of(
                        "equivalentResistance", ReferenceSupport.clean(equivalentResistance),
                        "totalCurrent", ReferenceSupport.clean(current)),
                Map.of("voltageDrops", voltageDrops), List.of());
    }
}
