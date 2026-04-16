import { describe, it, expect } from "vitest";
import type { Session } from "next-auth";
import { hasRole, ROLE_ADMIN, ROLE_IT_ADMIN } from "@/lib/roles";

function makeSession(roles: string[]): Session {
  return {
    user: { name: "Alice", email: "alice@example.com", image: null },
    expires: new Date(Date.now() + 86400000).toISOString(),
    roles,
  };
}

describe("hasRole", () => {
  it("returns false when session is null", () => {
    expect(hasRole(null, ROLE_ADMIN)).toBe(false);
  });

  it("returns false when session is undefined", () => {
    expect(hasRole(undefined, ROLE_ADMIN)).toBe(false);
  });

  it("returns false when roles is empty", () => {
    expect(hasRole(makeSession([]), ROLE_ADMIN)).toBe(false);
  });

  it("returns true when role is present", () => {
    expect(hasRole(makeSession(["ADMIN"]), ROLE_ADMIN)).toBe(true);
  });

  it("returns false for a role the session does not have", () => {
    expect(hasRole(makeSession(["ADMIN"]), ROLE_IT_ADMIN)).toBe(false);
  });

  it("returns true for IT-ADMIN when present alongside ADMIN", () => {
    const session = makeSession(["ADMIN", "IT-ADMIN"]);
    expect(hasRole(session, ROLE_ADMIN)).toBe(true);
    expect(hasRole(session, ROLE_IT_ADMIN)).toBe(true);
  });
});

describe("role constants", () => {
  it("exposes canonical role strings matching the JWT claim values", () => {
    expect(ROLE_ADMIN).toBe("ADMIN");
    expect(ROLE_IT_ADMIN).toBe("IT-ADMIN");
  });
});
