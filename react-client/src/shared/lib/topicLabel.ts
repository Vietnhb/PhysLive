import type { LocalizedName } from "../types/localization";

/** Locale selection only. Translations come from the API/database. */
export function topicLabel(name: LocalizedName | string, locale: keyof LocalizedName = "vi"): string {
  if (typeof name === "string") return name;
  return name[locale]?.trim() || name.en?.trim() || name.vi?.trim() || "";
}
