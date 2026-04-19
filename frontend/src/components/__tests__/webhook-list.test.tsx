import { describe, it, expect, afterEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor, render } from "@testing-library/react";
import { SessionProvider } from "next-auth/react";
import { LanguageProvider, TooltipProvider } from "@open-elements/ui";
import { WebhookList } from "@/components/webhook-list";
import { de } from "@/lib/i18n/de";
import { translations } from "@/lib/i18n";
import type { WebhookDto, Page } from "@/lib/types";

function renderWebhookList() {
  return render(
    <SessionProvider session={null}>
      <LanguageProvider translations={translations} defaultLanguage="de">
        <TooltipProvider>
          <WebhookList />
        </TooltipProvider>
      </LanguageProvider>
    </SessionProvider>,
  );
}

const W = de.webhooks;

// Mock localStorage for pageSize
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
  usePathname: () => "/webhooks",
  useSearchParams: () => new URLSearchParams(),
}));

const mockGetWebhooks = vi.fn();
const mockCreateWebhook = vi.fn();
const mockUpdateWebhook = vi.fn();
const mockDeleteWebhook = vi.fn();
const mockPingWebhook = vi.fn();

vi.mock("@/lib/api", () => ({
  getWebhooks: (...args: unknown[]) => mockGetWebhooks(...args),
  createWebhook: (...args: unknown[]) => mockCreateWebhook(...args),
  updateWebhook: (...args: unknown[]) => mockUpdateWebhook(...args),
  deleteWebhook: (...args: unknown[]) => mockDeleteWebhook(...args),
  pingWebhook: (...args: unknown[]) => mockPingWebhook(...args),
}));

function makeWebhook(overrides: Partial<WebhookDto> = {}): WebhookDto {
  return {
    id: "wh-1",
    url: "https://example.com/hook",
    active: true,
    lastStatus: null,
    lastCalledAt: null,
    createdAt: "2026-04-04T10:00:00Z",
    updatedAt: "2026-04-04T10:00:00Z",
    ...overrides,
  };
}

function makePage(
  webhooks: WebhookDto[],
  totalElements?: number,
): Page<WebhookDto> {
  return {
    content: webhooks,
    page: {
      size: 20,
      number: 0,
      totalElements: totalElements ?? webhooks.length,
      totalPages: Math.ceil((totalElements ?? webhooks.length) / 20),
    },
  };
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("WebhookList", () => {
  describe("table display", () => {
    it("should render table with correct columns", async () => {
      mockGetWebhooks.mockResolvedValue(makePage([makeWebhook()]));

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText(W.columns.url)).toBeInTheDocument();
        expect(screen.getByText(W.columns.active)).toBeInTheDocument();
        expect(screen.getByText(W.columns.lastStatus)).toBeInTheDocument();
        expect(screen.getByText(W.columns.lastCalledAt)).toBeInTheDocument();
        expect(screen.getByText(W.columns.actions)).toBeInTheDocument();
      });
    });

    it("should display webhook URL", async () => {
      mockGetWebhooks.mockResolvedValue(
        makePage([makeWebhook({ url: "https://receiver.test/hook" })]),
      );

      renderWebhookList();
      await waitFor(() => {
        expect(
          screen.getByText("https://receiver.test/hook"),
        ).toBeInTheDocument();
      });
    });

    it("should display OK for status 200", async () => {
      mockGetWebhooks.mockResolvedValue(
        makePage([makeWebhook({ lastStatus: 200 })]),
      );

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText(W.status.ok)).toBeInTheDocument();
      });
    });

    it("should display Timeout for status -1", async () => {
      mockGetWebhooks.mockResolvedValue(
        makePage([makeWebhook({ lastStatus: -1 })]),
      );

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText(W.status.timeout)).toBeInTheDocument();
      });
    });

    it("should display Connection Error for status 0", async () => {
      mockGetWebhooks.mockResolvedValue(
        makePage([makeWebhook({ lastStatus: 0 })]),
      );

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText(W.status.connectionError)).toBeInTheDocument();
      });
    });

    it("should display Bad Call with code for 4xx", async () => {
      mockGetWebhooks.mockResolvedValue(
        makePage([makeWebhook({ lastStatus: 404 })]),
      );

      renderWebhookList();
      await waitFor(() => {
        expect(
          screen.getByText(`${W.status.badCall} (404)`),
        ).toBeInTheDocument();
      });
    });

    it("should display dash for never called", async () => {
      mockGetWebhooks.mockResolvedValue(
        makePage([makeWebhook({ lastStatus: null })]),
      );

      renderWebhookList();
      await waitFor(() => {
        const cells = screen.getAllByText("—");
        expect(cells.length).toBeGreaterThan(0);
      });
    });
  });

  describe("empty state", () => {
    it("should show empty state when no webhooks", async () => {
      mockGetWebhooks.mockResolvedValue(makePage([]));

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText(W.empty)).toBeInTheDocument();
        expect(screen.getByText(W.createFirst)).toBeInTheDocument();
      });
    });
  });

  describe("loading state", () => {
    it("should show skeletons during loading", () => {
      mockGetWebhooks.mockReturnValue(new Promise(() => {})); // Never resolves

      renderWebhookList();
      const skeletons = document.querySelectorAll("[data-slot='skeleton']");
      expect(skeletons.length).toBeGreaterThan(0);
    });
  });

  describe("create webhook", () => {
    it("should open create dialog on button click", async () => {
      mockGetWebhooks.mockResolvedValue(makePage([makeWebhook()]));

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText(W.newWebhook)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(W.newWebhook));

      await waitFor(() => {
        expect(screen.getByText(W.createDialog.title)).toBeInTheDocument();
      });
    });

    it("should create webhook with valid URL", async () => {
      mockGetWebhooks.mockResolvedValue(makePage([]));
      mockCreateWebhook.mockResolvedValue(
        makeWebhook({ url: "https://new.test/hook" }),
      );

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText(W.createFirst)).toBeInTheDocument();
      });

      // Open dialog from empty state button
      fireEvent.click(screen.getByText(W.createFirst));
      await waitFor(() => {
        expect(screen.getByText(W.createDialog.title)).toBeInTheDocument();
      });

      // Enter URL and submit
      const input = screen.getByPlaceholderText(W.createDialog.urlPlaceholder);
      fireEvent.change(input, {
        target: { value: "https://new.test/hook" },
      });
      fireEvent.click(screen.getByText(W.createDialog.create));

      await waitFor(() => {
        expect(mockCreateWebhook).toHaveBeenCalledWith({
          url: "https://new.test/hook",
        });
      });
    });

    it("should show validation error on empty URL", async () => {
      mockGetWebhooks.mockResolvedValue(makePage([makeWebhook()]));

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText(W.newWebhook)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(W.newWebhook));
      await waitFor(() => {
        expect(screen.getByText(W.createDialog.title)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(W.createDialog.create));

      await waitFor(() => {
        expect(
          screen.getByText(W.createDialog.urlRequired),
        ).toBeInTheDocument();
      });
    });

    it("should show API error in dialog", async () => {
      mockGetWebhooks.mockResolvedValue(makePage([makeWebhook()]));
      mockCreateWebhook.mockRejectedValue(new Error("Server error"));

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText(W.newWebhook)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(W.newWebhook));
      await waitFor(() => {
        expect(screen.getByText(W.createDialog.title)).toBeInTheDocument();
      });

      const input = screen.getByPlaceholderText(W.createDialog.urlPlaceholder);
      fireEvent.change(input, {
        target: { value: "https://example.com/hook" },
      });
      fireEvent.click(screen.getByText(W.createDialog.create));

      await waitFor(() => {
        expect(screen.getByText("Server error")).toBeInTheDocument();
      });
    });

    it("should close dialog on cancel", async () => {
      mockGetWebhooks.mockResolvedValue(makePage([makeWebhook()]));

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText(W.newWebhook)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(W.newWebhook));
      await waitFor(() => {
        expect(screen.getByText(W.createDialog.title)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(W.createDialog.cancel));

      await waitFor(() => {
        expect(
          screen.queryByText(W.createDialog.title),
        ).not.toBeInTheDocument();
      });
    });
  });

  describe("toggle active", () => {
    it("should call updateWebhook to deactivate", async () => {
      mockGetWebhooks.mockResolvedValue(
        makePage([makeWebhook({ id: "wh-1", active: true })]),
      );
      mockUpdateWebhook.mockResolvedValue(
        makeWebhook({ id: "wh-1", active: false }),
      );

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText("OFF")).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText("OFF"));

      await waitFor(() => {
        expect(mockUpdateWebhook).toHaveBeenCalledWith("wh-1", {
          url: "https://example.com/hook",
          active: false,
        });
      });
    });

    it("should call updateWebhook to activate", async () => {
      mockGetWebhooks.mockResolvedValue(
        makePage([makeWebhook({ id: "wh-1", active: false })]),
      );
      mockUpdateWebhook.mockResolvedValue(
        makeWebhook({ id: "wh-1", active: true }),
      );

      renderWebhookList();
      await waitFor(() => {
        expect(screen.getByText("ON")).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText("ON"));

      await waitFor(() => {
        expect(mockUpdateWebhook).toHaveBeenCalledWith("wh-1", {
          url: "https://example.com/hook",
          active: true,
        });
      });
    });
  });

  describe("ping", () => {
    it("should call pingWebhook on click", async () => {
      mockGetWebhooks.mockResolvedValue(
        makePage([makeWebhook({ id: "wh-1" })]),
      );
      mockPingWebhook.mockResolvedValue(undefined);

      renderWebhookList();
      await waitFor(() => {
        expect(
          screen.getByText("https://example.com/hook"),
        ).toBeInTheDocument();
      });

      // Find the ping button (Radio icon button)
      const buttons = document.querySelectorAll("button");
      const pingButton = Array.from(buttons).find((btn) =>
        btn.querySelector(".lucide-radio"),
      );
      expect(pingButton).toBeTruthy();
      fireEvent.click(pingButton!);

      await waitFor(() => {
        expect(mockPingWebhook).toHaveBeenCalledWith("wh-1");
      });
    });

    it("should call pingWebhook for inactive webhook", async () => {
      mockGetWebhooks.mockResolvedValue(
        makePage([makeWebhook({ id: "wh-2", active: false })]),
      );
      mockPingWebhook.mockResolvedValue(undefined);

      renderWebhookList();
      await waitFor(() => {
        expect(
          screen.getByText("https://example.com/hook"),
        ).toBeInTheDocument();
      });

      const buttons = document.querySelectorAll("button");
      const pingButton = Array.from(buttons).find((btn) =>
        btn.querySelector(".lucide-radio"),
      );
      expect(pingButton).toBeTruthy();
      fireEvent.click(pingButton!);

      await waitFor(() => {
        expect(mockPingWebhook).toHaveBeenCalledWith("wh-2");
      });
    });
  });

  describe("delete", () => {
    it("should open delete dialog on click", async () => {
      mockGetWebhooks.mockResolvedValue(makePage([makeWebhook()]));

      renderWebhookList();
      await waitFor(() => {
        expect(
          screen.getByText("https://example.com/hook"),
        ).toBeInTheDocument();
      });

      // Find delete button (Trash2 icon button)
      const buttons = document.querySelectorAll("button");
      const deleteButton = Array.from(buttons).find((btn) =>
        btn.querySelector(".lucide-trash-2"),
      );
      expect(deleteButton).toBeTruthy();
      fireEvent.click(deleteButton!);

      await waitFor(() => {
        expect(screen.getByText(W.deleteDialog.title)).toBeInTheDocument();
        expect(
          screen.getByText(W.deleteDialog.description),
        ).toBeInTheDocument();
      });
    });

    it("should delete webhook on confirm", async () => {
      mockGetWebhooks.mockResolvedValue(
        makePage([makeWebhook({ id: "wh-1" })]),
      );
      mockDeleteWebhook.mockResolvedValue(undefined);

      renderWebhookList();
      await waitFor(() => {
        expect(
          screen.getByText("https://example.com/hook"),
        ).toBeInTheDocument();
      });

      const buttons = document.querySelectorAll("button");
      const deleteButton = Array.from(buttons).find((btn) =>
        btn.querySelector(".lucide-trash-2"),
      );
      fireEvent.click(deleteButton!);

      await waitFor(() => {
        expect(screen.getByText(W.deleteDialog.title)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(W.deleteDialog.confirm));

      await waitFor(() => {
        expect(mockDeleteWebhook).toHaveBeenCalledWith("wh-1");
      });
    });

    it("should close dialog on cancel", async () => {
      mockGetWebhooks.mockResolvedValue(makePage([makeWebhook()]));

      renderWebhookList();
      await waitFor(() => {
        expect(
          screen.getByText("https://example.com/hook"),
        ).toBeInTheDocument();
      });

      const buttons = document.querySelectorAll("button");
      const deleteButton = Array.from(buttons).find((btn) =>
        btn.querySelector(".lucide-trash-2"),
      );
      fireEvent.click(deleteButton!);

      await waitFor(() => {
        expect(screen.getByText(W.deleteDialog.title)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(W.deleteDialog.cancel));

      await waitFor(() => {
        expect(mockDeleteWebhook).not.toHaveBeenCalled();
      });
    });
  });

  describe("pagination", () => {
    it("should show pagination controls with multiple pages", async () => {
      const webhooks = Array.from({ length: 20 }, (_, i) =>
        makeWebhook({ id: `wh-${i}`, url: `https://example.com/hook${i}` }),
      );
      mockGetWebhooks.mockResolvedValue(makePage(webhooks, 25));

      renderWebhookList();
      await waitFor(() => {
        expect(
          screen.getByText(W.pagination.next),
        ).toBeInTheDocument();
      });
    });
  });
});
