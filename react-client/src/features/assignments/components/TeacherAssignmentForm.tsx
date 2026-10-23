import { useState, type FormEvent, type ReactNode } from "react";
import { Link } from "react-router-dom";
import { Badge, Button, Callout, Checkbox, Heading, Select, Text, TextArea, TextField } from "@radix-ui/themes";
import type { AssignmentActivityType, LibraryItem, Simulation, StudentOption, TeacherClassOption } from "../../../shared/types/physlive";
import { useAssignmentClock } from "../hooks/useAssignmentClock";
import { useTopicName } from "../../../shared/hooks/useTopicName";
import LearningIcon from "../../../shared/ui/LearningIcon";
import "../styles/assignment-studio.css";

type TeacherAssignmentFormProps = {
  workspaceLayout: boolean;
  saved: LibraryItem[];
  selectedLibrary: LibraryItem | undefined;
  libraryItemId: string;
  title: string;
  description: string;
  prompt: string;
  activityType: AssignmentActivityType;
  simulation: Simulation | null;
  simulationOptionsLoading: boolean;
  simulationOptionsError: boolean;
  targetSeriesSource: string;
  sampleTime: string;
  measurementTolerance: string;
  investigationParameter: string;
  investigationOutcome: string;
  maxScore: string;
  dueAt: string;
  students: StudentOption[];
  classes: TeacherClassOption[];
  selectedClassId: string;
  selectedStudents: number[];
  submitting: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onLibraryChange: (id: string) => void;
  onTitleChange: (value: string) => void;
  onPromptChange: (value: string) => void;
  onActivityTypeChange: (value: AssignmentActivityType) => void;
  onTargetSeriesSourceChange: (value: string) => void;
  onSampleTimeChange: (value: string) => void;
  onMeasurementToleranceChange: (value: string) => void;
  onInvestigationParameterChange: (value: string) => void;
  onInvestigationOutcomeChange: (value: string) => void;
  onMaxScoreChange: (value: string) => void;
  onDueAtChange: (value: string) => void;
  onDescriptionChange: (value: string) => void;
  onToggleStudent: (id: number) => void;
  onSelectAll: () => void;
  onClassChange: (id: string) => void;
};

const ACTIVITIES: { value: AssignmentActivityType; label: string; help: string; icon: "bulb" | "chart" | "sliders" | "play"; prompt: string }[] = [
  { value: "PREDICT_OBSERVE_EXPLAIN", label: "Dự đoán – giải thích", help: "Học sinh dự đoán trước, rồi mở mô phỏng để đối chiếu.", icon: "bulb", prompt: "Câu hỏi dự đoán" },
  { value: "MEASUREMENT", label: "Đo đại lượng", help: "Học sinh đọc một đại lượng tại thời điểm thầy cô chọn.", icon: "chart", prompt: "Yêu cầu đo và giải thích" },
  { value: "PARAMETER_INVESTIGATION", label: "Khảo sát tham số", help: "Học sinh thay đổi một tham số và tìm quy luật.", icon: "sliders", prompt: "Câu hỏi khảo sát" },
  { value: "FREE_EXPLORATION", label: "Khám phá tự do", help: "Mô phỏng mở ngay, học sinh tự quan sát và kết luận.", icon: "play", prompt: "Nhiệm vụ khám phá" },
];
const STEPS = ["Nội dung bài", "Lớp & hạn nộp", "Xem lại & giao"];

/** Value for a datetime-local `min`, in the viewer's own time zone. */
function localDateTime(time: number) {
  const date = new Date(time - new Date(time).getTimezoneOffset() * 60000);
  return date.toISOString().slice(0, 16);
}

function Field({ label, htmlFor, hint, children, required = false }: Readonly<{ label: string; htmlFor?: string; hint?: ReactNode; children: ReactNode; required?: boolean }>) {
  return <div className="studio-field">
    <Text as="label" size="2" weight="medium" htmlFor={htmlFor}>{label}{required && <span className="studio-required" aria-hidden="true"> *</span>}</Text>
    {children}
    {hint && <Text as="p" size="1" color="gray">{hint}</Text>}
  </div>;
}

export function TeacherAssignmentForm({
  workspaceLayout, saved, selectedLibrary, libraryItemId, title, description, prompt, activityType, simulation,
  simulationOptionsLoading, simulationOptionsError, targetSeriesSource, sampleTime, measurementTolerance,
  investigationParameter, investigationOutcome, maxScore, dueAt, students, classes, selectedClassId, selectedStudents,
  submitting, onSubmit, onLibraryChange, onTitleChange, onPromptChange, onActivityTypeChange, onTargetSeriesSourceChange,
  onSampleTimeChange, onMeasurementToleranceChange, onInvestigationParameterChange, onInvestigationOutcomeChange,
  onMaxScoreChange, onDueAtChange, onDescriptionChange, onToggleStudent, onSelectAll, onClassChange,
}: Readonly<TeacherAssignmentFormProps>) {
  const [step, setStep] = useState(0);
  const [search, setSearch] = useState("");
  const now = useAssignmentClock();
  const topicName = useTopicName();
  const series = simulation?.visualization?.series ?? [];
  const controls = simulation?.visualization?.controls ?? [];
  const activity = ACTIVITIES.find(item => item.value === activityType) ?? ACTIVITIES[0];
  const typeConfigValid = activityType === "MEASUREMENT"
    ? Boolean(targetSeriesSource && sampleTime.trim() && Number.isFinite(Number(sampleTime)) && measurementTolerance.trim() && Number.isFinite(Number(measurementTolerance)) && Number(measurementTolerance) >= 0)
    : activityType === "PARAMETER_INVESTIGATION" ? Boolean(investigationParameter) : true;
  const contentValid = Boolean(selectedLibrary && simulation && title.trim() && title.trim().length <= 160 && prompt.trim()) && Number(maxScore) >= 0.001 && Number(maxScore) <= 99999.999 && Number.isFinite(Number(maxScore)) && typeConfigValid;
  const dueInPast = Boolean(dueAt) && new Date(dueAt).getTime() <= now;
  const recipientsValid = Boolean(selectedClassId) && selectedStudents.length > 0 && !dueInPast;
  const selectedClass = classes.find(item => item.id === selectedClassId);
  const shownStudents = students.filter(student => student.fullName.toLocaleLowerCase("vi").includes(search.toLocaleLowerCase("vi")));
  const wholeClass = students.length > 0 && selectedStudents.length === students.length;
  const goTo = (next: number) => { if (next <= step || (step === 0 ? contentValid : recipientsValid)) setStep(next); };
  const missing = step === 0
    ? [!selectedLibrary && "chọn mô phỏng", !title.trim() && "nhập tiêu đề", !prompt.trim() && "nhập yêu cầu cho học sinh", !typeConfigValid && "cấu hình loại bài"].filter(Boolean)
    : [!selectedClassId && "chọn lớp", selectedClassId && selectedStudents.length === 0 && "chọn ít nhất một học sinh"].filter(Boolean);

  return (
    <div className={`studio-card${workspaceLayout ? " assignment-workspace-panel" : ""}`}>
      <header className="studio-header">
        <div>
          <Text as="p" size="1" weight="medium" color="gray" className="studio-eyebrow">Giao bài mới</Text>
          <Heading as="h2" size="5" weight="bold">Thiết kế bài tập mô phỏng</Heading>
        </div>
      </header>
      {saved.length === 0 ? (
        <div className="studio-empty">
          <LearningIcon name="atom" />
          <Text as="p" size="3" weight="medium">Chưa có mô phỏng nào để giao</Text>
          <Text as="p" size="2" color="gray">Hãy dựng một mô phỏng rồi chọn “Lưu vào thư viện”. Chỉ mô phỏng đã kiểm định mới giao được cho học sinh.</Text>
          <Button asChild><Link to="/workspace">Tạo mô phỏng</Link></Button>
        </div>
      ) : (
        <form noValidate onSubmit={event => { if (step < 2) { event.preventDefault(); goTo(step + 1); } else if (contentValid && recipientsValid) onSubmit(event); else { event.preventDefault(); setStep(contentValid ? 1 : 0); } }}>
          <ol className="studio-steps">{STEPS.map((label, index) => <li key={label}>
            <button type="button" aria-current={index === step ? "step" : undefined} className={index < step ? "is-done" : index === step ? "is-current" : ""}
              disabled={index > step && !(index === step + 1 && (step === 0 ? contentValid : recipientsValid))} onClick={() => goTo(index)}>
              <span className="studio-steps__dot">{index < step ? <LearningIcon name="check" /> : index + 1}</span>{label}
            </button>
          </li>)}</ol>

          <fieldset className="studio-step" disabled={submitting} hidden={step !== 0}>
            <Field label="Mô phỏng" htmlFor="assignment-library-item" required>
              <Select.Root value={libraryItemId || undefined} onValueChange={onLibraryChange}>
                <Select.Trigger id="assignment-library-item" placeholder="Chọn một mô phỏng đã kiểm định trong thư viện" className="studio-select" />
                <Select.Content position="popper" className="studio-select-content">{saved.map(item => <Select.Item key={item.id} value={item.id}>{item.title}{item.topic ? ` · ${topicName(item.topic)}` : ""}</Select.Item>)}</Select.Content>
              </Select.Root>
            </Field>
            {selectedLibrary && <div className="studio-selected">
              <LearningIcon name="atom" />
              <div><Text as="p" size="2" weight="medium">{selectedLibrary.title}</Text><Text as="p" size="1" color="gray">{topicName(selectedLibrary.topic) || "Vật lý"}</Text></div>
              <Badge color="green" variant="soft">Đã kiểm định</Badge>
            </div>}
            {simulationOptionsLoading && <Text as="p" size="1" color="gray" role="status">Đang đọc các đại lượng và tham số của mô phỏng…</Text>}
            {simulationOptionsError && <Callout.Root color="red" size="1" role="alert"><Callout.Text>Không mở được mô phỏng này để giao. Hãy chọn mô phỏng khác, hoặc mở lại nó ở trang Mô phỏng để kiểm tra.</Callout.Text></Callout.Root>}

            <Field label="Hình thức hoạt động" required>
              <div className="studio-activities" role="radiogroup" aria-label="Hình thức hoạt động">
                {ACTIVITIES.map(item => <button key={item.value} type="button" role="radio" aria-checked={activityType === item.value}
                  className={activityType === item.value ? "is-selected" : ""} onClick={() => onActivityTypeChange(item.value)}>
                  <span className="studio-activities__icon"><LearningIcon name={item.icon} /></span>
                  <span><strong>{item.label}</strong><small>{item.help}</small></span>
                </button>)}
              </div>
            </Field>

            <Field label="Tiêu đề bài" htmlFor="assignment-title" required>
              <TextField.Root id="assignment-title" size="3" maxLength={160} value={title} placeholder="Ví dụ: Ném ngang – dự đoán vận tốc chạm đất" onChange={event => onTitleChange(event.target.value)} />
            </Field>
            <Field label={activity.prompt} htmlFor="assignment-prompt" required
              hint={activityType === "PREDICT_OBSERVE_EXPLAIN" ? "Học sinh gửi dự đoán trước, sau đó mô phỏng mới mở." : "Mô phỏng mở ngay để học sinh vừa thí nghiệm vừa trả lời."}>
              <TextArea id="assignment-prompt" size="3" rows={3} value={prompt} placeholder="Ví dụ: Khi góc ném tăng từ 30° lên 45°, tầm xa tăng hay giảm? Vì sao?" onChange={event => onPromptChange(event.target.value)} />
            </Field>

            {activityType === "MEASUREMENT" && <section className="studio-config">
              <Text as="p" size="2" weight="bold">Phép đo</Text>
              <div className="studio-grid">
                <Field label="Đại lượng cần đo" htmlFor="measurement-series" required>
                  <Select.Root value={targetSeriesSource || undefined} onValueChange={onTargetSeriesSourceChange} disabled={series.length === 0}>
                    <Select.Trigger id="measurement-series" placeholder="Chọn đại lượng" className="studio-select" />
                    <Select.Content position="popper" className="studio-select-content">{series.map(item => <Select.Item key={item.source} value={item.source}>{item.label}{item.unit ? ` (${item.unit})` : ""}</Select.Item>)}</Select.Content>
                  </Select.Root>
                </Field>
                <Field label="Tại thời điểm (giây)" htmlFor="measurement-time" required hint={simulation ? `Từ ${simulation.time.at(0) ?? 0} đến ${simulation.time.at(-1) ?? 0} s` : undefined}>
                  <TextField.Root id="measurement-time" size="3" type="number" min={simulation?.time.at(0) ?? 0} max={simulation?.time.at(-1) ?? 0} step="any" value={sampleTime} onChange={event => onSampleTimeChange(event.target.value)} />
                </Field>
                <Field label="Sai số chấp nhận" htmlFor="measurement-tolerance" required hint="Cùng đơn vị với đại lượng đo.">
                  <TextField.Root id="measurement-tolerance" size="3" type="number" min="0" step="any" value={measurementTolerance} onChange={event => onMeasurementToleranceChange(event.target.value)} />
                </Field>
              </div>
              {series.length === 0 && !simulationOptionsLoading && <Text as="p" size="1" color="red">Mô phỏng này không có đại lượng nào để đo. Hãy chọn hình thức khác.</Text>}
              <Text as="p" size="1" color="gray">Đáp án đúng được lấy từ chính mô phỏng; học sinh không nhìn thấy.</Text>
            </section>}

            {activityType === "PARAMETER_INVESTIGATION" && <section className="studio-config">
              <Text as="p" size="2" weight="bold">Khảo sát</Text>
              <div className="studio-grid">
                <Field label="Tham số học sinh thay đổi" htmlFor="investigation-parameter" required>
                  <Select.Root value={investigationParameter || undefined} onValueChange={onInvestigationParameterChange} disabled={controls.length === 0}>
                    <Select.Trigger id="investigation-parameter" placeholder="Chọn tham số" className="studio-select" />
                    <Select.Content position="popper" className="studio-select-content">{controls.map(item => <Select.Item key={item.key} value={item.key}>{item.label}{item.unit ? ` (${item.unit})` : ""}</Select.Item>)}</Select.Content>
                  </Select.Root>
                </Field>
                <Field label="Đại lượng cần quan sát" htmlFor="investigation-outcome">
                  <Select.Root value={investigationOutcome || undefined} onValueChange={onInvestigationOutcomeChange} disabled={series.length === 0}>
                    <Select.Trigger id="investigation-outcome" placeholder="Không bắt buộc" className="studio-select" />
                    <Select.Content position="popper" className="studio-select-content">{series.map(item => <Select.Item key={item.source} value={item.source}>{item.label}</Select.Item>)}</Select.Content>
                  </Select.Root>
                </Field>
              </div>
              {controls.length === 0 && !simulationOptionsLoading && <Text as="p" size="1" color="red">Mô phỏng này không có tham số nào để thay đổi. Hãy chọn hình thức khác.</Text>}
              <Text as="p" size="1" color="gray">Học sinh được thử nhiều giá trị trong giới hạn của mô phỏng rồi giải thích quy luật.</Text>
            </section>}

            <div className="studio-grid">
              <Field label="Thang điểm" htmlFor="assignment-max-score">
                <TextField.Root id="assignment-max-score" size="3" type="number" min="1" step="0.5" value={maxScore} onChange={event => onMaxScoreChange(event.target.value)} />
              </Field>
              <div className="studio-field"><Text size="2" weight="medium">Cách chấm</Text>
                <Text as="p" size="2" className="studio-readonly">{activityType === "MEASUREMENT" ? "Hệ thống chấm phần kết quả đo, thầy cô xem lại" : "Thầy cô chấm theo bài làm"}</Text></div>
            </div>
          </fieldset>

          <fieldset className="studio-step" disabled={submitting} hidden={step !== 1}>
            <Field label="Lớp nhận bài" htmlFor="assignment-class" required hint={classes.length === 0 ? "Bạn chưa được nhà trường phân công vào lớp nào." : undefined}>
              <Select.Root value={selectedClassId || undefined} onValueChange={onClassChange} disabled={classes.length === 0}>
                <Select.Trigger id="assignment-class" placeholder="Chọn lớp đang phụ trách" className="studio-select" />
                <Select.Content position="popper" className="studio-select-content">{classes.map(item => <Select.Item key={item.id} value={item.id}>Lớp {item.name} · Khối {item.gradeLevel} · {item.schoolYear} ({item.students.length} học sinh)</Select.Item>)}</Select.Content>
              </Select.Root>
            </Field>
            <div className="studio-grid">
              <Field label="Hạn nộp" htmlFor="assignment-due-at" hint={dueInPast ? undefined : "Bỏ trống nếu không giới hạn."}>
                <TextField.Root id="assignment-due-at" size="3" type="datetime-local" min={localDateTime(now)} value={dueAt} color={dueInPast ? "red" : undefined} onChange={event => onDueAtChange(event.target.value)} />
                {dueInPast && <Text as="p" size="1" color="red" role="alert">Hạn nộp phải ở tương lai.</Text>}
              </Field>
              <Field label="Lời dặn thêm" htmlFor="assignment-description">
                <TextField.Root id="assignment-description" size="3" placeholder="Không bắt buộc" value={description} onChange={event => onDescriptionChange(event.target.value)} />
              </Field>
            </div>
            <div className="studio-field">
              <div className="studio-recipients__head">
                <Text size="2" weight="medium">Học sinh nhận bài <Text color="gray">({selectedStudents.length}/{students.length})</Text></Text>
                {students.length > 0 && <Button type="button" variant="soft" size="1" onClick={onSelectAll}>{wholeClass ? "Bỏ chọn tất cả" : "Chọn cả lớp"}</Button>}
              </div>
              {students.length > 0 && <TextField.Root id="student-search" size="2" placeholder="Tìm học sinh…" value={search} onChange={event => setSearch(event.target.value)}><TextField.Slot><LearningIcon name="search" /></TextField.Slot></TextField.Root>}
              <div className="studio-recipients">
                {students.length === 0 ? <Text as="p" size="2" color="gray">{selectedClassId ? "Lớp này chưa có học sinh đang hoạt động." : "Chọn lớp để thấy danh sách học sinh."}</Text>
                  : shownStudents.length === 0 ? <Text as="p" size="2" color="gray">Không có học sinh khớp “{search}”.</Text>
                  : shownStudents.map(student => <Text as="label" size="2" key={student.id} className="studio-recipient">
                    <Checkbox checked={selectedStudents.includes(student.id)} onCheckedChange={() => onToggleStudent(student.id)} />{student.fullName}
                  </Text>)}
              </div>
            </div>
          </fieldset>

          {step === 2 && <section className="studio-review" aria-label="Xem lại bài giao">
            <Badge color="iris" variant="soft">{activity.label}</Badge>
            <Heading as="h3" size="5" weight="bold">{title}</Heading>
            <Text as="p" size="3" className="studio-review__prompt">{prompt}</Text>
            {description && <Text as="p" size="2" color="gray">{description}</Text>}
            <dl className="studio-review__facts">
              <div><dt>Mô phỏng</dt><dd>{selectedLibrary?.title}</dd></div>
              <div><dt>Lớp</dt><dd>{selectedClass ? `${selectedClass.name} · Khối ${selectedClass.gradeLevel}` : "Chưa chọn"}</dd></div>
              <div><dt>Học sinh</dt><dd>{wholeClass ? `Cả lớp (${students.length})` : `${selectedStudents.length}/${students.length} học sinh`}</dd></div>
              <div><dt>Hạn nộp</dt><dd>{dueAt ? new Date(dueAt).toLocaleString("vi-VN", { dateStyle: "medium", timeStyle: "short" }) : "Không giới hạn"}</dd></div>
              <div><dt>Chấm điểm</dt><dd>{activityType === "MEASUREMENT" ? "Tự động phần kết quả đo" : "Thầy cô chấm"} · thang {maxScore}</dd></div>
              {activityType === "MEASUREMENT" && <div><dt>Phép đo</dt><dd>{series.find(item => item.source === targetSeriesSource)?.label} tại {sampleTime} s, sai số ±{measurementTolerance}</dd></div>}
              {activityType === "PARAMETER_INVESTIGATION" && <div><dt>Khảo sát</dt><dd>Thay đổi {controls.find(item => item.key === investigationParameter)?.label}{investigationOutcome ? `, quan sát ${series.find(item => item.source === investigationOutcome)?.label}` : ""}</dd></div>}
            </dl>
            {!wholeClass && <Text as="p" size="2" color="gray">Nhận bài: {students.filter(student => selectedStudents.includes(student.id)).map(student => student.fullName).join(", ")}</Text>}
          </section>}

          <footer className="studio-actions">
            {step < 2 && missing.length > 0 && <Text size="1" color="gray" className="studio-actions__hint">Còn thiếu: {missing.join(", ")}.</Text>}
            {step > 0 && <Button type="button" variant="soft" color="gray" size="3" disabled={submitting} onClick={() => setStep(step - 1)}>Quay lại</Button>}
            {step < 2 && <Button type="button" size="3" disabled={step === 0 ? !contentValid : !recipientsValid} onClick={() => goTo(step + 1)}>Tiếp tục</Button>}
            {step === 2 && <Button type="submit" size="3" loading={submitting} disabled={submitting || !contentValid || !recipientsValid || !libraryItemId}>
              Giao bài cho {wholeClass ? "cả lớp" : `${selectedStudents.length} học sinh`}
            </Button>}
          </footer>
        </form>
      )}
    </div>
  );
}
