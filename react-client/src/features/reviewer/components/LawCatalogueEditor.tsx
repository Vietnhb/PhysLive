import { useMemo, useState } from "react";
import { AdvancedJson } from "./JsonEditor";
import type { JsonObject } from "../model/jsonRows";
import { codeFrom, parseRelation, plainName, pretty, relationOf, relationText, sameAs, symbols, print, type Ast } from "../model/lawText";
import "../styles/lawbook.css";

type Catalogue = JsonObject;
type Law = JsonObject;
type Bind = Record<string, JsonObject>;
type Names = Record<string, unknown>;

const record = (value: unknown): Names => value && typeof value === "object" && !Array.isArray(value) ? value as Names : {};
const without = (source: Names, ...keys: string[]) => Object.fromEntries(Object.entries(source).filter(([key]) => !keys.includes(key)));
const bindOf = (law: Law) => record(law.bind) as Bind;
const UNSORTED = "Chưa xếp vào phần nào";
const partOf = (law: Law) => String(law.curriculum ?? "").split(" – ")[0].trim() || UNSORTED;

/** What the reviewer still has to fill in before this law can check anything. */
function gaps(law: Law, roles: Names): string[] {
  const out: string[] = [];
  if (!String(law.title ?? "").trim()) out.push("chưa có tên");
  if (!relationOf(law)) out.push("chưa có công thức");
  const unnamed = Object.entries(bindOf(law)).filter(([, rule]) => !rule?.role || !(String(rule.role) in roles)).map(([symbol]) => symbol);
  if (unnamed.length) out.push(`chưa nói ${unnamed.join(", ")} là đại lượng nào`);
  return out;
}

/** "tất cả lực tác dụng lên vật — loại: lực ma sát trượt" */
function meaning(rule: JsonObject, roles: Names, kinds: Names): string {
  const role = String(rule?.role ?? "");
  if (!role) return "chưa chọn đại lượng";
  const where = record(rule.where);
  const kind = where.kind;
  const kindText = kind === undefined ? "" : kind === "*" ? "thuộc một loại bất kì" : `loại: ${(Array.isArray(kind) ? kind : [kind]).map(item => plainName(kinds[String(item)], String(item))).join(" / ")}`;
  const extra = [kindText, Object.keys(where).some(key => key !== "kind") ? "có điều kiện riêng" : "", rule.global ? "dùng chung cho cả mô hình" : ""].filter(Boolean);
  return `${rule.many ? "tất cả " : ""}${plainName(roles[role], role).replace(rule.many ? /^(một|mỗi) / : /^$/, "")}${extra.length ? ` — ${extra.join(", ")}` : ""}`;
}

/** The fine print of a law, in a sentence. */
function conditions(law: Law, roles: Names): string[] {
  const out: string[] = [];
  const scope = Array.isArray(law.scope) ? law.scope.map(String) : [];
  if (scope.length) out.push(`Xét các đại lượng thuộc ${scope.map(sameAs).join(", ")}.`);
  if (law.when !== undefined) out.push(`Chỉ xét những lúc ${pretty(print(law.when as Ast))} khác 0.`);
  const unless = record(law.unless);
  if (unless.role) out.push(`Bỏ qua khi mô hình có ${plainName(roles[String(unless.role)], String(unless.role))}${unless.where ? " (thuộc loại nêu trong phần nâng cao)" : ""}.`);
  return out;
}

/**
 * The laws a topic checks simulations against, laid out like a formula sheet: grouped by part of the programme,
 * each law with its formula, what every letter stands for and when it applies. With onChange it is editable.
 */
export function LawBook({ value, onChange }: Readonly<{ value: Catalogue; onChange?: (next: Catalogue) => void }>) {
  const [query, setQuery] = useState("");
  const [editing, setEditing] = useState<number | null>(null);
  const laws = useMemo(() => (Array.isArray(value.laws) ? value.laws as Law[] : []), [value.laws]);
  const roles = record(value.roles);
  const kinds = { ...record(value.forceKinds), ...record(value.kinds) };
  const constants = record(value.constants);
  const needle = query.trim().toLowerCase();
  const hit = (law: Law) => !needle || [law.title, law.curriculum, relationText(law), ...Object.values(bindOf(law)).map(rule => plainName(roles[String(rule?.role)], ""))].some(text => String(text ?? "").toLowerCase().includes(needle));
  const groups = new Map<string, number[]>();
  laws.forEach((law, index) => { if (index === editing || hit(law)) groups.set(partOf(law), [...(groups.get(partOf(law)) ?? []), index]); });
  const unfinished = laws.filter(law => gaps(law, roles).length).length;
  const setLaws = (next: Law[]) => onChange?.({ ...value, laws: next });
  const add = () => { setQuery(""); setLaws([{ id: "", title: "", curriculum: "", scope: ["of"], bind: {} }, ...laws]); setEditing(0); };

  // The book sits inside the topic form: Enter in one of its boxes must not save the whole topic.
  return <div className="lawbook" onKeyDown={event => { if (event.key === "Enter" && (event.target as HTMLElement).tagName === "INPUT") event.preventDefault(); }}>
    <div className="lawbook-bar">
      <input className="lawbook-input lawbook-search" type="search" placeholder="Tìm định luật, công thức hoặc đại lượng…" value={query} onChange={event => setQuery(event.target.value)} aria-label="Tìm định luật" />
      <span className="lawbook-count">{laws.length} định luật · {new Set(laws.map(partOf)).size} phần{unfinished > 0 && <b> · {unfinished} cần bổ sung</b>}</span>
      {onChange && <button type="button" className="lawbook-button is-primary" onClick={add}>+ Thêm định luật</button>}
    </div>

    {laws.length === 0 && <p className="lawbook-empty">Chủ đề này chưa có định luật nào, nên mô phỏng thuộc chủ đề chỉ được gắn nhãn “minh hoạ”.{onChange ? " Bấm “Thêm định luật” để bắt đầu." : ""}</p>}
    {laws.length > 0 && groups.size === 0 && <p className="lawbook-empty">Không có định luật nào khớp với “{query}”.</p>}

    {[...groups].map(([part, indexes]) => <details key={part} className="lawbook-part" open>
      <summary><span>{part}</span><small>{indexes.length} định luật</small></summary>
      {indexes.map(index => {
        const law = laws[index], missing = gaps(law, roles), notes = conditions(law, roles);
        if (onChange && editing === index) return <LawForm key={index} law={law} roles={roles} kinds={kinds} constants={constants} curricula={[...new Set(laws.map(item => String(item.curriculum ?? "")).filter(Boolean))]}
          onChange={next => setLaws(laws.map((item, i) => i === index ? next : item))} onDone={() => setEditing(null)}
          onDelete={() => { setEditing(null); setLaws(laws.filter((_, i) => i !== index)); }}
          takenIds={laws.filter((_, i) => i !== index).map(item => String(item.id ?? ""))} />;
        return <article key={index} className={`law${missing.length ? " has-gap" : ""}`}>
          <div className="law-text">
            <h4>{String(law.title || "Định luật chưa đặt tên")}</h4>
            {String(law.curriculum ?? "").includes(" – ") && <p className="law-lesson">{String(law.curriculum).split(" – ").slice(1).join(" – ")}</p>}
            {notes.map(note => <p key={note} className="law-note">{note}</p>)}
            {missing.length > 0 && <p className="law-gap">Cần bổ sung: {missing.join("; ")}.</p>}
          </div>
          <div className="law-maths">
            <div className="law-formula">{pretty(relationText(law)) || "—"}</div>
            <dl className="law-legend">{Object.entries(bindOf(law)).map(([symbol, rule]) => <div key={symbol}><dt>{symbol}</dt><dd>{meaning(rule, roles, kinds)}</dd></div>)}</dl>
          </div>
          {onChange && <button type="button" className="lawbook-button" onClick={() => setEditing(index)}>Sửa</button>}
        </article>;
      })}
    </details>)}

    <RoleDictionary value={value} laws={laws} onChange={onChange} />
  </div>;
}

function LawForm({ law, roles, kinds, constants, curricula, takenIds, onChange, onDone, onDelete }: Readonly<{ law: Law; roles: Names; kinds: Names; constants: Names; curricula: string[]; takenIds: string[]; onChange: (next: Law) => void; onDone: () => void; onDelete: () => void }>) {
  const current = relationText(law);
  // The box keeps what is being typed, even half a formula; the law changes only when the text reads as one.
  const [draft, setDraft] = useState<{ base: string; text: string; problem: string } | null>(null);
  const typed = draft && draft.base === current ? draft : null;
  const bind = bindOf(law);
  const scope = Array.isArray(law.scope) ? law.scope.map(String) : [];
  const write = (text: string) => {
    const read = parseRelation(text);
    if (typeof read === "string") { setDraft({ base: current, text, problem: text.trim() ? read : "" }); return; }
    // Letters follow the formula: a new one gets a row to fill in, one that left and was never filled in goes away.
    const used = [...symbols(read.left), ...symbols(read.right), ...symbols(law.when)].filter(symbol => !(symbol in constants));
    const next: Bind = {};
    for (const [symbol, rule] of Object.entries(bind)) if (used.includes(symbol) || rule?.role) next[symbol] = rule;
    for (const symbol of used) next[symbol] ??= { role: "" };
    const updated = { ...without(law, "equal", "atMost"), bind: next, [read.sign === "=" ? "equal" : "atMost"]: [read.left, read.right] };
    setDraft({ base: relationText(updated), text, problem: "" });
    onChange(updated);
  };
  const setRule = (symbol: string, rule: JsonObject | null) => {
    const next = { ...bind };
    if (rule) next[symbol] = rule; else delete next[symbol];
    onChange({ ...law, bind: next });
  };
  // A new law gets its code from its name; a law that already has one keeps it, since saved results refer to it.
  const setTitle = (title: string) => onChange({ ...law, title, id: !law.id || law.id === codeFrom(String(law.title ?? ""), takenIds) ? codeFrom(title, takenIds) : law.id });
  const inFormula = symbols([0, relationOf(law)?.left, relationOf(law)?.right, law.when]);
  const kindNames = Object.keys(kinds);
  const scopeChoices = [...new Set(["of", "axis", ...scope])];

  return <article className="law is-editing">
    <div className="lawform">
      <label className="lawform-field"><span>Tên định luật</span>
        <input className="lawbook-input" value={String(law.title ?? "")} placeholder="Định luật II Newton: m·a bằng tổng các lực" onChange={event => setTitle(event.target.value)} /></label>
      <label className="lawform-field"><span>Thuộc phần nào của chương trình</span>
        <input className="lawbook-input" list="lawbook-curricula" value={String(law.curriculum ?? "")} placeholder="Động lực học – Ba định luật Newton" onChange={event => onChange({ ...law, curriculum: event.target.value })} />
        <datalist id="lawbook-curricula">{curricula.map(item => <option key={item} value={item} />)}</datalist></label>

      <label className="lawform-field is-wide"><span>Công thức</span>
        <input className="lawbook-input lawform-formula" spellCheck={false} value={typed ? typed.text : current} placeholder="m·d(v)/dt = ΣF" aria-invalid={Boolean(typed?.problem)} onChange={event => write(event.target.value)} />
        {typed?.problem ? <em className="lawform-problem">{typed.problem}</em>
          : <small>Nhân: <kbd>·</kbd> hoặc <kbd>*</kbd> · luỹ thừa: <kbd>v^2</kbd> · tổng: <kbd>ΣF</kbd> hoặc <kbd>sum(F)</kbd> · đạo hàm theo thời gian: <kbd>d(v)/dt</kbd> · căn: <kbd>sqrt(x)</kbd> · trị tuyệt đối: <kbd>|x|</kbd> · bất đẳng thức: <kbd>≤</kbd> hoặc <kbd>&lt;=</kbd></small>}</label>

      <div className="lawform-field is-wide"><span>Mỗi kí hiệu là đại lượng nào</span>
        {Object.keys(bind).length === 0 ? <small>Viết công thức ở trên; từng kí hiệu sẽ hiện ra ở đây để bạn chọn đại lượng.</small>
          : <table className="lawform-symbols"><thead><tr><th>Kí hiệu</th><th>Là đại lượng</th><th>Lấy bao nhiêu</th>{kindNames.length > 0 && <th>Chỉ loại</th>}<th /></tr></thead><tbody>
            {Object.entries(bind).map(([symbol, rule]) => {
              const where = record(rule.where), simple = Object.keys(where).every(key => key === "kind") && (where.kind === undefined || typeof where.kind === "string");
              return <tr key={symbol}>
                <td><b>{symbol}</b></td>
                <td><select className="lawbook-input" value={String(rule.role ?? "")} aria-label={`Đại lượng của ${symbol}`} onChange={event => setRule(symbol, { ...rule, role: event.target.value })}>
                  <option value="">— chọn đại lượng —</option>
                  {rule.role && !(String(rule.role) in roles) ? <option value={String(rule.role)}>{String(rule.role)} (chưa có trong danh mục)</option> : null}
                  {Object.entries(roles).map(([id, description]) => <option key={id} value={id}>{plainName(description, id)}</option>)}
                </select></td>
                <td><select className="lawbook-input" value={rule.many ? "many" : "one"} aria-label={`Số lượng của ${symbol}`} onChange={event => setRule(symbol, event.target.value === "many" ? { ...rule, many: true, min: 1 } : without(rule, "many", "min"))}>
                  <option value="one">một đại lượng</option><option value="many">tất cả (để cộng lại)</option>
                </select></td>
                {kindNames.length > 0 && <td>{simple ? <select className="lawbook-input" value={String(where.kind ?? "")} aria-label={`Loại của ${symbol}`} onChange={event => setRule(symbol, event.target.value ? { ...rule, where: { kind: event.target.value } } : without(rule, "where"))}>
                  <option value="">mọi loại</option>{kindNames.map(kind => <option key={kind} value={kind}>{plainName(kinds[kind], kind)}</option>)}
                </select> : <small>có điều kiện riêng (xem phần nâng cao)</small>}</td>}
                <td>{!inFormula.includes(symbol) && <button type="button" className="lawbook-button is-quiet" onClick={() => setRule(symbol, null)} title="Kí hiệu này không còn trong công thức">Bỏ</button>}</td>
              </tr>;
            })}</tbody></table>}
      </div>

      <div className="lawform-field is-wide"><span>Chỉ so sánh các đại lượng thuộc</span>
        <div className="lawform-checks">{scopeChoices.map(attribute => <label key={attribute}><input type="checkbox" checked={scope.includes(attribute)} onChange={event => onChange({ ...law, scope: event.target.checked ? [...scope, attribute] : scope.filter(item => item !== attribute) })} />{sameAs(attribute)}</label>)}</div>
      </div>
    </div>
    <details className="lawform-advanced"><summary>Nâng cao: mã định luật và điều kiện đặc biệt</summary>
      <AdvancedJson open value={law} onChange={onChange} label="Dữ liệu đầy đủ của định luật" hint="Chỉ cần mở khi định luật có điều kiện mà biểu mẫu trên chưa diễn đạt được (when, unless, where)." />
    </details>
    <div className="lawform-actions">
      <button type="button" className="lawbook-button is-danger" onClick={() => { if (window.confirm("Xoá định luật này khỏi chủ đề?")) onDelete(); }}>Xoá định luật</button>
      <button type="button" className="lawbook-button is-primary" onClick={onDone}>Xong</button>
    </div>
  </article>;
}

/** The quantities laws can talk about. The AI tags each quantity of a model with one of these names. */
function RoleDictionary({ value, laws, onChange }: Readonly<{ value: Catalogue; laws: Law[]; onChange?: (next: Catalogue) => void }>) {
  const [name, setName] = useState("");
  const roles = record(value.roles);
  const uses = (id: string) => laws.filter(law => Object.values(bindOf(law)).some(rule => rule?.role === id) || record(law.unless).role === id).length;
  const setRoles = (next: Names) => onChange?.({ ...value, roles: next });
  const add = () => { if (name.trim()) { setRoles({ ...roles, [codeFrom(name, Object.keys(roles))]: name.trim() }); setName(""); } };
  return <details className="lawbook-part lawbook-roles">
    <summary><span>Danh mục đại lượng</span><small>{Object.keys(roles).length} đại lượng</small></summary>
    <p className="lawbook-hint">Định luật được viết bằng các đại lượng trong danh mục này. Khi AI dựng mô hình, nó gắn mỗi đại lượng của mô hình với một tên ở đây, nhờ vậy hệ thống biết định luật nào cần kiểm.</p>
    <table className="lawform-symbols"><thead><tr><th>Đại lượng</th><th>Dùng trong</th>{onChange && <th />}</tr></thead><tbody>
      {Object.entries(roles).map(([id, description]) => <tr key={id}>
        <td>{onChange ? <input className="lawbook-input" value={String(description ?? "")} aria-label={`Tên đại lượng ${id}`} onChange={event => setRoles({ ...roles, [id]: event.target.value })} /> : String(description ?? id)}</td>
        <td className="lawbook-uses">{uses(id) ? `${uses(id)} định luật` : "chưa dùng"}</td>
        {onChange && <td>{uses(id) === 0 && <button type="button" className="lawbook-button is-quiet" onClick={() => setRoles(without(roles, id))}>Xoá</button>}</td>}
      </tr>)}
    </tbody></table>
    {onChange && <div className="lawbook-bar"><input className="lawbook-input" value={name} placeholder="Tên đại lượng mới, ví dụ: tốc độ góc của vật quay" aria-label="Tên đại lượng mới" onChange={event => setName(event.target.value)} onKeyDown={event => { if (event.key === "Enter") { event.preventDefault(); add(); } }} />
      <button type="button" className="lawbook-button" disabled={!name.trim()} onClick={add}>+ Thêm đại lượng</button></div>}
  </details>;
}

export const LawCatalogueEditor = LawBook;
