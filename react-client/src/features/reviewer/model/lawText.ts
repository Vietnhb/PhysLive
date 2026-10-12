/* A law the way a physics teacher writes it — "m·d(v)/dt = ΣF" — and back to the tree the checker reads.
   One printer serves both the page and the edit box, so what a reviewer reads is exactly what they can type. */

export type Ast = number | string | [string, ...Ast[]];
export type Relation = { sign: "=" | "≤"; left: Ast; right: Ast };

const BINARY: Record<string, [string, number]> = { add: [" + ", 1], sub: [" − ", 1], mul: ["·", 2], div: ["/", 2], pow: ["^", 4] };
const FUNCTIONS: Record<string, string> = { sum: "sum", "Σ": "sum", sqrt: "sqrt", "√": "sqrt", abs: "abs", sin: "sin", cos: "cos", exp: "exp", log: "log", ln: "log", max: "max", min: "min", ddt: "ddt" };

function level(ast: Ast): number {
  if (!Array.isArray(ast)) return typeof ast === "number" && ast < 0 ? 3 : 5;
  return BINARY[ast[0]]?.[1] ?? (ast[0] === "neg" ? 3 : 5);
}

export function print(ast: Ast): string {
  if (!Array.isArray(ast)) return typeof ast === "number" ? String(ast).replace("-", "−") : String(ast);
  const [op, ...args] = ast;
  const wrap = (item: Ast, least: number) => level(item) < least ? `(${print(item)})` : print(item);
  if (BINARY[op] && args.length === 2) {
    const [sign, own] = BINARY[op];
    // The right side of −, / and ^ binds tighter than the operator itself: a − (b − c), a/(b·c).
    return wrap(args[0], op === "pow" ? 5 : own) + sign + wrap(args[1], op === "add" || op === "mul" ? own : op === "pow" ? 5 : own + 1);
  }
  if (op === "neg") return `−${wrap(args[0], 2)}`;
  if (op === "ddt") return `d(${print(args[0])})/dt`;
  if (op === "sum") return `Σ${typeof args[0] === "string" ? args[0] : `(${print(args[0])})`}`;
  if (op === "sqrt") return `√(${print(args[0])})`;
  if (op === "abs") return `|${print(args[0])}|`;
  return `${op}(${args.map(print).join(", ")})`;
}

/** Squares and cubes as a reader expects them; only for showing, never typed back. */
export const pretty = (text: string) => text.replace(/\^2(?!\d)/g, "²").replace(/\^3(?!\d)/g, "³");

export function relationOf(law: Record<string, unknown>): Relation | null {
  const sides = (law.equal ?? law.atMost) as unknown;
  return Array.isArray(sides) && sides.length === 2 ? { sign: law.equal ? "=" : "≤", left: sides[0] as Ast, right: sides[1] as Ast } : null;
}

export const relationText = (law: Record<string, unknown>) => {
  const relation = relationOf(law);
  return relation ? `${print(relation.left)} ${relation.sign} ${print(relation.right)}` : "";
};

/** Every letter the expression mentions, in reading order. */
export function symbols(ast: unknown, into: string[] = []): string[] {
  if (typeof ast === "string") { if (!into.includes(ast)) into.push(ast); }
  else if (Array.isArray(ast)) ast.slice(1).forEach(item => symbols(item, into));
  return into;
}

class Unreadable extends Error {}

/** Reads one side of a law. Throws Unreadable with a sentence the reviewer can act on. */
export function parse(text: string): Ast {
  const tokens = text.replace(/²/g, "^2").replace(/³/g, "^3").match(/\d+(?:\.\d+)?(?:[eE][+-]?\d+)?|[\p{L}_][\p{L}\p{N}_]*|\S/gu) ?? [];
  let at = 0;
  const peek = () => tokens[at];
  const take = (expected?: string) => {
    if (expected && tokens[at] !== expected) throw new Unreadable(tokens[at] ? `Chỗ “${tokens[at]}” lẽ ra phải là “${expected}”.` : `Thiếu “${expected}” ở cuối.`);
    return tokens[at++];
  };
  const sum = (): Ast => {
    let left = product();
    while (["+", "-", "−"].includes(peek())) left = [take() === "+" ? "add" : "sub", left, product()];
    return left;
  };
  const product = (): Ast => {
    let left = signed();
    while (["*", "·", "×", "/"].includes(peek())) left = [take() === "/" ? "div" : "mul", left, signed()];
    return left;
  };
  const signed = (): Ast => {
    if (peek() === "-" || peek() === "−") { take(); return ["neg", signed()]; }
    const base = atom();
    return peek() === "^" ? (take(), ["pow", base, signed()]) : base;
  };
  const atom = (): Ast => {
    const token = take();
    if (token === undefined) throw new Unreadable("Công thức đang viết dở.");
    if (/^\d/.test(token)) return Number(token);
    if (token === "(") { const inner = sum(); take(")"); return inner; }
    if (token === "|") { const inner = sum(); take("|"); return ["abs", inner]; }
    if (token === "Σ" || token === "√") return [FUNCTIONS[token], atom()];
    if (!/^[\p{L}_]/u.test(token)) throw new Unreadable(`Không hiểu kí tự “${token}”.`);
    if (peek() !== "(") return token;
    take("(");
    const args = [sum()];
    while (peek() === ",") { take(); args.push(sum()); }
    take(")");
    if (token === "d" && peek() === "/" && tokens[at + 1] === "dt") { at += 2; return ["ddt", args[0]]; }
    if (!FUNCTIONS[token]) throw new Unreadable(`Không có hàm “${token}”. Viết phép nhân bằng dấu · hoặc *, ví dụ ${token}·(…).`);
    return [FUNCTIONS[token], ...args];
  };
  const result = sum();
  if (at < tokens.length) throw new Unreadable(`Thừa “${tokens[at]}”. Viết phép nhân bằng dấu · hoặc *.`);
  return result;
}

/** "m·a = ΣF" or "Pu ≤ P" → the relation, or the reason it cannot be read. */
export function parseRelation(text: string): Relation | string {
  const parts = text.replace(/<=/g, "≤").split(/([=≤])/);
  if (parts.length !== 3) return parts.length < 3 ? "Công thức cần có dấu = (hoặc ≤) giữa hai vế." : "Chỉ dùng một dấu = hoặc ≤.";
  try { return { sign: parts[1] as "=" | "≤", left: parse(parts[0]), right: parse(parts[2]) }; }
  catch (problem) { if (problem instanceof Unreadable) return problem.message; throw problem; }
}

/** "vận tốc của vật trên một trục (of, axis)" → "vận tốc của vật trên một trục": the bracket is a note for the AI. */
export const plainName = (description: unknown, fallback: string) => {
  const text = String(description ?? "").split(/[(;]/)[0].trim();
  return text || fallback;
};

/** A code the system can keep from a Vietnamese name: "Tốc độ góc" → "toc_do_goc". */
export const codeFrom = (name: string, taken: Iterable<string>) => {
  const base = name.normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/đ/g, "d").replace(/Đ/g, "D").toLowerCase().replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "").slice(0, 40) || "muc";
  const used = new Set(taken);
  let code = base;
  for (let n = 2; used.has(code); n++) code = `${base}_${n}`;
  return code;
};

const SAME: Record<string, string> = {
  of: "cùng một vật", axis: "cùng một trục", kind: "cùng loại", by: "cùng vật gây ra", in: "cùng một hệ",
  in_series: "cùng đoạn mạch nối tiếp", in_parallel: "cùng đoạn mạch song song", in_node: "cùng một nút mạch", in_loop: "cùng một mạch kín", inString: "cùng một sợi dây",
};
export const sameAs = (attribute: string) => SAME[attribute] ?? `cùng “${attribute}”`;
