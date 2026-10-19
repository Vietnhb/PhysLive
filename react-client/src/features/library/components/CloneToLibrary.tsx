import { useState } from "react";
import { Link } from "react-router-dom";
import { Button, Select, Text } from "@radix-ui/themes";
import { apiMessage } from "../../../shared/lib/apiError";
import type { LibraryFolder, LibraryItem } from "../../../shared/types/physlive";
import { cloneSharedLibrary, createLibraryFolder, libraryFolders } from "../api/libraryApi";

const COMMUNITY_FOLDER = "Từ cộng đồng";

/** Saves the teacher's own copy of a shared simulation into one of their folders; the copy can then be changed freely. */
export function CloneToLibrary({ item }: Readonly<{ item: LibraryItem }>) {
  const [folders, setFolders] = useState<LibraryFolder[] | null>(null);
  const [folderId, setFolderId] = useState("");
  const [busy, setBusy] = useState(false);
  const [saved, setSaved] = useState<LibraryItem | null>(null);
  const [error, setError] = useState("");

  const start = async () => {
    setBusy(true); setError("");
    try {
      let mine = await libraryFolders();
      if (mine.length === 0) mine = [await createLibraryFolder(COMMUNITY_FOLDER)];
      setFolders(mine);
      setFolderId((mine.find(folder => folder.name === COMMUNITY_FOLDER) ?? mine[0]).id);
    } catch (cause) { setError(apiMessage(cause, "Chưa tải được thư mục thư viện.")); }
    finally { setBusy(false); }
  };

  const clone = async () => {
    setBusy(true); setError("");
    try { setSaved(await cloneSharedLibrary(item.id, folderId)); }
    catch (cause) { setError(apiMessage(cause, "Chưa lưu được bản sao. Vui lòng thử lại.")); }
    finally { setBusy(false); }
  };

  if (saved) return <Text as="p" size="2" mt="3" role="status">
    Đã lưu "{saved.title}" vào thư viện của bạn. Bản sao là của riêng bạn, chỉnh sửa không ảnh hưởng bản gốc. <Link to="/library">Mở thư viện</Link>
  </Text>;

  return <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap", marginTop: 12 }}>
    {folders === null
      ? <Button size="2" variant="soft" disabled={busy} onClick={() => void start()}>Lưu bản sao về thư viện</Button>
      : <>
        <Select.Root size="2" value={folderId} onValueChange={setFolderId}>
          <Select.Trigger aria-label="Thư mục lưu bản sao" />
          <Select.Content>{folders.map(folder => <Select.Item key={folder.id} value={folder.id}>{folder.name}</Select.Item>)}</Select.Content>
        </Select.Root>
        <Button size="2" disabled={busy || !folderId} onClick={() => void clone()}>Lưu bản sao</Button>
        <Button size="2" variant="ghost" color="gray" disabled={busy} onClick={() => setFolders(null)}>Huỷ</Button>
      </>}
    {error && <Text as="p" size="2" color="red" role="alert" style={{ width: "100%" }}>{error}</Text>}
  </div>;
}
