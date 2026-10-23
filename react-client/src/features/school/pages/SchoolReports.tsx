import { useCallback, useEffect, useMemo, useState } from "react";
import { Badge, Button, Card, Select, Table, TextField } from "@radix-ui/themes";
import { motion, useReducedMotion } from "motion/react";
import { downloadSchoolClassesCsv, schoolReportClasses, schoolReportSummary, schoolReportTokenAudit, type SchoolReportClass, type SchoolReportSummary, type SchoolTokenAudit } from "../api/schoolApi";
import { apiMessage } from "../../../shared/lib/apiError";
import LearningIcon from "../../../shared/ui/LearningIcon";
import { useSessionStore } from "../../../shared/auth/sessionStore";
import s from "./SchoolReports.module.css";
import { DashboardLoadingSkeleton } from "../../../shared/ui/LoadingSkeleton";

const TOKEN_PAGE_SIZES = [10, 25, 50, 100] as const;

function formatUsageMonth(value: string) {
  const match = /^(\d{4})-(\d{2})/.exec(value);
  return match ? `Tháng ${Number(match[2])}/${match[1]}` : value;
}

function ReportSummaryCard({ label, value, detail, icon, tone }: Readonly<{ label: string; value: string; detail: string; icon: "users" | "book" | "activity" | "chart"; tone: string }>) {
  const reducedMotion = useReducedMotion();
  return <Card size="3" asChild><motion.article className="admin-stat-card" initial={reducedMotion ? false : { opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: reducedMotion ? 0 : .18 }}><div className={`admin-stat-icon ${tone}`}><LearningIcon name={icon} /></div><p>{label}</p><strong>{value}</strong><small>{detail}</small></motion.article></Card>;
}

export default function SchoolReports() {
  const reducedMotion = useReducedMotion();
  const schoolId = useSessionStore((state) => state.user?.schoolId);
  const [summary, setSummary] = useState<SchoolReportSummary | null>(null);
  const [classes, setClasses] = useState<SchoolReportClass[]>([]);
  const [tokens, setTokens] = useState<SchoolTokenAudit[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [tokenQuery, setTokenQuery] = useState("");
  const [tokenOperation, setTokenOperation] = useState("ALL");
  const [tokenMonth, setTokenMonth] = useState("ALL");
  const [tokenPageSize, setTokenPageSize] = useState<number>(25);
  const [tokenPage, setTokenPage] = useState(1);

  const load = useCallback(async () => {
    if (!schoolId) return;
    setLoading(true);
    setError("");
    try {
      const [nextSummary, nextClasses, nextTokens] = await Promise.all([
        schoolReportSummary(schoolId),
        schoolReportClasses(schoolId),
        schoolReportTokenAudit(schoolId),
      ]);
      setSummary(nextSummary);
      setClasses(nextClasses);
      setTokens(nextTokens);
    } catch (err) {
      setError(apiMessage(err, "Không thể tải báo cáo trường."));
    } finally {
      setLoading(false);
    }
  }, [schoolId]);

  useEffect(() => {
    const timer = window.setTimeout(() => { void load(); }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const exportClasses = async () => {
    if (!schoolId || exporting) return;
    setExporting(true);
    setError("");
    try {
      await downloadSchoolClassesCsv(schoolId);
    } catch (err) {
      setError(apiMessage(err, "Không thể xuất danh sách lớp."));
    } finally {
      setExporting(false);
    }
  };

  const tokenOperations = useMemo(
    () => [...new Set(tokens.map((item) => item.operation))].sort((a, b) => a.localeCompare(b)),
    [tokens],
  );
  const tokenMonths = useMemo(
    () => [...new Set(tokens.map((item) => item.usageMonth))].sort((a, b) => b.localeCompare(a)),
    [tokens],
  );
  const filteredTokens = useMemo(() => {
    const query = tokenQuery.trim().toLocaleLowerCase("vi");
    return tokens.filter((item) => {
      const matchesQuery = !query
        || item.userEmail.toLocaleLowerCase("vi").includes(query)
        || item.operation.toLocaleLowerCase("vi").includes(query);
      return matchesQuery
        && (tokenOperation === "ALL" || item.operation === tokenOperation)
        && (tokenMonth === "ALL" || item.usageMonth === tokenMonth);
    });
  }, [tokenMonth, tokenOperation, tokenQuery, tokens]);

  useEffect(() => {
    setTokenPage(1);
  }, [tokenMonth, tokenOperation, tokenPageSize, tokenQuery]);

  const tokenPageCount = Math.max(1, Math.ceil(filteredTokens.length / tokenPageSize));
  const currentTokenPage = Math.min(tokenPage, tokenPageCount);
  const tokenPageStart = (currentTokenPage - 1) * tokenPageSize;
  const visibleTokens = filteredTokens.slice(tokenPageStart, tokenPageStart + tokenPageSize);
  const hasTokenFilters = Boolean(tokenQuery.trim()) || tokenOperation !== "ALL" || tokenMonth !== "ALL";

  const clearTokenFilters = () => {
    setTokenQuery("");
    setTokenOperation("ALL");
    setTokenMonth("ALL");
  };

  if (!schoolId) return null;

  return <div className={`admin-content school-reports ${s.reports}`}>
    <header className="admin-content-header"><div><h1 className="admin-content-title">Báo cáo</h1></div></header>
    {error && <div className="admin-error-banner" role="alert">{error}<Button variant="soft" color="red" onClick={() => void load()}>Thử lại</Button></div>}
    {loading && !summary ? <DashboardLoadingSkeleton cards={4} label="Đang tải báo cáo…" /> : summary && <>
      <section className={`admin-stats-grid school-summary-grid ${s.summary}`} aria-label="Tổng quan trường">
        <ReportSummaryCard label="Học sinh" value={summary.students.toLocaleString("vi-VN")} detail={`${summary.enrolledStudents.toLocaleString("vi-VN")} lượt xếp lớp`} icon="users" tone="blue" />
        <ReportSummaryCard label="Giáo viên" value={summary.teachers.toLocaleString("vi-VN")} detail="Tài khoản thuộc trường" icon="book" tone="purple" />
        <ReportSummaryCard label="Lớp hoạt động" value={summary.activeClasses.toLocaleString("vi-VN")} detail="Đang được sử dụng" icon="activity" tone="green" />
        <ReportSummaryCard label="Token đã dùng" value={summary.usedTokens.toLocaleString("vi-VN")} detail={summary.tokenQuota == null ? "Không giới hạn" : `Quota ${summary.tokenQuota.toLocaleString("vi-VN")}/tháng`} icon="chart" tone="orange" />
      </section>

      <motion.div style={{ display: "grid", gap: 20, minWidth: 0 }} initial={reducedMotion ? false : { opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: reducedMotion ? 0 : .18 }}>
      <Card size="3" asChild><section className={`admin-panel ${s.dataPanel}`}>
        <div className="admin-panel-heading"><div><h2>Danh sách lớp</h2></div><Button variant="soft" onClick={() => void exportClasses()} loading={exporting} disabled={exporting || loading}><LearningIcon name="download" />{exporting ? "Đang xuất…" : "Xuất CSV"}</Button></div>
        <div className="admin-table-scroll"><Table.Root size="2" variant="surface"><Table.Header><Table.Row><Table.ColumnHeaderCell>Lớp</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Khối</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Năm học</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Giáo viên</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Học sinh</Table.ColumnHeaderCell></Table.Row></Table.Header><Table.Body>{classes.length === 0 ? <Table.Row><Table.Cell colSpan={5}>Chưa có lớp đang hoạt động.</Table.Cell></Table.Row> : classes.map(item => <Table.Row key={item.id}><Table.RowHeaderCell>{item.name}</Table.RowHeaderCell><Table.Cell><Badge color="indigo" variant="soft">{item.gradeLevel}</Badge></Table.Cell><Table.Cell>{item.schoolYear}</Table.Cell><Table.Cell>{item.teachers}</Table.Cell><Table.Cell>{item.students}</Table.Cell></Table.Row>)}</Table.Body></Table.Root></div>
      </section></Card>

      <Card size="3" asChild><section className={`admin-panel ${s.dataPanel}`}>
        <div className="admin-panel-heading"><div><h2>Lịch sử token AI</h2><p className="admin-panel-description">Tối đa 200 lượt sử dụng gần nhất của trường.</p></div></div>

        <div className={s.tokenToolbar}>
          <TextField.Root className={s.tokenSearch} type="search" size="3" value={tokenQuery} onChange={(event) => setTokenQuery(event.target.value)} placeholder="Tìm theo tài khoản hoặc hoạt động..." aria-label="Tìm lịch sử token AI">
            <TextField.Slot><LearningIcon name="search" /></TextField.Slot>
          </TextField.Root>

          <Select.Root size="3" value={tokenOperation} onValueChange={setTokenOperation}>
            <Select.Trigger className={s.tokenSelect} aria-label="Lọc theo hoạt động" />
            <Select.Content>
              <Select.Item value="ALL">Tất cả hoạt động</Select.Item>
              {tokenOperations.map((operation) => <Select.Item key={operation} value={operation}>{operation}</Select.Item>)}
            </Select.Content>
          </Select.Root>

          <Select.Root size="3" value={tokenMonth} onValueChange={setTokenMonth}>
            <Select.Trigger className={s.tokenSelect} aria-label="Lọc theo tháng" />
            <Select.Content>
              <Select.Item value="ALL">Tất cả tháng</Select.Item>
              {tokenMonths.map((month) => <Select.Item key={month} value={month}>{formatUsageMonth(month)}</Select.Item>)}
            </Select.Content>
          </Select.Root>

          <Button className={s.clearFilters} type="button" size="3" variant="soft" color="gray" disabled={!hasTokenFilters} onClick={clearTokenFilters}>Xóa bộ lọc</Button>
        </div>

        <div className="admin-table-scroll">
          <Table.Root size="2" variant="surface">
            <Table.Header><Table.Row><Table.ColumnHeaderCell>Thời điểm</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Tài khoản</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Hoạt động</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Tháng</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Token</Table.ColumnHeaderCell></Table.Row></Table.Header>
            <Table.Body>
              {tokens.length === 0
                ? <Table.Row><Table.Cell colSpan={5}>Chưa có dữ liệu sử dụng.</Table.Cell></Table.Row>
                : visibleTokens.length === 0
                  ? <Table.Row><Table.Cell colSpan={5}>Không có lượt sử dụng phù hợp với bộ lọc.</Table.Cell></Table.Row>
                  : visibleTokens.map((item, index) => <Table.Row key={`${item.recordedAt}-${item.userEmail}-${tokenPageStart + index}`}><Table.Cell>{new Date(item.recordedAt).toLocaleString("vi-VN")}</Table.Cell><Table.Cell>{item.userEmail}</Table.Cell><Table.Cell>{item.operation}</Table.Cell><Table.Cell>{formatUsageMonth(item.usageMonth)}</Table.Cell><Table.Cell>{item.tokens.toLocaleString("vi-VN")}</Table.Cell></Table.Row>)}
            </Table.Body>
          </Table.Root>
        </div>

        <div className={s.tokenFooter}>
          <p aria-live="polite">{filteredTokens.length === 0 ? "0 kết quả" : `Hiển thị ${tokenPageStart + 1}–${Math.min(tokenPageStart + tokenPageSize, filteredTokens.length)} / ${filteredTokens.length} lượt`}</p>
          <div className={s.tokenPagingControls}>
            <div className={s.pageSizeControl}>
              <span>Số dòng/trang</span>
              <Select.Root size="2" value={String(tokenPageSize)} onValueChange={(value) => setTokenPageSize(Number(value))}>
                <Select.Trigger aria-label="Số dòng trên mỗi trang" />
                <Select.Content>{TOKEN_PAGE_SIZES.map((size) => <Select.Item key={size} value={String(size)}>{size}</Select.Item>)}</Select.Content>
              </Select.Root>
            </div>
            <div className={s.pagination}>
              <Button type="button" size="2" variant="soft" color="gray" disabled={currentTokenPage <= 1} onClick={() => setTokenPage((page) => Math.max(1, page - 1))}>Trước</Button>
              <span>Trang {currentTokenPage}/{tokenPageCount}</span>
              <Button type="button" size="2" variant="soft" color="gray" disabled={currentTokenPage >= tokenPageCount} onClick={() => setTokenPage((page) => Math.min(tokenPageCount, page + 1))}>Sau</Button>
            </div>
          </div>
        </div>
      </section></Card>
      </motion.div>
    </>}
  </div>;
}
