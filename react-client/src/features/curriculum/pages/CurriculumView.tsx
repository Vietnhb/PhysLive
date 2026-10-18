import { useEffect, useState, type FormEvent } from "react";
import { Badge, Button as ThemeButton, Flex, Select, Text, TextField } from "@radix-ui/themes";
import { adminCurriculum, createCurriculumNode, toggleCurriculum, type CurriculumTree } from "../api/curriculumApi";
import { apiMessage } from "../../../shared/lib/apiError";
import { curriculumParentOptions, curriculumPath } from "../model/curriculumUtils";
import type { CurriculumKind } from "../model/curriculumUtils";
import { formatTopicCode, formatGradeCode, formatLessonCode } from "../model/curriculumCodeGenerator";
import { PageHeader, Panel, Loading, ErrorNotice, Button, FormField } from "../../../shared/ui/ManagementUI";

export function CurriculumView() {
  const [tree, setTree] = useState<CurriculumTree | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [name, setName] = useState("");
  const [kind, setKind] = useState<"topic" | "module" | "level" | "lesson">("topic");
  const [parentId, setParentId] = useState("");

  const load = async () => {
    setLoading(true);
    try {
      setTree(await adminCurriculum());
      setError("");
    } catch (e) {
      setError(apiMessage(e, "Không thể tải chương trình học."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, []);

  const create = async (event: FormEvent) => {
    event.preventDefault();
    if (!name.trim() || (kind !== "topic" && !parentId)) return;
    setBusy(true);
    try {
      setTree(await createCurriculumNode(curriculumPath(kind, parentId), { name: name.trim() }));
      setName("");
      setParentId("");
    } catch (e) {
      setError(apiMessage(e, "Không thể tạo mục chương trình học mới."));
    } finally {
      setBusy(false);
    }
  };

  const toggle = async (type: "topic" | "module" | "level" | "lesson", id: string) => {
    setBusy(true);
    try {
      setTree(await toggleCurriculum(type, id));
    } catch (e) {
      setError(apiMessage(e, "Không thể cập nhật trạng thái chương trình học."));
    } finally {
      setBusy(false);
    }
  };

  const parentOptions = curriculumParentOptions(kind, tree);

  return (
    <div className="admin-content">
      <PageHeader
        title="Chương trình học chuẩn hóa"
        description="Quản lý cây chương trình phân cấp: Gốc (K) → Môn/Học phần (K01) → Khối lớp (K01-10) → Bài học chi tiết (K01-10-01)."
      />
      {error && <ErrorNotice error={error} onRetry={() => void load()} />}
      {loading ? (
        <Loading />
      ) : (
        <>
          <Panel title="Thêm nội dung chương trình">
            <form className="admin-form-grid admin-form-inline" onSubmit={create}>
              <FormField label="Cấp bậc phân hệ" htmlFor="managed-curriculum-kind">
                <Select.Root size="3" value={kind} onValueChange={value => { setKind(value as typeof kind); setParentId(""); }}>
                  <Select.Trigger id="managed-curriculum-kind" />
                  <Select.Content>
                    <Select.Item value="topic">Môn học / Chủ đề (K01)</Select.Item>
                    <Select.Item value="module">Chương / Học phần</Select.Item>
                    <Select.Item value="level">Khối lớp (K01-10)</Select.Item>
                    <Select.Item value="lesson">Bài học chi tiết (K01-10-01)</Select.Item>
                  </Select.Content>
                </Select.Root>
              </FormField>

              {kind !== "topic" && (
                <FormField label="Trực thuộc cấp cha" htmlFor="managed-curriculum-parent">
                  <Select.Root size="3" name="parentId" required value={parentId} onValueChange={setParentId}>
                    <Select.Trigger id="managed-curriculum-parent" placeholder="Chọn mục cha trực thuộc" />
                    <Select.Content>
                      {parentOptions.map(item => (
                        <Select.Item key={item.id} value={item.id}>{item.label}</Select.Item>
                      ))}
                    </Select.Content>
                  </Select.Root>
                </FormField>
              )}

              <FormField label="Tên nội dung" htmlFor="managed-curriculum-name">
                <TextField.Root
                  id="managed-curriculum-name"
                  size="3"
                  required
                  value={name}
                  onChange={event => setName(event.target.value)}
                  placeholder="Ví dụ: Cơ học cổ điển, Khối 10, Con lắc đơn…"
                />
              </FormField>

              <div className="admin-form-actions">
                <Button type="submit" primary disabled={busy}>Thêm vào chương trình</Button>
              </div>
            </form>
          </Panel>

          <Panel title="Cấu trúc chương trình & Mã định danh phân cấp (K / K01 / K01-xx / K01-xx-xx)">
            <CurriculumTreeRows tree={tree} onToggle={toggle} />
          </Panel>
        </>
      )}
    </div>
  );
}

function CurriculumTreeRows({ tree, onToggle }: Readonly<{ tree: CurriculumTree | null; onToggle: (type: CurriculumKind, id: string) => void }>) {
  return (
    <div className="admin-tree">
      {tree?.topics.map((topic, topicIdx) => {
        const topicCode = formatTopicCode(topicIdx + 1);
        return (
          <CurriculumTopicRow
            key={topic.id}
            topic={topic}
            topicCode={topicCode}
            onToggle={onToggle}
          />
        );
      })}
    </div>
  );
}

function CurriculumTopicRow({
  topic,
  topicCode,
  onToggle,
}: Readonly<{
  topic: CurriculumTree["topics"][number];
  topicCode: string;
  onToggle: (type: CurriculumKind, id: string) => void;
}>) {
  return (
    <div className="admin-tree-topic">
      <TreeRow code={topicCode} label={topic.name} active={topic.enabled} onToggle={() => onToggle("topic", topic.id)} isTopic />
      {topic.modules.map(module => (
        <CurriculumModuleRow key={module.id} module={module} topicCode={topicCode} onToggle={onToggle} />
      ))}
    </div>
  );
}

function CurriculumModuleRow({
  module,
  topicCode,
  onToggle,
}: Readonly<{
  module: CurriculumTree["topics"][number]["modules"][number];
  topicCode: string;
  onToggle: (type: CurriculumKind, id: string) => void;
}>) {
  return (
    <div className="admin-tree-child">
      <TreeRow code={`${topicCode}`} label={`Chương: ${module.name}`} active={module.active} onToggle={() => onToggle("module", module.id)} />
      {module.levels.map(level => {
        const gradeCode = formatGradeCode(topicCode, level.name);
        return (
          <CurriculumLevelRow key={level.id} level={level} gradeCode={gradeCode} onToggle={onToggle} />
        );
      })}
    </div>
  );
}

function CurriculumLevelRow({
  level,
  gradeCode,
  onToggle,
}: Readonly<{
  level: CurriculumTree["topics"][number]["modules"][number]["levels"][number];
  gradeCode: string;
  onToggle: (type: CurriculumKind, id: string) => void;
}>) {
  return (
    <div className="admin-tree-grandchild">
      <TreeRow code={gradeCode} label={`Khối lớp: ${level.name}`} active={level.active} onToggle={() => onToggle("level", level.id)} />
      {level.lessons.map((lesson, lessonIdx) => {
        const lessonCode = formatLessonCode(gradeCode, lessonIdx + 1);
        return (
          <div className="admin-tree-lesson" key={lesson.id}>
            <TreeRow code={lessonCode} label={`Bài học: ${lesson.name}`} active={lesson.active} onToggle={() => onToggle("lesson", lesson.id)} />
          </div>
        );
      })}
    </div>
  );
}

function TreeRow({
  code,
  label,
  active,
  onToggle,
  isTopic,
}: Readonly<{
  code?: string;
  label: string;
  active: boolean;
  onToggle: () => void;
  isTopic?: boolean;
}>) {
  return (
    <div className="admin-tree-row">
      <Flex align="center" gap="2">
        {code && (
          <Badge color={isTopic ? "indigo" : "cyan"} variant="surface" size="1">
            {code}
          </Badge>
        )}
        <Text weight={isTopic ? "bold" : "medium"}>{label}</Text>
      </Flex>
      <ThemeButton
        type="button"
        variant="soft"
        color={active ? "indigo" : "gray"}
        size="1"
        onClick={onToggle}
      >
        {active ? "Đang áp dụng" : "Đã tạm ẩn"}
      </ThemeButton>
    </div>
  );
}