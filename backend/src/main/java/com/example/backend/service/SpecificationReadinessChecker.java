package com.example.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.backend.entity.ConfirmationState;
import com.example.backend.entity.Specification;
import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.adapter.SpecificationToSolverInputAdapter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpecificationReadinessChecker {

    private final SpecificationToSolverInputAdapter adapter;

    public List<String> blockers(Specification specification) {
        List<String> blockers = new ArrayList<>();
        if (specification.getConfirmationState() == ConfirmationState.UNRESOLVED) {
            blockers.add("Unresolved ambiguity cases remain");
        }
        if (!StringUtils.hasText(specification.getTopic())) {
            blockers.add("Topic is missing");
        }
        if (specification.getQuantities() == null || specification.getQuantities().isEmpty()) {
            blockers.add("No physical quantities were extracted");
        }
        if (!blockers.isEmpty()) {
            return List.copyOf(blockers);
        }
        try {
            adapter.convert(specification);
        } catch (InvalidPhysicsInputException exception) {
            blockers.add(exception.getMessage());
        }
        return List.copyOf(blockers);
    }
}
