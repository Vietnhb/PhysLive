package com.example.backend.physics.validation;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.physics.model.ComparisonTolerance;
import com.example.backend.physics.model.PhysicsArchetype;

@Component
public class ValidationTolerancePolicy {

    private static final ComparisonTolerance KINEMATICS = tolerance("1E-9", "1E-9");
    private static final ComparisonTolerance DYNAMICS = tolerance("1E-6", "1E-6");
    private static final ComparisonTolerance DYNAMICS_POSITION = tolerance("1E-3", "1E-6");
    private static final ComparisonTolerance CIRCUITS = tolerance("1E-12", "1E-10");

    private final Map<PhysicsArchetype, Map<String, ComparisonTolerance>> configured;

    public ValidationTolerancePolicy() {
        EnumMap<PhysicsArchetype, Map<String, ComparisonTolerance>> policies =
                new EnumMap<>(PhysicsArchetype.class);
        policies.put(PhysicsArchetype.UNIFORM_MOTION, Map.of("finalPosition", KINEMATICS));
        policies.put(PhysicsArchetype.CONSTANT_ACCELERATION, Map.of(
                "finalPosition", KINEMATICS,
                "finalVelocity", KINEMATICS));
        policies.put(PhysicsArchetype.PROJECTILE_MOTION, Map.of(
                "finalPositionX", DYNAMICS_POSITION,
                "finalPositionY", DYNAMICS_POSITION,
                "finalVelocityX", DYNAMICS,
                "finalVelocityY", DYNAMICS));
        policies.put(PhysicsArchetype.HORIZONTAL_FORCE, dynamicsPolicy());
        policies.put(PhysicsArchetype.INCLINED_PLANE, dynamicsPolicy());
        policies.put(PhysicsArchetype.ELASTIC_COLLISION, Map.of(
                "finalPosition1", DYNAMICS_POSITION,
                "finalPosition2", DYNAMICS_POSITION,
                "finalVelocity1", DYNAMICS,
                "finalVelocity2", DYNAMICS,
                "collisionTime", DYNAMICS_POSITION));
        policies.put(PhysicsArchetype.SPRING_OSCILLATION, Map.of(
                "finalPosition", DYNAMICS_POSITION,
                "finalVelocity", DYNAMICS_POSITION,
                "angularFrequency", DYNAMICS));
        policies.put(PhysicsArchetype.SERIES_CIRCUIT, Map.of(
                "equivalentResistance", CIRCUITS,
                "totalCurrent", CIRCUITS,
                "voltageDrops", CIRCUITS));
        policies.put(PhysicsArchetype.PARALLEL_CIRCUIT, Map.of(
                "equivalentResistance", CIRCUITS,
                "totalCurrent", CIRCUITS,
                "branchCurrents", CIRCUITS));
        ComparisonTolerance rcTolerance = tolerance("1E-4", "1E-3");
        policies.put(PhysicsArchetype.RC_CHARGING, Map.of(
                "finalVoltage", rcTolerance, "finalCurrent", rcTolerance));
        policies.put(PhysicsArchetype.RC_DISCHARGING, Map.of(
                "finalVoltage", rcTolerance, "finalCurrent", rcTolerance));
        configured = Map.copyOf(policies);
    }

    public ComparisonTolerance require(PhysicsArchetype archetype, String outputKey) {
        Map<String, ComparisonTolerance> outputs = configured.get(archetype);
        ComparisonTolerance tolerance = outputs == null ? null : outputs.get(outputKey);
        if (tolerance == null) {
            throw new IllegalArgumentException("No tolerance configured for " + archetype + "." + outputKey);
        }
        return tolerance;
    }

    private Map<String, ComparisonTolerance> dynamicsPolicy() {
        Map<String, ComparisonTolerance> result = new HashMap<>();
        result.put("normalForce", DYNAMICS);
        result.put("frictionForce", DYNAMICS);
        result.put("acceleration", DYNAMICS);
        result.put("finalVelocity", DYNAMICS);
        result.put("finalPosition", DYNAMICS_POSITION);
        return Map.copyOf(result);
    }

    private static ComparisonTolerance tolerance(String absolute, String relative) {
        return new ComparisonTolerance(new BigDecimal(absolute), new BigDecimal(relative));
    }
}
