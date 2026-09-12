package com.example.backend.physics.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ArchetypeDefinition(
        PhysicsArchetype archetype,
        String description,
        List<PhysicsQuantityDefinition> inputs,
        List<PhysicsQuantityDefinition> outputs) {

    public ArchetypeDefinition {
        Objects.requireNonNull(archetype, "Archetype is required");
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        outputs = outputs == null ? List.of() : List.copyOf(outputs);
    }

    public Optional<PhysicsQuantityDefinition> input(String key) {
        return inputs.stream().filter(quantity -> quantity.key().equals(key)).findFirst();
    }
}
