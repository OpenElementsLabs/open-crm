import { describe, it, expect, afterEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor, render } from "@testing-library/react";
import { renderWithProviders } from "@/test/test-utils";
import { useLanguage, LanguageProvider } from "@open-elements/ui";
import { de } from "@/lib/i18n/de";
import { en } from "@/lib/i18n/en";
import { translations, useTranslations } from "@/lib/i18n";

function TranslationDisplay() {
  const t = useTranslations();
  return <span data-testid="nav-companies">{t.nav.companies}</span>;
}

function LanguageSwitchTest() {
  const { language, setLanguage } = useLanguage();
  const t = useTranslations();
  return (
    <div>
      <span data-testid="lang">{language}</span>
      <span data-testid="text">{t.nav.companies}</span>
      <button data-testid="switch-de" onClick={() => setLanguage("de")}>DE</button>
      <button data-testid="switch-en" onClick={() => setLanguage("en")}>EN</button>
    </div>
  );
}

afterEach(() => {
  cleanup();
  try {
    localStorage.removeItem("language");
  } catch {
    // localStorage may not be available
  }
  vi.restoreAllMocks();
});

describe("Language Detection", () => {
  it("should detect German browser language", async () => {
    vi.spyOn(navigator, "language", "get").mockReturnValue("de-DE");

    render(
      <LanguageProvider translations={translations}>
        <TranslationDisplay />
      </LanguageProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("nav-companies")).toHaveTextContent(de.nav.companies);
    });
    expect(document.documentElement.lang).toBe("de");
  });

  it("should detect English browser language", async () => {
    vi.spyOn(navigator, "language", "get").mockReturnValue("en-US");

    render(
      <LanguageProvider translations={translations}>
        <TranslationDisplay />
      </LanguageProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("nav-companies")).toHaveTextContent(en.nav.companies);
    });
    expect(document.documentElement.lang).toBe("en");
  });

  it("should fall back to English for unknown browser language", async () => {
    vi.spyOn(navigator, "language", "get").mockReturnValue("fr");

    render(
      <LanguageProvider translations={translations}>
        <TranslationDisplay />
      </LanguageProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("nav-companies")).toHaveTextContent(en.nav.companies);
    });
    expect(document.documentElement.lang).toBe("en");
  });

  it("should prefer stored language over browser language", () => {
    // When localStorage has "en" stored but browser is "de",
    // the provider should use the stored preference
    renderWithProviders(<TranslationDisplay />, { language: "en" });

    expect(screen.getByTestId("nav-companies")).toHaveTextContent(en.nav.companies);
    expect(document.documentElement.lang).toBe("en");
  });

  it("should work when localStorage is not available", async () => {
    vi.spyOn(navigator, "language", "get").mockReturnValue("de");
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("localStorage disabled");
    });

    render(
      <LanguageProvider translations={translations}>
        <TranslationDisplay />
      </LanguageProvider>,
    );

    await waitFor(() => {
      expect(screen.getByTestId("nav-companies")).toHaveTextContent(de.nav.companies);
    });
  });
});

describe("Language Switching", () => {
  it("should switch from English to German", () => {
    renderWithProviders(<LanguageSwitchTest />, { language: "en" });

    expect(screen.getByTestId("text")).toHaveTextContent(en.nav.companies);

    fireEvent.click(screen.getByTestId("switch-de"));

    expect(screen.getByTestId("lang")).toHaveTextContent("de");
    expect(screen.getByTestId("text")).toHaveTextContent(de.nav.companies);
    expect(document.documentElement.lang).toBe("de");
  });

  it("should switch from German to English", () => {
    renderWithProviders(<LanguageSwitchTest />, { language: "de" });

    expect(screen.getByTestId("text")).toHaveTextContent(de.nav.companies);

    fireEvent.click(screen.getByTestId("switch-en"));

    expect(screen.getByTestId("lang")).toHaveTextContent("en");
    expect(screen.getByTestId("text")).toHaveTextContent(en.nav.companies);
    expect(document.documentElement.lang).toBe("en");
  });

  it("should do nothing when clicking already active language", () => {
    renderWithProviders(<LanguageSwitchTest />, { language: "de" });

    fireEvent.click(screen.getByTestId("switch-de"));

    expect(screen.getByTestId("lang")).toHaveTextContent("de");
    expect(screen.getByTestId("text")).toHaveTextContent(de.nav.companies);
  });
});
