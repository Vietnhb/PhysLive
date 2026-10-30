export const SUBSCRIPTION_STATUS = {
  ACTIVE: { label: "Đang hiệu lực", color: "green" },
  UPCOMING: { label: "Chưa đến hạn", color: "blue" },
  EXPIRED: { label: "Đã hết hạn", color: "orange" },
  UNLICENSED: { label: "Chưa có thời hạn", color: "gray" },
  SUSPENDED: { label: "Trường tạm khóa", color: "red" },
  UNKNOWN: { label: "Chưa xác minh", color: "gray" },
} as const;
