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
        configured.put(PhysicsArchetype.PROJECTILE_MOTION, definition(
                PhysicsArchetype.PROJECTILE_MOTION,
                "Two-dimensional projectile motion under constant gravity",
                List.of(
                        optional("initialPosition", "m", ZERO, "Initial horizontal position"),
                        optional("initialHeight", "m", ZERO, "Initial vertical position"),
                        required("initialVelocity", "m/s", "Launch speed"),
                        required("angle", "rad", "Launch angle"),
                        optional("gravity", "m/s2", STANDARD_GRAVITY, "Gravitational acceleration"),
                        required("duration", "s", "Elapsed time"),
                        optional("deltaTime", "s", new BigDecimal("0.01"), "Numerical integration step")),
                List.of(
                        required("finalPositionX", "m", "Final horizontal position"),
                        required("finalPositionY", "m", "Final vertical position"),
                        required("finalVelocityX", "m/s", "Final horizontal velocity"),
                        required("finalVelocityY", "m/s", "Final vertical velocity"))));
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
        configured.put(PhysicsArchetype.ELASTIC_COLLISION, definition(
                PhysicsArchetype.ELASTIC_COLLISION,
                "One-dimensional perfectly elastic collision",
                List.of(
                        required("mass1", "kg", "First object mass"),
                        required("mass2", "kg", "Second object mass"),
                        required("initialPosition1", "m", "First object position"),
                        required("initialPosition2", "m", "Second object position"),
                        required("velocity1", "m/s", "First object velocity"),
                        required("velocity2", "m/s", "Second object velocity"),
                        required("duration", "s", "Elapsed time"),
                        optional("deltaTime", "s", new BigDecimal("0.01"), "Numerical integration step")),
                List.of(
                        required("finalPosition1", "m", "First object final position"),
                        required("finalPosition2", "m", "Second object final position"),
                        required("finalVelocity1", "m/s", "First object final velocity"),
                        required("finalVelocity2", "m/s", "Second object final velocity"),
                        required("collisionTime", "s", "Collision time or -1 when no collision occurs"))));
        configured.put(PhysicsArchetype.SPRING_OSCILLATION, definition(
                PhysicsArchetype.SPRING_OSCILLATION,
                "Ideal mass-spring harmonic oscillator",
                List.of(
                        required("amplitude", "m", "Oscillation amplitude"),
                        required("mass", "kg", "Oscillating mass"),
                        required("springConstant", "N/m", "Spring stiffness"),
                        optional("phase", "rad", ZERO, "Initial phase"),
                        required("duration", "s", "Elapsed time"),
                        optional("deltaTime", "s", new BigDecimal("0.01"), "Numerical integration step")),
                List.of(
                        required("finalPosition", "m", "Final displacement"),
                        required("finalVelocity", "m/s", "Final velocity"),
                        required("angularFrequency", "rad/s", "Angular frequency"))));
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
        configured.put(PhysicsArchetype.RC_CHARGING, rcDefinition(
                PhysicsArchetype.RC_CHARGING, "RC capacitor charging"));
        configured.put(PhysicsArchetype.RC_DISCHARGING, rcDefinition(
                PhysicsArchetype.RC_DISCHARGING, "RC capacitor discharging"));
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

    private ArchetypeDefinition rcDefinition(PhysicsArchetype archetype, String description) {
        return definition(
                archetype,
                description,
                List.of(
                        required("voltage", "V", "Source or initial capacitor voltage"),
                        required("resistance", "ohm", "Resistance"),
                        required("capacitance", "F", "Capacitance"),
                        required("duration", "s", "Elapsed time"),
                        optional("deltaTime", "s", new BigDecimal("0.01"), "Numerical integration step")),
                List.of(
                        required("finalVoltage", "V", "Final capacitor voltage"),
                        required("finalCurrent", "A", "Final circuit current")));
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
