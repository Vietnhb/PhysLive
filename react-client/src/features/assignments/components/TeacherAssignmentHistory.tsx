import { useMemo, useState } from "react";
import { Badge, Button, Heading, IconButton, Select, Text } from "@radix-ui/themes";
import type { Assignment, StudentOption, TeacherClassOption } from "../../../shared/types/physlive";
import { questionPrompt } from "../model/teacherAssignmentUtils";
import LearningIcon from "../../../shared/ui/LearningIcon";
import "../styles/assignment-studio.css";

type TeacherAssignmentHistoryProps = {
  workspaceLayout: boolean;
  items: Assignment[];
  classes: TeacherClassOption[];
  students: StudentOption[];
  selectedAssignmentId?: string;
  loading: boolean;
  onRefresh: () => void;
  onOpenSubmissions: (assignment: Assignment) => void;
  onToggleOpen: (assignment: Assignment) => void;
};

const ALL = "__all";
const ACTIVITY_LABEL: Record<string, string> = {
  PREDICT_OBSERVE_EXPLAIN: "Dự đoán – giải thích", MEASUREMENT: "Đo đại lượng",
  PARAMETER_INVESTIGATION: "Khảo sát tham số", FREE_EXPLORATION: "Khám phá tự do",
};
const shortDate = (value: string) => new Date(value).toLocaleString("vi-VN", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });

export function TeacherAssignmentHistory({
  workspaceLayout, items, classes, students, selectedAssignmentId, loading, onRefresh, onOpenSubmissions, onToggleOpen,
}: Readonly<TeacherAssignmentHistoryProps>) {
  /* "overdue" is judged against the moment this list was opened, so rendering stays pure */
  const [openedAt] = useState(() => Date.now());
  const [classFilter, setClassFilter] = useState("");
  const [studentFilter, setStudentFilter] = useState("");
  const [expanded, setExpanded] = useState<string | null>(null);
  const visibleStudents = useMemo(() => {
    if (!classFilter) return students;
    return classes.find(item => item.id === classFilter)?.students ?? [];
  }, [classFilter, classes, students]);
  const filteredItems = useMemo(() => items.filter(item =>
    (!classFilter || item.classId === classFilter)
    && (!studentFilter || item.studentIds.includes(Number(studentFilter)))), [classFilter, items, studentFilter]);
  const studentNames = useMemo(() => new Map(students.map(student => [student.id, student.fullName])), [students]);
  const openCount = items.filter(item => item.status === "ACTIVE").length;

  return (
    <section className={`studio-card studio-history${workspaceLayout ? " assignment-workspace-panel assignment-history-panel" : ""}`} aria-labelledby="assigned-work-title">
      <header className="studio-header">
        <div>
          <Heading as="h2" size="4" weight="bold" id="assigned-work-title">Bài đã giao</Heading>
          <Text as="p" size="1" color="gray">{items.length} bài · {openCount} đang mở</Text>
        </div>
        <IconButton variant="soft" color="gray" aria-label="Làm mới danh sách bài đã giao" onClick={onRefresh} disabled={loading}><LearningIcon name="refresh" /></IconButton>
      </header>
      <div className="studio-history__filters">
        <Select.Root value={classFilter || ALL} onValueChange={value => { setClassFilter(value === ALL ? "" : value); setStudentFilter(""); }}>
          <Select.Trigger aria-label="Lọc theo lớp" />
          <Select.Content position="popper" className="studio-select-content"><Select.Item value={ALL}>Tất cả lớp</Select.Item>{classes.map(item => <Select.Item key={item.id} value={item.id}>Lớp {item.name}</Select.Item>)}</Select.Content>
        </Select.Root>
        <Select.Root value={studentFilter || ALL} onValueChange={value => setStudentFilter(value === ALL ? "" : value)}>
          <Select.Trigger aria-label="Lọc theo học sinh" />
          <Select.Content position="popper" className="studio-select-content"><Select.Item value={ALL}>Tất cả học sinh</Select.Item>{visibleStudents.map(student => <Select.Item key={student.id} value={String(student.id)}>{student.fullName}</Select.Item>)}</Select.Content>
        </Select.Root>
      </div>

      {loading ? (
        <div className="studio-history__list assignment-history-list" role="status" aria-label="Đang tải danh sách bài đã giao">
          {[0, 1, 2].map(index => <div className="studio-assignment assignment-history-skeleton" key={index}>
            <span className="skeleton skeleton-title" style={{ width: `${70 - index * 12}%` }} />
            <span className="skeleton skeleton-line" style={{ width: "45%" }} />
            <span className="skeleton skeleton-line" />
          </div>)}
        </div>
      ) : filteredItems.length === 0 ? (
        <div className="studio-history__empty">
          <LearningIcon name="file" />
          <Text as="p" size="2" color="gray">{items.length === 0 ? "Chưa giao bài nào. Soạn bài ở khung bên cạnh để bắt đầu." : "Không có bài nào khớp bộ lọc."}</Text>
        </div>
      ) : (
        <div className="studio-history__list assignment-history-list">
          {filteredItems.map(item => {
            const overdue = item.status === "ACTIVE" && Boolean(item.dueAt) && new Date(item.dueAt as string).getTime() < openedAt;
            const prompt = questionPrompt(item.questions) || item.description;
            const activity = typeof item.questions === "object" ? ACTIVITY_LABEL[item.questions.activityType ?? ""] : undefined;
            const simulationTitle = item.libraryItemTitle && item.libraryItemTitle !== item.title ? item.libraryItemTitle : null;
            return <article key={item.id} className={`studio-assignment${selectedAssignmentId === item.id ? " is-selected" : ""}${item.status !== "ACTIVE" ? " is-closed" : ""}`} aria-current={selectedAssignmentId === item.id ? "true" : undefined}>
              <div className="studio-assignment__top">
                <Heading as="h3" size="2" weight="bold" className="studio-clamp-2" title={item.title}>{item.title}</Heading>
                <Badge color={item.status === "ACTIVE" ? (overdue ? "amber" : "green") : "gray"} variant="soft">{item.status === "ACTIVE" ? (overdue ? "Quá hạn" : "Đang mở") : "Đã đóng"}</Badge>
              </div>
              {prompt && <Text as="p" size="1" color="gray" className="studio-clamp-2">{prompt}</Text>}
              <ul className="studio-assignment__meta">
                <li><LearningIcon name="users" />{item.className ? `Lớp ${item.className}` : "Giao cá nhân"} · {item.studentIds.length} học sinh</li>
                <li><LearningIcon name="activity" />{item.dueAt ? `Hạn ${shortDate(item.dueAt)}` : "Không giới hạn hạn nộp"}</li>
                {activity && <li><LearningIcon name="bulb" />{activity}</li>}
                {simulationTitle && <li className="studio-clamp-1" title={simulationTitle}><LearningIcon name="atom" />{simulationTitle}</li>}
              </ul>
              <div className="studio-assignment__actions">
                <Button size="2" onClick={() => onOpenSubmissions(item)}>Xem & chấm bài</Button>
                <Button size="2" variant="soft" color="gray" onClick={() => onToggleOpen(item)} title={item.status === "ACTIVE" ? "Ngừng nhận bài nộp" : "Cho học sinh nộp tiếp"}>{item.status === "ACTIVE" ? "Đóng bài" : "Mở lại"}</Button>
                <Button size="2" variant="ghost" color="gray" aria-expanded={expanded === item.id} onClick={() => setExpanded(expanded === item.id ? null : item.id)}>Học sinh</Button>
              </div>
              {expanded === item.id && <Text as="p" size="1" color="gray" className="studio-assignment__students">
                Giao lúc {new Date(item.assignedAt).toLocaleString("vi-VN")}. Nhận bài: {item.studentIds.map(id => studentNames.get(id) || `Học sinh #${id}`).join(", ")}
              </Text>}
            </article>;
          })}
        </div>
      )}
    </section>
  );
}
