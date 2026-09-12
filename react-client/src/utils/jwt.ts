export const isTokenExpired = (token: string): boolean => {
  if (!token) return true;
  // Hỗ trợ token mock thử nghiệm của Hùng
  if (token.startsWith("mock_")) {
    return false;
  }
  try {
    const parts = token.split(".");
    if (parts.length < 2) return true;
    const payload = JSON.parse(atob(parts[1]));
    const now = Math.floor(Date.now() / 1000);
    // Chuẩn JWT là payload.exp
    if (typeof payload.exp === "number") {
      return payload.exp <= now;
    }
    return false;
  } catch {
    return true;
  }
};
