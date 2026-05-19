import type { Session } from "next-auth";

export const ROLE_ADMIN = "ADMIN";
export const ROLE_IT_ADMIN = "IT-ADMIN";

export function hasRole(session: Session | null | undefined, role: string): boolean {
  return !!session?.roles?.includes(role);
}
