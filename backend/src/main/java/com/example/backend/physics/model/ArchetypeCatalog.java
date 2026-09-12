package com.example.backend.physics.model;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ArchetypeCatalog {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal STANDARD_GRAVITY = new BigDecimal("9.80665");

    private final Map<PhysicsArchetype, ArchetypeDefinition> definitions;

    public ArchetypeCatalog() {
        EnumMap<PhysicsArchetype, ArchetypeDefinition> configured = new EnumMap<>(PhysicsArchetype.class);
        configured.put(PhysicsArchetype.UNIFORM_MOTION, definition(
                PhysicsArchetype.UNIFORM_MOTION,
                "Uniform linear motion",
                List.of(
                        optional("initialPosition", "m", ZERO, "Initial position"),
                        required("velocity", "m/s", "Constant velocity"),
                        required("duration", "s", "Elapsed time"),
                        optional("deltaTime", "s", new BigDecimal("0.01"), "Numerical integration step")),
                List.of(required("finalPosition", "m", "Position at the end of the interval"))));
        configured.put(PhysicsArchetype.CONSTANT_ACCELERATION, definition(
                PhysicsArchetype.CONSTANT_ACCELERATION,
                "Linear motion with constant acceleration",
                List.of(
                        optional("initialPosition", "m", ZERO, "Initial position"),
                        optional("initialVelocity", "m/s", ZERO, "Initial velocity"),
                        required("acceleration", "m/s2", "Constant acceleration"),
                        required("duration", "s", "Elapsed time"),
                        optional("deltaTime", "s", new BigDecimal("0.01"), "Numerical integration step")),
                List.of(
                        required("finalPosition", "m", "Position at the end of the interval"),
                        required("finalVelocity", "m/s", "Velocity at the end of the interval"))));
        configured.put(PhysicsArchetype.HORIZONTAL_FORCE, definition(
                PhysicsArchetype.HORIZONTAL_FORCE,
                "Horizontal force with optional kinetic friction",
                List.of(
                        required("mass", "kg", "Object mass"),
                        required("appliedForce", "N", "Applied horizontal force"),
                        optional("frictionCoefficient", "1", ZERO, "Kinetic friction coefficient"),
                        optional("gravity", "m/s2", STANDARD_GRAVITY, "Gravitational acceleration"),
                        optional("initialPosition", "m", ZERO, "Initial position"),
                        optional("initialVelocity", "m/s", ZERO, "Initial velocity"),
                        required("duration", "s", "Elapsed time"),
                        optional("deltaTime", "s", new BigDecimal("0.01"), "Numerical integration step")),
                List.of(
                        required("normalForce", "N", "Normal force"),
                        required("frictionForce", "N", "Kinetic friction force"),
                        required("acceleration", "m/s2", "Resulting acceleration"),
                        required("finalVelocity", "m/s", "Final velocity"),
                        required("finalPosition", "m", "Final position"))));
        configured.put(PhysicsArchetype.INCLINED_PLANE, definition(
                PhysicsArchetype.INCLINED_PLANE,
                "Motion on an inclined plane with optional kinetic friction",
                List.of(
                        required("mass", "kg", "Object mass"),
                        required("angle", "rad", "Incline angle in radians"),
                        optional("frictionCoefficient", "1", ZERO, "Kinetic friction coefficient"),
                        optional("gravity", "m/s2", STANDARD_GRAVITY, "Gravitational acceleration"),
                        optional("initialPosition", "m", ZERO, "Initial position along the plane"),
                        optional("initialVelocity", "m/s", ZERO, "Initial velocity along the plane"),
                        required("duration", "s", "Elapsed time"),
                        optional("deltaTime", "s", new BigDecimal("0.01"), "Numerical integration step")),
                List.of(
                        required("normalForce", "N", "Normal force"),
                        required("frictionForce", "N", "Kinetic friction force"),
                        required("acceleration", "m/s2", "Acceleration along the plane"),
                        required("finalVelocity", "m/s", "Final velocity"),
                        required("finalPosition", "m", "Final position"))));
        configured.put(PhysicsArchetype.SERIES_CIRCUIT, circuitDefinition(
                PhysicsArchetype.SERIES_CIRCUIT,
                "Series resistor circuit",
                List.of(
                        required("equivalentResistance", "ohm", "Equivalent resistance"),
                        required("totalCurrent", "A", "Circuit current"),
                        requiredSeries("voltageDrops", "V", "Voltage drop across each resistor"))));
        configured.put(PhysicsArchetype.PARALLEL_CIRCUIT, circuitDefinition(
                PhysicsArchetype.PARALLEL_CIRCUIT,
                "Parallel resistor circuit",
                List.of(
                        required("equivalentResistance", "ohm", "Equivalent resistance"),
                        required("totalCurrent", "A", "Total source current"),
                        requiredSeries("branchCurrents", "A", "Current through each branch"))));
        definitions = Map.copyOf(configured);
    }

    public ArchetypeDefinition require(PhysicsArchetype archetype) {
        ArchetypeDefinition definition = definitions.get(archetype);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported physics archetype: " + archetype);
        }
        return definition;
    }

    public Map<PhysicsArchetype, ArchetypeDefinition> all() {
        return definitions;
    }

    private ArchetypeDefinition circuitDefinition(
            PhysicsArchetype archetype,
            String description,
            List<PhysicsQuantityDefinition> outputs) {
        return definition(
                archetype,
                description,
                List.of(
                        required("voltage", "V", "Source voltage"),
                        requiredSeries("resistances", "ohm", "At least two positive resistances")),
                outputs);
    }

    private ArchetypeDefinition definition(
            PhysicsArchetype archetype,
            String description,
            List<PhysicsQuantityDefinition> inputs,
            List<PhysicsQuantityDefinition> outputs) {
        return new ArchetypeDefinition(archetype, description, inputs, outputs);
    }

    private PhysicsQuantityDefinition required(String key, String unit, String description) {
        return new PhysicsQuantityDefinition(key, unit, QuantityCardinality.SCALAR, true, null, description);
    }

    private PhysicsQuantityDefinition optional(
            String key, String unit, BigDecimal defaultValue, String description) {
        return new PhysicsQuantityDefinition(key, unit, QuantityCardinality.SCALAR, false, defaultValue, description);
    }

    private PhysicsQuantityDefinition requiredSeries(String key, String unit, String description) {
        return new PhysicsQuantityDefinition(key, unit, QuantityCardinality.SERIES, true, null, description);
    }
}
