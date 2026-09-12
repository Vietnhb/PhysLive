package com.example.backend.physics.adapter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.backend.entity.ConfirmationState;
import com.example.backend.entity.Specification;
import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.extraction.PhysicalQuantity;
import com.example.backend.extraction.PhysicalRelation;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.validation.ArchetypeInputValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpecificationToSolverInputAdapter {

    private final ObjectMapper objectMapper;
    private final ArchetypeInputValidator inputValidator;

    public SolverInput convert(Specification specification) {
        validateSpecification(specification);
        List<PhysicalQuantity> quantities = readQuantities(specification.getQuantities());
        PhysicsArchetype archetype = inferArchetype(specification, quantities);
        return convert(specification, archetype, quantities);
    }

    public SolverInput convert(Specification specification, PhysicsArchetype archetype) {
        validateSpecification(specification);
        if (archetype == null) {
            throw new InvalidPhysicsInputException("Physics archetype is required");
        }
        if (!archetype.belongsTo(specification.getTopic())) {
            throw new InvalidPhysicsInputException(
                    "Archetype " + archetype + " does not belong to topic " + specification.getTopic());
        }
        return convert(specification, archetype, readQuantities(specification.getQuantities()));
    }

    private SolverInput convert(
            Specification specification,
            PhysicsArchetype archetype,
            List<PhysicalQuantity> quantities) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        Map<String, List<BigDecimal>> seriesValues = new LinkedHashMap<>();
        Map<String, String> units = new LinkedHashMap<>();
        for (PhysicalQuantity quantity : quantities) {
            String key = canonicalKey(quantity, archetype);
            if (quantity.normalizedValue() == null || !StringUtils.hasText(quantity.normalizedUnit())) {
                throw new InvalidPhysicsInputException("Quantity '" + key + "' is not normalized");
            }
            String previousUnit = units.putIfAbsent(key, quantity.normalizedUnit());
            if (previousUnit != null && !previousUnit.equals(quantity.normalizedUnit())) {
                throw new InvalidPhysicsInputException("Quantity '" + key + "' contains inconsistent units");
            }
            if ("resistances".equals(key)) {
                seriesValues.computeIfAbsent(key, ignored -> new ArrayList<>()).add(quantity.normalizedValue());
            } else if (values.putIfAbsent(key, quantity.normalizedValue()) != null) {
                throw new InvalidPhysicsInputException("Duplicate physical quantity key: " + key);
            }
        }

        SolverInput input = new SolverInput(
                specification.getId(),
                specification.getSchemaVersion().trim(),
                archetype,
                immutableCopy(values),
                immutableSeriesCopy(seriesValues),
                immutableCopy(units));
        inputValidator.validateOrThrow(input);
        return input;
    }

    private void validateSpecification(Specification specification) {
        if (specification == null) {
            throw new InvalidPhysicsInputException("Specification is required");
        }
        if (specification.getConfirmationState() == ConfirmationState.UNRESOLVED) {
            throw new InvalidPhysicsInputException("Specification still has unresolved ambiguities");
        }
        if (!StringUtils.hasText(specification.getTopic())) {
            throw new InvalidPhysicsInputException("Specification topic is required");
        }
        if (!StringUtils.hasText(specification.getSchemaVersion())) {
            throw new InvalidPhysicsInputException("Specification schema version is required");
        }
    }

    private List<PhysicalQuantity> readQuantities(JsonNode quantities) {
        if (quantities == null || !quantities.isArray() || quantities.isEmpty()) {
            throw new InvalidPhysicsInputException("Specification must contain physical quantities");
        }
        List<PhysicalQuantity> result = new ArrayList<>();
        for (JsonNode node : quantities) {
            result.add(readQuantity(node));
        }
        return List.copyOf(result);
    }

    private PhysicalQuantity readQuantity(JsonNode node) {
        try {
            return objectMapper.treeToValue(node, PhysicalQuantity.class);
        } catch (JsonProcessingException exception) {
            throw new InvalidPhysicsInputException("Specification contains an invalid physical quantity", exception);
        }
    }

    private PhysicsArchetype inferArchetype(
            Specification specification,
            List<PhysicalQuantity> quantities) {
        String topic = specification.getTopic().trim().toUpperCase(Locale.ROOT);
        return switch (topic) {
            case "KINEMATICS" -> inferKinematics(quantities);
            case "DYNAMICS" -> inferDynamics(quantities);
            case "CIRCUITS" -> inferCircuit(specification.getRelations());
            default -> throw new InvalidPhysicsInputException("Unsupported physics topic: " + topic);
        };
    }

    private PhysicsArchetype inferKinematics(List<PhysicalQuantity> quantities) {
        if (hasIdentifier(quantities, "acceleration", "a")) {
            return PhysicsArchetype.CONSTANT_ACCELERATION;
        }
        if (hasIdentifier(quantities, "velocity", "v") && hasIdentifier(quantities, "time", "t", "duration")) {
            return PhysicsArchetype.UNIFORM_MOTION;
        }
        throw new InvalidPhysicsInputException(
                "Cannot identify a KINEMATICS archetype from the available quantities");
    }

    private PhysicsArchetype inferDynamics(List<PhysicalQuantity> quantities) {
        if (hasIdentifier(quantities, "angle", "theta")) {
            return PhysicsArchetype.INCLINED_PLANE;
        }
        if (hasIdentifier(quantities, "mass", "m")
                && hasIdentifier(quantities, "force", "appliedforce", "f")) {
            return PhysicsArchetype.HORIZONTAL_FORCE;
        }
        throw new InvalidPhysicsInputException(
                "Cannot identify a DYNAMICS archetype from the available quantities");
    }

    private PhysicsArchetype inferCircuit(JsonNode relationNodes) {
        boolean series = false;
        boolean parallel = false;
        if (relationNodes != null && relationNodes.isArray()) {
            for (JsonNode node : relationNodes) {
                PhysicalRelation relation = readRelation(node);
                String evidence = String.join(" ",
                        safe(relation.type()), safe(relation.sourceText())).toLowerCase(Locale.ROOT);
                series |= evidence.contains("series") || evidence.contains("nối tiếp");
                parallel |= evidence.contains("parallel") || evidence.contains("song song");
            }
        }
        if (series == parallel) {
            throw new InvalidPhysicsInputException(
                    "CIRCUITS requires one explicit SERIES or PARALLEL relation to identify the archetype");
        }
        return series ? PhysicsArchetype.SERIES_CIRCUIT : PhysicsArchetype.PARALLEL_CIRCUIT;
    }

    private PhysicalRelation readRelation(JsonNode node) {
        try {
            return objectMapper.treeToValue(node, PhysicalRelation.class);
        } catch (JsonProcessingException exception) {
            throw new InvalidPhysicsInputException("Specification contains an invalid physical relation", exception);
        }
    }

    private boolean hasIdentifier(List<PhysicalQuantity> quantities, String... candidates) {
        for (PhysicalQuantity quantity : quantities) {
            String name = normalizeIdentifier(quantity.name());
            String symbol = normalizeIdentifier(quantity.symbol());
            for (String candidate : candidates) {
                if (candidate.equals(name) || candidate.equals(symbol)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String canonicalKey(PhysicalQuantity quantity, PhysicsArchetype archetype) {
        String candidate = StringUtils.hasText(quantity.name()) ? quantity.name() : quantity.symbol();
        if (!StringUtils.hasText(candidate)) {
            throw new InvalidPhysicsInputException("Physical quantity must have a symbol or name");
        }
        String normalized = normalizeIdentifier(candidate);
        return switch (normalized) {
            case "x0", "s0", "initialposition", "position" -> "initialPosition";
            case "v0", "initialvelocity" -> "initialVelocity";
            case "velocity", "speed", "v" -> archetype == PhysicsArchetype.CONSTANT_ACCELERATION
                    || archetype == PhysicsArchetype.HORIZONTAL_FORCE
                    || archetype == PhysicsArchetype.INCLINED_PLANE
                            ? "initialVelocity"
                            : archetype == PhysicsArchetype.SERIES_CIRCUIT
                                    || archetype == PhysicsArchetype.PARALLEL_CIRCUIT
                                            ? "voltage"
                                            : "velocity";
            case "time", "t", "duration" -> "duration";
            case "deltatime", "timestep", "dt" -> "deltaTime";
            case "acceleration", "a" -> "acceleration";
            case "mass", "m" -> "mass";
            case "force", "appliedforce", "f" -> "appliedForce";
            case "frictioncoefficient", "coefficientoffriction", "mu" -> "frictionCoefficient";
            case "gravity", "g" -> "gravity";
            case "angle", "theta" -> "angle";
            case "voltage", "u" -> "voltage";
            case "resistance", "resistances", "r" -> "resistances";
            default -> candidate.trim();
        };
    }

    private String normalizeIdentifier(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private <K, V> Map<K, V> immutableCopy(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private Map<String, List<BigDecimal>> immutableSeriesCopy(Map<String, List<BigDecimal>> source) {
        Map<String, List<BigDecimal>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Collections.unmodifiableMap(copy);
    }
}
