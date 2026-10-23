import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { AlertDialog, Badge, Button, Card, Dialog, Flex, IconButton, Select, Table, TextField } from "@radix-ui/themes";
import { Cross2Icon, EyeOpenIcon, MagnifyingGlassIcon, PlusIcon } from "@radix-ui/react-icons";
import { motion, useReducedMotion } from "motion/react";
import { useNavigate } from "react-router-dom";
import { archiveSchoolClass, createSchoolClass, schoolClasses } from "../api/schoolApi";
import type { SchoolClassRequest, SchoolClassSummary } from "../types";
import { apiMessage } from "../../../shared/lib/apiError";
import { useSessionStore } from "../../../shared/auth/sessionStore";
import s from "./SchoolClasses.module.css";
import { ClassTableLoading } from "./SchoolLoading";

const normalize = (value: string) => value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("vi");
const defaultSchoolYear = () => {
  const now = new Date();
  const start = now.getMonth() >= 7 ? now.getFullYear() : now.getFullYear() - 1;
  return `${start}-${start + 1}`;
};
const blankClass = (): SchoolClassRequest => ({ name: "", gradeLevel: 10, schoolYear: defaultSchoolYear(), subject: "Vật lý" });

export default function SchoolClasses() {
  const reducedMotion = useReducedMotion();
  const navigate = useNavigate();
  const nameInputRef = useRef<HTMLInputElement>(null);
  const schoolId = useSessionStore(state => state.user?.schoolId);
  const [items, setItems] = useState<SchoolClassSummary[]>([]);
  const [query, setQuery] = useState("");
  const [grade, setGrade] = useState("ALL");
  const [schoolYear, setSchoolYear] = useState("ALL");
  const [classPage, setClassPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<SchoolClassRequest>(blankClass);
  const [archiveTarget, setArchiveTarget] = useState<SchoolClassSummary | null>(null);

  const load = useCallback(async () => {
    if (!schoolId) return;
    setLoading(true);
    setError("");
    try {
      setItems(await schoolClasses(schoolId));
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

  const years = useMemo(() => [...new Set(items.map(item => item.schoolYear))].sort((a, b) => b.localeCompare(a)), [items]);
  const filteredItems = useMemo(() => {
    const needle = normalize(query.trim());
    return items.filter(item => {
      const matchesQuery = !needle || normalize(`${item.name} ${item.subject ?? ""}`).includes(needle);
      return matchesQuery && (grade === "ALL" || String(item.gradeLevel) === grade) && (schoolYear === "ALL" || item.schoolYear === schoolYear);
    });
  }, [grade, items, query, schoolYear]);

  const classPageSize = 10;
  const classPageCount = Math.max(1, Math.ceil(filteredItems.length / classPageSize));
  const currentClassPage = Math.min(classPage, classPageCount);
  const classPageStart = (currentClassPage - 1) * classPageSize;
  const visibleClasses = filteredItems.slice(classPageStart, classPageStart + classPageSize);

  const updateQuery = (value: string) => { setQuery(value); setClassPage(1); };
  const updateGrade = (value: string) => { setGrade(value); setClassPage(1); };
  const updateSchoolYear = (value: string) => { setSchoolYear(value); setClassPage(1); };

  const openCreate = () => {
    setForm(blankClass());
    setFormError("");
    setShowForm(true);
  };

  const closeForm = () => {
    if (busy) return;
    setShowForm(false);
    setForm(blankClass());
    setFormError("");
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!schoolId || busy) return;
    setBusy(true);
    setFormError("");
    try {
      await createSchoolClass(schoolId, form);
      setShowForm(false);
      setForm(blankClass());
      await load();
    } catch (err) {
      setFormError(apiMessage(err, "Không thể lưu lớp học."));
    } finally {
      setBusy(false);
    }
  };

  const confirmArchive = async () => {
    if (!schoolId || !archiveTarget || busy) return;
    const target = archiveTarget;
    setArchiveTarget(null);
    setBusy(true);
    setError("");
    try {
      await archiveSchoolClass(schoolId, target.id);
      await load();
    } catch (err) {
      setError(apiMessage(err, "Không thể tắt lớp học."));
    } finally {
      setBusy(false);
    }
  };

  if (!schoolId) return null;

  return <div className="admin-content">
    <header className="admin-content-header">
      <div>
        <h1 className="admin-content-title">Quản lý lớp học</h1>
        <p className="admin-content-description">Xem chi tiết, tạo mới, cập nhật hoặc tắt lớp học trong trường.</p>
      </div>
      <Button onClick={openCreate}><PlusIcon />Tạo lớp</Button>
    </header>

    {error && <div className="admin-error-banner" role="alert">{error}<Button variant="soft" color="red" onClick={() => void load()}>Tải lại</Button></div>}

    <motion.div className="school-classes-grid" initial={reducedMotion ? false : { opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: reducedMotion ? 0 : .18 }}>
      <Card asChild size="3"><section className="admin-panel">
        <div className="admin-panel-heading">
          <div><h2>Danh sách lớp</h2><p className="admin-panel-description">{loading ? "Đang tải dữ liệu lớp học…" : `${filteredItems.length.toLocaleString("vi-VN")} lớp đang hoạt động`}</p></div>
        </div>

        <div className={s.filters}>
          <TextField.Root type="search" value={query} onChange={event => updateQuery(event.target.value)} placeholder="Tìm theo tên lớp hoặc môn học…" aria-label="Tìm lớp học">
            <TextField.Slot><MagnifyingGlassIcon /></TextField.Slot>
          </TextField.Root>
          <Select.Root value={grade} onValueChange={updateGrade}>
            <Select.Trigger aria-label="Lọc theo khối" />
            <Select.Content><Select.Item value="ALL">Tất cả khối</Select.Item>{[10, 11, 12].map(value => <Select.Item key={value} value={String(value)}>Khối {value}</Select.Item>)}</Select.Content>
          </Select.Root>
          <Select.Root value={schoolYear} onValueChange={updateSchoolYear}>
            <Select.Trigger aria-label="Lọc theo năm học" />
            <Select.Content><Select.Item value="ALL">Tất cả năm học</Select.Item>{years.map(value => <Select.Item key={value} value={value}>{value}</Select.Item>)}</Select.Content>
          </Select.Root>
        </div>

        {loading ? <ClassTableLoading headers={["Lớp", "Khối", "Năm học", "Môn học", "Giáo viên", "Học sinh", "Trạng thái", "Thao tác"]} /> : <>
          <div className="admin-table-scroll"><Table.Root size="2" variant="surface">
            <Table.Header><Table.Row>
              <Table.ColumnHeaderCell>Lớp</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Khối</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Năm học</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Môn học</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Giáo viên</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Học sinh</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Trạng thái</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Thao tác</Table.ColumnHeaderCell>
            </Table.Row></Table.Header>
            <Table.Body>
              {visibleClasses.length === 0 ? <Table.Row><Table.Cell colSpan={8}><div className={s.empty}>Không tìm thấy lớp phù hợp với bộ lọc.</div></Table.Cell></Table.Row> : visibleClasses.map(item => <Table.Row key={item.id}>
                <Table.RowHeaderCell>{item.name}</Table.RowHeaderCell>
                <Table.Cell><Badge color="indigo" variant="soft">{item.gradeLevel}</Badge></Table.Cell>
                <Table.Cell>{item.schoolYear}</Table.Cell>
                <Table.Cell>{item.subject || "—"}</Table.Cell>
                <Table.Cell>{item.teacherCount}</Table.Cell>
                <Table.Cell>{item.studentCount}</Table.Cell>
                <Table.Cell><Badge color="green" variant="soft">Hoạt động</Badge></Table.Cell>
                <Table.Cell><div className={s.actions}>
                  <Button size="1" variant="soft" disabled={busy} onClick={() => navigate(`/school/classes/${item.id}`)}><EyeOpenIcon />Xem</Button>
                  <Button size="1" variant="soft" color="cyan" disabled={busy} onClick={() => navigate(`/school/classes/${item.id}?mode=edit`)}>Sửa</Button>
                  <Button size="1" variant="soft" color="red" disabled={busy} onClick={() => setArchiveTarget(item)}>Tắt</Button>
                </div></Table.Cell>
              </Table.Row>)}
            </Table.Body>
          </Table.Root></div>
          {filteredItems.length > 0 && <Pagination start={classPageStart} pageSize={classPageSize} total={filteredItems.length} page={currentClassPage} pageCount={classPageCount} onPageChange={setClassPage} />}
        </>}
      </section></Card>

    </motion.div>

    <Dialog.Root open={showForm} onOpenChange={open => { if (!open) closeForm(); }}><Dialog.Content maxWidth="620px" size="3" onOpenAutoFocus={event => { event.preventDefault(); nameInputRef.current?.focus(); }} onEscapeKeyDown={event => { if (busy) event.preventDefault(); }} onPointerDownOutside={event => { if (busy) event.preventDefault(); }}>
      <div className="admin-modal-header"><div><Dialog.Title>Tạo lớp học mới</Dialog.Title><Dialog.Description size="2">Nhập thông tin cơ bản của lớp học.</Dialog.Description></div><Dialog.Close><IconButton variant="soft" color="gray" aria-label="Đóng" disabled={busy}><Cross2Icon /></IconButton></Dialog.Close></div>
      {formError && <div className="admin-error-banner" role="alert">{formError}</div>}
      <form className="admin-form-grid" onSubmit={submit}>
        <label>Tên lớp<TextField.Root ref={nameInputRef} required maxLength={100} value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} placeholder="Ví dụ: 10A1" aria-label="Tên lớp" /></label>
        <label>Khối<Select.Root size="2" value={String(form.gradeLevel)} onValueChange={value => setForm({ ...form, gradeLevel: Number(value) })}><Select.Trigger aria-label="Khối" /><Select.Content>{[10, 11, 12].map(value => <Select.Item key={value} value={String(value)}>Khối {value}</Select.Item>)}</Select.Content></Select.Root></label>
        <label>Năm học<TextField.Root required maxLength={20} value={form.schoolYear} onChange={event => setForm({ ...form, schoolYear: event.target.value })} placeholder="2026-2027" aria-label="Năm học" /></label>
        <label>Môn học<TextField.Root maxLength={50} value={form.subject} onChange={event => setForm({ ...form, subject: event.target.value })} aria-label="Môn học" /></label>
        <div className="admin-form-actions"><Button variant="soft" color="gray" type="button" disabled={busy} onClick={closeForm}>Hủy</Button><Button type="submit" loading={busy} disabled={busy}>Tạo lớp</Button></div>
      </form>
    </Dialog.Content></Dialog.Root>

    <AlertDialog.Root open={archiveTarget !== null} onOpenChange={open => { if (!open) setArchiveTarget(null); }}><AlertDialog.Content maxWidth="460px"><AlertDialog.Title>Tắt lớp học?</AlertDialog.Title><AlertDialog.Description size="2">Lớp “{archiveTarget?.name}” sẽ không còn xuất hiện trong danh sách lớp đang hoạt động.</AlertDialog.Description><Flex gap="3" mt="4" justify="end"><AlertDialog.Cancel><Button variant="soft" color="gray">Hủy</Button></AlertDialog.Cancel><AlertDialog.Action><Button color="red" disabled={busy} onClick={() => void confirmArchive()}>Tắt lớp</Button></AlertDialog.Action></Flex></AlertDialog.Content></AlertDialog.Root>
  </div>;
}

function Pagination({ start, pageSize, total, page, pageCount, onPageChange, compact = false }: Readonly<{ start: number; pageSize: number; total: number; page: number; pageCount: number; onPageChange: (page: number) => void; compact?: boolean }>) {
  return <div className={`${s.pagination} ${compact ? s.compactPagination : ""}`}>
    <span>Hiển thị {start + 1}–{Math.min(start + pageSize, total)} / {total}</span>
    <div><Button type="button" size="1" variant="soft" color="gray" disabled={page <= 1} onClick={() => onPageChange(Math.max(1, page - 1))}>Trước</Button><span>Trang {page}/{pageCount}</span><Button type="button" size="1" variant="soft" color="gray" disabled={page >= pageCount} onClick={() => onPageChange(Math.min(pageCount, page + 1))}>Sau</Button></div>
  </div>;
}
