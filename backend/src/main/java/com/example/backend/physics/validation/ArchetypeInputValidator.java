package com.example.backend.physics.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.ArchetypeDefinition;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.PhysicsQuantityDefinition;
import com.example.backend.physics.model.QuantityCardinality;
import com.example.backend.physics.model.SolverInput;

@Component
public class ArchetypeInputValidator {

    private static final BigDecimal HALF_PI = new BigDecimal("1.5707963267948966");
    private static final BigDecimal PI = new BigDecimal("3.1415926535897932");

    private final ArchetypeCatalog catalog;

    public ArchetypeInputValidator(ArchetypeCatalog catalog) {
        this.catalog = catalog;
    }

    public List<String> validate(SolverInput input) {
        List<String> blockers = new ArrayList<>();
        if (input == null) {
            return List.of("Solver input is required");
        }

        ArchetypeDefinition definition = catalog.require(input.archetype());
        Set<String> allowedKeys = new HashSet<>();
        for (PhysicsQuantityDefinition quantity : definition.inputs()) {
            allowedKeys.add(quantity.key());
            validateQuantity(input, quantity, blockers);
        }

        input.values().keySet().stream()
                .filter(key -> !allowedKeys.contains(key))
                .forEach(key -> blockers.add("Unexpected scalar input: " + key));
        input.seriesValues().keySet().stream()
                .filter(key -> !allowedKeys.contains(key))
                .forEach(key -> blockers.add("Unexpected series input: " + key));

        validatePhysicalRanges(input, blockers);
        return List.copyOf(blockers);
    }

    public void validateOrThrow(SolverInput input) {
        List<String> blockers = validate(input);
        if (!blockers.isEmpty()) {
            throw new InvalidPhysicsInputException(String.join("; ", blockers));
        }
    }

    private void validateQuantity(
            SolverInput input,
            PhysicsQuantityDefinition definition,
            List<String> blockers) {
        boolean present = definition.cardinality() == QuantityCardinality.SCALAR
                ? input.values().containsKey(definition.key())
                : input.seriesValues().containsKey(definition.key());
        if (!present && definition.required()) {
            blockers.add("Missing required input: " + definition.key());
            return;
        }
        if (!present) {
            return;
        }

        String actualUnit = input.units().get(definition.key());
        if (!definition.unit().equals(actualUnit)) {
            blockers.add("Input '" + definition.key() + "' must use " + definition.unit()
                    + " but received " + (actualUnit == null ? "no unit" : actualUnit));
        }
        if (definition.cardinality() == QuantityCardinality.SCALAR
                && input.values().get(definition.key()) == null) {
            blockers.add("Input '" + definition.key() + "' must have a value");
        }
        if (definition.cardinality() == QuantityCardinality.SERIES) {
            List<BigDecimal> values = input.seriesValues().get(definition.key());
            if (values == null || values.isEmpty()) {
                blockers.add("Input series '" + definition.key() + "' must not be empty");
            } else if (values.stream().anyMatch(value -> value == null)) {
                blockers.add("Input series '" + definition.key() + "' contains a null value");
            }
        }
    }

    private void validatePhysicalRanges(SolverInput input, List<String> blockers) {
        nonNegative(input, "duration", blockers);
        positive(input, "deltaTime", blockers);
        positive(input, "mass", blockers);
        positive(input, "mass1", blockers);
        positive(input, "mass2", blockers);
        positive(input, "amplitude", blockers);
        positive(input, "springConstant", blockers);
        positive(input, "resistance", blockers);
        positive(input, "capacitance", blockers);
        nonNegative(input, "frictionCoefficient", blockers);
        positive(input, "gravity", blockers);

        BigDecimal angle = input.values().get("angle");
        BigDecimal maximumAngle = input.archetype() == PhysicsArchetype.PROJECTILE_MOTION ? PI : HALF_PI;
        if (angle != null && (angle.compareTo(BigDecimal.ZERO) < 0 || angle.compareTo(maximumAngle) >= 0)) {
            blockers.add("Input 'angle' must be in radians and satisfy 0 <= angle < "
                    + (input.archetype() == PhysicsArchetype.PROJECTILE_MOTION ? "pi" : "pi/2"));
        }

        if (input.archetype() == PhysicsArchetype.SERIES_CIRCUIT
                || input.archetype() == PhysicsArchetype.PARALLEL_CIRCUIT) {
            List<BigDecimal> resistances = input.seriesValues().get("resistances");
            if (resistances != null && resistances.size() < 2) {
                blockers.add("Input 'resistances' must contain at least two values");
            }
            if (resistances != null && resistances.stream()
                    .filter(value -> value != null)
                    .anyMatch(value -> value.compareTo(BigDecimal.ZERO) <= 0)) {
                blockers.add("Every resistance must be greater than zero");
            }
        }
    }

    private void positive(SolverInput input, String key, List<String> blockers) {
        BigDecimal value = input.values().get(key);
        if (value != null && value.compareTo(BigDecimal.ZERO) <= 0) {
            blockers.add("Input '" + key + "' must be greater than zero");
        }
    }

    private void nonNegative(SolverInput input, String key, List<String> blockers) {
        BigDecimal value = input.values().get(key);
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            blockers.add("Input '" + key + "' must not be negative");
        }
    }
}
