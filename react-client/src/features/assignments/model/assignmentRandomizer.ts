/**
 * Assignment Parameter Randomizer (Chống gian lận & Cá nhân hóa đề bài)
 * Sinh ngẫu nhiên bộ biến số đề bài cho từng học sinh dựa trên template tham số
 * và lưu trữ dạng JSONB làm căn cứ chấm điểm tự động.
 */

export type ParameterRange = {
  min: number;
  max: number;
  step?: number;
  unit?: string;
  decimals?: number;
};

export type ParameterTemplate = Record<string, ParameterRange>;

export type AssignedVariable = {
  value: number;
  unit?: string;
  min: number;
  max: number;
};

export type AssignedParameters = {
  studentId: number;
  assignmentId?: string;
  seed: number;
  generatedAt: string;
  variables: Record<string, AssignedVariable>;
};

/**
 * Seeded Pseudo-Random Number Generator (Mulberry32)
 * Đảm bảo tính tất định: cùng studentId + assignmentId luôn sinh cùng bộ số
 * Khác studentId luôn sinh bộ số khác nhau.
 */
function createSeededRandom(seed: number): () => number {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/**
 * Tạo seed số nguyên từ studentId và assignmentId
 */
export function hashStudentAssignmentSeed(studentId: number, assignmentId?: string): number {
  let hash = (studentId * 2654435761) >>> 0;
  if (assignmentId) {
    for (let i = 0; i < assignmentId.length; i++) {
      hash = ((hash << 5) - hash + assignmentId.charCodeAt(i)) >>> 0;
    }
  }
  return hash;
}

/**
 * Làm tròn giá trị theo bước nhảy (step) hoặc số chữ số thập phân
 */
export function roundToStep(value: number, step?: number, decimals?: number): number {
  if (step && step > 0) {
    const inv = 1 / step;
    return Math.round(value * inv) / inv;
  }
  const d = decimals !== undefined ? decimals : 2;
  const factor = Math.pow(10, d);
  return Math.round(value * factor) / factor;
}

/**
 * Sinh bộ biến số ngẫu nhiên cho một học sinh
 */
export function generateStudentParameters(
  studentId: number,
  template: ParameterTemplate,
  assignmentId?: string
): AssignedParameters {
  const seed = hashStudentAssignmentSeed(studentId, assignmentId);
  const rng = createSeededRandom(seed);

  const variables: Record<string, AssignedVariable> = {};

  for (const [key, range] of Object.entries(template)) {
    const min = Math.min(range.min, range.max);
    const max = Math.max(range.min, range.max);
    const rawVal = min + rng() * (max - min);
    const value = roundToStep(rawVal, range.step, range.decimals);

    variables[key] = {
      value,
      unit: range.unit,
      min,
      max,
    };
  }

  return {
    studentId,
    assignmentId,
    seed,
    generatedAt: new Date().toISOString(),
    variables,
  };
}

/**
 * Sinh hàng loạt bộ biến số cho danh sách học sinh (lưu vào submissions)
 */
export function generateBatchStudentParameters(
  studentIds: number[],
  template: ParameterTemplate,
  assignmentId?: string
): Record<number, AssignedParameters> {
  const result: Record<number, AssignedParameters> = {};
  for (const id of studentIds) {
    result[id] = generateStudentParameters(id, template, assignmentId);
  }
  return result;
}

/**
 * Thay thế biến trong đề bài bằng giá trị đã sinh ngẫu nhiên của học sinh
 * Ví dụ: "Vật có khối lượng m = {m} chịu lực kéo F = {F}" -> "Vật có khối lượng m = 2.5 kg chịu lực kéo F = 15 N"
 */
export function interpolateProblemPrompt(promptTemplate: string, params: AssignedParameters): string {
  let output = promptTemplate;
  for (const [key, v] of Object.entries(params.variables)) {
    const formatted = v.unit ? `${v.value} ${v.unit}` : `${v.value}`;
    const pattern = new RegExp(`\\{${key}\\}`, "g");
    output = output.replace(pattern, formatted);
  }
  return output;
}

/**
 * Tính toán đáp án kỳ vọng dựa trên công thức và bộ biến số cá nhân của học sinh
 */
export function calculateExpectedAnswer(
  formula: (vars: Record<string, number>) => number,
  params: AssignedParameters
): number {
  const numericalVars: Record<string, number> = {};
  for (const [key, v] of Object.entries(params.variables)) {
    numericalVars[key] = v.value;
  }
  return formula(numericalVars);
}
