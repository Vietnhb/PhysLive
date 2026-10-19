import { readVerification } from "../model/verification";
import { useState, type Ref } from "react";
import Icon from "../../../shared/ui/LearningIcon";
import SaveSimulationPanel from "../../library/components/SaveSimulationPanel";
import { FormulaReview, SimulationParameterControl } from "./SimulationReview";
import type { SimulationWorkspaceModel } from "../hooks/useSimulationWorkspace";
import type { SimulationParameter } from "../api/simulationUnderstandingApi";

const QUICK_DESCRIPTIONS = [
  { label: "Ba con lắc đơn", text: "3 con lắc đơn có chiều dài dây 1.5 m, vật nặng 0.5 kg được kéo lệch góc 30 độ so với phương thẳng đứng rồi thả nhẹ không vận tốc đầu." },
  { label: "Ô tô hãm phanh", text: "Một ô tô đang chạy với vận tốc 15 m/s thì hãm phanh chuyển động chậm dần đều với gia tốc 2 m/s². Hãy mô phỏng chuyển động của xe cho đến khi dừng lại." },
  { label: "Ném ngang", text: "Ném một vật từ độ cao 20 m theo phương ngang với vận tốc đầu 15 m/s. Lấy gia tốc trọng trường g = 9.8 m/s², bỏ qua sức cản không khí." },
  { label: "Mạch điện nối tiếp và song song", text: "Một nguồn điện có suất điện động 12 V và điện trở trong 1 Ω được nối với mạch ngoài gồm điện trở R1 = 4 Ω mắc nối tiếp với cụm hai điện trở R2 = 6 Ω và R3 = 3 Ω mắc song song với nhau. Hãy mô phỏng cường độ dòng điện trong mạch chính và dòng điện qua từng điện trở R2, R3." },
];

export default function SimulationInspector({ experiment, explanation, save, library, onSelectExample, readoutsRef }: Readonly<Pick<SimulationWorkspaceModel, "experiment" | "explanation" | "save" | "library"> & { onSelectExample: (description: string) => void; readoutsRef?: Ref<HTMLDivElement> }>) {
  const { simulation, values, validation, updateParameter, resetParameters } = experiment;
  const { intent, revision, setRevision, handleRevision } = explanation;
  const { busy, saveOpen, savedMessage, renderError, currentSimulationId, canManageLearningContent,
    folders, onBusyChange, onFolder, onSaved, onClose, onOpen } = save;
  const [navigation, setNavigation] = useState({
    simulation,
    tab: "experiment" as "experiment" | "understand" | "details" | "readouts",
  });
  const inspectorTab = navigation.simulation !== simulation && simulation ? "experiment" : navigation.tab;
  if (navigation.simulation !== simulation) setNavigation({ simulation, tab: inspectorTab });
  const setInspectorTab = (tab: typeof navigation.tab) => setNavigation({ simulation, tab });
  const recentItems = [...library.items].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .filter((item, index, items) => items.findIndex(value => value.simulationId === item.simulationId) === index).slice(0, 5);
  return (
    <aside className="learn-inspector" aria-label="Thông số và giải thích">
      <div className="learn-inspector-nav">
        <div className="learn-tabs" role="tablist">
          <button
            role="tab"
            type="button"
            aria-selected={!saveOpen && inspectorTab === "experiment"}
            disabled={saveOpen && busy}
            onClick={() => { onClose(); setInspectorTab("experiment"); }}
          >
            Thử nghiệm
          </button>
          {simulation && <button role="tab" type="button" id="simulation-readouts-tab"
            aria-controls="simulation-readouts-panel"
            aria-selected={!saveOpen && inspectorTab === "readouts"}
            disabled={saveOpen && busy}
            onClick={() => { onClose(); setInspectorTab("readouts"); }}>
            Số liệu
          </button>}
          <button
            role="tab"
            type="button"
            aria-selected={!saveOpen && inspectorTab === "understand"}
            disabled={saveOpen && busy}
            onClick={() => { onClose(); setInspectorTab("understand"); }}
          >
            Giải thích
          </button>
          <button
            role="tab"
            type="button"
            aria-selected={!saveOpen && inspectorTab === "details"}
            disabled={saveOpen && busy}
            onClick={() => { onClose(); setInspectorTab("details"); }}
          >
            Chi tiết
          </button>
        </div>
        {simulation && canManageLearningContent && !currentSimulationId && (
          <button type="button" className="learn-save-button"
            disabled={busy || Boolean(renderError) || saveOpen}
            onClick={onOpen}>
            Lưu mô phỏng
          </button>
        )}
      </div>

      <div className="learn-inspector-body">
        <div id="simulation-readouts-panel" role="tabpanel" aria-labelledby="simulation-readouts-tab"
          hidden={!simulation || saveOpen || inspectorTab !== "readouts"}>
          <div ref={readoutsRef} />
        </div>
        {canManageLearningContent && savedMessage && <div className="simulation-actions">
          <span role="status">{savedMessage}</span>
        </div>}
        {simulation && saveOpen && <SaveSimulationPanel simulation={{ ...simulation, formulas: intent?.formulas, explanation: intent?.explanation }} parameters={{ ...values }} folders={folders}
          onBusyChange={onBusyChange}
          onFolder={onFolder}
          onClose={onClose}
          onSaved={onSaved} />}
        {/* Tab 1: Parameters / Controls */}
        {!saveOpen && inspectorTab === "experiment" && (
          <div>
            <div className="learn-section-title">
              <h3>Thông số mô phỏng</h3>
              {simulation?.parameters?.length ? (
                <button
                  type="button"
                  className="simulation-reset"
                  onClick={resetParameters}
                  title="Khôi phục thông số mặc định"
                >
                  <Icon name="reset" /> Mặc định
                </button>
              ) : null}
            </div>

            {simulation?.parameters?.length ? (
              <ParameterControls
                key={simulation.planSignature ?? simulation.sessionId}
                parameters={simulation.parameters}
                values={values}
                onChange={updateParameter}
              />
            ) : (
              <div className="simulation-empty">
                {simulation
                  ? "Mô phỏng này không có biến số điều chỉnh."
                  : (
                    <div>
                      <p>Nhập đề bài để xem và điều chỉnh các thông số mô phỏng tại đây.</p>
                    </div>
                  )}
              </div>
            )}

            {/* Trạng thái kiểm chứng */}
            <div className="simulation-validation" role="status" aria-live="polite" style={{ marginTop: 20 }}>
              <strong>
                {simulation?.simulationSpec?.runtimeKind === "VISUAL"
                  ? "Trạng thái mô hình: "
                  : "Kiểm tra vật lý: "}
                {readVerification(validation?.status).sentence}
              </strong>

              {validation?.verificationScope && <p>Phạm vi xác minh: {validation.verificationScope}</p>}
              {!!validation?.lawChecks?.length && (
                <details>
                  <summary>Đã đối chiếu {validation.lawChecks.filter(check => check.status === "PASSED").length}/{validation.lawChecks.length} định luật</summary>
                  <ul>
                    {[...new Map(validation.lawChecks.map(check => [check.title, check])).values()].map(check => (
                      <li key={check.law}>{check.status === "PASSED" ? "✓" : check.status === "AMBIGUOUS" ? "?" : "✗"} {check.title}{check.status === "AMBIGUOUS" && " (chưa xác định được đại lượng để đối chiếu)"}</li>
                    ))}
                  </ul>
                </details>
              )}
              {!!validation?.uncheckedQuantities?.length && (
                <p>Chưa có định luật đối chiếu: {validation.uncheckedQuantities.map(id =>
                  simulation?.simulationSpec?.model?.quantities.find(quantity => quantity.id === id)?.label || id).join("; ")}</p>
              )}
              {validation?.verificationMethod && (
                <p>Phương pháp xác minh: {validation.verificationMethod}</p>
              )}
              {validation?.absoluteError !== undefined && validation.absoluteError !== null && (
                <p>Sai số lớn nhất: {validation.absoluteError} (tương đối {validation.relativeError ?? "—"})</p>
              )}
              {!!validation?.assumptions?.length && (
                <p>Giả định: {validation.assumptions.join("; ")}</p>
              )}
              {validation?.flags?.map((flag, index) => (
                <p key={index}>{flag}</p>
              ))}
            </div>
            <section className="simulation-shortcuts" aria-labelledby="simulation-examples-heading">
              <h3 id="simulation-examples-heading">Ví dụ mô tả nhanh</h3>
              <p className="simulation-muted">Chọn để điền đề bài, rồi chỉnh sửa theo ý bạn.</p>
              <div className="simulation-shortcut-list">
                {QUICK_DESCRIPTIONS.map(example => <button key={example.label} type="button"
                  className="simulation-shortcut" disabled={busy || Boolean(library.openingId)}
                  onClick={() => onSelectExample(example.text)}>
                  <Icon name="atom" /><span><strong>{example.label}</strong><small>{example.text}</small></span>
                </button>)}
              </div>
            </section>
            {canManageLearningContent && <section className="simulation-shortcuts" aria-labelledby="simulation-recent-heading">
              <h3 id="simulation-recent-heading">Mô phỏng gần đây</h3>
              <p className="simulation-muted">Các mô phỏng bạn đã tạo và lưu gần nhất.</p>
              {library.loading ? <p role="status" className="simulation-muted">Đang tải mô phỏng…</p>
                : library.error ? <div><p role="alert" className="simulation-muted">{library.error}</p>
                  <button type="button" className="simulation-reset" onClick={library.onRetry}>Thử lại</button></div>
                : recentItems.length ? <div className="simulation-shortcut-list">
                  {recentItems.map(item => <button key={item.id} type="button" className="simulation-shortcut"
                    disabled={busy || Boolean(library.openingId)} aria-current={currentSimulationId === item.simulationId ? "true" : undefined}
                    onClick={() => void library.onOpen(item)}>
                    <Icon name="play" /><span><strong>{item.title}</strong>
                      <small>{library.openingId === item.id ? "Đang mở…" : item.topic || "Mở mô phỏng đã lưu"}</small></span>
                  </button>)}
                </div> : <p className="simulation-muted">Chưa có mô phỏng đã lưu. Các bài mới sẽ xuất hiện ở đây sau khi lưu.</p>}
            </section>}
          </div>
        )}

        {/* Tab 2: Physics Explanation */}
        {!saveOpen && inspectorTab === "understand" && (
          <div>
            <h3>Giải thích hiện tượng vật lý</h3>
            {intent?.explanation ? (
              <div style={{ marginTop: 10 }}>
                <p className="simulation-explanation">{intent.explanation}</p>
                <FormulaReview intent={intent} />
              </div>
            ) : (
              <p className="simulation-muted" style={{ padding: "12px 0" }}>
                Phần giải thích hiện tượng và lý thuyết vật lý sẽ xuất hiện ở đây sau khi AI phân tích đề bài.
              </p>
            )}

            {!!intent?.defaults?.length && (
              <div className="simulation-defaults" style={{ marginTop: 14 }}>
                <strong>Giả định mặc định</strong>
                <ul>
                  {intent.defaults.map((item, index) => (
                    <li key={index}>{item}</li>
                  ))}
                </ul>
              </div>
            )}

            {intent && (
              <form className="simulation-revision-form" onSubmit={handleRevision}>
                <label htmlFor="inspector-simulation-change">Cần điều chỉnh gì?</label>
                <textarea
                  id="inspector-simulation-change"
                  rows={2}
                  value={revision}
                  onChange={(event) => setRevision(event.target.value)}
                  placeholder="Ví dụ: Thay đổi vận tốc hoặc góc ném..."
                />
                <button disabled={busy || !revision.trim()}>Cập nhật giải thích</button>
              </form>
            )}
          </div>
        )}

        {/* Tab 3: Details & Scene Inventory */}
        {!saveOpen && inspectorTab === "details" && (
          <div>
            <h3>Chi tiết mô hình</h3>

            {intent?.simulationSpec?.requiredObjects?.length ? (
              <div className="simulation-requirement-review" style={{ marginTop: 12 }}>
                <h4>Thành phần AI nhận diện ({intent.simulationSpec.requiredObjects.length})</h4>
                <ul>
                  {intent.simulationSpec.requiredObjects.map((object, index) => (
                    <li key={index}>
                      <strong>
                        {object.count} × {object.label}
                      </strong>
                      {object.shape && object.shape !== "unspecified" && <span> ({object.shape})</span>}
                    </li>
                  ))}
                </ul>
              </div>
            ) : (
              !simulation && (
                <p className="simulation-muted" style={{ padding: "12px 0" }}>
                  AI sẽ chọn các thành phần trực quan theo ngữ cảnh mô tả.
                </p>
              )
            )}
          </div>
        )}
      </div>

      <footer className="learn-inspector-footer">
        <Icon name="atom" />
        <span>PhysLive · Kết quả được kiểm tra bằng hai phương pháp</span>
      </footer>
    </aside>
  );
}

/** Each declared parameter is edited independently; relationships belong to the backend plan. */
function ParameterControls({ parameters, values, onChange }: Readonly<{
  parameters: SimulationParameter[];
  values: Record<string, number>;
  onChange: (parameter: SimulationParameter, value: number) => void;
}>) {
  return <div className="simulation-controls">{parameters.map(parameter =>
    <SimulationParameterControl key={parameter.name} parameter={parameter}
      value={values[parameter.name] ?? parameter.value}
      onChange={value => onChange(parameter, value)} />
  )}</div>;
}
