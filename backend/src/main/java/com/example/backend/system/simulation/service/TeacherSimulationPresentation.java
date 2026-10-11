package com.example.backend.system.simulation.service;

import com.example.backend.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

/** Server-owned presentation contract for the teacher creation workspace only. */
final class TeacherSimulationPresentation {
    private TeacherSimulationPresentation() { }

    static ObjectNode create(ObjectNode timeline, JsonNode spec, JsonNode metadata, JsonNode overrides) {
        if (!timeline.path("frames").isArray() || timeline.path("frames").isEmpty())
            throw ApiException.unprocessable("Mô phỏng thiếu mẫu dữ liệu từ máy chủ");
        ObjectNode result = timeline.objectNode().put("version", 1);
        try {
            result.put("frameResolver", new ClassPathResource("prompts/teacher-frame-resolver.js")
                    .getContentAsString(StandardCharsets.UTF_8));
        } catch (java.io.IOException error) { throw new IllegalStateException("Teacher frame contract unavailable", error); }
        ObjectNode ranges = result.putObject("fieldRanges");
        double previous = 0;
        boolean first = true;
        for (JsonNode item : timeline.path("frames")) {
            ObjectNode frame = (ObjectNode) item;
            double t = frame.path("t").asDouble();
            frame.put("dt", first ? 0 : t - previous);
            previous = t; first = false;
            frame.path("values").fields().forEachRemaining(entry -> {
                double value = entry.getValue().asDouble();
                ObjectNode range = ranges.has(entry.getKey()) ? (ObjectNode) ranges.get(entry.getKey()) : ranges.putObject(entry.getKey()).put("min", value).put("max", value);
                range.put("min", Math.min(range.path("min").asDouble(), value));
                range.put("max", Math.max(range.path("max").asDouble(), value));
            });
        }
        ObjectNode parameters = result.putObject("parameters");
        for (JsonNode parameter : spec.path("parameters")) {
            String name = parameter.path("name").asText();
            parameters.set(name, overrides != null && overrides.has(name) ? overrides.get(name) : parameter.path("value"));
        }
        result.set("initialFrame", timeline.path("frames").path(0).deepCopy());
        ObjectNode descriptor = result.putObject("scene");
        descriptor.set("durationSeconds", timeline.path("durationSeconds"));
        ObjectNode fields = descriptor.putObject("fields"), constants = descriptor.putObject("constants");
        Map<String, String> participants = new LinkedHashMap<>();
        metadata.fields().forEachRemaining(entry -> {
            JsonNode meta = entry.getValue(); String key = entry.getKey();
            String participant = meta.path("participantId").asText("");
            if (meta.has("constant")) {
                constants.putObject(key).put("label", meta.path("label").asText(key)).put("unit", meta.path("unit").asText()).set("value", meta.get("constant"));
            }
            if (!ranges.has(key)) return;
            participants.putIfAbsent(participant, meta.path("participantLabel").asText(participant));
            ObjectNode field = fields.putObject(key).put("key", key).put("participantId", participant)
                    .put("quantity", meta.path("quantity").asText(key)).put("label", meta.path("label").asText(key))
                    .put("symbol", meta.path("symbol").asText(meta.path("label").asText(key))).put("unit", meta.path("unit").asText());
            field.set("min", ranges.path(key).path("min")); field.set("max", ranges.path(key).path("max"));
            if (meta.has("rendererRole")) field.set("role", meta.get("rendererRole"));
        });
        ranges.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (fields.has(key)) return;
            participants.putIfAbsent("_", "_");
            ObjectNode field = fields.putObject(key).put("key", key).put("participantId", "_")
                    .put("quantity", key).put("label", key).put("symbol", key).put("unit", "");
            field.set("min", entry.getValue().path("min"));
            field.set("max", entry.getValue().path("max"));
        });
        var people = descriptor.putArray("participants");
        participants.forEach((id, label) -> people.addObject().put("id", id).put("label", label).put("colorIndex", people.size()));
        var observables = descriptor.putArray("observables");
        for (JsonNode observable : spec.path("observables")) {
            String key = observable.path("field").asText();
            if (!fields.has(key)) continue;
            observables.add(key); ObjectNode field = (ObjectNode) fields.get(key);
            if (observable.has("label")) field.set("caption", observable.get("label"));
            if (observable.has("object")) field.set("object", observable.get("object"));
        }
        ObjectNode controls = descriptor.putObject("parameters");
        for (JsonNode parameter : spec.path("parameters")) {
            String name = parameter.path("name").asText();
            controls.putObject(name).put("label", parameter.path("label").asText(name)).put("unit", parameter.path("unit").asText());
        }
        return result;
    }
}
