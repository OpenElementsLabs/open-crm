import { describe, it, expect, afterEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { BrevoSync } from "@/components/brevo-sync";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";

const S = de.brevo;

const mockGetBrevoSettings = vi.fn();
const mockUpdateBrevoSettings = vi.fn();
const mockDeleteBrevoSettings = vi.fn();
const mockStartBrevoSync = vi.fn();

vi.mock("@/lib/api", () => ({
  getBrevoSettings: (...args: unknown[]) => mockGetBrevoSettings(...args),
  updateBrevoSettings: (...args: unknown[]) => mockUpdateBrevoSettings(...args),
  deleteBrevoSettings: (...args: unknown[]) => mockDeleteBrevoSettings(...args),
  startBrevoSync: (...args: unknown[]) => mockStartBrevoSync(...args),
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  vi.restoreAllMocks();
});

describe("BrevoSync", () => {
  describe("Settings Card", () => {
    it("should show unconfigured state when no API key", async () => {
      mockGetBrevoSettings.mockResolvedValue({ apiKeyConfigured: false });

      renderWithProviders(<BrevoSync />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.settings.apiKeyPlaceholder)).toBeInTheDocument();
        expect(screen.getByText(S.settings.save)).toBeInTheDocument();
      });
    });

    it("should show configured state when API key exists", async () => {
      mockGetBrevoSettings.mockResolvedValue({ apiKeyConfigured: true });

      renderWithProviders(<BrevoSync />);

      await waitFor(() => {
        expect(screen.getByText(S.settings.configured)).toBeInTheDocument();
        expect(screen.getByText(S.settings.change)).toBeInTheDocument();
        expect(screen.getByText(S.settings.remove)).toBeInTheDocument();
      });

      expect(screen.queryByPlaceholderText(S.settings.apiKeyPlaceholder)).not.toBeInTheDocument();
    });

    it("should save API key successfully", async () => {
      mockGetBrevoSettings.mockResolvedValue({ apiKeyConfigured: false });
      mockUpdateBrevoSettings.mockResolvedValue({ apiKeyConfigured: true });

      renderWithProviders(<BrevoSync />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.settings.apiKeyPlaceholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.settings.apiKeyPlaceholder), {
        target: { value: "xkeysib-test-key" },
      });

      fireEvent.click(screen.getByText(S.settings.save));

      await waitFor(() => {
        expect(mockUpdateBrevoSettings).toHaveBeenCalledWith("xkeysib-test-key");
      });
    });

    it("should show error for invalid API key", async () => {
      mockGetBrevoSettings.mockResolvedValue({ apiKeyConfigured: false });
      mockUpdateBrevoSettings.mockRejectedValue(new Error("Invalid API key"));

      renderWithProviders(<BrevoSync />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.settings.apiKeyPlaceholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.settings.apiKeyPlaceholder), {
        target: { value: "bad-key" },
      });

      fireEvent.click(screen.getByText(S.settings.save));

      await waitFor(() => {
        expect(screen.getByText(S.settings.errorInvalid)).toBeInTheDocument();
      });
    });

    it("should remove API key", async () => {
      mockGetBrevoSettings.mockResolvedValue({ apiKeyConfigured: true });
      mockDeleteBrevoSettings.mockResolvedValue(undefined);
      vi.spyOn(window, "confirm").mockReturnValue(true);

      renderWithProviders(<BrevoSync />);

      await waitFor(() => {
        expect(screen.getByText(S.settings.remove)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.settings.remove));

      await waitFor(() => {
        expect(mockDeleteBrevoSettings).toHaveBeenCalled();
      });
    });
  });

  describe("Sync Card", () => {
    it("should disable Start Import when no API key", async () => {
      mockGetBrevoSettings.mockResolvedValue({ apiKeyConfigured: false });

      renderWithProviders(<BrevoSync />);

      await waitFor(() => {
        expect(screen.getByText(S.sync.configureFirst)).toBeInTheDocument();
      });

      expect(screen.queryByText(S.sync.start)).not.toBeInTheDocument();
    });

    it("should enable Start Import when API key configured", async () => {
      mockGetBrevoSettings.mockResolvedValue({ apiKeyConfigured: true });

      renderWithProviders(<BrevoSync />);

      await waitFor(() => {
        const startButton = screen.getByText(S.sync.start);
        expect(startButton).toBeInTheDocument();
        expect(startButton.closest("button")).not.toBeDisabled();
      });
    });

    it("should show loading state during sync", async () => {
      mockGetBrevoSettings.mockResolvedValue({ apiKeyConfigured: true });
      mockStartBrevoSync.mockReturnValue(new Promise(() => {}));

      renderWithProviders(<BrevoSync />);

      await waitFor(() => {
        expect(screen.getByText(S.sync.start)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.sync.start));

      await waitFor(() => {
        expect(screen.getByText(S.sync.running)).toBeInTheDocument();
      });
    });

    it("should display sync results", async () => {
      mockGetBrevoSettings.mockResolvedValue({ apiKeyConfigured: true });
      mockStartBrevoSync.mockResolvedValue({
        companiesImported: 5,
        companiesUpdated: 3,
        companiesFailed: 0,
        contactsImported: 20,
        contactsUpdated: 10,
        contactsFailed: 2,
        errors: ["Error 1"],
      });

      renderWithProviders(<BrevoSync />);

      await waitFor(() => {
        expect(screen.getByText(S.sync.start)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.sync.start));

      await waitFor(() => {
        expect(screen.getByText("5")).toBeInTheDocument();
        expect(screen.getByText("3")).toBeInTheDocument();
        expect(screen.getByText("20")).toBeInTheDocument();
        expect(screen.getByText("10")).toBeInTheDocument();
        expect(screen.getByText("2")).toBeInTheDocument();
      });

      expect(screen.getByText(`${S.sync.errors} (1)`)).toBeInTheDocument();

      fireEvent.click(screen.getByText(`${S.sync.errors} (1)`));

      await waitFor(() => {
        expect(screen.getByText("Error 1")).toBeInTheDocument();
      });
    });
  });
});
