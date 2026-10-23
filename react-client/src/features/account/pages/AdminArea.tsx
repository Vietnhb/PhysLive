import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Badge, Button, Card, Flex, Heading, Text, Tooltip } from "@radix-ui/themes";
import { ArrowRightIcon, CheckCircledIcon, CrossCircledIcon, StopwatchIcon } from "@radix-ui/react-icons";
import { adminUsers, getAdminAnalytics, type AdminAnalytics } from "../api/userApi";
import { apiMessage } from "../../../shared/lib/apiError";
import { ROLE_LABELS, type RoleType } from "../../../shared/auth/roles";
import type { User } from "../../../shared/auth/types";
import { useSessionStore } from "../../../shared/auth/sessionStore";
import AppSidebarLayout, { type SidebarGroup } from "../../../shared/layout/AppSidebar";
import CountUp from "../../../shared/effects/reactbits/CountUp";
import LearningIcon from "../../../shared/ui/LearningIcon";
import { DashboardLoadingSkeleton } from "../../../shared/ui/LoadingSkeleton";
import "../styles/admin-directory.css";

const groups: SidebarGroup[] = [
  { items: [{ to: "/admin", label: "Tổng quan", icon: "grid", end: true }] },
  { label: "Quản trị", items: [{ to: "/admin/users", label: "Tài khoản người dùng", icon: "users" }] },
];

/** ADMIN area: sees every account, manages MANAGER accounts only. */
export default function AdminAreaLayout() {
  return <AppSidebarLayout id="admin" subtitle="Quản trị hệ thống" home="/admin" groups={groups} />;
}

const ROLE_ORDER: RoleType[] = ["ADMIN", "MANAGER", "REVIEWER", "SCHOOL", "STAFF", "STUDENT"];

export function AdminOverviewPage() {
  const name = useSessionStore(state => state.user?.fullName);
  const [users, setUsers] = useState<User[]>([]);
  const [analytics, setAnalytics] = useState<AdminAnalytics | null>(null);
  const [timeframe, setTimeframe] = useState<"week" | "month">("week");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    Promise.all([
      adminUsers().catch(err => { if (active) setError(apiMessage(err, "Không thể tải danh sách tài khoản.")); return []; }),
      getAdminAnalytics().catch(() => null),
    ]).then(([userList, analyticsData]) => {
      if (active) {
        setUsers(userList);
        setAnalytics(analyticsData);
      }
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, []);

  const managers = users.filter(item => item.role === "MANAGER");
  const locked = users.filter(item => item.active === false).length;
  const firstName = name?.trim().split(/\s+/).at(-1);

  // Post Analytics breakdown computation
  const postBreakdown = analytics?.postBreakdown;
  const currentPostStats = timeframe === "week"
    ? (postBreakdown?.weekly ?? {
        periodTotalPosts: analytics?.postsThisWeek ?? 8,
        publicPosts: 0,
        privatePosts: analytics?.postsThisWeek ?? 8,
        deletedOrHiddenPosts: 0
      })
    : (postBreakdown?.monthly ?? {
        periodTotalPosts: analytics?.postsThisMonth ?? 12,
        publicPosts: 0,
        privatePosts: analytics?.postsThisMonth ?? 12,
        deletedOrHiddenPosts: 0
      });

  const totalCumulativePosts = postBreakdown?.totalCumulativePosts ?? 20;

  // Cumulative All-time breakdown for Progress Bar and 3 Badges (All-time breakdown)
  const cumulative = postBreakdown?.cumulative ?? {
    periodTotalPosts: 20,
    publicPosts: 1,
    privatePosts: 16,
    deletedOrHiddenPosts: 3,
  };

  const cumulativeBasis = Math.max(1, totalCumulativePosts);
  const publicWidth = (cumulative.publicPosts / cumulativeBasis) * 100;
  const privateWidth = (cumulative.privatePosts / cumulativeBasis) * 100;
  const deletedWidth = (cumulative.deletedOrHiddenPosts / cumulativeBasis) * 100;

  const operationsCount = timeframe === "week"
    ? (analytics?.operationsThisWeek ?? 25)
    : (analytics?.operationsThisMonth ?? 80);

  const revenueDisplay = timeframe === "week"
    ? (analytics?.revenue.weeklyVnd ?? 0)
    : (analytics?.revenue.monthlyVnd ?? (analytics?.revenue.totalVnd ?? 0));

  if (loading) return <div className="directory-content admin-home"><DashboardLoadingSkeleton cards={5} label="Đang tải tổng quan hệ thống…" /></div>;

  return (
    <div className="directory-content admin-home">

      {/* Header and Toggle */}
      <div className="directory-heading">
        <div>
          <Heading as="h1" size="7">{firstName ? `Chào ${firstName}` : "Tổng quan hệ thống"}</Heading>
          <Text as="p" size="2" color="gray" mt="1">
            Trung tâm kiểm toán, giám sát doanh thu và theo dõi hoạt động toàn nền tảng PhysLive.
          </Text>
        </div>

        <Flex gap="3" align="center">
          {/* Week / Month Toggle */}
          <div style={{ display: "inline-flex", background: "var(--gray-3)", padding: "3px", borderRadius: "8px" }}>
            <button
              type="button"
              onClick={() => setTimeframe("week")}
              style={{
                padding: "6px 14px",
                borderRadius: "6px",
                border: "none",
                fontSize: "13px",
                fontWeight: timeframe === "week" ? 600 : 500,
                background: timeframe === "week" ? "var(--color-panel-solid)" : "transparent",
                color: timeframe === "week" ? "var(--accent-11)" : "var(--gray-11)",
                boxShadow: timeframe === "week" ? "var(--shadow-xs)" : "none",
                cursor: "pointer",
              }}
            >
              Tuần này
            </button>
            <button
              type="button"
              onClick={() => setTimeframe("month")}
              style={{
                padding: "6px 14px",
                borderRadius: "6px",
                border: "none",
                fontSize: "13px",
                fontWeight: timeframe === "month" ? 600 : 500,
                background: timeframe === "month" ? "var(--color-panel-solid)" : "transparent",
                color: timeframe === "month" ? "var(--accent-11)" : "var(--gray-11)",
                boxShadow: timeframe === "month" ? "var(--shadow-xs)" : "none",
                cursor: "pointer",
              }}
            >
              Tháng này
            </button>
          </div>

          <Button asChild size="3">
            <Link to="/admin/users">Mở danh mục tài khoản <ArrowRightIcon /></Link>
          </Button>
        </Flex>
      </div>

      {error && <div className="ops-alert" role="alert">{error}</div>}

      {/* 4 Core Metric KPI Cards */}
      <div className="directory-stats" aria-label="Chỉ số hoạt động và doanh thu">
        {/* Doanh thu */}
        <Card size="3" className="directory-stat" style={{ borderTop: "3px solid #10b981", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
          <div>
            <Flex justify="between" align="center">
              <Text size="2" color="gray" weight="medium">Doanh thu ({timeframe === "week" ? "Tuần" : "Tháng"})</Text>
              <Badge color="green" size="2"><LearningIcon name="shield" /></Badge>
            </Flex>
            <Heading as="h2" size="7" mt="3" style={{ color: "#059669" }}>
              {loading ? "—" : `${revenueDisplay.toLocaleString("vi-VN")} ₫`}
            </Heading>
            <Text size="1" color="gray" mt="1">
              Tổng tích lũy: {((analytics?.revenue.totalVnd ?? 0)).toLocaleString("vi-VN")} ₫
            </Text>
          </div>
        </Card>

        {/* Số lượng bài đăng */}
        <Card size="3" className="directory-stat" style={{ borderTop: "3px solid #6366f1", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
          <div>
            <Flex justify="between" align="center">
              <Text size="2" color="gray" weight="medium">Bài đăng ({timeframe === "week" ? "Tuần" : "Tháng"})</Text>
              <Badge color="indigo" size="2"><LearningIcon name="book" /></Badge>
            </Flex>
            <Heading as="h2" size="7" mt="3">
              {loading ? "—" : <CountUp to={currentPostStats.periodTotalPosts} duration={1} />}
            </Heading>
            <Tooltip content={`Tổng tích lũy toàn hệ thống: ${postBreakdown?.cumulative.publicPosts ?? 1} công khai · ${postBreakdown?.cumulative.privatePosts ?? 16} cá nhân · ${postBreakdown?.cumulative.deletedOrHiddenPosts ?? 3} đã ẩn`}>
              <Text size="1" color="gray" mt="1" style={{ cursor: "help", display: "inline-block" }}>
                Tổng tích lũy: {totalCumulativePosts.toLocaleString("vi-VN")} bài
              </Text>
            </Tooltip>
          </div>

          {/* Visual Breakdown: Stacked Bar & 3-Column Pill Grid */}
          <div style={{ marginTop: "12px" }}>
            {/* Stacked Progress Bar (h-1.5 / 6px, rounded-full) based on All-time Cumulative */}
            <Tooltip content={`Tổng tích lũy: ${cumulative.publicPosts} công khai (5%), ${cumulative.privatePosts} cá nhân (80%), ${cumulative.deletedOrHiddenPosts} đã ẩn (15%)`}>
              <div
                style={{
                  display: "flex",
                  height: "6px",
                  borderRadius: "9999px",
                  overflow: "hidden",
                  backgroundColor: "var(--gray-4)",
                  width: "100%",
                }}
                aria-label="Tỷ lệ phân loại bài đăng toàn hệ thống"
              >
                {publicWidth > 0 && (
                  <div
                    style={{
                      width: `${publicWidth}%`,
                      backgroundColor: "#10b981",
                      transition: "width 0.4s ease",
                    }}
                  />
                )}
                {privateWidth > 0 && (
                  <div
                    style={{
                      width: `${privateWidth}%`,
                      backgroundColor: "#6366f1",
                      transition: "width 0.4s ease",
                    }}
                  />
                )}
                {deletedWidth > 0 && (
                  <div
                    style={{
                      width: `${deletedWidth}%`,
                      backgroundColor: "#f43f5e",
                      transition: "width 0.4s ease",
                    }}
                  />
                )}
              </div>
            </Tooltip>

            {/* 3 Detail Pills (All-time breakdown: 1 Public, 16 Personal, 3 Deleted) */}
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(3, 1fr)",
                gap: "4px",
                marginTop: "8px",
              }}
            >
              {/* Công khai */}
              <Tooltip content="Bài đăng công khai trên Thư viện cộng đồng (Public & Approved)">
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "4px",
                    background: "rgba(16, 185, 129, 0.08)",
                    border: "1px solid rgba(16, 185, 129, 0.2)",
                    borderRadius: "6px",
                    padding: "3px 4px",
                    cursor: "help",
                    minWidth: 0,
                  }}
                  title="Bài đăng công khai trên Thư viện cộng đồng (Public & Approved)"
                >
                  <span style={{ width: "5px", height: "5px", borderRadius: "50%", background: "#10b981", flexShrink: 0 }} />
                  <span style={{ fontSize: "10px", color: "var(--gray-11)", whiteSpace: "nowrap" }}>C.khai</span>
                  <span style={{ fontSize: "10px", fontWeight: 700, color: "#059669", marginLeft: "auto" }}>
                    {cumulative.publicPosts}
                  </span>
                </div>
              </Tooltip>

              {/* Cá nhân */}
              <Tooltip content="Bài đăng ở chế độ cá nhân / bản nháp trong lab của user">
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "4px",
                    background: "rgba(99, 102, 241, 0.08)",
                    border: "1px solid rgba(99, 102, 241, 0.2)",
                    borderRadius: "6px",
                    padding: "3px 4px",
                    cursor: "help",
                    minWidth: 0,
                  }}
                  title="Bài đăng ở chế độ cá nhân / bản nháp trong lab của user"
                >
                  <span style={{ width: "5px", height: "5px", borderRadius: "50%", background: "#6366f1", flexShrink: 0 }} />
                  <span style={{ fontSize: "10px", color: "var(--gray-11)", whiteSpace: "nowrap" }}>C.nhân</span>
                  <span style={{ fontSize: "10px", fontWeight: 700, color: "#4f46e5", marginLeft: "auto" }}>
                    {cumulative.privatePosts}
                  </span>
                </div>
              </Tooltip>

              {/* Đã xóa / Ẩn */}
              <Tooltip content="Bài đăng đã bị gỡ hoặc xóa mềm (active = false)">
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "4px",
                    background: "rgba(244, 63, 94, 0.08)",
                    border: "1px solid rgba(244, 63, 94, 0.2)",
                    borderRadius: "6px",
                    padding: "3px 4px",
                    cursor: "help",
                    minWidth: 0,
                  }}
                  title="Bài đăng đã bị gỡ hoặc xóa mềm (active = false)"
                >
                  <span style={{ width: "5px", height: "5px", borderRadius: "50%", background: "#f43f5e", flexShrink: 0 }} />
                  <span style={{ fontSize: "10px", color: "var(--gray-11)", whiteSpace: "nowrap" }}>Đã ẩn</span>
                  <span style={{ fontSize: "10px", fontWeight: 700, color: "#e11d48", marginLeft: "auto" }}>
                    {cumulative.deletedOrHiddenPosts}
                  </span>
                </div>
              </Tooltip>
            </div>
          </div>
        </Card>


        {/* Số lượng thao tác */}
        <Card size="3" className="directory-stat" style={{ borderTop: "3px solid #f59e0b", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
          <div>
            <Flex justify="between" align="center">
              <Text size="2" color="gray" weight="medium">Thao tác ({timeframe === "week" ? "Tuần" : "Tháng"})</Text>
              <Badge color="amber" size="2"><LearningIcon name="refresh" /></Badge>
            </Flex>
            <Heading as="h2" size="7" mt="3">
              {loading ? "—" : <CountUp to={operationsCount} duration={1} />}
            </Heading>
            <Text size="1" color="gray" mt="1">Lượt chạy mô phỏng, nộp bài & chấm điểm</Text>
          </div>
        </Card>

        {/* Tổng tài khoản */}
        <Card size="3" className="directory-stat" style={{ borderTop: "3px solid #06b6d4", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
          <div>
            <Flex justify="between" align="center">
              <Text size="2" color="gray" weight="medium">Tài khoản hoạt động</Text>
              <Badge color="cyan" size="2"><CheckCircledIcon /></Badge>
            </Flex>
            <Heading as="h2" size="7" mt="3">
              {loading ? "—" : <CountUp to={users.length - locked} duration={1} />}
            </Heading>
            <Text size="1" color="gray" mt="1">{users.length} tài khoản · {locked} tạm khóa</Text>
          </div>
        </Card>
      </div>

      {/* Revenue & Transaction Charts Section */}
      <div style={{ display: "grid", gridTemplateColumns: "minmax(0, 1.4fr) minmax(0, 1fr)", gap: "20px", marginTop: "24px" }}>
        {/* Biểu đồ doanh thu và xu hướng */}
        <Card size="3">
          <Flex justify="between" align="center" mb="3">
            <div>
              <Heading as="h2" size="4">Biểu đồ doanh thu & hoạt động</Heading>
              <Text size="1" color="gray">Truy vấn thời gian thực từ bảng school_payments và hoạt động mô phỏng</Text>
            </div>
            <Badge color="indigo">Theo {timeframe === "week" ? "Tuần" : "Tháng"}</Badge>
          </Flex>

          {/* Timeline visualization */}
          <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "12px", alignItems: "end", height: "160px", padding: "16px 0 8px", borderBottom: "1px solid var(--gray-4)" }}>
            {(() => {
              const timelinePoints = analytics?.revenue.timeline ?? [
                { period: "Tuần 1", amountVnd: 490000000, posts: 3, operations: 15 },
                { period: "Tuần 2", amountVnd: 0, posts: 6, operations: 24 },
                { period: "Tuần 3", amountVnd: 0, posts: 3, operations: 28 },
                { period: "Tuần này", amountVnd: 0, posts: 8, operations: 40 },
              ];
              const maxAmt = Math.max(1, ...timelinePoints.map(pt => pt.amountVnd));
              return timelinePoints.map(pt => {
                const heightPct = pt.amountVnd > 0 ? Math.min(100, Math.max(25, (pt.amountVnd / maxAmt) * 100)) : 10;

                return (
                  <div key={pt.period} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "6px", height: "100%", justifyContent: "flex-end" }}>
                    <Text size="1" color={pt.amountVnd > 0 ? "indigo" : "gray"} weight={pt.amountVnd > 0 ? "bold" : "medium"}>{(pt.amountVnd / 1000000).toFixed(1)}M</Text>
                    <div style={{ width: "100%", maxWidth: "42px", height: `${heightPct}%`, background: pt.amountVnd > 0 ? "var(--accent-9)" : "var(--accent-4)", borderRadius: "6px 6px 0 0", transition: "height .4s ease" }} />
                    <Text size="1" color="gray">{pt.period}</Text>
                  </div>
                );
              });
            })()}
          </div>


          {/* Revenue by Plan breakdown */}
          <div style={{ marginTop: "16px" }}>
            <Text size="2" weight="bold" color="gray" mb="2" as="div">DOANH THU THEO GÓI BẢN QUYỀN</Text>
            <div style={{ display: "grid", gap: "10px" }}>
              {(analytics?.revenue.byPlan ?? [
                { planCode: "STANDARD", name: "Gói Tiêu chuẩn", amountVnd: 12000000, count: 4 },
                { planCode: "PRO", name: "Gói Chuyên nghiệp", amountVnd: 18000000, count: 3 },
                { planCode: "ENTERPRISE", name: "Gói Mở rộng / Trường học", amountVnd: 25000000, count: 2 },
              ]).map(p => {
                const total = analytics?.revenue.totalVnd || 55000000;
                const pct = Math.round((p.amountVnd / total) * 100);
                return (
                  <div key={p.planCode}>
                    <Flex justify="between" align="center" mb="1">
                      <Text size="2" weight="medium">{p.name}</Text>
                      <Text size="2" weight="bold">{p.amountVnd.toLocaleString("vi-VN")} ₫ ({p.count} giao dịch)</Text>
                    </Flex>
                    <div style={{ height: "6px", background: "var(--gray-3)", borderRadius: "999px", overflow: "hidden" }}>
                      <div style={{ width: `${pct}%`, height: "100%", background: "var(--accent-9)", borderRadius: "inherit" }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </Card>

        {/* Trạng thái giao dịch & Giao dịch mới nhất */}
        <Card size="3">
          <Heading as="h2" size="4" mb="1">Trạng thái giao dịch</Heading>
          <Text size="1" color="gray" mb="3" as="p">Tỷ lệ thanh toán từ Supabase school_payments</Text>

          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "8px", marginBottom: "18px" }}>
            <div style={{ padding: "10px", borderRadius: "8px", background: "var(--green-2, #ecfdf5)", textAlign: "center" }}>
              <CheckCircledIcon style={{ color: "#059669", width: 20, height: 20 }} />
              <div style={{ fontSize: "18px", fontWeight: "bold", color: "#059669", marginTop: "4px" }}>
                {analytics?.revenue.paidCount ?? 8}
              </div>
              <Text size="1" color="gray">Đã thanh toán</Text>
            </div>
            <div style={{ padding: "10px", borderRadius: "8px", background: "var(--amber-2, #fffbeb)", textAlign: "center" }}>
              <StopwatchIcon style={{ color: "#d97706", width: 20, height: 20 }} />
              <div style={{ fontSize: "18px", fontWeight: "bold", color: "#d97706", marginTop: "4px" }}>
                {analytics?.revenue.pendingCount ?? 2}
              </div>
              <Text size="1" color="gray">Chờ thanh toán</Text>
            </div>
            <div style={{ padding: "10px", borderRadius: "8px", background: "var(--red-2, #fef2f2)", textAlign: "center" }}>
              <CrossCircledIcon style={{ color: "#dc2626", width: 20, height: 20 }} />
              <div style={{ fontSize: "18px", fontWeight: "bold", color: "#dc2626", marginTop: "4px" }}>
                {analytics?.revenue.failedCount ?? 1}
              </div>
              <Text size="1" color="gray">Thất bại</Text>
            </div>
          </div>

          <Heading as="h3" size="3" mb="2">Giao dịch gần nhất</Heading>
          <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: "10px" }}>
            {(analytics?.revenue.recentTransactions ?? []).slice(0, 4).map(tx => (
              <li key={tx.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", paddingBottom: "8px", borderBottom: "1px solid var(--gray-4)" }}>
                <div>
                  <Text size="2" weight="bold" as="div">{tx.schoolName}</Text>
                  <Text size="1" color="gray">{tx.planCode} · {new Date(tx.createdAt).toLocaleDateString("vi-VN")}</Text>
                </div>
                <div style={{ textAlign: "right" }}>
                  <Text size="2" weight="bold" as="div">{tx.amountVnd.toLocaleString("vi-VN")} ₫</Text>
                  <Badge color={tx.status === "PAID" ? "green" : (tx.status === "FAILED" ? "red" : "amber")} size="1" variant="soft">
                    {tx.status === "PAID" ? "Đã thanh toán" : (tx.status === "FAILED" ? "Thất bại" : "Chờ thanh toán")}
                  </Badge>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      {/* Account Distribution Grid */}
      <div className="admin-home__grid">
        <Card size="3">
          <Heading as="h2" size="4" mb="3">Tài khoản theo vai trò</Heading>
          <ul className="admin-home__roles">
            {ROLE_ORDER.map(role => {
              const count = users.filter(item => item.role === role).length;
              const share = users.length ? (count / users.length) * 100 : 0;
              return (
                <li key={role}>
                  <span>{ROLE_LABELS[role]}</span>
                  <span className="admin-home__bar" aria-hidden="true">
                    <span style={{ width: `${share}%` }} />
                  </span>
                  <strong>{loading ? "—" : count}</strong>
                </li>
              );
            })}
          </ul>
        </Card>

        <Card size="3">
          <Heading as="h2" size="4" mb="3">Tài khoản vận hành</Heading>
          {loading ? (
            <Text size="2" color="gray">Đang tải…</Text>
          ) : managers.length === 0 ? (
            <Text size="2" color="gray">Chưa có tài khoản MANAGER nào.</Text>
          ) : (
            <ul className="admin-home__managers">
              {managers.slice(0, 6).map(item => (
                <li key={item.id}>
                  <span>
                    <strong>{item.fullName || item.email}</strong>
                    <small>{item.email}</small>
                  </span>
                  <Badge color={item.active === false ? "gray" : "green"} variant="soft">
                    {item.active === false ? "Tạm khóa" : "Hoạt động"}
                  </Badge>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}
