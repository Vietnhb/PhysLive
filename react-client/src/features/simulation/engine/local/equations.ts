/**
 * The server's SchemaEquationRuntime, line for line: bounded arithmetic ASTs, units, dimensions, the integration of a
 * plan (RK4 with located events, conditions held over a step, jumps, latched ends) and its step-refinement check.
 * Limits and algorithm constants come from the server (SolverContext); nothing here is specific to any situation.
 */
import {
  type Node, SolverError, require, isNumber, isText, isArray, isObject, path, has, hasNonNull, size, isEmpty, elements, fields,
  fieldNames, asText, asDouble, asBoolean, signum, javaInt, ulp, MIN_NORMAL, MAX_VALUE, javaDouble,
} from "./jvm";

export type SolverLimits = {
  defaultStep: number; maxDuration: number; maxSamples: number; maxParticipants: number; maxDepth: number; maxNodes: number;
};
export type SolverConstants = Record<string, number>;
type Values = Map<string, number>;
type States = Map<string, Values>;
type Dimension = Map<string, number> | null;

type Until = { sourceId: string; key: string; rising: boolean; value: number };
type Bound = {
  id: string; capability: Node; constants: Values; references: Map<string, string>; referenceFactor: Values;
  changing: Set<string>; after: string | null; until: Until | null;
};
const math = (bound: Bound) => path(path(bound.capability, "execution"), "math");
const absTolerance = (bound: Bound) => asDouble(path(path(bound.capability, "validation"), "absoluteTolerance"), 0);
const relTolerance = (bound: Bound) => asDouble(path(path(bound.capability, "validation"), "relativeTolerance"), 0);
const alone = (bound: Bound, inputs: Values): Bound =>
  ({ id: bound.id, capability: bound.capability, constants: inputs, references: new Map(), referenceFactor: new Map(), changing: new Set(), after: null, until: null });

type Lookup = { get(name: string): number; side?(condition: Node): number | null };

const UNARY = new Set(["neg", "sin", "cos", "tan", "sqrt", "exp", "abs", "log", "asin", "acos", "atan", "sign", "floor", "latched"]);
const BINARY = new Set(["add", "sub", "mul", "div", "pow", "min", "max", "atan2", "mod"]);
const AT_PARTICIPANT = " (đối tượng ";

function arity(op: string): number {
  if (UNARY.has(op)) return 1;
  if (BINARY.has(op)) return 2;
  if (op === "if") return 3;
  throw new SolverError("Phép toán số học không được hỗ trợ: " + op);
}

const dimEquals = (a: Map<string, number>, b: Map<string, number> | null | undefined) =>
  !!b && a.size === b.size && [...a].every(([key, value]) => b.has(key) && b.get(key) === value);

function names(ast: Node, into: Set<string>) {
  if (isText(ast)) into.add(ast);
  else if (isArray(ast)) for (let i = 1; i < ast.length; i++) names(ast[i], into);
}

export function collectConditions(ast: Node, into: Node[]) {
  if (!isArray(ast) || ast.length === 0) return;
  const op = asText(ast[0]);
  if ((op === "if" || op === "sign" || op === "latched") && ast.length > 1 && !into.some(known => known === ast)) into.push(ast);
  for (let i = 1; i < ast.length; i++) collectConditions(ast[i], into);
}

const close = (a: number, b: number) => Math.abs(a - b) <= 1e-9 * Math.max(Math.abs(a), Math.abs(b));

type Trajectory = {
  times: number[];
  outputs: Map<string, (Values | null)[]>;
  rates: Map<string, (Values | null)[]>;
  inputs: Map<string, (Values | null)[]>;
  start: Values; end: Values;
  startInputs: Map<string, Values>; startOutputs: Map<string, Values>;
  events: [number, Map<string, Values>][];
  switches: number[];
  jumped: [number, string[]][];
  endTime: number;
};

export class Equations {
  private readonly unitDimensions = new Map<string, Map<string, number>>();
  private readonly canonicalUnit = new Map<string, string>();
  private readonly unitFactor = new Map<string, number>();

  constructor(units: Node[], readonly limits: SolverLimits, readonly constants: SolverConstants) {
    for (const unit of units) {
      const dimension = new Map<string, number>();
      for (const [key, value] of fields(path(unit, "dimensions"))) dimension.set(key, asDouble(value));
      const canonical = asText(path(unit, "canonical")), factor = asDouble(path(unit, "factor"));
      if (!this.unitDimensions.has(canonical)) this.unitDimensions.set(canonical, dimension);
      if (!this.canonicalUnit.has(canonical)) this.canonicalUnit.set(canonical, canonical);
      if (!this.unitFactor.has(canonical)) this.unitFactor.set(canonical, 1.0);
      for (const alias of elements(path(unit, "aliases"))) {
        const spelling = asText(alias);
        if (!this.unitDimensions.has(spelling)) this.unitDimensions.set(spelling, dimension);
        if (!this.canonicalUnit.has(spelling)) this.canonicalUnit.set(spelling, canonical);
        if (!this.unitFactor.has(spelling)) this.unitFactor.set(spelling, factor);
      }
    }
  }

  private c(name: string) {
    const value = this.constants[name];
    if (typeof value !== "number") throw new SolverError("Thiếu hằng số của bộ giải: " + name, 500);
    return value;
  }

  // ---------------------------------------------------------------- units

  sameQuantity(a: string, b: string) { return (this.canonicalUnit.get(a) ?? a) === (this.canonicalUnit.get(b) ?? b); }
  convert(value: number, from: string, to: string) { return value * (this.unitFactor.get(from) ?? 1.0) / (this.unitFactor.get(to) ?? 1.0); }
  canonical(unit: string): string | null { return this.canonicalUnit.get(unit) ?? null; }

  // ---------------------------------------------------------------- what a law allows

  changingInputs(capability: Node): Set<string> {
    const m = path(path(capability, "execution"), "math");
    const inputs = new Set<string>(), declared = new Set<string>(), states = new Set<string>(), clocks = new Set<string>(["t"]);
    for (const input of elements(path(capability, "canonicalInputs"))) {
      inputs.add(asText(path(input, "key")));
      if (asBoolean(path(input, "timeVarying"), false)) declared.add(asText(path(input, "key")));
    }
    for (const key of fieldNames(path(m, "initial"))) states.add(key);
    for (const [key, rate] of fields(path(m, "rates"))) if (isNumber(rate) && rate !== 0) clocks.add(key);
    for (const phase of ["outputs", "rates"]) {
      const expressions = path(m, phase);
      for (const [key, value] of fields(expressions)) {
        const symbols = new Set<string>();
        this.symbols(value, expressions, states, inputs, new Set([key]), symbols, 0);
        if ([...symbols].some(s => clocks.has(s))) for (const s of symbols) declared.delete(s);
      }
    }
    return declared;
  }

  private symbols(ast: Node, expressions: Node, states: Set<string>, inputs: Set<string>, seen: Set<string>, into: Set<string>, depth: number) {
    require(depth <= this.limits.maxDepth * 4, "Cấu trúc phương trình vượt quá giới hạn xử lý");
    if (isText(ast)) {
      if (!states.has(ast) && !inputs.has(ast) && has(expressions, ast) && !seen.has(ast)) {
        seen.add(ast);
        this.symbols(expressions[ast], expressions, states, inputs, seen, into, depth + 1);
      } else into.add(ast);
    } else if (isArray(ast)) for (let i = 1; i < ast.length; i++) this.symbols(ast[i], expressions, states, inputs, seen, into, depth + 1);
  }

  // ---------------------------------------------------------------- the plan

  compute(definition: Node, spec: Node, overrides: Node): Node {
    const parameters = new Map<string, number>(), parameterUnits = new Map<string, string>();
    for (const parameter of elements(path(spec, "parameters"))) {
      const name = asText(path(parameter, "name"));
      const value = has(overrides, name) ? asDouble(overrides[name], NaN) : asDouble(path(parameter, "value"), NaN);
      require(name.trim() !== "" && !parameters.has(name) && Number.isFinite(value), "Tham số không hợp lệ");
      require(value >= asDouble(path(parameter, "min"), -MAX_VALUE) && value <= asDouble(path(parameter, "max"), MAX_VALUE),
        "Tham số nằm ngoài phạm vi cho phép: " + name);
      parameters.set(name, value);
      parameterUnits.set(name, asText(path(parameter, "unit")));
    }
    for (const name of fieldNames(overrides)) require(parameters.has(name), "Tham số không xác định: " + name);
    let duration = asDouble(path(spec, "durationSeconds"), NaN);
    if (hasNonNull(spec, "durationParameter")) {
      const key = asText(spec.durationParameter);
      require(parameters.has(key), "Tham số thời gian không xác định");
      for (const parameter of elements(path(spec, "parameters"))) if (asText(path(parameter, "name")) === key) {
        const unit = asText(path(parameter, "unit"));
        require(this.sameQuantity(unit, "s") && close(this.convert(asDouble(path(parameter, "value")), unit, "s"), duration),
          "Tham số thời gian phải khớp thời lượng ban đầu và dùng đơn vị thời gian");
      }
      duration = this.convert(parameters.get(key)!, parameterUnits.get(key)!, "s");
    }
    require(Number.isFinite(duration) && duration > 0 && duration <= this.limits.maxDuration, "Thời lượng mô phỏng không hợp lệ");
    const bindings = this.bind(definition, spec, parameters, parameterUnits);
    const budgetSteps = Math.max(0, Math.trunc((Math.trunc(this.limits.maxSamples / Math.max(1, bindings.length)) - 1) / 2));
    let steps = javaInt(Math.max(Math.min(this.c("MIN_STEPS"), budgetSteps),
      Math.min(Math.min(budgetSteps, this.c("MAX_BASE_STEPS")), Math.ceil(duration / this.limits.defaultStep))));
    require(steps > 0 && (steps * 2 + 1) * Math.max(1, bindings.length) <= this.limits.maxSamples,
      "Dữ liệu diễn tiến vượt quá giới hạn số mẫu. Vui lòng giảm thời lượng mô phỏng");
    const coverage = asText(path(spec, "physicsCoverage")) === "COMPLETE";
    let result = this.run(coverage, bindings, duration, steps);
    while (asText(path(path(result, "validation"), "status")) === "FLAGGED" && steps * 2 <= budgetSteps) {
      steps *= 2;
      result = this.run(coverage, bindings, duration, steps);
    }
    let staircase = false;
    for (const bound of bindings) {
      const conditions: Node[] = [];
      for (const equation of elements(path(math(bound), "outputs"))) collectConditions(equation, conditions);
      staircase ||= conditions.length > this.c("MAX_LOCATED_CONDITIONS");
    }
    while (!staircase && underSampled(path(path(result, "solverTimeline"), "frames"))
      && steps * 2 <= Math.min(budgetSteps, this.c("MAX_DETAIL_STEPS"))) {
      steps *= 2;
      result = this.run(coverage, bindings, duration, steps);
    }
    return result;
  }

  private bind(definition: Node, spec: Node, parameters: Values, parameterUnits: Map<string, string>): Bound[] {
    const models = new Map<string, Node>(), capabilities = new Map<string, Node>();
    for (const model of elements(path(spec, "physicsModels"))) {
      const id = asText(path(model, "id"));
      require(id.trim() !== "" && !models.has(id), "Định danh mô hình bị trùng hoặc bị thiếu");
      models.set(id, model);
      require(models.size <= this.limits.maxParticipants, "Số lượng vật thể vượt quá giới hạn cho phép");
      let capability: Node = null;
      for (const candidate of elements(path(definition, "capabilities")))
        if (asText(path(candidate, "capabilityId")) === asText(path(model, "capabilityId"))) capability = candidate;
      require(capability != null && isObject(path(path(capability, "execution"), "math")), "Chức năng chưa có hệ phương trình thực thi được phê duyệt");
      this.checkDimensions(capability);
      capabilities.set(id, capability);
    }
    const bindings: Bound[] = [];
    for (const model of models.values()) {
      const id = asText(path(model, "id"));
      const capability = capabilities.get(id);
      const constants: Values = new Map(), factors: Values = new Map();
      const references = new Map<string, string>();
      for (const input of elements(path(capability, "canonicalInputs"))) {
        const key = asText(path(input, "key")), unit = asText(path(input, "unit"));
        const binding = path(path(model, "inputs"), key);
        if (isText(binding) && !parameters.has(binding)) {
          const source = this.source(binding, new Set(models.keys()), capabilities);
          require(this.sameQuantity(asText(path(source, "unit")), unit), "Đầu vào phải sử dụng đơn vị SI chuẩn: " + key
            + " cần đơn vị " + unit + ", kết quả " + binding + " có đơn vị " + asText(path(source, "unit")));
          references.set(key, binding);
          factors.set(key, this.convert(1, asText(path(source, "unit")), unit));
          continue;
        }
        let value: number;
        if (isText(binding)) {
          const parameterUnit = parameterUnits.get(binding)!;
          require(this.sameQuantity(parameterUnit, unit), "Đầu vào phải sử dụng đơn vị SI chuẩn: " + key + " cần đơn vị "
            + unit + ", tham số " + binding + " đang dùng " + parameterUnit);
          value = this.convert(parameters.get(binding)!, parameterUnit, unit);
        } else value = isNumber(binding) ? binding : asDouble(path(input, "defaultValue"), NaN);
        require(Number.isFinite(value), "Thiếu đầu vào chuẩn: " + key);
        require(value >= asDouble(path(input, "min"), -MAX_VALUE) && value <= asDouble(path(input, "max"), MAX_VALUE),
          "Giá trị nằm ngoài miền tính toán của bộ giải: " + key);
        constants.set(key, value);
      }
      const after = asText(path(model, "after"), "");
      if (after !== "") {
        require(models.has(after) && after !== id, "Giai đoạn đứng trước không xác định: " + after + " (id hiện có: " + [...models.keys()].join(", ") + ")");
        require(isObject(path(models.get(after), "until")), "Vật " + id + " bắt đầu sau " + after + " nên " + after + " phải có điều kiện kết thúc (until)");
      }
      let until: Until | null = null;
      if (isObject(path(model, "until"))) {
        const rule = model.until;
        const field = asText(path(rule, "field")), direction = asText(path(rule, "direction"));
        const watched = this.source(field, new Set(models.keys()), capabilities);
        require(direction === "rising" || direction === "falling", "Điều kiện kết thúc của " + id + " cần direction là rising hoặc falling");
        let value: number;
        if (isText(path(rule, "value"))) {
          const name = rule.value as string;
          require(parameters.has(name), "Tham số không xác định: " + name);
          require(this.sameQuantity(parameterUnits.get(name)!, asText(path(watched, "unit"))), "Điều kiện kết thúc của " + id
            + ": tham số " + name + " phải cùng loại đơn vị với " + field + " (" + asText(path(watched, "unit")) + ")");
          value = this.convert(parameters.get(name)!, parameterUnits.get(name)!, asText(path(watched, "unit")));
        } else value = asDouble(path(rule, "value"), NaN);
        require(Number.isFinite(value), "Điều kiện kết thúc của " + id + " thiếu giá trị");
        const dot = field.lastIndexOf(".");
        until = { sourceId: field.substring(0, dot), key: field.substring(dot + 1), rising: direction === "rising", value };
      }
      bindings.push({ id, capability, constants, references, referenceFactor: factors, changing: this.changingInputs(capability), after: after === "" ? null : after, until });
    }
    for (const bound of bindings) {
      const chain = new Set<string>();
      for (let at: string | null = bound.id; at != null; at = bindings[indexOf(bindings, at)].after) {
        require(!chain.has(at), "Các giai đoạn nối tiếp nhau theo vòng tròn: " + bound.id);
        chain.add(at);
      }
    }
    return bindings;
  }

  private source(reference: string, ids: Set<string>, capabilities: Map<string, Node>): Node {
    const dot = reference.lastIndexOf(".");
    const id = dot > 0 ? reference.substring(0, dot) : "", key = reference.substring(dot + 1);
    require(ids.has(id), "Tham số đầu vào không xác định: " + reference
      + " (dùng tên một tham số, hoặc id của vật khác, dấu chấm và tên đầu ra; id hiện có: " + [...ids].join(", ") + ")");
    if (key === "t") return { key: "t", unit: "s" };
    let output: Node = null;
    for (const candidate of elements(path(capabilities.get(id), "outputs"))) if (asText(path(candidate, "key")) === key) output = candidate;
    const available = elements(path(capabilities.get(id), "outputs")).map(candidate => asText(path(candidate, "key")));
    require(output != null, "Tham số đầu vào không xác định: " + reference + " (đầu ra của " + id + ": " + available.join(", ") + ")");
    return output;
  }

  // ---------------------------------------------------------------- integration of the whole plan

  private integrate(bounds: Bound[], duration: number, steps: number): Trajectory {
    const sim = new Simulation(this, bounds);
    const out: Trajectory = {
      times: [], outputs: new Map(), rates: new Map(), inputs: new Map(), start: new Map(), end: new Map(),
      startInputs: new Map(), startOutputs: new Map(), events: [], switches: [], jumped: [], endTime: 0,
    };
    for (const bound of bounds) { out.outputs.set(bound.id, []); out.rates.set(bound.id, []); out.inputs.set(bound.id, []); }
    sim.at(new Map(), 0);
    for (const bound of bounds) if (bound.after == null) this.begin(sim, out, bound);
    const h = duration / steps;
    let over = false;
    let looseBefore: number[] | null = null;
    const maxEvents = this.c("MAX_EVENTS_PER_STEP"), bisections = this.c("EVENT_BISECTIONS");
    for (let i = 0; i <= steps && !over; i++) {
      let t = duration * i / steps;
      let state = sim.states;
      let k1: States | null = null;
      if (i < steps) k1 = sim.derivative(state, t); else sim.at(state, t);
      out.times.push(t);
      if (sim.loose.size > 0) {
        const sides = sim.looseSides();
        if (looseBefore != null && !(sides.length === looseBefore.length && sides.every((s, k) => s === looseBefore![k]))) out.switches.push(t);
        looseBefore = sides;
      }
      for (const bound of bounds) {
        const running = sim.running(bound.id);
        out.outputs.get(bound.id)!.push(running || sim.finished.has(bound.id) ? sim.results(bound) : null);
        out.rates.get(bound.id)!.push(running && k1 != null ? k1.get(bound.id)! : null);
        out.inputs.get(bound.id)!.push(running ? sim.inputs(bound) : null);
      }
      out.endTime = t;
      if (i === steps) break;
      let remaining = h;
      sim.hold(true);
      if ([...sim.conditions.values()].some(list => list.length > 0)) k1 = sim.derivative(state, t);
      for (let events = 0; ; events++) {
        const before: Values = new Map();
        for (const bound of bounds) if (bound.until != null && sim.running(bound.id)) before.set(bound.id, sim.distance(bound));
        const next = sim.step(state, t, remaining, k1!);
        sim.at(next, t + remaining);
        const ending = crossed(sim, bounds, before);
        if (ending.length === 0 && !sim.switched()) break;
        require(events < maxEvents, "Trong một bước tính có quá nhiều lần kết thúc giai đoạn hoặc đổi điều kiện (if, sign) liên tiếp. "
          + "Một điều kiện đổi qua lại liên tục thường là vật đã dừng lại mà phương trình vẫn coi là đang chuyển động: hãy kết thúc bằng until, hoặc viết riêng trạng thái đứng yên");
        let low = 0, high = remaining;
        for (let cut = 0; cut < bisections && high - low > ulp(t + high); cut++) {
          const middle = (low + high) / 2;
          sim.at(sim.step(state, t, middle, k1!), t + middle);
          if (crossed(sim, bounds, before).length === 0 && !sim.switched()) low = middle; else high = middle;
        }
        sim.at(sim.step(state, t, high, k1!), t + high);
        const held = sim.lock;
        sim.hold(false);
        const hit = sim.jump(held);
        if (hit.length > 0) out.jumped.push([sim.time, hit]);
        const ended = crossed(sim, bounds, before);
        if (ended.length === 0) out.switches.push(sim.time);
        for (const bound of ended) {
          sim.finish(bound);
          out.end.set(bound.id, sim.time);
          for (const follower of bounds) if (bound.id === follower.after) this.begin(sim, out, follower);
        }
        sim.hold(true);
        const snapshot = new Map<string, Values>();
        for (const bound of bounds) if (sim.running(bound.id) || sim.finished.has(bound.id)) snapshot.set(bound.id, sim.results(bound));
        out.events.push([sim.time, snapshot]);
        out.endTime = sim.time;
        if (sim.states.size === 0) { over = true; break; }
        t = sim.time;
        remaining = duration * (i + 1) / steps - t;
        state = sim.states;
        if (remaining <= 0) break;
        k1 = sim.derivative(state, t);
      }
      sim.hold(false);
    }
    sim.at(sim.states, out.endTime);
    for (const bound of bounds) {
      const first = out.startOutputs.has(bound.id) ? out.startOutputs.get(bound.id)! : sim.results(bound);
      const series = out.outputs.get(bound.id)!;
      for (let i = 0; i < series.length; i++) if (series[i] == null) series[i] = first;
      for (const [, snapshot] of out.events) if (!snapshot.has(bound.id)) snapshot.set(bound.id, first);
    }
    return out;
  }

  private begin(sim: Simulation, out: Trajectory, bound: Bound) {
    sim.start(bound);
    out.start.set(bound.id, sim.time);
    out.startInputs.set(bound.id, sim.inputs(bound));
    out.startOutputs.set(bound.id, sim.results(bound));
  }

  // ---------------------------------------------------------------- verification and the timeline

  private run(coverage: boolean, bindings: Bound[], duration: number, steps: number): Node {
    const flags: Node[] = [], assumptions: Node[] = [], evidence: Node[] = [], invariants: Record<string, boolean> = {};
    const validation: Record<string, Node> = {
      flags, assumptions, convergenceEvidence: evidence, invariantResults: invariants,
      solverVersion: "schema-ast-rk4/1.0", formulaSource: "APPROVED_TOPIC_SCHEMA",
      verificationScope: "Các đại lượng chuẩn đã được liên kết; các giả định vẫn được hiển thị để kiểm tra",
    };
    const result: Record<string, Node> = { validation };
    const coarse = this.integrate(bindings, duration, steps);
    const fine = this.integrate(bindings, duration, steps * 2);
    let allReference = bindings.length > 0, numericalEvidence = bindings.length > 0, passed = true;
    let absoluteError = 0, relativeError = 0, maxResidual = 0, benchmarksChecked = 0;
    const h = duration / steps;
    for (const bound of bindings) {
      const m = math(bound);
      const absTol = asDouble(path(path(bound.capability, "validation"), "absoluteTolerance"), NaN);
      const relTol = asDouble(path(path(bound.capability, "validation"), "relativeTolerance"), NaN);
      require(Number.isFinite(absTol) && absTol >= 0 && Number.isFinite(relTol) && relTol >= 0, "Mô hình cần có sai số cho phép để kiểm định");
      const samples = coarse.outputs.get(bound.id)!, liveInputs = coarse.inputs.get(bound.id)!;
      const byRefinement = asText(path(path(bound.capability, "validation"), "strategy")) === "step_refinement";
      const sizeOf: Values = new Map();
      if (byRefinement) {
        const worth: Values = new Map(), unitOf = new Map<string, string>();
        for (const output of elements(path(bound.capability, "outputs")))
          unitOf.set(asText(path(output, "key")), this.canonicalUnit.get(asText(path(output, "unit"))) ?? asText(path(output, "unit")));
        for (const sample of samples) if (sample != null)
          for (const [key, value] of sample) { const unit = unitOf.get(key) ?? key; worth.set(unit, Math.max(worth.get(unit) ?? -Infinity, Math.abs(value))); }
        for (const [key, unit] of unitOf) sizeOf.set(key, worth.get(unit) ?? 0.0);
      }
      const startInputs = coarse.startInputs.get(bound.id)!;
      const start = coarse.start.has(bound.id) ? coarse.start.get(bound.id)! : NaN;
      let first = -1, last = -1;
      for (let i = 0; i < samples.length; i++) if (liveInputs[i] != null) { if (first < 0) first = i; last = i; }
      let driven = false;
      for (let i = first; first >= 0 && i <= last; i++) for (const key of bound.references.keys()) {
        const value = liveInputs[i]!.get(key)!, initial = startInputs.get(key)!;
        const reference = bound.references.get(key)!;
        const source = bindings[indexOf(bindings, reference.substring(0, reference.lastIndexOf(".")))];
        if (Math.abs(value - initial) <= absTolerance(source) + relTolerance(source) * Math.abs(initial)) continue;
        require(bound.changing.has(key), "Kết quả " + reference + " thay đổi theo thời gian nên không dùng làm đầu vào "
          + key + " của " + bound.id + " được: định luật này chỉ đúng khi " + key + " không đổi");
        driven = true;
      }
      const hasReference = isObject(path(m, "closedForm")) && !isEmpty(path(m, "closedForm")) && !driven;
      allReference &&= hasReference;
      const initialInvariants: Values = new Map();
      for (let i = first; first >= 0 && i <= last; i++) {
        const sample = samples[i]!;
        const reference = hasReference
          ? this.evaluateMap(path(m, "closedForm"), startInputs, new Map([["t", coarse.times[i] - start]]))
          : fineSample(fine, bound.id, i * 2);
        if (reference != null) for (const key of sample.keys()) {
          require(reference.has(key), "Dữ liệu đối chiếu thiếu đầu ra chuẩn: " + key);
          const error = Math.abs(sample.get(key)! - reference.get(key)!);
          const scale = Math.max(Math.max(Math.abs(sample.get(key)!), Math.abs(reference.get(key)!)), sizeOf.get(key) ?? 0.0);
          absoluteError = Math.max(absoluteError, error);
          relativeError = Math.max(relativeError, error / Math.max(scale, absTol > 0 ? absTol : MIN_NORMAL));
          passed &&= error <= absTol + relTol * scale;
        }
        if (!driven) {
          const inv = this.evaluateMap(path(m, "invariants"), startInputs, sample);
          if (i === first) for (const [k, v] of inv) initialInvariants.set(k, v);
          for (const [key, value] of inv) {
            const baseline = initialInvariants.get(key)!;
            const valid = Math.abs(value - baseline) <= absTol + relTol * Math.abs(baseline);
            const name = bound.id + "." + key;
            invariants[name] = (has(invariants, name) ? invariants[name] : true) && valid;
            passed &&= valid;
          }
        }
        const stencilFrom = i >= 2 ? coarse.times[i - 2] : 0, stencilTo = coarse.times[Math.min(i + 2, coarse.times.length - 1)];
        const smooth = !coarse.switches.some(at => at >= stencilFrom && at <= stencilTo);
        if (smooth && !byRefinement && i >= first + 2 && i <= last - 2 && coarse.times[i - 2] >= start) {
          for (const [key, rate] of coarse.rates.get(bound.id)![i]!) {
            require(sample.has(key), "Kết quả bộ giải thiếu trạng thái phương trình vi phân: " + key);
            const derivative = (-samples[i + 2]!.get(key)! + 8 * samples[i + 1]!.get(key)!
              - 8 * samples[i - 1]!.get(key)! + samples[i - 2]!.get(key)!) / (12 * duration / steps);
            const residual = Math.abs(derivative - rate);
            maxResidual = Math.max(maxResidual, residual);
            passed &&= residual <= absTol + relTol * Math.abs(rate);
          }
        }
      }
      let declaredBenchmarks = 0;
      for (const benchmark of elements(path(path(bound.capability, "validation"), "benchmarks"))) {
        if (startInputs == null || !isObject(path(benchmark, "expectedOutputs")) || isEmpty(path(benchmark, "expectedOutputs"))) continue;
        const benchmarkInputs: Values = new Map(startInputs);
        for (const [key, value] of fields(path(benchmark, "inputs"))) benchmarkInputs.set(key, asDouble(value));
        const benchmarkDuration = asDouble(path(benchmark, "durationSeconds"), 1);
        require(benchmarkDuration > 0 && benchmarkDuration <= this.limits.maxDuration, "Thời lượng kiểm chuẩn không hợp lệ");
        const benchmarkSteps = javaInt(Math.ceil(benchmarkDuration / this.limits.defaultStep));
        require(benchmarkSteps <= this.limits.maxSamples, "Số mẫu kiểm chuẩn vượt quá giới hạn");
        const benchmarkSamples = this.integrate([alone(bound, benchmarkInputs)], benchmarkDuration, benchmarkSteps).outputs.get(bound.id)!;
        const end = benchmarkSamples[benchmarkSamples.length - 1]!;
        for (const [key, expected] of fields(path(benchmark, "expectedOutputs"))) {
          require(end.has(key), "Bài kiểm chuẩn tham chiếu đến đầu ra không xác định");
          const value = asDouble(expected, NaN);
          passed &&= Number.isFinite(value) && Math.abs(end.get(key)! - value) <= absTol + relTol * Math.abs(value);
        }
        declaredBenchmarks++; benchmarksChecked++;
      }
      const hasInvariants = isObject(path(m, "invariants")) && !isEmpty(path(m, "invariants"));
      if (driven) {
        const law = asText(path(path(this.run(true, [alone(bound, startInputs)], Math.max(coarse.times[last] - start, h), Math.max(4, last - first)), "validation"), "status"));
        passed &&= law !== "FLAGGED";
        numericalEvidence &&= law.startsWith("VERIFIED") && steps >= 4;
        evidence.push(bound.id + ": inputs change during the run; the law verified on its own (" + law
          + "), the coupled run by step refinement h -> h/2 over " + (last - first + 1) + " checkpoints");
      } else {
        numericalEvidence &&= hasReference || (hasInvariants && declaredBenchmarks > 0 && steps >= 4) || (byRefinement && steps >= 4);
        evidence.push(bound.id + ": compared " + (first < 0 ? 0 : last - first + 1) + " checkpoints using "
          + (hasReference ? "independent approved closed-form AST" : "step refinement h -> h/2 and declared invariants"));
      }
      for (const assumption of elements(path(bound.capability, "assumptions"))) assumptions.push(asText(assumption));
    }
    const staged = bindings.some(bound => bound.after != null || bound.until != null);
    const frames: Node[] = [];
    const timeline: Record<string, Node> = { durationSeconds: staged ? coarse.endTime : duration, frames };
    result.solverTimeline = timeline;
    let event = 0;
    for (let i = 0; i < coarse.times.length; i++) {
      const t = coarse.times[i];
      for (; event < coarse.events.length && coarse.events[event][0] < t; event++) frame(frames, coarse.events[event][0], bindings, coarse.events[event][1]);
      if (event < coarse.events.length && coarse.events[event][0] === t) event++;
      const values = new Map<string, Values>();
      for (const bound of bindings) values.set(bound.id, coarse.outputs.get(bound.id)![i]!);
      frame(frames, t, bindings, values);
    }
    for (; event < coarse.events.length; event++) frame(frames, coarse.events[event][0], bindings, coarse.events[event][1]);
    if (coarse.events.length > 0) {
      timeline.breaks = coarse.events.map(([at]) => at);
      if (coarse.jumped.length > 0) timeline.jumps = coarse.jumped.map(([at, names]) => ({ t: at, quantities: [...names] }));
    }
    if (staged) {
      const phases: Record<string, Node> = {};
      for (const bound of bindings)
        phases[bound.id] = { start: coarse.start.has(bound.id) ? coarse.start.get(bound.id) : null, end: coarse.end.has(bound.id) ? coarse.end.get(bound.id) : null };
      timeline.phases = phases;
    }
    const status = !passed ? "FLAGGED" : !coverage || !numericalEvidence ? "VISUAL_ONLY_UNVERIFIED" : allReference ? "VERIFIED_ANALYTICAL" : "VERIFIED_NUMERICAL";
    if (!coverage) flags.push("Một phần nội dung vật lý chưa được liên kết với phương trình thực thi đã phê duyệt.");
    if (!numericalEvidence && bindings.length > 0) flags.push("Minh chứng tính toán số chưa đầy đủ.");
    if (!passed) flags.push("Kết quả đối chiếu hoặc kiểm tra đại lượng bảo toàn không đạt; mô phỏng chưa được xác minh.");
    validation.status = status;
    validation.executionMethod = bindings.length === 0 ? "VISUAL_ONLY" : "NUMERICAL";
    validation.solverMethod = "RK4";
    validation.verificationMethod = allReference ? "CLOSED_FORM_CHECKPOINTS" : "STEP_REFINEMENT_AND_INVARIANTS";
    validation.absoluteError = absoluteError;
    validation.relativeError = relativeError;
    validation.maxOdeResidual = maxResidual;
    validation.benchmarkSummary = "Đã kiểm tra " + benchmarksChecked + " liên kết kiểm chuẩn được phê duyệt";
    if (allReference) validation.referenceSolverVersion = "schema-ast-closed-form/1.0";
    return result;
  }

  // ---------------------------------------------------------------- dimensions

  private checkDimensions(capability: Node) {
    const symbols = new Map<string, Dimension>();
    for (const [key, list] of fields(path(path(path(capability, "execution"), "math"), "jumps"))) for (const pair of elements(list)) {
      const known = new Map<string, Dimension>();
      known.set("t", new Map([["T", 1.0]]));
      for (const group of ["canonicalInputs", "outputs"]) for (const quantity of elements(path(capability, group)))
        known.set(asText(path(quantity, "key")), this.unitDimensions.get(asText(path(quantity, "unit"))) ?? null);
      try {
        this.dimension(pair[0], known, 0, { n: 0 });
        const actual = this.dimension(pair[1], known, 0, { n: 0 });
        require(actual == null || dimEquals(actual, known.get(key)), "Giá trị sau bước nhảy không cùng thứ nguyên với đại lượng");
      } catch (error) {
        if (error instanceof SolverError) throw new SolverError(error.message + " (trong bước nhảy của " + key + ")");
        throw error;
      }
    }
    symbols.set("t", new Map([["T", 1.0]]));
    for (const group of ["canonicalInputs", "outputs"]) for (const quantity of elements(path(capability, group))) {
      const unit = asText(path(quantity, "unit"));
      require(this.unitDimensions.has(unit), "Thứ nguyên đơn vị chuẩn không xác định: " + unit);
      symbols.set(asText(path(quantity, "key")), this.unitDimensions.get(unit)!);
    }
    const m = path(path(capability, "execution"), "math");
    for (const phase of ["initial", "rates", "outputs", "closedForm", "invariants"]) {
      for (const [key, equation] of fields(path(m, phase))) {
        let actual: Dimension;
        try {
          actual = this.dimension(equation, symbols, 0, { n: 0 });
        } catch (error) {
          if (error instanceof SolverError) throw new SolverError(error.message + " (trong phương trình của " + key
            + (phase === "rates" ? ", tốc độ biến thiên" : phase === "initial" ? ", giá trị ban đầu" : "") + ")");
          throw error;
        }
        if (phase !== "invariants") {
          require(symbols.has(key), "Đơn vị của trạng thái hoặc đầu ra chưa được khai báo: " + key);
          let expected = symbols.get(key)!;
          if (phase === "rates") expected = combine(expected!, new Map([["T", 1.0]]), -1);
          require(actual == null || dimEquals(actual, expected), "Hai vế không cùng thứ nguyên trong phương trình của " + key
            + (phase === "rates" ? " (tốc độ biến thiên phải có đơn vị của đại lượng chia cho giây)" : phase === "initial" ? " (giá trị ban đầu)" : "")
            + ": đơn vị khai báo không khớp với biểu thức");
        }
      }
    }
  }

  private dimension(ast: Node, symbols: Map<string, Dimension>, depth: number, nodes: { n: number }): Dimension {
    require(depth <= this.limits.maxDepth && ++nodes.n <= this.limits.maxNodes, "Phép kiểm tra thứ nguyên vượt quá giới hạn xử lý");
    if (isNumber(ast)) return ast === 0 ? null : new Map();
    if (isText(ast)) { require(symbols.has(ast), "Thứ nguyên đại lượng chưa được khai báo: " + ast); return symbols.get(ast)!; }
    require(isArray(ast) && ast.length >= 2, "Cấu trúc phương trình không hợp lệ");
    const op = asText(path(ast, 0));
    require(ast.length === arity(op) + 1, "Cấu trúc phương trình không hợp lệ");
    const a = this.dimension(ast[1], symbols, depth + 1, nodes);
    const b = ast.length > 2 ? this.dimension(ast[2], symbols, depth + 1, nodes) : new Map<string, number>();
    switch (op) {
      case "add": case "sub": case "min": case "max": case "mod":
        require(a == null || b == null || dimEquals(a, b), "Không thể cộng các đại lượng khác thứ nguyên"); return a == null ? b : a;
      case "mul": return a == null || b == null ? null : combine(a, b, 1);
      case "div": return a == null ? null : combine(a, b == null ? new Map() : b, -1);
      case "neg": case "abs": return a;
      case "sqrt": case "pow": {
        const exponent = op === "sqrt" ? 0.5 : asDouble(path(ast, 2), NaN);
        require(Number.isFinite(exponent) && (b == null || b.size === 0), "Số mũ thứ nguyên phải là hằng số");
        const out = new Map<string, number>();
        if (a != null) for (const [key, value] of a) if (value * exponent !== 0) out.set(key, value * exponent);
        return out;
      }
      case "sin": case "cos": case "tan": case "exp": case "log": case "asin": case "acos": case "atan": case "floor":
        require(a == null || a.size === 0, "Đầu vào hàm siêu việt phải không có thứ nguyên; góc dùng đơn vị radian"); return new Map();
      case "sign": case "latched": return new Map();
      case "atan2": require(a == null || b == null || dimEquals(a, b), "Hai thành phần của atan2 phải cùng thứ nguyên"); return new Map();
      case "if": {
        const otherwise = this.dimension(ast[3], symbols, depth + 1, nodes);
        require(b == null || otherwise == null || dimEquals(b, otherwise), "Hai nhánh của điều kiện phải cùng thứ nguyên");
        return b == null ? otherwise : b;
      }
      default: throw new SolverError("Phép toán thứ nguyên không xác định: " + op);
    }
  }

  // ---------------------------------------------------------------- expressions

  evaluateMap(expressions: Node, inputs: Values, state: Values): Values {
    const result: Values = new Map();
    const resolving: string[] = [];
    const resolver: Lookup = {
      get: (key: string) => state.has(key) ? state.get(key)! : inputs.has(key) ? inputs.get(key)! : evaluateKey(key),
    };
    const evaluateKey = (key: string): number => {
      if (result.has(key)) return result.get(key)!;
      require(has(expressions, key), "Đại lượng trong phương trình không xác định: " + key);
      require(!resolving.includes(key) && resolving.length < this.limits.maxDepth, "Các phương trình phụ thuộc vòng hoặc vượt quá độ sâu cho phép");
      resolving.push(key);
      const value = this.evaluate(expressions[key], resolver, 0, { n: 0 });
      resolving.pop(); result.set(key, value); return value;
    };
    for (const key of fieldNames(expressions)) evaluateKey(key);
    return result;
  }

  value(ast: Node, lookup: (name: string) => number): number {
    return this.evaluate(ast, { get: lookup }, 0, { n: 0 });
  }

  evaluate(ast: Node, lookup: Lookup, depth: number, nodes: { n: number }): number {
    require(depth <= this.limits.maxDepth && ++nodes.n <= this.limits.maxNodes, "Cấu trúc phương trình vượt quá giới hạn xử lý");
    if (isNumber(ast)) return ast;
    if (isText(ast)) return lookup.get(ast);
    require(isArray(ast) && ast.length >= 2, "Cấu trúc biểu thức số học không hợp lệ");
    const op = asText(path(ast, 0));
    require(ast.length === arity(op) + 1, "Số toán hạng trong biểu thức không hợp lệ");
    const a = this.evaluate(ast[1], lookup, depth + 1, nodes);
    const held = (op === "if" || op === "sign" || op === "latched") && lookup.side ? lookup.side(ast[1]) : null;
    if (op === "if") return this.evaluate(ast[(held != null ? held > 0 : a > 0) ? 2 : 3], lookup, depth + 1, nodes);
    if (held != null) return held;
    const b = ast.length > 2 ? this.evaluate(ast[2], lookup, depth + 1, nodes) : 0;
    let value: number;
    switch (op) {
      case "add": value = a + b; break;
      case "sub": value = a - b; break;
      case "mul": value = a * b; break;
      case "div": value = a / b; break;
      case "pow": value = Math.pow(a, b); break;
      case "min": value = javaMin(a, b); break;
      case "max": value = javaMax(a, b); break;
      case "atan2": value = Math.atan2(a, b); break;
      case "mod": value = a - b * Math.floor(a / b); break;
      case "neg": value = -a; break;
      case "sin": value = Math.sin(a); break;
      case "cos": value = Math.cos(a); break;
      case "tan": value = Math.tan(a); break;
      case "sqrt": value = Math.sqrt(a); break;
      case "exp": value = Math.exp(a); break;
      case "abs": value = Math.abs(a); break;
      case "log": value = Math.log(a); break;
      case "asin": value = Math.asin(a); break;
      case "acos": value = Math.acos(a); break;
      case "atan": value = Math.atan(a); break;
      case "sign": value = signum(a); break;
      case "latched": value = a > 0 ? 1 : 0; break;
      case "floor": value = Math.floor(a); break;
      default: throw new SolverError("Phép toán số học không được hỗ trợ: " + op);
    }
    require(Number.isFinite(value), "Phương trình trả về giá trị không xác định hoặc vô hạn");
    return value;
  }
}

/** Math.min / Math.max: NaN wins, and -0.0 is smaller than 0.0 — as Java's. */
export const javaMin = (a: number, b: number) => Number.isNaN(a) || Number.isNaN(b) ? NaN : Math.min(a, b);
export const javaMax = (a: number, b: number) => Number.isNaN(a) || Number.isNaN(b) ? NaN : Math.max(a, b);

function combine(a: Map<string, number>, b: Map<string, number>, sign: number): Map<string, number> {
  const result = new Map(a);
  for (const [key, value] of b) result.set(key, (result.get(key) ?? 0) + sign * value);
  for (const [key, value] of [...result]) if (value === 0) result.delete(key);
  return result;
}

function indexOf(bindings: Bound[], id: string) {
  for (let i = 0; i < bindings.length; i++) if (bindings[i].id === id) return i;
  return -1;
}

function crossed(sim: Simulation, bounds: Bound[], before: Values): Bound[] {
  const ending: Bound[] = [];
  for (const bound of bounds)
    if (before.has(bound.id) && sim.running(bound.id) && before.get(bound.id)! > 0 && sim.distance(bound) <= 0) ending.push(bound);
  return ending;
}

function fineSample(fine: Trajectory, id: string, index: number): Values | null {
  const series = fine.outputs.get(id)!;
  return index < series.length && fine.inputs.get(id)![index] != null ? series[index] : null;
}

function frame(frames: Node[], t: number, bindings: Bound[], values: Map<string, Values>) {
  const out: Record<string, number> = { t };
  for (const bound of bindings) {
    const prefix = bound.id + ".";
    for (const [key, value] of values.get(bound.id)!) if (key !== "t") out[prefix + key] = value;
  }
  frames.push({ t, values: out });
}

/** True when straight lines between the samples misrepresent some result (see SchemaEquationRuntime.underSampled). */
function underSampled(frames: Node): boolean {
  const count = size(frames);
  if (count < 5) return false;
  for (const key of fieldNames(path(frames[0], "values"))) {
    let min = Infinity, max = -Infinity;
    for (const f of frames) { const v = asDouble(path(path(f, "values"), key)); min = javaMin(min, v); max = javaMax(max, v); }
    const range = max - min;
    if (!(range > 1e-9 * Math.max(Math.abs(min), Math.abs(max)))) continue;
    let off = 0;
    for (let i = 1; i < count - 1; i++) {
      const before = asDouble(path(frames[i - 1], "t")), here = asDouble(path(frames[i], "t")), after = asDouble(path(frames[i + 1], "t"));
      if (!(after > before)) continue;
      const a = asDouble(path(path(frames[i - 1], "values"), key)), b = asDouble(path(path(frames[i + 1], "values"), key));
      const line = a + (b - a) * (here - before) / (after - before);
      if (Math.abs(asDouble(path(path(frames[i], "values"), key)) - line) > 0.02 * range) off++;
    }
    if (off > 0.08 * (count - 2)) return true;
  }
  return false;
}

// ---------------------------------------------------------------- the plan at one instant

const NONE = new Map<Node, number>();

class Simulation {
  readonly byId = new Map<string, Bound>();
  states: States = new Map();
  readonly finished = new Map<string, Values>();
  readonly started: Values = new Map();
  time = 0;
  private readonly memo = new Map<string, number>();
  private readonly resolving = new Set<string>();
  readonly conditions = new Map<string, Node[]>();
  lock: Map<string, Map<Node, number>> | null = null;
  readonly latched = new Map<string, Map<Node, number>>();
  readonly loose = new Map<string, Node[]>();

  constructor(private readonly eq: Equations, readonly bounds: Bound[]) {
    for (const bound of bounds) this.byId.set(bound.id, bound);
  }

  at(states: States, time: number) { this.states = states; this.time = time; this.memo.clear(); }
  running(id: string) { return this.states.has(id); }
  localTime(id: string) { return this.running(id) ? this.time - this.started.get(id)! : 0; }

  named(bound: Bound, phase: string, key: string): number {
    const slot = phase + "\u0000" + bound.id + "\u0000" + key;
    const known = this.memo.get(slot);
    if (known !== undefined) return known;
    const expressions = path(math(bound), phase);
    require(has(expressions, key), "Đại lượng trong phương trình không xác định: " + key);
    const fresh = !this.resolving.has(slot);
    this.resolving.add(slot);
    require(fresh && this.resolving.size <= this.eq.limits.maxDepth * 8, "Các phương trình phụ thuộc vòng hoặc vượt quá độ sâu cho phép");
    let value: number;
    try {
      value = this.eq.evaluate(expressions[key], this.scope(bound, phase), 0, { n: 0 });
    } catch (error) {
      if (!(error instanceof SolverError)) throw error;
      if (error.message.includes(AT_PARTICIPANT)) throw error;
      throw new SolverError(error.message + AT_PARTICIPANT + bound.id + ", đại lượng " + key + ")");
    }
    this.resolving.delete(slot);
    this.memo.set(slot, value);
    return value;
  }

  symbol(bound: Bound, phase: string, name: string): number {
    if (name === "t") return this.localTime(bound.id);
    if (phase !== "initial") {
      if (this.running(bound.id)) { const state = this.states.get(bound.id)!.get(name); if (state !== undefined) return state; }
      else if (has(path(math(bound), "initial"), name)) return this.named(bound, "initial", name);
    }
    if (bound.constants.has(name)) return bound.constants.get(name)!;
    if (bound.references.has(name)) return this.input(bound, name);
    const m = math(bound);
    return this.named(bound, has(path(m, phase), name) || !has(path(m, "outputs"), name) ? phase : "outputs", name);
  }

  input(bound: Bound, key: string) {
    const reference = bound.references.get(key)!;
    const dot = reference.lastIndexOf(".");
    return this.result(reference.substring(0, dot), reference.substring(dot + 1)) * bound.referenceFactor.get(key)!;
  }

  result(id: string, key: string): number {
    if (key === "t") return this.finished.has(id) ? this.finished.get(id)!.get("t")! : this.localTime(id);
    if (this.finished.has(id)) return this.finished.get(id)!.get(key)!;
    return this.named(this.byId.get(id)!, "outputs", key);
  }

  results(bound: Bound): Values {
    if (this.finished.has(bound.id)) return this.finished.get(bound.id)!;
    const values: Values = new Map();
    for (const key of fieldNames(path(math(bound), "outputs"))) values.set(key, this.named(bound, "outputs", key));
    return values;
  }

  inputs(bound: Bound): Values {
    const values: Values = new Map(bound.constants);
    for (const key of bound.references.keys()) values.set(key, this.input(bound, key));
    return values;
  }

  rates(bound: Bound): Values {
    const values: Values = new Map();
    for (const key of fieldNames(path(math(bound), "rates"))) values.set(key, this.named(bound, "rates", key));
    return values;
  }

  start(bound: Bound) {
    const state: Values = new Map();
    for (const key of fieldNames(path(math(bound), "initial"))) state.set(key, this.named(bound, "initial", key));
    require(state.size > 0, "Hệ phương trình chưa có trạng thái ban đầu");
    const next: States = new Map(this.states);
    next.set(bound.id, state);
    this.started.set(bound.id, this.time);
    this.at(next, this.time);
  }

  finish(bound: Bound) {
    const last: Values = new Map(this.results(bound));
    last.set("t", this.localTime(bound.id));
    const next: States = new Map(this.states);
    next.delete(bound.id);
    this.finished.set(bound.id, last);
    this.at(next, this.time);
  }

  distance(bound: Bound) {
    const value = this.result(bound.until!.sourceId, bound.until!.key) - bound.until!.value;
    return bound.until!.rising ? -value : value;
  }

  scope(bound: Bound, phase: string): Lookup {
    return {
      get: (name: string) => this.symbol(bound, phase, name),
      side: (condition: Node) => {
        if ((this.latched.get(bound.id) ?? NONE).has(condition)) return 1;
        if (this.lock == null || !this.lock.has(bound.id)) return null;
        const side = this.lock.get(bound.id)!.get(condition);
        return side === undefined ? null : side;
      },
    };
  }

  sides(): Map<string, Map<Node, number>> {
    const all = new Map<string, Map<Node, number>>();
    for (const bound of this.bounds) {
      if (!this.running(bound.id)) continue;
      const own = new Map<Node, number>();
      if (!this.conditions.has(bound.id)) this.conditions.set(bound.id, this.located(bound));
      for (const node of this.conditions.get(bound.id)!) {
        let value: number;
        try { value = this.eq.evaluate(node[1], this.scope(bound, "outputs"), 0, { n: 0 }); }
        catch (error) { if (!(error instanceof SolverError)) throw error; value = NaN; }
        own.set(node[1], asText(node[0]) === "sign" ? javaInt(signum(value))
          : value > 0 || (this.latched.get(bound.id) ?? NONE).has(node[1]) ? 1 : 0);
      }
      all.set(bound.id, own);
    }
    return all;
  }

  located(bound: Bound): Node[] {
    const all: Node[] = [];
    const m = math(bound);
    for (const phase of ["rates", "outputs"]) for (const equation of elements(path(m, phase))) collectConditions(equation, all);
    for (const list of elements(path(m, "jumps"))) for (const pair of elements(list)) collectConditions(pair[0], all);
    if (all.length <= this.eq.constants.MAX_LOCATED_CONDITIONS) return all;
    const needed: Node[] = [];
    const seen = new Set<string>();
    const open: Node[] = [];
    for (const rate of elements(path(m, "rates"))) open.push(rate);
    for (const list of elements(path(m, "jumps"))) for (const pair of elements(list)) { open.push(pair[0]); open.push(pair[1]); }
    for (const other of this.bounds) if (other.until != null && other.until.sourceId === bound.id && has(path(m, "outputs"), other.until.key))
      open.push(path(m, "outputs")[other.until.key]);
    while (open.length > 0) {
      const ast = open.shift();
      collectConditions(ast, needed);
      const found = new Set<string>();
      names(ast, found);
      for (const name of found) if (!seen.has(name)) { seen.add(name); if (has(path(m, "outputs"), name)) open.push(path(m, "outputs")[name]); }
    }
    const rest: Node[] = [];
    for (const node of all) if (!needed.some(known => known === node)) { if (asText(node[0]) === "latched") needed.push(node); else rest.push(node); }
    this.loose.set(bound.id, rest);
    return needed;
  }

  looseSides(): number[] {
    const sides: number[] = [];
    for (const bound of this.bounds) for (const node of this.loose.get(bound.id) ?? []) {
      if (!this.running(bound.id)) { sides.push(0); continue; }
      let value: number;
      try { value = this.eq.evaluate(node[1], this.scope(bound, "outputs"), 0, { n: 0 }); }
      catch (error) { if (!(error instanceof SolverError)) throw error; value = NaN; }
      sides.push(asText(node[0]) === "sign" ? javaInt(signum(value)) : value > 0 ? 1 : 0);
    }
    return sides;
  }

  jump(held: Map<string, Map<Node, number>> | null): string[] {
    const hit: string[] = [];
    const next: States = new Map(this.states);
    let any = false;
    for (const bound of this.bounds) {
      if (!this.running(bound.id) || held == null || !held.has(bound.id)) continue;
      for (const [key, list] of fields(path(math(bound), "jumps"))) {
        for (const pair of elements(list)) {
          const condition = pair[0][1];
          const was = held.get(bound.id)!.get(condition);
          if (was === undefined || was !== 0 || !(this.eq.evaluate(condition, this.scope(bound, "outputs"), 0, { n: 0 }) > 0)) continue;
          const value = this.eq.evaluate(pair[1], this.scope(bound, "outputs"), 0, { n: 0 });
          require(Number.isFinite(value), "Bước nhảy của " + key + " cho giá trị không xác định");
          if (next.get(bound.id) === this.states.get(bound.id)) next.set(bound.id, new Map(this.states.get(bound.id)!));
          next.get(bound.id)!.set(key, value);
          any = true;
          hit.push(key);
          break;
        }
      }
    }
    if (any) this.at(next, this.time);
    return hit;
  }

  hold(on: boolean) {
    this.lock = null;
    this.memo.clear();
    if (on) {
      this.lock = this.sides();
      for (const bound of this.bounds) for (const node of this.conditions.get(bound.id) ?? [])
        if (asText(node[0]) === "latched" && this.lock.has(bound.id) && this.lock.get(bound.id)!.get(node[1]) === 1) {
          if (!this.latched.has(bound.id)) this.latched.set(bound.id, new Map());
          this.latched.get(bound.id)!.set(node[1], 1);
        }
      this.memo.clear();
    }
  }

  switched(): boolean {
    const now = this.sides();
    for (const [id, own] of now) {
      const held = this.lock == null ? undefined : this.lock.get(id);
      if (held === undefined) continue;
      for (const [condition, side] of own) if (side !== held.get(condition)) return true;
    }
    return false;
  }

  derivative(at: States, t: number): States {
    this.at(at, t);
    const all: States = new Map();
    for (const id of at.keys()) all.set(id, this.rates(this.byId.get(id)!));
    return all;
  }

  advance(from: States, slope: States, h: number): States {
    const all: States = new Map();
    for (const [id, state] of from) {
      const next: Values = new Map();
      for (const [key, value] of state) {
        require(slope.get(id)!.has(key), "Thiếu đạo hàm của trạng thái: " + key);
        next.set(key, value + h * slope.get(id)!.get(key)!);
      }
      all.set(id, next);
    }
    return all;
  }

  step(from: States, t: number, h: number, k1: States): States {
    const k2 = this.derivative(this.advance(from, k1, h / 2), t + h / 2);
    const k3 = this.derivative(this.advance(from, k2, h / 2), t + h / 2);
    const k4 = this.derivative(this.advance(from, k3, h), t + h);
    const all: States = new Map();
    for (const [id, state] of from) {
      const next: Values = new Map();
      for (const key of state.keys()) {
        require(k1.get(id)!.has(key) && k2.get(id)!.has(key) && k3.get(id)!.has(key) && k4.get(id)!.has(key), "Thiếu đạo hàm của trạng thái: " + key);
        const value = state.get(key)! + h / 6 * (k1.get(id)!.get(key)! + 2 * k2.get(id)!.get(key)! + 2 * k3.get(id)!.get(key)! + k4.get(id)!.get(key)!);
        require(Number.isFinite(value), "Bộ giải trả về giá trị không xác định hoặc vô hạn");
        next.set(key, value);
      }
      all.set(id, next);
    }
    return all;
  }
}

export { javaDouble };
