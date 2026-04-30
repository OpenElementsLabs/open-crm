import { describe, it, expect, afterEach, beforeEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { TranslateButton } from "@/components/translate-button";
import { de } from "@/lib/i18n/de";
import { en } from "@/lib/i18n/en";
import { __resetTranslationConfigCache } from "@/lib/use-translation-config";
import { renderWithProviders } from "@/test/test-utils";

const mockGetTranslationSettings = vi.fn();
const mockTranslateText = vi.fn();

vi.mock("@/lib/api", () => ({
  getTranslationSettings: (...args: unknown[]) => mockGetTranslationSettings(...args),
  translateText: (...args: unknown[]) => mockTranslateText(...args),
}));

beforeEach(() => {
  __resetTranslationConfigCache();
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("TranslateButton", () => {
  it("renders nothing when text is empty", async () => {
    mockGetTranslationSettings.mockResolvedValue({ configured: true });
    const { container } = renderWithProviders(<TranslateButton text="" />);
    await waitFor(() => {
      expect(container.firstChild).toBeNull();
    });
  });

  it("renders nothing when text is whitespace only", async () => {
    mockGetTranslationSettings.mockResolvedValue({ configured: true });
    const { container } = renderWithProviders(<TranslateButton text="   " />);
    await waitFor(() => {
      expect(container.firstChild).toBeNull();
    });
  });

  it("renders nothing when translation is not configured", async () => {
    mockGetTranslationSettings.mockResolvedValue({ configured: false });
    const { container } = renderWithProviders(<TranslateButton text="Hallo" />);
    await waitFor(() => {
      expect(container.firstChild).toBeNull();
    });
  });

  it("renders translate button when configured and text is present", async () => {
    mockGetTranslationSettings.mockResolvedValue({ configured: true });
    renderWithProviders(<TranslateButton text="Hallo Welt" />);
    await waitFor(() => {
      expect(screen.getByLabelText(de.translation.translate)).toBeInTheDocument();
    });
  });

  it("opens the dialog and calls the translation API with the current UI language", async () => {
    mockGetTranslationSettings.mockResolvedValue({ configured: true });
    mockTranslateText.mockResolvedValue({ translatedText: "Hello World" });

    renderWithProviders(<TranslateButton text="Hallo Welt" />, { language: "en" });

    const button = await waitFor(() =>
      screen.getByLabelText(en.translation.translate),
    );
    fireEvent.click(button);

    await waitFor(() => {
      expect(mockTranslateText).toHaveBeenCalledWith("Hallo Welt", "en");
      expect(screen.getByText("Hello World")).toBeInTheDocument();
    });
  });

  it("shows an error message when the translation API fails", async () => {
    mockGetTranslationSettings.mockResolvedValue({ configured: true });
    mockTranslateText.mockRejectedValue(new Error("Server error"));

    renderWithProviders(<TranslateButton text="Hallo" />);

    const button = await waitFor(() =>
      screen.getByLabelText(de.translation.translate),
    );
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText(de.translation.error)).toBeInTheDocument();
    });
  });
});
