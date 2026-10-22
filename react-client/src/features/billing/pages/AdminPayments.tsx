import { useCallback, useEffect, useMemo, useState } from "react";
import { Button as ThemeButton, Select, TextField } from "@radix-ui/themes";
import { apiMessage } from "../../../shared/lib/apiError";
import LearningIcon from "../../../shared/ui/LearningIcon";
import { ErrorNotice } from "../../../shared/ui/ManagementUI";
import {
  adminPaymentAnalytics,
  adminPayments,
  reconcileAdminPayment,
  type AdminPayment,
  type PaymentAnalytics,
  type PaymentPeriod,
  type PaymentPeriodFilter,
  type PaymentStatus,
} from "../api/billingApi";
import styles from "./AdminPayments.module.css";

const STATUS_CONFIG: Record<PaymentStatus, { label: string; className: string; color: string }> = {
  PAID: { label: "Đã thanh toán", className: styles.paid, color: "#059669" },
  PENDING: { label: "Chờ thanh toán", className: styles.pending, color: "#d97706" },
  REQUIRES_REVIEW: { label: "Cần đối soát", className: styles.review, color: "#7c3aed" },
  FAILED: { label: "Thanh toán thất bại", className: styles.failed, color: "#dc2626" },
};

const PURPOSE_LABELS: Record<string, string> = {
  NEW: "Đăng ký mới",
  REGISTRATION: "Đăng ký mới",
  RENEWAL: "Gia hạn giấy phép",
  UPGRADE: "Nâng cấp gói",
};

const PAGE_SIZES = [10, 25, 50] as const;
const MONTHS = Array.from({ length: 12 }, (_, index) => index + 1);
const QUARTERS = [1, 2, 3, 4] as const;

function normalizeStatus(status: string): PaymentStatus {
  const normalized = (status || "").toUpperCase();
  if (normalized === "PAID" || normalized === "PENDING" || normalized === "REQUIRES_REVIEW") return normalized;
  return "FAILED";
}

function formatVnd(value: number) {
  return `${value.toLocaleString("vi-VN")} ₫`;
}

function compactVnd(value: number) {
  if (value >= 1_000_000_000) return `${(value / 1_000_000_000).toLocaleString("vi-VN", { maximumFractionDigits: 1 })} tỷ`;
  if (value >= 1_000_000) return `${(value / 1_000_000).toLocaleString("vi-VN", { maximumFractionDigits: 1 })} tr`;
  if (value >= 1_000) return `${(value / 1_000).toLocaleString("vi-VN", { maximumFractionDigits: 0 })} nghìn`;
  return value.toLocaleString("vi-VN");
}

function periodDescription(filter: PaymentPeriodFilter) {
  if (filter.period === "MONTH") return `Tháng ${filter.month}/${filter.year}`;
  if (filter.period === "QUARTER") return `Quý ${filter.quarter}/${filter.year}`;
  return `Năm ${filter.year}`;
}

function DashboardSkeleton() {
  return <div className={styles.skeletonGrid} role="status" aria-label="Đang tải dữ liệu thanh toán">
    {Array.from({ length: 5 }, (_, index) => <div key={index} className={styles.skeletonCard} />)}
    <div className={`${styles.skeletonCard} ${styles.skeletonWide}`} />
    <div className={`${styles.skeletonCard} ${styles.skeletonHalf}`} />
    <div className={`${styles.skeletonCard} ${styles.skeletonHalf}`} />
  </div>;
}

function RevenueTrendChart({ data }: Readonly<{ data: PaymentAnalytics["trend"] }>) {
  const [active, setActive] = useState<number | null>(null);
  const hasRevenue = data.some(point => point.revenueVnd > 0);
  if (!hasRevenue) return <div className={styles.chartEmpty}><LearningIcon name="chart" /><strong>Chưa có doanh thu trong kỳ</strong><span>Biểu đồ sẽ xuất hiện khi có giao dịch đã thanh toán.</span></div>;

  const width = 960;
  const height = 280;
  const margin = { top: 18, right: 24, bottom: 42, left: 74 };
  const plotWidth = width - margin.left - margin.right;
  const plotHeight = height - margin.top - margin.bottom;
  const max = Math.max(...data.map(point => point.revenueVnd), 1);
  const x = (index: number) => margin.left + (data.length === 1 ? plotWidth / 2 : (index / (data.length - 1)) * plotWidth);
  const y = (value: number) => margin.top + plotHeight - (value / max) * plotHeight;
  const points = data.map((point, index) => `${x(index)},${y(point.revenueVnd)}`).join(" ");
  const labelStep = Math.max(1, Math.ceil(data.length / 8));
  const activePoint = active == null ? null : data[active];

  return <div className={styles.trendChart}>
    <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Biểu đồ xu hướng doanh thu">
      {[0, 0.25, 0.5, 0.75, 1].map(ratio => {
        const gridY = margin.top + plotHeight - ratio * plotHeight;
        return <g key={ratio}><line x1={margin.left} x2={width - margin.right} y1={gridY} y2={gridY} className={styles.gridLine} /><text x={margin.left - 12} y={gridY + 4} textAnchor="end" className={styles.axisLabel}>{compactVnd(max * ratio)}</text></g>;
      })}
      <polyline points={points} className={styles.revenueAreaLine} />
      {data.map((point, index) => <g key={point.key}>
        {(index % labelStep === 0 || index === data.length - 1) && <text x={x(index)} y={height - 12} textAnchor="middle" className={styles.axisLabel}>{point.label}</text>}
        <circle cx={x(index)} cy={y(point.revenueVnd)} r={active === index ? 7 : 4} className={styles.revenuePoint} tabIndex={0} aria-label={`${point.label}: ${formatVnd(point.revenueVnd)}`} onMouseEnter={() => setActive(index)} onMouseLeave={() => setActive(null)} onFocus={() => setActive(index)} onBlur={() => setActive(null)} />
      </g>)}
    </svg>
    {activePoint && <div className={styles.chartTooltip} style={{ left: `${(x(active ?? 0) / width) * 100}%`, top: `${(y(activePoint.revenueVnd) / height) * 100}%` }}><strong>{activePoint.label}</strong><span>{formatVnd(activePoint.revenueVnd)}</span><small>{activePoint.transactions} giao dịch thành công</small></div>}
  </div>;
}

function PackageRevenueChart({ data, total }: Readonly<{ data: PaymentAnalytics["packages"]; total: number }>) {
  if (data.length === 0) return <div className={styles.chartEmpty}><LearningIcon name="chart" /><strong>Chưa có doanh thu theo gói</strong><span>Không có giao dịch thành công trong kỳ.</span></div>;
  const max = Math.max(...data.map(item => item.revenueVnd), 1);
  return <div className={styles.packageList}>{data.map(item => <div key={item.planCode} className={styles.packageRow}>
    <div><strong>{item.planCode}</strong><span>{item.transactions} giao dịch · {total === 0 ? 0 : Math.round((item.revenueVnd / total) * 100)}%</span></div>
    <div className={styles.packageValue}>{formatVnd(item.revenueVnd)}</div>
    <div className={styles.barTrack}><span style={{ width: `${Math.max(3, (item.revenueVnd / max) * 100)}%` }} /></div>
  </div>)}</div>;
}

function StatusDonut({ data }: Readonly<{ data: PaymentAnalytics["statuses"] }>) {
  const total = data.reduce((sum, item) => sum + item.transactions, 0);
  if (total === 0) return <div className={styles.chartEmpty}><LearningIcon name="activity" /><strong>Chưa có giao dịch trong kỳ</strong><span>Phân bổ trạng thái sẽ xuất hiện tại đây.</span></div>;
  const percentages = data.map(item => (item.transactions / total) * 100);
  const segments = data.map((item, index) => {
    const start = percentages.slice(0, index).reduce((sum, value) => sum + value, 0);
    const end = start + percentages[index];
    return `${STATUS_CONFIG[item.status].color} ${start}% ${end}%`;
  }).join(", ");
  return <div className={styles.donutLayout}>
    <div className={styles.donut} style={{ background: `conic-gradient(${segments})` }} role="img" aria-label={`Phân bổ ${total} giao dịch`}><div><strong>{total}</strong><span>Giao dịch</span></div></div>
    <div className={styles.legend}>{data.map(item => <div key={item.status}><i style={{ background: STATUS_CONFIG[item.status].color }} /><span>{STATUS_CONFIG[item.status].label}</span><strong>{item.transactions}</strong><small>{Math.round((item.transactions / total) * 100)}%</small></div>)}</div>
  </div>;
}

export default function AdminPayments() {
  const [initialNow] = useState(() => new Date());
  const [period, setPeriod] = useState<PaymentPeriod>("YEAR");
  const [year, setYear] = useState(initialNow.getFullYear());
  const [month, setMonth] = useState(initialNow.getMonth() + 1);
  const [quarter, setQuarter] = useState(Math.floor(initialNow.getMonth() / 3) + 1);
  const [items, setItems] = useState<AdminPayment[]>([]);
  const [analytics, setAnalytics] = useState<PaymentAnalytics | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [reconcilingId, setReconcilingId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"ALL" | PaymentStatus>("ALL");
  const [query, setQuery] = useState("");
  const [pageSize, setPageSize] = useState<number>(10);
  const [page, setPage] = useState(1);
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || "Asia/Ho_Chi_Minh";
  const filter = useMemo<PaymentPeriodFilter>(() => ({ period, year, month, quarter, timezone }), [month, period, quarter, timezone, year]);
  const years = useMemo(() => Array.from({ length: 11 }, (_, index) => initialNow.getFullYear() - index), [initialNow]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [payments, report] = await Promise.all([adminPayments(filter), adminPaymentAnalytics(filter)]);
      setItems(payments);
      setAnalytics(report);
      setError("");
    } catch (err) {
      setError(apiMessage(err, "Không thể tải dữ liệu thanh toán."));
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { setPage(1); setActiveTab("ALL"); }, [period, year, month, quarter]);

  const reconcile = async (id: string) => {
    setReconcilingId(id);
    try {
      await reconcileAdminPayment(id);
      await load();
    } catch (err) {
      setError(apiMessage(err, "Không thể đối soát giao dịch."));
    } finally {
      setReconcilingId(null);
    }
  };

  const statusCount = useCallback((status: PaymentStatus) => analytics?.statuses.find(item => item.status === status)?.transactions ?? 0, [analytics]);
  const filteredItems = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return items.filter(item => {
      if (activeTab !== "ALL" && normalizeStatus(item.status) !== activeTab) return false;
      return !normalizedQuery || `${item.schoolName} ${item.managerEmail} ${item.planCode} ${item.id}`.toLowerCase().includes(normalizedQuery);
    });
  }, [activeTab, items, query]);
  const pageCount = Math.max(1, Math.ceil(filteredItems.length / pageSize));
  const currentPage = Math.min(page, pageCount);
  const pageStart = (currentPage - 1) * pageSize;
  const visibleItems = filteredItems.slice(pageStart, pageStart + pageSize);

  const kpis = analytics?.kpis;
  return <div className="admin-content">
    <header className={styles.pageHeader}>
      <div><h1>Thanh toán &amp; Doanh thu</h1><p>Theo dõi doanh thu, giao dịch và các khoản cần đối soát.</p></div>
      <div className={styles.headerActions}>
        <div className={styles.periodFilters} aria-label="Bộ lọc thời gian">
          <Select.Root value={period} onValueChange={value => setPeriod(value as PaymentPeriod)}><Select.Trigger aria-label="Loại kỳ thống kê" /><Select.Content><Select.Item value="MONTH">Theo tháng</Select.Item><Select.Item value="QUARTER">Theo quý</Select.Item><Select.Item value="YEAR">Theo năm</Select.Item></Select.Content></Select.Root>
          {period === "MONTH" && <Select.Root value={String(month)} onValueChange={value => setMonth(Number(value))}><Select.Trigger aria-label="Chọn tháng" /><Select.Content>{MONTHS.map(value => <Select.Item key={value} value={String(value)}>Tháng {value}</Select.Item>)}</Select.Content></Select.Root>}
          {period === "QUARTER" && <Select.Root value={String(quarter)} onValueChange={value => setQuarter(Number(value))}><Select.Trigger aria-label="Chọn quý" /><Select.Content>{QUARTERS.map(value => <Select.Item key={value} value={String(value)}>Quý {value}</Select.Item>)}</Select.Content></Select.Root>}
          <Select.Root value={String(year)} onValueChange={value => setYear(Number(value))}><Select.Trigger aria-label="Chọn năm" /><Select.Content>{years.map(value => <Select.Item key={value} value={String(value)}>{value}</Select.Item>)}</Select.Content></Select.Root>
        </div>
        <ThemeButton type="button" variant="surface" size="2" disabled={loading} onClick={() => void load()}><LearningIcon name="refresh" />Làm mới</ThemeButton>
      </div>
    </header>

    {error && <ErrorNotice error={error} onRetry={() => void load()} />}
    {loading && !analytics ? <DashboardSkeleton /> : analytics && <>
      <section className={styles.kpiGrid} aria-label={`Tổng quan ${periodDescription(filter)}`}>
        <article className={styles.kpiCard}><div className={`${styles.kpiIcon} ${styles.blue}`}><LearningIcon name="chart" /></div><span>Doanh thu đã thanh toán</span><strong title={formatVnd(kpis?.grossPaidVnd ?? 0)}>{formatVnd(kpis?.grossPaidVnd ?? 0)}</strong><small>{periodDescription(filter)}</small></article>
        <article className={styles.kpiCard}><div className={`${styles.kpiIcon} ${styles.green}`}><LearningIcon name="check" /></div><span>Đã thanh toán</span><strong>{kpis?.paidTransactions ?? 0}</strong><small>Giao dịch thành công</small></article>
        <article className={styles.kpiCard}><div className={`${styles.kpiIcon} ${styles.amber}`}><LearningIcon name="activity" /></div><span>Chờ thanh toán</span><strong>{kpis?.pendingTransactions ?? 0}</strong><small>Đang chờ kết quả</small></article>
        <article className={styles.kpiCard}><div className={`${styles.kpiIcon} ${styles.purple}`}><LearningIcon name="shield" /></div><span>Cần đối soát</span><strong>{kpis?.reviewTransactions ?? 0}</strong><small>Cần kiểm tra thêm</small></article>
        <article className={styles.kpiCard}><div className={`${styles.kpiIcon} ${styles.red}`}><LearningIcon name="close" /></div><span>Thanh toán thất bại</span><strong>{kpis?.failedTransactions ?? 0}</strong><small>Không ghi nhận doanh thu</small></article>
      </section>

      <section className={`${styles.panel} ${styles.trendPanel}`}><div className={styles.panelHeading}><div><h2>Xu hướng doanh thu</h2><p>Doanh thu từ giao dịch đã thanh toán · {periodDescription(filter)}</p></div><span className={styles.periodBadge}>{analytics.period === "MONTH" ? "Theo ngày" : "Theo tháng"}</span></div><RevenueTrendChart data={analytics.trend} /></section>

      <div className={styles.insightGrid}>
        <section className={styles.panel}><div className={styles.panelHeading}><div><h2>Doanh thu theo gói</h2><p>Sắp xếp theo doanh thu giảm dần</p></div></div><PackageRevenueChart data={analytics.packages} total={analytics.kpis.grossPaidVnd} /></section>
        <section className={styles.panel}><div className={styles.panelHeading}><div><h2>Trạng thái giao dịch</h2><p>Phân bổ trong cùng phạm vi thời gian</p></div></div><StatusDonut data={analytics.statuses} /></section>
      </div>

      <section className={`${styles.panel} ${styles.transactionsPanel}`}>
        <div className={styles.panelHeading}><div><h2>Danh sách giao dịch</h2><p>{periodDescription(filter)} · Doanh thu chỉ tính giao dịch đã thanh toán</p></div><span className={styles.resultCount}>{analytics.kpis.totalTransactions} giao dịch</span></div>
        <div className={styles.transactionFilters}>
          <div className={styles.tabs} role="tablist" aria-label="Lọc trạng thái thanh toán">
            <button type="button" role="tab" aria-selected={activeTab === "ALL"} onClick={() => { setActiveTab("ALL"); setPage(1); }}>Tất cả <span>{analytics.kpis.totalTransactions}</span></button>
            {(["PAID", "PENDING", "REQUIRES_REVIEW", "FAILED"] as PaymentStatus[]).map(status => <button key={status} type="button" role="tab" aria-selected={activeTab === status} data-tone={status.toLowerCase()} onClick={() => { setActiveTab(status); setPage(1); }}>{STATUS_CONFIG[status].label} <span>{statusCount(status)}</span></button>)}
          </div>
          <TextField.Root className={styles.search} type="search" size="3" aria-label="Tìm giao dịch" placeholder="Tìm trường, email, gói hoặc mã giao dịch…" value={query} onChange={event => { setQuery(event.target.value); setPage(1); }}><TextField.Slot><LearningIcon name="search" /></TextField.Slot></TextField.Root>
        </div>

        {loading ? <div className={styles.tableSkeleton} role="status">Đang cập nhật danh sách giao dịch…</div> : <>
          <div className={`${styles.desktopTable} admin-table-scroll`}><table className={`${styles.table} admin-table`}><thead><tr><th>Trường học</th><th>Người đại diện</th><th>Gói dịch vụ</th><th className={styles.amountColumn}>Số tiền</th><th>Trạng thái</th><th>Thời gian</th><th>Thao tác</th></tr></thead><tbody>
            {visibleItems.length === 0 ? <tr><td colSpan={7} className="admin-empty-table">Không có giao dịch phù hợp với bộ lọc hiện tại.</td></tr> : visibleItems.map(item => {
              const status = normalizeStatus(item.status); const config = STATUS_CONFIG[status]; const relevantTime = status === "PAID" && item.paidAt ? item.paidAt : item.createdAt;
              return <tr key={item.id}><td><strong>{item.schoolName || "Chưa xác định"}</strong><small className={styles.secondary}>Mã: {item.id.slice(0, 8).toUpperCase()}</small></td><td>{item.managerEmail || "—"}</td><td><strong>{item.planCode}</strong><small className={styles.secondary}>{PURPOSE_LABELS[item.purpose] ?? item.purpose}</small></td><td className={styles.amountColumn}><strong>{formatVnd(item.amountVnd)}</strong></td><td><span className={`${styles.status} ${config.className}`}>{config.label}</span></td><td><span>{new Date(relevantTime).toLocaleString("vi-VN")}</span><small className={styles.secondary}>{status === "PAID" && item.paidAt ? "Đã thanh toán" : "Khởi tạo"}</small></td><td>{status === "PENDING" ? <ThemeButton type="button" size="2" variant="soft" color="gray" loading={reconcilingId === item.id} disabled={reconcilingId !== null} onClick={() => void reconcile(item.id)}>Đối soát</ThemeButton> : <span className={styles.noAction}>—</span>}</td></tr>;
            })}
          </tbody></table></div>

          <div className={styles.mobileList}>{visibleItems.length === 0 ? <div className={styles.mobileEmpty}>Không có giao dịch phù hợp.</div> : visibleItems.map(item => { const status = normalizeStatus(item.status); const config = STATUS_CONFIG[status]; const relevantTime = status === "PAID" && item.paidAt ? item.paidAt : item.createdAt; return <article key={item.id} className={styles.mobilePayment}><div className={styles.mobilePaymentHeading}><div><strong>{item.schoolName || "Chưa xác định"}</strong><span>{item.managerEmail || "—"}</span></div><span className={`${styles.status} ${config.className}`}>{config.label}</span></div><dl><div><dt>Gói</dt><dd>{item.planCode} · {PURPOSE_LABELS[item.purpose] ?? item.purpose}</dd></div><div><dt>Số tiền</dt><dd>{formatVnd(item.amountVnd)}</dd></div><div><dt>Thời gian</dt><dd>{new Date(relevantTime).toLocaleString("vi-VN")}</dd></div><div><dt>Mã</dt><dd>{item.id.slice(0, 8).toUpperCase()}</dd></div></dl>{status === "PENDING" && <ThemeButton type="button" size="2" variant="soft" color="gray" loading={reconcilingId === item.id} disabled={reconcilingId !== null} onClick={() => void reconcile(item.id)}>Đối soát giao dịch</ThemeButton>}</article>; })}</div>

          <div className="admin-table-pagination"><span aria-live="polite">{filteredItems.length === 0 ? "0 giao dịch" : `Hiển thị ${pageStart + 1}–${Math.min(pageStart + pageSize, filteredItems.length)} / ${filteredItems.length} giao dịch`}</span><div><label>Số dòng/trang<Select.Root size="2" value={String(pageSize)} onValueChange={value => { setPageSize(Number(value)); setPage(1); }}><Select.Trigger aria-label="Số giao dịch trên mỗi trang" /><Select.Content>{PAGE_SIZES.map(size => <Select.Item key={size} value={String(size)}>{size}</Select.Item>)}</Select.Content></Select.Root></label><ThemeButton type="button" size="2" variant="soft" color="gray" disabled={currentPage <= 1} onClick={() => setPage(currentPage - 1)}>Trước</ThemeButton><span>Trang {currentPage}/{pageCount}</span><ThemeButton type="button" size="2" variant="soft" color="gray" disabled={currentPage >= pageCount} onClick={() => setPage(currentPage + 1)}>Sau</ThemeButton></div></div>
        </>}
      </section>
    </>}
  </div>;
}
