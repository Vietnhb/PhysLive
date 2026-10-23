import { useEffect, useState } from "react";
import { loadTopicNames } from "../api/topicCatalog";

/** The Vietnamese name of a topic code; the code itself until the catalogue has loaded. */
export function useTopicName() {
  const [names, setNames] = useState<Map<string, string> | null>(null);
  useEffect(() => { let active = true; void loadTopicNames().then(value => { if (active) setNames(value); }); return () => { active = false; }; }, []);
  return (code?: string | null) => !code ? "" : names?.get(code) ?? code;
}
