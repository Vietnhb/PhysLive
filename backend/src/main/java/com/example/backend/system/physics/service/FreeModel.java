package com.example.backend.system.physics.service;

import com.example.backend.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A model written for one description: its quantities, each with a unit, a role and an equation. The equation
 * solver computes it exactly as it computes an approved law, and the curriculum's laws then check the result.
 * Nothing here knows any particular situation.
 */
public final class FreeModel {
    /** The participant id under which the model's quantities appear in the timeline ("model.x"). */
    public static final String ID = "model";
    private static final List<String> ATTRIBUTES = List.of("role", "of", "axis", "kind", "by");

    private final ObjectMapper json;
    private final SchemaEquationRuntime equations;

    public FreeModel(ObjectMapper json, SchemaEquationRuntime equations) {
        this.json = json;
        this.equations = equations;
    }

    /** Solves the model of a plan and checks the run against the catalogue's laws. */
    public ObjectNode compute(JsonNode catalogue, JsonNode authored, JsonNode overrides) {
        require(authored.path("model").path("quantities").isArray() && !authored.path("model").path("quantities").isEmpty(), "Mô hình chưa có đại lượng nào: simulationSpec.model.quantities phải là một danh sách, mỗi phần tử có id, label, unit, role và value (hoặc initial và rate)");
        for (JsonNode object : authored.path("objects"))
            require(object.path("id").asText().matches("[A-Za-z_][A-Za-z0-9_]{0,31}") && !object.path("id").asText().equals(ID),
                    "Mỗi đối tượng cần một id riêng gồm chữ, số, gạch dưới (không dùng \"" + ID + "\"): " + object.path("id").asText());
        boolean[] random = {false};
        Run run = solve(catalogue, plain(authored, 0, random), overrides);
        Map<String, double[]> noise = new LinkedHashMap<>(), means = new LinkedHashMap<>();
        boolean expects = false;
        for (JsonNode quantity : authored.path("model").path("quantities")) expects |= quantity.hasNonNull("expected");
        if (random[0] && expects) {
            // a model that draws random numbers is run again with other draws: what differs between the runs is chance
            List<Run> others = java.util.stream.IntStream.rangeClosed(1, EXTRA_DRAWS).parallel()
                    .mapToObj(draw -> solve(catalogue, plain(authored, draw, new boolean[1]), overrides)).toList();
            for (int index = 0; index < run.symbols.size(); index++) {
                LawChecks.Symbol symbol = run.symbols.get(index);
                if (!run.chancy.contains(symbol.id())) continue;
                // the mean over the draws, and how far a mean of that many draws can be off by chance (four standard
                // errors, the spread pooled over neighbouring instants: few draws tell the spread of one instant poorly)
                int count = others.size() + 1, length = run.time.length, reach = Math.max(1, length / 10);
                double[] mean = new double[length], variance = new double[length], chance = new double[length];
                for (int i = 0; i < length; i++) {
                    double sum = symbol.values()[i], squares = sum * sum;
                    for (Run other : others) { double value = at(other.time, other.symbols.get(index).values(), run.time[i]); sum += value; squares += value * value; }
                    mean[i] = sum / count;
                    variance[i] = Math.max(0, (squares - sum * sum / count) / (count - 1));
                }
                for (int i = 0; i < length; i++) {
                    double pooled = 0;
                    int from = Math.max(0, i - reach), to = Math.min(length - 1, i + reach);
                    for (int k = from; k <= to; k++) pooled += variance[k];
                    chance[i] = 4 * Math.sqrt(pooled / (to - from + 1) / count);
                }
                noise.putIfAbsent(symbol.id(), chance);
                means.putIfAbsent(symbol.id(), mean);
            }
        }
        return verdict(catalogue, run, noise, means);
    }

    private static final int EXTRA_DRAWS = 5, MAX_MEMBERS = 400, MAX_EXPANDED = 1200, SAMPLED_MEMBERS = 4, MAX_FIELD_CELLS = 2500, MAX_FIELD_VALUES = 150_000, MAX_REPORTED_VALUES = 120_000;
    private static final Set<String> OVER_GROUP = Set.of("total", "mean", "largest", "smallest");

    private record Run(JsonNode model, ObjectNode computed, List<LawChecks.Symbol> symbols, double[] time, Map<String, boolean[]> active, Set<String> stopping, Set<String> chancy, Map<Double, Set<String>> impacts) { }

    private static double at(double[] time, double[] values, double t) {
        if (time.length == 0) return 0;
        int high = java.util.Arrays.binarySearch(time, t);
        if (high >= 0) return values[high];
        high = -high - 1;
        if (high == 0) return values[0];
        if (high >= time.length) return values[time.length - 1];
        double share = (t - time[high - 1]) / (time[high] - time[high - 1]);
        return values[high - 1] + share * (values[high] - values[high - 1]);
    }

    private static String safe(String object) { return object.replaceAll("\\W", "_"); }

    /**
     * The authored model written out in full: every quantity of a group once per member, a group read from outside
     * as a sum, a mean or an extreme, each random draw as a fixed number of this run, and whether an object is
     * there as a quantity of its own. What is left is a model of plain quantities.
     */
    private ObjectNode plain(JsonNode authored, int draw, boolean[] random) {
        ObjectNode spec = (ObjectNode) authored.deepCopy();
        ObjectNode model = (ObjectNode) spec.path("model");
        Map<String, Integer> groups = new LinkedHashMap<>();
        for (JsonNode object : authored.path("objects")) if (object.path("members").asInt(1) > 1) {
            require(object.path("members").asInt() <= MAX_MEMBERS, "Nhóm " + object.path("id").asText() + " có quá nhiều phần tử (tối đa " + MAX_MEMBERS + ")");
            groups.put(object.path("id").asText(), object.path("members").asInt());
        }
        Map<String, String> memberOf = new LinkedHashMap<>();
        int expanded = 0;
        for (JsonNode quantity : authored.path("model").path("quantities")) if (groups.containsKey(quantity.path("of").asText(""))) {
            memberOf.put(quantity.path("id").asText(), quantity.path("of").asText());
            expanded += groups.get(quantity.path("of").asText());
        }
        require(expanded <= MAX_EXPANDED, "Các nhóm có quá nhiều đại lượng theo từng phần tử (" + expanded + ", tối đa " + MAX_EXPANDED + "): hãy giảm số phần tử hoặc số đại lượng của mỗi phần tử");
        long seed = authored.path("model").path("seed").asLong(1);
        Map<String, Double> draws = new LinkedHashMap<>();
        class Writer {
            JsonNode write(JsonNode ast, String group, int index) {
                if (ast.isTextual()) {
                    String name = ast.asText();
                    if (name.equals("index") || name.equals("members")) {
                        require(group != null, "\"" + name + "\" chỉ dùng được trong đại lượng của một nhóm (đối tượng có members)");
                        return json.getNodeFactory().numberNode(name.equals("index") ? index : groups.get(group));
                    }
                    if (!memberOf.containsKey(name)) return ast;
                    require(memberOf.get(name).equals(group), "Đại lượng " + name + " thuộc từng phần tử của nhóm " + memberOf.get(name)
                            + ": từ bên ngoài nhóm hãy đọc nó bằng total, mean, largest, smallest hoặc [\"member\", \"" + name + "\", số thứ tự]");
                    return json.getNodeFactory().textNode(name + "__" + index);
                }
                if (!ast.isArray() || ast.isEmpty()) return ast;
                String op = ast.get(0).asText();
                if (op.equals("random")) {
                    require(ast.size() == 2 && ast.get(1).isInt() && ast.get(1).asInt() >= 0 && ast.get(1).asInt() < 16, "random cần một số thứ tự nguyên từ 0 đến 15");
                    String name = "rnd_" + (group == null ? "" : group + "_") + ast.get(1).asInt() + (group == null ? "" : "__" + index);
                    random[0] = true;
                    draws.computeIfAbsent(name, key -> {
                        // every (seed, draw, name) gets a number of its own, unrelated to its neighbours'
                        long z = seed * 0x9E3779B97F4A7C15L + draw * 0xBF58476D1CE4E5B9L + key.hashCode() * 0x94D049BB133111EBL;
                        for (int round = 0; round < 2; round++) { z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L; z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL; z ^= z >>> 31; }
                        double value = (z >>> 11) * 0x1.0p-53;
                        return value <= 0 ? Double.MIN_NORMAL : value;
                    });
                    return json.getNodeFactory().textNode(name);
                }
                if (OVER_GROUP.contains(op) || op.equals("member")) {
                    require(ast.size() >= 2 && ast.get(1).isTextual() && memberOf.containsKey(ast.get(1).asText()), op + " cần tên một đại lượng của nhóm (đối tượng có members)");
                    String name = ast.get(1).asText();
                    int size = groups.get(memberOf.get(name));
                    if (op.equals("member")) {
                        require(ast.size() == 3 && ast.get(2).isInt() && ast.get(2).asInt() >= 0 && ast.get(2).asInt() < size, "member cần số thứ tự nguyên từ 0 đến " + (size - 1));
                        return json.getNodeFactory().textNode(name + "__" + ast.get(2).asInt());
                    }
                    require(ast.size() == 2, op + " cần đúng một tên đại lượng");
                    JsonNode all = fold(op.equals("largest") ? "max" : op.equals("smallest") ? "min" : "add", name, 0, size);
                    return op.equals("mean") ? json.createArrayNode().add("div").add(all).add(size) : all;
                }
                ArrayNode copy = json.createArrayNode().add(op);
                for (int i = 1; i < ast.size(); i++) copy.add(write(ast.get(i), group, index));
                return copy;
            }
            JsonNode fold(String op, String name, int from, int to) {
                if (to - from == 1) return json.getNodeFactory().textNode(name + "__" + from);
                int middle = (from + to) / 2;
                return json.createArrayNode().add(op).add(fold(op, name, from, middle)).add(fold(op, name, middle, to));
            }
            void parts(ObjectNode quantity, String group, int index) {
                for (String part : List.of("value", "initial", "rate")) if (quantity.has(part)) quantity.set(part, write(quantity.get(part), group, index));
                for (JsonNode jump : quantity.path("jumps")) for (String part : List.of("when", "to")) ((ObjectNode) jump).set(part, write(jump.path(part), group, index));
            }
        }
        Writer writer = new Writer();
        ArrayNode objects = spec.putArray("objects"), quantities = model.putArray("quantities"), untils = json.createArrayNode();
        List<ObjectNode> presence = new ArrayList<>();
        for (JsonNode object : authored.path("objects")) {
            String id = object.path("id").asText();
            for (int i = 0; i < groups.getOrDefault(id, 1); i++) {
                boolean member = groups.containsKey(id);
                ObjectNode one = objects.addObject().put("id", member ? id + "[" + i + "]" : id).put("label", object.path("label").asText(id) + (member ? " " + (i + 1) : ""));
                if (!object.has("present")) continue;
                ObjectNode there = json.createObjectNode().put("id", "present_" + safe(one.path("id").asText())).put("label", "có mặt").put("unit", "1").put("role", "other")
                        .put("of", one.path("id").asText()).put("key", one.path("id").asText() + ".present").put("bookkeeping", true);
                there.set("value", json.createArrayNode().add("if").add(writer.write(object.path("present"), member ? id : null, i)).add(1).add(0));
                presence.add(there);
            }
        }
        for (JsonNode quantity : authored.path("model").path("quantities")) {
            String group = memberOf.get(quantity.path("id").asText());
            for (int i = 0; i < (group == null ? 1 : groups.get(group)); i++) {
                ObjectNode one = (ObjectNode) quantity.deepCopy();
                if (group != null) {
                    one.put("id", quantity.path("id").asText() + "__" + i).put("of", group + "[" + i + "]").put("key", group + "[" + i + "]." + quantity.path("id").asText())
                            .put("source", quantity.path("id").asText());
                    if (i >= SAMPLED_MEMBERS) one.put("unsampled", true);
                }
                writer.parts(one, group, i);
                quantities.add(one);
            }
        }
        presence.forEach(quantities::add);
        JsonNode ends = authored.path("model").path("until");
        for (JsonNode until : ends.isArray() ? ends : ends.isObject() ? json.createArrayNode().add(ends) : json.createArrayNode()) {
            String group = memberOf.get(until.path("quantity").asText());
            if (group == null && groups.containsKey(until.path("of").asText(""))) group = until.path("of").asText();
            for (int i = 0; i < (group == null ? 1 : groups.get(group)); i++) {
                ObjectNode one = (ObjectNode) until.deepCopy();
                if (group != null) {
                    if (memberOf.containsKey(until.path("quantity").asText())) one.put("quantity", until.path("quantity").asText() + "__" + i);
                    one.put("of", group + "[" + i + "]");
                }
                untils.add(one);
            }
        }
        if (!untils.isEmpty()) model.set("until", untils);
        if (model.has("duration")) model.set("duration", writer.write(model.get("duration"), null, 0));
        ArrayNode constants = model.withArray("constants");
        draws.forEach((name, value) -> constants.addObject().put("id", name).put("label", name).put("value", value).put("unit", "1"));
        return spec;
    }

    private Run solve(JsonNode catalogue, JsonNode spec, JsonNode overrides) {
        JsonNode model = spec.path("model");
        Set<String> names = new LinkedHashSet<>();
        Map<String, String> objects = new LinkedHashMap<>();
        for (JsonNode object : spec.path("objects")) {
            String id = object.path("id").asText();
            require(objects.put(id, object.path("label").asText(id)) == null, "Mỗi đối tượng cần một id riêng: " + id);
        }
        Map<String, Double> fixed = new LinkedHashMap<>();
        Map<String, JsonNode> axes = new LinkedHashMap<>(), fields = new LinkedHashMap<>();
        for (JsonNode quantity : model.path("quantities")) if (quantity.has("along")) fields.put(quantity.path("id").asText(), quantity);
        ObjectNode capability = json.createObjectNode().put("capabilityId", ID).put("title", "model").put("version", "1.0");
        ArrayNode inputs = capability.putArray("canonicalInputs"), outputs = capability.putArray("outputs");
        ObjectNode math = capability.putObject("execution").putObject("math");
        ObjectNode initial = math.putObject("initial"), rates = math.putObject("rates"), results = math.putObject("outputs"), jumps = json.createObjectNode();
        // No reference solution exists for a model written for one description: the solver checks it by step refinement.
        capability.putObject("validation").put("absoluteTolerance", 0).put("relativeTolerance", 1e-3).put("strategy", "step_refinement").putArray("invariants").add("finite_output");
        ObjectNode bindings = json.createObjectNode();
        for (JsonNode parameter : spec.path("parameters")) {
            String name = name(parameter.path("name").asText(), names), unit = parameter.path("unit").asText();
            String canonical = equations.canonical(unit);
            require(canonical != null, "Đơn vị của thanh trượt " + name + " không có trong danh mục: " + unit);
            inputs.addObject().put("key", name).put("unit", canonical);
            bindings.put(name, name);
            fixed.put(name, equations.convert(overrides != null && overrides.has(name) ? overrides.path(name).asDouble() : parameter.path("value").asDouble(), unit, canonical));
        }
        for (JsonNode constant : model.path("constants")) {
            String name = name(constant.path("id").asText(), names), unit = constant.path("unit").asText();
            String canonical = equations.canonical(unit);
            require(canonical != null, "Đơn vị của hằng số " + name + " không có trong danh mục: " + unit);
            require(constant.path("value").isNumber(), "Hằng số " + name + " thiếu giá trị");
            inputs.addObject().put("key", name).put("unit", canonical);
            bindings.put(name, equations.convert(constant.path("value").asDouble(), unit, canonical));
            fixed.put(name, equations.convert(constant.path("value").asDouble(), unit, canonical));
        }
        // a universal constant of the laws (G, R, h, c…) can be used by its name without declaring it, unless the model uses that name itself
        Set<String> own = new LinkedHashSet<>(names), used = new LinkedHashSet<>();
        model.path("quantities").forEach(quantity -> {
            own.add(quantity.path("id").asText());
            for (String part : List.of("value", "initial", "rate")) mentioned(quantity.path(part), used);
            for (JsonNode jump : quantity.path("jumps")) { mentioned(jump.path("when"), used); mentioned(jump.path("to"), used); }
            for (JsonNode axis : quantity.path("along")) own.add(axis.path("name").asText());
        });
        mentioned(model.path("duration"), used);
        catalogue.path("constants").fields().forEachRemaining(constant -> {
            if (!used.contains(constant.getKey()) || own.contains(constant.getKey())) return;
            String canonical = equations.canonical(constant.getValue().path("unit").asText());
            require(canonical != null, "Hằng số " + constant.getKey() + " của danh mục định luật có đơn vị không có trong danh mục đơn vị");
            names.add(constant.getKey());
            inputs.addObject().put("key", constant.getKey()).put("unit", canonical);
            bindings.put(constant.getKey(), equations.convert(constant.getValue().path("value").asDouble(), constant.getValue().path("unit").asText(), canonical));
            fixed.put(constant.getKey(), bindings.path(constant.getKey()).asDouble());
        });
        // a quantity over space is worked out at every point of its axes; to the solver an axis is one more given value
        for (JsonNode field : fields.values()) {
            require(field.has("value") && field.path("along").isArray() && !field.path("along").isEmpty() && field.path("along").size() <= 2,
                    "Đại lượng theo không gian " + field.path("id").asText() + " cần value và một hoặc hai trục trong along");
            int cells = 1;
            for (JsonNode axis : field.path("along")) {
                String name = axis.path("name").asText(), unit = axis.path("unit").asText();
                require(equations.canonical(unit) != null && isBase(unit), "Trục " + name + " phải dùng đơn vị SI có trong danh mục");
                int points = axis.path("points").asInt();
                require(points >= 2 && points <= 400, "Trục " + name + " cần từ 2 đến 400 điểm");
                cells *= points;
                for (String end : List.of("from", "to")) require(axis.path(end).isNumber() || fixed.containsKey(axis.path(end).asText()), "Trục " + name + " cần " + end + " là một số hoặc tên thanh trượt, hằng số");
                if (axes.containsKey(name)) { require(axes.get(name).equals(axis), "Trục " + name + " được khai báo khác nhau ở hai đại lượng"); continue; }
                axes.put(name(name, names), axis);
                inputs.addObject().put("key", name).put("unit", unit);
                bindings.put(name, axis.path("from").isNumber() ? axis.path("from").asDouble() : fixed.get(axis.path("from").asText()));
            }
            require(cells <= MAX_FIELD_CELLS, "Lưới của " + field.path("id").asText() + " có quá nhiều điểm (" + cells + ", tối đa " + MAX_FIELD_CELLS + ")");
        }
        for (JsonNode quantity : model.path("quantities")) {
            String id = name(quantity.path("id").asText(), names), unit = quantity.path("unit").asText();
            require(equations.canonical(unit) != null && isBase(unit), "Đại lượng " + id + " phải dùng đơn vị SI có trong danh mục, không phải " + unit);
            outputs.addObject().put("key", id).put("unit", unit);
            boolean state = quantity.has("rate");
            require(state == quantity.has("initial") && state != quantity.has("value"), "Đại lượng " + id
                    + " phải có hoặc cả initial và rate (đại lượng biến đổi theo phương trình vi phân), hoặc chỉ value (tính trực tiếp)");
            if (state) { initial.set(id, quantity.path("initial")); rates.set(id, quantity.path("rate")); results.put(id, id); }
            else results.set(id, quantity.path("value"));
            require(!quantity.has("jumps") || state, "Chỉ đại lượng có initial và rate mới có bước nhảy (jumps): " + id);
            for (JsonNode jump : quantity.path("jumps")) {
                require(jump.has("when") && jump.has("to"), "Mỗi bước nhảy của " + id + " cần when và to");
                jumps.withArray(id).addArray().add(json.createArrayNode().add("if").add(jump.path("when")).add(1).add(0)).add(jump.path("to"));
            }
            if (!quantity.has("along")) {
                Set<String> uses = new LinkedHashSet<>();
                for (String part : List.of("value", "initial", "rate")) mentioned(quantity.path(part), uses);
                for (String use : uses) require(!fields.containsKey(use) && !axes.containsKey(use), "Đại lượng " + id + " dùng " + use
                        + ", vốn chỉ có nghĩa tại từng điểm trong không gian; chỉ đại lượng có along mới dùng được toạ độ và đại lượng theo không gian");
            }
            // "by" may name something outside the model (the room, the mains); what a quantity belongs to must be an object of it
            require(!quantity.hasNonNull("of") || objects.containsKey(quantity.path("of").asText()), "Đại lượng " + id + " có of = \""
                    + quantity.path("of").asText() + "\" không phải id của đối tượng nào trong objects (" + String.join(", ", objects.keySet()) + ")");
            String role = quantity.path("role").asText();
            require(!role.isBlank() && catalogue.path("roles").has(role), "Đại lượng " + id + " có vai trò không thuộc danh mục: " + role);
            if (role.equals("force")) require(catalogue.path("forceKinds").has(quantity.path("kind").asText()), "Lực " + id + " cần kind thuộc danh mục lực");
        }
        if (!jumps.isEmpty()) math.set("jumps", jumps);
        if (initial.isEmpty()) {
            // a model given entirely by direct formulas still runs on the solver's clock
            String clock = name("elapsed", names);
            outputs.addObject().put("key", clock).put("unit", "s");
            initial.put(clock, 0); rates.put(clock, 1); results.put(clock, clock);
        }
        ObjectNode definition = json.createObjectNode();
        definition.putArray("capabilities").add(capability);
        ObjectNode solverSpec = json.createObjectNode();
        solverSpec.set("parameters", spec.path("parameters"));
        solverSpec.set("durationSeconds", spec.path("durationSeconds"));
        if (spec.has("durationParameter")) solverSpec.set("durationParameter", spec.path("durationParameter"));
        solverSpec.put("physicsCoverage", "COMPLETE");
        ObjectNode participant = solverSpec.putArray("physicsModels").addObject().put("id", ID).put("capabilityId", ID);
        participant.set("inputs", bindings);
        // Every way the run or one object can end is a condition that, once met, stays met.
        List<JsonNode> ends = new ArrayList<>();
        Map<String, List<JsonNode>> stops = new LinkedHashMap<>();
        JsonNode untils = model.path("until");
        for (JsonNode until : untils.isArray() ? untils : untils.isObject() ? json.createArrayNode().add(untils) : json.createArrayNode()) {
            String watched = until.path("quantity").asText(), direction = until.path("direction").asText();
            require(names.contains(watched), "Điều kiện dừng dùng đại lượng không có trong mô hình: " + watched);
            require(direction.equals("rising") || direction.equals("falling"), "Điều kiện dừng của " + watched + " cần direction là rising hoặc falling");
            JsonNode value = until.path("value");
            require(value.isNumber() || value.isTextual() && names.contains(value.asText()), "Điều kiện dừng của " + watched + " cần value là một số hoặc tên thanh trượt, hằng số");
            if (value.isNumber() && value.asDouble() != 0) {
                // a bare number here is a value of the watched quantity, in its unit
                String target = name("target_" + ends.size() + "_" + stops.values().stream().mapToInt(List::size).sum() + "_" + watched, names);
                String unit = "1";
                for (JsonNode quantity : model.path("quantities")) if (quantity.path("id").asText().equals(watched)) unit = quantity.path("unit").asText();
                inputs.addObject().put("key", target).put("unit", unit);
                bindings.set(target, value);
                fixed.put(target, value.asDouble());
                value = json.getNodeFactory().textNode(target);
            }
            ArrayNode reached = json.createArrayNode().add("latched");
            reached.add(direction.equals("rising") ? json.createArrayNode().add("sub").add(watched).add(value) : json.createArrayNode().add("sub").add(value).add(watched));
            require(!until.hasNonNull("of") || objects.containsKey(until.path("of").asText()), "Điều kiện dừng có of không phải id của đối tượng nào: " + until.path("of").asText());
            if (until.hasNonNull("of")) stops.computeIfAbsent(until.path("of").asText(), of -> new ArrayList<>()).add(reached); else ends.add(reached);
        }
        if (model.has("duration")) ends.add(json.createArrayNode().add("latched").add(json.createArrayNode().add("sub").add("t").add(model.path("duration"))));
        Set<String> moving = new LinkedHashSet<>();
        for (JsonNode quantity : model.path("quantities")) if (quantity.has("rate")) moving.add(quantity.path("of").asText(""));
        for (var stop : stops.entrySet()) {
            // an object that has reached its end keeps the values it had there
            String flag = name("stopped_" + safe(stop.getKey()), names);
            JsonNode any = stop.getValue().get(0);
            for (JsonNode more : stop.getValue().subList(1, stop.getValue().size())) any = json.createArrayNode().add("max").add(any).add(more);
            outputs.addObject().put("key", flag).put("unit", "1");
            results.set(flag, any);
            for (JsonNode quantity : model.path("quantities"))
                if (quantity.has("rate") && quantity.path("of").asText("").equals(stop.getKey()))
                    rates.set(quantity.path("id").asText(), json.createArrayNode().add("if").add(flag).add(0).add(quantity.path("rate")));
        }
        if (!stops.isEmpty() && stops.keySet().containsAll(moving)) {
            // the run is over when every moving object has reached its end
            JsonNode all = null;
            for (String object : stops.keySet()) all = all == null ? json.getNodeFactory().textNode("stopped_" + safe(object)) : json.createArrayNode().add("min").add(all).add("stopped_" + safe(object));
            ends.add(all);
        }
        if (!ends.isEmpty()) {
            String over = name("run_over", names);
            JsonNode any = ends.get(0);
            for (JsonNode more : ends.subList(1, ends.size())) any = json.createArrayNode().add("max").add(any).add(more);
            outputs.addObject().put("key", over).put("unit", "1");
            results.set(over, any);
            participant.putObject("until").put("field", ID + "." + over).put("direction", "rising").put("value", 0.5);
        }
        ObjectNode computed = equations.compute(definition, solverSpec, overrides == null ? json.createObjectNode() : overrides);

        JsonNode frames = computed.path("solverTimeline").path("frames");
        double[] time = new double[frames.size()];
        for (int i = 0; i < time.length; i++) time[i] = frames.get(i).path("t").asDouble();
        List<LawChecks.Symbol> symbols = new ArrayList<>();
        for (JsonNode quantity : model.path("quantities")) {
            if (quantity.has("along") || quantity.has("bookkeeping") || quantity.has("unsampled")) continue;
            double[] values = new double[time.length];
            for (int i = 0; i < time.length; i++) values[i] = frames.get(i).path("values").path(ID + "." + quantity.path("id").asText()).asDouble();
            symbols.add(new LawChecks.Symbol(quantity.path("id").asText(), quantity.path("label").asText(), equations.canonical(quantity.path("unit").asText()), attributes(quantity), values));
        }
        for (JsonNode parameter : spec.path("parameters")) if (parameter.hasNonNull("role")) {
            String name = parameter.path("name").asText(), unit = parameter.path("unit").asText();
            double value = overrides != null && overrides.has(name) ? overrides.path(name).asDouble() : parameter.path("value").asDouble();
            for (JsonNode owner : owners(parameter, objects)) symbols.add(constant(name, owner, equations.canonical(unit), equations.convert(value, unit, equations.canonical(unit)), time.length));
        }
        for (JsonNode constant : model.path("constants")) if (constant.hasNonNull("role"))
            for (JsonNode owner : owners(constant, objects)) symbols.add(constant(constant.path("id").asText(), owner, equations.canonical(constant.path("unit").asText()), equations.convert(constant.path("value").asDouble(),
                    constant.path("unit").asText(), equations.canonical(constant.path("unit").asText())), time.length));
        // laws hold for an object while it is there and under way
        Map<String, boolean[]> active = new LinkedHashMap<>();
        for (String object : objects.keySet()) {
            boolean stops_ = stops.containsKey(object), comes = names.contains("present_" + safe(object));
            if (!stops_ && !comes) continue;
            boolean[] running = new boolean[time.length];
            for (int i = 0; i < time.length; i++) running[i] = !(stops_ && frames.get(i).path("values").path(ID + ".stopped_" + safe(object)).asDouble() > 0.5)
                    && !(comes && !(frames.get(i).path("values").path(ID + ".present_" + safe(object)).asDouble() > 0.5));
            active.put(object, running);
        }
        if (!fields.isEmpty()) spaces(computed.path("solverTimeline"), model, fields, axes, fixed);
        // what depends on a random draw, directly or through other quantities
        Set<String> chancy = new LinkedHashSet<>();
        for (boolean grown = true; grown; ) {
            grown = false;
            for (JsonNode quantity : model.path("quantities")) {
                if (chancy.contains(quantity.path("id").asText())) continue;
                Set<String> uses = new LinkedHashSet<>();
                for (String part : List.of("value", "initial", "rate")) mentioned(quantity.path(part), uses);
                for (JsonNode jump : quantity.path("jumps")) { mentioned(jump.path("when"), uses); mentioned(jump.path("to"), uses); }
                for (String use : uses) if (use.startsWith("rnd_") || chancy.contains(use)) { grown = chancy.add(quantity.path("id").asText()); break; }
            }
        }
        Map<Double, Set<String>> impacts = new LinkedHashMap<>();
        for (JsonNode jump : computed.path("solverTimeline").path("jumps")) for (JsonNode name : jump.path("quantities")) for (JsonNode quantity : model.path("quantities"))
            if (quantity.path("id").asText().equals(name.asText()) && quantity.hasNonNull("of")) impacts.computeIfAbsent(jump.path("t").asDouble(), at -> new LinkedHashSet<>()).add(quantity.path("of").asText());
        return new Run(model, computed, symbols, time, active, stops.keySet(), chancy, impacts);
    }

    /** A run of very many values is reported at fewer instants; the checks have already seen every one. */
    private void thin(JsonNode timeline, int quantities) {
        ArrayNode frames = (ArrayNode) timeline.path("frames");
        int keep = Math.max(120, MAX_REPORTED_VALUES / Math.max(1, quantities));
        if (frames.size() <= keep) return;
        ArrayNode fewer = json.createArrayNode();
        int last = -1;
        for (int k = 0; k < keep; k++) { int index = (int) Math.round(k * (frames.size() - 1) / (double) (keep - 1)); if (index != last) fewer.add(frames.get(index)); last = index; }
        ((ObjectNode) timeline).set("frames", fewer);
    }

    /** The objects a slider or constant belongs to: its own, or the members of its group that the laws look at. */
    private List<JsonNode> owners(JsonNode source, Map<String, String> objects) {
        String of = source.path("of").asText("");
        if (of.isEmpty() || objects.containsKey(of) || !objects.containsKey(of + "[0]")) return List.of(source);
        List<JsonNode> all = new ArrayList<>();
        for (int i = 0; i < SAMPLED_MEMBERS && objects.containsKey(of + "[" + i + "]"); i++) all.add(((ObjectNode) source.deepCopy()).put("of", of + "[" + i + "]"));
        return all;
    }

    /** Every quantity over space, worked out at each point of its axes at an even selection of instants. */
    private void spaces(JsonNode timeline, JsonNode model, Map<String, JsonNode> fields, Map<String, JsonNode> axes, Map<String, Double> fixed) {
        JsonNode frames = timeline.path("frames");
        ObjectNode all = ((ObjectNode) timeline).putObject("fields");
        for (JsonNode field : fields.values()) {
            if (field.has("source")) continue;
            List<String> order = new ArrayList<>();
            List<double[]> grid = new ArrayList<>();
            ObjectNode report = all.putObject(key(field)).put("label", field.path("label").asText()).put("unit", field.path("unit").asText());
            int cells = 1;
            for (JsonNode axis : field.path("along")) {
                double from = axis.path("from").isNumber() ? axis.path("from").asDouble() : fixed.get(axis.path("from").asText());
                double to = axis.path("to").isNumber() ? axis.path("to").asDouble() : fixed.get(axis.path("to").asText());
                double[] points = new double[axis.path("points").asInt()];
                ArrayNode values = report.withArray("axes").addObject().put("name", axis.path("name").asText()).put("unit", axis.path("unit").asText()).putArray("values");
                for (int i = 0; i < points.length; i++) { points[i] = from + (to - from) * i / (points.length - 1); values.add(points[i]); }
                order.add(axis.path("name").asText());
                grid.add(points);
                cells *= points.length;
            }
            int instants = Math.max(1, Math.min(Math.min(frames.size(), 60), MAX_FIELD_VALUES / cells));
            ArrayNode times = report.putArray("times"), samples = report.putArray("frames");
            double least = Double.POSITIVE_INFINITY, most = Double.NEGATIVE_INFINITY;
            ArrayNode before = null;
            boolean still = true;
            for (int k = 0; k < instants; k++) {
                JsonNode frame = frames.get(instants == 1 ? 0 : (int) Math.round(k * (frames.size() - 1) / (double) (instants - 1)));
                Map<String, Double> point = new LinkedHashMap<>();
                java.util.function.ToDoubleFunction<String>[] lookup = new java.util.function.ToDoubleFunction[1];
                lookup[0] = name -> {
                    if (point.containsKey(name)) return point.get(name);
                    if (name.equals("t")) return frame.path("t").asDouble();
                    if (fields.containsKey(name)) return equations.value(fields.get(name).path("value"), lookup[0]);
                    JsonNode value = frame.path("values").path(ID + "." + name);
                    if (value.isNumber()) return value.asDouble();
                    require(fixed.containsKey(name), "Đại lượng trong phương trình không xác định: " + name);
                    return fixed.get(name);
                };
                ArrayNode sample = json.createArrayNode();
                for (int cell = 0; cell < cells; cell++) {
                    int rest = cell;
                    for (int axis = 0; axis < order.size(); axis++) { point.put(order.get(axis), grid.get(axis)[rest % grid.get(axis).length]); rest /= grid.get(axis).length; }
                    double value;
                    try { value = equations.value(field.path("value"), lookup[0]); } catch (ApiException undefined) { value = Double.NaN; }
                    if (Double.isFinite(value)) { sample.add(Double.parseDouble(String.format(java.util.Locale.ROOT, "%.5g", value))); least = Math.min(least, value); most = Math.max(most, value); }
                    else sample.addNull();
                }
                still &= before == null || before.equals(sample);
                before = sample;
                times.add(frame.path("t").asDouble());
                samples.add(sample);
            }
            require(Double.isFinite(least), "Đại lượng theo không gian " + field.path("id").asText() + " không xác định tại mọi điểm của lưới");
            report.put("min", least).put("max", most).put("unchanging", still);
            if (still) { while (samples.size() > 1) samples.remove(1); while (times.size() > 1) times.remove(1); }
        }
    }

    private ObjectNode verdict(JsonNode catalogue, Run run, Map<String, double[]> noise, Map<String, double[]> means) {
        JsonNode model = run.model;
        ObjectNode computed = run.computed;
        double[] time = run.time;
        JsonNode breakTimes = computed.path("solverTimeline").path("breaks");
        double[] breaks = new double[breakTimes.size()];
        for (int i = 0; i < breaks.length; i++) breaks[i] = breakTimes.get(i).asDouble();
        Map<String, LawChecks.Symbol> byId = new LinkedHashMap<>();
        for (LawChecks.Symbol symbol : run.symbols) byId.putIfAbsent(symbol.id(), symbol);
        ArrayNode checks = new LawChecks(json).impacts(run.impacts).check(catalogue, run.symbols, time, breaks, run.active);
        // a count of random events follows its law only on average: it is held to the quantity it is expected to follow
        for (JsonNode quantity : model.path("quantities")) {
            if (!quantity.hasNonNull("expected")) continue;
            String id = quantity.path("id").asText(), other = quantity.path("expected").asText();
            require(byId.containsKey(id) && byId.containsKey(other) && !run.chancy.contains(other) && byId.get(id).unit().equals(byId.get(other).unit()),
                    "expected của " + id + " phải là id của một đại lượng cùng đơn vị, không phụ thuộc số ngẫu nhiên, không thuộc từng phần tử của nhóm: " + other);
            double[] drawn = means.getOrDefault(id, byId.get(id).values()), mean = byId.get(other).values(), chance = noise.getOrDefault(id, new double[time.length]);
            double scale = 0, worstBy = 0;
            int worst = -1;
            for (int i = 0; i < time.length; i++) scale = Math.max(scale, Math.max(Math.abs(drawn[i]), Math.abs(mean[i])));
            for (int i = 0; i < time.length; i++) {
                double by = Math.abs(drawn[i] - mean[i]) - chance[i] - 0.02 * scale;
                if (by > worstBy) { worstBy = by; worst = i; }
            }
            ObjectNode check = checks.addObject().put("law", "expected_value").put("title", "Giá trị ngẫu nhiên thăng giáng quanh giá trị kỳ vọng").put("curriculum", "")
                    .put("status", worst < 0 ? "PASSED" : "FAILED").put("relativeDeviation", scale == 0 ? 0 : worstBy / scale);
            check.putArray("quantities").add(id).add(other);
            // what the drawn quantity is built from is checked with it
            Set<String> built = new LinkedHashSet<>(List.of(id));
            for (boolean grown = true; grown; ) {
                grown = false;
                for (JsonNode part : model.path("quantities")) if (built.contains(part.path("id").asText())) {
                    Set<String> uses = new LinkedHashSet<>();
                    for (String side : List.of("value", "initial", "rate")) mentioned(part.path(side), uses);
                    for (String use : uses) if (run.chancy.contains(use) && built.add(use)) { grown = true; check.withArray("quantities").add(use); }
                }
            }
            if (worst >= 0) check.put("at", time[worst]).put("left", drawn[worst]).put("right", mean[worst]).put("message", quantity.path("label").asText(id) + " (" + id
                    + ") lệch khỏi giá trị kỳ vọng " + other + " nhiều hơn thăng giáng thống kê cho phép: tại t = " + time[worst] + " s trung bình của " + (EXTRA_DRAWS + 1) + " lần gieo số là " + drawn[worst] + " trong khi kỳ vọng là "
                    + mean[worst] + " (thăng giáng cho phép ±" + chance[worst] + "). Đổi seed không sửa được điều này: hãy xem lại công thức gieo ngẫu nhiên hoặc công thức kỳ vọng.");
        }
        ObjectNode validation = (ObjectNode) computed.path("validation");
        boolean solved = validation.path("status").asText().startsWith("VERIFIED");
        ArrayNode kept = json.createArrayNode();
        if (!solved) kept.add("Nghiệm số chưa ổn định khi chia nhỏ bước tính (sai lệch tương đối lớn nhất "
                + validation.path("relativeError").asDouble() + "). Thường do một đại lượng nhảy bậc mà không viết bằng if hoặc sign, "
                + "hoặc do một lực rất cứng so với thời lượng mô phỏng; hãy viết bước nhảy bằng if, hoặc làm mềm, hoặc rút ngắn thời lượng.");
        validation.set("flags", kept);
        validation.set("lawChecks", checks);
        model.path("assumptions").forEach(((ArrayNode) validation.path("assumptions"))::add);
        Set<String> checked = new LinkedHashSet<>();
        int failed = 0;
        for (JsonNode check : checks) {
            check.path("quantities").forEach(id -> checked.add(id.asText()));
            if (check.path("status").asText().equals("AMBIGUOUS")) { validation.withArray("ambiguousLaws").add(check); continue; }
            if (check.path("status").asText().equals("FAILED")) { failed++; ((ArrayNode) validation.path("flags")).add(check.path("message").asText()); }
        }
        ArrayNode unchecked = validation.putArray("uncheckedQuantities");
        // a quantity of a group is checked when the members the laws looked at are
        Set<String> left = new LinkedHashSet<>(), done = new LinkedHashSet<>();
        for (JsonNode quantity : model.path("quantities")) {
            if (quantity.has("bookkeeping") || quantity.has("unsampled")) continue;
            (checked.contains(quantity.path("id").asText()) ? done : left).add(quantity.path("source").asText(quantity.path("id").asText()));
        }
        left.removeAll(done);
        left.forEach(unchecked::add);
        if (!run.chancy.isEmpty()) validation.put("chance", "Mô hình có yếu tố ngẫu nhiên: đại lượng phụ thuộc số ngẫu nhiên được so với giá trị kỳ vọng trong phạm vi thăng giáng thống kê, đo bằng cách chạy lại với các lần gieo số khác");
        validation.put("numericalStatus", solved ? "STEP_REFINEMENT_PASSED" : "NOT_CONVERGED");
        validation.put("formulaSource", "MODEL_CHECKED_BY_CURRICULUM_LAWS");
        validation.put("solverMethod", "RK4");
        validation.put("verificationMethod", "CURRICULUM_LAWS");
        int passed = 0;
        for (JsonNode check : checks) if (check.path("status").asText().equals("PASSED")) passed++;
        validation.put("status", failed > 0 || !solved ? FLAGGED : passed == 0 ? ILLUSTRATIVE : unchecked.isEmpty() ? VERIFIED : PARTLY_VERIFIED);
        validation.put("verificationScope", failed > 0 || !solved ? "Mô hình vi phạm định luật hoặc chưa tính ổn định; xem các ghi chú"
                : passed == 0 ? "Không có định luật nào của chương trình áp dụng cho các đại lượng này; hình chỉ mang tính minh hoạ"
                : unchecked.isEmpty() ? "Mọi đại lượng của mô hình đều được đối chiếu với định luật của chương trình"
                : "Các định luật áp dụng được đều thoả; " + unchecked.size() + " đại lượng chưa có định luật nào đối chiếu");
        thin(computed.path("solverTimeline"), model.path("quantities").size());
        relabel(computed.path("solverTimeline"), model, run.stopping);
        return computed;
    }

    public static final String VERIFIED = "VERIFIED_BY_LAWS", PARTLY_VERIFIED = "VERIFIED_PARTLY", ILLUSTRATIVE = "ILLUSTRATIVE", FLAGGED = "FLAGGED";

    /** The key under which a quantity is reported: the object it belongs to, a dot, its id ("box.v"); "model.g" when it belongs to none. */
    public static String key(JsonNode quantity) {
        if (quantity.hasNonNull("key")) return quantity.path("key").asText();
        return (quantity.hasNonNull("of") ? quantity.path("of").asText() : ID) + "." + quantity.path("id").asText();
    }

    /** Reports the run by object, drops the solver's own bookkeeping, and says when each object with an end of its own ended. */
    private void relabel(JsonNode timeline, JsonNode model, Set<String> stopping) {
        Map<String, String> keys = new LinkedHashMap<>();
        for (JsonNode quantity : model.path("quantities")) if (!quantity.has("along")) keys.put(ID + "." + quantity.path("id").asText(), key(quantity));
        Map<String, Double> ended = new LinkedHashMap<>();
        for (JsonNode frame : timeline.path("frames")) {
            ObjectNode values = json.createObjectNode();
            for (String object : stopping)
                if (!ended.containsKey(object) && frame.path("values").path(ID + ".stopped_" + safe(object)).asDouble() > 0.5) ended.put(object, frame.path("t").asDouble());
            frame.path("values").fields().forEachRemaining(value -> { if (keys.containsKey(value.getKey())) values.set(keys.get(value.getKey()), value.getValue()); });
            ((ObjectNode) frame).set("values", values);
        }
        ObjectNode phases = ((ObjectNode) timeline).putObject("phases");
        for (String object : stopping) {
            ObjectNode phase = phases.putObject(object).put("start", 0);
            if (ended.containsKey(object)) phase.put("end", ended.get(object)); else phase.putNull("end");
        }
        if (stopping.isEmpty()) ((ObjectNode) timeline).remove("phases");
    }

    /** What each reported value is, for the page and the drawing model: its object, name, unit and role; fixed values too. */
    public ObjectNode fieldMeta(JsonNode spec) {
        ObjectNode meta = json.createObjectNode();
        Map<String, String> objects = new LinkedHashMap<>();
        spec.path("objects").forEach(object -> objects.put(object.path("id").asText(), object.path("label").asText(object.path("id").asText())));
        Map<String, Integer> groups = new LinkedHashMap<>();
        for (JsonNode object : spec.path("objects")) {
            String id = object.path("id").asText();
            if (object.path("members").asInt(1) > 1) groups.put(id, object.path("members").asInt());
            if (object.has("present")) meta.putObject(id + (groups.containsKey(id) ? "[i]" : "") + ".present").put("participantId", id).put("participantLabel", objects.get(id))
                    .put("quantity", "present").put("unit", "1").put("label", "1 khi đối tượng đang có mặt, 0 khi chưa xuất hiện hoặc đã biến mất");
        }
        for (JsonNode quantity : spec.path("model").path("quantities")) {
            Integer members = groups.get(quantity.path("of").asText(""));
            if (members != null && quantity.has("along")) continue;
            ObjectNode field = meta.putObject(members == null ? key(quantity) : quantity.path("of").asText() + "[i]." + quantity.path("id").asText());
            describe(field, quantity, objects);
            if (members != null) field.put("members", members).put("note", "một giá trị cho mỗi phần tử: thay i bằng 0 … " + (members - 1));
            if (quantity.has("along")) { field.put("space", true).put("note", "đại lượng theo không gian: đọc ở api.timeline.fields, không có trong frame.fields"); field.set("along", quantity.path("along")); }
        }
        for (JsonNode constant : spec.path("model").path("constants")) {
            ObjectNode field = meta.putObject(key(constant));
            describe(field, constant, objects);
            field.set("constant", constant.path("value"));
        }
        return meta;
    }

    private static void describe(ObjectNode field, JsonNode source, Map<String, String> objects) {
        String object = source.hasNonNull("of") ? source.path("of").asText() : ID;
        field.put("participantId", object).put("participantLabel", objects.getOrDefault(object, ""));
        field.put("quantity", source.path("id").asText()).put("unit", source.path("unit").asText()).put("label", source.path("label").asText(source.path("id").asText()));
        for (String attribute : List.of("role", "axis", "kind", "by")) if (source.hasNonNull(attribute)) field.put(attribute.equals("role") ? "rendererRole" : attribute, source.path(attribute).asText());
    }

    /**
     * Puts right what is only a matter of form in a model as written — a unit spelled another way, a role or a kind
     * of force the catalogue does not have, an object named but not listed, a slider whose value lies outside its
     * own range — so that such slips never refuse a model. Nothing here touches an equation or a number of the physics.
     */
    public void tidy(ObjectNode spec, JsonNode catalogue) {
        // "duration": null says the same as no duration at all
        unset(spec);
        // the parts of the model written one level too high, or as a map by id instead of a list, are the same model
        boolean misplaced = false;
        for (String part : List.of("quantities", "constants", "until", "duration", "assumptions", "seed")) misplaced |= spec.has(part);
        if (!spec.path("model").isObject()) { if (!misplaced) return; spec.putObject("model"); }
        ObjectNode model = (ObjectNode) spec.path("model");
        for (String part : List.of("quantities", "constants", "until", "duration", "assumptions", "seed"))
            if (spec.has(part)) { JsonNode moved = spec.remove(part); if (!model.has(part) || model.path(part).isEmpty() && !moved.isEmpty()) model.set(part, moved); }
        listed(model, "quantities", "id");
        listed(model, "constants", "id");
        listed(spec, "parameters", "name");
        listed(spec, "objects", "id");
        // an equation may be written as an ordinary formula ("m*g*sin(a) - mu*N"); it is read into the same tree
        for (JsonNode quantity : model.path("quantities")) {
            for (String side : List.of("value", "initial", "rate")) if (quantity.has(side)) ((ObjectNode) quantity).set(side, formula(quantity.get(side)));
            for (JsonNode jump : quantity.path("jumps")) for (String side : List.of("when", "to")) if (jump.has(side)) ((ObjectNode) jump).set(side, formula(jump.get(side)));
        }
        if (model.has("duration")) model.set("duration", formula(model.get("duration")));
        for (JsonNode object : spec.path("objects")) if (object.has("present")) ((ObjectNode) object).set("present", formula(object.get("present")));
        // Inside an equation a slider already has its SI value (30 độ is 0.5236 rad, 10 cm is 0.1 m). A formula that
        // converts it once more by hand (theta*pi/180, d*0.01) would make it wrong by that factor without any
        // dimension to show it; the second conversion is taken out.
        Map<String, Double> factors = new LinkedHashMap<>(), plain = new LinkedHashMap<>();
        for (JsonNode slider : spec.path("parameters")) {
            String canonical = equations.canonical(slider.path("unit").asText());
            if (canonical == null) continue;
            double factor = equations.convert(1, slider.path("unit").asText(), canonical);
            if (Math.abs(factor - 1) > 1e-9 && equations.convert(0, slider.path("unit").asText(), canonical) == 0) factors.put(slider.path("name").asText(), factor);
        }
        for (JsonNode constant : model.path("constants")) if (constant.path("value").isNumber() && "1".equals(equations.canonical(constant.path("unit").asText("1"))) && !constant.hasNonNull("of"))
            plain.put(constant.path("id").asText(), constant.path("value").asDouble());
        if (!factors.isEmpty()) for (JsonNode quantity : model.path("quantities")) {
            for (String side : List.of("value", "initial", "rate")) if (quantity.has(side)) ((ObjectNode) quantity).set(side, once(quantity.get(side), factors, plain));
            for (JsonNode jump : quantity.path("jumps")) for (String side : List.of("when", "to")) if (jump.has(side)) ((ObjectNode) jump).set(side, once(jump.get(side), factors, plain));
        }
        apart(spec, model);
        if (!(spec.path("durationSeconds").asDouble(0) > 0)) spec.put("durationSeconds", 10);
        Set<String> objects = new LinkedHashSet<>();
        spec.withArray("objects").forEach(object -> objects.add(object.path("id").asText()));
        java.util.function.Consumer<JsonNode> owned = source -> {
            String of = source.path("of").asText("");
            if (of.matches("[A-Za-z_][A-Za-z0-9_]{0,31}") && !of.equals(ID) && objects.add(of)) spec.withArray("objects").addObject().put("id", of).put("label", of);
            else if (!of.isEmpty() && !objects.contains(of)) ((ObjectNode) source).remove("of");
        };
        for (JsonNode parameter : spec.withArray("parameters")) {
            ObjectNode slider = (ObjectNode) parameter;
            slider.put("unit", spelled(slider.path("unit").asText()));
            double value = slider.path("value").asDouble(), min = slider.path("min").asDouble(value), max = slider.path("max").asDouble(value);
            if (min > max) { double swap = min; min = max; max = swap; }
            slider.put("min", Math.min(min, value)).put("max", Math.max(max, value));
            if (!(slider.path("step").asDouble(0) > 0)) slider.put("step", max > min ? (max - min) / 100 : 1);
            if (!slider.hasNonNull("label")) slider.put("label", slider.path("name").asText());
            owned.accept(slider);
        }
        for (JsonNode constant : model.withArray("constants")) { ((ObjectNode) constant).put("unit", spelled(constant.path("unit").asText())); owned.accept(constant); }
        for (JsonNode item : model.withArray("quantities")) {
            ObjectNode quantity = (ObjectNode) item;
            // a quantity is computed in SI whatever unit was written beside it
            String unit = spelled(quantity.path("unit").asText()), canonical = equations.canonical(unit);
            // … so a formula that turns its SI result into the unit written beside it (angle*180/pi for "deg") would
            // be shown under the SI unit with the wrong number: that last conversion is taken out
            if (canonical != null && quantity.has("value") && equations.convert(0, unit, canonical) == 0) {
                double factor = equations.convert(1, unit, canonical);
                List<JsonNode> above = new ArrayList<>(), below = new ArrayList<>(), keptAbove = new ArrayList<>(), keptBelow = new ArrayList<>();
                chain(quantity.get("value"), false, above, below);
                double number = 1;
                boolean constant = false;
                for (JsonNode part : above) { Double value = part.isNumber() ? Double.valueOf(part.asDouble()) : part.isTextual() ? plain.get(part.asText()) : null; if (value != null) { number *= value; constant = true; } else keptAbove.add(part); }
                for (JsonNode part : below) { Double value = part.isNumber() ? Double.valueOf(part.asDouble()) : part.isTextual() ? plain.get(part.asText()) : null; if (value != null) { number /= value; constant = true; } else keptBelow.add(part); }
                if (Math.abs(factor - 1) > 1e-9 && constant && !keptAbove.isEmpty() && Math.abs(number * factor - 1) < 2e-3) {
                    JsonNode product = null;
                    for (JsonNode part : keptAbove) product = product == null ? part : json.createArrayNode().add("mul").add(product).add(part);
                    for (JsonNode part : keptBelow) product = json.createArrayNode().add("div").add(product).add(part);
                    quantity.set("value", product);
                }
            }
            quantity.put("unit", canonical == null ? unit : canonical);
            if (!catalogue.path("roles").has(quantity.path("role").asText())) quantity.put("role", "other");
            if (quantity.path("role").asText().equals("force") && !catalogue.path("forceKinds").has(quantity.path("kind").asText())) quantity.put("kind", "applied");
            if (!quantity.hasNonNull("label")) quantity.put("label", quantity.path("id").asText());
            owned.accept(quantity);
        }
        JsonNode untils = model.path("until");
        if (untils.isObject()) model.putArray("until").add(untils);
        for (JsonNode until : model.path("until")) if (until.hasNonNull("of") && !objects.contains(until.path("of").asText())) ((ObjectNode) until).remove("of");
    }

    /**
     * A few alike things written as one group (three pendulums, two carts) become that many objects of their own:
     * each gets its own copy of the group's sliders and quantities, so the learner can set and watch them one by
     * one. A crowd stays a group. Nothing of the physics changes: every copy has the equations the group had.
     */
    private void apart(ObjectNode spec, ObjectNode model) {
        if (model.toString().contains("\"random\"")) return;
        for (JsonNode group : List.copyOf(toList(spec.path("objects")))) {
            int size = group.path("members").asInt(group.path("count").asInt(1));
            String id = group.path("id").asText();
            if (size < 2 || size >= SMALL_GROUP || group.has("present") || !id.matches("[A-Za-z_][A-Za-z0-9_]{0,24}")) continue;
            // with "count" the author wrote one representative: only what belongs to it is copied, and only if something does
            Set<String> own = new LinkedHashSet<>();
            for (String part : List.of("parameters")) for (JsonNode item : spec.path(part)) if (item.path("of").asText("").equals(id)) own.add(item.path("name").asText());
            for (JsonNode item : model.path("constants")) if (item.path("of").asText("").equals(id)) own.add(item.path("id").asText());
            for (JsonNode item : model.path("quantities")) if (item.path("of").asText("").equals(id)) own.add(item.path("id").asText());
            if (own.isEmpty()) continue;
            boolean taken = false;
            Set<String> names = new LinkedHashSet<>();
            spec.path("parameters").forEach(item -> names.add(item.path("name").asText()));
            for (String part : List.of("constants", "quantities")) model.path(part).forEach(item -> names.add(item.path("id").asText()));
            for (String name : own) for (int i = 1; i <= size; i++) taken |= names.contains(name + "_" + i);
            if (taken) continue;
            class Copy {
                JsonNode of(JsonNode ast, int i) {
                    if (ast.isTextual()) {
                        String name = ast.asText();
                        if (name.equals("index")) return json.getNodeFactory().numberNode(i - 1);
                        if (name.equals("members")) return json.getNodeFactory().numberNode(size);
                        return own.contains(name) ? json.getNodeFactory().textNode(name + "_" + i) : ast;
                    }
                    if (!ast.isArray() || ast.isEmpty()) return ast;
                    String op = ast.get(0).asText();
                    if ((OVER_GROUP.contains(op) || op.equals("member")) && ast.size() >= 2 && own.contains(ast.get(1).asText())) {
                        String name = ast.get(1).asText();
                        if (op.equals("member")) return json.getNodeFactory().textNode(name + "_" + (ast.path(2).asInt() + 1));
                        JsonNode all = json.getNodeFactory().textNode(name + "_1");
                        for (int k = 2; k <= size; k++) all = json.createArrayNode().add(op.equals("largest") ? "max" : op.equals("smallest") ? "min" : "add").add(all).add(name + "_" + k);
                        return op.equals("mean") ? json.createArrayNode().add("div").add(all).add(size) : all;
                    }
                    ArrayNode copy = json.createArrayNode().add(op);
                    for (int k = 1; k < ast.size(); k++) copy.add(of(ast.get(k), i));
                    return copy;
                }
                void each(ArrayNode list, String key, boolean equations) {
                    ArrayNode out = json.createArrayNode();
                    for (JsonNode item : list) {
                        boolean mine = own.contains(item.path(key).asText());
                        for (int i = 1; i <= (mine ? size : 1); i++) {
                            ObjectNode one = (ObjectNode) item.deepCopy();
                            if (mine) {
                                one.put(key, item.path(key).asText() + "_" + i).put("of", id + "_" + i);
                                if (one.hasNonNull("label")) one.put("label", one.path("label").asText() + " " + i);
                            }
                            if (equations) {
                                for (String side : List.of("value", "initial", "rate")) if (one.has(side)) one.set(side, of(one.get(side), mine ? i : 1));
                                for (JsonNode jump : one.path("jumps")) for (String side : List.of("when", "to")) ((ObjectNode) jump).set(side, of(jump.path(side), mine ? i : 1));
                            }
                            out.add(one);
                        }
                    }
                    list.removeAll();
                    list.addAll(out);
                }
            }
            Copy copy = new Copy();
            // a quantity outside the group that reads one of its sliders by name reads the first copy; totals read them all
            copy.each(spec.withArray("parameters"), "name", false);
            copy.each(model.withArray("constants"), "id", false);
            copy.each(model.withArray("quantities"), "id", true);
            ArrayNode ends = json.createArrayNode();
            for (JsonNode until : model.path("until")) {
                boolean mine = own.contains(until.path("quantity").asText()) || until.path("of").asText("").equals(id);
                for (int i = 1; i <= (mine ? size : 1); i++) {
                    ObjectNode one = (ObjectNode) until.deepCopy();
                    if (mine) {
                        if (own.contains(until.path("quantity").asText())) one.put("quantity", until.path("quantity").asText() + "_" + i);
                        if (until.path("value").isTextual() && own.contains(until.path("value").asText())) one.put("value", until.path("value").asText() + "_" + i);
                        one.put("of", id + "_" + i);
                    }
                    ends.add(one);
                }
            }
            if (model.has("until")) model.set("until", ends);
            if (model.has("duration")) model.set("duration", copy.of(model.get("duration"), 1));
            if (own.contains(spec.path("durationParameter").asText(""))) spec.remove("durationParameter");
            ArrayNode objects = json.createArrayNode();
            for (JsonNode object : spec.path("objects")) {
                if (!object.path("id").asText().equals(id)) { objects.add(object); continue; }
                for (int i = 1; i <= size; i++) objects.addObject().put("id", id + "_" + i).put("label", object.path("label").asText(id) + " " + i);
            }
            spec.set("objects", objects);
        }
    }

    /** Takes out every field written as null, at any depth. */
    private static void unset(JsonNode node) {
        if (node.isObject()) {
            List<String> empty = new ArrayList<>();
            node.fields().forEachRemaining(field -> { if (field.getValue().isNull()) empty.add(field.getKey()); else unset(field.getValue()); });
            ((ObjectNode) node).remove(empty);
        } else if (node.isArray()) node.forEach(FreeModel::unset);
    }

    /** A product in which a slider is multiplied by exactly the number that turns its unit into SI: the product without that number. */
    private JsonNode once(JsonNode ast, Map<String, Double> factors, Map<String, Double> plain) {
        if (!ast.isArray() || ast.isEmpty()) return ast;
        String op = ast.get(0).asText();
        if ((op.equals("mul") || op.equals("div")) && ast.size() == 3) {
            List<JsonNode> above = new ArrayList<>(), below = new ArrayList<>();
            chain(ast, false, above, below);
            double number = 1;
            String slider = null;
            int sliders = 0;
            List<JsonNode> keptAbove = new ArrayList<>(), keptBelow = new ArrayList<>();
            for (JsonNode item : above) {
                Double value = item.isNumber() ? Double.valueOf(item.asDouble()) : item.isTextual() ? plain.get(item.asText()) : null;
                if (value != null) number *= value;
                else { if (item.isTextual() && factors.containsKey(item.asText())) { slider = item.asText(); sliders++; } keptAbove.add(item); }
            }
            for (JsonNode item : below) {
                Double value = item.isNumber() ? Double.valueOf(item.asDouble()) : item.isTextual() ? plain.get(item.asText()) : null;
                if (value != null) number /= value; else keptBelow.add(item);
            }
            if (sliders == 1 && Math.abs(number / factors.get(slider) - 1) < 2e-3) {
                JsonNode product = null;
                for (JsonNode item : keptAbove) { JsonNode part = once(item, factors, plain); product = product == null ? part : json.createArrayNode().add("mul").add(product).add(part); }
                for (JsonNode item : keptBelow) product = json.createArrayNode().add("div").add(product).add(once(item, factors, plain));
                return product;
            }
        }
        ArrayNode copy = json.createArrayNode().add(ast.get(0));
        for (int i = 1; i < ast.size(); i++) copy.add(once(ast.get(i), factors, plain));
        return copy;
    }

    private static void chain(JsonNode ast, boolean inverted, List<JsonNode> above, List<JsonNode> below) {
        if (ast.isArray() && ast.size() == 3 && (ast.get(0).asText().equals("mul") || ast.get(0).asText().equals("div"))) {
            chain(ast.get(1), inverted, above, below);
            chain(ast.get(2), ast.get(0).asText().equals("div") != inverted, above, below);
        } else (inverted ? below : above).add(ast);
    }

    private static final int SMALL_GROUP = 10;
    private static final Set<String> FUNCTIONS = Set.of("add", "sub", "mul", "div", "pow", "min", "max", "mod", "atan2", "neg", "abs", "sqrt", "sin", "cos", "tan",
            "exp", "log", "asin", "acos", "atan", "sign", "floor", "if", "random", "total", "mean", "largest", "smallest", "member");

    /** An equation given as text, or a tree with text inside it, as a tree; a name, a number or a tree stays what it is. */
    private JsonNode formula(JsonNode written) {
        if (written.isArray()) {
            ArrayNode copy = json.createArrayNode();
            for (int i = 0; i < written.size(); i++) copy.add(i == 0 ? written.get(i) : formula(written.get(i)));
            return copy;
        }
        if (!written.isTextual() || written.asText().matches("\\s*[A-Za-z_][A-Za-z0-9_]*\\s*")) return written.isTextual() ? json.getNodeFactory().textNode(written.asText().trim()) : written;
        String text = written.asText().replace("·", "*").replace("×", "*").replace("−", "-").replace("÷", "/").replace("²", "^2").replace("³", "^3").replace("π", "pi").replace("√", "sqrt");
        int[] at = {0};
        class Reader {
            void space() { while (at[0] < text.length() && Character.isWhitespace(text.charAt(at[0]))) at[0]++; }
            boolean take(String token) { space(); if (!text.startsWith(token, at[0])) return false; at[0] += token.length(); return true; }
            JsonNode two(String op, JsonNode a, JsonNode b) { return json.createArrayNode().add(op).add(a).add(b); }
            JsonNode compared() {
                JsonNode left = sum();
                // a condition holds when it is positive: a > b is a - b
                if (take(">=") || take(">")) return two("sub", left, sum());
                if (take("<=") || take("<")) return two("sub", sum(), left);
                return left;
            }
            JsonNode sum() {
                JsonNode left = product();
                while (true) {
                    if (take("+")) left = two("add", left, product());
                    else if (take("-")) left = two("sub", left, product());
                    else return left;
                }
            }
            JsonNode product() {
                JsonNode left = signed();
                while (true) {
                    if (take("*")) left = two("mul", left, signed());
                    else if (take("/")) left = two("div", left, signed());
                    else return left;
                }
            }
            JsonNode signed() {
                if (take("-")) { JsonNode inner = signed(); return inner.isNumber() ? json.getNodeFactory().numberNode(-inner.asDouble()) : json.createArrayNode().add("neg").add(inner); }
                if (take("+")) return signed();
                JsonNode base = primary();
                if (take("^") || take("**")) return two("pow", base, signed());
                return base;
            }
            JsonNode primary() {
                space();
                require(at[0] < text.length(), "Công thức kết thúc giữa chừng: " + text);
                char c = text.charAt(at[0]);
                if (c == '(') { at[0]++; JsonNode inner = compared(); require(take(")"), "Thiếu dấu ) trong công thức: " + text); return inner; }
                if (Character.isDigit(c) || c == '.') {
                    java.util.regex.Matcher number = java.util.regex.Pattern.compile("\\d*\\.?\\d+(?:[eE][-+]?\\d+)?|\\d+\\.").matcher(text).region(at[0], text.length());
                    require(number.lookingAt(), "Số không đọc được trong công thức: " + text);
                    at[0] = number.end();
                    double value = Double.parseDouble(number.group());
                    return value == Math.rint(value) && Math.abs(value) < 1e9 && !number.group().contains(".") && !number.group().toLowerCase().contains("e") ? json.getNodeFactory().numberNode((int) value) : json.getNodeFactory().numberNode(value);
                }
                require(Character.isLetter(c) || c == '_', "Ký hiệu không đọc được trong công thức \"" + text + "\" tại: " + text.substring(at[0]));
                int start = at[0];
                while (at[0] < text.length() && (Character.isLetterOrDigit(text.charAt(at[0])) || text.charAt(at[0]) == '_')) at[0]++;
                String name = text.substring(start, at[0]);
                if (!take("(")) return name.equals("pi") ? json.getNodeFactory().numberNode(Math.PI) : json.getNodeFactory().textNode(name);
                String op = name.equals("ln") ? "log" : name.equals("sqr") ? "pow" : name;
                require(FUNCTIONS.contains(op), "Hàm không có trong danh sách phép toán: " + name + " (trong công thức " + text + ")");
                ArrayNode call = json.createArrayNode().add(op);
                if (!take(")")) { do call.add(compared()); while (take(",")); require(take(")"), "Thiếu dấu ) trong công thức: " + text); }
                if (name.equals("sqr")) call.add(2);
                return call;
            }
        }
        Reader reader = new Reader();
        JsonNode tree = reader.compared();
        reader.space();
        require(at[0] == text.length(), "Công thức không đọc được hết: \"" + text + "\" (dừng tại \"" + text.substring(at[0]) + "\")");
        return tree;
    }

    private static List<JsonNode> toList(JsonNode array) { List<JsonNode> list = new ArrayList<>(); array.forEach(list::add); return list; }

    /** {"v": {…}, "x": {…}} written where a list was asked for becomes that list, each entry named by its key. */
    private void listed(ObjectNode owner, String part, String key) {
        if (!owner.path(part).isObject()) return;
        ArrayNode list = json.createArrayNode();
        owner.path(part).fields().forEachRemaining(entry -> {
            if (!entry.getValue().isObject()) return;
            ObjectNode one = (ObjectNode) entry.getValue();
            if (!one.hasNonNull(key)) one.put(key, entry.getKey());
            list.add(one);
        });
        owner.set(part, list);
    }

    /** A unit as the catalogue spells it, when it was written in another common way (m/s², kg·m/s, N.m, m s-1). */
    private String spelled(String unit) {
        if (unit.isBlank()) return "1";
        if (equations.canonical(unit) != null) return unit;
        String plain = unit.trim().replace("²", "^2").replace("³", "^3").replace("·", "*").replace("⋅", "*").replace("×", "*").replace(" ", "");
        for (String candidate : List.of(plain, plain.replace(".", "*"), plain.replaceAll("([A-Za-zΩµ])(-?\\d)", "$1^$2"), plain.replace(".", "*").replaceAll("([A-Za-zΩµ])(-?\\d)", "$1^$2"), plain.toLowerCase(java.util.Locale.ROOT)))
            if (equations.canonical(candidate) != null) return candidate;
        // "kg m s-2": factors side by side, negative powers below the line
        StringBuilder above = new StringBuilder(), below = new StringBuilder();
        for (String factor : unit.trim().replace("²", "^2").replace("³", "^3").split("[\\s.*·⋅×]+")) {
            java.util.regex.Matcher parts = java.util.regex.Pattern.compile("([A-Za-zΩµ°]+)\\^?(-?\\d+)?").matcher(factor);
            if (!parts.matches()) return unit;
            int power = parts.group(2) == null ? 1 : Integer.parseInt(parts.group(2));
            if (power == 0) continue;
            String written = parts.group(1) + (Math.abs(power) == 1 ? "" : "^" + Math.abs(power));
            if (power > 0) above.append(above.length() == 0 ? "" : "*").append(written); else below.append("/").append(written);
        }
        String rebuilt = (above.length() == 0 ? "1" : above.toString()) + below;
        return equations.canonical(rebuilt) != null ? rebuilt : unit;
    }

    /**
     * The plan as the page reads it, derived from the model and never authored: one entry per object with the
     * sliders that belong to it, the objects the learner sees, and each watched quantity under its reported key.
     */
    public void project(ObjectNode spec) {
        Map<String, String> objects = new LinkedHashMap<>();
        ArrayNode required = spec.putArray("requiredObjects"), participants = spec.putArray("physicsModels");
        Map<String, ObjectNode> inputs = new LinkedHashMap<>();
        for (JsonNode object : spec.path("objects")) {
            String id = object.path("id").asText();
            objects.put(id, object.path("label").asText(id));
            required.addObject().put("label", objects.get(id)).put("count", object.path("members").asInt(object.path("count").asInt(1)));
            inputs.put(id, participants.addObject().put("id", id).put("label", objects.get(id)).put("capabilityId", ID).putObject("inputs"));
        }
        for (JsonNode parameter : spec.path("parameters")) {
            ObjectNode own = inputs.get(parameter.path("of").asText(""));
            if (own != null) own.put(parameter.path("role").asText("other") + (parameter.hasNonNull("axis") ? "." + parameter.path("axis").asText() : "")
                    + (parameter.hasNonNull("kind") ? "." + parameter.path("kind").asText() : ""), parameter.path("name").asText());
        }
        Map<String, JsonNode> quantities = new LinkedHashMap<>();
        spec.path("model").path("quantities").forEach(quantity -> quantities.put(quantity.path("id").asText(), quantity));
        // What the learner watches is presentation, not physics: a name that is the common stem of several objects'
        // quantities ("theta" for theta1, theta2, theta3) means each of them, and a name that matches nothing is left out.
        ArrayNode watched = json.createArrayNode();
        for (JsonNode observable : spec.path("observables")) {
            String name = observable.path("quantity").asText();
            if (quantities.containsKey(name)) { watched.add(observable); continue; }
            for (String id : quantities.keySet()) if (!name.isBlank() && id.startsWith(name) && id.substring(name.length()).matches("_?[A-Za-z0-9]{1,3}")) {
                ObjectNode one = ((ObjectNode) observable.deepCopy()).put("quantity", id);
                one.remove("label");
                watched.add(one);
            }
        }
        if (watched.isEmpty()) for (JsonNode quantity : quantities.values()) { if (watched.size() < 4 && quantity.has("rate")) watched.addObject().put("quantity", quantity.path("id").asText()); }
        if (watched.isEmpty()) for (JsonNode quantity : quantities.values()) { if (watched.size() < 6 && !quantity.path("role").asText("other").equals("other")) watched.addObject().put("quantity", quantity.path("id").asText()); }
        spec.set("observables", watched);
        for (JsonNode observable : spec.path("observables")) {
            JsonNode quantity = quantities.get(observable.path("quantity").asText());
            boolean member = false;
            for (JsonNode object : spec.path("objects")) member |= object.path("members").asInt(1) > 1 && object.path("id").asText().equals(quantity.path("of").asText(""));
            // a quantity of a group is watched through its first member
            ((ObjectNode) observable).put("field", member ? quantity.path("of").asText() + "[0]." + quantity.path("id").asText() : key(quantity));
            if (quantity.hasNonNull("of")) ((ObjectNode) observable).put("object", objects.getOrDefault(quantity.path("of").asText(), ""));
            if (!observable.hasNonNull("label")) ((ObjectNode) observable).put("label", quantity.path("label").asText());
        }
    }

    /** The model's equations by object, written the way a reader checks them, with the values that enter them. */
    public ArrayNode formulas(JsonNode spec) {
        ArrayNode formulas = json.createArrayNode();
        Map<String, ObjectNode> byObject = new LinkedHashMap<>();
        java.util.function.BiFunction<String, String, ObjectNode> entry = (id, label) -> byObject.computeIfAbsent(id, key -> {
            ObjectNode formula = formulas.addObject().put("modelId", key).put("label", label).put("capabilityId", ID);
            formula.putArray("canonical"); formula.putArray("bindings");
            return formula;
        });
        for (JsonNode object : spec.path("objects")) entry.apply(object.path("id").asText(), object.path("label").asText(object.path("id").asText()));
        for (JsonNode quantity : spec.path("model").path("quantities")) {
            ObjectNode formula = entry.apply(quantity.hasNonNull("of") ? quantity.path("of").asText() : ID, "Chung");
            String id = quantity.path("id").asText();
            if (quantity.has("rate")) formula.withArray("canonical").add("d" + id + "/dt = " + text(quantity.path("rate"))).add(id + "(0) = " + text(quantity.path("initial")));
            else formula.withArray("canonical").add(id + " = " + text(quantity.path("value")));
            for (JsonNode jump : quantity.path("jumps")) formula.withArray("canonical").add("khi " + text(jump.path("when")) + " > 0: " + id + " ← " + text(jump.path("to")));
        }
        for (JsonNode parameter : spec.path("parameters"))
            entry.apply(parameter.hasNonNull("of") ? parameter.path("of").asText() : ID, "Chung").withArray("bindings").addObject()
                    .put("quantity", parameter.path("name").asText()).put("label", parameter.path("label").asText(parameter.path("name").asText()))
                    .put("unit", parameter.path("unit").asText()).put("source", "PARAMETER").put("parameter", parameter.path("name").asText())
                    .put("parameterLabel", parameter.path("label").asText(parameter.path("name").asText())).set("value", parameter.path("value"));
        for (JsonNode constant : spec.path("model").path("constants"))
            entry.apply(constant.hasNonNull("of") ? constant.path("of").asText() : ID, "Chung").withArray("bindings").addObject()
                    .put("quantity", constant.path("id").asText()).put("label", constant.path("label").asText(constant.path("id").asText()))
                    .put("unit", constant.path("unit").asText()).put("source", "FIXED").set("value", constant.path("value"));
        for (JsonNode assumption : spec.path("model").path("assumptions")) if (!formulas.isEmpty()) ((ObjectNode) formulas.get(0)).withArray("assumptions").add(assumption);
        for (int i = formulas.size() - 1; i >= 0; i--) if (formulas.get(i).path("canonical").isEmpty() && formulas.get(i).path("bindings").isEmpty()) formulas.remove(i);
        return formulas;
    }

    private static final Map<String, String> INFIX = Map.of("add", " + ", "sub", " - ", "mul", "*", "div", "/", "pow", "^");
    private static final Map<String, Integer> RANK = Map.of("add", 1, "sub", 1, "mul", 2, "div", 2, "neg", 3, "pow", 4);

    /** An equation tree in ordinary notation: a*(b + c), sqrt(x), if(c, a, b). */
    static String text(JsonNode ast) {
        if (ast.isNumber() || ast.isTextual()) return ast.asText();
        if (!ast.isArray() || ast.isEmpty()) return "?";
        String op = ast.get(0).asText();
        if (op.equals("neg") && ast.size() == 2) return "-" + within(ast.get(1), 3, false);
        if (INFIX.containsKey(op) && ast.size() == 3)
            return within(ast.get(1), RANK.get(op), false) + INFIX.get(op) + within(ast.get(2), RANK.get(op), !op.equals("add") && !op.equals("mul"));
        StringBuilder call = new StringBuilder(op).append("(");
        for (int i = 1; i < ast.size(); i++) call.append(i > 1 ? ", " : "").append(text(ast.get(i)));
        return call.append(")").toString();
    }

    private static String within(JsonNode ast, int rank, boolean strict) {
        int own = ast.isArray() && !ast.isEmpty() ? RANK.getOrDefault(ast.get(0).asText(), 5) : ast.isNumber() && ast.asDouble() < 0 ? 0 : 5;
        return own < rank || strict && own == rank ? "(" + text(ast) + ")" : text(ast);
    }

    private static void mentioned(JsonNode ast, Set<String> into) {
        if (ast.isTextual()) into.add(ast.asText());
        else if (ast.isArray()) for (int i = 1; i < ast.size(); i++) mentioned(ast.get(i), into);
    }

    private boolean isBase(String unit) { return equations.convert(1, unit, equations.canonical(unit)) == 1; }

    private static LawChecks.Symbol constant(String id, JsonNode source, String unit, double value, int count) {
        double[] values = new double[count];
        java.util.Arrays.fill(values, value);
        return new LawChecks.Symbol(id, source.path("label").asText(), unit, attributes(source), values);
    }

    private static Map<String, String> attributes(JsonNode source) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (String attribute : ATTRIBUTES) if (source.hasNonNull(attribute) && !source.path(attribute).asText().isBlank()) attributes.put(attribute, source.path(attribute).asText());
        // attributes a strand adds for its own laws (the branch of a circuit, the medium of a wave…)
        source.fields().forEachRemaining(field -> { if (field.getKey().startsWith("in") && field.getValue().isTextual() && !field.getValue().asText().isBlank()) attributes.putIfAbsent(field.getKey(), field.getValue().asText()); });
        attributes.putIfAbsent("role", "other");
        return attributes;
    }

    private static String name(String name, Set<String> names) {
        require(name.matches("[A-Za-z_][A-Za-z0-9_]{0,63}") && !name.equals("t"), "Tên không hợp lệ (chữ, số, gạch dưới; không dùng t): " + name);
        require(names.add(name), "Tên bị trùng giữa thanh trượt, hằng số và đại lượng: " + name);
        return name;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw ApiException.unprocessable(message);
    }
}
