package com.example.backend.physics.simulation;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.backend.exception.InvalidPhysicsInputException;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.SimulationFrame;
import com.example.backend.physics.model.SolverInput;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.solver.NumericalSolverLimits;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class SimulationPayloadBuilder {

    public static final String PAYLOAD_VERSION = "simulation-replay-v1";
    public static final int MAX_SNAPSHOT_BYTES = 64 * 1024;
    public static final int MAX_TOTAL_PAYLOAD_BYTES = 8 * 1024 * 1024;

    private static final Set<String> VECTOR_KEYS = Set.of(
            "velocity", "acceleration", "normalForce", "frictionForce", "netForce");

    private final ObjectMapper objectMapper;

    public SimulationPayloadBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RunArtifacts build(SolverInput input, SolverResult result) {
        List<SimulationFrame> frames = replayFrames(result);
        if (frames.size() > NumericalSolverLimits.MAX_FRAMES) {
            throw new InvalidPhysicsInputException(
                    "Simulation run must not exceed " + NumericalSolverLimits.MAX_FRAMES + " snapshots");
        }

        ObjectNode sceneDefinition = sceneDefinition(input.archetype());
        ObjectNode output = objectMapper.createObjectNode();
        output.put("payloadVersion", PAYLOAD_VERSION);
        output.put("archetype", input.archetype().name());
        output.set("summary", summary(result));
        output.set("graph", graph(frames));
        output.set("table", table(frames));

        List<SnapshotPayload> snapshots = new ArrayList<>(frames.size());
        long totalBytes = jsonBytes(output) + jsonBytes(sceneDefinition);
        for (int sequence = 0; sequence < frames.size(); sequence++) {
            SimulationFrame frame = frames.get(sequence);
            JsonNode state = snapshotState(input.archetype(), frame, result);
            int snapshotBytes = jsonBytes(state);
            if (snapshotBytes > MAX_SNAPSHOT_BYTES) {
                throw new InvalidPhysicsInputException(
                        "Snapshot payload exceeds " + MAX_SNAPSHOT_BYTES + " bytes");
            }
            totalBytes += snapshotBytes;
            if (totalBytes > MAX_TOTAL_PAYLOAD_BYTES) {
                throw new InvalidPhysicsInputException(
                        "Simulation replay payload exceeds " + MAX_TOTAL_PAYLOAD_BYTES + " bytes");
            }
            snapshots.add(new SnapshotPayload(sequence, frame.time(), state));
        }
        return new RunArtifacts(output, sceneDefinition, List.copyOf(snapshots));
    }

    private List<SimulationFrame> replayFrames(SolverResult result) {
        if (!result.frames().isEmpty()) {
            return result.frames();
        }
        Map<String, BigDecimal> state = new LinkedHashMap<>(result.outputs());
        return List.of(new SimulationFrame(BigDecimal.ZERO, state));
    }

    private ObjectNode sceneDefinition(PhysicsArchetype archetype) {
        ObjectNode scene = objectMapper.createObjectNode();
        scene.put("payloadVersion", PAYLOAD_VERSION);
        scene.put("archetype", archetype.name());
        scene.put("coordinateSystem", isCircuit(archetype) ? "SCHEMATIC" : "CARTESIAN_1D");
        scene.put("positionKey", isCircuit(archetype) ? null : "position");
        return scene;
    }

    private ObjectNode summary(SolverResult result) {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.set("values", objectMapper.valueToTree(result.outputs()));
        summary.set("series", objectMapper.valueToTree(result.seriesOutputs()));
        return summary;
    }

    private ObjectNode graph(List<SimulationFrame> frames) {
        ObjectNode graph = objectMapper.createObjectNode();
        graph.put("xKey", "time");
        graph.put("xUnit", "s");
        ArrayNode series = graph.putArray("series");
        for (String key : orderedStateKeys(frames)) {
            ObjectNode line = series.addObject();
            line.put("key", key);
            line.put("unit", unitFor(key));
            ArrayNode points = line.putArray("points");
            for (SimulationFrame frame : frames) {
                BigDecimal value = frame.state().get(key);
                if (value != null) {
                    ObjectNode point = points.addObject();
                    point.put("time", frame.time());
                    point.put("value", value);
                }
            }
        }
        return graph;
    }

    private ObjectNode table(List<SimulationFrame> frames) {
        ObjectNode table = objectMapper.createObjectNode();
        ArrayNode columns = table.putArray("columns");
        ObjectNode timeColumn = columns.addObject();
        timeColumn.put("key", "time");
        timeColumn.put("unit", "s");
        List<String> keys = orderedStateKeys(frames);
        keys.forEach(key -> {
            ObjectNode column = columns.addObject();
            column.put("key", key);
            column.put("unit", unitFor(key));
        });
        ArrayNode rows = table.putArray("rows");
        for (SimulationFrame frame : frames) {
            ObjectNode row = rows.addObject();
            row.put("time", frame.time());
            keys.forEach(key -> {
                BigDecimal value = frame.state().get(key);
                if (value != null) {
                    row.put(key, value);
                }
            });
        }
        return table;
    }

    private JsonNode snapshotState(
            PhysicsArchetype archetype,
            SimulationFrame frame,
            SolverResult result) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("payloadVersion", PAYLOAD_VERSION);
        root.put("archetype", archetype.name());
        ObjectNode scene = root.putObject("scene");
        BigDecimal position = frame.state().get("position");
        if (position != null) {
            scene.put("position", position);
            scene.put("positionUnit", "m");
        }
        ObjectNode vectors = root.putObject("vectors");
        frame.state().forEach((key, value) -> {
            if (VECTOR_KEYS.contains(key)) {
                ObjectNode vector = vectors.putObject(key);
                vector.put("value", value);
                vector.put("unit", unitFor(key));
            }
        });
        root.set("values", objectMapper.valueToTree(frame.state()));
        if (isCircuit(archetype)) {
            root.set("series", objectMapper.valueToTree(result.seriesOutputs()));
        }
        return root;
    }

    private List<String> orderedStateKeys(List<SimulationFrame> frames) {
        Set<String> keys = new LinkedHashSet<>();
        frames.forEach(frame -> keys.addAll(frame.state().keySet()));
        return keys.stream().sorted().toList();
    }

    private boolean isCircuit(PhysicsArchetype archetype) {
        return archetype == PhysicsArchetype.SERIES_CIRCUIT
                || archetype == PhysicsArchetype.PARALLEL_CIRCUIT;
    }

    private String unitFor(String key) {
        return switch (key) {
            case "position", "finalPosition" -> "m";
            case "velocity", "finalVelocity" -> "m/s";
            case "acceleration" -> "m/s2";
            case "normalForce", "frictionForce", "netForce" -> "N";
            case "equivalentResistance" -> "ohm";
            case "totalCurrent" -> "A";
            default -> "1";
        };
    }

    private int jsonBytes(JsonNode node) {
        return node.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    public record SnapshotPayload(int sequence, BigDecimal time, JsonNode state) {
    }

    public record RunArtifacts(
            JsonNode outputPayload,
            JsonNode sceneDefinition,
            List<SnapshotPayload> snapshots) {
    }
}
