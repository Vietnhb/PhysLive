package com.example.backend.physics.simulation;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.dto.simulation.CreateSimulationRunRequest;
import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.ArchetypeDefinition;
import com.example.backend.physics.model.PhysicsQuantityDefinition;
import com.example.backend.physics.model.QuantityCardinality;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.solver.NumericalSolverLimits;
import com.example.backend.physics.validation.ArchetypeInputValidator;

@Component
public class SimulationParameterPolicy {

    private final ArchetypeCatalog catalog;
    private final ArchetypeInputValidator validator;
    private final NumericalSolverLimits numericalLimits;

    public SimulationParameterPolicy(
            ArchetypeCatalog catalog,
            ArchetypeInputValidator validator,
            NumericalSolverLimits numericalLimits) {
        this.catalog = catalog;
        this.validator = validator;
        this.numericalLimits = numericalLimits;
    }

    public SolverInput apply(SolverInput base, CreateSimulationRunRequest request) {
        CreateSimulationRunRequest safeRequest = request == null
                ? new CreateSimulationRunRequest(Map.of(), Map.of())
                : request;
        ArchetypeDefinition definition = catalog.require(base.archetype());
        Map<String, PhysicsQuantityDefinition> allowed = new LinkedHashMap<>();
        definition.inputs().forEach(input -> allowed.put(input.key(), input));

        Map<String, BigDecimal> values = new LinkedHashMap<>(base.values());
        Map<String, List<BigDecimal>> seriesValues = new LinkedHashMap<>(base.seriesValues());
        Map<String, String> units = new LinkedHashMap<>(base.units());

        safeRequest.values().forEach((key, value) -> {
            PhysicsQuantityDefinition quantity = requireAllowed(allowed, key, QuantityCardinality.SCALAR);
            if (value == null) {
                throw new InvalidPhysicsInputException("Override '" + key + "' must have a value");
            }
            values.put(key, value);
            units.put(key, quantity.unit());
        });
        safeRequest.seriesValues().forEach((key, series) -> {
            PhysicsQuantityDefinition quantity = requireAllowed(allowed, key, QuantityCardinality.SERIES);
            if (series.isEmpty() || series.stream().anyMatch(value -> value == null)) {
                throw new InvalidPhysicsInputException("Override series '" + key + "' must not be empty or contain null");
            }
            seriesValues.put(key, List.copyOf(series));
            units.put(key, quantity.unit());
        });

        SolverInput merged = new SolverInput(base.specificationId(), base.schemaVersion(), base.archetype(),
                values, seriesValues, units);
        validator.validateOrThrow(merged);
        if (merged.values().containsKey("duration")) {
            numericalLimits.stepSizes(merged);
        }
        return merged;
    }

    private PhysicsQuantityDefinition requireAllowed(
            Map<String, PhysicsQuantityDefinition> allowed,
            String key,
            QuantityCardinality cardinality) {
        PhysicsQuantityDefinition quantity = allowed.get(key);
        if (quantity == null) {
            throw new InvalidPhysicsInputException("Parameter override is not allowed: " + key);
        }
        if (quantity.cardinality() != cardinality) {
            throw new InvalidPhysicsInputException("Parameter '" + key + "' has the wrong cardinality");
        }
        return quantity;
    }
}
