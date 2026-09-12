// Type definitions bám sát 100% sơ đồ ERD Supabase (14 bảng) và nghiệp vụ PhysLive

export type RoleType = 'TEACHER' | 'STUDENT' | 'REVIEWER' | 'ADMIN';

export interface User {
  id: string; // uuid
  role_id: string;
  role: RoleType;
  email: string;
  full_name: string;
  status: 'active' | 'suspended' | 'pending';
  school_name?: string;
  created_at: string;
  updated_at: string;
}

export interface Topic {
  id: string; // uuid
  code: string; // 'KINEMATICS' | 'DYNAMICS' | 'CIRCUITS'
  name: string;
  description: string;
  created_at: string;
}

export interface Module {
  id: string;
  topic_id: string;
  name: string;
  description: string;
  order_index: number;
  is_active: boolean;
  created_at: string;
}

export interface Level {
  id: string; // uuid
  module_id: string; // FK -> Module
  name: string; // e.g. "Cơ bản", "Nâng cao", "Khối 10", "Khối 11"
  order_index: number;
  is_active: boolean;
  created_at: string;
}

export interface ClassLesson {
  id: string; // uuid
  level_id: string; // FK -> Level (1-1)
  teacher_id: string; // FK -> Users (Teacher ID)
  name: string; // Tên bài học / Tiết dạy
  class_name: string; // Lớp áp dụng (e.g. "Lớp 10A1", "Lớp 11 Lý")
  explanation_notes: string; // "Mô tả bài giải của giảng viên trong bài học trước"
  is_active: boolean;
  created_at: string;
}

export interface Lesson {
  id: string;
  module_id: string;
  name: string;
  grade_level: number; // 10, 11, 12
  description: string;
  order_index: number;
  is_active: boolean;
  created_at: string;
}

export interface TopicSchema {
  id: string;
  module_id: string;
  version: string;
  schema_definition: Record<string, any>;
  solver_contract: Record<string, any>;
  status: 'draft' | 'approved' | 'retired';
  created_by: string;
  created_at: string;
}

export interface ProblemSubmission {
  id: string;
  user_id: string;
  lesson_id?: string;
  input_type: 'text' | 'image' | 'paste';
  raw_text: string;
  image_url?: string;
  status: 'Draft' | 'Processing' | 'Completed' | 'Failed';
  created_at: string;
}

export interface ExtractedQuantity {
  symbol: string;
  name: string;
  value?: number;
  unit: string;
  is_given: boolean;
  is_target: boolean;
}

export interface ExtractionResult {
  id: string;
  submission_id: string;
  schema_version_id?: string;
  extracted_spec: {
    topic: string;
    archetype: string;
    objects: Array<{ name: string; type: string }>;
    quantities: ExtractedQuantity[];
    relationships: string[];
    equations?: string[];
  };
  has_ambiguity: boolean;
  extraction_duration_ms: number;
  created_at: string;
}

export interface AmbiguityFlag {
  id: string;
  extraction_id: string;
  field_path: string; // e.g. "parameters.initial_angle.direction"
  prompt_question: string;
  candidate_options: Array<{
    label: string;
    value: string | number;
    description?: string;
  }>;
  resolved_value?: string | number;
  adjudicated_by?: string;
  status: 'pending' | 'resolved' | 'dismissed';
  created_at: string;
  resolved_at?: string;
}

export interface SimulationVariable {
  id: string;
  label: string;
  symbol: string;
  unit: string;
  min: number;
  max: number;
  step: number;
  default_value: number;
  current_value: number;
}

export interface SimulationFrame {
  t: number; // time in seconds
  x: number; // coordinates
  y: number;
  vx: number; // velocity components
  vy: number;
  ax: number; // acceleration
  ay: number;
  energy_kinetic: number;
  energy_potential: number;
  energy_total: number;
}

export interface Simulation {
  id: string;
  submission_id?: string;
  author_id: string;
  lesson_id?: string;
  title: string;
  description: string;
  spec_json: {
    topic: 'KINEMATICS' | 'DYNAMICS' | 'CIRCUITS';
    archetype: string;
    variables: SimulationVariable[];
    render_config: {
      world_bounds: { width: number; height: number; scale: number };
      vectors: Array<{ name: string; color: string; origin: string }>;
    };
    calculation_steps: Array<{
      step_number: number;
      formula: string;
      description: string;
      result: string;
    }>;
  };
  is_public: boolean;
  validation_status: 'passed' | 'failed' | 'unvalidated';
  export_data?: string;
  created_at: string;
  updated_at: string;
}

export interface ValidationLog {
  id: string;
  simulation_id: string;
  closed_form_output: Record<string, any>;
  numerical_output: Record<string, any>;
  tolerance_threshold: number;
  discrepancy: number;
  passed: boolean;
  failure_reason?: string;
  execution_time_ms: number;
  created_at: string;
}

export interface Assignment {
  id: string;
  simulation_id: string;
  teacher_id: string;
  class_lesson_id?: string;
  title: string;
  instructions: string;
  target_question: string; // Câu hỏi học sinh phải dự đoán
  target_quantity_symbol: string; // e.g. "Tầm xa x_max", "Thời gian rơi"
  due_date: string;
  created_at: string;
}

export interface StudentPractice {
  id: string;
  assignment_id: string;
  student_id: string;
  predicted_answer: string;
  actual_answer?: string;
  replayed_presets?: Record<string, any>;
  is_correct?: boolean;
  submitted_at: string;
}
