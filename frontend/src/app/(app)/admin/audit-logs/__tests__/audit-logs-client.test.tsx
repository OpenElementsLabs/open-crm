import { describe, it, expect, afterEach, beforeEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor, render } from "@testing-library/react";
import { LanguageProvider, TooltipProvider } from "@open-elements/ui";
import {
  AuditLogsClient,
  PAGE_SIZE_OPTIONS,
  DEFAULT_PAGE_SIZE,
  PAGE_SIZE_STORAGE_KEY,
} from "../audit-logs-client";
import { de } from "@/lib/i18n/de";
import { translations } from "@/lib/i18n";
import type { AuditLogDto, Page, UserDto } from "@/lib/types";

const T = de.auditLog;

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
  usePathname: () => "/admin/audit-logs",
  useSearchParams: () => new URLSearchParams(),
}));

const mockGetAuditLogs = vi.fn();
const mockGetAuditLogEntityTypes = vi.fn();
const mockGetUsers = vi.fn();

vi.mock("@/lib/api", () => ({
  getAuditLogs: (...args: unknown[]) => mockGetAuditLogs(...args),
  getAuditLogEntityTypes: (...args: unknown[]) => mockGetAuditLogEntityTypes(...args),
  getUsers: (...args: unknown[]) => mockGetUsers(...args),
}));

function makeEntry(overrides: Partial<AuditLogDto> = {}): AuditLogDto {
  return {
    id: "log-1",
    entityType: "CompanyDto",
    entityId: "550e8400-e29b-41d4-a716-446655440000",
    action: "INSERT",
    user: "Max Mustermann",
    createdAt: "2026-04-25T14:30:00Z",
    ...overrides,
  };
}

function makeUser(overrides: Partial<UserDto> = {}): UserDto {
  return {
    id: "u-1",
    name: "Max Mustermann",
    email: "max@example.com",
    avatarUrl: null,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

function makePage<T>(
  items: T[],
  opts: { totalElements?: number; size?: number; number?: number } = {},
): Page<T> {
  const size = opts.size ?? 20;
  const totalElements = opts.totalElements ?? items.length;
  return {
    content: items,
    page: {
      size,
      number: opts.number ?? 0,
      totalElements,
      totalPages: Math.max(1, Math.ceil(totalElements / size)),
    },
  };
}

function renderClient() {
  return render(
    <LanguageProvider translations={translations} defaultLanguage="de">
      <TooltipProvider>
        <AuditLogsClient />
      </TooltipProvider>
    </LanguageProvider>,
  );
}

beforeEach(() => {
  localStorageMock.clear();
  // Default: dropdown population calls succeed with empty data so tests don't
  // accidentally surface entity-types or users they didn't explicitly opt into.
  mockGetAuditLogEntityTypes.mockResolvedValue([]);
  mockGetUsers.mockResolvedValue(makePage<UserDto>([]));
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("AuditLogsClient", () => {
  describe("constants (matches behaviors.md)", () => {
    it("exposes the canonical page-size options 10/20/50/100/200", () => {
      expect(Array.from(PAGE_SIZE_OPTIONS)).toEqual([10, 20, 50, 100, 200]);
    });

    it("defaults to 20 items per page", () => {
      expect(DEFAULT_PAGE_SIZE).toBe(20);
    });

    it("persists user-selected page size under pageSize.auditLogs", () => {
      expect(PAGE_SIZE_STORAGE_KEY).toBe("pageSize.auditLogs");
    });
  });

  describe("loading and empty states", () => {
    it("renders skeleton while loading", () => {
      mockGetAuditLogs.mockReturnValue(new Promise(() => {}));
      renderClient();
      expect(screen.getByTestId("audit-logs-loading")).toBeInTheDocument();
    });

    it("renders empty state when no entries exist", async () => {
      mockGetAuditLogs.mockResolvedValue(makePage<AuditLogDto>([], { totalElements: 0 }));
      renderClient();
      await waitFor(() => {
        expect(screen.getByTestId("audit-logs-empty")).toBeInTheDocument();
        expect(screen.getByText(T.empty)).toBeInTheDocument();
      });
    });

    it("renders error state when the fetch rejects", async () => {
      const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
      mockGetAuditLogs.mockRejectedValue(new Error("boom"));
      renderClient();
      await waitFor(() => {
        expect(screen.getByTestId("audit-logs-error")).toBeInTheDocument();
        expect(screen.getByText(T.loadError)).toBeInTheDocument();
      });
      // Empty state must NOT be shown — that would mislead the user.
      expect(screen.queryByTestId("audit-logs-empty")).toBeNull();
      consoleError.mockRestore();
    });
  });

  describe("table rendering", () => {
    it("renders all five columns for an entry", async () => {
      mockGetAuditLogs.mockResolvedValue(
        makePage([
          makeEntry({
            entityType: "CompanyDto",
            entityId: "550e8400-e29b-41d4-a716-446655440000",
            action: "INSERT",
            user: "Max Mustermann",
          }),
        ]),
      );
      renderClient();
      await waitFor(() => {
        expect(screen.getByText("CompanyDto")).toBeInTheDocument();
      });
      expect(screen.getByText("550e8400-e29b-41d4-a716-446655440000")).toBeInTheDocument();
      expect(screen.getByText("INSERT")).toBeInTheDocument();
      expect(screen.getByText("Max Mustermann")).toBeInTheDocument();
    });

    it("renders System-user entries in the table", async () => {
      mockGetAuditLogs.mockResolvedValue(
        makePage([makeEntry({ id: "log-sys", user: "System" })]),
      );
      renderClient();
      await waitFor(() => {
        expect(screen.getByText("System")).toBeInTheDocument();
      });
    });
  });

  describe("pagination", () => {
    it("requests size 20 on first load when localStorage is empty", async () => {
      mockGetAuditLogs.mockResolvedValue(makePage([makeEntry()]));
      renderClient();
      await waitFor(() => {
        expect(mockGetAuditLogs).toHaveBeenCalledWith({
          page: 0,
          size: 20,
          entityType: undefined,
          user: undefined,
        });
      });
    });

    it("reads page size from localStorage on mount", async () => {
      localStorageMock.setItem("pageSize.auditLogs", "50");
      mockGetAuditLogs.mockResolvedValue(makePage([makeEntry()], { size: 50 }));
      renderClient();
      await waitFor(() => {
        expect(mockGetAuditLogs).toHaveBeenCalledWith({
          page: 0,
          size: 50,
          entityType: undefined,
          user: undefined,
        });
      });
    });

    it("displays singular total label when exactly one entry exists", async () => {
      mockGetAuditLogs.mockResolvedValue(makePage([makeEntry()], { totalElements: 1 }));
      renderClient();
      await waitFor(() => {
        expect(screen.getByText(/· 1 Eintrag/)).toBeInTheDocument();
      });
    });

    it("displays plural total label for multiple entries", async () => {
      mockGetAuditLogs.mockResolvedValue(
        makePage([makeEntry()], { totalElements: 35 }),
      );
      renderClient();
      await waitFor(() => {
        expect(screen.getByText(/· 35 Einträge/)).toBeInTheDocument();
      });
    });

    it("hides prev/next buttons when totalPages <= 1", async () => {
      mockGetAuditLogs.mockResolvedValue(
        makePage([makeEntry()], { totalElements: 5, size: 20 }),
      );
      renderClient();
      await waitFor(() => {
        expect(screen.getByText("CompanyDto")).toBeInTheDocument();
      });
      expect(screen.queryByText(T.pagination.previous)).toBeNull();
      expect(screen.queryByText(T.pagination.next)).toBeNull();
    });

    it("shows prev/next when totalPages > 1; prev disabled on first page", async () => {
      mockGetAuditLogs.mockResolvedValue(
        makePage([makeEntry()], { totalElements: 35, size: 20 }),
      );
      renderClient();
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
      mockGetAuditLogs.mockResolvedValue(
        makePage([makeEntry()], { totalElements: 35, size: 20 }),
      );
      renderClient();
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
      mockGetAuditLogs.mockResolvedValue(
        makePage([makeEntry()], { totalElements: 35, size: 20 }),
      );
      renderClient();
      await waitFor(() => {
        expect(screen.getByText(T.pagination.next)).toBeInTheDocument();
      });
      mockGetAuditLogs.mockClear();
      mockGetAuditLogs.mockResolvedValue(
        makePage([makeEntry()], { totalElements: 35, size: 20, number: 1 }),
      );
      fireEvent.click(screen.getByText(T.pagination.next));
      await waitFor(() => {
        expect(mockGetAuditLogs).toHaveBeenCalledWith({
          page: 1,
          size: 20,
          entityType: undefined,
          user: undefined,
        });
      });
    });

    it("requests page 0 when previous is clicked", async () => {
      mockGetAuditLogs.mockResolvedValue(
        makePage([makeEntry()], { totalElements: 35, size: 20 }),
      );
      renderClient();
      await waitFor(() => {
        expect(screen.getByText(T.pagination.next)).toBeInTheDocument();
      });
      fireEvent.click(screen.getByText(T.pagination.next));
      await waitFor(() => {
        expect(mockGetAuditLogs).toHaveBeenLastCalledWith({
          page: 1,
          size: 20,
          entityType: undefined,
          user: undefined,
        });
      });
      mockGetAuditLogs.mockClear();
      fireEvent.click(screen.getByText(T.pagination.previous));
      await waitFor(() => {
        expect(mockGetAuditLogs).toHaveBeenCalledWith({
          page: 0,
          size: 20,
          entityType: undefined,
          user: undefined,
        });
      });
    });
  });

  describe("filter dropdown population", () => {
    it("loads entity types on mount", async () => {
      mockGetAuditLogEntityTypes.mockResolvedValue(["CompanyDto", "ContactDto"]);
      mockGetAuditLogs.mockResolvedValue(makePage([makeEntry()]));
      renderClient();
      await waitFor(() => {
        expect(mockGetAuditLogEntityTypes).toHaveBeenCalled();
      });
    });

    it("loads users for the dropdown on mount", async () => {
      mockGetUsers.mockResolvedValue(
        makePage([makeUser({ name: "Alice" }), makeUser({ id: "u-2", name: "Bob" })]),
      );
      mockGetAuditLogs.mockResolvedValue(makePage([makeEntry()]));
      renderClient();
      await waitFor(() => {
        expect(mockGetUsers).toHaveBeenCalledWith({ size: 200 });
      });
    });
  });
});
