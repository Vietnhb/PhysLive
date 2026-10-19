/**
 * What the server's solver relies on from Java and Jackson, written out so the browser computes the same thing:
 * JSON node reading with Jackson's defaults, Java's double to text, String.hashCode, Math.ulp and int casts.
 */

/* eslint-disable @typescript-eslint/no-explicit-any */
export type Node = any;

export class SolverError extends Error {
  /** The HTTP status the server answers this with (422 for a model or slider the solver refuses). */
  readonly status: number;
  constructor(message: string, status = 422) { super(message); this.status = status; }
}

export function require(condition: unknown, message: string): asserts condition {
  if (!condition) throw new SolverError(message);
}

export const isNumber = (n: Node): n is number => typeof n === "number";
export const isText = (n: Node): n is string => typeof n === "string";
export const isArray = (n: Node): n is Node[] => Array.isArray(n);
export const isObject = (n: Node): n is Record<string, Node> => n !== null && typeof n === "object" && !Array.isArray(n);
/** Jackson's isInt: an integral number (the browser cannot tell 2 from 2.0 once parsed). */
export const isInt = (n: Node) => typeof n === "number" && Number.isInteger(n) && Math.abs(n) <= 2147483647;

/** node.path(key): the child, or undefined (Jackson's MissingNode). */
export function path(n: Node, key: string | number): Node {
  if (typeof key === "number") return Array.isArray(n) ? n[key] : undefined;
  return isObject(n) && Object.prototype.hasOwnProperty.call(n, key) ? n[key] : undefined;
}

export const has = (n: Node, key: string | number) =>
  typeof key === "number" ? Array.isArray(n) && key >= 0 && key < n.length : isObject(n) && Object.prototype.hasOwnProperty.call(n, key);
export const hasNonNull = (n: Node, key: string) => has(n, key) && n[key] !== null;
export const size = (n: Node) => Array.isArray(n) ? n.length : isObject(n) ? Object.keys(n).length : 0;
export const isEmpty = (n: Node) => size(n) === 0;
/** Iterating a node as Jackson does: array elements, object values, nothing for a value. */
export const elements = (n: Node): Node[] => Array.isArray(n) ? n : isObject(n) ? Object.values(n) : [];
export const fields = (n: Node): [string, Node][] => isObject(n) ? Object.entries(n) : [];
export const fieldNames = (n: Node): string[] => isObject(n) ? Object.keys(n) : [];

export function asText(n: Node, fallback?: string): string {
  if (n === undefined) return fallback ?? "";
  if (n === null) return fallback ?? "null";
  if (typeof n === "string") return n;
  if (typeof n === "number") return Number.isInteger(n) ? String(n) : javaDouble(n);
  if (typeof n === "boolean") return String(n);
  return fallback ?? "";
}

export function asDouble(n: Node, fallback = 0): number {
  if (typeof n === "number") return n;
  if (typeof n === "boolean") return n ? 1 : 0;
  if (typeof n === "string") { const t = n.trim(); const v = t === "" ? NaN : Number(t); return Number.isNaN(v) && t !== "NaN" ? fallback : v; }
  return fallback;
}

export function asInt(n: Node, fallback = 0): number {
  if (typeof n === "number") return javaInt(n);
  if (typeof n === "boolean") return n ? 1 : 0;
  if (typeof n === "string") { const v = Number(n.trim()); return Number.isFinite(v) ? javaInt(v) : fallback; }
  return fallback;
}

export function asLong(n: Node, fallback = 0): bigint {
  if (typeof n === "number") return Number.isFinite(n) ? BigInt(Math.trunc(n)) : 0n;
  if (typeof n === "string") { const v = Number(n.trim()); return Number.isFinite(v) ? BigInt(Math.trunc(v)) : BigInt(fallback); }
  return BigInt(fallback);
}

export function asBoolean(n: Node, fallback = false): boolean {
  if (typeof n === "boolean") return n;
  if (typeof n === "number") return n !== 0;
  if (typeof n === "string") return n.trim() === "true" ? true : n.trim() === "false" ? false : fallback;
  return fallback;
}

/** JsonNode.equals: deep, by value. */
export function jsonEquals(a: Node, b: Node): boolean {
  if (a === b) return true;
  if (Array.isArray(a)) return Array.isArray(b) && a.length === b.length && a.every((x, i) => jsonEquals(x, b[i]));
  if (isObject(a)) {
    if (!isObject(b)) return false;
    const ka = Object.keys(a), kb = Object.keys(b);
    return ka.length === kb.length && ka.every(k => Object.prototype.hasOwnProperty.call(b, k) && jsonEquals(a[k], b[k]));
  }
  return Number.isNaN(a) && Number.isNaN(b);
}

export const deepCopy = <T>(n: T): T => n === undefined ? n : structuredClone(n);

/** (int) cast of a double: NaN is 0, values beyond the int range clamp. */
export function javaInt(x: number): number {
  if (Number.isNaN(x)) return 0;
  if (x >= 2147483647) return 2147483647;
  if (x <= -2147483648) return -2147483648;
  return Math.trunc(x);
}

/** Math.signum: the sign of a double, NaN stays NaN. */
export const signum = (x: number) => Number.isNaN(x) ? NaN : x > 0 ? 1 : x < 0 ? -1 : x;

export const MIN_NORMAL = 2.2250738585072014e-308;
export const MAX_VALUE = Number.MAX_VALUE;

const buffer = new DataView(new ArrayBuffer(8));

/** Math.ulp: the distance from x to the next larger double in magnitude. */
export function ulp(x: number): number {
  if (!Number.isFinite(x)) return Number.isNaN(x) ? NaN : Infinity;
  buffer.setFloat64(0, Math.abs(x));
  const exponent = (buffer.getUint32(0) >>> 20) & 0x7ff;
  return exponent === 0 ? Number.MIN_VALUE : Math.pow(2, exponent - 1075);
}

/** String.hashCode: 31·h + char over the UTF-16 units, as a 32-bit int. */
export function javaHash(text: string): number {
  let h = 0;
  for (let i = 0; i < text.length; i++) h = (Math.imul(31, h) + text.charCodeAt(i)) | 0;
  return h;
}

/** Double.toString (JDK 19+): the shortest digits that round-trip, plain between 10^-3 and 10^7, else d.dddE±n. */
export function javaDouble(d: number): string {
  if (Number.isNaN(d)) return "NaN";
  if (d === Infinity) return "Infinity";
  if (d === -Infinity) return "-Infinity";
  if (d === 0) return Object.is(d, -0) ? "-0.0" : "0.0";
  const sign = d < 0 ? "-" : "", a = Math.abs(d);
  const [mantissa, exp] = a.toExponential().split("e");
  const digits = mantissa.replace(".", ""), e = Number(exp);
  if (a >= 1e-3 && a < 1e7) {
    if (e >= 0) {
      const whole = digits.length > e + 1 ? digits.slice(0, e + 1) : digits.padEnd(e + 1, "0");
      const fraction = digits.length > e + 1 ? digits.slice(e + 1) : "0";
      return sign + whole + "." + fraction;
    }
    return sign + "0." + "0".repeat(-e - 1) + digits;
  }
  return sign + digits[0] + "." + (digits.length > 1 ? digits.slice(1) : "0") + "E" + e;
}

/** String concatenation of a value as Java writes it: ints plainly, doubles as Double.toString. */
export const javaText = (value: number, integral = false) => integral ? String(value) : javaDouble(value);
