import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { AlertDialog, Badge, Button, Callout, Card, Dialog, Flex, Select, Table, TextField } from "@radix-ui/themes";
import { EyeOpenIcon, MagnifyingGlassIcon, PlusIcon } from "@radix-ui/react-icons";
import { motion, useReducedMotion } from "motion/react";
import { useNavigate } from "react-router-dom";
import { archiveSchoolClass, createSchoolClass, restoreSchoolClass, schoolClasses } from "../api/schoolApi";
import type { SchoolClassRequest, SchoolClassSummary } from "../types";
import { apiMessage } from "../../../shared/lib/apiError";
import { useSessionStore } from "../../../shared/auth/sessionStore";
import s from "./SchoolClasses.module.css";
import { ClassTableLoading } from "./SchoolLoading";
import { FormField } from "../../../shared/ui/ManagementUI";
import modal from "../../../shared/ui/ModalScroll.module.css";

const normalize = (value: string) => value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("vi");
const blankClass = (): SchoolClassRequest => {
  const today = new Date();
  const year = today.getMonth() >= 7 ? today.getFullYear() : today.getFullYear() - 1;
  return { name: "", gradeLevel: 10, schoolYear: `${year}-${year + 1}`, subject: "Vật lý" };
};

export default function SchoolClasses() {
  const reducedMotion = useReducedMotion();
  const navigate = useNavigate();
  const schoolId = useSessionStore(state => state.user?.schoolId);
  const actorRole = useSessionStore(state => state.user?.role);
  const [items, setItems] = useState<SchoolClassSummary[]>([]);
  const [query, setQuery] = useState("");
  const [grade, setGrade] = useState("ALL");
  const [schoolYear, setSchoolYear] = useState("ALL");
  const [status, setStatus] = useState("ACTIVE");
  const [classPage, setClassPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [statusTarget, setStatusTarget] = useState<SchoolClassSummary | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState<SchoolClassRequest>(blankClass);
  const [formError, setFormError] = useState("");
  const [notice, setNotice] = useState("");
  const createLock = useRef(false);

  const submitClass = async (event: FormEvent) => {
    event.preventDefault();
    if (!schoolId || busy || createLock.current) return;
    createLock.current = true; setBusy(true); setFormError(""); setNotice("");
    try {
      const created = await createSchoolClass(schoolId, { ...form, name: form.name.trim(), schoolYear: form.schoolYear.trim(), subject: form.subject.trim() });
      setItems(current => [{ ...created, teacherCount: created.teachers.length, studentCount: created.students.length }, ...current]);
      setQuery(""); setGrade("ALL"); setSchoolYear("ALL"); setStatus("ACTIVE"); setClassPage(1);
      setShowCreate(false); setForm(blankClass());
      setNotice(`Đã tạo lớp ${created.name}. Bạn có thể chọn Sửa để phân công giáo viên và thêm học sinh.`);
    } catch (err) {
      setFormError(apiMessage(err, "Không thể tạo lớp học."));
    } finally {
      createLock.current = false; setBusy(false);
    }
  };

  const load = useCallback(async () => {
    if (!schoolId) return;
    setLoading(true);
    setError("");
    try {
      setItems(await schoolClasses(schoolId, true));
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
      return matchesQuery && (grade === "ALL" || String(item.gradeLevel) === grade) && (schoolYear === "ALL" || item.schoolYear === schoolYear)
        && (status === "ALL" || item.active === (status === "ACTIVE"));
    });
  }, [grade, items, query, schoolYear, status]);

  const classPageSize = 10;
  const classPageCount = Math.max(1, Math.ceil(filteredItems.length / classPageSize));
  const currentClassPage = Math.min(classPage, classPageCount);
  const classPageStart = (currentClassPage - 1) * classPageSize;
  const visibleClasses = filteredItems.slice(classPageStart, classPageStart + classPageSize);

  const updateQuery = (value: string) => { setQuery(value); setClassPage(1); };
  const updateGrade = (value: string) => { setGrade(value); setClassPage(1); };
  const updateSchoolYear = (value: string) => { setSchoolYear(value); setClassPage(1); };

  const confirmStatusChange = async () => {
    if (!schoolId || !statusTarget || busy) return;
    const target = statusTarget;
    setStatusTarget(null);
    setBusy(true);
    setError(""); setNotice("");
    try {
      if (target.active) {
        await archiveSchoolClass(schoolId, target.id);
        setItems(current => current.map(item => item.id === target.id ? { ...item, active: false, teacherCount: 0, studentCount: 0 } : item));
        setNotice(`Đã tắt lớp ${target.name}. Có thể mở lại trong bộ lọc Đã tắt.`);
      } else {
        const restored = await restoreSchoolClass(schoolId, target.id);
        setItems(current => current.map(item => item.id === restored.id ? restored : item));
        setStatus("ACTIVE"); setClassPage(1);
        setNotice(`Đã mở lại lớp ${restored.name}. Chọn Sửa để phân công giáo viên và xếp học sinh lại.`);
      }
    } catch (err) {
      setError(apiMessage(err, target.active ? "Không thể tắt lớp học." : "Không thể mở lại lớp học."));
    } finally {
      setBusy(false);
    }
  };

  if (!schoolId) return null;

  return <div className="admin-content">
    {notice && <Callout.Root color="green" role="status" mb="4"><Callout.Text>{notice}</Callout.Text></Callout.Root>}
    {error && <div className="admin-error-banner" role="alert">{error}<Button variant="soft" color="red" onClick={() => void load()}>Tải lại</Button></div>}

    <motion.div className="school-classes-grid" initial={reducedMotion ? false : { opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: reducedMotion ? 0 : .18 }}>
      <Card asChild size="3"><section className="admin-panel">
        <div className="admin-panel-heading">
          <div><h2>Danh sách lớp</h2><p className="admin-panel-description">{loading ? "Đang tải dữ liệu lớp học…" : `${filteredItems.length.toLocaleString("vi-VN")} lớp ${status === "ACTIVE" ? "đang hoạt động" : status === "INACTIVE" ? "đã tắt" : "phù hợp"}`}</p></div>
          {actorRole === "SCHOOL" && <Button disabled={busy || loading} onClick={() => { setForm(blankClass()); setFormError(""); setNotice(""); setShowCreate(true); }}><PlusIcon />Tạo lớp</Button>}
        </div>

        <div className={`${s.filters} ${s.classFilters}`}>
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
          <Select.Root value={status} disabled={busy} onValueChange={value => { setStatus(value); setClassPage(1); }}>
            <Select.Trigger aria-label="Lọc theo trạng thái lớp" />
            <Select.Content position="popper"><Select.Item value="ACTIVE">Đang hoạt động</Select.Item><Select.Item value="INACTIVE">Đã tắt</Select.Item><Select.Item value="ALL">Tất cả trạng thái</Select.Item></Select.Content>
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
                <Table.Cell><Badge color={item.active ? "green" : "gray"} variant="soft">{item.active ? "Hoạt động" : "Đã tắt"}</Badge></Table.Cell>
                <Table.Cell><div className={s.actions}>
                  {item.active ? <>
                    <Button size="1" variant="soft" disabled={busy} onClick={() => navigate(`/school/classes/${item.id}`)}><EyeOpenIcon />Xem</Button>
                    <Button size="1" variant="soft" color="cyan" disabled={busy} onClick={() => navigate(`/school/classes/${item.id}?mode=edit`)}>Sửa</Button>
                    <Button size="1" variant="soft" color="red" disabled={busy} onClick={() => setStatusTarget(item)}>Tắt</Button>
                  </> : <Button size="1" variant="soft" color="green" disabled={busy} onClick={() => setStatusTarget(item)}>Mở lại</Button>}
                </div></Table.Cell>
              </Table.Row>)}
            </Table.Body>
          </Table.Root></div>
          {filteredItems.length > 0 && <Pagination start={classPageStart} pageSize={classPageSize} total={filteredItems.length} page={currentClassPage} pageCount={classPageCount} onPageChange={setClassPage} />}
        </>}
      </section></Card>

    </motion.div>

    <Dialog.Root open={showCreate} onOpenChange={open => { if (!createLock.current) setShowCreate(open); }}>
      <Dialog.Content className={modal.dialog} maxWidth="620px" onEscapeKeyDown={event => { if (createLock.current) event.preventDefault(); }} onInteractOutside={event => { if (createLock.current) event.preventDefault(); }}>
        <div className={modal.body}>
          <Dialog.Title>Tạo lớp học</Dialog.Title>
          <Dialog.Description size="2" mb="5">Nhập thông tin lớp. Sau khi tạo, bạn có thể phân công giáo viên và thêm học sinh.</Dialog.Description>
          {formError && <Callout.Root color="red" role="alert" mb="4"><Callout.Text>{formError}</Callout.Text></Callout.Root>}
          <form className="admin-form-grid" onSubmit={submitClass}>
            <FormField label="Tên lớp" htmlFor="create-class-name"><input id="create-class-name" required maxLength={100} disabled={busy} value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} placeholder="Ví dụ: 10A1" /></FormField>
            <FormField label="Khối" htmlFor="create-class-grade"><Select.Root size="3" value={String(form.gradeLevel)} disabled={busy} onValueChange={value => setForm({ ...form, gradeLevel: Number(value) })}><Select.Trigger id="create-class-grade" /><Select.Content position="popper" align="start">{[10, 11, 12].map(value => <Select.Item key={value} value={String(value)}>Khối {value}</Select.Item>)}</Select.Content></Select.Root></FormField>
            <FormField label="Năm học" htmlFor="create-class-year"><input id="create-class-year" required maxLength={20} pattern="[0-9]{4}-[0-9]{4}" title="Nhập năm học dạng 2026-2027" disabled={busy} value={form.schoolYear} onChange={event => setForm({ ...form, schoolYear: event.target.value })} /></FormField>
            <FormField label="Môn học" htmlFor="create-class-subject"><input id="create-class-subject" maxLength={50} disabled={busy} value={form.subject} onChange={event => setForm({ ...form, subject: event.target.value })} /></FormField>
            <div className="admin-form-actions"><Button type="button" variant="soft" color="gray" disabled={busy} onClick={() => setShowCreate(false)}>Hủy</Button><Button type="submit" loading={busy} disabled={busy}>Tạo lớp</Button></div>
          </form>
        </div>
      </Dialog.Content>
    </Dialog.Root>

    <AlertDialog.Root open={statusTarget !== null} onOpenChange={open => { if (!open) setStatusTarget(null); }}><AlertDialog.Content maxWidth="480px"><AlertDialog.Title>{statusTarget?.active ? "Tắt lớp học?" : "Mở lại lớp học?"}</AlertDialog.Title><AlertDialog.Description size="2">{statusTarget?.active ? `Lớp “${statusTarget.name}” sẽ ngừng hoạt động. Phân công giáo viên sẽ được gỡ và xếp lớp học sinh sẽ kết thúc. Dữ liệu lớp vẫn được giữ lại.` : `Lớp “${statusTarget?.name}” sẽ hoạt động trở lại. Bạn cần phân công giáo viên và xếp học sinh lại; các phân công cũ không tự động khôi phục.`}</AlertDialog.Description><Flex gap="3" mt="4" justify="end"><AlertDialog.Cancel><Button variant="soft" color="gray">Hủy</Button></AlertDialog.Cancel><AlertDialog.Action><Button color={statusTarget?.active ? "red" : "green"} disabled={busy} onClick={() => void confirmStatusChange()}>{statusTarget?.active ? "Tắt lớp" : "Mở lại lớp"}</Button></AlertDialog.Action></Flex></AlertDialog.Content></AlertDialog.Root>
  </div>;
}

function Pagination({ start, pageSize, total, page, pageCount, onPageChange, compact = false }: Readonly<{ start: number; pageSize: number; total: number; page: number; pageCount: number; onPageChange: (page: number) => void; compact?: boolean }>) {
  return <div className={`${s.pagination} ${compact ? s.compactPagination : ""}`}>
    <span>Hiển thị {start + 1}–{Math.min(start + pageSize, total)} / {total}</span>
    <div><Button type="button" size="1" variant="soft" color="gray" disabled={page <= 1} onClick={() => onPageChange(Math.max(1, page - 1))}>Trước</Button><span>Trang {page}/{pageCount}</span><Button type="button" size="1" variant="soft" color="gray" disabled={page >= pageCount} onClick={() => onPageChange(Math.min(pageCount, page + 1))}>Sau</Button></div>
  </div>;
}
