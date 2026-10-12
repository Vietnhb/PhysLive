/** Rows of the reviewer's editable tables: arrays of flat objects, with numbers kept as typed until they parse. */
export type JsonObject = Record<string, unknown>;

export function objectRows(value: unknown): JsonObject[] {
  return Array.isArray(value) ? value.filter((row): row is JsonObject => Boolean(row) && typeof row === "object" && !Array.isArray(row)) : [];
}

/** Keeps half-typed numbers ("0.", "-") as text until they parse cleanly. */
export function numericOrRaw(raw: string): number | string | undefined {
  const normalized = raw.trim().replace(",", ".");
  if (normalized === "") return undefined;
  const parsed = Number(normalized);
  return Number.isFinite(parsed) && String(parsed) === normalized ? parsed : raw;
}
