import type { User } from "../../../shared/auth/types";
import { hasPermission } from "../../../shared/auth/permissions";

/** What the signed-in account may do inside the reviewer console.
 *  MANAGER can edit reference data; public moderation belongs to REVIEWER. */
export type ReviewerAccess = { canEdit: boolean; canReview: boolean; isManager: boolean };

export function reviewerAccess(user: User | null | undefined): ReviewerAccess {
  const isManager = user?.role === "MANAGER";
  return {
    isManager,
    canEdit: isManager || (user?.role === "REVIEWER" && hasPermission(user, "CONTENT_EDIT")),
    canReview: user?.role === "REVIEWER" && hasPermission(user, "CONTENT_REVIEW"),
  };
}

export type ReviewerView = "overview" | "moderation" | "topics" | "gaps";
