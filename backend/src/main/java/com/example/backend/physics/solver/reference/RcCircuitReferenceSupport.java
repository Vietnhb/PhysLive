package com.example.backend.physics.solver.reference;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;

final class RcCircuitReferenceSupport {
    private RcCircuitReferenceSupport() { }

    static SolverResult solve(SolverInput input, PhysicsArchetype archetype, String version) {
        double source = ReferenceSupport.required(input, "voltage").doubleValue();
        double resistance = ReferenceSupport.required(input, "resistance").doubleValue();
        double capacitance = ReferenceSupport.required(input, "capacitance").doubleValue();
        double time = ReferenceSupport.required(input, "duration").doubleValue();
        double decay = Math.exp(-time / (resistance * capacitance));
        boolean discharging = archetype == PhysicsArchetype.RC_DISCHARGING;
        double voltage = discharging ? source * decay : source * (1 - decay);
        double current = (discharging ? -source : source) / resistance * decay;
        return new SolverResult(SolverMethod.REFERENCE, version, Map.of(
                "finalVoltage", decimal(voltage), "finalCurrent", decimal(current)), List.of());
    }

    private static BigDecimal decimal(double value) { return ReferenceSupport.clean(BigDecimal.valueOf(value)); }
}
