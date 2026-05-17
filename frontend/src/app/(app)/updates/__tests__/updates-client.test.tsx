import { describe, it, expect, afterEach, beforeEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor, render, within } from "@testing-library/react";
import { LanguageProvider, TooltipProvider } from "@open-elements/ui";
import {
  UpdatesClient,
  PAGE_SIZE_OPTIONS,
  DEFAULT_PAGE_SIZE,
  PAGE_SIZE_STORAGE_KEY,
} from "../updates-client";
import { de } from "@/lib/i18n/de";
import { en } from "@/lib/i18n/en";
import { translations } from "@/lib/i18n";
import type { Page, UpdateEntryDto, UserDto } from "@/lib/types";

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
  usePathname: () => "/updates",
  useSearchParams: () => new URLSearchParams(),
}));

const mockGetUpdates = vi.fn();

vi.mock("@/lib/api", () => ({
  getUpdates: (...args: unknown[]) => mockGetUpdates(...args),
  getCompanyLogoUrl: (id: string) => `/api/companies/${id}/logo`,
  getContactPhotoUrl: (id: string) => `/api/contacts/${id}/photo`,
}));

function makeUser(overrides: Partial<UserDto> = {}): UserDto {
  return {
    id: "u-1",
    name: "Alice",
    email: "alice@example.com",
    avatarUrl: null,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

function makeEntry(overrides: Partial<UpdateEntryDto> = {}): UpdateEntryDto {
  return {
    id: "log-1",
    type: "COMPANY_CREATED",
    entityId: "550e8400-e29b-41d4-a716-446655440000",
    entityName: "Open Elements GmbH",
    entityHasLogo: false,
    entityHasPhoto: false,
    user: makeUser(),
    createdAt: "2026-04-25T14:30:00Z",
    ...overrides,
  };
}

function makePage<T>(items: T[], size = 20): Page<T> {
  return {
    content: items,
    page: { size, number: 0, totalElements: items.length, totalPages: 1 },
  };
}

function renderClient(lang: "de" | "en" = "de") {
  return render(
    <LanguageProvider translations={translations} defaultLanguage={lang}>
      <TooltipProvider>
        <UpdatesClient />
      </TooltipProvider>
    </LanguageProvider>,
  );
}

beforeEach(() => {
  localStorageMock.clear();
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("UpdatesClient", () => {
  describe("constants", () => {
    it("exposes page-size options 20/50/100/200", () => {
      expect(Array.from(PAGE_SIZE_OPTIONS)).toEqual([20, 50, 100, 200]);
    });

    it("defaults to 20 items per page", () => {
      expect(DEFAULT_PAGE_SIZE).toBe(20);
    });

    it("persists user-selected page size under updates.pageSize", () => {
      expect(PAGE_SIZE_STORAGE_KEY).toBe("updates.pageSize");
    });
  });

  describe("loading / empty / error", () => {
    it("renders a skeleton while loading", () => {
      mockGetUpdates.mockReturnValue(new Promise(() => {}));
      renderClient();
      expect(screen.getByTestId("updates-loading")).toBeInTheDocument();
    });

    it("renders the empty state when content is empty", async () => {
      mockGetUpdates.mockResolvedValue(makePage<UpdateEntryDto>([]));
      renderClient("de");
      await waitFor(() => {
        expect(screen.getByTestId("updates-empty")).toBeInTheDocument();
        expect(screen.getByText(de.updates.empty)).toBeInTheDocument();
      });
    });

    it("renders the empty state with English copy when language is en", async () => {
      mockGetUpdates.mockResolvedValue(makePage<UpdateEntryDto>([]));
      renderClient("en");
      await waitFor(() => {
        expect(screen.getByText(en.updates.empty)).toBeInTheDocument();
      });
    });

    it("renders the error state when the fetch rejects", async () => {
      const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
      mockGetUpdates.mockRejectedValue(new Error("boom"));
      renderClient();
      await waitFor(() => {
        expect(screen.getByTestId("updates-error")).toBeInTheDocument();
        expect(screen.getByText(de.updates.loadError)).toBeInTheDocument();
      });
      expect(screen.queryByTestId("updates-empty")).toBeNull();
      consoleError.mockRestore();
    });
  });

  describe("rendering rows with links", () => {
    it("renders COMPANY_UPDATED as a link to /companies/{id}", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_UPDATED",
            entityId: "c-1",
            entityName: "Acme",
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const link = within(row).getByRole("link", { name: "Acme" });
      expect(link).toHaveAttribute("href", "/companies/c-1");
    });

    it("renders CONTACT_CREATED as a link to /contacts/{id}", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "CONTACT_CREATED",
            entityId: "p-1",
            entityName: "John Doe",
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const link = within(row).getByRole("link", { name: "John Doe" });
      expect(link).toHaveAttribute("href", "/contacts/p-1");
    });

    it("renders COMPANY_COMMENT_CREATED with link to parent company", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_COMMENT_CREATED",
            entityId: "c-9",
            entityName: "Open Elements GmbH",
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const link = within(row).getByRole("link", { name: "Open Elements GmbH" });
      expect(link).toHaveAttribute("href", "/companies/c-9");
    });

    it("renders CONTACT_COMMENT_CREATED with link to parent contact", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "CONTACT_COMMENT_CREATED",
            entityId: "p-9",
            entityName: "Jane Doe",
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const link = within(row).getByRole("link", { name: "Jane Doe" });
      expect(link).toHaveAttribute("href", "/contacts/p-9");
    });

    it("renders COMPANY_DELETED as plain text without a link or name", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({ type: "COMPANY_DELETED", entityId: null, entityName: null }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).queryByRole("link")).toBeNull();
      expect(within(row).getByText(/A company was deleted/)).toBeInTheDocument();
    });

    it("renders CONTACT_DELETED as plain text without a link or name", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({ type: "CONTACT_DELETED", entityId: null, entityName: null }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).queryByRole("link")).toBeNull();
      expect(within(row).getByText(/A person was deleted/)).toBeInTheDocument();
    });

    it("still renders a link with a placeholder name when entityName is null but entityId is present", async () => {
      // Per design.md: `entityId` is preserved for diagnosis even when the entity name cannot be
      // resolved (race / unexpected state). The link is kept; the visible label falls back to a dash.
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_UPDATED",
            entityId: "c-1",
            entityName: null,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const link = within(row).getByRole("link");
      expect(link).toHaveAttribute("href", "/companies/c-1");
    });

    it("renders the actor name", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({ user: makeUser({ id: "u-2", name: "Bob" }) }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).getByText(/by Bob/)).toBeInTheDocument();
    });
  });

  describe("page-size selector", () => {
    it("requests size=20 on first load when localStorage is empty", async () => {
      mockGetUpdates.mockResolvedValue(makePage([makeEntry()]));
      renderClient();
      await waitFor(() => {
        expect(mockGetUpdates).toHaveBeenCalledWith({ size: 20 });
      });
    });

    it("reads page size from localStorage on mount", async () => {
      localStorageMock.setItem("updates.pageSize", "100");
      mockGetUpdates.mockResolvedValue(makePage([makeEntry()], 100));
      renderClient();
      await waitFor(() => {
        expect(mockGetUpdates).toHaveBeenCalledWith({ size: 100 });
      });
    });

    it("ignores out-of-range stored values and falls back to default", async () => {
      localStorageMock.setItem("updates.pageSize", "999");
      mockGetUpdates.mockResolvedValue(makePage([makeEntry()]));
      renderClient();
      await waitFor(() => {
        expect(mockGetUpdates).toHaveBeenCalledWith({ size: 20 });
      });
    });
  });

  describe("auto-refresh", () => {
    it("does not refetch after the initial load when no input changes", async () => {
      mockGetUpdates.mockResolvedValue(makePage([makeEntry()]));
      renderClient();
      await waitFor(() => {
        expect(mockGetUpdates).toHaveBeenCalledTimes(1);
      });
      // wait a tick — no polling
      await new Promise((r) => setTimeout(r, 30));
      expect(mockGetUpdates).toHaveBeenCalledTimes(1);
    });
  });

  describe("i18n", () => {
    it("renders German company-created text by default", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({ type: "COMPANY_CREATED", entityName: "Acme" }),
        ]),
      );
      renderClient("de");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).getByText(/Neue Firma/)).toBeInTheDocument();
      expect(within(row).getByText(/wurde angelegt/)).toBeInTheDocument();
    });

    it("renders English company-created text when language is en", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({ type: "COMPANY_CREATED", entityName: "Acme" }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).getByText(/New company/)).toBeInTheDocument();
      expect(within(row).getByText(/was created/)).toBeInTheDocument();
    });
  });

  describe("leading entity image slot", () => {
    it("renders the company logo image wrapped in an aria-hidden link when entityHasLogo is true", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_UPDATED",
            entityId: "c-1",
            entityName: "Acme",
            entityHasLogo: true,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const logo = within(row).getByTestId("updates-row-company-logo");
      expect(logo).toHaveAttribute("src", "/api/companies/c-1/logo");
      expect(logo).toHaveAttribute("alt", "");
      expect(logo).toHaveClass("h-8", "w-8");
      const wrapper = logo.closest("a");
      expect(wrapper).not.toBeNull();
      expect(wrapper).toHaveAttribute("href", "/companies/c-1");
      expect(wrapper).toHaveAttribute("aria-hidden", "true");
      expect(wrapper).toHaveAttribute("tabIndex", "-1");
    });

    it("renders the Building2 placeholder when a company event has no logo", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_UPDATED",
            entityId: "c-1",
            entityName: "Acme",
            entityHasLogo: false,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const placeholder = within(row).getByTestId("updates-row-company-placeholder");
      expect(placeholder.tagName.toLowerCase()).toBe("svg");
      expect(placeholder.closest("a")).toBeNull();
    });

    it("renders the contact photo wrapped in an aria-hidden link when entityHasPhoto is true", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "CONTACT_UPDATED",
            entityId: "p-1",
            entityName: "John Doe",
            entityHasPhoto: true,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const photo = within(row).getByTestId("updates-row-contact-photo");
      expect(photo).toHaveAttribute("src", "/api/contacts/p-1/photo");
      expect(photo).toHaveClass("rounded-full");
      const wrapper = photo.closest("a");
      expect(wrapper).toHaveAttribute("href", "/contacts/p-1");
      expect(wrapper).toHaveAttribute("aria-hidden", "true");
      expect(wrapper).toHaveAttribute("tabIndex", "-1");
    });

    it("renders the UserIcon placeholder when a contact event has no photo", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "CONTACT_UPDATED",
            entityId: "p-1",
            entityName: "John Doe",
            entityHasPhoto: false,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const placeholder = within(row).getByTestId("updates-row-contact-placeholder");
      expect(placeholder.tagName.toLowerCase()).toBe("svg");
      expect(placeholder.closest("a")).toBeNull();
    });

    it("uses the parent company logo for a COMPANY_COMMENT_CREATED row", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_COMMENT_CREATED",
            entityId: "c-9",
            entityName: "Open Elements GmbH",
            entityHasLogo: true,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const logo = within(row).getByTestId("updates-row-company-logo");
      expect(logo).toHaveAttribute("src", "/api/companies/c-9/logo");
      expect(logo.closest("a")).toHaveAttribute("href", "/companies/c-9");
    });

    it("uses the parent contact photo for a CONTACT_COMMENT_CREATED row", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "CONTACT_COMMENT_CREATED",
            entityId: "p-9",
            entityName: "Jane Doe",
            entityHasPhoto: true,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const photo = within(row).getByTestId("updates-row-contact-photo");
      expect(photo).toHaveAttribute("src", "/api/contacts/p-9/photo");
      expect(photo.closest("a")).toHaveAttribute("href", "/contacts/p-9");
    });

    it("renders a Trash2 icon for COMPANY_DELETED with no link", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_DELETED",
            entityId: null,
            entityName: null,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const trash = within(row).getByTestId("updates-row-deleted-icon");
      expect(trash.tagName.toLowerCase()).toBe("svg");
      expect(trash).toHaveAttribute("aria-hidden", "true");
      expect(trash.closest("a")).toBeNull();
    });

    it("renders a Trash2 icon for CONTACT_DELETED with no link", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "CONTACT_DELETED",
            entityId: null,
            entityName: null,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const trash = within(row).getByTestId("updates-row-deleted-icon");
      expect(trash).toBeInTheDocument();
      expect(trash.closest("a")).toBeNull();
    });

    it("keeps the parent company logo for COMPANY_COMMENT_DELETED (not a Trash2 icon)", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_COMMENT_DELETED",
            entityId: "c-9",
            entityName: "Open Elements GmbH",
            entityHasLogo: true,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).queryByTestId("updates-row-deleted-icon")).toBeNull();
      const logo = within(row).getByTestId("updates-row-company-logo");
      expect(logo.closest("a")).toHaveAttribute("href", "/companies/c-9");
    });
  });

  describe("entity name styling", () => {
    it("renders the entity-name link as font-bold text-oe-blue when linkable", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_UPDATED",
            entityId: "c-1",
            entityName: "Acme",
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const link = within(row).getByRole("link", { name: "Acme" });
      expect(link).toHaveClass("font-bold", "text-oe-blue");
    });

    it("renders the fallback dash as bold non-link when the entity cannot be resolved", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_UPDATED",
            entityId: null,
            entityName: null,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).queryByRole("link")).toBeNull();
      const dash = within(row).getByText("—");
      expect(dash.tagName.toLowerCase()).toBe("span");
      expect(dash).toHaveClass("font-bold");
    });
  });

  describe("author avatar slot", () => {
    it("renders an <img> with the avatarUrl when set", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            user: makeUser({ avatarUrl: "https://example.com/avatar.png" }),
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const avatar = within(row).getByTestId("updates-author-avatar");
      expect(avatar).toHaveAttribute("src", "https://example.com/avatar.png");
      expect(avatar).toHaveAttribute("alt", "");
      expect(avatar).toHaveClass("h-5", "w-5", "rounded-full", "object-cover");
      expect(within(row).queryByTestId("updates-author-avatar-fallback")).toBeNull();
    });

    it("renders a fallback circle with UserIcon when avatarUrl is null", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({ user: makeUser({ avatarUrl: null }) }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const fallback = within(row).getByTestId("updates-author-avatar-fallback");
      expect(fallback).toHaveClass("h-5", "w-5", "rounded-full", "bg-oe-gray-lightest");
      expect(fallback).toHaveAttribute("aria-hidden", "true");
      expect(within(row).queryByTestId("updates-author-avatar")).toBeNull();
    });
  });

  describe("page-size label", () => {
    it("uses updates.perPage as the combobox label (de)", async () => {
      mockGetUpdates.mockResolvedValue(makePage([makeEntry()]));
      renderClient("de");
      await screen.findByTestId("updates-row");
      expect(screen.getByText(de.updates.perPage)).toBeInTheDocument();
      expect(screen.getByTestId("updates-page-size")).toHaveAttribute("aria-label", de.updates.perPage);
    });

    it("uses updates.perPage as the combobox label (en)", async () => {
      mockGetUpdates.mockResolvedValue(makePage([makeEntry()]));
      renderClient("en");
      await screen.findByTestId("updates-row");
      expect(screen.getByText(en.updates.perPage)).toBeInTheDocument();
    });
  });
});
