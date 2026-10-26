import { useCallback, useEffect, useMemo, useState } from "react";
import { AlertDialog, Badge, Button, Card, Flex, Select, Table, TextField } from "@radix-ui/themes";
import { EyeOpenIcon, MagnifyingGlassIcon } from "@radix-ui/react-icons";
import { motion, useReducedMotion } from "motion/react";
import { useNavigate } from "react-router-dom";
import { archiveSchoolClass, schoolClasses } from "../api/schoolApi";
import type { SchoolClassSummary } from "../types";
import { apiMessage } from "../../../shared/lib/apiError";
import { useSessionStore } from "../../../shared/auth/sessionStore";
import s from "./SchoolClasses.module.css";
import { ClassTableLoading } from "./SchoolLoading";

const normalize = (value: string) => value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("vi");
export default function SchoolClasses() {
  const reducedMotion = useReducedMotion();
  const navigate = useNavigate();
  const schoolId = useSessionStore(state => state.user?.schoolId);
  const [items, setItems] = useState<SchoolClassSummary[]>([]);
  const [query, setQuery] = useState("");
  const [grade, setGrade] = useState("ALL");
  const [schoolYear, setSchoolYear] = useState("ALL");
  const [classPage, setClassPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
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

    <AlertDialog.Root open={archiveTarget !== null} onOpenChange={open => { if (!open) setArchiveTarget(null); }}><AlertDialog.Content maxWidth="460px"><AlertDialog.Title>Tắt lớp học?</AlertDialog.Title><AlertDialog.Description size="2">Lớp “{archiveTarget?.name}” sẽ không còn xuất hiện trong danh sách lớp đang hoạt động.</AlertDialog.Description><Flex gap="3" mt="4" justify="end"><AlertDialog.Cancel><Button variant="soft" color="gray">Hủy</Button></AlertDialog.Cancel><AlertDialog.Action><Button color="red" disabled={busy} onClick={() => void confirmArchive()}>Tắt lớp</Button></AlertDialog.Action></Flex></AlertDialog.Content></AlertDialog.Root>
  </div>;
}

function Pagination({ start, pageSize, total, page, pageCount, onPageChange, compact = false }: Readonly<{ start: number; pageSize: number; total: number; page: number; pageCount: number; onPageChange: (page: number) => void; compact?: boolean }>) {
  return <div className={`${s.pagination} ${compact ? s.compactPagination : ""}`}>
    <span>Hiển thị {start + 1}–{Math.min(start + pageSize, total)} / {total}</span>
    <div><Button type="button" size="1" variant="soft" color="gray" disabled={page <= 1} onClick={() => onPageChange(Math.max(1, page - 1))}>Trước</Button><span>Trang {page}/{pageCount}</span><Button type="button" size="1" variant="soft" color="gray" disabled={page >= pageCount} onClick={() => onPageChange(Math.min(pageCount, page + 1))}>Sau</Button></div>
  </div>;
}
