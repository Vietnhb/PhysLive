import type { PermissionCode } from './permissions';

export type AccountStatus = 'ACTIVE' | 'WARNED' | 'SUSPENDED_READ_ONLY';

export type User = {
  id: number; email: string; fullName: string; role: string; active?: boolean; isActive?: boolean;
  accountStatus?: AccountStatus;
  suspensionReason?: string | null;
  suspendedUntil?: string | null;
  institutionId?: string | null; schoolId?: string | null; schoolName?: string | null;
  dateOfBirth?: string | null; lastLogin?: string | null; avatarUrl?: string | null;
  billingRequired?: boolean; mustChangePassword?: boolean;
  /** Permission codes granted through user_permissions (STAFF: TEACH, DEPARTMENT_HEAD_PHYSICS; REVIEWER: CONTENT_EDIT, CONTENT_REVIEW). */
  permissions?: PermissionCode[];
};

export type LoginResponse = { token: string; user: User };