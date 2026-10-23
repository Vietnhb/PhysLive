import { useCallback, useEffect, useState } from "react";
import axios from "axios";
import { Link } from "react-router-dom";
import { Button, Card, Heading, SegmentedControl, Text, TextArea } from "@radix-ui/themes";
import api from "../../../shared/api/client";
import { CardLoadingSkeleton } from "../../../shared/ui/LoadingSkeleton";

type Gap = {
  id: string; kind: "UNCHECKED" | "NO_ROLE" | "AMBIGUOUS"; schemaId?: string; topic?: string; role?: string; unit?: string; label?: string;
  occurrences: number; exampleDescription?: string; exampleFormula?: string; status: "OPEN" | "RESOLVED" | "DISMISSED"; note?: string; updatedAt: string;
};
type Filter = "OPEN" | "RESOLVED" | "DISMISSED";

const KIND: Record<Gap["kind"], { title: string; todo: string }> = {
  UNCHECKED: { title: "Vai trò chưa có định luật kiểm", todo: "Thêm một định luật dùng vai trò này vào chủ đề liên quan." },
  NO_ROLE: { title: "Đại lượng chưa có vai trò phù hợp", todo: "Thêm vai trò mới cho loại đại lượng này, rồi thêm định luật dùng vai trò đó." },
  AMBIGUOUS: { title: "Định luật không xác định được đại lượng", todo: "Bổ sung điều kiện (where, scope) cho định luật để nó chọn đúng một đại lượng." },
};

/** What teachers' simulations computed that no law could check, most frequent first. */
export function LawGapsDesk() {
  const [items, setItems] = useState<Gap[]>([]);
  const [filter, setFilter] = useState<Filter>("OPEN");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState("");

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try { setItems((await api.get<Gap[]>("/reviewer/law-gaps", { params: { status: filter } })).data); }
    catch { setError("Chưa tải được danh sách. Vui lòng thử lại."); }
    finally { setLoading(false); }
  }, [filter]);
  useEffect(() => { void load(); }, [load]);

  const decide = async (gap: Gap, status: Filter) => {
    setBusy(gap.id); setError("");
    try {
      await api.put(`/reviewer/law-gaps/${gap.id}`, { status, note: notes[gap.id] ?? gap.note ?? "" });
      setItems(current => current.filter(item => item.id !== gap.id));
    } catch (cause) {
      const message = axios.isAxiosError<{ message?: string }>(cause) ? cause.response?.data?.message : undefined;
      setError(message || "Chưa lưu được. Vui lòng thử lại.");
    } finally { setBusy(""); }
  };

  return <div className="reviewer-stack">
    <header className="reviewer-page-head"><div>
      <Heading as="h1" size="6">Định luật còn thiếu</Heading>
      <Text as="p" size="2" color="gray" mt="1">Khi giáo viên tạo mô phỏng, có đại lượng hệ thống tính được nhưng chưa định luật nào đối chiếu. Mỗi dòng là một loại như vậy, xếp theo số lần gặp. Giáo viên vẫn dùng được mô phỏng và đã được báo là phần này đang chờ bổ sung.</Text>
    </div></header>
    <SegmentedControl.Root value={filter} onValueChange={value => setFilter(value as Filter)}>
      <SegmentedControl.Item value="OPEN">Cần xử lý</SegmentedControl.Item>
      <SegmentedControl.Item value="RESOLVED">Đã bổ sung</SegmentedControl.Item>
      <SegmentedControl.Item value="DISMISSED">Không cần bổ sung</SegmentedControl.Item>
    </SegmentedControl.Root>
    {error && <div className="ops-alert" role="alert">{error}</div>}
    {loading && items.length === 0 && <CardLoadingSkeleton cards={3} label="Đang tải danh sách định luật còn thiếu…" />}
    {!loading && items.length === 0 && <Card size="3"><Text as="p" color="gray">{filter === "OPEN" ? "Không có mục nào cần xử lý. Các mục mới sẽ xuất hiện khi giáo viên tạo mô phỏng có đại lượng chưa được kiểm." : "Chưa có mục nào."}</Text></Card>}
    {items.map(gap => <Card key={gap.id} size="3">
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
        <div style={{ minWidth: 0 }}>
          <Text as="div" size="1" color="gray">{KIND[gap.kind].title}{gap.topic ? ` · ${gap.topic}` : ""}</Text>
          <Heading as="h2" size="4" mt="1">{gap.kind === "UNCHECKED" ? <><code>{gap.role}</code>{gap.unit ? ` (${gap.unit})` : ""}</> : gap.label}</Heading>
          {gap.kind === "UNCHECKED" && gap.label && <Text as="p" size="2" color="gray">Ví dụ gần nhất: {gap.label}</Text>}
          {gap.kind === "NO_ROLE" && gap.unit && <Text as="p" size="2" color="gray">Đơn vị: {gap.unit}</Text>}
        </div>
        <Text as="div" size="2" weight="bold" style={{ whiteSpace: "nowrap" }}>Gặp {gap.occurrences} lần</Text>
      </div>
      {gap.exampleDescription && <Text as="p" size="2" mt="2"><strong>Mô tả của giáo viên:</strong> {gap.exampleDescription}</Text>}
      {gap.exampleFormula && <Text as="p" size="2" mt="1"><strong>AI đã viết:</strong> <code style={{ wordBreak: "break-word" }}>{gap.exampleFormula}</code></Text>}
      <Text as="p" size="2" mt="2" color="gray"><strong>Việc cần làm:</strong> {KIND[gap.kind].todo} <Link to="/reviewer/topics">Mở Chủ đề vật lý</Link></Text>
      {gap.status === "OPEN" ? <>
        <label className="reviewer-field" style={{ marginTop: 8 }}><span>Ghi chú xử lý (không bắt buộc)</span>
          <TextArea size="2" rows={2} placeholder="Đã thêm định luật … vào chủ đề …" value={notes[gap.id] ?? ""} onChange={event => setNotes(current => ({ ...current, [gap.id]: event.target.value }))} /></label>
        <div style={{ display: "flex", gap: 8, marginTop: 8, flexWrap: "wrap" }}>
          <Button size="2" disabled={busy === gap.id} onClick={() => void decide(gap, "RESOLVED")}>Đã bổ sung định luật</Button>
          <Button size="2" variant="soft" color="gray" disabled={busy === gap.id} onClick={() => void decide(gap, "DISMISSED")}>Không cần bổ sung</Button>
        </div>
      </> : <>
        {gap.note && <Text as="p" size="2" mt="2"><strong>Ghi chú:</strong> {gap.note}</Text>}
        <Button size="1" variant="soft" color="gray" mt="2" disabled={busy === gap.id} onClick={() => void decide(gap, "OPEN")}>Mở lại</Button>
      </>}
    </Card>)}
  </div>;
}
