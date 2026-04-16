import { describe, it, expect, afterEach } from "vitest";
import { cleanup, screen } from "@testing-library/react";
import { ForbiddenPage } from "@/components/forbidden-page";
import { de } from "@/lib/i18n/de";
import { en } from "@/lib/i18n/en";
import { renderWithProviders } from "@/test/test-utils";

afterEach(() => {
  cleanup();
});

describe("ForbiddenPage", () => {
  it("renders German heading, description, and back-to-home link by default", () => {
    renderWithProviders(<ForbiddenPage />);

    expect(screen.getByText(de.errors.forbidden.title)).toBeInTheDocument();
    expect(screen.getByText(de.errors.forbidden.description)).toBeInTheDocument();
    const backLink = screen.getByRole("link", { name: de.errors.forbidden.backToHome });
    expect(backLink).toHaveAttribute("href", "/companies");
  });

  it("renders English translations when language is en", () => {
    renderWithProviders(<ForbiddenPage />, { language: "en" });

    expect(screen.getByText(en.errors.forbidden.title)).toBeInTheDocument();
    expect(screen.getByText(en.errors.forbidden.description)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: en.errors.forbidden.backToHome })).toBeInTheDocument();
  });
});
