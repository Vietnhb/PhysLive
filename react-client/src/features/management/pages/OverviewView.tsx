import { useEffect, useState } from "react";
import { Badge, Heading, Progress, Text } from "@radix-ui/themes";
import { adminUsers } from "../../account/api/userApi";
import { adminSchools, type ManagedSchool } from "../../school/api/schoolApi";
import { validationMetrics } from "../api/operationsApi";
import type { User } from "../../../shared/auth/types";
import { apiMessage } from "../../../shared/lib/apiError";
import LearningIcon from "../../../shared/ui/LearningIcon";
import { Button, ErrorNotice, PageHeader, Panel, Stat } from "../../../shared/ui/ManagementUI";
import { DashboardLoadingSkeleton } from "../../../shared/ui/LoadingSkeleton";

const SYSTEM_ROLES = new Set(["ADMIN", "MANAGER"]);
const SCHOOL_ROLES = new Set(["SCHOOL", "STAFF"]);

export function OverviewView() {
  const [users, setUsers] = useState<User[]>([]);
  const [schools, setSchools] = useState<ManagedSchool[]>([]);
  const [metrics, setMetrics] = useState<{ total: number; failed: number; failureRate: number } | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const [nextUsers, nextSchools, nextMetrics] = await Promise.all([
        adminUsers(),
        adminSchools(),
        validationMetrics(),
      ]);
      setUsers(nextUsers);
      setSchools(nextSchools);
      setMetrics(nextMetrics);
      setError("");
    } catch (e) {
      setError(apiMessage(e, "Không thể tải tổng quan."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, []);

  const activeUsers = users.filter(item => item.active !== false).length;
  const teachers = users.filter(item => item.role === "STAFF").length;
  const systemAccounts = users.filter(item => SYSTEM_ROLES.has(item.role)).length;
  const schoolAccounts = users.filter(item => SCHOOL_ROLES.has(item.role)).length;
  const individualAccounts = users.length - systemAccounts - schoolAccounts;
  const activeSchools = schools.filter(item => item.active).length;
  const passed = metrics ? metrics.total - metrics.failed : 0;
  const hasValidationData = Boolean(metrics?.total);
  const rate = hasValidationData && metrics ? (passed / metrics.total) * 100 : 0;

  if (loading) return <div className="admin-content"><DashboardLoadingSkeleton cards={4} label="Đang tải tổng quan hệ thống…" /></div>;

  return (
    <div className="admin-content">
      
      {error && <ErrorNotice error={error} onRetry={() => void load()} />}

      <div className="admin-stats-grid">
        <Stat label="Tổng người dùng" value={users.length} icon="users" tone="blue" />
        <Stat label="Đang hoạt động" value={activeUsers} icon="activity" tone="green" note={`${users.length - activeUsers} tài khoản tạm khóa`} />
        <Stat label="Giáo viên" value={teachers} icon="book" tone="orange" />
        <Stat label="Trường học" value={schools.length} icon="grid" tone="purple" note={`${activeSchools} đang hoạt động`} />
      </div>

      <div className="admin-dashboard-grid">
        <Panel title="Kết quả kiểm định" description="Tổng hợp các lượt kiểm tra solver và lời giải tham chiếu.">
          <Heading as="h3" size="8" color="indigo" className="admin-health-value">
            {hasValidationData ? `${rate.toFixed(1)}%` : "—"}
          </Heading>
          <Progress value={Math.max(0, Math.min(100, rate))} color="indigo" aria-label="Tỷ lệ kiểm định đạt" />
          <div className="admin-health-meta">
            <Badge color="green" size="2">{passed} lượt đạt</Badge>
            <Badge color="amber" size="2">{metrics?.failed ?? 0} lượt lỗi</Badge>
          </div>
        </Panel>

        <Panel title="Phân bổ tài khoản" description={`Ba nhóm bao phủ toàn bộ ${users.length} tài khoản.`}>
          <div className="admin-breakdown-list">
            <div><Text>Quản trị hệ thống</Text><Badge color="gray" size="2">{systemAccounts}</Badge></div>
            <div><Text>Trường học</Text><Badge color="gray" size="2">{schoolAccounts}</Badge></div>
            <div><Text>Cá nhân</Text><Badge color="gray" size="2">{individualAccounts}</Badge></div>
          </div>
        </Panel>
      </div>
    </div>
  );
}

