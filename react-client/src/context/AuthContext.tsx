import React, { createContext, useContext, useState, useEffect } from 'react';
import type { User, RoleType } from '../types/database';
import { MOCK_USERS, getStoredUser, setStoredUser } from '../mocks/mockUsers';

interface AuthContextType {
  currentUser: User;
  switchRole: (role: RoleType) => void;
  setUser: (user: User) => void;
  isAuthenticated: boolean;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUserState] = useState<User>(getStoredUser());

  useEffect(() => {
    setStoredUser(currentUser);
  }, [currentUser]);

  const switchRole = (role: RoleType) => {
    const targetUser = MOCK_USERS.find(u => u.role === role) || MOCK_USERS[0];
    setCurrentUserState(targetUser);
  };

  const setUser = (user: User) => {
    setCurrentUserState(user);
  };

  const logout = () => {
    // Để tiện test giao diện, khi logout ta có thể chuyển về student hoặc giữ trạng thái
    localStorage.removeItem('token');
  };

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        switchRole,
        setUser,
        isAuthenticated: true,
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
