import { describe, it, expect, afterEach } from "vitest";
import { screen, cleanup, fireEvent } from "@testing-library/react";
import { LanguageSwitch } from "@/components/language-switch";
import { renderWithProviders } from "@/test/test-utils";

afterEach(() => {
  cleanup();
});

describe("LanguageSwitch", () => {
  it("should render DE and EN buttons", () => {
    renderWithProviders(<LanguageSwitch />);

    expect(screen.getByText("DE")).toBeInTheDocument();
    expect(screen.getByText("EN")).toBeInTheDocument();
  });

  it("should highlight active language in green and bold", () => {
    renderWithProviders(<LanguageSwitch />, { language: "de" });

    const deButton = screen.getByText("DE");
    expect(deButton.className).toContain("text-oe-green");
    expect(deButton.className).toContain("font-bold");

    const enButton = screen.getByText("EN");
    expect(enButton.className).toContain("text-oe-white/70");
    expect(enButton.className).not.toContain("font-bold");
  });

  it("should highlight EN when English is active", () => {
    renderWithProviders(<LanguageSwitch />, { language: "en" });

    const enButton = screen.getByText("EN");
    expect(enButton.className).toContain("text-oe-green");
    expect(enButton.className).toContain("font-bold");

    const deButton = screen.getByText("DE");
    expect(deButton.className).toContain("text-oe-white/70");
  });

  it("should switch language when clicking inactive language", () => {
    renderWithProviders(<LanguageSwitch />, { language: "de" });

    fireEvent.click(screen.getByText("EN"));

    const enButton = screen.getByText("EN");
    expect(enButton.className).toContain("text-oe-green");
    expect(enButton.className).toContain("font-bold");
  });

  it("should show separator between buttons", () => {
    const { container } = renderWithProviders(<LanguageSwitch />);

    const separator = container.querySelector(".text-oe-white\\/30");
    expect(separator).toBeInTheDocument();
    expect(separator?.textContent).toBe("|");
  });
});
