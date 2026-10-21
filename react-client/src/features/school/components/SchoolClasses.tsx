import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { Badge, Button, Card, Dialog, IconButton, Popover, Select, Spinner, Table, TextField } from "@radix-ui/themes";
import { CheckIcon, ChevronDownIcon, Cross2Icon, MagnifyingGlassIcon, PlusIcon } from "@radix-ui/react-icons";
import { motion, useReducedMotion } from "motion/react";
import { adminUsers } from "../../account/api/userApi";
import { archiveSchoolClass, assignClassTeacher, createSchoolClass, enrollClassStudent, removeClassStudent, removeClassTeacher, schoolClass, schoolClasses, transferClassStudent, updateSchoolClass } from "../api/schoolApi";
import type { SchoolClassDetail, SchoolClassRequest, SchoolClassSummary } from "../types";
import type { User } from "../../../shared/auth/types";
import { apiMessage } from "../../../shared/lib/apiError";
import { useSessionStore } from "../../../shared/auth/sessionStore";
import SchoolBulkImport from "../dataio/components/SchoolBulkImport";

const defaultSchoolYear = () => {
  const now = new Date();
  const start = now.getMonth() >= 7 ? now.getFullYear() : now.getFullYear() - 1;
  return `${start}-${start + 1}`;
};
const blank = (): SchoolClassRequest => ({ name: "", gradeLevel: 10, schoolYear: defaultSchoolYear(), subject: "Vật lý" });
type SchoolOption = { value: string; label: string };

const searchableText = (value: string) => value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("vi");

function SchoolSelect({ value, disabled, label, placeholder, options, onValueChange }: Readonly<{ value?: string; disabled?: boolean; label: string; placeholder: string; options: SchoolOption[]; onValueChange: (value: string) => void }>) {
  return <Select.Root size="2" value={value} defaultValue="" disabled={disabled} onValueChange={onValueChange}><Select.Trigger className="admin-school-select" placeholder={placeholder} aria-label={label} /><Select.Content>{options.map(option => <Select.Item key={option.value} value={option.value}>{option.label}</Select.Item>)}</Select.Content></Select.Root>;
}

function SearchableSchoolPicker({ value, disabled, label, placeholder, emptyLabel, options, onValueChange }: Readonly<{ value?: string; disabled?: boolean; label: string; placeholder: string; emptyLabel: string; options: SchoolOption[]; onValueChange: (value: string) => void }>) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const selected = options.find(option => option.value === value);
  const normalizedQuery = searchableText(query.trim());
  const filtered = normalizedQuery ? options.filter(option => searchableText(option.label).includes(normalizedQuery)) : options;

  return <Popover.Root open={open} onOpenChange={next => { setOpen(next); if (!next) setQuery(""); }}>
    <Popover.Trigger>
      <Button type="button" variant="surface" color="gray" className="admin-search-picker-trigger" disabled={disabled} aria-label={label} aria-expanded={open}>
        <span>{selected?.label ?? placeholder}</span><ChevronDownIcon aria-hidden="true" />
      </Button>
    </Popover.Trigger>
    <Popover.Content className="admin-search-picker-content" align="start" sideOffset={6} collisionPadding={16}>
      <TextField.Root autoFocus type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="Tìm theo tên hoặc email…" aria-label={`Tìm trong ${label.toLocaleLowerCase("vi")}`}>
        <TextField.Slot><MagnifyingGlassIcon /></TextField.Slot>
      </TextField.Root>
      <div className="admin-search-picker-options" role="listbox" aria-label={label}>
        {filtered.length === 0 ? <p>{emptyLabel}</p> : filtered.map(option => <button type="button" role="option" aria-selected={option.value === value} key={option.value} onClick={() => { onValueChange(option.value); setOpen(false); setQuery(""); }}><span>{option.label}</span>{option.value === value && <CheckIcon aria-hidden="true" />}</button>)}
      </div>
      <small>{filtered.length}/{options.length} tài khoản</small>
    </Popover.Content>
  </Popover.Root>;
}

export default function SchoolClasses() {
  const reducedMotion = useReducedMotion();
  const nameInputRef = useRef<HTMLInputElement>(null);
  const returnFocusRef = useRef<HTMLButtonElement | null>(null);
  const schoolId = useSessionStore(state => state.user?.schoolId);
  const canCreateClass = useSessionStore(state => state.user?.role === "SCHOOL");
  const [items, setItems] = useState<SchoolClassSummary[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [detail, setDetail] = useState<SchoolClassDetail | null>(null);
  const [form, setForm] = useState<SchoolClassRequest>(blank);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(false);
  const [teacherId, setTeacherId] = useState("");
  const [studentId, setStudentId] = useState("");
  const [studentPageSize, setStudentPageSize] = useState(10);
  const [studentPage, setStudentPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const teachers = useMemo(() => users.filter(item => item.role === "STAFF" && item.active !== false), [users]);
  const students = useMemo(() => users.filter(item => item.role === "STUDENT" && item.active !== false), [users]);
  const studentPageCount = Math.max(1, Math.ceil((detail?.students.length ?? 0) / studentPageSize));
  const currentStudentPage = Math.min(studentPage, studentPageCount);
  const studentPageStart = (currentStudentPage - 1) * studentPageSize;
  const visibleStudents = detail?.students.slice(studentPageStart, studentPageStart + studentPageSize) ?? [];
  const load = useCallback(async () => {
    if (!schoolId) return;
    setLoading(true);
    setError("");
    try {
      const [next, people] = await Promise.all([schoolClasses(schoolId), adminUsers(schoolId)]);
      setItems(next);
      setUsers(people);
    } catch (err) {
      setError(apiMessage(err, "Không thể tải danh sách lớp."));
    } finally {
      setLoading(false);
    }
  }, [schoolId]);
  useEffect(() => {
    const timer = window.setTimeout(() => { void load(); }, 0);
    return () => window.clearTimeout(timer);
  }, [load]);
  const submit = async (event: FormEvent) => {
    event.preventDefault(); if (!schoolId || busy) return; setBusy(true); setFormError("");
    try { const next = editing && detail ? await updateSchoolClass(schoolId, detail.id, form) : await createSchoolClass(schoolId, form); setDetail(next); setStudentPage(1); setEditing(false); setForm(blank()); setShowForm(false); await load(); }
    catch (err) { setFormError(apiMessage(err, "Không thể lưu lớp.")); } finally { setBusy(false); }
  };
  const openCreateForm = () => { setEditing(false); setForm(blank()); setFormError(""); setShowForm(true); };
  const closeForm = () => { if (busy) return; setShowForm(false); setEditing(false); setForm(blank()); setFormError(""); };
  const action = async (work: () => Promise<unknown>) => {
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      await work();
      await load();
      if (schoolId && detail) {
        const refreshed = await schoolClass(schoolId, detail.id).catch(() => null);
        setDetail(refreshed);
      }
    } catch (err) {
      setError(apiMessage(err, "Không thể cập nhật lớp."));
    } finally {
      setBusy(false);
    }
  };
  const refreshImported = async () => {
    await load();
    if (schoolId && detail) setDetail(await schoolClass(schoolId, detail.id));
  };
  if (!schoolId) return null;
  return <div className="admin-content">
    <header className="admin-content-header"><div><h1 className="admin-content-title">Lớp học</h1></div></header>
    {error && <div className="admin-error-banner" role="alert">{error}<Button variant="soft" color="red" onClick={() => void load()}>Tải lại</Button></div>}
    <motion.div className="school-classes-grid" initial={reducedMotion ? false : { opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: reducedMotion ? 0 : .18 }}>
      <Card asChild size="3"><section className="admin-panel">
        <div className="admin-panel-heading"><div><h2>Danh sách lớp</h2></div><div className="school-bulk-tools">{canCreateClass && <><SchoolBulkImport schoolId={schoolId} kind="CLASSES" onImported={refreshImported} /><Button onClick={event => { returnFocusRef.current = event.currentTarget; openCreateForm(); }}><PlusIcon /> Tạo lớp</Button></>}</div></div>
        {loading ? <p role="status"><Spinner /> Đang tải…</p> : <div className="admin-table-scroll"><Table.Root size="2" variant="surface"><Table.Header><Table.Row><Table.ColumnHeaderCell>Lớp</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Khối</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Năm học</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Giáo viên</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Học sinh</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Thao tác</Table.ColumnHeaderCell></Table.Row></Table.Header><Table.Body>
          {items.length === 0 ? <Table.Row><Table.Cell colSpan={6}>{canCreateClass ? "Chưa có lớp. Chọn Tạo lớp hoặc Nhập CSV để bắt đầu." : "Trường chưa có lớp đang hoạt động."}</Table.Cell></Table.Row> : items.map(item => <Table.Row key={item.id} className={detail?.id === item.id ? "admin-row-selected" : ""} style={detail?.id === item.id ? { backgroundColor: "var(--indigo-3)" } : undefined}>
            <Table.RowHeaderCell><Button variant="ghost" onClick={() => void schoolClass(schoolId, item.id).then(next => { setDetail(next); setStudentPage(1); }).catch(err => setError(apiMessage(err, "Không thể tải lớp.")))}>{item.name}</Button></Table.RowHeaderCell>
            <Table.Cell><Badge color="indigo" variant="soft">{item.gradeLevel}</Badge></Table.Cell><Table.Cell>{item.schoolYear}</Table.Cell><Table.Cell>{item.teacherCount}</Table.Cell><Table.Cell>{item.studentCount}</Table.Cell>
            <Table.Cell><Button size="1" variant="soft" color="cyan" mr="2" onClick={event => {
              returnFocusRef.current = event.currentTarget;
              void schoolClass(schoolId, item.id).then(next => { setDetail(next); setStudentPage(1); setEditing(true); setForm({ name: next.name, gradeLevel: next.gradeLevel, schoolYear: next.schoolYear, subject: next.subject ?? "" }); setFormError(""); setShowForm(true); }).catch(err => setError(apiMessage(err, "Không thể tải lớp.")));
            }}>Sửa</Button><Button size="1" variant="soft" color="red" disabled={busy} onClick={() => { if (window.confirm(`Tắt lớp ${item.name}?`)) void action(() => archiveSchoolClass(schoolId, item.id)); }}>Tắt</Button></Table.Cell>
          </Table.Row>)}
        </Table.Body></Table.Root></div>}
      </section></Card>
      {detail && <Card asChild size="3"><motion.section className="admin-panel" initial={reducedMotion ? false : { opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: reducedMotion ? 0 : .18 }}>
        <div className="admin-panel-heading"><div><h2>{detail.name}</h2><p className="admin-panel-description">Khối {detail.gradeLevel} · {detail.schoolYear} · {detail.subject || "Chưa chọn môn"}</p></div><Button variant="ghost" color="gray" onClick={() => setDetail(null)}>Đóng</Button></div>
        <div className="admin-class-members"><div className="admin-panel-heading"><h3>Giáo viên</h3><SchoolBulkImport schoolId={schoolId} kind="TEACHER_ASSIGNMENTS" label="Phân công bằng CSV" disabled={busy} onImported={refreshImported} /></div><div className="admin-member-add"><SearchableSchoolPicker value={teacherId} disabled={busy} label="Chọn giáo viên" placeholder="Chọn giáo viên" emptyLabel="Không tìm thấy giáo viên phù hợp." options={teachers.filter(user => !detail.teachers.some(item => item.id === user.id)).map(user => ({ value: String(user.id), label: `${user.fullName} · ${user.email}` }))} onValueChange={setTeacherId} /><Button variant="soft" disabled={!teacherId || busy} onClick={() => void action(async () => { await assignClassTeacher(schoolId, detail.id, Number(teacherId)); setTeacherId(""); })}>Phân công</Button></div>
          <ul>{detail.teachers.map(item => <li key={item.id}><span>{item.fullName} <small>{item.email}</small></span><Button size="1" variant="soft" color="red" disabled={busy} onClick={() => void action(() => removeClassTeacher(schoolId, detail.id, item.id))}>Gỡ</Button></li>)}</ul>
        </div>
        <div className="admin-class-members"><div className="admin-panel-heading"><h3>Học sinh <Badge color="cyan" ml="2">{detail.students.length}</Badge></h3><SchoolBulkImport schoolId={schoolId} kind="ENROLLMENTS" label="Xếp lớp bằng CSV" disabled={busy} onImported={refreshImported} /></div><div className="admin-member-add"><SearchableSchoolPicker value={studentId} disabled={busy} label="Chọn học sinh" placeholder="Chọn học sinh" emptyLabel="Không tìm thấy học sinh phù hợp." options={students.filter(user => !detail.students.some(item => item.id === user.id)).map(user => ({ value: String(user.id), label: `${user.fullName} · ${user.email}` }))} onValueChange={setStudentId} /><Button variant="soft" color="cyan" disabled={!studentId || busy} onClick={() => void action(async () => { await enrollClassStudent(schoolId, detail.id, Number(studentId)); setStudentId(""); })}>Thêm vào lớp</Button></div>
          <ul>{visibleStudents.map(item => <li key={item.id}><span>{item.fullName} <small>{item.email}</small></span><span>{items.some(candidate => candidate.id !== detail.id && candidate.schoolYear === detail.schoolYear) && <SchoolSelect label={`Lớp đích của ${item.fullName}`} placeholder="Chuyển lớp…" disabled={busy} options={items.filter(candidate => candidate.id !== detail.id && candidate.schoolYear === detail.schoolYear).map(candidate => ({ value: candidate.id, label: candidate.name }))} onValueChange={targetClassId => { if (targetClassId) void action(() => transferClassStudent(schoolId, targetClassId, item.id)); }} />}<Button size="1" variant="soft" color="red" disabled={busy} onClick={() => void action(() => removeClassStudent(schoolId, detail.id, item.id))}>Gỡ</Button></span></li>)}</ul>
          {detail.students.length > 0 && <div className="admin-member-pagination"><span>Hiển thị {studentPageStart + 1}–{Math.min(studentPageStart + studentPageSize, detail.students.length)} / {detail.students.length}</span><div><label>Số dòng<Select.Root size="1" value={String(studentPageSize)} onValueChange={value => { setStudentPageSize(Number(value)); setStudentPage(1); }}><Select.Trigger aria-label="Số học sinh trên mỗi trang" /><Select.Content>{[10, 20, 30].map(size => <Select.Item key={size} value={String(size)}>{size}</Select.Item>)}</Select.Content></Select.Root></label><Button type="button" size="1" variant="soft" color="gray" disabled={currentStudentPage <= 1} onClick={() => setStudentPage(Math.max(1, currentStudentPage - 1))}>Trước</Button><span>Trang {currentStudentPage}/{studentPageCount}</span><Button type="button" size="1" variant="soft" color="gray" disabled={currentStudentPage >= studentPageCount} onClick={() => setStudentPage(Math.min(studentPageCount, currentStudentPage + 1))}>Sau</Button></div></div>}
        </div>
      </motion.section></Card>}
    </motion.div>
    <Dialog.Root open={showForm} onOpenChange={open => { if (!open) closeForm(); }}><Dialog.Content maxWidth="620px" size="3" onEscapeKeyDown={event => { if (busy) event.preventDefault(); }} onPointerDownOutside={event => { if (busy) event.preventDefault(); }} onOpenAutoFocus={event => { event.preventDefault(); nameInputRef.current?.focus(); }} onCloseAutoFocus={event => { event.preventDefault(); returnFocusRef.current?.focus(); }}>
      <div className="admin-modal-header"><div><Dialog.Title>{editing ? "Cập nhật lớp" : "Tạo lớp mới"}</Dialog.Title><Dialog.Description size="2">Học sinh chỉ có một lớp đang hoạt động trong cùng năm học.</Dialog.Description></div><Dialog.Close><IconButton variant="soft" color="gray" aria-label="Đóng" disabled={busy}><Cross2Icon /></IconButton></Dialog.Close></div>
      {formError && <div className="admin-error-banner" role="alert">{formError}</div>}
      <form className="admin-form-grid" onSubmit={submit}>
        <label>Tên lớp<TextField.Root ref={nameInputRef} required maxLength={100} value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} placeholder="Ví dụ: 10A1" aria-label="Tên lớp" /></label>
        <label>Khối<SchoolSelect value={String(form.gradeLevel)} label="Khối" placeholder="Chọn khối" options={[10, 11, 12].map(value => ({ value: String(value), label: String(value) }))} onValueChange={value => setForm({ ...form, gradeLevel: Number(value) })} /></label>
        <label>Năm học<TextField.Root required maxLength={20} value={form.schoolYear} onChange={event => setForm({ ...form, schoolYear: event.target.value })} placeholder="2026-2027" aria-label="Năm học" /></label>
        <label>Môn học<TextField.Root maxLength={50} value={form.subject} onChange={event => setForm({ ...form, subject: event.target.value })} aria-label="Môn học" /></label>
        <div className="admin-form-actions"><Button variant="soft" color="gray" type="button" disabled={busy} onClick={closeForm}>Hủy</Button><Button type="submit" loading={busy} disabled={busy}>{editing ? "Lưu thay đổi" : "Tạo lớp"}</Button></div>
      </form>
    </Dialog.Content></Dialog.Root>
  </div>;
}
