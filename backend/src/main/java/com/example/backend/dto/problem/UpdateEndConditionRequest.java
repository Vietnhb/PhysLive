package com.example.backend.dto.problem;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;

public record UpdateEndConditionRequest(@NotNull JsonNode endCondition) {
}
