import { useEffect, useRef, useState, type ChangeEvent, type DragEvent } from "react";
import { AlertDialog, Badge, Button, Callout, Dialog, Flex, IconButton, Select, Table, Text, TextField } from "@radix-ui/themes";
import { CheckCircledIcon, Cross2Icon, DownloadIcon, FileTextIcon, ReloadIcon, TrashIcon, UploadIcon } from "@radix-ui/react-icons";
import { apiMessage } from "../../../../shared/lib/apiError";
import {
  commitImport,
  credentialsCsv,
  downloadBlob,
  downloadImportTemplate,
  IMPORT_LABELS,
  previewImportFile,
  previewImportRows,
  type ImportKind,
  type ImportPreview,
  type ImportResult,
} from "../api/importApi";
import s from "./SchoolBulkImport.module.css";

const columnLabels: Record<string, string> = {
  fullName: "Họ và tên",
  dateOfBirth: "Ngày sinh",
  email: "Email",
  initialPassword: "Mật khẩu ban đầu",
  role: "Vai trò",
  permissions: "Quyền trong trường",
  classCode: "Mã lớp",
  schoolYear: "Năm học",
  avatarUrl: "Ảnh đại diện (URL)",
  gradeLevel: "Khối",
  subject: "Môn học",
  studentEmail: "Email học sinh",
  teacherEmail: "Email giáo viên",
};

const STEPS = ["Chọn tệp", "Kiểm tra dữ liệu", "Xem trước & xác nhận"] as const;
type Props = { schoolId: string; kind: ImportKind; label?: string; disabled?: boolean; onImported?: () => void | Promise<void> };
type WorkflowProps = Props & { onPendingChange?: (pending: boolean) => void; onBusyChange?: (busy: boolean) => void };

export function SchoolImportWorkflow({ schoolId, kind, onImported, onPendingChange, onBusyChange }: Readonly<WorkflowProps>) {
  const fileRef = useRef<HTMLInputElement>(null);
  const busyRef = useRef(false);
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [busy, setBusy] = useState(false);
  const [templateBusy, setTemplateBusy] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ImportPreview | null>(null);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [dirty, setDirty] = useState(false);
  const [error, setError] = useState("");

  const pending = Boolean(selectedFile || preview) && !result;
  useEffect(() => onPendingChange?.(pending), [onPendingChange, pending]);
  useEffect(() => onBusyChange?.(busy), [busy, onBusyChange]);

  const start = () => {
    if (busyRef.current) return false;
    busyRef.current = true;
    setBusy(true);
    setError("");
    return true;
  };
  const finish = () => { busyRef.current = false; setBusy(false); };

  const reset = () => {
    if (busyRef.current) return;
    setStep(1);
    setSelectedFile(null);
    setPreview(null);
    setResult(null);
    setDirty(false);
    setDragging(false);
    setError("");
  };

  const selectFile = (file?: File) => {
    if (!file || busyRef.current) return;
    setError("");
    if (!file.name.toLocaleLowerCase("vi").endsWith(".csv")) {
      setSelectedFile(null);
      setError("Chỉ chấp nhận tệp có định dạng .csv.");
      return;
    }
    if (file.size === 0) {
      setSelectedFile(null);
      setError("Tệp CSV trống.");
      return;
    }
    if (file.size > 1_048_576) {
      setSelectedFile(null);
      setError("Tệp CSV tối đa 1 MB.");
      return;
    }
    setSelectedFile(file);
    setPreview(null);
    setResult(null);
    setDirty(false);
    setStep(1);
  };

  const chooseFile = (event: ChangeEvent<HTMLInputElement>) => {
    selectFile(event.currentTarget.files?.[0]);
    event.currentTarget.value = "";
  };

  const dropFile = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragging(false);
    selectFile(event.dataTransfer.files?.[0]);
  };

  const inspectFile = async () => {
    if (!selectedFile || !start()) return;
    try {
      const next = await previewImportFile(schoolId, kind, selectedFile);
      setPreview(next);
      setDirty(false);
      setStep(2);
    } catch (err) {
      setError(apiMessage(err, err instanceof Error ? err.message : "Không thể kiểm tra CSV."));
    } finally { finish(); }
  };

  const edit = (rowNumber: number, column: string, value: string) => {
    setPreview(current => current ? {
      ...current,
      canCommit: false,
      previewToken: null,
      rows: current.rows.map(row => row.row === rowNumber ? {
        ...row,
        data: { ...row.data, [column]: value, ...(column === "role" ? { staffType: value === "STAFF" ? row.data.staffType || "TEACHER" : "" } : {}) },
      } : row),
    } : current);
    setDirty(true);
    setError("");
  };

  const remove = (rowNumber: number) => {
    setPreview(current => current ? { ...current, canCommit: false, previewToken: null, rows: current.rows.filter(row => row.row !== rowNumber) } : current);
    setDirty(true);
    setError("");
  };

  const validate = async () => {
    if (!preview?.rows.length || !start()) return;
    try {
      setPreview(await previewImportRows(schoolId, kind, preview.rows.map(({ row, data }) => ({ row, data }))));
      setDirty(false);
    } catch (err) {
      setError(apiMessage(err, "Không thể kiểm tra dữ liệu."));
    } finally { finish(); }
  };

  const commit = async () => {
    if (!preview?.canCommit || dirty || !start()) return;
    try {
      const next = await commitImport(schoolId, preview);
      setResult(next);
      setPreview(null);
      setSelectedFile(null);
      setDirty(false);
      try { await onImported?.(); }
      catch { setError("Đã nhập thành công. Tải lại danh sách để xem dữ liệu mới."); }
    } catch (err) {
      setError(apiMessage(err, "Không thể nhập dữ liệu. Hãy kiểm tra lại trước khi thử tiếp."));
      setDirty(true);
    } finally { finish(); }
  };

  const template = async () => {
    if (templateBusy) return;
    setTemplateBusy(true);
    setError("");
    try { await downloadImportTemplate(schoolId, kind); }
    catch (err) { setError(apiMessage(err, "Không thể tải tệp mẫu.")); }
    finally { setTemplateBusy(false); }
  };

  const selectOptions = (column: string, row: Record<string, string>) => column === "role"
    ? [{ value: "STAFF", label: "Giáo viên" }, { value: "STUDENT", label: "Học sinh" }]
    : column === "permissions" && row.role === "STAFF"
      ? [{ value: "TEACH", label: "Giáo viên" }, { value: "DEPARTMENT_HEAD_PHYSICS", label: "Tổ trưởng bộ môn" }, { value: "TEACH|DEPARTMENT_HEAD_PHYSICS", label: "Giáo viên + Tổ trưởng bộ môn" }]
      : column === "gradeLevel"
        ? [10, 11, 12].map(value => ({ value: String(value), label: String(value) }))
        : null;

  const validationRows = preview?.rows.filter(row => row.errors.length > 0 || row.warnings.length > 0) ?? [];

  if (result) return <div className={s.result} role="status">
    <span className={s.resultIcon}><CheckCircledIcon width="28" height="28" /></span>
    <div><Text as="p" size="5" weight="bold">Nhập dữ liệu thành công</Text><Text as="p" size="2" color="gray">Backend đã xử lý và xác nhận {result.imported.toLocaleString("vi-VN")} bản ghi.</Text></div>
    <div className={s.resultCount}><span>Đã xử lý</span><strong>{result.imported.toLocaleString("vi-VN")}</strong></div>
    {error && <Callout.Root color="amber" role="alert"><Callout.Text>{error}</Callout.Text></Callout.Root>}
    {result.credentials.length > 0 && <div className={s.credentials}><Text as="p" size="2">Hãy tải danh sách tài khoản và mật khẩu ban đầu ngay. Mật khẩu sẽ không hiển thị lại trong danh sách người dùng.</Text><Button onClick={() => downloadBlob(new Blob([credentialsCsv(result.credentials)], { type: "text/csv;charset=utf-8" }), "tai-khoan-moi.csv")}><DownloadIcon />Tải tài khoản và mật khẩu</Button></div>}
    <Button variant="soft" color="gray" onClick={reset}><ReloadIcon />Nhập tệp khác</Button>
  </div>;

  return <div className={s.workflow}>
    <ol className={s.stepper} aria-label="Tiến trình nhập dữ liệu">
      {STEPS.map((label, index) => {
        const number = index + 1;
        const active = step === number;
        const completed = step > number;
        return <li key={label} className={`${active ? s.activeStep : ""} ${completed ? s.completedStep : ""}`} aria-current={active ? "step" : undefined}><span>{completed ? <CheckCircledIcon /> : number}</span><div><small>Bước {number}</small><strong>{label}</strong></div></li>;
      })}
    </ol>

    {error && <Callout.Root color="red" role="alert"><Callout.Text>{error}</Callout.Text></Callout.Root>}

    {step === 1 && <section className={s.stepContent} aria-labelledby="import-step-file">
      <div className={s.sectionHeading}><div><h2 id="import-step-file">Chọn tệp {IMPORT_LABELS[kind].toLocaleLowerCase("vi")}</h2><p>Tải lên tệp CSV để hệ thống kiểm tra trước khi ghi dữ liệu.</p></div><Button variant="outline" onClick={() => void template()} loading={templateBusy} disabled={templateBusy || busy}><DownloadIcon />Tải CSV mẫu</Button></div>
      <div className={`${s.dropzone} ${dragging ? s.dropzoneActive : ""}`} onDragEnter={event => { event.preventDefault(); setDragging(true); }} onDragOver={event => event.preventDefault()} onDragLeave={event => { if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setDragging(false); }} onDrop={dropFile}>
        <span className={s.uploadIcon}><UploadIcon /></span>
        <strong>{selectedFile ? selectedFile.name : "Kéo và thả tệp CSV vào đây"}</strong>
        <Text size="2" color="gray">{selectedFile ? `${Math.max(1, Math.ceil(selectedFile.size / 1024)).toLocaleString("vi-VN")} KB · Sẵn sàng kiểm tra` : "hoặc chọn tệp từ máy tính"}</Text>
        <Button type="button" variant="soft" disabled={busy} onClick={() => fileRef.current?.click()}><FileTextIcon />{selectedFile ? "Chọn tệp khác" : "Chọn tệp CSV"}</Button>
        <input ref={fileRef} type="file" accept=".csv,text/csv" hidden aria-label="Chọn tệp CSV" disabled={busy} onChange={chooseFile} />
      </div>
      <div className={s.guide}>{kind === "USERS" ? <Text as="p" size="2">Ngày sinh dùng định dạng yyyy-MM-dd. Có thể để trống mật khẩu để hệ thống tạo tự động. Email phải duy nhất; mã lớp và năm học là tùy chọn.</Text> : <Text as="p" size="2">Mã lớp là tên lớp trong trường. Năm học gồm hai năm liên tiếp, ví dụ 2026-2027. Xếp lớp và phân công chỉ dùng email tài khoản đã tồn tại trong trường.</Text>}</div>
      <div className={s.actions}><span /><Button size="3" disabled={!selectedFile || busy} loading={busy} onClick={() => void inspectFile()}>Tiếp tục kiểm tra</Button></div>
    </section>}

    {step === 2 && preview && <section className={s.stepContent} aria-labelledby="import-step-validation">
      <div className={s.sectionHeading}><div><h2 id="import-step-validation">Kết quả kiểm tra dữ liệu</h2><p>Kết quả dưới đây được trả về từ API preview; chưa có dữ liệu nào được ghi.</p></div><Badge color={preview.invalidRows ? "red" : "green"} size="2">{preview.invalidRows ? "Cần chỉnh sửa" : "Sẵn sàng xem trước"}</Badge></div>
      <div className={s.summaryGrid}>
        <article><span>Tổng số dòng</span><strong>{preview.rows.length}</strong></article>
        <article><span>Hợp lệ</span><strong className={s.validCount}>{preview.validRows}</strong></article>
        <article><span>Có lỗi</span><strong className={preview.invalidRows ? s.invalidCount : ""}>{preview.invalidRows}</strong></article>
      </div>
      <div className={s.detectedColumns}><h3>Cột được nhận diện</h3><div>{preview.columns.map(column => <Badge key={column} color="gray" variant="soft">{columnLabels[column] ?? column}<small>{column}</small></Badge>)}</div></div>
      <div className={s.validationList}><h3>Lỗi và cảnh báo</h3>{validationRows.length === 0 ? <div className={s.allValid}><CheckCircledIcon />Không phát hiện lỗi định dạng hoặc dữ liệu thiếu.</div> : validationRows.map(row => <article key={row.row}><strong>Dòng {row.row}</strong><div>{row.errors.map(message => <p className={s.errorText} key={message}>{message}</p>)}{row.warnings.map(message => <p className={s.warningText} key={message}>{message}</p>)}</div></article>)}</div>
      <div className={s.actions}><Button variant="soft" color="gray" disabled={busy} onClick={() => setStep(1)}>Quay lại</Button><Button size="3" disabled={!preview.rows.length || busy} onClick={() => setStep(3)}>Xem trước dữ liệu</Button></div>
    </section>}

    {step === 3 && preview && <section className={s.stepContent} aria-labelledby="import-step-preview">
      <div className={s.sectionHeading}><div><h2 id="import-step-preview">Xem trước và xác nhận</h2><p>Có thể sửa hoặc loại bỏ dòng, sau đó phải kiểm tra lại trước khi xác nhận.</p></div><Button variant="soft" onClick={() => void validate()} loading={busy} disabled={busy || !preview.rows.length}><ReloadIcon />Kiểm tra lại</Button></div>
      <div className={s.previewMeta}><Badge color="indigo">{preview.rows.length} dòng</Badge>{dirty ? <Badge color="amber">Cần kiểm tra lại</Badge> : <><Badge color="green">{preview.validRows} hợp lệ</Badge>{preview.invalidRows > 0 && <Badge color="red">{preview.invalidRows} lỗi</Badge>}</>}</div>
      <div className={s.tableScroll}><Table.Root size="2" variant="surface"><Table.Header><Table.Row><Table.ColumnHeaderCell>Dòng</Table.ColumnHeaderCell>{preview.columns.map(column => <Table.ColumnHeaderCell key={column}>{columnLabels[column] ?? column}<Text as="div" size="1" color="gray">{column}</Text></Table.ColumnHeaderCell>)}<Table.ColumnHeaderCell>Trạng thái</Table.ColumnHeaderCell><Table.ColumnHeaderCell>Xóa</Table.ColumnHeaderCell></Table.Row></Table.Header><Table.Body>{preview.rows.map(row => <Table.Row key={row.row}><Table.RowHeaderCell>{row.row}</Table.RowHeaderCell>{preview.columns.map(column => {
        const options = selectOptions(column, row.data);
        const label = `${columnLabels[column] ?? column}, dòng ${row.row}`;
        return <Table.Cell key={column} className={s.cell}>{options ? <Select.Root size="2" value={row.data[column] || undefined} disabled={busy} onValueChange={value => edit(row.row, column, value)}><Select.Trigger aria-label={label} placeholder="Chọn…" style={{ width: "100%" }} /><Select.Content>{options.map(option => <Select.Item key={option.value} value={option.value}>{option.label}</Select.Item>)}</Select.Content></Select.Root> : <TextField.Root aria-label={label} disabled={busy || column === "staffType" && row.data.role !== "STAFF"} placeholder={column === "staffType" && row.data.role !== "STAFF" ? "Không áp dụng" : undefined} type={column === "initialPassword" ? "password" : column === "dateOfBirth" ? "date" : "text"} value={row.data[column] ?? ""} onChange={event => edit(row.row, column, event.target.value)} />}</Table.Cell>;
      })}<Table.Cell className={s.validation}>{dirty ? <Text size="2" color="gray">Cần kiểm tra lại</Text> : <>{row.errors.map(message => <Text as="p" color="red" size="2" key={message}>{message}</Text>)}{row.warnings.map(message => <Text as="p" color="amber" size="2" key={message}>{message}</Text>)}{row.matches.map(match => <Text as="p" size="2" color="gray" key={match.id}>{match.fullName} · {match.dateOfBirth} · {match.email}</Text>)}{!row.errors.length && !row.warnings.length && <Badge color="green">Hợp lệ</Badge>}</>}</Table.Cell><Table.Cell><IconButton variant="ghost" color="red" aria-label={`Xóa dòng ${row.row}`} disabled={busy} onClick={() => remove(row.row)}><TrashIcon /></IconButton></Table.Cell></Table.Row>)}</Table.Body></Table.Root></div>
      {!preview.rows.length && <Text as="p" size="2" color="gray">Không còn dòng nào. Hãy quay lại và chọn tệp khác.</Text>}
      <div className={s.commitNote}>Toàn bộ lô chỉ được nhập khi mọi dòng hợp lệ. Hệ thống không tự suy đoán bản ghi tạo mới hoặc cập nhật ngoài kết quả API.</div>
      <div className={s.actions}><Button variant="soft" color="gray" disabled={busy} onClick={() => setStep(2)}>Quay lại</Button><Button size="3" disabled={busy || dirty || !preview.canCommit || !preview.rows.length} loading={busy} onClick={() => void commit()}><CheckCircledIcon />Xác nhận nhập {preview.rows.length} dòng</Button></div>
    </section>}
  </div>;
}

export default function SchoolBulkImport({ schoolId, kind, label = "Nhập CSV", disabled, onImported }: Readonly<Props>) {
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState(false);
  const [busy, setBusy] = useState(false);
  const [confirmClose, setConfirmClose] = useState(false);
  const changeOpen = (next: boolean) => {
    if (next) { setOpen(true); return; }
    if (busy) return;
    if (pending) {
      setConfirmClose(true);
      return;
    }
    setOpen(false);
  };
  const discardAndClose = () => {
    setPending(false);
    setConfirmClose(false);
    setOpen(false);
  };
  return <>
    <Dialog.Root open={open} onOpenChange={changeOpen}>
      <Dialog.Trigger><Button variant="soft" disabled={disabled}><UploadIcon />{label}</Button></Dialog.Trigger>
      <Dialog.Content maxWidth="1200px" size="3" className={s.dialog} onEscapeKeyDown={event => { if (busy) event.preventDefault(); }} onPointerDownOutside={event => { if (busy) event.preventDefault(); }}>
        <Flex justify="between" align="start" gap="3" mb="4"><div><Dialog.Title>Nhập CSV · {IMPORT_LABELS[kind]}</Dialog.Title><Dialog.Description size="2">Kiểm tra dữ liệu an toàn trước khi xác nhận ghi vào hệ thống.</Dialog.Description></div><IconButton type="button" variant="soft" color="gray" aria-label="Đóng" disabled={busy} onClick={() => changeOpen(false)}><Cross2Icon /></IconButton></Flex>
        {open && <SchoolImportWorkflow schoolId={schoolId} kind={kind} onImported={onImported} onPendingChange={setPending} onBusyChange={setBusy} />}
      </Dialog.Content>
    </Dialog.Root>
    <AlertDialog.Root open={confirmClose} onOpenChange={setConfirmClose}>
      <AlertDialog.Content maxWidth="460px">
        <AlertDialog.Title>Đóng và hủy phiên nhập?</AlertDialog.Title>
        <AlertDialog.Description size="2">
          Tệp và kết quả xem trước chưa xác nhận sẽ bị xóa khỏi phiên nhập hiện tại.
        </AlertDialog.Description>
        <Flex gap="3" mt="4" justify="end">
          <AlertDialog.Cancel><Button variant="soft" color="gray">Tiếp tục nhập</Button></AlertDialog.Cancel>
          <AlertDialog.Action><Button color="red" onClick={discardAndClose}>Hủy và đóng</Button></AlertDialog.Action>
        </Flex>
      </AlertDialog.Content>
    </AlertDialog.Root>
  </>;
}
