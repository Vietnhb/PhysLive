import type { ProblemSubmission, ExtractionResult, AmbiguityFlag } from '../types/database';

export interface EnrichedProblemItem {
  submission: ProblemSubmission;
  extraction: ExtractionResult;
  ambiguities: AmbiguityFlag[];
}

export const MOCK_PROBLEMS: EnrichedProblemItem[] = [
  {
    submission: {
      id: 'sub-001',
      user_id: 'u-teacher-hung',
      lesson_id: 'les-proj-1',
      input_type: 'text',
      raw_text: 'Một vật được ném từ độ cao 20m so với mặt đất với vận tốc đầu v0 = 15 m/s dưới góc ném 30 độ. Bỏ qua sức cản không khí, lấy g = 9.8 m/s^2. Hãy xác định quỹ đạo chuyển động, tầm xa vật đạt được và thời gian từ lúc ném đến khi chạm đất.',
      status: 'Completed',
      created_at: '2026-09-12T09:30:00Z',
    },
    extraction: {
      id: 'ext-001',
      submission_id: 'sub-001',
      schema_version_id: 'schema-kinematics-v1',
      extracted_spec: {
        topic: 'KINEMATICS',
        archetype: 'PROJECTILE_MOTION_ELEVATED',
        objects: [{ name: 'Vật ném', type: 'PointMass' }],
        quantities: [
          { symbol: 'h', name: 'Độ cao ban đầu', value: 20, unit: 'm', is_given: true, is_target: false },
          { symbol: 'v0', name: 'Vận tốc ban đầu', value: 15, unit: 'm/s', is_given: true, is_target: false },
          { symbol: 'alpha', name: 'Góc ném', value: 30, unit: 'deg', is_given: true, is_target: false },
          { symbol: 'g', name: 'Gia tốc trọng trường', value: 9.8, unit: 'm/s^2', is_given: true, is_target: false },
          { symbol: 'L_max', name: 'Tầm xa cực đại', unit: 'm', is_given: false, is_target: true },
          { symbol: 't_flight', name: 'Thời gian bay', unit: 's', is_given: false, is_target: true },
        ],
        relationships: [
          'x(t) = v0 * cos(alpha) * t',
          'y(t) = h + v0 * sin(alpha) * t - 0.5 * g * t^2'
        ],
        equations: [
          't_flight = (v0*sin(alpha) + sqrt((v0*sin(alpha))^2 + 2*g*h)) / g',
          'L_max = v0 * cos(alpha) * t_flight'
        ]
      },
      has_ambiguity: true,
      extraction_duration_ms: 1240,
      created_at: '2026-09-12T09:30:02Z',
    },
    ambiguities: [
      {
        id: 'amb-001',
        extraction_id: 'ext-001',
        field_path: 'parameters.alpha.orientation',
        prompt_question: 'Đề bài chưa nói rõ góc ném 30° là chếch lên trên hay chúc xuống dưới so với phương ngang?',
        candidate_options: [
          { label: 'Chếch lên trên so với phương ngang (+30°)', value: 'UPWARD', description: 'Vật bay lên đỉnh parabol rồi mới rơi xuống đất.' },
          { label: 'Chúc xuống dưới so với phương ngang (-30°)', value: 'DOWNWARD', description: 'Vật lao dốc ngay từ thời điểm ban đầu.' }
        ],
        resolved_value: 'UPWARD',
        status: 'resolved',
        created_at: '2026-09-12T09:30:02Z',
        resolved_at: '2026-09-12T09:31:10Z',
      }
    ]
  },
  {
    submission: {
      id: 'sub-002',
      user_id: 'u-teacher-hung',
      lesson_id: 'les-incline-1',
      input_type: 'image',
      raw_text: '[OCR Kết quả]: Vật khối lượng m = 2kg trượt không vận tốc đầu từ đỉnh mặt phẳng nghiêng dài s = 10m, góc nghiêng 30 độ. Cho g = 10 m/s^2.',
      image_url: 'https://images.unsplash.com/photo-1636466497217-26a8cbeaf0aa?auto=format&fit=crop&w=600&q=80',
      status: 'Completed',
      created_at: '2026-09-12T10:15:00Z',
    },
    extraction: {
      id: 'ext-002',
      submission_id: 'sub-002',
      schema_version_id: 'schema-dynamics-v1',
      extracted_spec: {
        topic: 'DYNAMICS',
        archetype: 'INCLINED_PLANE_SLIDER',
        objects: [{ name: 'Khối hộp', type: 'RigidBody' }, { name: 'Mặt phẳng dốc', type: 'FixedPlane' }],
        quantities: [
          { symbol: 'm', name: 'Khối lượng', value: 2, unit: 'kg', is_given: true, is_target: false },
          { symbol: 's', name: 'Chiều dài dốc', value: 10, unit: 'm', is_given: true, is_target: false },
          { symbol: 'alpha', name: 'Góc nghiêng', value: 30, unit: 'deg', is_given: true, is_target: false },
          { symbol: 'g', name: 'Gia tốc trọng trường', value: 10, unit: 'm/s^2', is_given: true, is_target: false },
          { symbol: 'v_bottom', name: 'Vận tốc tại chân dốc', unit: 'm/s', is_given: false, is_target: true },
        ],
        relationships: ['a = g*(sin(alpha) - mu*cos(alpha))', 'v^2 = 2*a*s'],
      },
      has_ambiguity: true,
      extraction_duration_ms: 1850,
      created_at: '2026-09-12T10:15:03Z',
    },
    ambiguities: [
      {
        id: 'amb-002',
        extraction_id: 'ext-002',
        field_path: 'parameters.friction_coefficient',
        prompt_question: 'Đề bài chưa cung cấp hệ số ma sát giữa vật và mặt dốc (mu). Bạn muốn thiết lập trường hợp nào?',
        candidate_options: [
          { label: 'Bỏ qua ma sát hoàn toàn (mu = 0)', value: 0, description: 'Chỉ có thành phần trọng lực P_x gây gia tốc.' },
          { label: 'Có ma sát trượt thông thường (mu = 0.1)', value: 0.1, description: 'Lực ma sát cản trở chuyển động F_ms = mu*m*g*cos(alpha).' },
          { label: 'Có ma sát trượt lớn (mu = 0.25)', value: 0.25, description: 'Vật trượt chậm hơn đáng kể.' }
        ],
        status: 'pending', // Đang chờ Teacher xác nhận
        created_at: '2026-09-12T10:15:03Z',
      }
    ]
  }
];
