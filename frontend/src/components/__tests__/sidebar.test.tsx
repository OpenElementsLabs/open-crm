import { describe, it, expect, afterEach, vi } from "vitest";
import { render, cleanup } from "@testing-library/react";
import { Sidebar } from "@/components/sidebar";
import { STRINGS } from "@/lib/constants";

// Mock next/navigation
vi.mock("next/navigation", () => ({
  usePathname: () => "/companies",
}));

afterEach(() => {
  cleanup();
});

describe("Sidebar", () => {
  it("should render navigation entries for Firmen and Server-Health", () => {
    const { container } = render(<Sidebar />);

    const links = container.querySelectorAll("a");
    const linkTexts = Array.from(links).map((link) => link.textContent);

    expect(linkTexts).toContain(STRINGS.nav.companies);
    expect(linkTexts).toContain(STRINGS.nav.health);
  });

  it("should render app title", () => {
    const { container } = render(<Sidebar />);

    const titles = container.querySelectorAll("a");
    const titleTexts = Array.from(titles).map((t) => t.textContent);

    expect(titleTexts).toContain(STRINGS.app.title);
  });

  it("should have hamburger button for mobile", () => {
    const { container } = render(<Sidebar />);

    const hamburgerButton = container.querySelector("button");
    expect(hamburgerButton).toBeInTheDocument();
  });

  it("should link Firmen to /companies", () => {
    const { container } = render(<Sidebar />);

    const companyLink = Array.from(container.querySelectorAll("a")).find(
      (link) => link.textContent?.includes(STRINGS.nav.companies),
    );

    expect(companyLink).toHaveAttribute("href", "/companies");
  });

  it("should link Server-Health to /health", () => {
    const { container } = render(<Sidebar />);

    const healthLink = Array.from(container.querySelectorAll("a")).find(
      (link) => link.textContent?.includes(STRINGS.nav.health),
    );

    expect(healthLink).toHaveAttribute("href", "/health");
  });
});
