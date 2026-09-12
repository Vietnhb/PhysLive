import type { Simulation } from '../types/database';

export const MOCK_SIMULATIONS: Simulation[] = [
  {
    id: 'sim-proj-001',
    submission_id: 'sub-001',
    author_id: 'u-teacher-hung',
    lesson_id: 'les-proj-1',
    title: 'Mô phỏng chuyển động ném xiên từ độ cao h',
    description: 'Khảo sát quỹ đạo ném xiên của vật với các thông số vận tốc đầu v0, góc bắn alpha, độ cao h và lực cản.',
    spec_json: {
      topic: 'KINEMATICS',
      archetype: 'PROJECTILE_MOTION_ELEVATED',
      variables: [
        { id: 'v0', label: 'Vận tốc ban đầu (v₀)', symbol: 'v₀', unit: 'm/s', min: 1, max: 40, step: 0.5, default_value: 15, current_value: 15 },
        { id: 'alpha', label: 'Góc ném (α)', symbol: 'α', unit: '°', min: 0, max: 85, step: 1, default_value: 30, current_value: 30 },
        { id: 'h', label: 'Độ cao ban đầu (h)', symbol: 'h', unit: 'm', min: 0, max: 50, step: 1, default_value: 20, current_value: 20 },
        { id: 'g', label: 'Gia tốc trọng trường (g)', symbol: 'g', unit: 'm/s²', min: 1, max: 20, step: 0.1, default_value: 9.8, current_value: 9.8 },
      ],
      render_config: {
        world_bounds: { width: 100, height: 60, scale: 12 },
        vectors: [
          { name: 'v_vector', color: '#10b981', origin: 'center' },
          { name: 'g_vector', color: '#ef4444', origin: 'center' }
        ]
      },
      calculation_steps: [
        {
          step_number: 1,
          formula: 'v_{0x} = v_0 \\cdot \\cos(\\alpha), \\quad v_{0y} = v_0 \\cdot \\sin(\\alpha)',
          description: 'Phân tích thành phần vận tốc ban đầu theo hai trục toạ độ Ox và Oy.',
          result: 'v_0x = 12.99 m/s, v_0y = 7.50 m/s'
        },
        {
          step_number: 2,
          formula: 't_{flight} = \\frac{v_{0y} + \\sqrt{v_{0y}^2 + 2 g h}}{g}',
          description: 'Thời gian chuyển động từ lúc ném đến khi vật chạm đất (y = 0).',
          result: 't = 2.96 s'
        },
        {
          step_number: 3,
          formula: 'L_{max} = v_{0x} \\cdot t_{flight}',
          description: 'Tầm xa cực đại đạt được trên mặt đất phẳng.',
          result: 'L_max = 38.45 m'
        },
        {
          step_number: 4,
          formula: 'H_{max} = h + \\frac{v_{0y}^2}{2g}',
          description: 'Độ cao cực đại mà vật đạt được so với mặt đất.',
          result: 'H_max = 22.87 m'
        }
      ]
    },
    is_public: true,
    validation_status: 'passed',
    created_at: '2026-09-12T09:35:00Z',
    updated_at: '2026-09-12T09:35:00Z'
  },
  {
    id: 'sim-incline-002',
    submission_id: 'sub-002',
    author_id: 'u-teacher-hung',
    lesson_id: 'les-incline-1',
    title: 'Mô phỏng trượt dốc mặt phẳng nghiêng có ma sát',
    description: 'Quan sát các vector lực P, N, Fms và tính gia tốc chuyển động trượt.',
    spec_json: {
      topic: 'DYNAMICS',
      archetype: 'INCLINED_PLANE_SLIDER',
      variables: [
        { id: 'm', label: 'Khối lượng vật (m)', symbol: 'm', unit: 'kg', min: 0.1, max: 20, step: 0.1, default_value: 2, current_value: 2 },
        { id: 'alpha', label: 'Góc nghiêng dốc (α)', symbol: 'α', unit: '°', min: 5, max: 60, step: 1, default_value: 30, current_value: 30 },
        { id: 'mu', label: 'Hệ số ma sát trượt (μ)', symbol: 'μ', unit: '', min: 0, max: 0.6, step: 0.02, default_value: 0.1, current_value: 0.1 },
        { id: 's', label: 'Chiều dài dốc (s)', symbol: 's', unit: 'm', min: 1, max: 30, step: 0.5, default_value: 10, current_value: 10 },
      ],
      render_config: {
        world_bounds: { width: 80, height: 50, scale: 15 },
        vectors: [
          { name: 'P_gravity', color: '#f59e0b', origin: 'center' },
          { name: 'N_normal', color: '#3b82f6', origin: 'center' },
          { name: 'F_friction', color: '#ef4444', origin: 'surface' }
        ]
      },
      calculation_steps: [
        {
          step_number: 1,
          formula: 'a = g \\cdot (\\sin(\\alpha) - \\mu \\cdot \\cos(\\alpha))',
          description: 'Gia tốc chuyển động trượt của vật xuôi theo mặt phẳng dốc.',
          result: 'a = 10 * (0.5 - 0.1 * 0.866) = 4.13 m/s²'
        },
        {
          step_number: 2,
          formula: 'v = \\sqrt{2 \\cdot a \\cdot s}',
          description: 'Vận tốc của vật khi trượt đến chân dốc.',
          result: 'v = 9.09 m/s'
        }
      ]
    },
    is_public: true,
    validation_status: 'passed',
    created_at: '2026-09-12T10:20:00Z',
    updated_at: '2026-09-12T10:20:00Z'
  },
  {
    id: 'sim-car-brake-003',
    submission_id: 'sub-003',
    author_id: 'u-teacher-hung',
    lesson_id: 'les-car-1',
    title: 'Mô phỏng chuyển động ô tô phanh gấp chậm dần đều',
    description: 'Khảo sát chuyển động của xe ô tô khi hãm phanh với vận tốc ban đầu và gia tốc cản, quan sát đồ thị vận tốc v(t) giảm về 0.',
    spec_json: {
      topic: 'KINEMATICS',
      archetype: 'DECELERATED_CAR_BRAKING',
      variables: [
        { id: 'v0', label: 'Vận tốc ban đầu (v₀)', symbol: 'v₀', unit: 'm/s', min: 5, max: 40, step: 1, default_value: 20, current_value: 20 },
        { id: 'a', label: 'Gia tốc hãm phanh (a)', symbol: 'a', unit: 'm/s²', min: -10, max: -1, step: 0.5, default_value: -4, current_value: -4 },
      ],
      render_config: {
        world_bounds: { width: 120, height: 40, scale: 8 },
        vectors: [
          { name: 'v_vector', color: '#10b981', origin: 'center' },
          { name: 'a_vector', color: '#ef4444', origin: 'center' }
        ]
      },
      calculation_steps: [
        {
          step_number: 1,
          formula: 'v_0 = 72 \\text{ km/h} = \\frac{72}{3.6} = 20 \\text{ m/s}',
          description: 'Hệ thống tự động chuẩn hoá đơn vị từ km/h sang m/s (SI).',
          result: 'v_0 = 20 m/s'
        },
        {
          step_number: 2,
          formula: 't_{stop} = \\frac{0 - v_0}{a} = \\frac{-20}{-4}',
          description: 'Thời gian từ lúc đạp phanh đến khi xe dừng hẳn (v = 0).',
          result: 't = 5.0 s'
        },
        {
          step_number: 3,
          formula: 's = v_0 \\cdot t + \\frac{1}{2} a t^2 = 20 \\cdot 5 + 0.5 \\cdot (-4) \\cdot 5^2',
          description: 'Quãng đường xe đi được trong thời gian hãm phanh.',
          result: 's = 50.0 m'
        }
      ]
    },
    is_public: true,
    validation_status: 'passed',
    created_at: '2026-09-12T11:00:00Z',
    updated_at: '2026-09-12T11:00:00Z'
  }
];
