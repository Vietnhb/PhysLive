package com.example.backend.physics.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.backend.physics.model.SimulationFrame;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class EndConditionResolverTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final EndConditionResolver resolver = new EndConditionResolver();

    @Test
    void resolvesAndTrimsThresholdAtInterpolatedTime() throws Exception {
        var condition = mapper.readTree(
                "{\"type\":\"threshold\",\"quantity\":\"positions.x\",\"operator\":\">=\",\"value\":10}");
        SolverResult result = result(List.of(
                frame("0", "1"), frame("1", "5"), frame("2", "13")));
        var end = resolver.resolve(condition, result);
        var trimmed = resolver.trim(result, end.time());
        assertThat(end.time()).isEqualByComparingTo("1.625");
        assertThat(end.conditionReached()).isTrue();
        assertThat(trimmed.frames().get(trimmed.frames().size() - 1).state().get("position"))
                .isEqualByComparingTo("10");
    }

    @Test
    void resolvesCollisionEvent() throws Exception {
        var condition = mapper.readTree(
                "{\"type\":\"event\",\"event\":{\"type\":\"collision\",\"entities\":[\"left\",\"right\"]}}");
        SolverResult result = new SolverResult(SolverMethod.NUMERICAL, "test", Map.of(), List.of(
                new SimulationFrame(d("0"), Map.of("position1", d("0"), "position2", d("3"))),
                new SimulationFrame(d("1"), Map.of("position1", d("1"), "position2", d("2"))),
                new SimulationFrame(d("2"), Map.of("position1", d("2"), "position2", d("1")))));
        assertThat(resolver.resolve(condition, result).time()).isEqualByComparingTo("1.5");
    }

    private SolverResult result(List<SimulationFrame> frames) {
        return new SolverResult(SolverMethod.NUMERICAL, "test", Map.of(), frames);
    }

    private SimulationFrame frame(String time, String position) {
        return new SimulationFrame(d(time), Map.of("position", d(position)));
    }

    private BigDecimal d(String value) { return new BigDecimal(value); }
}
