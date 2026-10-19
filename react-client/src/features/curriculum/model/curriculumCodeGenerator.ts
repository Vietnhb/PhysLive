/**
 * Curriculum ID Generation Logic (Chuẩn hóa & Sinh mã chương trình học)
 * Quy tắc phân cấp:
 * - Root: K
 * - Môn / Học phần: K01, K02, ...
 * - Khối lớp: K01-10, K01-11, K01-12
 * - Bài học chi tiết: K01-10-01, K01-10-02
 */

export type CurriculumLevelType = "ROOT" | "TOPIC" | "GRADE" | "LESSON";

export type ParsedCurriculumCode = {
  isValid: boolean;
  code: string;
  level: CurriculumLevelType;
  root: string;
  topicNumber?: string;
  gradeLevel?: string;
  lessonNumber?: string;
};

/**
 * Định dạng mã Môn / Học phần (K01, K02, ...)
 */
export function formatTopicCode(index: number): string {
  const num = Math.max(1, Math.floor(index));
  return `K${String(num).padStart(2, "0")}`;
}

/**
 * Rút trích số khối lớp từ chuỗi (ví dụ: "Lớp 10" -> "10", "Khối 11" -> "11", 12 -> "12")
 */
export function extractGradeNumber(input: string | number): string {
  if (typeof input === "number") {
    return String(input).padStart(2, "0");
  }
  const match = input.match(/\b(10|11|12|[1-9])\b/);
  if (match) {
    return match[1].padStart(2, "0");
  }
  const numMatch = input.match(/\d+/);
  if (numMatch) {
    return numMatch[0].padStart(2, "0");
  }
  return "10";
}

/**
 * Định dạng mã Khối lớp (K01-10, K01-11, ...)
 */
export function formatGradeCode(topicCode: string, gradeInput: string | number): string {
  const normalizedTopic = topicCode.toUpperCase().startsWith("K") ? topicCode.toUpperCase() : `K${topicCode}`;
  const gradeNum = extractGradeNumber(gradeInput);
  return `${normalizedTopic}-${gradeNum}`;
}

/**
 * Định dạng mã Bài học chi tiết (K01-10-01, K01-10-02, ...)
 */
export function formatLessonCode(gradeCode: string, lessonIndex: number): string {
  const normalizedGrade = gradeCode.toUpperCase();
  const lessonNum = String(Math.max(1, Math.floor(lessonIndex))).padStart(2, "0");
  return `${normalizedGrade}-${lessonNum}`;
}

/**
 * Sinh mã định danh phân cấp tổng quát
 */
export function generateCurriculumCode(params: {
  kind: "topic" | "module" | "level" | "lesson";
  topicIndex?: number;
  gradeInput?: string | number;
  lessonIndex?: number;
  parentCode?: string;
}): string {
  if (params.kind === "topic") {
    return formatTopicCode(params.topicIndex ?? 1);
  }
  if (params.kind === "module" || params.kind === "level") {
    const parent = params.parentCode ?? "K01";
    return formatGradeCode(parent, params.gradeInput ?? 10);
  }
  if (params.kind === "lesson") {
    const parent = params.parentCode ?? "K01-10";
    return formatLessonCode(parent, params.lessonIndex ?? 1);
  }
  return "K";
}

/**
 * Kiểm tra tính hợp lệ của mã chương trình học theo biểu thức chính quy
 * Khớp: K, K01, K01-10, K01-10-01
 */
export const CURRICULUM_CODE_REGEX = /^K(?:(\d{2})(?:-(\d{2})(?:-(\d{2}))?)?)?$/;

export function isValidCurriculumCode(code: string): boolean {
  if (!code) return false;
  return CURRICULUM_CODE_REGEX.test(code.trim().toUpperCase());
}

/**
 * Phân tích cú pháp mã chương trình học ra các thành phần
 */
export function parseCurriculumCode(code: string): ParsedCurriculumCode {
  const clean = (code || "").trim().toUpperCase();
  const match = clean.match(CURRICULUM_CODE_REGEX);

  if (!match) {
    return { isValid: false, code: clean, level: "ROOT", root: "K" };
  }

  const topicNumber = match[1];
  const gradeLevel = match[2];
  const lessonNumber = match[3];

  let level: CurriculumLevelType = "ROOT";
  if (lessonNumber) level = "LESSON";
  else if (gradeLevel) level = "GRADE";
  else if (topicNumber) level = "TOPIC";

  return {
    isValid: true,
    code: clean,
    level,
    root: "K",
    topicNumber,
    gradeLevel,
    lessonNumber,
  };
}

/**
 * Trả về nhãn tiếng Việt tương ứng với cấp bậc của mã
 */
export function getCurriculumLevelName(code: string): string {
  const parsed = parseCurriculumCode(code);
  switch (parsed.level) {
    case "TOPIC": return "Môn / Học phần";
    case "GRADE": return "Khối lớp";
    case "LESSON": return "Bài học chi tiết";
    default: return "Gốc chương trình (K)";
  }
}
