import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { MOCK_TOPICS } from '../../mocks/mockTaxonomy';
import { MOCK_SIMULATIONS } from '../../mocks/mockSimulations';

export const LibraryPage: React.FC = () => {
  const [selectedTopic, setSelectedTopic] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');

  const filteredSimulations = MOCK_SIMULATIONS.filter(sim => {
    const matchTopic = selectedTopic === 'ALL' || sim.spec_json.topic === selectedTopic;
    const matchSearch = sim.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                        sim.description.toLowerCase().includes(searchTerm.toLowerCase());
    return matchTopic && matchSearch;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Page Header */}
      <div>
        <h1 style={{ margin: '0 0 6px', fontSize: '24px', fontWeight: 800, color: '#0f172a' }}>
          Thư viện học liệu & Trình duyệt phân cấp (Task H06 & H10)
        </h1>
        <p style={{ margin: 0, fontSize: '14px', color: '#64748b' }}>
          Tra cứu toàn bộ mô phỏng, bài toán theo phân cấp chuẩn: Chủ đề (Topic) → Chuyên đề (Module) → Bài học (Lesson).
        </p>
      </div>

      {/* Filter & Taxonomy Browser Bar */}
      <div
        style={{
          backgroundColor: '#ffffff',
          borderRadius: '14px',
          padding: '16px 20px',
          border: '1px solid #e2e8f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
          flexWrap: 'wrap',
        }}
      >
        {/* Topic Filter Pills */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '13px', fontWeight: 700, color: '#334155' }}>Chủ đề:</span>
          <button
            onClick={() => setSelectedTopic('ALL')}
            style={{
              padding: '6px 14px',
              borderRadius: '9999px',
              border: 'none',
              backgroundColor: selectedTopic === 'ALL' ? '#2563eb' : '#f1f5f9',
              color: selectedTopic === 'ALL' ? '#ffffff' : '#475569',
              fontSize: '12px',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Tất cả (3 Topics)
          </button>
          {MOCK_TOPICS.map((topic) => (
            <button
              key={topic.id}
              onClick={() => setSelectedTopic(topic.code)}
              style={{
                padding: '6px 14px',
                borderRadius: '9999px',
                border: 'none',
                backgroundColor: selectedTopic === topic.code ? '#2563eb' : '#f1f5f9',
                color: selectedTopic === topic.code ? '#ffffff' : '#475569',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              {topic.name}
            </button>
          ))}
        </div>

        {/* Search input */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', background: '#f8fafc', padding: '6px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', minWidth: '240px' }}>
          <span>🔍</span>
          <input
            type="text"
            placeholder="Tìm tên mô phỏng..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ border: 'none', background: 'transparent', outline: 'none', fontSize: '13px', width: '100%' }}
          />
        </div>
      </div>

      {/* Grid of Simulations */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: '20px' }}>
        {filteredSimulations.map((sim) => (
          <div
            key={sim.id}
            style={{
              backgroundColor: '#ffffff',
              borderRadius: '16px',
              border: '1px solid #e2e8f0',
              padding: '20px',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
            }}
          >
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                <span style={{ fontSize: '11px', padding: '3px 8px', borderRadius: '4px', backgroundColor: '#eff6ff', color: '#1d4ed8', fontWeight: 700 }}>
                  {sim.spec_json.topic}
                </span>
                <span style={{ fontSize: '12px', color: '#10b981', fontWeight: 600 }}>
                  ✓ Đã kiểm chứng
                </span>
              </div>

              <h3 style={{ margin: '0 0 8px', fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>
                {sim.title}
              </h3>
              <p style={{ margin: '0 0 16px', fontSize: '13px', color: '#64748b', lineHeight: 1.5 }}>
                {sim.description}
              </p>

              <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '16px' }}>
                {sim.spec_json.variables.map((v) => (
                  <span
                    key={v.id}
                    style={{
                      fontSize: '11px',
                      backgroundColor: '#f1f5f9',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      color: '#475569',
                    }}
                  >
                    {v.symbol}: {v.default_value}{v.unit}
                  </span>
                ))}
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '14px', borderTop: '1px solid #f1f5f9' }}>
              <span style={{ fontSize: '12px', color: '#94a3b8' }}>
                {new Date(sim.created_at).toLocaleDateString('vi-VN')}
              </span>
              <Link
                to={`/player?id=${sim.id}`}
                style={{
                  padding: '7px 16px',
                  backgroundColor: '#2563eb',
                  color: '#ffffff',
                  borderRadius: '8px',
                  fontSize: '13px',
                  fontWeight: 600,
                  textDecoration: 'none',
                }}
              >
                Mở mô phỏng 🚀
              </Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
