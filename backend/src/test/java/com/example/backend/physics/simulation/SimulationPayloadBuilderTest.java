package com.example.backend.physics.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SimulationFrame;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverMethod;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.NumericalSolverLimits;
import com.fasterxml.jackson.databind.ObjectMapper;

class SimulationPayloadBuilderTest {

    private SimulationPayloadBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SimulationPayloadBuilder(new ObjectMapper());
    }

    @Test
    void buildsStableSceneGraphTableAndSnapshots() {
        SolverInput input = uniformInput();
        SolverResult result = new SolverResult(SolverMethod.NUMERICAL, "uniform-v1",
                Map.of("finalPosition", decimal("6")),
                List.of(frame("0", "0"), frame("1", "2"), frame("3", "6")));

        SimulationPayloadBuilder.RunArtifacts artifacts = builder.build(input, result);

        assertThat(artifacts.outputPayload().get("payloadVersion").asText())
                .isEqualTo(SimulationPayloadBuilder.PAYLOAD_VERSION);
        assertThat(artifacts.outputPayload().get("graph").get("series").isArray()).isTrue();
        assertThat(artifacts.outputPayload().get("table").get("rows")).hasSize(3);
        assertThat(artifacts.snapshots()).hasSize(3);
        assertThat(artifacts.snapshots().get(2).state().get("scene").get("position").decimalValue())
                .isEqualByComparingTo("6");
        assertThat(artifacts.snapshots().get(2).state().get("vectors").get("velocity").get("unit").asText())
                .isEqualTo("m/s");
        assertThat(artifacts.outputPayload().get("graph").get("series").get(0).has("unit")).isTrue();
    }

    @Test
    void createsSingleReplaySnapshotForCircuitResult() {
        SolverInput input = new SolverInput(null, "1.0", PhysicsArchetype.SERIES_CIRCUIT,
                Map.of("voltage", decimal("12")),
                Map.of("resistances", List.of(decimal("10"), decimal("20"))),
                Map.of("voltage", "V", "resistances", "ohm"));
        SolverResult result = new SolverResult(SolverMethod.NUMERICAL, "series-v1",
                Map.of("equivalentResistance", decimal("30"), "totalCurrent", decimal("0.4")),
                Map.of("voltageDrops", List.of(decimal("4"), decimal("8"))), List.of());

        SimulationPayloadBuilder.RunArtifacts artifacts = builder.build(input, result);

        assertThat(artifacts.snapshots()).hasSize(1);
        assertThat(artifacts.snapshots().get(0).time()).isEqualByComparingTo("0");
        assertThat(artifacts.snapshots().get(0).state().get("series").get("voltageDrops")).hasSize(2);
    }

    @Test
    void rejectsMoreThanMaximumFrames() {
        List<SimulationFrame> frames = new ArrayList<>();
        for (int index = 0; index <= NumericalSolverLimits.MAX_FRAMES; index++) {
            frames.add(frame(Integer.toString(index), Integer.toString(index)));
        }
        SolverResult result = new SolverResult(SolverMethod.NUMERICAL, "uniform-v1",
                Map.of("finalPosition", BigDecimal.ZERO), frames);

        assertThatThrownBy(() -> builder.build(uniformInput(), result))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("snapshots");
    }

    @Test
    void rejectsOversizedSingleSnapshotPayload() {
        Map<String, BigDecimal> hugeState = new java.util.LinkedHashMap<>();
        for (int index = 0; index < 5_000; index++) {
            hugeState.put("measurement" + index, BigDecimal.valueOf(index));
        }
        SolverResult result = new SolverResult(SolverMethod.NUMERICAL, "uniform-v1",
                Map.of("finalPosition", BigDecimal.ZERO),
                List.of(new SimulationFrame(BigDecimal.ZERO, hugeState)));

        assertThatThrownBy(() -> builder.build(uniformInput(), result))
                .isInstanceOf(InvalidPhysicsInputException.class)
                .hasMessageContaining("payload");
    }

    private SolverInput uniformInput() {
        return new SolverInput(null, "1.0", PhysicsArchetype.UNIFORM_MOTION,
                Map.of("velocity", decimal("2"), "duration", decimal("3")),
                Map.of("velocity", "m/s", "duration", "s"));
    }

    private SimulationFrame frame(String time, String position) {
        return new SimulationFrame(decimal(time),
                Map.of("position", decimal(position), "velocity", decimal("2"),
                        "acceleration", BigDecimal.ZERO));
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
