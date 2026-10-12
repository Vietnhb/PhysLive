/** One reading of a run's verification status for every place that shows it. */
export type VerificationTone = "ok" | "partial" | "info" | "warn" | "pending";
export type VerificationReading = { tone: VerificationTone; badge: string; sentence: string };

const READINGS: Record<string, VerificationReading> = {
  VERIFIED_BY_LAWS: { tone: "ok", badge: "Đã đối chiếu định luật", sentence: "Mọi đại lượng đã được đối chiếu với định luật của chương trình ✓" },
  VERIFIED_PARTLY: { tone: "partial", badge: "Đã đối chiếu một phần", sentence: "Các định luật áp dụng được đều thoả; còn đại lượng chưa có định luật đối chiếu" },
  ILLUSTRATIVE: { tone: "info", badge: "Minh hoạ", sentence: "Minh hoạ định tính — chương trình không có định luật nào để đối chiếu" },
  FLAGGED: { tone: "warn", badge: "Cần kiểm tra lại", sentence: "Cần kiểm tra lại" },
  PENDING: { tone: "pending", badge: "Đang tính lại…", sentence: "Đang tính lại và kiểm tra…" },
  PAUSED: { tone: "pending", badge: "Đã dừng", sentence: "Đã dừng" },
  /* runs saved before models were written per description */
  VERIFIED_ANALYTICAL: { tone: "ok", badge: "Dữ liệu vật lý đã xác minh", sentence: "Đã xác minh bằng số + công thức chuẩn ✓" },
  VERIFIED_NUMERICAL: { tone: "ok", badge: "Dữ liệu vật lý đã xác minh", sentence: "Đã xác minh bằng tính toán số ✓" },
  VISUAL_ONLY_UNVERIFIED: { tone: "info", badge: "Minh hoạ", sentence: "Chỉ minh họa — chưa xác minh vật lý" },
  UNSUPPORTED: { tone: "warn", badge: "Chưa mô phỏng được", sentence: "Chưa mô phỏng chính xác được tình huống này" },
  UNVERIFIED: { tone: "warn", badge: "Chưa xác minh vật lý", sentence: "Chưa kiểm chứng độc lập" },
};

export const readVerification = (status: string | undefined | null): VerificationReading =>
  (status && READINGS[status]) || { tone: "pending", badge: "Đang sẵn sàng", sentence: "Đang sẵn sàng" };
