import { useState } from "react";
import type { SimulationWorkspaceModel } from "../hooks/useSimulationWorkspace";
import { downloadFile, timelineCsv } from "../engine/exportData";

export default function SimulationExport({ preview }: Readonly<Pick<SimulationWorkspaceModel, "preview">>) {
  const [busy, setBusy] = useState(false), [error, setError] = useState("");
  const simulation = preview.simulation, timeline = preview.liveTimeline ?? simulation?.simulationSpec.solverTimeline;
  if (!simulation || !timeline) return null;
  const snapshot = { ...simulation, presentation: preview.presentation,
    savedParameters: preview.runValues, validation: preview.validation,
    simulationSpec: { ...simulation.simulationSpec, solverTimeline: timeline } };
  const replay = async () => {
    setBusy(true); setError("");
    try {
      const { offlineReplay } = await import("../engine/offlineReplay");
      downloadFile("physlive-replay.html", offlineReplay(snapshot, timeline, preview.runValues), "text/html;charset=utf-8");
    } catch (failure) { setError(failure instanceof Error ? failure.message : "Không thể xuất mô phỏng."); }
    finally { setBusy(false); }
  };
  return <details><summary>Xuất mô phỏng và số liệu</summary>
    <button type="button" className="sim-action" disabled={preview.recomputing} onClick={() => downloadFile("physlive.json", JSON.stringify(snapshot, null, 2), "application/json")}>JSON</button>
    <button type="button" className="sim-action" disabled={preview.recomputing} onClick={() => downloadFile("physlive.csv", timelineCsv(timeline), "text/csv;charset=utf-8")}>CSV</button>
    <button type="button" className="sim-action" disabled={busy || preview.recomputing} onClick={() => void replay()}>{busy ? "Đang xuất…" : "Phát lại ngoại tuyến / PDF"}</button>
    {error && <p role="alert">{error}</p>}
  </details>;
}
