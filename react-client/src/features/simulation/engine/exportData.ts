import type { SolverTimeline } from "../model/svgScene";

export function timelineCsv(timeline: SolverTimeline): string {
  const keys = [...new Set(timeline.frames.flatMap(frame => Object.keys(frame.values)))];
  const cell = (value: string | number) => `"${String(value).replaceAll('"', '""')}"`;
  return ["time_seconds," + keys.map(cell).join(","), ...timeline.frames.map(frame =>
    [frame.t, ...keys.map(key => frame.values[key] ?? "")].map(cell).join(","))].join("\r\n");
}

export function scriptJson(value: unknown): string {
  return JSON.stringify(value).replaceAll("<", "\\u003c").replaceAll("\u2028", "\\u2028").replaceAll("\u2029", "\\u2029");
}

export function downloadFile(name: string, content: string, type: string) {
  const blob = new Blob([content], { type });
  // The Android shell shares the same snapshot without exposing session credentials.
  const mobile = (window as unknown as { PhysLiveDownload?: { postMessage: (value: string) => void } }).PhysLiveDownload;
  if (mobile) {
    const reader = new FileReader();
    reader.onload = () => mobile.postMessage(JSON.stringify({ name, type, data: String(reader.result).split(",")[1] }));
    reader.readAsDataURL(blob);
    return;
  }
  const url = URL.createObjectURL(blob), link = document.createElement("a");
  link.href = url; link.download = name; link.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
