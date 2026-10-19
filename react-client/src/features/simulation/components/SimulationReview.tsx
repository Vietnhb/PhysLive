import { useState, type CSSProperties } from "react";
import MathFormula from "../../../shared/ui/MathFormula";
import { prettyUnit } from "../model/sceneModel";
import type { IntentResult, RecognitionResult, SimulationParameter, SimulationFormulaBinding } from "../api/simulationUnderstandingApi";

export function RecognitionDisplay({ recognition }: Readonly<{ recognition: RecognitionResult }>) {
  const source = recognition.displayText || recognition.recognizedText || "";
  if (recognition.sourceMode === "LATEX") {
    return <div className="simulation-recognized-math" aria-label={source}>
      <MathFormula latex={recognition.recognizedText} />
    </div>;
  }
  return <div className="simulation-recognized-text">
    {source.split(/\n\s*\n/).map((paragraph, index) => <p key={index}>{paragraph}</p>)}
  </div>;
}

export function SimulationParameterControl({ parameter, value, onChange }: Readonly<{
  parameter: SimulationParameter;
  value: number;
  onChange: (value: number) => void;
}>) {
  const { min, max, step } = parameter;
  const [draft, setDraft] = useState<string | null>(null);
  // While the thumb moves only the number follows it; the value is sent once, when the thumb is let go.
  const [dragging, setDragging] = useState<number | null>(null);
  const release = () => {
    if (dragging !== null && dragging !== value) onChange(dragging);
    setDragging(null);
  };
  const commit = () => {
    const numeric = draft?.trim() ? Number(draft) : NaN;
    if (Number.isFinite(numeric) && numeric !== value) onChange(numeric);
    setDraft(null);
  };
  const unit = prettyUnit(parameter.unit ?? "");
  const rawLabel = parameter.label || parameter.name;
  const label = rawLabel;
  const validBounds = typeof min === "number" && Number.isFinite(min) && typeof max === "number" && Number.isFinite(max) && min <= max;
  const showSlider = validBounds && min! < max! && value >= min! && value <= max!;
  const shown = dragging ?? value;
  const fill = showSlider ? Math.min(100, Math.max(0, ((shown - min!) / (max! - min!)) * 100)) : 0;
  return <div className="simulation-control">
    <div className="simulation-control__head">
      <label htmlFor={`sim-param-${parameter.name}`}>{label}</label>
      <span className="simulation-control__value">
        <input id={`sim-param-${parameter.name}`} type="number" inputMode="decimal" step="any"
          value={draft ?? String(shown)}
          onFocus={(event) => { setDraft(String(value)); event.currentTarget.select(); }} onChange={(event) => setDraft(event.target.value)}
          onBlur={commit} onKeyDown={(event) => { if (event.key === "Enter") event.currentTarget.blur(); }} />
        {unit && <span className="simulation-control__unit">{unit}</span>}
      </span>
    </div>
    {showSlider && <>
      <input className="simulation-control__range" type="range" aria-label={`Điều chỉnh ${rawLabel}`} min={min} max={max} step={step ?? "any"} value={shown}
        style={{ "--fill": `${fill}%` } as CSSProperties}
        onChange={(event) => setDragging(Number(event.target.value))}
        onPointerUp={release} onKeyUp={release} onBlur={release} />
      <div className="simulation-control__scale" aria-hidden="true"><span>{formatBound(min!)}</span><span>{formatBound(max!)}</span></div>
    </>}
  </div>;
}

const formatBound = (bound: number) => String(bound);

const bindingSource = (row: SimulationFormulaBinding) =>
  row.source === "PARAMETER" ? `thanh trượt “${row.parameterLabel || row.parameter}”`
    : row.source === "OUTPUT" ? `${row.outputLabel || row.output} của “${row.participantLabel || row.participant}”`
    : row.source === "FIXED" ? "giá trị cố định (không có thanh trượt)" : "giá trị mặc định của định luật";

/** Value → law-input table taken from the signed plan, so a wrong binding is visible before confirming. */
function BindingTable({ bindings }: Readonly<{ bindings: SimulationFormulaBinding[] }>) {
  return <table className="simulation-bindings">
    <caption>Giá trị đưa vào công thức</caption>
    <thead><tr><th scope="col">Đại lượng trong định luật</th><th scope="col">Giá trị</th><th scope="col">Lấy từ</th></tr></thead>
    <tbody>{bindings.map(row => <tr key={row.quantity}>
      <td>{row.label}</td>
      <td className="simulation-bindings__value">{typeof row.value === "number" && Number.isFinite(row.value)
        ? `${String(row.value)} ${prettyUnit(row.unit)}`.trim() : "—"}</td>
      <td>{bindingSource(row)}</td>
    </tr>)}</tbody>
  </table>;
}

export function FormulaReview({ intent }: Readonly<{ intent: IntentResult }>) {
  const labels = new Map((intent.simulationSpec?.physicsModels ?? []).map(model => [model.id, model.label]));
  return <div className="simulation-formulas">
    <h3>Công thức áp dụng</h3>
    {intent.formulas?.length ? intent.formulas.map(formula => <div key={formula.modelId}>
      <strong>{formula.label || labels.get(formula.modelId) || "Đối tượng"}</strong>
      {formula.canonical.map((equation, index) => <MathFormula key={index} equation={equation} block />)}
      {!!formula.derived?.length && <div><span>Suy ra: </span>{formula.derived.map((equation, index) => <span key={index}>{index > 0 && "; "}<MathFormula equation={equation} /></span>)}</div>}
      {!!formula.bindings?.length && <BindingTable bindings={formula.bindings} />}
    </div>) : <p>Hiện chưa có công thức tính toán đã kiểm duyệt cho tình huống này; hình sẽ chỉ mang tính minh họa.</p>}
  </div>;
}
