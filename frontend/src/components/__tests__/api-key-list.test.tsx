import { describe, it, expect, afterEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor, render } from "@testing-library/react";
import { SessionProvider } from "next-auth/react";
import { LanguageProvider } from "@/lib/i18n/language-context";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ApiKeyList } from "@/components/api-key-list";
import { de } from "@/lib/i18n/de";
import type { ApiKeyDto, ApiKeyCreatedDto, Page } from "@/lib/types";

const K = de.apiKeys;

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value; }),
    removeItem: vi.fn((key: string) => { delete store[key]; }),
    clear: vi.fn(() => { store = {}; }),
  };
})();
Object.defineProperty(globalThis, "localStorage", { value: localStorageMock });

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  usePathname: () => "/api-keys",
  useSearchParams: () => new URLSearchParams(),
}));

const mockGetApiKeys = vi.fn();
const mockCreateApiKey = vi.fn();
const mockDeleteApiKey = vi.fn();

vi.mock("@/lib/api", () => ({
  getApiKeys: (...args: unknown[]) => mockGetApiKeys(...args),
  createApiKey: (...args: unknown[]) => mockCreateApiKey(...args),
  deleteApiKey: (...args: unknown[]) => mockDeleteApiKey(...args),
}));

function makeApiKey(overrides: Partial<ApiKeyDto> = {}): ApiKeyDto {
  return {
    id: "ak-1",
    name: "CI Pipeline",
    keyPrefix: "crm_a1B2...w3X4",
    createdBy: "Test User",
    createdAt: "2026-04-05T10:00:00Z",
    ...overrides,
  };
}

function makePage(keys: ApiKeyDto[], totalElements?: number): Page<ApiKeyDto> {
  return {
    content: keys,
    page: {
      size: 20,
      number: 0,
      totalElements: totalElements ?? keys.length,
      totalPages: Math.ceil((totalElements ?? keys.length) / 20),
    },
  };
}

function renderApiKeyList() {
  return render(
    <SessionProvider session={null}>
      <LanguageProvider defaultLanguage="de">
        <TooltipProvider>
          <ApiKeyList />
        </TooltipProvider>
      </LanguageProvider>
    </SessionProvider>,
  );
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("ApiKeyList", () => {
  describe("table display", () => {
    it("should render table with correct columns", async () => {
      mockGetApiKeys.mockResolvedValue(makePage([makeApiKey()]));

      renderApiKeyList();
      await waitFor(() => {
        expect(screen.getByText(K.columns.name)).toBeInTheDocument();
        expect(screen.getByText(K.columns.key)).toBeInTheDocument();
        expect(screen.getByText(K.columns.createdBy)).toBeInTheDocument();
        expect(screen.getByText(K.columns.createdAt)).toBeInTheDocument();
        expect(screen.getByText(K.columns.actions)).toBeInTheDocument();
      });
    });

    it("should display key prefix in monospace", async () => {
      mockGetApiKeys.mockResolvedValue(
        makePage([makeApiKey({ keyPrefix: "crm_xYzW...AbCd" })]),
      );

      renderApiKeyList();
      await waitFor(() => {
        expect(screen.getByText("crm_xYzW...AbCd")).toBeInTheDocument();
      });
    });

    it("should display key name and creator", async () => {
      mockGetApiKeys.mockResolvedValue(
        makePage([makeApiKey({ name: "My Key", createdBy: "Admin" })]),
      );

      renderApiKeyList();
      await waitFor(() => {
        expect(screen.getByText("My Key")).toBeInTheDocument();
        expect(screen.getByText("Admin")).toBeInTheDocument();
      });
    });
  });

  describe("empty state", () => {
    it("should show empty state when no keys", async () => {
      mockGetApiKeys.mockResolvedValue(makePage([]));

      renderApiKeyList();
      await waitFor(() => {
        expect(screen.getByText(K.empty)).toBeInTheDocument();
        expect(screen.getByText(K.createFirst)).toBeInTheDocument();
      });
    });
  });

  describe("create API key", () => {
    it("should open create dialog on button click", async () => {
      mockGetApiKeys.mockResolvedValue(makePage([makeApiKey()]));

      renderApiKeyList();
      await waitFor(() => {
        expect(screen.getByText(K.newApiKey)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(K.newApiKey));
      await waitFor(() => {
        expect(screen.getByText(K.createDialog.title)).toBeInTheDocument();
      });
    });

    it("should create key and show key dialog", async () => {
      mockGetApiKeys.mockResolvedValue(makePage([]));
      const createdKey: ApiKeyCreatedDto = {
        id: "ak-new",
        name: "New Key",
        keyPrefix: "crm_newK...ey12",
        key: "crm_newKey1234567890abcdefghijklmnopqrstuvwxyz123456",
        createdBy: "Test User",
        createdAt: "2026-04-05T10:00:00Z",
      };
      mockCreateApiKey.mockResolvedValue(createdKey);

      renderApiKeyList();
      await waitFor(() => {
        expect(screen.getByText(K.createFirst)).toBeInTheDocument();
      });

      // Open create dialog
      fireEvent.click(screen.getByText(K.createFirst));
      await waitFor(() => {
        expect(screen.getByText(K.createDialog.title)).toBeInTheDocument();
      });

      // Enter name and submit
      const input = screen.getByPlaceholderText(K.createDialog.namePlaceholder);
      fireEvent.change(input, { target: { value: "New Key" } });
      fireEvent.click(screen.getByText(K.createDialog.create));

      // Key dialog should appear
      await waitFor(() => {
        expect(screen.getByText(K.keyDialog.title)).toBeInTheDocument();
        expect(screen.getByText(K.keyDialog.warning)).toBeInTheDocument();
        expect(screen.getByText(createdKey.key)).toBeInTheDocument();
      });
    });

    it("should show validation error on empty name", async () => {
      mockGetApiKeys.mockResolvedValue(makePage([makeApiKey()]));

      renderApiKeyList();
      await waitFor(() => {
        expect(screen.getByText(K.newApiKey)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(K.newApiKey));
      await waitFor(() => {
        expect(screen.getByText(K.createDialog.title)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(K.createDialog.create));
      await waitFor(() => {
        expect(screen.getByText(K.createDialog.nameRequired)).toBeInTheDocument();
      });
    });

    it("should close key dialog and refresh list", async () => {
      mockGetApiKeys.mockResolvedValue(makePage([]));
      const createdKey: ApiKeyCreatedDto = {
        id: "ak-new",
        name: "New Key",
        keyPrefix: "crm_newK...ey12",
        key: "crm_newKey1234567890abcdefghijklmnopqrstuvwxyz123456",
        createdBy: "Test User",
        createdAt: "2026-04-05T10:00:00Z",
      };
      mockCreateApiKey.mockResolvedValue(createdKey);

      renderApiKeyList();
      await waitFor(() => {
        expect(screen.getByText(K.createFirst)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(K.createFirst));
      await waitFor(() => {
        expect(screen.getByText(K.createDialog.title)).toBeInTheDocument();
      });

      const input = screen.getByPlaceholderText(K.createDialog.namePlaceholder);
      fireEvent.change(input, { target: { value: "New Key" } });
      fireEvent.click(screen.getByText(K.createDialog.create));

      await waitFor(() => {
        expect(screen.getByText(K.keyDialog.title)).toBeInTheDocument();
      });

      // Close key dialog
      fireEvent.click(screen.getByText(K.keyDialog.close));

      await waitFor(() => {
        // List should be refetched
        expect(mockGetApiKeys).toHaveBeenCalledTimes(2); // initial + after close
      });
    });
  });

  describe("delete", () => {
    it("should open delete dialog on click", async () => {
      mockGetApiKeys.mockResolvedValue(makePage([makeApiKey()]));

      renderApiKeyList();
      await waitFor(() => {
        expect(screen.getByText("CI Pipeline")).toBeInTheDocument();
      });

      const buttons = document.querySelectorAll("button");
      const deleteButton = Array.from(buttons).find((btn) =>
        btn.querySelector(".lucide-trash-2"),
      );
      expect(deleteButton).toBeTruthy();
      fireEvent.click(deleteButton!);

      await waitFor(() => {
        expect(screen.getByText(K.deleteDialog.title)).toBeInTheDocument();
      });
    });

    it("should delete key on confirm", async () => {
      mockGetApiKeys.mockResolvedValue(makePage([makeApiKey({ id: "ak-1" })]));
      mockDeleteApiKey.mockResolvedValue(undefined);

      renderApiKeyList();
      await waitFor(() => {
        expect(screen.getByText("CI Pipeline")).toBeInTheDocument();
      });

      const buttons = document.querySelectorAll("button");
      const deleteButton = Array.from(buttons).find((btn) =>
        btn.querySelector(".lucide-trash-2"),
      );
      fireEvent.click(deleteButton!);

      await waitFor(() => {
        expect(screen.getByText(K.deleteDialog.title)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(K.deleteDialog.confirm));
      await waitFor(() => {
        expect(mockDeleteApiKey).toHaveBeenCalledWith("ak-1");
      });
    });

    it("should close dialog on cancel", async () => {
      mockGetApiKeys.mockResolvedValue(makePage([makeApiKey()]));

      renderApiKeyList();
      await waitFor(() => {
        expect(screen.getByText("CI Pipeline")).toBeInTheDocument();
      });

      const buttons = document.querySelectorAll("button");
      const deleteButton = Array.from(buttons).find((btn) =>
        btn.querySelector(".lucide-trash-2"),
      );
      fireEvent.click(deleteButton!);

      await waitFor(() => {
        expect(screen.getByText(K.deleteDialog.title)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(K.deleteDialog.cancel));
      await waitFor(() => {
        expect(mockDeleteApiKey).not.toHaveBeenCalled();
      });
    });
  });
});
