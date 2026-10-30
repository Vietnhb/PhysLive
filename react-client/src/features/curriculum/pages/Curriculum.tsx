import { useEffect, useState } from "react";
import { curriculum } from "../api/curriculumApi";
import type { Curriculum as CurriculumTree } from "../../../shared/types/physlive";
import { formatTopicCode, formatGradeCode, formatLessonCode } from "../model/curriculumCodeGenerator";
import { CardLoadingSkeleton } from "../../../shared/ui/LoadingSkeleton";
import { topicLabel } from "../../../shared/lib/topicLabel";

type Topic = CurriculumTree["topics"][number];
type Module = Topic["modules"][number];

function Curriculum() {
  const [tree, setTree] = useState<CurriculumTree | null>(null);
  useEffect(() => { void curriculum().then(setTree).catch(() => setTree({ topics: [] })); }, []);
  const topics = tree?.topics ?? [];

  return (
    <main className="main curriculum-page">
      <div className="hero">
        <div>
          <h1>Chương trình học chuẩn hóa</h1>
          <p className="muted">Cấu trúc môn học, chương, khối lớp và bài học chi tiết dùng chung cho mô phỏng, đề bài và kiểm tra.</p>
        </div>
      </div>
      {tree === null && <CardLoadingSkeleton cards={3} label="Đang tải chương trình học…" />}
      {tree !== null && topics.length === 0 && (
        <section className="card">
          <p className="muted">Chưa có nội dung chương trình học. Quản lý hệ thống có thể tạo trong trang Quản trị chương trình học.</p>
        </section>
      )}
      {topics.map((topic, topicIdx) => {
        const topicCode = formatTopicCode(topicIdx + 1);
        return <CurriculumTopic key={topic.id} topic={topic} topicCode={topicCode} />;
      })}
    </main>
  );
}

function CurriculumTopic({ topic, topicCode }: Readonly<{ topic: Topic; topicCode: string }>) {
  return (
    <section className="curriculum-topic" aria-labelledby={`topic-${topic.id}`}>
      <h2 id={`topic-${topic.id}`}>
        <span style={{ display: "inline-block", background: "var(--accent-3)", color: "var(--accent-11)", padding: "2px 8px", borderRadius: "6px", fontSize: "14px", marginRight: "10px", verticalAlign: "middle" }}>
          {topicCode}
        </span>
        {topicLabel(topic.name)}
      </h2>
      <div className="curriculum-modules">
        {topic.modules.map(module => (
          <CurriculumModule key={module.id} module={module} topicCode={topicCode} />
        ))}
      </div>
    </section>
  );
}

function CurriculumModule({ module, topicCode }: Readonly<{ module: Module; topicCode: string }>) {
  return (
    <article className="card curriculum-module">
      <h3>
        <span style={{ fontSize: "12px", color: "var(--muted)", marginRight: "6px" }}>Chương:</span>
        {module.name}
      </h3>
      {module.levels.map(level => {
        const gradeCode = formatGradeCode(topicCode, level.name);
        return (
          <div className="curriculum-level" key={level.id}>
            <span style={{ fontWeight: 600 }}>
              <span style={{ background: "var(--gray-3)", padding: "1px 6px", borderRadius: "4px", fontSize: "12px", marginRight: "6px" }}>
                {gradeCode}
              </span>
              Khối lớp {level.name}
            </span>
            {level.lessons.length ? (
              <ul>
                {level.lessons.map((lesson, lessonIdx) => {
                  const lessonCode = formatLessonCode(gradeCode, lessonIdx + 1);
                  return (
                    <li key={lesson.id}>
                      <code style={{ fontSize: "11px", color: "var(--accent)", marginRight: "6px" }}>
                        {lessonCode}
                      </code>
                      {lesson.name}
                    </li>
                  );
                })}
              </ul>
            ) : (
              <p className="muted">Chưa có bài học chi tiết.</p>
            )}
          </div>
        );
      })}
    </article>
  );
}

export default Curriculum;
