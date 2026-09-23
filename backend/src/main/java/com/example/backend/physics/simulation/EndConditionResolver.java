package com.example.backend.physics.simulation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.dto.simulation.ResolvedEnd;
import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.SimulationFrame;
import com.example.backend.physics.model.SolverResult;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class EndConditionResolver {
    public static final BigDecimal MAX_DYNAMIC_SECONDS = new BigDecimal("300");
    private static final MathContext MC = MathContext.DECIMAL128;

    public ResolvedEnd resolve(JsonNode condition, SolverResult result) {
        List<SimulationFrame> frames = result.frames();
        if (frames.isEmpty()) return new ResolvedEnd(BigDecimal.ZERO, "no_frames", false);
        BigDecimal horizon = frames.get(frames.size() - 1).time();
        if (condition == null || condition.isNull() || condition.isMissingNode()) {
            return new ResolvedEnd(horizon, "time_limit", true);
        }
        validate(condition);
        String type = condition.path("type").asText("time_limit").trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "time_limit" -> new ResolvedEnd(limit(condition, horizon), "time_limit", true);
            case "threshold" -> threshold(condition, frames, horizon);
            case "event" -> event(condition.path("event"), frames, horizon);
            case "cycle_count" -> cycles(condition, frames, horizon);
            default -> throw new InvalidPhysicsInputException("Unsupported end condition type: " + type);
        };
    }

    public boolean expandable(JsonNode condition, ResolvedEnd resolved, BigDecimal horizon) {
        if (condition == null || resolved.conditionReached() || horizon.compareTo(MAX_DYNAMIC_SECONDS) >= 0) return false;
        String type = condition.path("type").asText("time_limit");
        return !"time_limit".equalsIgnoreCase(type) && !condition.has("maxTime");
    }

    public BigDecimal requestedHorizon(JsonNode condition, BigDecimal fallback) {
        if (condition == null || condition.isNull()) return fallback;
        validate(condition);
        if ("time_limit".equalsIgnoreCase(condition.path("type").asText("time_limit"))
                && condition.path("duration").isNumber()) {
            return condition.path("duration").decimalValue();
        }
        if (condition.path("maxTime").isNumber()) return condition.path("maxTime").decimalValue();
        return fallback;
    }

    public SolverResult trim(SolverResult result, BigDecimal endTime) {
        if (result.frames().isEmpty()) return result;
        List<SimulationFrame> source = result.frames();
        List<SimulationFrame> trimmed = new ArrayList<>();
        for (SimulationFrame frame : source) {
            if (frame.time().compareTo(endTime) <= 0) trimmed.add(frame);
            else break;
        }
        if (trimmed.isEmpty()) trimmed.add(source.get(0));
        SimulationFrame last = trimmed.get(trimmed.size() - 1);
        if (last.time().compareTo(endTime) < 0 && trimmed.size() < source.size()) {
            SimulationFrame next = source.get(trimmed.size());
            trimmed.add(interpolate(last, next, endTime));
        }
        return new SolverResult(result.method(), result.solverVersion(), outputsAtEnd(result.outputs(), trimmed.get(trimmed.size() - 1)),
                result.seriesOutputs(), List.copyOf(trimmed));
    }

    private Map<String, BigDecimal> outputsAtEnd(Map<String, BigDecimal> outputs, SimulationFrame frame) {
        Map<String, BigDecimal> adjusted = new LinkedHashMap<>(outputs);
        Map<String, String> stateByOutput = Map.ofEntries(
                Map.entry("finalPosition", "position"),
                Map.entry("finalVelocity", "velocity"),
                Map.entry("finalPositionX", "positionX"),
                Map.entry("finalPositionY", "positionY"),
                Map.entry("finalVelocityX", "velocityX"),
                Map.entry("finalVelocityY", "velocityY"),
                Map.entry("finalPosition1", "position1"),
                Map.entry("finalPosition2", "position2"),
                Map.entry("finalVelocity1", "velocity1"),
                Map.entry("finalVelocity2", "velocity2"),
                Map.entry("finalVoltage", "voltage"),
                Map.entry("finalCurrent", "current"));
        stateByOutput.forEach((output, state) -> {
            BigDecimal value = frame.state().get(state);
            if (value != null && adjusted.containsKey(output)) adjusted.put(output, value);
        });
        return adjusted;
    }

    private ResolvedEnd threshold(JsonNode condition, List<SimulationFrame> frames, BigDecimal horizon) {
        String key = leaf(condition.path("quantity").asText());
        String operator = condition.path("operator").asText(">=");
        BigDecimal target = decimal(condition, "value");
        for (int index = 0; index < frames.size(); index++) {
            BigDecimal value = frames.get(index).state().get(key);
            if (value == null) continue;
            if (matches(value, target, operator)) {
                if (index == 0) return new ResolvedEnd(frames.get(0).time(), "threshold", true);
                SimulationFrame before = frames.get(index - 1);
                BigDecimal previous = before.state().get(key);
                BigDecimal time = previous == null ? frames.get(index).time()
                        : crossingTime(before.time(), frames.get(index).time(), previous, value, target);
                return new ResolvedEnd(time, "threshold", true);
            }
        }
        return unresolved(condition, horizon);
    }

    private ResolvedEnd event(JsonNode event, List<SimulationFrame> frames, BigDecimal horizon) {
        String type = event.path("type").asText().toLowerCase(Locale.ROOT);
        if ("contact".equals(type)) {
            String key = hasKey(frames, "positionY") ? "positionY" : "position";
            return zeroCrossing(frames, key, null, "event_contact", horizon);
        }
        if ("collision".equals(type)) {
            return zeroCrossing(frames, "position1", "position2", "event_collision", horizon);
        }
        throw new InvalidPhysicsInputException("Unsupported end event type: " + type);
    }

    private ResolvedEnd zeroCrossing(List<SimulationFrame> frames, String leftKey, String rightKey,
            String reason, BigDecimal horizon) {
        for (int index = 1; index < frames.size(); index++) {
            BigDecimal before = difference(frames.get(index - 1), leftKey, rightKey);
            BigDecimal after = difference(frames.get(index), leftKey, rightKey);
            if (before == null || after == null) continue;
            if (before.signum() != 0 && (after.signum() == 0 || before.signum() != after.signum())) {
                return new ResolvedEnd(crossingTime(frames.get(index - 1).time(), frames.get(index).time(),
                        before, after, BigDecimal.ZERO), reason, true);
            }
        }
        return new ResolvedEnd(horizon, "max_time", false);
    }

    private ResolvedEnd cycles(JsonNode condition, List<SimulationFrame> frames, BigDecimal horizon) {
        String requested = leaf(condition.path("quantity").asText("position"));
        String key = hasKey(frames, requested) ? requested : hasKey(frames, "position") ? "position" : requested;
        int required = condition.path("count").asInt();
        int peaks = 0;
        for (int index = 1; index < frames.size() - 1; index++) {
            BigDecimal before = frames.get(index - 1).state().get(key);
            BigDecimal value = frames.get(index).state().get(key);
            BigDecimal after = frames.get(index + 1).state().get(key);
            if (before != null && value != null && after != null
                    && value.compareTo(before) >= 0 && value.compareTo(after) > 0) {
                if (++peaks >= required) return new ResolvedEnd(frames.get(index).time(), "cycle_count", true);
            }
        }
        return unresolved(condition, horizon);
    }

    private ResolvedEnd unresolved(JsonNode condition, BigDecimal horizon) {
        BigDecimal end = condition.has("maxTime") ? decimal(condition, "maxTime").min(horizon) : horizon;
        return new ResolvedEnd(end, "max_time", false);
    }

    private BigDecimal limit(JsonNode condition, BigDecimal horizon) {
        JsonNode value = condition.has("duration") ? condition.get("duration") : condition.get("maxTime");
        return value != null && value.isNumber() ? value.decimalValue().min(horizon) : horizon;
    }

    private void validate(JsonNode condition) {
        String type = condition.path("type").asText("time_limit").toLowerCase(Locale.ROOT);
        if (!List.of("time_limit", "threshold", "event", "cycle_count").contains(type)) {
            throw new InvalidPhysicsInputException("Unsupported end condition type: " + type);
        }
        if (condition.has("maxTime")) positiveBounded(condition.get("maxTime"), "endCondition.maxTime");
        if ("time_limit".equals(type) && condition.has("duration")) {
            positiveBounded(condition.get("duration"), "endCondition.duration");
        }
        if ("threshold".equals(type) && (!condition.path("quantity").isTextual()
                || !condition.path("value").isNumber())) {
            throw new InvalidPhysicsInputException("Threshold end condition requires quantity and numeric value");
        }
        if ("cycle_count".equals(type) && condition.path("count").asInt() <= 0) {
            throw new InvalidPhysicsInputException("Cycle-count end condition requires count > 0");
        }
        if ("event".equals(type) && !condition.path("event").path("type").isTextual()) {
            throw new InvalidPhysicsInputException("Event end condition requires event.type");
        }
    }

    private void positiveBounded(JsonNode value, String field) {
        if (!value.isNumber() || value.decimalValue().signum() <= 0
                || value.decimalValue().compareTo(MAX_DYNAMIC_SECONDS) > 0) {
            throw new InvalidPhysicsInputException(field + " must be positive and <= " + MAX_DYNAMIC_SECONDS);
        }
    }

    private SimulationFrame interpolate(SimulationFrame before, SimulationFrame after, BigDecimal targetTime) {
        BigDecimal span = after.time().subtract(before.time());
        BigDecimal ratio = span.signum() == 0 ? BigDecimal.ZERO
                : targetTime.subtract(before.time()).divide(span, MC);
        Map<String, BigDecimal> state = new LinkedHashMap<>();
        before.state().forEach((key, value) -> {
            BigDecimal next = after.state().get(key);
            state.put(key, next == null ? value : value.add(next.subtract(value).multiply(ratio, MC), MC));
        });
        return new SimulationFrame(targetTime, state);
    }

    private BigDecimal crossingTime(BigDecimal beforeTime, BigDecimal afterTime,
            BigDecimal before, BigDecimal after, BigDecimal target) {
        BigDecimal range = after.subtract(before);
        if (range.signum() == 0) return afterTime;
        BigDecimal ratio = target.subtract(before).divide(range, MC);
        return beforeTime.add(afterTime.subtract(beforeTime).multiply(ratio, MC), MC);
    }

    private boolean matches(BigDecimal value, BigDecimal target, String operator) {
        int comparison = value.compareTo(target);
        return switch (operator) {
            case ">" -> comparison > 0;
            case ">=" -> comparison >= 0;
            case "<" -> comparison < 0;
            case "<=" -> comparison <= 0;
            case "==", "=" -> comparison == 0;
            default -> throw new InvalidPhysicsInputException("Unsupported threshold operator: " + operator);
        };
    }

    private BigDecimal difference(SimulationFrame frame, String leftKey, String rightKey) {
        BigDecimal left = frame.state().get(leftKey);
        if (left == null) return null;
        return rightKey == null ? left : frame.state().get(rightKey) == null
                ? null : left.subtract(frame.state().get(rightKey));
    }

    private boolean hasKey(List<SimulationFrame> frames, String key) {
        return frames.stream().anyMatch(frame -> frame.state().containsKey(key));
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) throw new InvalidPhysicsInputException(field + " must be numeric");
        return value.decimalValue();
    }

    private String leaf(String path) {
        if (path == null || path.isBlank()) return "position";
        int dot = path.lastIndexOf('.');
        String value = dot < 0 ? path : path.substring(dot + 1);
        return switch (value) {
            case "x" -> "position";
            case "y" -> "positionY";
            default -> value;
        };
    }
}
