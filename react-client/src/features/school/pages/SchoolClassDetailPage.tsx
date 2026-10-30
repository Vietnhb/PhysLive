import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { Badge, Button, Card, Popover, Select, Table, TextField } from "@radix-ui/themes";
import { ArrowLeftIcon, CheckIcon, ChevronDownIcon, MagnifyingGlassIcon, Pencil1Icon } from "@radix-ui/react-icons";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { adminUsers } from "../../account/api/userApi";
import {
  assignClassTeacher,
  enrollClassStudent,
  removeClassStudent,
  removeClassTeacher,
  schoolClass,
  schoolClasses,
  transferClassStudent,
  updateSchoolClass,
} from "../api/schoolApi";
import type { SchoolClassDetail, SchoolClassRequest, SchoolClassSummary } from "../types";
import type { User } from "../../../shared/auth/types";
import { useSessionStore } from "../../../shared/auth/sessionStore";
import { apiMessage } from "../../../shared/lib/apiError";
import { usePageBreadcrumb } from "../../../shared/layout/PageBreadcrumbContext";
import { ClassDetailLoading } from "../components/SchoolLoading";
import s from "../components/SchoolClasses.module.css";

type Option = { value: string; label: string };
type Feedback = { type: "success" | "error"; message: string };
const searchable = (value: string) => value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("vi");

function AccountPicker({ value, disabled, label, placeholder, emptyLabel, options, onValueChange }: Readonly<{ value?: string; disabled?: boolean; label: string; placeholder: string; emptyLabel: string; options: Option[]; onValueChange: (value: string) => void }>) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const selected = options.find(option => option.value === value);
  const needle = searchable(query.trim());
  const filtered = needle ? options.filter(option => searchable(option.label).includes(needle)) : options;
  return <Popover.Root open={open} onOpenChange={next => { setOpen(next); if (!next) setQuery(""); }}>
    <Popover.Trigger><Button type="button" variant="surface" color="gray" className="admin-search-picker-trigger" disabled={disabled} aria-label={label} aria-expanded={open}><span>{selected?.label ?? placeholder}</span><ChevronDownIcon /></Button></Popover.Trigger>
    <Popover.Content className="admin-search-picker-content" align="start" sideOffset={6} collisionPadding={16}>
      <TextField.Root autoFocus type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="Tìm theo tên hoặc email…" aria-label={`Tìm trong ${label.toLocaleLowerCase("vi")}`}><TextField.Slot><MagnifyingGlassIcon /></TextField.Slot></TextField.Root>
      <div className="admin-search-picker-options" role="listbox" aria-label={label}>{filtered.length === 0 ? <p>{emptyLabel}</p> : filtered.map(option => <button type="button" role="option" aria-selected={option.value === value} key={option.value} onClick={() => { onValueChange(option.value); setOpen(false); setQuery(""); }}><span>{option.label}</span>{option.value === value && <CheckIcon />}</button>)}</div>
      <small>{filtered.length}/{options.length} tài khoản</small>
    </Popover.Content>
  </Popover.Root>;
}

export default function SchoolClassDetailPage() {
  const navigate = useNavigate();
  const { classId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const editMode = searchParams.get("mode") === "edit";
  const schoolId = useSessionStore(state => state.user?.schoolId);
  const setPageBreadcrumb = usePageBreadcrumb();
  const [detail, setDetail] = useState<SchoolClassDetail | null>(null);
  const [classes, setClasses] = useState<SchoolClassSummary[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [form, setForm] = useState<SchoolClassRequest | null>(null);
  const [teacherId, setTeacherId] = useState("");
  const [studentId, setStudentId] = useState("");
  const [studentPageSize, setStudentPageSize] = useState(10);
  const [studentPage, setStudentPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState<Feedback | null>(null);

  const load = useCallback(async (showSkeleton = true) => {
    if (!schoolId || !classId) return;
    if (showSkeleton) setLoading(true);
    setError("");
    try {
      const [nextDetail, nextClasses, nextUsers] = await Promise.all([
        schoolClass(schoolId, classId),
        schoolClasses(schoolId),
        editMode ? adminUsers(schoolId) : Promise.resolve([]),
      ]);
      setDetail(nextDetail);
      setClasses(nextClasses);
      setUsers(nextUsers);
      setForm({ name: nextDetail.name, gradeLevel: nextDetail.gradeLevel, schoolYear: nextDetail.schoolYear, subject: nextDetail.subject ?? "" });
    } catch (err) {
      setError(apiMessage(err, "Không thể tải chi tiết lớp học."));
    } finally {
      if (showSkeleton) setLoading(false);
    }
  }, [classId, editMode, schoolId]);

  useEffect(() => {
    const timer = window.setTimeout(() => { void load(); }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    if (detail?.name) setPageBreadcrumb(detail.name);
    return () => setPageBreadcrumb(undefined);
  }, [detail?.name, setPageBreadcrumb]);

  useEffect(() => {
    if (!feedback) return;
    const timer = window.setTimeout(() => setFeedback(null), 3500);
    return () => window.clearTimeout(timer);
  }, [feedback]);

  const teachers = useMemo(() => users.filter(item => item.role === "STAFF" && item.active !== false), [users]);
  const students = useMemo(() => users.filter(item => item.role === "STUDENT" && item.active !== false), [users]);
  const pageCount = Math.max(1, Math.ceil((detail?.students.length ?? 0) / studentPageSize));
  const currentPage = Math.min(studentPage, pageCount);
  const pageStart = (currentPage - 1) * studentPageSize;
  const visibleStudents = detail?.students.slice(pageStart, pageStart + studentPageSize) ?? [];

  const runAction = async (work: () => Promise<unknown>, successMessage: string) => {
    if (busy) return;
    setBusy(true);
    setError("");
    setFeedback(null);
    try {
      await work();
      await load(false);
      setFeedback({ type: "success", message: successMessage });
    } catch (err) {
      const message = apiMessage(err, "Không thể cập nhật lớp học.");
      setError(message);
      setFeedback({ type: "error", message });
    } finally {
      setBusy(false);
    }
  };

  const saveClass = (event: FormEvent) => {
    event.preventDefault();
    if (!schoolId || !classId || !form) return;
    void runAction(() => updateSchoolClass(schoolId, classId, form), "Đã lưu thông tin lớp học.");
  };

  const changeMode = (editing: boolean) => setSearchParams(editing ? { mode: "edit" } : {}, { replace: true });

  if (!schoolId || !classId) return null;

  return <div className="admin-content">
    <div className={s.pageToolbar}><Button variant="ghost" color="gray" onClick={() => navigate("/school/classes")}><ArrowLeftIcon />Quay lại quản lý lớp học</Button>{editMode && <Button variant="soft" color="gray" onClick={() => changeMode(false)}>Xem chi tiết</Button>}</div>
    {!editMode && <header className="admin-content-header">
      <div><h1 className="admin-content-title">Chi tiết lớp học</h1><p className="admin-content-description">Thông tin lớp, giáo viên và học sinh.</p></div>
      <Button onClick={() => changeMode(true)}><Pencil1Icon />Chỉnh sửa</Button>
    </header>}
    {error && <div className="admin-error-banner" role="alert">{error}<Button variant="soft" color="red" onClick={() => void load()}>Tải lại</Button></div>}
    {feedback && <div className={`${s.feedbackToast} ${feedback.type === "success" ? s.feedbackSuccess : s.feedbackError}`} role={feedback.type === "error" ? "alert" : "status"} aria-live="polite">{feedback.message}</div>}
    {loading && <ClassDetailLoading />}

    {!loading && detail && <Card asChild size="3"><section className="admin-panel">
      {!editMode && <div className="admin-panel-heading"><div><h2>{detail.name}</h2><p className="admin-panel-description">Khối {detail.gradeLevel} · {detail.schoolYear} · {detail.subject || "Chưa cập nhật môn học"}</p></div><Badge color="green" variant="soft">Hoạt động</Badge></div>}

      {editMode && form ? <form className={`admin-form-grid ${s.editForm}`} onSubmit={saveClass}>
        <div className={s.editFormFields}>
          <label><span className={s.fieldLabel}>Tên lớp</span><TextField.Root required maxLength={100} value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} /></label>
          <label><span className={s.fieldLabel}>Khối</span><Select.Root value={String(form.gradeLevel)} onValueChange={value => setForm({ ...form, gradeLevel: Number(value) })}><Select.Trigger /><Select.Content position="popper" side="bottom" align="start" sideOffset={6} className={s.gradeSelectContent}>{[10, 11, 12].map(value => <Select.Item key={value} value={String(value)}>Khối {value}</Select.Item>)}</Select.Content></Select.Root></label>
          <label><span className={s.fieldLabel}>Năm học</span><TextField.Root required maxLength={20} value={form.schoolYear} onChange={event => setForm({ ...form, schoolYear: event.target.value })} /></label>
          <label><span className={s.fieldLabel}>Môn học</span><TextField.Root maxLength={50} value={form.subject} onChange={event => setForm({ ...form, subject: event.target.value })} /></label>
        </div>
        <div className="admin-form-actions"><Button type="submit" loading={busy} disabled={busy}>Lưu thông tin lớp</Button></div>
      </form> : <div className={s.summary}>
        <article><span>Khối</span><strong>{detail.gradeLevel}</strong></article><article><span>Giáo viên</span><strong>{detail.teachers.length}</strong></article><article><span>Học sinh</span><strong>{detail.students.length}</strong></article><article><span>Trạng thái</span><Badge color="green" variant="soft">Hoạt động</Badge></article>
      </div>}

      <section className={`${s.memberSection} ${editMode ? s.editSection : ""}`} aria-labelledby="detail-teachers-heading">
        <div className={s.sectionHeading}><h3 id="detail-teachers-heading">Giáo viên</h3><span>{detail.teachers.length} người</span></div>
        {editMode && <div className="admin-member-add"><AccountPicker value={teacherId} disabled={busy || detail.teachers.length > 0} label="Chọn giáo viên" placeholder="Chọn giáo viên" emptyLabel="Không tìm thấy giáo viên phù hợp." options={teachers.filter(user => !detail.teachers.some(item => item.id === user.id)).map(user => ({ value: String(user.id), label: `${user.fullName} · ${user.email}` }))} onValueChange={setTeacherId} /><Button variant="soft" disabled={!teacherId || busy || detail.teachers.length > 0} onClick={() => void runAction(async () => { await assignClassTeacher(schoolId, detail.id, Number(teacherId)); setTeacherId(""); }, "Đã phân công giáo viên.")}>Phân công</Button></div>}
        {editMode && <p className="admin-panel-description">Mỗi lớp có một giáo viên phụ trách; mỗi giáo viên chỉ dạy một lớp. Để thay giáo viên, hãy gỡ phân công cũ trước.</p>}
        {detail.teachers.length > 1 && <div className="admin-error-banner" role="alert">Lớp có nhiều giáo viên từ phân công cũ. Hãy gỡ các phân công dư để chỉ giữ lại một giáo viên.</div>}
        {detail.teachers.length === 0 ? <p className={s.empty}>Lớp chưa có giáo viên được phân công.</p> : <ul className={editMode ? s.manageList : s.teacherList}>{detail.teachers.map(item => <li key={item.id}>{!editMode && <span className={s.avatar}>{item.fullName.charAt(0).toLocaleUpperCase("vi")}</span>}<div><strong>{item.fullName}</strong><small>{item.email}</small></div>{editMode && <Button size="1" variant="soft" color="red" disabled={busy} onClick={() => void runAction(() => removeClassTeacher(schoolId, detail.id, item.id), "Đã gỡ giáo viên khỏi lớp.")}>Gỡ</Button>}</li>)}</ul>}
      </section>

      <section className={`${s.memberSection} ${editMode ? s.editSection : ""}`} aria-labelledby="detail-students-heading">
        <div className={s.sectionHeading}><h3 id="detail-students-heading">Học sinh</h3><span>{detail.students.length} người</span></div>
        {editMode && <div className="admin-member-add"><AccountPicker value={studentId} disabled={busy} label="Chọn học sinh" placeholder="Chọn học sinh" emptyLabel="Không tìm thấy học sinh phù hợp." options={students.filter(user => !detail.students.some(item => item.id === user.id)).map(user => ({ value: String(user.id), label: `${user.fullName} · ${user.email}` }))} onValueChange={setStudentId} /><Button variant="soft" color="cyan" disabled={!studentId || busy} onClick={() => void runAction(async () => { await enrollClassStudent(schoolId, detail.id, Number(studentId)); setStudentId(""); }, "Đã thêm học sinh vào lớp.")}>Thêm vào lớp</Button></div>}
        {detail.students.length === 0 ? <p className={s.empty}>Lớp chưa có học sinh.</p> : <>
          <div className="admin-table-scroll"><Table.Root size="2" variant="surface"><Table.Header><Table.Row><Table.ColumnHeaderCell>STT</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Họ và tên</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Email</Table.ColumnHeaderCell>{editMode && <Table.ColumnHeaderCell className={s.actionColumn}>Thao tác</Table.ColumnHeaderCell>}</Table.Row></Table.Header><Table.Body>{visibleStudents.map((item, index) => <Table.Row key={item.id}><Table.Cell>{pageStart + index + 1}</Table.Cell><Table.RowHeaderCell>{item.fullName}</Table.RowHeaderCell><Table.Cell>{item.email}</Table.Cell>{editMode && <Table.Cell><div className={s.studentActions}><Select.Root disabled={busy} onValueChange={targetId => { if (targetId) void runAction(() => transferClassStudent(schoolId, targetId, item.id), "Đã chuyển học sinh sang lớp mới."); }}><Select.Trigger className={s.transferSelect} placeholder="Chuyển lớp…" aria-label={`Chuyển lớp cho ${item.fullName}`} /><Select.Content>{classes.filter(candidate => candidate.id !== detail.id && candidate.schoolYear === detail.schoolYear).map(candidate => <Select.Item key={candidate.id} value={candidate.id}>{candidate.name}</Select.Item>)}</Select.Content></Select.Root><Button size="1" variant="soft" color="red" disabled={busy} onClick={() => void runAction(() => removeClassStudent(schoolId, detail.id, item.id), "Đã gỡ học sinh khỏi lớp.")}>Gỡ</Button></div></Table.Cell>}</Table.Row>)}</Table.Body></Table.Root></div>
          <div className={s.studentFooter}><Select.Root size="1" value={String(studentPageSize)} onValueChange={value => { setStudentPageSize(Number(value)); setStudentPage(1); }}><Select.Trigger aria-label="Số học sinh trên mỗi trang" /><Select.Content>{[10, 20, 30].map(size => <Select.Item key={size} value={String(size)}>{size} dòng</Select.Item>)}</Select.Content></Select.Root><div className={`${s.pagination} ${s.compactPagination}`}><span>Hiển thị {pageStart + 1}–{Math.min(pageStart + studentPageSize, detail.students.length)} / {detail.students.length}</span><div><Button size="1" variant="soft" color="gray" disabled={currentPage <= 1} onClick={() => setStudentPage(Math.max(1, currentPage - 1))}>Trước</Button><span>Trang {currentPage}/{pageCount}</span><Button size="1" variant="soft" color="gray" disabled={currentPage >= pageCount} onClick={() => setStudentPage(Math.min(pageCount, currentPage + 1))}>Sau</Button></div></div></div>
        </>}
      </section>
    </section></Card>}
  </div>;
}
