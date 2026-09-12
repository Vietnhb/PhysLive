import { API_URL, IS_MOCK_ENABLED } from "../config/api";
import type { User, RoleType } from "../types/database";
import axiosClient from "./axios";
import {
  getAdminUsers,
  disableUserInStorage,
  restoreUserInStorage,
  changeUserRoleInStorage,
  getStoredUser,
} from "../mocks/mockUsers";

export const getMe = async (): Promise<User> => {
  if (IS_MOCK_ENABLED) {
    const user = getStoredUser();
    if (user) return user;
  }
  try {
    const res = await axiosClient.get<User>(`${API_URL}/user/me`);
    return res.data;
  } catch (err) {
    const fallback = getStoredUser();
    if (fallback) return fallback;
    throw err;
  }
};

export const getUsers = async (): Promise<User[]> => {
  if (IS_MOCK_ENABLED) {
    return getAdminUsers();
  }
  try {
    const res = await axiosClient.get<User[]>(`${API_URL}/user/all`);
    return res.data;
  } catch {
    // Fallback to local admin user store if backend API is not available
    return getAdminUsers();
  }
};

export const disableUserApi = async (userId: string): Promise<User[]> => {
  if (!IS_MOCK_ENABLED) {
    try {
      await axiosClient.put(`${API_URL}/user/${userId}/disable`);
    } catch {
      // Backend not implemented yet, fallback to mock storage
    }
  }
  return disableUserInStorage(userId);
};

export const restoreUserApi = async (userId: string): Promise<User[]> => {
  if (!IS_MOCK_ENABLED) {
    try {
      await axiosClient.put(`${API_URL}/user/${userId}/restore`);
    } catch {
      // Backend not implemented yet, fallback to mock storage
    }
  }
  return restoreUserInStorage(userId);
};

export const changeUserRoleApi = async (userId: string, newRole: RoleType): Promise<User[]> => {
  if (!IS_MOCK_ENABLED) {
    try {
      await axiosClient.put(`${API_URL}/user/${userId}/role`, { role: newRole });
    } catch {
      // Backend not implemented yet, fallback to mock storage
    }
  }
  return changeUserRoleInStorage(userId, newRole);
};