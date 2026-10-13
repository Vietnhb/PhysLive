import axiosClient from "../../../api/axios";
import type { SolverTimeline, PixiVisualProgram } from "../model/svgScene";
import type { TeacherPresentation } from "../model/teacherPresentation";

export type SimulationSourceMode = "TEXT" | "LATEX" | "IMAGE";
export type RecognitionResult = {
  sessionId: string; stage: "RECOGNITION" | "RECOGNITION_FAILED";
  recognizedText: string; displayText: string; sourceMode: SimulationSourceMode;
  confidence: number | null; message?: string;
};
export type SimulationParameter = {
  name: string; label?: string; value: number; unit?: string; min?: number; max?: number; step?: number;
};
/** Which value feeds each input of the law (built by the backend from the signed plan). */
export type SimulationFormulaBinding = {
  quantity: string; label: string; unit: string; source: "PARAMETER" | "FIXED" | "DEFAULT" | "OUTPUT";
  parameter?: string; parameterLabel?: string; value?: number | null;
  /** source OUTPUT: the result of another participant's law feeds this input. */
  participant?: string; participantLabel?: string; output?: string; outputLabel?: string;
};
export type SimulationFormula = {
  modelId: string; label?: string; capabilityId: string; canonical: string[]; derived?: string[]; assumptions?: string[];
  bindings?: SimulationFormulaBinding[];
};
export type SimulationSpec = {
  durationSeconds: number; durationParameter?: string; parameters: SimulationParameter[];
  physicsCoverage: "COMPLETE" | "PARTIAL" | "NONE";
  /** The physics written for this description: its objects and the quantities the backend computes and checks. */
  objects?: Array<{ id: string; label?: string; count?: number }>;
  model?: { constants?: unknown[]; quantities: Array<{ id: string; label?: string; unit?: string; role?: string; of?: string }>;
    until?: unknown; duration?: unknown; assumptions?: string[] };
  /** Derived by the backend from the model for the page (one entry per object); plans saved earlier bind library laws here. */
  physicsModels: Array<{ id: string; label?: string; capabilityId: string; inputs: Record<string, string | number>;
    /** Stages: the participant whose end starts this one, and the condition that ends this one. */
    after?: string; until?: { field: string; direction: "rising" | "falling"; value: number | string } }>;
  /** Further topics whose laws the signed plan uses (set by the backend). */
  relatedSchemas?: Array<{ schemaId: string; schemaVersion: string; topic?: string }>;
  requiredObjects?: Array<{ count?: number; label?: string; shape?: string }>;
  /** The values the learner watches: a participant output, the object it belongs to and its name in the user's words. */
  observables?: Array<{ field: string; object?: string; label?: string }>;
  runtimeKind?: string; visualProgram?: PixiVisualProgram; solverTimeline?: SolverTimeline;
  [key: string]: unknown;
};
export type IntentResult = {
  sessionId: string; stage: "CLARIFY" | "EXPLAIN" | "UNSUPPORTED";
  description: string; schemaId: string; schemaVersion: string; planSignature?: string;
  question?: string; choices?: string[]; explanation?: string; defaults?: string[]; message?: string;
  simulationSpec?: SimulationSpec; formulas?: SimulationFormula[]; validation?: SimulationValidation;
};
export type SimulationValidation = {
  status: "PENDING" | "FLAGGED" | "PAUSED" | "UNVERIFIED" | "UNDERSTOOD"
    | "NEEDS_CLARIFICATION" | "VERIFIED_BY_LAWS" | "VERIFIED_PARTLY" | "ILLUSTRATIVE"
    | "VERIFIED_ANALYTICAL" | "VERIFIED_NUMERICAL" | "ASSUMPTION_REVIEW" | "VISUAL_ONLY_UNVERIFIED" | "UNSUPPORTED";
  flags: string[]; metrics?: Record<string, number>;
  /** Every curriculum law that applied to the run, and whether it held. */
  lawChecks?: Array<{ law: string; title: string; curriculum?: string; status: "PASSED" | "FAILED" | "AMBIGUOUS"; scope?: Record<string, string> }>;
  /** Ids of the model's quantities that no law checked. */
  uncheckedQuantities?: string[];
  executionMethod?: string; solverMethod?: string; verificationMethod?: string; verificationScope?: string;
  formulaSource?: string; capabilityId?: string; topicVersion?: string; solverVersion?: string;
  referenceSolverVersion?: string; absoluteError?: number | null; relativeError?: number | null;
  convergenceEvidence?: string[]; invariantResults?: Record<string, boolean>; assumptions?: string[];
  benchmarkSummary?: string | null;
};
export type GeneratedSimulationResult = {
  presentation?: TeacherPresentation;
  savedParameters?: Record<string, number>;
  formulas?: SimulationFormula[];
  explanation?: string;
  sessionId: string; stage: "SIMULATION"; code?: string; schemaId: string; schemaVersion: string;
  description: string; planSignature: string;
  parameters: SimulationParameter[]; validation: SimulationValidation; simulationSpec: SimulationSpec;
};
/** True when a stored run carries the scene its author built (illustration and solver timeline). */
export function hasSavedScene(value: unknown): value is GeneratedSimulationResult {
  const spec = (value as GeneratedSimulationResult | null | undefined)?.simulationSpec;
  return Boolean(spec?.visualProgram && spec.solverTimeline);
}
export const reportSimulationRenderFailure = (simulation: GeneratedSimulationResult, message: string,
  parameters?: Record<string, number>, timeline?: SolverTimeline) =>
  axiosClient.post<void>("/simulation/render-diagnostics", { simulation, message, ...(parameters ? { runtime: { parameters, timeline } } : {}) });
export const understandSimulationText = (description: string) =>
  axiosClient.post<IntentResult>("/simulation/understand", { description }).then(response => response.data);

export const recognizeSimulationImage = (file: File, text?: string) => {
  const body = new FormData(); body.append("file", file);
  if (text?.trim()) body.append("text", text.trim());
  return axiosClient.post<RecognitionResult>("/simulation/understand", body).then(response => response.data);
};
export const confirmSimulationInput = (sessionId: string, recognizedText: string, correctedText?: string) =>
  axiosClient.post<RecognitionResult | IntentResult>("/simulation/understand", {
    sessionId, recognizedText, ...(correctedText !== undefined ? { correctedText } : {}),
  }).then(response => response.data);

export const reviseSimulationIntent = (intent: IntentResult, text: string) =>
  axiosClient.post<IntentResult>("/simulation/revise", { intent, text }).then(response => response.data);

export const confirmSimulationExplanation = (intent: IntentResult, renderDiagnostics?: { code: string; message: string }) =>
  axiosClient.post<GeneratedSimulationResult>("/simulation/generate", {
    ...intent, ...(renderDiagnostics ? { renderDiagnostics } : {}),
  }).then(response => response.data);

export const recomputeSimulation = (simulation: GeneratedSimulationResult, parameters: Record<string, number>, signal: AbortSignal) =>
  axiosClient.post<{ solverTimeline: SolverTimeline; validation: SimulationValidation; presentation: TeacherPresentation }>("/simulation/compute", {
    schemaId: simulation.schemaId, schemaVersion: simulation.schemaVersion,
    description: simulation.description, planSignature: simulation.planSignature,
    simulationSpec: simulation.simulationSpec, parameters,
  }, { signal }).then(response => response.data);
