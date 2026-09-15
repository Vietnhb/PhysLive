import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export const Sidebar: React.FC = () => {
  const { currentUser } = useAuth();
  const role = currentUser?.role;

  return (
    <aside className="app-sidebar">
      {/* Brand Header */}
      <div className="app-sidebar-header">
        <Link to="/" className="brand-logo">
          <img className="brand-icon" src="/favicon.ico" alt="PhysLive" />
          <div>
            <div className="brand-title">PhysLive</div>
            <div style={{ fontSize: '10px', color: '#64748b', fontWeight: 600 }}>
              Interactive Physics Lab
            </div>
          </div>
        </Link>
      </div>

      {/* Navigation Links */}
      <nav className="sidebar-nav">
        <NavLink
          to="/"
          end
          className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
        >
          <span className="sidebar-icon">🏠</span>
          <span>Bảng điều khiển</span>
        </NavLink>

        {/* TEACHER FLOW */}
        {(role === 'TEACHER' || role === 'ADMIN') && (
          <>
            <div className="sidebar-section-title">Giáo Viên (Teacher)</div>
            <NavLink
              to="/teacher/submit"
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <span className="sidebar-icon">📝</span>
              <span>Nộp đề & Trích xuất AI</span>
            </NavLink>
            <NavLink
              to="/player"
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <span className="sidebar-icon">🧪</span>
              <span>Phòng thí nghiệm Player</span>
            </NavLink>
            <NavLink
              to="/library"
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <span className="sidebar-icon">📚</span>
              <span>Thư viện & Bản nháp</span>
            </NavLink>
            <NavLink
              to="/assignments"
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <span className="sidebar-icon">📋</span>
              <span>Giao bài tập lớp học</span>
            </NavLink>
          </>
        )}

        {/* STUDENT FLOW */}
        {(role === 'STUDENT' || role === 'ADMIN') && (
          <>
            <div className="sidebar-section-title">Học Sinh (Student)</div>
            <NavLink
              to="/student/assignments"
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <span className="sidebar-icon">🎯</span>
              <span>Bài tập cần dự đoán</span>
            </NavLink>
            <NavLink
              to="/player"
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <span className="sidebar-icon">🔬</span>
              <span>Sandbox thực nghiệm</span>
            </NavLink>
          </>
        )}

        {/* REVIEWER & ADMIN */}
        {(role === 'REVIEWER' || role === 'ADMIN') && (
          <>
            <div className="sidebar-section-title">Thẩm Định & Quản Trị</div>
            <NavLink
              to="/reviewer/schemas"
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <span className="sidebar-icon">⚖️</span>
              <span>Duyệt Schema & Solver</span>
            </NavLink>
          </>
        )}

        {role === 'ADMIN' && (
          <>
            <NavLink
              to="/admin"
              className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
            >
              <span className="sidebar-icon">⚙️</span>
              <span>Quản trị tài khoản & DB</span>
            </NavLink>
          </>
        )}
      </nav>

      {/* Footer Info */}
      <div style={{ padding: '16px 20px', borderTop: '1px solid #1e293b', fontSize: '12px', color: '#64748b' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
          <span>Sprint 1 MVP</span>
          <span style={{ color: '#10b981', fontWeight: 600 }}>● Online</span>
        </div>
        <div>Web React + Spring Boot</div>
      </div>
    </aside>
  );
};
