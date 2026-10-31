import { useCallback, useEffect, useMemo, useState } from "react";
import { Badge, Button, Callout, Card, Progress, Select, Table, TextField } from "@radix-ui/themes";
import { Link } from "react-router-dom";
import { downloadSchoolClassesCsv, schoolReportClasses, schoolReportSummary, schoolReportTokenOptions, schoolReportTokenPage, type SchoolReportClass, type SchoolReportSummary, type SchoolTokenOptions, type SchoolTokenPage } from "../api/schoolApi";
import { apiMessage } from "../../../shared/lib/apiError";
import LearningIcon from "../../../shared/ui/LearningIcon";
import { useSessionStore } from "../../../shared/auth/sessionStore";
import { DashboardLoadingSkeleton, TableLoadingSkeleton } from "../../../shared/ui/LoadingSkeleton";
import s from "./SchoolReports.module.css";

const number = (value: number) => value.toLocaleString("vi-VN");
const monthLabel = (value: string) => { const [year, month] = value.split("-"); return `Tháng ${Number(month)}/${year}`; };
const dateLabel = (value: string | null) => value ? new Date(`${value}T00:00:00`).toLocaleDateString("vi-VN") : "Chưa thiết lập";
const operations: Record<string, string> = { AI_GENERATION: "Tạo mô phỏng bằng AI", ROUTE: "Phân loại chủ đề", MODEL: "Phân tích bài toán", OCR: "Đọc nội dung từ ảnh", SCENE: "Tạo cảnh mô phỏng" };
const operationLabel = (value: string) => operations[value] ?? value;
const licenceLabels: Record<SchoolReportSummary["licenseStatus"], string> = { ACTIVE: "Đang hiệu lực", EXPIRED: "Đã hết hạn", UPCOMING: "Chưa đến hạn sử dụng", UNLICENSED: "Chưa có bản quyền", SUSPENDED: "Trường tạm khóa" };

function SummaryCard({ label, value, detail, icon }: Readonly<{ label: string; value: number; detail: string; icon: "users" | "book" | "activity" | "chart" }>) {
  return <Card size="3" className="admin-stat-card"><div className="admin-stat-icon blue"><LearningIcon name={icon} /></div><p>{label}</p><strong>{number(value)}</strong><small>{detail}</small></Card>;
}

export default function SchoolReports() {
  const schoolId = useSessionStore(state => state.user?.schoolId);
  const [summary, setSummary] = useState<SchoolReportSummary | null>(null);
  const [classes, setClasses] = useState<SchoolReportClass[]>([]);
  const [options, setOptions] = useState<SchoolTokenOptions>({ operations: [], months: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [exporting, setExporting] = useState(false);
  const [classQuery, setClassQuery] = useState("");
  const [classGrade, setClassGrade] = useState("ALL");
  const [classYear, setClassYear] = useState("ALL");
  const [classPage, setClassPage] = useState(0);
  const [tokenQuery, setTokenQuery] = useState("");
  const [tokenOperation, setTokenOperation] = useState("ALL");
  const [tokenMonth, setTokenMonth] = useState("ALL");
  const [tokenPage, setTokenPage] = useState(0);
  const [tokenSize, setTokenSize] = useState(10);
  const [tokenResult, setTokenResult] = useState<SchoolTokenPage | null>(null);
  const [tokenLoading, setTokenLoading] = useState(true);
  const [tokenError, setTokenError] = useState("");
  const [tokenRefresh, setTokenRefresh] = useState(0);

  const load = useCallback(async () => {
    if (!schoolId) return;
    setLoading(true); setError("");
    try {
      const [nextSummary, nextClasses, nextOptions] = await Promise.all([schoolReportSummary(schoolId), schoolReportClasses(schoolId), schoolReportTokenOptions(schoolId)]);
      setSummary(nextSummary); setClasses(nextClasses); setOptions(nextOptions);
    } catch (cause) { setError(apiMessage(cause, "Không thể tải báo cáo trường.")); }
    finally { setLoading(false); }
  }, [schoolId]);
  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    if (!schoolId) return;
    const controller = new AbortController();
    setTokenLoading(true); setTokenError("");
    const timer = window.setTimeout(() => {
      void schoolReportTokenPage(schoolId, { page: tokenPage, size: tokenSize, query: tokenQuery.trim() || undefined,
        operation: tokenOperation === "ALL" ? undefined : tokenOperation, month: tokenMonth === "ALL" ? undefined : tokenMonth }, controller.signal)
        .then(result => { if (!controller.signal.aborted) setTokenResult(result); })
        .catch(cause => { if (!controller.signal.aborted) setTokenError(apiMessage(cause, "Không thể tải lịch sử token.")); })
        .finally(() => { if (!controller.signal.aborted) setTokenLoading(false); });
    }, 250);
    return () => { window.clearTimeout(timer); controller.abort(); };
  }, [schoolId, tokenPage, tokenSize, tokenQuery, tokenOperation, tokenMonth, tokenRefresh]);

  const years = useMemo(() => [...new Set(classes.map(item => item.schoolYear))].sort().reverse(), [classes]);
  const filteredClasses = useMemo(() => classes.filter(item => item.name.toLocaleLowerCase("vi").includes(classQuery.trim().toLocaleLowerCase("vi"))
    && (classGrade === "ALL" || item.gradeLevel === Number(classGrade)) && (classYear === "ALL" || item.schoolYear === classYear)), [classes, classQuery, classGrade, classYear]);
  const currentClassPage = Math.min(classPage, Math.max(0, Math.ceil(filteredClasses.length / 10) - 1));
  const visibleClasses = filteredClasses.slice(currentClassPage * 10, currentClassPage * 10 + 10);
  const hasTokenFilters = !!tokenQuery.trim() || tokenOperation !== "ALL" || tokenMonth !== "ALL";

  const exportClasses = async () => {
    if (!schoolId || exporting) return;
    setExporting(true); setError(""); setNotice("");
    try {
      await downloadSchoolClassesCsv(schoolId, { query: classQuery.trim() || undefined, grade: classGrade === "ALL" ? undefined : Number(classGrade), year: classYear === "ALL" ? undefined : classYear });
      setNotice("Đã xuất CSV các lớp phù hợp với bộ lọc.");
    } catch (cause) { setError(apiMessage(cause, "Không thể xuất danh sách lớp.")); }
    finally { setExporting(false); }
  };

  if (!schoolId) return null;
  return <div className={`admin-content ${s.reports}`}>
    <div className={s.reportToolbar}><span>Số liệu tài khoản và lớp đang hoạt động</span><Button variant="outline" disabled={loading || tokenLoading} onClick={() => { void load(); setTokenRefresh(value => value + 1); }}><LearningIcon name="refresh" />Làm mới</Button></div>
    {error && <Callout.Root color="red" role="alert"><Callout.Text>{error}</Callout.Text><Button variant="soft" onClick={() => void load()}>Thử lại</Button></Callout.Root>}
    {notice && <Callout.Root color="green" role="status"><Callout.Text>{notice}</Callout.Text></Callout.Root>}
    {loading && !summary ? <DashboardLoadingSkeleton cards={4} label="Đang tải báo cáo…" /> : summary && <>
      <section className={`admin-stats-grid ${s.summary}`} aria-label="Tổng quan trường">
        <SummaryCard label="Học sinh đang hoạt động" value={summary.students} detail={`${number(summary.enrolledStudents)} học sinh đã xếp lớp`} icon="users" />
        <SummaryCard label="Giáo viên đang hoạt động" value={summary.teachers} detail="Tài khoản thuộc trường" icon="book" />
        <SummaryCard label="Lớp đang hoạt động" value={summary.activeClasses} detail="Tất cả năm học" icon="activity" />
        <SummaryCard label="Token AI tháng này" value={summary.usedTokens} detail={`${monthLabel(summary.usageMonth)} · ${summary.tokenQuota == null ? "Không giới hạn" : `Hạn mức ${number(summary.tokenQuota)}`}`} icon="chart" />
      </section>

      <Card size="3" asChild><section className={`admin-panel ${s.licencePanel}`} aria-label="Bản quyền và hạn mức">
        <div className="admin-panel-heading"><div><h2>Bản quyền và hạn mức</h2><p className="admin-panel-description">Gói {summary.planCode || "chưa được thiết lập"}</p></div><Badge color={summary.licenseStatus === "ACTIVE" ? "green" : "orange"}>{licenceLabels[summary.licenseStatus]}</Badge></div>
        <div className={s.licenceGrid}>
          <div><span>Thời hạn bản quyền</span><strong>{dateLabel(summary.licenseStart)} – {dateLabel(summary.licenseEnd)}</strong><Button asChild size="1" variant="soft"><Link to="/school/billing">Xem gói / gia hạn</Link></Button></div>
          <div><span>Hạn mức học sinh</span><strong>{number(summary.students)} / {summary.studentQuota == null ? "Không giới hạn" : number(summary.studentQuota)}</strong>{summary.studentQuota != null && <><Progress value={summary.studentQuota > 0 ? Math.min(100, summary.students / summary.studentQuota * 100) : summary.students > 0 ? 100 : 0} color={summary.students >= summary.studentQuota ? "orange" : "indigo"} aria-label="Mức sử dụng hạn mức học sinh" /><small>{summary.students > summary.studentQuota ? `Vượt ${number(summary.students - summary.studentQuota)} học sinh` : `Còn ${number(summary.studentQuota - summary.students)} chỗ`}</small></>}</div>
          <div><span>Token AI · {monthLabel(summary.usageMonth)}</span><strong>{number(summary.usedTokens)} / {summary.tokenQuota == null ? "Không giới hạn" : number(summary.tokenQuota)}</strong>{summary.tokenQuota != null && <Progress value={summary.tokenQuota > 0 ? Math.min(100, summary.usedTokens / summary.tokenQuota * 100) : summary.usedTokens > 0 ? 100 : 0} aria-label="Mức sử dụng token AI tháng này" />}<small>Hạn mức token được tính lại mỗi tháng.</small></div>
        </div>
      </section></Card>

      <Card size="3" asChild><section className={`admin-panel ${s.dataPanel}`}>
        <div className="admin-panel-heading"><div><h2 id="report-classes">Thống kê lớp học</h2><p className="admin-panel-description">{number(filteredClasses.length)} lớp đang hoạt động phù hợp bộ lọc. CSV xuất toàn bộ kết quả đã lọc.</p></div><Button variant="soft" loading={exporting} disabled={exporting || loading || !filteredClasses.length} onClick={() => void exportClasses()}><LearningIcon name="download" />Xuất CSV</Button></div>
        <div className={s.classToolbar}>
          <TextField.Root value={classQuery} onChange={event => { setClassQuery(event.target.value); setClassPage(0); }} placeholder="Tìm tên lớp…" aria-label="Tìm lớp trong báo cáo"><TextField.Slot><LearningIcon name="search" /></TextField.Slot></TextField.Root>
          <Select.Root value={classGrade} onValueChange={value => { setClassGrade(value); setClassPage(0); }}><Select.Trigger aria-label="Khối lớp báo cáo" /><Select.Content position="popper"><Select.Item value="ALL">Tất cả khối</Select.Item>{[10, 11, 12].map(grade => <Select.Item key={grade} value={String(grade)}>Khối {grade}</Select.Item>)}</Select.Content></Select.Root>
          <Select.Root value={classYear} onValueChange={value => { setClassYear(value); setClassPage(0); }}><Select.Trigger aria-label="Năm học báo cáo" /><Select.Content position="popper"><Select.Item value="ALL">Tất cả năm học</Select.Item>{years.map(year => <Select.Item key={year} value={year}>{year}</Select.Item>)}</Select.Content></Select.Root>
        </div>
        <div className={s.tableScroll} role="region" aria-labelledby="report-classes" tabIndex={0}><Table.Root size="2" variant="surface"><Table.Header><Table.Row>{["Lớp", "Khối", "Năm học", "Giáo viên", "Học sinh"].map(label => <Table.ColumnHeaderCell key={label}>{label}</Table.ColumnHeaderCell>)}</Table.Row></Table.Header><Table.Body>{visibleClasses.length ? visibleClasses.map(item => <Table.Row key={item.id}><Table.RowHeaderCell>{item.name}</Table.RowHeaderCell><Table.Cell><Badge>{item.gradeLevel}</Badge></Table.Cell><Table.Cell>{item.schoolYear}</Table.Cell><Table.Cell>{item.teachers}</Table.Cell><Table.Cell>{item.students}</Table.Cell></Table.Row>) : <Table.Row><Table.Cell colSpan={5}>Không có lớp phù hợp với bộ lọc.</Table.Cell></Table.Row>}</Table.Body></Table.Root></div>
        <Paging page={currentClassPage} size={10} total={filteredClasses.length} onPage={setClassPage} />
      </section></Card>

      <Card size="3" asChild><section className={`admin-panel ${s.dataPanel}`}>
        <div className="admin-panel-heading"><div><h2 id="report-token-history">Lịch sử sử dụng AI</h2><p className="admin-panel-description">Tra cứu toàn bộ lịch sử theo tháng. Bộ lọc này chỉ áp dụng cho bảng bên dưới.</p></div></div>
        <div className={s.tokenToolbar}>
          <TextField.Root size="3" value={tokenQuery} onChange={event => { setTokenQuery(event.target.value); setTokenPage(0); }} placeholder="Tìm theo email tài khoản…" aria-label="Tìm lịch sử token AI"><TextField.Slot><LearningIcon name="search" /></TextField.Slot></TextField.Root>
          <Select.Root size="3" value={tokenOperation} onValueChange={value => { setTokenOperation(value); setTokenPage(0); }}><Select.Trigger aria-label="Lọc theo hoạt động" /><Select.Content position="popper"><Select.Item value="ALL">Tất cả hoạt động</Select.Item>{options.operations.map(operation => <Select.Item key={operation} value={operation}>{operationLabel(operation)}</Select.Item>)}</Select.Content></Select.Root>
          <Select.Root size="3" value={tokenMonth} onValueChange={value => { setTokenMonth(value); setTokenPage(0); }}><Select.Trigger aria-label="Lọc theo tháng" /><Select.Content position="popper"><Select.Item value="ALL">Tất cả tháng</Select.Item>{options.months.map(month => <Select.Item key={month} value={month.slice(0, 7)}>{monthLabel(month)}</Select.Item>)}</Select.Content></Select.Root>
          <Button size="3" variant="soft" color="gray" disabled={!hasTokenFilters} onClick={() => { setTokenQuery(""); setTokenOperation("ALL"); setTokenMonth("ALL"); setTokenPage(0); }}>Xóa bộ lọc</Button>
        </div>
        {tokenError && <Callout.Root color="red" role="alert"><Callout.Text>{tokenError}</Callout.Text><Button variant="soft" onClick={() => setTokenRefresh(value => value + 1)}>Thử lại lịch sử</Button></Callout.Root>}
        {tokenLoading && !tokenResult ? <TableLoadingSkeleton headers={["Thời điểm", "Tài khoản", "Hoạt động", "Tháng", "Token"]} label="Đang tải lịch sử sử dụng AI…" /> : !tokenError && <>
          <p className={s.loadingStatus} role="status">{tokenLoading ? "Đang cập nhật lịch sử…" : `${number(tokenResult?.totalElements ?? 0)} lượt sử dụng${tokenMonth !== "ALL" ? ` · ${monthLabel(tokenMonth)}` : " · Tất cả tháng"}`}</p>
          <div className={s.tableScroll} role="region" aria-labelledby="report-token-history" tabIndex={0} aria-busy={tokenLoading}><Table.Root size="2" variant="surface"><Table.Header><Table.Row>{["Thời điểm", "Tài khoản", "Hoạt động", "Tháng", "Token"].map(label => <Table.ColumnHeaderCell key={label}>{label}</Table.ColumnHeaderCell>)}</Table.Row></Table.Header><Table.Body>{tokenResult?.items.length ? tokenResult.items.map((item, index) => <Table.Row key={`${item.recordedAt}-${item.userEmail}-${index}`}><Table.Cell>{new Date(item.recordedAt).toLocaleString("vi-VN")}</Table.Cell><Table.Cell>{item.userEmail}</Table.Cell><Table.Cell>{operationLabel(item.operation)}</Table.Cell><Table.Cell>{monthLabel(item.usageMonth)}</Table.Cell><Table.Cell>{number(item.tokens)}</Table.Cell></Table.Row>) : <Table.Row><Table.Cell colSpan={5}>Không có lượt sử dụng phù hợp với bộ lọc.</Table.Cell></Table.Row>}</Table.Body></Table.Root></div>
          <div className={s.tokenFooter}><Select.Root value={String(tokenSize)} onValueChange={value => { setTokenSize(Number(value)); setTokenPage(0); }}><Select.Trigger aria-label="Số lượt sử dụng trên mỗi trang" /><Select.Content position="popper">{[10, 25, 50, 100].map(size => <Select.Item key={size} value={String(size)}>{size} dòng/trang</Select.Item>)}</Select.Content></Select.Root><Paging page={tokenResult?.page ?? 0} size={tokenSize} total={tokenResult?.totalElements ?? 0} onPage={setTokenPage} disabled={tokenLoading} /></div>
        </>}
      </section></Card>
    </>}
  </div>;
}

function Paging({ page, size, total, onPage, disabled = false }: Readonly<{ page: number; size: number; total: number; onPage: (page: number) => void; disabled?: boolean }>) {
  const pages = Math.max(1, Math.ceil(total / size));
  return <div className={s.reportPaging}><span>{total ? `Hiển thị ${number(page * size + 1)}–${number(Math.min((page + 1) * size, total))} / ${number(total)}` : "0 kết quả"}</span><div><Button size="1" variant="soft" color="gray" disabled={disabled || page === 0} onClick={() => onPage(page - 1)}>Trước</Button><span>Trang {page + 1}/{pages}</span><Button size="1" variant="soft" color="gray" disabled={disabled || page + 1 >= pages} onClick={() => onPage(page + 1)}>Sau</Button></div></div>;
}
