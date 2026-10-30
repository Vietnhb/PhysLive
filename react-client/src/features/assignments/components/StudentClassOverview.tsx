import { Badge, Button, Callout, Card, Heading, Text } from "@radix-ui/themes";
import { motion, useReducedMotion } from "motion/react";
import type { StudentClassSummary } from "../../school/api/schoolApi";
import LearningIcon from "../../../shared/ui/LearningIcon";
import DotField from "../../../shared/effects/DotField";
import { CardLoadingSkeleton } from "../../../shared/ui/LoadingSkeleton";

type Props = {
  studentName: string;
  schoolName?: string | null;
  classes: StudentClassSummary[];
  loading: boolean;
  pendingAssignments: number;
  completedAssignments: number;
  assignmentsLoading: boolean;
  assignmentsError: string;
  classesError: string;
  onRetryClasses: () => void;
  onAssignments: () => void;
  onExplore: () => void;
};

export function StudentClassOverview({ studentName, schoolName, classes, loading, pendingAssignments, completedAssignments, assignmentsLoading, assignmentsError, classesError, onRetryClasses, onAssignments, onExplore }: Readonly<Props>) {
  const reducedMotion = useReducedMotion();
  const givenName = studentName.trim().split(/\s+/).at(-1) || studentName;
  const message = assignmentsLoading ? "Đang cập nhật tình hình bài tập của bạn…" : assignmentsError ? "Chưa tải được tình hình bài tập. Hãy thử làm mới danh sách bên dưới." : pendingAssignments > 0
    ? `Bạn có ${pendingAssignments} bài tập cần hoàn thành.`
    : completedAssignments > 0 ? "Bạn đã nộp tất cả bài tập hiện tại. Bạn có thể xem lại bài nộp và nhận xét của giáo viên." : "Chưa có bài tập được giao. Bạn có thể khám phá kho mô phỏng trong lúc chờ.";
  const assignmentCount = (value: number) => assignmentsLoading || assignmentsError ? "—" : value;
  const stats = [
    { label: "Cần làm / làm lại", value: assignmentCount(pendingAssignments) },
    { label: "Đã nộp", value: assignmentCount(completedAssignments) },
    { label: "Lớp học", value: loading || classesError ? "—" : classes.length },
    { label: "Tổng bài tập", value: assignmentCount(pendingAssignments + completedAssignments) },
  ];
  return <motion.div initial={reducedMotion ? false : { opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: .25 }}>
    <section className="student-hero" aria-label="Tổng quan học tập">
      <DotField gap={22} radius={110} />
      <div className="student-hero-copy">
        {schoolName && <span className="student-hero-school">{schoolName}</span>}
        <h1>Xin chào, <span className="text-shine">{givenName}</span></h1>
        <p>{message}</p>
        <div className="student-hero-actions"><button type="button" onClick={onAssignments} className="student-hero-cta"><LearningIcon name="book" />Xem bài tập</button><Button variant="ghost" onClick={onExplore}>Khám phá mô phỏng</Button></div>
      </div>
      <dl className="student-hero-stats">
        {stats.map(stat => <div key={stat.label}><dt>{stat.label}</dt><dd>{stat.value}</dd></div>)}
      </dl>
    </section>
    <section className="student-classes" aria-labelledby="student-classes-title">
      <div className="student-section-title"><Heading as="h2" size="4" id="student-classes-title">Lớp của tôi</Heading></div>
      {loading ? <CardLoadingSkeleton cards={3} label="Đang tải thông tin lớp…" /> : classesError ? <Callout.Root color="red"><Callout.Text>{classesError}</Callout.Text><Button variant="ghost" color="red" onClick={onRetryClasses}>Thử lại</Button></Callout.Root> : classes.length === 0 ? <Card className="student-quiet-state"><Text color="gray">Chưa được xếp vào lớp học.</Text></Card> :
        <div className="student-class-list">{classes.map(item => <Card asChild className="student-class-card" key={item.id} size="3"><motion.article whileHover={reducedMotion ? undefined : { y: -2 }} transition={{ duration: .18 }}>
          <div className="student-class-heading"><div className="student-class-title"><Heading as="h3" size="4">{item.name}</Heading><Badge color="indigo" variant="soft" size="2">Khối {item.gradeLevel}</Badge></div><Text as="p" size="2" color="gray">{item.subject || "Chưa cập nhật môn học"} · Năm học {item.schoolYear}</Text><Text size="1" color="gray" className="student-class-size">{item.classmateCount} bạn cùng lớp</Text></div>
          <div className="student-class-teacher"><Text size="1" color="gray">Giáo viên phụ trách</Text>{item.teachers.length ? <ul>{item.teachers.map(teacher => <li key={teacher.id}><Text size="2">{teacher.fullName}</Text></li>)}</ul> : <Text size="2">Chưa phân công giáo viên</Text>}</div>
        </motion.article></Card>)}</div>}
    </section>
  </motion.div>;
}
