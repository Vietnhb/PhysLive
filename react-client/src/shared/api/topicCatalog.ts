import api from "./client";

type SchemaSummary = { topic?: string | null; name?: string | null; lifecycleStatus?: string | null };

let cache: Promise<Map<string, string>> | null = null;

/**
 * Vietnamese names of topic codes (ELECTRIC_CURRENT → "Dòng điện, mạch điện"), read once from the topic
 * catalogue every signed-in role may read. An approved version names its topic before any other version.
 */
export function loadTopicNames(): Promise<Map<string, string>> {
  cache ??= api.get<SchemaSummary[]>("/schemas").then(response => {
    const names = new Map<string, string>();
    const ordered = [...response.data].sort((a, b) => Number(b.lifecycleStatus === "APPROVED") - Number(a.lifecycleStatus === "APPROVED"));
    for (const schema of ordered) if (schema.topic && schema.name && !names.has(schema.topic)) names.set(schema.topic, schema.name);
    return names;
  }).catch(() => { cache = null; return new Map<string, string>(); });
  return cache;
}
