import { useCallback, useEffect, useState } from "react";
import axiosClient from "../../../shared/api/client";
import { apiMessage } from "../../../shared/lib/apiError";

export type PaymentStatus = "PAID" | "PENDING" | "FAILED";

type Payment = {
  id: string;
  schoolName: string;
  managerEmail: string;
  planCode: string;
  purpose: string;
  amountVnd: number;
  status: string;
  createdAt: string;
  paidAt: string | null;
};

const PAYMENT_STATUS_CONFIG: Record<PaymentStatus, { label: string; color: "green" | "amber" | "red"; bg: string }> = {
  PAID: { label: "Đã thanh toán", color: "green", bg: "var(--green-3, #dcfce7)" },
  PENDING: { label: "Chờ thanh toán", color: "amber", bg: "var(--amber-3, #fef3c7)" },
  FAILED: { label: "Thanh toán thất bại", color: "red", bg: "var(--red-3, #fee2e2)" },
};

const normalizePaymentStatus = (status: string): PaymentStatus => {
  const s = (status || "").toUpperCase();
  if (s === "PAID") return "PAID";
  if (s === "FAILED" || s === "EXPIRED") return "FAILED";
  return "PENDING";
};

const paymentPurpose: Record<string, string> = {
  NEW: "Đăng ký mới",
  REGISTRATION: "Đăng ký mới",
  RENEWAL: "Gia hạn giấy phép",
  UPGRADE: "Nâng cấp gói",
};

type RevenueSummary = {
  paidTransactions: number;
  pendingTransactions: number;
  reviewTransactions: number;
  grossPaidVnd: number;
};

export default function AdminPayments() {
  const [items, setItems] = useState<Payment[]>([]);
  const [summary, setSummary] = useState<RevenueSummary | null>(null);
  const [error, setError] = useState("");
  const [activeTab, setActiveTab] = useState<"ALL" | PaymentStatus>("ALL");
  const [query, setQuery] = useState("");

  const load = useCallback(async () => {
    try {
      const [payments, report] = await Promise.all([
        axiosClient.get<Payment[]>("/admin/payments"),
        axiosClient.get<RevenueSummary>("/admin/payment-report"),
      ]);
      setItems(payments.data);
      setSummary(report.data);
      setError("");
    } catch (err) {
      setError(apiMessage(err, "Không thể tải giao dịch."));
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => { void load(); }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const reconcile = async (id: string) => {
    try {
      await axiosClient.post(`/admin/payments/${id}/reconcile`);
      await load();
    } catch (err) {
      setError(apiMessage(err, "Không thể đối soát giao dịch."));
    }
  };

  const filteredItems = items.filter(item => {
    const normalized = normalizePaymentStatus(item.status);
    if (activeTab !== "ALL" && normalized !== activeTab) return false;
    if (query.trim()) {
      const q = query.trim().toLowerCase();
      const match = `${item.schoolName} ${item.managerEmail} ${item.planCode}`.toLowerCase();
      if (!match.includes(q)) return false;
    }
    return true;
  });

  const countByStatus = {
    PAID: items.filter(i => normalizePaymentStatus(i.status) === "PAID").length,
    PENDING: items.filter(i => normalizePaymentStatus(i.status) === "PENDING").length,
    FAILED: items.filter(i => normalizePaymentStatus(i.status) === "FAILED").length,
  };

  return (
    <div className="admin-content">
      {/* 4 Stats Cards */}
      <section className="admin-stats-grid">
        <article className="admin-stat-card">
          <span>Doanh thu đã ghi nhận</span>
          <strong>{(summary?.grossPaidVnd ?? 0).toLocaleString("vi-VN")} ₫</strong>
        </article>
        <article className="admin-stat-card" style={{ borderLeft: "4px solid #10b981" }}>
          <span>Đã thanh toán (PAID)</span>
          <strong style={{ color: "#059669" }}>{countByStatus.PAID || summary?.paidTransactions || 0}</strong>
        </article>
        <article className="admin-stat-card" style={{ borderLeft: "4px solid #f59e0b" }}>
          <span>Chờ thanh toán (PENDING)</span>
          <strong style={{ color: "#d97706" }}>{countByStatus.PENDING || summary?.pendingTransactions || 0}</strong>
        </article>
        <article className="admin-stat-card" style={{ borderLeft: "4px solid #ef4444" }}>
          <span>Thanh toán thất bại (FAILED)</span>
          <strong style={{ color: "#dc2626" }}>{countByStatus.FAILED}</strong>
        </article>
      </section>

      {error && <div className="admin-error-banner">{error}</div>}

      <section className="admin-panel">
        <div className="admin-panel-heading">
          <div>
            <h2>Quản lý giao dịch và thanh toán</h2>
            <p style={{ margin: "4px 0 0", color: "var(--muted)", fontSize: "13px" }}>
              Theo dõi 3 trạng thái thanh toán từ Supabase: Đã thanh toán, Chờ thanh toán và Thất bại.
            </p>
          </div>
          <button type="button" className="admin-secondary-button" onClick={() => void load()}>
            Làm mới
          </button>
        </div>

        {/* Status Filter Tabs & Search */}
        <div style={{ padding: "0 20px 16px", display: "flex", flexWrap: "wrap", gap: "12px", alignItems: "center", justifyContent: "space-between" }}>
          <div style={{ display: "flex", gap: "8px" }} role="tablist" aria-label="Lọc trạng thái thanh toán">
            <button
              type="button"
              className={`directory-tier-pill ${activeTab === "ALL" ? "active" : ""}`}
              onClick={() => setActiveTab("ALL")}
            >
              Tất cả ({items.length})
            </button>
            <button
              type="button"
              className={`directory-tier-pill ${activeTab === "PAID" ? "active" : ""}`}
              onClick={() => setActiveTab("PAID")}
              style={activeTab === "PAID" ? { background: "#059669", borderColor: "#059669" } : {}}
            >
              Đã thanh toán ({countByStatus.PAID})
            </button>
            <button
              type="button"
              className={`directory-tier-pill ${activeTab === "PENDING" ? "active" : ""}`}
              onClick={() => setActiveTab("PENDING")}
              style={activeTab === "PENDING" ? { background: "#d97706", borderColor: "#d97706" } : {}}
            >
              Chờ thanh toán ({countByStatus.PENDING})
            </button>
            <button
              type="button"
              className={`directory-tier-pill ${activeTab === "FAILED" ? "active" : ""}`}
              onClick={() => setActiveTab("FAILED")}
              style={activeTab === "FAILED" ? { background: "#dc2626", borderColor: "#dc2626" } : {}}
            >
              Thanh toán thất bại ({countByStatus.FAILED})
            </button>
          </div>

          <input
            type="search"
            placeholder="Tìm theo tên trường, email quản lý…"
            value={query}
            onChange={e => setQuery(e.target.value)}
            style={{ padding: "8px 12px", borderRadius: "8px", border: "1px solid var(--line)", minWidth: "260px", fontSize: "13px" }}
          />
        </div>

        {/* Transactions Table */}
        <div className="admin-table-scroll">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Trường học</th>
                <th>Người đại diện</th>
                <th>Gói dịch vụ</th>
                <th>Số tiền</th>
                <th>Trạng thái</th>
                <th>Thời gian tạo</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {filteredItems.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: "32px", color: "var(--muted)" }}>
                    Không có giao dịch nào phù hợp với bộ lọc hiện tại.
                  </td>
                </tr>
              ) : (
                filteredItems.map(item => {
                  const normalizedStatus = normalizePaymentStatus(item.status);
                  const config = PAYMENT_STATUS_CONFIG[normalizedStatus];

                  return (
                    <tr key={item.id}>
                      <td><strong>{item.schoolName}</strong></td>
                      <td>{item.managerEmail}</td>
                      <td>
                        <strong>{item.planCode}</strong>
                        <small style={{ display: "block", color: "var(--muted)" }}>
                          {paymentPurpose[item.purpose] ?? item.purpose}
                        </small>
                      </td>
                      <td>
                        <strong style={{ color: "var(--ink)" }}>
                          {item.amountVnd.toLocaleString("vi-VN")} ₫
                        </strong>
                      </td>
                      <td>
                        <span style={{
                          display: "inline-flex",
                          alignItems: "center",
                          gap: "4px",
                          padding: "3px 10px",
                          borderRadius: "999px",
                          fontSize: "12px",
                          fontWeight: 600,
                          background: config.bg,
                          color: config.color === "green" ? "#065f46" : (config.color === "amber" ? "#92400e" : "#991b1b")
                        }}>
                          {config.label}
                        </span>
                      </td>
                      <td>{new Date(item.createdAt).toLocaleString("vi-VN")}</td>
                      <td>
                        {normalizedStatus === "PENDING" && (
                          <button
                            type="button"
                            className="admin-inline-button"
                            onClick={() => void reconcile(item.id)}
                          >
                            Đối soát
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}