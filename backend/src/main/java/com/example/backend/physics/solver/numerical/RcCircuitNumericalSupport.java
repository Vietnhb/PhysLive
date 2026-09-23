package com.example.backend.physics.solver.numerical;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SimulationFrame;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.NumericalSolverLimits;

final class RcCircuitNumericalSupport {
    private RcCircuitNumericalSupport() { }

    static SolverResult solve(SolverInput input, NumericalSolverLimits limits,
            PhysicsArchetype archetype, String version) {
        double source = NumericalSupport.required(input, "voltage").doubleValue();
        double resistance = NumericalSupport.required(input, "resistance").doubleValue();
        double capacitance = NumericalSupport.required(input, "capacitance").doubleValue();
        boolean discharging = archetype == PhysicsArchetype.RC_DISCHARGING;
        double capacitorVoltage = discharging ? source : 0.0;
        BigDecimal elapsed = BigDecimal.ZERO;
        List<SimulationFrame> frames = new ArrayList<>();
        frames.add(frame(elapsed, capacitorVoltage, current(source, resistance, capacitorVoltage, discharging)));
        for (BigDecimal stepValue : limits.stepSizes(input)) {
            double step = stepValue.doubleValue();
            double target = discharging ? 0.0 : source;
            // RK4 integrates dVc/dt=(target-Vc)/(RC), independently of the exponential reference solver.
            double k1 = derivative(target, capacitorVoltage, resistance, capacitance);
            double k2 = derivative(target, capacitorVoltage + step * k1 / 2, resistance, capacitance);
            double k3 = derivative(target, capacitorVoltage + step * k2 / 2, resistance, capacitance);
            double k4 = derivative(target, capacitorVoltage + step * k3, resistance, capacitance);
            capacitorVoltage += step * (k1 + 2 * k2 + 2 * k3 + k4) / 6;
            elapsed = elapsed.add(stepValue);
            frames.add(frame(elapsed, capacitorVoltage,
                    current(source, resistance, capacitorVoltage, discharging)));
        }
        double finalCurrent = current(source, resistance, capacitorVoltage, discharging);
        return new SolverResult(SolverMethod.NUMERICAL, version, Map.of(
                "finalVoltage", decimal(capacitorVoltage), "finalCurrent", decimal(finalCurrent)), frames);
    }

    private static double derivative(double target, double voltage, double resistance, double capacitance) {
        return (target - voltage) / (resistance * capacitance);
    }

    private static double current(double source, double resistance, double capacitorVoltage, boolean discharging) {
        return discharging ? -capacitorVoltage / resistance : (source - capacitorVoltage) / resistance;
    }

    private static SimulationFrame frame(BigDecimal time, double voltage, double current) {
        return new SimulationFrame(NumericalSupport.clean(time), Map.of(
                "voltage", decimal(voltage), "current", decimal(current)));
    }

    private static BigDecimal decimal(double value) { return NumericalSupport.clean(BigDecimal.valueOf(value)); }
}
