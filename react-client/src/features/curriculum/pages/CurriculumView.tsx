import { useEffect, useState, type FormEvent } from "react";
import { Badge, Button, Callout, Dialog, Select, Switch, TextField } from "@radix-ui/themes";
import { ChevronDownIcon, ChevronRightIcon, MagnifyingGlassIcon, PlusIcon, ReloadIcon } from "@radix-ui/react-icons";
import { adminCurriculum, createCurriculumNode, toggleCurriculum, type CurriculumTree, type CurriculumTopic, type CurriculumModule } from "../api/curriculumApi";
import { apiMessage } from "../../../shared/lib/apiError";
import { topicLabel } from "../../../shared/lib/topicLabel";
import type { LocalizedName } from "../../../shared/types/localization";
import { curriculumParentOptions, curriculumPath, type CurriculumKind } from "../model/curriculumUtils";
import { extractGradeNumber, formatTopicCode, formatGradeCode, formatLessonCode } from "../model/curriculumCodeGenerator";
import { PageHeader, Loading, ErrorNotice, FormField } from "../../../shared/ui/ManagementUI";
import modal from "../../../shared/ui/ModalScroll.module.css";
import s from "./CurriculumView.module.css";

const normalize = (value: string) => value.toLocaleLowerCase("vi").normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d");
const lessonCount = (topic: CurriculumTopic) => topic.modules.reduce((sum, module) => sum + module.levels.reduce((count, level) => count + level.lessons.length, 0), 0);
const matches = (query: string, ...values: (string | LocalizedName)[]) => normalize(values.flatMap(value => typeof value === "string" ? [value] : Object.values(value)).join(" ")).includes(query);

export function CurriculumView() {
  const [tree, setTree] = useState<CurriculumTree | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [selectedId, setSelectedId] = useState("");
  const [query, setQuery] = useState("");
  const [grade, setGrade] = useState("ALL");
  const [showCreate, setShowCreate] = useState(false);
  const [formError, setFormError] = useState("");
  const [name, setName] = useState("");
  const [viName, setViName] = useState("");
  const [kind, setKind] = useState<CurriculumKind>("topic");
  const [parentId, setParentId] = useState("");

  const load = async () => {
    setLoading(true);
    try { setTree(await adminCurriculum()); setError(""); }
    catch (e) { setError(apiMessage(e, "Không thể tải chương trình học.")); }
    finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, []);

  const create = async (event: FormEvent) => {
    event.preventDefault();
    if (busy || !name.trim() || (kind === "topic" ? !viName.trim() : !parentId)) return;
    setBusy(true); setFormError(""); setNotice("");
    try {
      setTree(await createCurriculumNode(curriculumPath(kind, parentId), { name: kind === "topic" ? { en: name.trim(), vi: viName.trim() } : name.trim() }));
      setShowCreate(false); setName(""); setViName(""); setParentId("");
      setNotice("Đã thêm nội dung vào chương trình học.");
    } catch (e) { setFormError(apiMessage(e, "Không thể tạo mục chương trình học mới.")); }
    finally { setBusy(false); }
  };

  const toggle = async (type: CurriculumKind, id: string) => {
    if (busy) return;
    setBusy(true); setError(""); setNotice("");
    try { setTree(await toggleCurriculum(type, id)); setNotice("Đã cập nhật trạng thái nội dung."); }
    catch (e) { setError(apiMessage(e, "Không thể cập nhật trạng thái chương trình học.")); }
    finally { setBusy(false); }
  };

  const search = normalize(query.trim());
  const topics = (tree?.topics ?? []).map((topic, index) => ({ topic, code: formatTopicCode(index + 1) }));
  const visibleTopics = topics.filter(({ topic, code }) => topic.modules.some(module => filterLevels(module, topic, code, search, grade).length || (grade === "ALL" && matches(search, topic.name, code, module.name))) || (grade === "ALL" && matches(search, topic.name, code)));
  const selected = visibleTopics.find(({ topic }) => topic.id === selectedId) ?? visibleTopics[0];
  const grades = [...new Set(topics.flatMap(({ topic }) => topic.modules.flatMap(module => module.levels.map(level => extractGradeNumber(level.name)))))].sort();
  const parentOptions = curriculumParentOptions(kind, tree);

  return <div className="admin-content">
    <PageHeader title="Chương trình học" description="Tra cứu và quản lý chủ đề, chương và bài học."
      action={<div className={s.headerActions}><Button variant="outline" disabled={loading || busy} onClick={() => void load()}><ReloadIcon />Làm mới</Button><Button disabled={loading || busy} onClick={() => { setFormError(""); setShowCreate(true); }}><PlusIcon />Thêm nội dung</Button></div>} />
    {error && <ErrorNotice error={error} onRetry={() => void load()} />}
    {notice && <Callout.Root color="green" role="status"><Callout.Text>{notice}</Callout.Text></Callout.Root>}
    {loading && !tree ? <Loading text="Đang tải chương trình học…" /> : <section className={s.workspace} aria-label="Danh mục chương trình học" aria-busy={loading}>
      <div className={s.toolbar}>
        <TextField.Root size="3" value={query} onChange={event => setQuery(event.target.value)} placeholder="Tìm chủ đề, chương, bài học hoặc mã…" aria-label="Tìm trong chương trình học"><TextField.Slot><MagnifyingGlassIcon /></TextField.Slot></TextField.Root>
        <Select.Root size="3" value={grade} onValueChange={setGrade}><Select.Trigger aria-label="Lọc theo khối" /><Select.Content position="popper"><Select.Item value="ALL">Tất cả khối</Select.Item>{grades.map(value => <Select.Item key={value} value={value}>Khối {Number(value)}</Select.Item>)}</Select.Content></Select.Root>
      </div>
      <div className={s.columns}>
        <aside className={s.sidebar} aria-label="Chủ đề chương trình">
          <div className={s.sidebarHeading}>Chủ đề <Badge color="gray">{visibleTopics.length}</Badge></div>
          <div className={s.topicList}>{visibleTopics.map(({ topic, code }) => <button key={topic.id} type="button" className={`rt-reset ${s.topicButton} ${selected?.topic.id === topic.id ? s.selected : ""}`} aria-pressed={selected?.topic.id === topic.id} onClick={() => setSelectedId(topic.id)}>
            <span className={s.topicCode}>{code}</span><span className={s.topicName}>{topicLabel(topic.name)}<small>{topic.modules.length} chương · {lessonCount(topic)} bài học{!topic.enabled ? " · Đã ẩn" : ""}</small></span><ChevronRightIcon />
          </button>)}</div>
        </aside>
        <div className={s.content}>
          {selected ? <>
            <div className={s.contentHeading}><div><div className={s.eyebrow}>{selected.code} · {selected.topic.modules.length} chương · {lessonCount(selected.topic)} bài học</div><h2>{topicLabel(selected.topic.name)}</h2></div><StatusControl active={selected.topic.enabled} label={topicLabel(selected.topic.name)} busy={busy} onToggle={() => void toggle("topic", selected.topic.id)} /></div>
            <div className={s.chapterList} key={selected.topic.id}>
              {selected.topic.modules.map((module, index) => {
                const levels = filterLevels(module, selected.topic, selected.code, search, grade);
                if (!levels.length && (grade !== "ALL" || (search && !matches(search, selected.topic.name, selected.code, module.name)))) return null;
                return <Chapter key={JSON.stringify([module.id, search, grade])} module={module} levels={levels} index={index} searching={!!search || grade !== "ALL"} busy={busy} onToggle={toggle} />;
              })}
              {!selected.topic.modules.length && <div className={s.empty}>Chủ đề này chưa có chương. Chọn “Thêm nội dung” để tạo chương mới.</div>}
            </div>
          </> : <div className={s.empty}><h2>{topics.length ? "Không tìm thấy nội dung" : "Chưa có chương trình học"}</h2><p>{topics.length ? "Thử từ khóa khác hoặc bỏ bộ lọc để xem toàn bộ chương trình." : "Thêm chủ đề đầu tiên để bắt đầu xây dựng chương trình."}</p>{topics.length > 0 && <Button variant="soft" onClick={() => { setQuery(""); setGrade("ALL"); }}>Xóa bộ lọc</Button>}</div>}
        </div>
      </div>
    </section>}
    <Dialog.Root open={showCreate} onOpenChange={open => { if (!busy) setShowCreate(open); }}><Dialog.Content className={modal.dialog} maxWidth="560px" onEscapeKeyDown={event => { if (busy) event.preventDefault(); }} onInteractOutside={event => { if (busy) event.preventDefault(); }}><div className={modal.body}>
      <Dialog.Title>Thêm nội dung chương trình</Dialog.Title><Dialog.Description size="2" mb="5">Chọn cấp nội dung và vị trí trong chương trình học.</Dialog.Description>
      <form className={s.form} onSubmit={create}>
        {formError && <Callout.Root color="red" role="alert"><Callout.Text>{formError}</Callout.Text></Callout.Root>}
        <FormField label="Cấp nội dung" htmlFor="managed-curriculum-kind"><Select.Root size="3" value={kind} disabled={busy} onValueChange={value => { setKind(value as CurriculumKind); setParentId(""); }}><Select.Trigger id="managed-curriculum-kind" /><Select.Content position="popper"><Select.Item value="topic">Môn học / Chủ đề</Select.Item><Select.Item value="module">Chương / Học phần</Select.Item><Select.Item value="level">Khối lớp</Select.Item><Select.Item value="lesson">Bài học</Select.Item></Select.Content></Select.Root></FormField>
        {kind !== "topic" && <FormField label="Thuộc nội dung" htmlFor="managed-curriculum-parent"><Select.Root size="3" required value={parentId} disabled={busy} onValueChange={setParentId}><Select.Trigger id="managed-curriculum-parent" placeholder="Chọn nội dung cha" /><Select.Content position="popper">{parentOptions.map(item => <Select.Item key={item.id} value={item.id}>{item.label}</Select.Item>)}</Select.Content></Select.Root>{!parentOptions.length && <small>Chưa có nội dung cha. Hãy tạo cấp phía trên trước.</small>}</FormField>}
        <FormField label={kind === "topic" ? "Tên tiếng Anh" : "Tên nội dung"} htmlFor="managed-curriculum-name"><TextField.Root id="managed-curriculum-name" size="3" required maxLength={kind === "topic" ? 80 : undefined} disabled={busy} value={name} onChange={event => setName(event.target.value)} placeholder={kind === "topic" ? "Ví dụ: Kinematics" : "Nhập tên nội dung…"} /></FormField>
        {kind === "topic" && <FormField label="Tên tiếng Việt" htmlFor="managed-curriculum-name-vi"><TextField.Root id="managed-curriculum-name-vi" size="3" required maxLength={80} disabled={busy} value={viName} onChange={event => setViName(event.target.value)} placeholder="Ví dụ: Động học" /></FormField>}
        <div className={s.formActions}><Button type="button" variant="soft" color="gray" disabled={busy} onClick={() => setShowCreate(false)}>Hủy</Button><Button type="submit" loading={busy} disabled={busy || !name.trim() || (kind === "topic" ? !viName.trim() : !parentId)}>Thêm nội dung</Button></div>
      </form>
    </div></Dialog.Content></Dialog.Root>
  </div>;
}

function filterLevels(module: CurriculumModule, topic: CurriculumTopic, code: string, search: string, grade: string) {
  return module.levels.filter(level => grade === "ALL" || extractGradeNumber(level.name) === grade).map(level => {
    const gradeCode = formatGradeCode(code, level.name);
    const parentMatch = matches(search, topic.name, code, module.name, level.name, gradeCode);
    const lessons = level.lessons.map((lesson, index) => ({ ...lesson, code: formatLessonCode(gradeCode, index + 1) })).filter(lesson => parentMatch || matches(search, lesson.name, lesson.code));
    return { ...level, code: gradeCode, lessons };
  }).filter(level => !search || level.lessons.length || matches(search, topic.name, code, module.name, level.name, level.code));
}

function Chapter({ module, levels, index, searching, busy, onToggle }: Readonly<{ module: CurriculumModule; levels: ReturnType<typeof filterLevels>; index: number; searching: boolean; busy: boolean; onToggle: (kind: CurriculumKind, id: string) => Promise<void> }>) {
  const [expanded, setExpanded] = useState(searching || index === 0);
  const open = expanded;
  return <article className={s.chapter}>
    <div className={s.chapterHeading}><button type="button" className={`rt-reset ${s.chapterTrigger}`} onClick={() => setExpanded(value => !value)} aria-expanded={open} aria-controls={`chapter-${module.id}`}>
      {open ? <ChevronDownIcon /> : <ChevronRightIcon />}<span className={s.chapterNumber}>{String(index + 1).padStart(2, "0")}</span><span>{module.name}<small>{module.levels.length} khối · {module.levels.reduce((sum, level) => sum + level.lessons.length, 0)} bài học</small></span>
    </button><StatusControl active={module.active} label={module.name} busy={busy} onToggle={() => void onToggle("module", module.id)} /></div>
    {open && <div id={`chapter-${module.id}`} className={s.levels}>
      {levels.map(level => <section key={level.id} className={s.level}>
        <div className={s.levelHeading}><div><Badge variant="surface">{level.code}</Badge><strong>{level.name}</strong></div><StatusControl active={level.active} label={level.name} busy={busy} onToggle={() => void onToggle("level", level.id)} /></div>
        {level.lessons.map(lesson => <div className={s.lesson} key={lesson.id}><span className={s.lessonCode}>{lesson.code}</span><span>{lesson.name}</span><StatusControl active={lesson.active} label={lesson.name} busy={busy} onToggle={() => void onToggle("lesson", lesson.id)} /></div>)}
        {!level.lessons.length && <p className={s.levelEmpty}>Chưa có bài học.</p>}
      </section>)}
      {!levels.length && <p className={s.levelEmpty}>Chương này chưa có khối lớp.</p>}
    </div>}
  </article>;
}

function StatusControl({ active, label, busy, onToggle }: Readonly<{ active: boolean; label: string; busy: boolean; onToggle: () => void }>) {
  return <label className={s.status}><span>{active ? "Áp dụng" : "Đã ẩn"}</span><Switch size="1" checked={active} disabled={busy} onCheckedChange={onToggle} aria-label={`${active ? "Ẩn" : "Áp dụng"} ${label}`} /></label>;
}
