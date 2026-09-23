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
                immutableCopy(units),
                specification.getEndCondition());
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
        PhysicsArchetype schemaArchetype = inferFromSchemaId(specification.getSchemaId());
        if (schemaArchetype != null) {
            return schemaArchetype;
        }
        String topic = specification.getTopic().trim().toUpperCase(Locale.ROOT);
        return switch (topic) {
            case "KINEMATICS" -> inferKinematics(quantities);
            case "DYNAMICS" -> inferDynamics(quantities);
            case "CIRCUITS" -> inferCircuit(quantities, specification.getRelations());
            default -> throw new InvalidPhysicsInputException("Unsupported physics topic: " + topic);
        };
    }

    private PhysicsArchetype inferFromSchemaId(String schemaId) {
        String normalized = normalizeIdentifier(schemaId);
        if (normalized.contains("projectile")) return PhysicsArchetype.PROJECTILE_MOTION;
        if (normalized.contains("collision")) return PhysicsArchetype.ELASTIC_COLLISION;
        if (normalized.contains("spring") || normalized.contains("oscillation")) {
            return PhysicsArchetype.SPRING_OSCILLATION;
        }
        if (normalized.contains("rccharging")) return PhysicsArchetype.RC_CHARGING;
        if (normalized.contains("rcdischarging")) return PhysicsArchetype.RC_DISCHARGING;
        return null;
    }

    private PhysicsArchetype inferKinematics(List<PhysicalQuantity> quantities) {
        if (hasIdentifier(quantities, "height", "initialheight", "y0")
                && hasIdentifier(quantities, "angle", "theta")
                && hasIdentifier(quantities, "velocity", "initialvelocity", "v0")) {
            return PhysicsArchetype.PROJECTILE_MOTION;
        }
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
        if (hasIdentifier(quantities, "mass1", "m1")
                && hasIdentifier(quantities, "mass2", "m2")
                && hasIdentifier(quantities, "velocity1", "v1")
                && hasIdentifier(quantities, "velocity2", "v2")) {
            return PhysicsArchetype.ELASTIC_COLLISION;
        }
        if (hasIdentifier(quantities, "amplitude")
                && hasIdentifier(quantities, "springconstant", "stiffness", "k")) {
            return PhysicsArchetype.SPRING_OSCILLATION;
        }
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

    private PhysicsArchetype inferCircuit(List<PhysicalQuantity> quantities, JsonNode relationNodes) {
        boolean series = false;
        boolean parallel = false;
        boolean discharging = false;
        if (relationNodes != null && relationNodes.isArray()) {
            for (JsonNode node : relationNodes) {
                PhysicalRelation relation = readRelation(node);
                String evidence = String.join(" ",
                        safe(relation.type()), safe(relation.sourceText())).toLowerCase(Locale.ROOT);
                series |= evidence.contains("series") || evidence.contains("nối tiếp");
                parallel |= evidence.contains("parallel") || evidence.contains("song song");
                discharging |= evidence.contains("discharg") || evidence.contains("phÃ³ng");
            }
        }
        if (hasIdentifier(quantities, "capacitance", "c")) {
            return discharging ? PhysicsArchetype.RC_DISCHARGING : PhysicsArchetype.RC_CHARGING;
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
        String candidate = archetype == PhysicsArchetype.ELASTIC_COLLISION
                && StringUtils.hasText(quantity.symbol())
                        ? quantity.symbol()
                        : StringUtils.hasText(quantity.name()) ? quantity.name() : quantity.symbol();
        if (!StringUtils.hasText(candidate)) {
            throw new InvalidPhysicsInputException("Physical quantity must have a symbol or name");
        }
        String normalized = normalizeIdentifier(candidate);
        return switch (normalized) {
            case "x0", "s0", "initialposition", "position" -> "initialPosition";
            case "y0", "initialheight", "height" -> "initialHeight";
            case "v0", "initialvelocity" -> "initialVelocity";
            case "velocity", "speed", "v" -> archetype == PhysicsArchetype.CONSTANT_ACCELERATION
                    || archetype == PhysicsArchetype.PROJECTILE_MOTION
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
            case "mass1", "m1" -> "mass1";
            case "mass2", "m2" -> "mass2";
            case "position1", "initialposition1", "x1" -> "initialPosition1";
            case "position2", "initialposition2", "x2" -> "initialPosition2";
            case "velocity1", "v1" -> "velocity1";
            case "velocity2", "v2" -> "velocity2";
            case "force", "appliedforce", "f" -> "appliedForce";
            case "frictioncoefficient", "coefficientoffriction", "mu" -> "frictionCoefficient";
            case "gravity", "g" -> "gravity";
            case "angle", "theta" -> "angle";
            case "amplitude" -> "amplitude";
            case "springconstant", "stiffness", "k" -> "springConstant";
            case "phase", "phi" -> "phase";
            case "voltage", "u" -> "voltage";
            case "resistance", "r" -> archetype == PhysicsArchetype.RC_CHARGING
                    || archetype == PhysicsArchetype.RC_DISCHARGING ? "resistance" : "resistances";
            case "capacitance", "c" -> "capacitance";
            case "resistances" -> "resistances";
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
