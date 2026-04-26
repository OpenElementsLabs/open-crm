import { describe, it, expect, afterEach, beforeEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor, render } from "@testing-library/react";
import { SessionProvider } from "next-auth/react";
import { LanguageProvider, TooltipProvider } from "@open-elements/ui";
import { UsersClient } from "../users-client";
import { de } from "@/lib/i18n/de";
import { translations } from "@/lib/i18n";
import type { Page, UserDto } from "@/lib/types";

const T = de.users;

const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value;
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key];
    }),
    clear: vi.fn(() => {
      store = {};
    }),
  };
})();
Object.defineProperty(globalThis, "localStorage", { value: localStorageMock });

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  usePathname: () => "/admin/users",
  useSearchParams: () => new URLSearchParams(),
}));

const mockGetUsers = vi.fn();

vi.mock("@/lib/api", () => ({
  getUsers: (...args: unknown[]) => mockGetUsers(...args),
}));

function makeUser(overrides: Partial<UserDto> = {}): UserDto {
  return {
    id: "u-1",
    name: "Alice Example",
    email: "alice@example.com",
    avatarUrl: "https://example.com/alice.png",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

function makePage(users: UserDto[], opts: { totalElements?: number; size?: number; number?: number } = {}): Page<UserDto> {
  const size = opts.size ?? 20;
  const totalElements = opts.totalElements ?? users.length;
  return {
    content: users,
    page: {
      size,
      number: opts.number ?? 0,
      totalElements,
      totalPages: Math.max(1, Math.ceil(totalElements / size)),
    },
  };
}

function renderUsersClient() {
  return render(
    <SessionProvider session={null}>
      <LanguageProvider translations={translations} defaultLanguage="de">
        <TooltipProvider>
          <UsersClient />
        </TooltipProvider>
      </LanguageProvider>
    </SessionProvider>,
  );
}

beforeEach(() => {
  localStorageMock.clear();
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("UsersClient", () => {
  describe("loading and empty states", () => {
    it("renders skeleton while loading", () => {
      // never-resolving promise keeps loading=true
      mockGetUsers.mockReturnValue(new Promise(() => {}));
      renderUsersClient();
      expect(screen.getByTestId("users-loading")).toBeInTheDocument();
    });

    it("renders empty state when no users exist", async () => {
      mockGetUsers.mockResolvedValue(makePage([], { totalElements: 0 }));
      renderUsersClient();
      await waitFor(() => {
        expect(screen.getByTestId("users-empty")).toBeInTheDocument();
        expect(screen.getByText(T.empty)).toBeInTheDocument();
      });
    });
  });

  describe("table rendering", () => {
    it("renders avatar image when avatarUrl is present", async () => {
      mockGetUsers.mockResolvedValue(
        makePage([makeUser({ avatarUrl: "https://cdn.example.com/a.png" })]),
      );
      renderUsersClient();
      await waitFor(() => {
        const img = document.querySelector("img");
        expect(img).toBeTruthy();
        expect(img!.getAttribute("src")).toBe("https://cdn.example.com/a.png");
      });
    });

    it("renders fallback icon when avatarUrl is null", async () => {
      mockGetUsers.mockResolvedValue(
        makePage([makeUser({ avatarUrl: null })]),
      );
      renderUsersClient();
      await waitFor(() => {
        expect(screen.getByTestId("user-avatar-fallback")).toBeInTheDocument();
        expect(document.querySelector("img")).toBeNull();
      });
    });

    it("renders name and email columns", async () => {
      mockGetUsers.mockResolvedValue(
        makePage([
          makeUser({ id: "u-a", name: "Alice", email: "alice@x.test" }),
          makeUser({ id: "u-b", name: "Bob", email: "bob@x.test" }),
        ]),
      );
      renderUsersClient();
      await waitFor(() => {
        expect(screen.getByText("Alice")).toBeInTheDocument();
        expect(screen.getByText("alice@x.test")).toBeInTheDocument();
        expect(screen.getByText("Bob")).toBeInTheDocument();
        expect(screen.getByText("bob@x.test")).toBeInTheDocument();
      });
    });
  });

  describe("pagination", () => {
    it("requests size 20 on first load when localStorage is empty", async () => {
      mockGetUsers.mockResolvedValue(makePage([makeUser()]));
      renderUsersClient();
      await waitFor(() => {
        expect(mockGetUsers).toHaveBeenCalledWith({ page: 0, size: 20 });
      });
    });

    it("uses page size from localStorage when present", async () => {
      localStorageMock.setItem("pageSize.users", "50");
      mockGetUsers.mockResolvedValue(makePage([makeUser()], { size: 50 }));
      renderUsersClient();
      await waitFor(() => {
        expect(mockGetUsers).toHaveBeenCalledWith({ page: 0, size: 50 });
      });
    });

    it("displays singular total label when exactly one user exists", async () => {
      mockGetUsers.mockResolvedValue(makePage([makeUser()], { totalElements: 1 }));
      renderUsersClient();
      await waitFor(() => {
        expect(screen.getByText(/· 1 Benutzer/)).toBeInTheDocument();
      });
    });

    it("displays plural total label for multiple users", async () => {
      const users = Array.from({ length: 3 }, (_, i) =>
        makeUser({ id: `u-${i}`, name: `User ${i}`, email: `u${i}@x.test` }),
      );
      mockGetUsers.mockResolvedValue(makePage(users, { totalElements: 35 }));
      renderUsersClient();
      await waitFor(() => {
        expect(screen.getByText(/· 35 Benutzer/)).toBeInTheDocument();
      });
    });

    it("hides prev/next buttons when totalPages <= 1", async () => {
      mockGetUsers.mockResolvedValue(makePage([makeUser()], { totalElements: 5 }));
      renderUsersClient();
      await waitFor(() => {
        expect(screen.getByText("Alice Example")).toBeInTheDocument();
      });
      expect(screen.queryByText(T.pagination.previous)).toBeNull();
      expect(screen.queryByText(T.pagination.next)).toBeNull();
    });

    it("shows prev/next when totalPages > 1; prev disabled on first page", async () => {
      mockGetUsers.mockResolvedValue(
        makePage([makeUser()], { totalElements: 35, size: 20 }),
      );
      renderUsersClient();
      await waitFor(() => {
        expect(screen.getByText(T.pagination.previous)).toBeInTheDocument();
        expect(screen.getByText(T.pagination.next)).toBeInTheDocument();
      });
      const prevBtn = screen.getByText(T.pagination.previous).closest("button");
      const nextBtn = screen.getByText(T.pagination.next).closest("button");
      expect(prevBtn).toBeDisabled();
      expect(nextBtn).not.toBeDisabled();
    });

    it("disables next on the last page", async () => {
      // Two pages: 35 items at size 20 → totalPages 2. After one Next click,
      // we're on the last page (index 1) and Next must be disabled.
      mockGetUsers.mockResolvedValue(
        makePage([makeUser()], { totalElements: 35, size: 20 }),
      );
      renderUsersClient();
      await waitFor(() => {
        expect(screen.getByText(T.pagination.next)).toBeInTheDocument();
      });
      fireEvent.click(screen.getByText(T.pagination.next));
      await waitFor(() => {
        const nextBtn = screen.getByText(T.pagination.next).closest("button");
        expect(nextBtn).toBeDisabled();
      });
    });

    it("requests page 1 when next is clicked", async () => {
      mockGetUsers.mockResolvedValue(
        makePage([makeUser()], { totalElements: 35, size: 20 }),
      );
      renderUsersClient();
      await waitFor(() => {
        expect(screen.getByText(T.pagination.next)).toBeInTheDocument();
      });
      mockGetUsers.mockClear();
      mockGetUsers.mockResolvedValue(
        makePage([makeUser()], { totalElements: 35, size: 20, number: 1 }),
      );
      fireEvent.click(screen.getByText(T.pagination.next));
      await waitFor(() => {
        expect(mockGetUsers).toHaveBeenCalledWith({ page: 1, size: 20 });
      });
    });

    it("requests page 0 when previous is clicked", async () => {
      mockGetUsers.mockResolvedValue(
        makePage([makeUser()], { totalElements: 35, size: 20 }),
      );
      renderUsersClient();
      await waitFor(() => {
        expect(screen.getByText(T.pagination.next)).toBeInTheDocument();
      });
      // First go to page 1
      fireEvent.click(screen.getByText(T.pagination.next));
      await waitFor(() => {
        expect(mockGetUsers).toHaveBeenLastCalledWith({ page: 1, size: 20 });
      });
      mockGetUsers.mockClear();
      // Now go back to page 0
      fireEvent.click(screen.getByText(T.pagination.previous));
      await waitFor(() => {
        expect(mockGetUsers).toHaveBeenCalledWith({ page: 0, size: 20 });
      });
    });
  });
});
