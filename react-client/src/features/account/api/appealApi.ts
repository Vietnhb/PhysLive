import axiosClient from "../../../shared/api/client";

export type Appeal = {
  id: string;
  userId: number;
  userEmail: string;
  userName: string;
  reason: string;
  content: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  resolutionNote?: string | null;
  createdAt: string;
  resolvedAt?: string | null;
};

export const createAppeal = async (payload: { reason: string; content: string }): Promise<Appeal> => {
  const res = await axiosClient.post<Appeal>("/appeals", payload);
  return res.data;
};

export const getMyAppeals = async (): Promise<Appeal[]> => {
  const res = await axiosClient.get<Appeal[]>("/appeals/my");
  return res.data;
};

export const getAdminAppeals = async (): Promise<Appeal[]> => {
  const res = await axiosClient.get<Appeal[]>("/admin/appeals");
  return res.data;
};

export const resolveAppeal = async (id: string, payload: { status: "APPROVED" | "REJECTED"; resolutionNote?: string }): Promise<Appeal> => {
  const res = await axiosClient.put<Appeal>(`/admin/appeals/${id}/resolve`, payload);
  return res.data;
};
