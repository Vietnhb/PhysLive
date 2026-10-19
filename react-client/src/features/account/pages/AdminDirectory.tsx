import { useEffect, useState } from "react";
import { Avatar, Badge, Button, Card, Flex, Heading, IconButton, Select, Table, Text, TextField } from "@radix-ui/themes";
import { ArrowRightIcon, CheckCircledIcon, Cross2Icon, ExclamationTriangleIcon, LockClosedIcon, MagnifyingGlassIcon, PersonIcon, ReloadIcon } from "@radix-ui/react-icons";
import { AnimatePresence, motion } from "motion/react";
import { adminUsers } from "../api/userApi";
import { apiMessage } from "../../../shared/lib/apiError";
import LearningIcon from "../../../shared/ui/LearningIcon";
import { ROLE_NAMES, getRoleLabel } from "../../../shared/auth/roles";
import type { User, AccountStatus } from "../../../shared/auth/types";
import { permissionSummary, permissionsForRole } from "../../../shared/auth/permissions";
import "../styles/admin-directory.css";

type UserTier = "ALL" | "SYSTEM" | "SCHOOL" | "INDIVIDUAL";

const USER_TIERS: Record<UserTier, { id: UserTier; label: string; roles: string[]; icon: Parameters<typeof LearningIcon>[0]["name"]; desc: string }> = {
  ALL: { id: "ALL", label: "Tất cả cấp bậc", roles: [], icon: "grid", desc: "Toàn bộ tài khoản trong nền tảng" },
  SCHOOL: { id: "SCHOOL", label: "Trường học (School)", roles: [ROLE_NAMES.SCHOOL, ROLE_NAMES.STAFF], icon: "book", desc: "Tài khoản đại diện trường và giáo viên bộ môn" },
  SYSTEM: { id: "SYSTEM", label: "Hệ thống (System)", roles: [ROLE_NAMES.ADMIN, ROLE_NAMES.MANAGER], icon: "shield", desc: "Quản trị viên nền tảng và quản lý vận hành" },
  INDIVIDUAL: { id: "INDIVIDUAL", label: "Cá nhân (Individual)", roles: [ROLE_NAMES.STUDENT, ROLE_NAMES.REVIEWER], icon: "users", desc: "Học sinh và chuyên gia thẩm định độc lập" },
};

function getUserTier(role: string): "SYSTEM" | "SCHOOL" | "INDIVIDUAL" {
  if (role === ROLE_NAMES.ADMIN || role === ROLE_NAMES.MANAGER) return "SYSTEM";
  if (role === ROLE_NAMES.SCHOOL || role === ROLE_NAMES.STAFF) return "SCHOOL";
  return "INDIVIDUAL";
}

const formatDate = (date?: string | null) => date ? new Date(date).toLocaleString("vi-VN") : "Chưa có thông tin";
const roleColors: Record<string, "iris" | "indigo" | "amber" | "cyan" | "green" | "gray"> = {
  ADMIN: "iris", MANAGER: "indigo", REVIEWER: "amber", SCHOOL: "cyan", STAFF: "green", STUDENT: "gray"
};

function accountStatusBadge(user: User) {
  const status: AccountStatus = user.accountStatus ?? (user.active === false ? "SUSPENDED_READ_ONLY" : "ACTIVE");
  if (status === "SUSPENDED_READ_ONLY" || user.active === false) {
    return <Badge color="red" variant="soft"><LockClosedIcon /> Tạm khóa (Chỉ xem)</Badge>;
  }
  if (status === "WARNED") {
    return <Badge color="amber" variant="soft"><ExclamationTriangleIcon /> Cảnh báo</Badge>;
  }
  return <Badge color="cyan" variant="soft"><CheckCircledIcon /> Hoạt động</Badge>;
}

export default function AdminDirectory() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [tier, setTier] = useState<UserTier>("ALL");
  const [role, setRole] = useState("");
  const [status, setStatus] = useState("");
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const selected = users.find(item => item.id === selectedId);

  const load = async () => {
    setLoading(true); setError("");
    try { setUsers(await adminUsers()); }
    catch (err) { setError(apiMessage(err, "Không thể tải danh sách tài khoản. Vui lòng thử lại.")); }
    finally { setLoading(false); }
  };

  useEffect(() => { void load(); }, []);

  // Filter accounts by tier, role, status and search query
  const filtered = users.filter(item => {
    if (tier !== "ALL" && !USER_TIERS[tier].roles.includes(item.role)) return false;
    if (role && item.role !== role) return false;
    if (status) {
      const userStatus: AccountStatus = item.accountStatus ?? (item.active === false ? "SUSPENDED_READ_ONLY" : "ACTIVE");
      if (status === "ACTIVE" && (userStatus !== "ACTIVE" || item.active === false)) return false;
      if (status === "WARNED" && userStatus !== "WARNED") return false;
      if (status === "SUSPENDED" && userStatus !== "SUSPENDED_READ_ONLY" && item.active !== false) return false;
    }
    if (query.trim()) {
      const q = query.trim().toLowerCase();
      const match = `${item.fullName} ${item.email} ${item.id} ${item.schoolName ?? ""}`.toLowerCase();
      if (!match.includes(q)) return false;
    }
    return true;
  });

  const tierCounts = {
    ALL: users.length,
    SCHOOL: users.filter(u => USER_TIERS.SCHOOL.roles.includes(u.role)).length,
    SYSTEM: users.filter(u => USER_TIERS.SYSTEM.roles.includes(u.role)).length,
    INDIVIDUAL: users.filter(u => USER_TIERS.INDIVIDUAL.roles.includes(u.role)).length,
  };

  return (
    <div className="directory-main">
      <div className="directory-content">
        {/* Page Heading & Strict View-only Badge */}
        <div className="directory-heading">
          <div>
            <Flex align="center" gap="3">
              <Heading as="h1" size="7">Tài khoản người dùng</Heading>
              <Badge color="amber" size="2" variant="surface">
                <LockClosedIcon /> Chế độ xem (View-only)
              </Badge>
            </Flex>
            <Text as="p" size="2" color="gray" mt="1">
              Tra cứu và kiểm toán tài khoản toàn hệ thống theo 3 cấp độ: Trường học, Hệ thống và Cá nhân. Toàn bộ thao tác sửa đổi đã được khóa an toàn.
            </Text>
          </div>
        </div>

        {/* High-level Stats */}
        <div className="directory-stats" aria-label="Thống kê tài khoản">
          {[
            { label: "Tổng tài khoản", value: users.length, icon: <PersonIcon /> },
            { label: "Trường học (School)", value: tierCounts.SCHOOL, icon: <LearningIcon name="book" /> },
            { label: "Hệ thống (System)", value: tierCounts.SYSTEM, icon: <LearningIcon name="shield" /> },
            { label: "Cá nhân (Individual)", value: tierCounts.INDIVIDUAL, icon: <PersonIcon /> },
          ].map((stat, i) => (
            <motion.div key={stat.label} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * .035 }} whileHover={{ y: -2 }}>
              <Card size="3" className="directory-stat">
                <Flex justify="between" align="center" gap="3">
                  <Text size="2" color="gray" weight="medium">{stat.label}</Text>
                  <Badge color="gray" size="3" className="directory-stat-icon">{stat.icon}</Badge>
                </Flex>
                <Heading as="h2" size="8" mt="3">{loading ? "—" : stat.value}</Heading>
              </Card>
            </motion.div>
          ))}
        </div>

        {/* View-Only Audit Callout */}
        <div className="directory-readonly-notice" role="status">
          <LockClosedIcon />
          <span><strong>Ràng buộc bảo mật:</strong> Màn hình này ở chế độ chỉ đọc (View-only). Mọi tác vụ thêm mới, sửa đổi hoặc xóa người dùng đều bị khóa để bảo toàn tính toàn vẹn dữ liệu kiểm toán.</span>
        </div>

        {/* 2-Column Layout with Tier Sidebar on the left */}
        <div className="directory-layout-with-sidebar">
          {/* Left Tier Hierarchy Sidebar */}
          <aside className="directory-tier-sidebar" aria-label="Phân cấp tài khoản">
            <div className="directory-tier-sidebar-header">
              <Text size="2" weight="bold" color="gray">CẤP BẬC TÀI KHOẢN</Text>
              <Text as="p" size="1" color="gray" mt="1">Chọn phân cấp để lọc danh sách</Text>
            </div>

            {(["ALL", "SCHOOL", "SYSTEM", "INDIVIDUAL"] as UserTier[]).map(key => {
              const item = USER_TIERS[key];
              const isActive = tier === key;
              return (
                <button
                  type="button"
                  key={key}
                  className={`directory-tier-btn ${isActive ? "active" : ""}`}
                  onClick={() => { setTier(key); setRole(""); }}
                  aria-pressed={isActive}
                >
                  <div className="directory-tier-btn-left">
                    <LearningIcon name={item.icon} />
                    <div>
                      <div>{item.label}</div>
                      <Text as="div" size="1" color={isActive ? "indigo" : "gray"}>
                        {key === "ALL" ? "Toàn bộ" : item.roles.join(", ")}
                      </Text>
                    </div>
                  </div>
                  <Badge color={isActive ? "indigo" : "gray"} variant="soft" size="1">
                    {loading ? "…" : tierCounts[key]}
                  </Badge>
                </button>
              );
            })}
          </aside>

          {/* Main User List & Detail Panel */}
          <div className={`directory-workspace ${selected ? "with-detail" : ""}`}>
            <Card className="directory-panel" size="1" aria-label="Danh sách tài khoản">
              <div className="directory-panel-heading">
                <div>
                  <Heading as="h2" size="4">
                    {USER_TIERS[tier].label}
                  </Heading>
                  <Text as="p" size="2" color="gray">
                    {loading ? "Đang tải dữ liệu…" : `${filtered.length} / ${users.length} tài khoản`}
                  </Text>
                </div>
                <IconButton variant="soft" color="gray" onClick={() => void load()} disabled={loading} aria-label="Làm mới danh sách">
                  <ReloadIcon />
                </IconButton>
              </div>

              {/* 3-Tier Filter Pills */}
              <div className="directory-tier-tabs" role="tablist" aria-label="Bộ lọc 3 cấp độ">
                {(["ALL", "SCHOOL", "SYSTEM", "INDIVIDUAL"] as UserTier[]).map(key => (
                  <button
                    type="button"
                    key={key}
                    role="tab"
                    aria-selected={tier === key}
                    className={`directory-tier-pill ${tier === key ? "active" : ""}`}
                    onClick={() => { setTier(key); setRole(""); }}
                  >
                    <span>{USER_TIERS[key].label}</span>
                    <Badge color={tier === key ? "indigo" : "gray"} size="1" variant="soft">
                      {tierCounts[key]}
                    </Badge>
                  </button>
                ))}
              </div>

              {/* Search & Dropdown Filters */}
              <div className="directory-filters">
                <TextField.Root aria-label="Tìm tài khoản" placeholder="Tìm tên, email, mã hoặc trường…" value={query} onChange={e => setQuery(e.target.value)} size="3">
                  <TextField.Slot><MagnifyingGlassIcon /></TextField.Slot>
                </TextField.Root>

                <Select.Root value={role || "all"} onValueChange={value => setRole(value === "all" ? "" : value)} size="3">
                  <Select.Trigger aria-label="Lọc vai trò" />
                  <Select.Content>
                    <Select.Item value="all">Tất cả vai trò</Select.Item>
                    {(tier === "ALL" ? Object.values(ROLE_NAMES) : USER_TIERS[tier].roles).map(value => (
                      <Select.Item key={value} value={value}>{getRoleLabel(value)} ({value})</Select.Item>
                    ))}
                  </Select.Content>
                </Select.Root>

                <Select.Root value={status || "all"} onValueChange={value => setStatus(value === "all" ? "" : value)} size="3">
                  <Select.Trigger aria-label="Lọc trạng thái" />
                  <Select.Content>
                    <Select.Item value="all">Tất cả trạng thái</Select.Item>
                    <Select.Item value="ACTIVE">Hoạt động (ACTIVE)</Select.Item>
                    <Select.Item value="WARNED">Cảnh báo (WARNED)</Select.Item>
                    <Select.Item value="SUSPENDED">Tạm khóa (SUSPENDED)</Select.Item>
                  </Select.Content>
                </Select.Root>
              </div>

              {error ? (
                <div className="directory-empty" role="alert">
                  <p>{error}</p>
                  <Button variant="soft" onClick={() => void load()}>Thử lại</Button>
                </div>
              ) : loading ? (
                <div className="directory-empty" role="status">Đang tải danh sách tài khoản…</div>
              ) : filtered.length === 0 ? (
                <div className="directory-empty">
                  <MagnifyingGlassIcon />
                  <Heading as="h3" size="4">Chưa tìm thấy tài khoản</Heading>
                  <p>Thử tìm theo từ khóa khác hoặc thay đổi bộ lọc cấp bậc.</p>
                  <Button variant="soft" onClick={() => { setQuery(""); setTier("ALL"); setRole(""); setStatus(""); }}>
                    Xóa bộ lọc
                  </Button>
                </div>
              ) : (
                <Table.Root size="3" className="directory-table">
                  <Table.Header>
                    <Table.Row>
                      <Table.ColumnHeaderCell>Người dùng</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>Cấp bậc & Vai trò</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>Trạng thái</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell><span className="sr-only">Chi tiết</span></Table.ColumnHeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {filtered.map(item => (
                      <Table.Row key={item.id} className={selectedId === item.id ? "selected" : ""}>
                        <Table.Cell>
                          <div className="directory-person">
                            <Avatar size="2" src={item.avatarUrl || undefined} color={roleColors[item.role]} fallback={(item.fullName || item.email).slice(0, 1).toUpperCase()} />
                            <div>
                              <Text weight="medium">{item.fullName || "Chưa có họ tên"}</Text>
                              <Text as="div" color="gray" size="2">{item.email}</Text>
                            </div>
                          </div>
                        </Table.Cell>
                        <Table.Cell>
                          <Flex direction="column" gap="1" align="start">
                            <Badge color={roleColors[item.role]}>{getRoleLabel(item.role)}</Badge>
                            <Text size="1" color="gray">{USER_TIERS[getUserTier(item.role)].label.split(" ")[0]}</Text>
                          </Flex>
                        </Table.Cell>
                        <Table.Cell>
                          {accountStatusBadge(item)}
                        </Table.Cell>
                        <Table.Cell>
                          <Button variant="ghost" aria-label={`Xem chi tiết ${item.fullName || item.email}`} onClick={() => setSelectedId(item.id)}>
                            Xem <ArrowRightIcon />
                          </Button>
                        </Table.Cell>
                      </Table.Row>
                    ))}
                  </Table.Body>
                </Table.Root>
              )}

              <footer className="directory-table-footer">
                <LearningIcon name="shield" />
                <span>Chế độ kiểm toán View-only dành cho Quản trị viên · Đồng bộ trực tuyến với Supabase Auth & Users</span>
              </footer>
            </Card>

            {/* Read-Only Account Detail Drawer */}
            <AnimatePresence initial={false}>
              {selected && (
                <motion.div key={selected.id} initial={{ opacity: 0, x: 12 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0 }}>
                  <Card className="directory-detail" size="3" aria-label="Thông tin tài khoản (Chỉ xem)">
                    <Flex justify="between" align="center" mb="4">
                      <div>
                        <Heading as="h2" size="4">Chi tiết tài khoản</Heading>
                        <Badge color="amber" size="1" variant="surface">Chỉ xem</Badge>
                      </div>
                      <IconButton variant="ghost" color="gray" aria-label="Đóng chi tiết" onClick={() => setSelectedId(null)}>
                        <Cross2Icon />
                      </IconButton>
                    </Flex>

                    <div className="directory-detail-identity">
                      <Avatar size="5" src={selected.avatarUrl || undefined} color={roleColors[selected.role]} fallback={(selected.fullName || selected.email).slice(0, 1).toUpperCase()} />
                      <Heading as="h3" size="4" mt="3" mb="1">{selected.fullName || "Chưa có họ tên"}</Heading>
                      <Flex justify="center" gap="2" mt="1">
                        <Badge color={roleColors[selected.role]}>{getRoleLabel(selected.role)}</Badge>
                        <Badge color="gray">{USER_TIERS[getUserTier(selected.role)].label.split(" ")[0]}</Badge>
                      </Flex>
                    </div>

                    <dl>
                      {[
                        ["Mã tài khoản", `#${selected.id}`],
                        ["Email", selected.email],
                        ["Phân cấp", USER_TIERS[getUserTier(selected.role)].label],
                        ["Vai trò", `${getRoleLabel(selected.role)} (${selected.role})`],
                        ["Trạng thái", selected.accountStatus ?? (selected.active === false ? "SUSPENDED_READ_ONLY" : "ACTIVE")],
                        ...(selected.suspensionReason ? [["Lý do tạm khóa", selected.suspensionReason]] : []),
                        ...(selected.suspendedUntil ? [["Thời hạn khóa", formatDate(selected.suspendedUntil)]] : []),
                        ["Trường / Tổ chức", selected.schoolName || selected.schoolId || selected.institutionId || "Tài khoản cấp nền tảng"],
                        ["Ngày sinh", selected.dateOfBirth ? new Date(`${selected.dateOfBirth}T00:00:00`).toLocaleDateString("vi-VN") : "Chưa cập nhật"],
                        ["Đăng nhập gần nhất", formatDate(selected.lastLogin)],
                        ...(permissionsForRole(selected.role).length > 0 ? [["Quyền cấp phát", permissionSummary(selected)]] : []),
                        ["Đổi mật khẩu", selected.mustChangePassword ? "Bắt buộc khi đăng nhập lại" : "Đã hoàn tất"],
                      ].map(([lbl, val]) => (
                        <div key={lbl}>
                          <dt>{lbl}</dt>
                          <dd>{val}</dd>
                        </div>
                      ))}
                    </dl>
                  </Card>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      </div>
    </div>
  );
}