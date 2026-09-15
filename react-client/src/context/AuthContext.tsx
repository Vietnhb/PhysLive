import React, { createContext, useContext, useState, useEffect } from 'react';
import type { User, RoleType } from '../types/database';
import { MOCK_USERS, getStoredUser, setStoredUser } from '../mocks/mockUsers';
import { getToken, setToken, clearToken } from '../utils/token';
import { isTokenExpired } from '../utils/jwt';

interface AuthContextType {
  currentUser: User | null;
  isAuthenticated: boolean;
  switchRole: (role: RoleType) => void;
  setUser: (user: User | null) => void;
  loginWithUser: (user: User, token?: string) => void;
  login: (email: string, password?: string) => boolean;
  loginDemo: (role: string) => void;
  register: (email: string, password?: string, fullName?: string, role?: string) => boolean;
  updateProfile: (fullName: string, schoolName?: string) => boolean;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUserState] = useState<User | null>(() => getStoredUser());

  // Kiểm tra token còn hợp lệ và có currentUser không
  const token = getToken();
  const isAuthenticated = Boolean(currentUser && token && !isTokenExpired(token));

  useEffect(() => {
    // Tự động clear nếu token hết hạn
    const currentToken = getToken();
    if (currentToken && isTokenExpired(currentToken)) {
      clearToken();
      setCurrentUserState(null);
      setStoredUser(null);
    }
  }, []);

  const switchRole = (role: RoleType) => {
    const targetUser = MOCK_USERS.find(u => u.role === role) || MOCK_USERS[0];
    setCurrentUserState(targetUser);
    setStoredUser(targetUser);
    if (!getToken()) {
      setToken(`mock_token_${role.toLowerCase()}`);
    }
  };

  const setUser = (user: User | null) => {
    setCurrentUserState(user);
    setStoredUser(user);
  };

  const loginWithUser = (user: User, authToken?: string) => {
    const tokenToSave = authToken || `mock_token_${user.role.toLowerCase()}`;
    setToken(tokenToSave);
    setCurrentUserState(user);
    setStoredUser(user);
  };

  const login = (email: string, _password?: string): boolean => {
    const cleanEmail = email.trim().toLowerCase();
    const found = MOCK_USERS.find(u => u.email.toLowerCase() === cleanEmail);
    const userToLogin: User = found || {
      id: `u-${Date.now()}`,
      role_id: 'r-teacher',
      role: 'TEACHER',
      email: email,
      full_name: email.split('@')[0] || 'Giáo viên Demo',
      school_name: 'THPT Chuyên Khoa Học Tự Nhiên',
      status: 'active',
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };
    loginWithUser(userToLogin);
    return true;
  };

  const loginDemo = (role: string) => {
    const targetRole = role.toUpperCase() as RoleType;
    const targetUser = MOCK_USERS.find(u => u.role === targetRole) || MOCK_USERS[0];
    loginWithUser(targetUser);
  };

  const register = (email: string, _password?: string, fullName?: string, role?: string): boolean => {
    const roleType: RoleType = (role?.toUpperCase() as RoleType) || 'STUDENT';
    const newUser: User = {
      id: `u-${Date.now()}`,
      role_id: `r-${roleType.toLowerCase()}`,
      role: roleType,
      email,
      full_name: fullName || email.split('@')[0] || 'Người dùng mới',
      school_name: 'Trường THPT PhysLive',
      status: 'active',
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };
    loginWithUser(newUser);
    return true;
  };

  const updateProfile = (fullName: string, schoolName?: string): boolean => {
    if (!currentUser) return false;
    const updated: User = {
      ...currentUser,
      full_name: fullName,
      school_name: schoolName !== undefined ? schoolName : currentUser.school_name,
      updated_at: new Date().toISOString(),
    };
    setCurrentUserState(updated);
    setStoredUser(updated);
    return true;
  };

  const logout = () => {
    clearToken();
    setCurrentUserState(null);
    setStoredUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        isAuthenticated,
        switchRole,
        setUser,
        loginWithUser,
        login,
        loginDemo,
        register,
        updateProfile,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
