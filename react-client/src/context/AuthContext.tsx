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
