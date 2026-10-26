import { useState } from "react";
import { AlertDialog, Button, Card, Flex } from "@radix-ui/themes";
import { useSessionStore } from "../../../shared/auth/sessionStore";
import { SchoolImportWorkflow } from "../dataio/components/SchoolBulkImport";
import { IMPORT_LABELS, type ImportKind } from "../dataio/api/importApi";
import s from "./SchoolImportPage.module.css";

const IMPORT_KINDS: ImportKind[] = ["USERS", "CLASSES", "ENROLLMENTS", "TEACHER_ASSIGNMENTS"];

export default function SchoolImportPage() {
  const schoolId = useSessionStore(state => state.user?.schoolId);
  const [kind, setKind] = useState<ImportKind>("USERS");
  const [hasPendingData, setHasPendingData] = useState(false);
  const [pendingKind, setPendingKind] = useState<ImportKind | null>(null);

  if (!schoolId) return null;

  const changeKind = (next: ImportKind) => {
    if (next === kind) return;
    if (hasPendingData) {
      setPendingKind(next);
      return;
    }
    setHasPendingData(false);
    setKind(next);
  };

  const confirmKindChange = () => {
    if (!pendingKind) return;
    setHasPendingData(false);
    setKind(pendingKind);
    setPendingKind(null);
  };

  return <div className={`admin-content ${s.page}`}>
    

    <Card size="3" className={s.card}>
      <div className={s.tabsScroll}>
        <div className={s.tabs} role="tablist" aria-label="Loại dữ liệu nhập">
          {IMPORT_KINDS.map(item => <button key={item} type="button" role="tab" aria-selected={kind === item} aria-controls="school-import-workflow" className={kind === item ? s.activeTab : ""} onClick={() => changeKind(item)}>{IMPORT_LABELS[item]}</button>)}
        </div>
      </div>
      <div id="school-import-workflow" role="tabpanel" aria-label={IMPORT_LABELS[kind]} className={s.panel}>
        <SchoolImportWorkflow key={kind} schoolId={schoolId} kind={kind} onPendingChange={setHasPendingData} />
      </div>
    </Card>

    <AlertDialog.Root open={pendingKind !== null} onOpenChange={open => { if (!open) setPendingKind(null); }}>
      <AlertDialog.Content maxWidth="460px">
        <AlertDialog.Title>Hủy phiên nhập hiện tại?</AlertDialog.Title>
        <AlertDialog.Description size="2">
          Bạn có dữ liệu chưa xác nhận. Chuyển sang tab khác sẽ xóa tệp và kết quả xem trước hiện tại.
        </AlertDialog.Description>
        <Flex gap="3" mt="4" justify="end">
          <AlertDialog.Cancel><Button variant="soft" color="gray">Tiếp tục nhập</Button></AlertDialog.Cancel>
          <AlertDialog.Action><Button color="red" onClick={confirmKindChange}>Hủy và chuyển tab</Button></AlertDialog.Action>
        </Flex>
      </AlertDialog.Content>
    </AlertDialog.Root>
  </div>;
}
