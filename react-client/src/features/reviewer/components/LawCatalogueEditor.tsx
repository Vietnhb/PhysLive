import { useMemo, useState } from "react";
import { Button, Text, TextField } from "@radix-ui/themes";
import { AdvancedJson, RowsEditor } from "./JsonEditor";
import type { JsonObject } from "../model/jsonRows";

type Catalogue = JsonObject;
type Law = JsonObject;

const INFIX: Record<string, string> = { add: " + ", sub: " − ", mul: "·", div: "/", pow: "^" };

/** A law's expression the way a reader checks it: m·a, d(v)/dt, Σ F. */
function expression(ast: unknown): string {
  if (typeof ast === "number" || typeof ast === "string") return String(ast);
  if (!Array.isArray(ast) || ast.length === 0) return "?";
  const [op, ...rest] = ast as [string, ...unknown[]];
  if (INFIX[op] && rest.length === 2) {
    const side = (item: unknown) => Array.isArray(item) && INFIX[String(item[0])] && op !== "add" && op !== "sub" ? `(${expression(item)})` : expression(item);
    return side(rest[0]) + INFIX[op] + side(rest[1]);
  }
  if (op === "ddt") return `d(${expression(rest[0])})/dt`;
  if (op === "sum") return `Σ ${expression(rest[0])}`;
  if (op === "neg") return `−${expression(rest[0])}`;
  return `${op}(${rest.map(expression).join(", ")})`;
}

function relation(law: Law): string {
  const sides = (law.equal ?? law.atMost) as unknown;
  if (!Array.isArray(sides) || sides.length !== 2) return "Chưa có quan hệ";
  return `${expression(sides[0])} ${law.equal ? "=" : "≤"} ${expression(sides[1])}`;
}

function rolesOf(law: Law): string[] {
  const bind = law.bind && typeof law.bind === "object" ? Object.values(law.bind as Record<string, JsonObject>) : [];
  return [...new Set(bind.map(item => String(item?.role ?? "")).filter(Boolean))];
}

const NEW_LAW: Law = {
  id: "", title: "", curriculum: "", scope: ["of"],
  bind: { a: { role: "" }, b: { role: "" } },
  equal: ["a", "b"],
};

/**
 * The laws a topic checks simulations against. A reviewer adds a role (what a quantity can be) and a law
 * (a relation between roles); the backend then checks every simulation whose quantities carry those roles.
 */
export function LawCatalogueEditor({ value, onChange }: Readonly<{ value: Catalogue; onChange: (next: Catalogue) => void }>) {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState<number | null>(null);
  const laws = useMemo(() => (Array.isArray(value.laws) ? value.laws as Law[] : []), [value.laws]);
  const roles = value.roles && typeof value.roles === "object" && !Array.isArray(value.roles) ? value.roles as Record<string, unknown> : {};
  const roleRows = Object.entries(roles).map(([id, description]) => ({ id, description: String(description ?? "") }));
  const setLaws = (next: Law[]) => onChange({ ...value, laws: next });
  const setLaw = (index: number, next: Law) => setLaws(laws.map((law, i) => i === index ? next : law));
  const needle = query.trim().toLowerCase();
  const shown = laws.map((law, index) => ({ law, index })).filter(({ law }) => !needle
    || String(law.title ?? "").toLowerCase().includes(needle) || String(law.id ?? "").toLowerCase().includes(needle)
    || rolesOf(law).some(role => role.toLowerCase().includes(needle)));
  const known = new Set(Object.keys(roles));

  return <div className="reviewer-form-sections">
    <section className="reviewer-form-section">
      <Text as="div" size="2" weight="bold">Vai trò vật lý ({roleRows.length})</Text>
      <Text as="p" size="1" color="gray" mb="2">Vai trò là thứ một đại lượng có thể là: lực, vận tốc, cường độ dòng điện… AI gắn vai trò cho từng đại lượng khi viết mô hình; định luật bên dưới kiểm theo vai trò.</Text>
      <details className="reviewer-disclosure">
        <summary>Xem và sửa danh sách vai trò</summary>
        <RowsEditor rows={roleRows} addLabel="Thêm vai trò" empty="Chưa có vai trò nào."
          onChange={rows => onChange({ ...value, roles: Object.fromEntries(rows.filter(row => String(row.id ?? "").trim()).map(row => [String(row.id).trim(), String(row.description ?? "")])) })}
          columns={[{ key: "id", label: "Mã vai trò", placeholder: "angular_velocity", width: "30%" }, { key: "description", label: "Mô tả cho AI", placeholder: "tốc độ góc của vật quay (of)" }]} />
      </details>
    </section>

    <section className="reviewer-form-section">
      <Text as="div" size="2" weight="bold">Định luật ({laws.length})</Text>
      <Text as="p" size="1" color="gray" mb="2">Mỗi định luật là một quan hệ giữa các vai trò. Mô phỏng nào có đủ các vai trò đó sẽ được đối chiếu; vi phạm thì không lưu được.</Text>
      <TextField.Root size="2" placeholder="Tìm theo tên định luật hoặc vai trò…" value={query} onChange={event => setQuery(event.target.value)} aria-label="Tìm định luật" />
      <table className="reviewer-mini-table" style={{ marginTop: 8 }}>
        <thead><tr><th style={{ width: "34%" }}>Định luật</th><th>Quan hệ được kiểm</th><th style={{ width: "22%" }}>Vai trò dùng</th><th aria-label="Thao tác" style={{ width: 96 }} /></tr></thead>
        <tbody>
          {shown.length === 0 && <tr><td colSpan={4}><Text size="2" color="gray">{laws.length ? "Không có định luật nào khớp." : "Chủ đề này chưa có định luật nào."}</Text></td></tr>}
          {shown.map(({ law, index }) => {
            const used = rolesOf(law), missing = used.filter(role => !known.has(role));
            return [
              <tr key={`row-${index}`}>
                <td><strong>{String(law.title || "(chưa đặt tên)")}</strong>{law.curriculum ? <><br /><Text size="1" color="gray">{String(law.curriculum)}</Text></> : null}</td>
                <td><code>{relation(law)}</code></td>
                <td><Text size="1">{used.join(", ") || "—"}</Text>{missing.length > 0 && <><br /><Text size="1" color="red">Chưa khai báo vai trò: {missing.join(", ")}</Text></>}</td>
                <td><Button type="button" size="1" variant="soft" onClick={() => setOpen(open === index ? null : index)}>{open === index ? "Đóng" : "Sửa"}</Button></td>
              </tr>,
              open === index && <tr key={`edit-${index}`}><td colSpan={4}>
                <div className="reviewer-form-row">
                  <label className="reviewer-field"><span>Tên định luật</span><TextField.Root size="2" value={String(law.title ?? "")} onChange={event => setLaw(index, { ...law, title: event.target.value })} /></label>
                  <label className="reviewer-field"><span>Mã (không dấu, duy nhất)</span><TextField.Root size="2" value={String(law.id ?? "")} onChange={event => setLaw(index, { ...law, id: event.target.value.trim() })} /></label>
                </div>
                <label className="reviewer-field"><span>Nội dung chương trình liên quan</span><TextField.Root size="2" value={String(law.curriculum ?? "")} onChange={event => setLaw(index, { ...law, curriculum: event.target.value })} /></label>
                <AdvancedJson open value={law} onChange={next => setLaw(index, next)} label="Quan hệ và điều kiện áp dụng"
                  hint={'"bind" gán mỗi kí hiệu cho một vai trò; "equal": [vế trái, vế phải] hoặc "atMost" cho bất đẳng thức; "scope" là các thuộc tính phải trùng nhau (thường là "of", "axis"); "when" giới hạn lúc áp dụng.'} />
                <Button type="button" size="1" variant="soft" color="red" mt="2" onClick={() => { setOpen(null); setLaws(laws.filter((_, i) => i !== index)); }}>Xoá định luật này</Button>
              </td></tr>,
            ];
          })}
        </tbody>
      </table>
      <Button type="button" size="1" variant="soft" mt="2" onClick={() => { setQuery(""); setLaws([...laws, structuredClone(NEW_LAW)]); setOpen(laws.length); }}>+ Thêm định luật</Button>
    </section>
  </div>;
}
