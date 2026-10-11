package com.example.backend.system.physics.service;

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
 * Checks a computed run against the laws of the curriculum. A law is data (schemas/laws): it names the
 * roles it relates (mass, velocity, the forces on a body…) and the relation that must hold at every instant.
 * Which laws apply to a model follows from the roles its quantities declare, never from the kind of situation.
 */
public final class LawChecks {
    private static final String AMBIGUOUS = "\u0000ambiguous";
    private static final double RELATIVE = 0.02, FRACTION = 0;

    /** One quantity of the model over the run, with the role attributes laws match on. */
    public record Symbol(String id, String label, String unit, Map<String, String> attributes, double[] values) {
        String get(String attribute) { return attributes.get(attribute); }
    }

    private final ObjectMapper json;
    private final Map<String, Double> worth = new LinkedHashMap<>();
    /** Universal constants the catalogue names (G, k, R, h…), usable in any law. */
    private JsonNode constants = com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    /** Samples at which a new smooth piece of the run begins. */
    private boolean[] starts = new boolean[0];
    /** Per object that has an end of its own: the samples at which it is still under way. Laws hold while it is. */
    private Map<String, boolean[]> active = Map.of();

    /** Per quantity id: how far each sample can be off by chance, for a model that draws random numbers. */
    private Map<String, double[]> noise = Map.of();

    public LawChecks(ObjectMapper json) { this.json = json; }

    /** Instants at which quantities jumped, with the objects they belong to. */
    private Map<Double, Set<String>> impacts = Map.of();

    public LawChecks impacts(Map<Double, Set<String>> impacts) { this.impacts = impacts; return this; }

    public LawChecks noise(Map<String, double[]> noise) { this.noise = noise; return this; }

    private double[] noiseOf(Symbol symbol, int count) { double[] own = noise.get(symbol.id()); return own == null ? new double[count] : own; }

    /** The catalogues of several strands as one: their roles, kinds, attributes, constants and laws together. */
    public static ObjectNode merge(ObjectMapper json, Iterable<JsonNode> catalogues) {
        ObjectNode all = json.createObjectNode();
        ArrayNode laws = all.putArray("laws"), strands = all.putArray("strands");
        for (JsonNode catalogue : catalogues) {
            // a catalogue may itself be a merge (the catalogue of a topic): its strands and contents carry over
            for (JsonNode strand : catalogue.has("strands") ? catalogue.path("strands") : List.of((JsonNode) json.createObjectNode()
                    .put("id", catalogue.path("id").asText()).put("title", catalogue.path("title").asText()).put("source", catalogue.path("source").asText()))) {
                boolean known = false;
                for (JsonNode existing : strands) known |= existing.path("id").equals(strand.path("id"));
                if (!known) strands.add(strand);
            }
            for (JsonNode content : catalogue.path("contents")) {
                boolean known = false;
                for (JsonNode existing : all.withArray("contents")) known |= existing.path("name").equals(content.path("name"));
                if (!known) all.withArray("contents").add(content);
            }
            for (String part : List.of("roles", "kinds", "forceKinds", "constants"))
                catalogue.path(part).fields().forEachRemaining(entry -> { if (!all.withObject("/" + part).has(entry.getKey())) all.withObject("/" + part).set(entry.getKey(), entry.getValue()); });
            for (JsonNode attribute : catalogue.path("attributes")) {
                boolean known = false;
                for (JsonNode existing : all.withArray("attributes")) known |= existing.equals(attribute);
                if (!known) all.withArray("attributes").add(attribute);
            }
            if (catalogue.hasNonNull("axes") && !all.has("axes")) all.set("axes", catalogue.path("axes"));
            for (JsonNode law : catalogue.path("laws")) {
                boolean known = false;
                for (JsonNode existing : laws) known |= existing.path("id").equals(law.path("id"));
                if (!known) laws.add(law.has("strand") ? law : ((ObjectNode) law.deepCopy()).put("strand", catalogue.path("id").asText()));
            }
        }
        return all;
    }

    /** Every law of the catalogue against the run: PASSED, FAILED (with where and by how much) or not applicable (left out). */
    public ArrayNode check(JsonNode catalogue, List<Symbol> symbols, double[] time, double[] breaks, Map<String, boolean[]> active) {
        this.active = active;
        ArrayNode results = json.createArrayNode();
        constants = catalogue.path("constants");
        // what quantities of each unit are worth in this run: the yardstick for "differs by little"
        worth.clear();
        constants.fields().forEachRemaining(constant -> worth.put("constant " + constant.getKey(), Math.abs(constant.getValue().path("value").asDouble())));
        worth.put("constant t", time.length > 0 ? time[time.length - 1] : 0);
        starts = new boolean[time.length];
        for (double at : breaks) for (int i = 0; i < time.length; i++) if (time[i] == at) { starts[i] = true; break; }
        double span = time.length > 1 ? time[time.length - 1] - time[0] : 0;
        for (Symbol symbol : symbols) {
            double[] values = symbol.values();
            for (int i = 0; i < values.length; i++) {
                // a value is measured against the quantity itself; a rate against how fast quantities of that kind
                // of the same object change (the energies of one body exchange while their sum stays still)
                worth.merge("own " + symbol.id(), Math.abs(values[i]), Math::max);
                if (span > 0) worth.merge("d/dt " + symbol.unit() + "@" + symbol.get("of"), Math.abs(values[i]) / span, Math::max);
                if (i > 0 && time[i] > time[i - 1]) worth.merge("d/dt " + symbol.unit() + "@" + symbol.get("of"), Math.abs(values[i] - values[i - 1]) / (time[i] - time[i - 1]), Math::max);
            }
        }
        for (JsonNode law : catalogue.path("laws")) {
            List<String> scope = new ArrayList<>();
            law.path("scope").forEach(attribute -> scope.add(attribute.asText()));
            for (Map<String, String> where : scopes(scope, symbols)) {
                Map<String, List<Symbol>> bound = bind(law, scope, where, symbols);
                if (bound == null) continue;
                if (bound.containsKey(AMBIGUOUS)) {
                    ObjectNode note = results.addObject().put("law", law.path("id").asText()).put("title", law.path("title").asText()).put("status", "AMBIGUOUS");
                    where.forEach(note.putObject("scope")::put);
                    bound.get(AMBIGUOUS).forEach(symbol -> note.withArray("quantities").add(symbol.id()));
                    continue;
                }
                ObjectNode result = evaluate(law, where, bound, time);
                if (result != null) results.add(result);
            }
        }
        return results;
    }

    private static Set<Map<String, String>> scopes(List<String> scope, List<Symbol> symbols) {
        Set<Map<String, String>> all = new LinkedHashSet<>();
        if (scope.isEmpty()) { all.add(Map.of()); return all; }
        for (Symbol symbol : symbols) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String attribute : scope) if (symbol.get(attribute) != null) values.put(attribute, symbol.get(attribute));
            if (values.size() == scope.size()) all.add(values);
        }
        return all;
    }

    private static boolean matches(Symbol symbol, JsonNode rule, List<String> scope, Map<String, String> where) {
        if (!symbol.get("role").equals(rule.path("role").asText())) return false;
        for (String attribute : scope) {
            // "scope": false binds across scopes; the variable then says itself how it relates to the scope ("$by")
            if (rule.path("scope").isBoolean() && !rule.path("scope").asBoolean()) break;
            if (attribute.equals("kind") && rule.path("anyKind").asBoolean()) continue;
            String own = symbol.get(attribute);
            if (own != null && !own.equals(where.get(attribute))) return false;
        }
        var filters = rule.path("where").fields();
        while (filters.hasNext()) {
            var filter = filters.next();
            String own = symbol.get(filter.getKey());
            JsonNode wanted = filter.getValue();
            if (wanted.isNull()) { if (own != null) return false; }
            else if (wanted.isArray()) { boolean any = false; for (JsonNode option : wanted) any |= option.asText().equals(own); if (!any) return false; }
            else if (wanted.asText().equals("*")) { if (own == null) return false; }
            else if (wanted.asText().startsWith("$")) { if (own == null || !own.equals(where.get(wanted.asText().substring(1)))) return false; }
            else if (!wanted.asText().equals(own)) return false;
        }
        return true;
    }

    private static Map<String, List<Symbol>> bind(JsonNode law, List<String> scope, Map<String, String> where, List<Symbol> symbols) {
        JsonNode unless = law.path("unless");
        for (JsonNode exception : unless.isArray() ? unless : unless.isObject() ? List.of(unless) : List.<JsonNode>of())
            for (Symbol symbol : symbols) if (matches(symbol, exception, scope, where) && scope.stream().allMatch(a -> symbol.get(a) != null)) return null;
        Map<String, List<Symbol>> bound = new LinkedHashMap<>();
        List<Symbol> unclear = null;
        var variables = law.path("bind").fields();
        while (variables.hasNext()) {
            var variable = variables.next();
            List<Symbol> found = new ArrayList<>();
            for (Symbol symbol : symbols) if (matches(symbol, variable.getValue(), scope, where)) found.add(symbol);
            boolean many = variable.getValue().path("many").asBoolean();
            if (many ? found.size() < variable.getValue().path("min").asInt(0) : found.isEmpty()) return null;
            if (!many && found.size() > 1) unclear = found;
            bound.put(variable.getKey(), found);
        }
        // every quantity the law needs is there, but one of them more than once: the law cannot tell which is meant
        if (unclear != null) { bound.clear(); bound.put(AMBIGUOUS, unclear); }
        return bound;
    }

    private ObjectNode evaluate(JsonNode law, Map<String, String> where, Map<String, List<Symbol>> bound, double[] time) {
        ObjectNode result = json.createObjectNode();
        result.put("law", law.path("id").asText()).put("title", law.path("title").asText()).put("curriculum", law.path("curriculum").asText());
        ObjectNode about = result.putObject("scope");
        where.forEach(about::put);
        ArrayNode uses = result.putArray("quantities");
        bound.values().forEach(list -> list.forEach(symbol -> uses.add(symbol.id())));
        boolean equal = law.has("equal");
        JsonNode sides = law.path(equal ? "equal" : "atMost");
        Map<String, double[][]> single = new LinkedHashMap<>();
        double[][] leftSide = series(sides.get(0), bound, single, time), rightSide = series(sides.get(1), bound, single, time);
        double[] left = leftSide[0], right = rightSide[0];
        double[] mask = law.has("when") ? series(law.path("when"), bound, single, time)[0] : null;
        double span = time.length > 1 ? time[time.length - 1] - time[0] : 0;
        // rounding of a conserved quantity against an exact zero is measured against what such quantities are worth here
        double floor = 1e-3 * Math.max(size(sides.get(0), bound, span, worth), size(sides.get(1), bound, span, worth)), scale = 0, maskScale = 0;
        for (int i = 0; i < time.length; i++) {
            if (!Double.isNaN(left[i]) && !Double.isNaN(right[i])) scale = Math.max(scale, Math.max(Math.abs(left[i]), Math.abs(right[i])));
            if (mask != null) maskScale = Math.max(maskScale, Math.abs(mask[i]));
        }
        int counted = 0, off = 0, worst = -1;
        double worstBy = 0;
        for (int i = 0; i < time.length; i++) {
            if (mask != null && !(Math.abs(mask[i]) > 0.05 * maskScale)) continue;
            if (Double.isNaN(left[i]) || Double.isNaN(right[i])) continue;
            boolean under = true;
            for (List<Symbol> list : bound.values()) for (Symbol symbol : list) {
                boolean[] running = symbol.get("of") == null ? null : active.get(symbol.get("of"));
                if (running != null && !running[i]) under = false;
            }
            if (!under) continue;
            counted++;
            double by = (equal ? Math.abs(left[i] - right[i]) : left[i] - right[i]) - 2 * (leftSide[1][i] + rightSide[1][i]);
            if (by > RELATIVE * scale + floor) off++;
            if (by > worstBy) { worstBy = by; worst = i; }
        }
        if (counted == 0) return null;
        boolean passed = off <= FRACTION * counted;
        // "the rate of X is Y" also says that X does not jump. Where several of the law's objects jump at one instant
        // (they act on each other there), X just after must be X just before; one object jumping alone met something
        // outside the model, and the law does not speak of that instant.
        JsonNode rated = !equal ? null : sides.get(0).isArray() && sides.get(0).get(0).asText().equals("ddt") ? sides.get(0).get(1)
                : sides.get(1).isArray() && sides.get(1).get(0).asText().equals("ddt") ? sides.get(1).get(1) : null;
        if (passed && rated != null && !impacts.isEmpty()) {
            double[] kept = series(rated, bound, single, time)[0], rate = sides.get(0).get(1) == rated ? right : left;
            double size = 0;
            for (double value : kept) if (!Double.isNaN(value)) size = Math.max(size, Math.abs(value));
            Set<String> own = new LinkedHashSet<>();
            for (List<Symbol> list : bound.values()) for (Symbol symbol : list) if (symbol.get("of") != null) own.add(symbol.get("of"));
            for (int i = 1; i < time.length; i++) {
                Set<String> hit = impacts.get(time[i]);
                if (hit == null) continue;
                Set<String> together = new LinkedHashSet<>(hit);
                together.retainAll(own);
                if (together.size() < 2 || Double.isNaN(kept[i]) || Double.isNaN(kept[i - 1])) continue;
                double step = Math.abs(kept[i] - kept[i - 1]), drift = (Double.isNaN(rate[i - 1]) ? 0 : Math.abs(rate[i - 1])) * (time[i] - time[i - 1]);
                if (step - drift > RELATIVE * size) {
                    result.put("status", "FAILED").put("relativeDeviation", size == 0 ? 0 : step / size).put("at", time[i]).put("left", kept[i - 1]).put("right", kept[i]);
                    result.put("message", law.path("title").asText() + " không được thoả" + (where.isEmpty() ? "" : " (" + String.join(", ", where.values()) + ")")
                            + ": tại t = " + time[i] + " s các đối tượng " + String.join(", ", together) + " cùng đổi đột ngột, và đại lượng phải giữ nguyên qua thời điểm đó lại nhảy từ "
                            + kept[i - 1] + " sang " + kept[i] + ". Các đại lượng được kiểm: " + String.join(", ", iterable(uses)) + ".");
                    return result;
                }
            }
        }
        result.put("status", passed ? "PASSED" : "FAILED");
        result.put("relativeDeviation", scale == 0 ? 0 : worstBy / scale);
        if (!passed) {
            result.put("at", time[worst]).put("left", left[worst]).put("right", right[worst]);
            result.put("message", law.path("title").asText() + " không được thoả"
                    + (where.isEmpty() ? "" : " (" + String.join(", ", where.values()) + ")") + ": tại t = " + time[worst] + " s vế trái là "
                    + left[worst] + (equal ? " còn vế phải là " : " nhưng không được vượt quá ") + right[worst]
                    + ". Các đại lượng được kiểm: " + String.join(", ", iterable(uses)) + ".");
        }
        return result;
    }

    private static List<String> iterable(ArrayNode node) { List<String> list = new ArrayList<>(); node.forEach(item -> list.add(item.asText())); return list; }

    /** Slope at sample i of the parabola through three samples of a smooth piece. */
    private static double slope(double[] a, double[] time, int first, int i) {
        double x0 = time[first], x1 = time[first + 1], x2 = time[first + 2], x = time[i];
        return a[first] * (2 * x - x1 - x2) / ((x0 - x1) * (x0 - x2)) + a[first + 1] * (2 * x - x0 - x2) / ((x1 - x0) * (x1 - x2))
                + a[first + 2] * (2 * x - x0 - x1) / ((x2 - x0) * (x2 - x1));
    }

    /**
     * A law expression over the bound quantities: one value per sample and how far that value can be off.
     * Only a rate of change read from the samples is uncertain; where the samples are too coarse to follow a
     * quantity, its rate is weak evidence and is weighed as such instead of being taken for a violation.
     */
    private double[][] series(JsonNode ast, Map<String, List<Symbol>> bound, Map<String, double[][]> current, double[] time) {
        int count = time.length;
        double[] out = new double[count], doubt = new double[count];
        if (ast.isNumber()) { java.util.Arrays.fill(out, ast.asDouble()); return new double[][]{out, doubt}; }
        if (ast.isTextual()) {
            String name = ast.asText();
            if (current.containsKey(name)) return current.get(name);
            List<Symbol> list = bound.get(name);
            if (list == null && name.equals("t")) return new double[][]{time, doubt};
            if (list == null && constants.path(name).path("value").isNumber()) { java.util.Arrays.fill(out, constants.path(name).path("value").asDouble()); return new double[][]{out, doubt}; }
            if (list == null) throw new IllegalStateException("Law uses an unbound quantity: " + name);
            if (list.size() == 1) return new double[][]{list.get(0).values(), noiseOf(list.get(0), count)};
            for (Symbol symbol : list) { double[] own = noiseOf(symbol, count); for (int i = 0; i < count; i++) { out[i] += symbol.values()[i]; doubt[i] += own[i]; } }
            return new double[][]{out, doubt};
        }
        String op = ast.get(0).asText();
        if (op.equals("sum")) {
            List<String> lists = new ArrayList<>();
            names(ast.get(1), lists);
            lists.removeIf(name -> current.containsKey(name) || !bound.containsKey(name));
            if (lists.isEmpty()) return series(ast.get(1), bound, current, time);
            // Terms are formed per item of the first list; items of the other lists join it on the same axis.
            String primary = lists.get(0);
            for (Symbol item : bound.get(primary)) {
                Map<String, double[][]> inner = new LinkedHashMap<>(current);
                inner.put(primary, new double[][]{item.values(), noiseOf(item, count)});
                for (String other : lists.subList(1, lists.size())) {
                    double[] partner = new double[count];
                    // a product of two directed quantities needs their directions: a magnitude given without an axis
                    // cannot be paired, and the law then says nothing about this model
                    boolean directed = item.get("axis") != null;
                    for (Symbol candidate : bound.get(other)) {
                        directed &= candidate.get("axis") != null;
                        if (java.util.Objects.equals(candidate.get("axis"), item.get("axis")))
                            for (int i = 0; i < count; i++) partner[i] += candidate.values()[i];
                    }
                    if (!directed) java.util.Arrays.fill(partner, Double.NaN);
                    inner.put(other, new double[][]{partner, new double[count]});
                }
                double[][] term = series(ast.get(1), bound, inner, time);
                for (int i = 0; i < count; i++) { out[i] += term[0][i]; doubt[i] += term[1][i]; }
            }
            return new double[][]{out, doubt};
        }
        double[][] left = series(ast.get(1), bound, current, time);
        double[] a = left[0], da = left[1];
        if (op.equals("ddt")) {
            for (int i = 0; i < count; i++) {
                int from = i, to = i;
                while (from > 0 && !starts[from]) from--;
                while (to + 1 < count && !starts[to + 1]) to++;
                if (to - from < 2 || !(time[to] > time[from])) { out[i] = Double.NaN; continue; }
                int centred = Math.max(from, Math.min(i - 1, to - 2));
                out[i] = slope(a, time, centred, i);
                // a second reading from a neighbouring triple tells how well the samples resolve the rate here
                int other = centred + 1 <= to - 2 ? centred + 1 : centred - 1 >= from ? centred - 1 : -1;
                doubt[i] = other < 0 ? Math.abs(out[i]) : Math.abs(out[i] - slope(a, time, other, i));
            }
            return new double[][]{out, doubt};
        }
        double[][] right = ast.size() > 2 ? series(ast.get(2), bound, current, time) : null;
        double[] b = right == null ? null : right[0], db = right == null ? null : right[1];
        for (int i = 0; i < count; i++) {
            switch (op) {
                case "add" -> { out[i] = a[i] + b[i]; doubt[i] = da[i] + db[i]; }
                case "sub" -> { out[i] = a[i] - b[i]; doubt[i] = da[i] + db[i]; }
                case "mul" -> { out[i] = a[i] * b[i]; doubt[i] = Math.abs(a[i]) * db[i] + Math.abs(b[i]) * da[i]; }
                case "div" -> { out[i] = a[i] / b[i]; doubt[i] = da[i] / Math.abs(b[i]) + Math.abs(a[i]) * db[i] / (b[i] * b[i]); }
                case "pow" -> { out[i] = Math.pow(a[i], b[i]); doubt[i] = da[i] == 0 ? 0 : Math.abs(b[i] * Math.pow(a[i], b[i] - 1)) * da[i]; }
                case "neg" -> { out[i] = -a[i]; doubt[i] = da[i]; }
                case "abs" -> { out[i] = Math.abs(a[i]); doubt[i] = da[i]; }
                case "sqrt" -> { out[i] = Math.sqrt(a[i]); doubt[i] = da[i] == 0 ? 0 : da[i] / (2 * Math.sqrt(Math.abs(a[i]))); }
                case "sin" -> { out[i] = Math.sin(a[i]); doubt[i] = da[i]; }
                case "cos" -> { out[i] = Math.cos(a[i]); doubt[i] = da[i]; }
                case "exp" -> { out[i] = Math.exp(a[i]); doubt[i] = out[i] * da[i]; }
                case "log" -> { out[i] = Math.log(a[i]); doubt[i] = da[i] == 0 ? 0 : da[i] / Math.abs(a[i]); }
                case "min" -> { out[i] = Math.min(a[i], b[i]); doubt[i] = Math.max(da[i], db[i]); }
                case "max" -> { out[i] = Math.max(a[i], b[i]); doubt[i] = Math.max(da[i], db[i]); }
                default -> throw new IllegalStateException("Law uses an unknown operation: " + op);
            }
        }
        return new double[][]{out, doubt};
    }

    /**
     * What the quantities of an expression are worth over the run. A relation holds when the two sides differ by
     * little against this, so rounding of a conserved quantity against an exact zero is not read as a violation.
     */
    private static double size(JsonNode ast, Map<String, List<Symbol>> bound, double span, Map<String, Double> worth) {
        return size(ast, bound, worth, "");
    }

    /** prefix "" measures values; "d/dt " measures how fast quantities of that unit change in this run. */
    private static double size(JsonNode ast, Map<String, List<Symbol>> bound, Map<String, Double> worth, String prefix) {
        if (ast.isNumber()) return prefix.isEmpty() ? Math.abs(ast.asDouble()) : 0;
        if (ast.isTextual()) {
            double largest = 0;
            if (!bound.containsKey(ast.asText())) return prefix.isEmpty() ? worth.getOrDefault("constant " + ast.asText(), 0.0) : 0;
            for (Symbol symbol : bound.get(ast.asText())) largest = Math.max(largest, worth.getOrDefault(prefix.isEmpty() ? "own " + symbol.id() : prefix + symbol.unit() + "@" + symbol.get("of"), 0.0));
            return largest;
        }
        String op = ast.get(0).asText();
        if (op.equals("ddt")) return size(ast.get(1), bound, worth, "d/dt ");
        double a = size(ast.get(1), bound, worth, prefix), b = ast.size() > 2 ? size(ast.get(2), bound, worth, prefix) : 0;
        if (!prefix.isEmpty() && (op.equals("mul") || op.equals("div")))
            return size(ast.get(1), bound, worth, "") * b + a * size(ast.get(2), bound, worth, "");
        return switch (op) {
            case "mul" -> a * b;
            case "div" -> b > 0 ? a / b : 0;
            case "pow" -> Math.pow(a, ast.get(2).asDouble(1));
            case "sqrt" -> Math.sqrt(a);
            case "add", "sub", "min", "max" -> Math.max(a, b);
            case "sin", "cos" -> 1;
            case "exp", "log" -> 1;
            default -> a;
        };
    }

    private static void names(JsonNode ast, List<String> into) {
        if (ast.isTextual()) { if (!into.contains(ast.asText())) into.add(ast.asText()); }
        else if (ast.isArray()) for (int i = 1; i < ast.size(); i++) names(ast.get(i), into);
    }
}
