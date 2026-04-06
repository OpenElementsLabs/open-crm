import { describe, it, expect, afterEach, vi } from "vitest";
import { cleanup, screen, fireEvent, waitFor } from "@testing-library/react";
import { Sidebar } from "@/components/sidebar";
import { de } from "@/lib/i18n/de";
import { en } from "@/lib/i18n/en";
import { renderWithProviders } from "@/test/test-utils";
// Mock next/navigation
vi.mock("next/navigation", () => ({
  usePathname: () => "/companies",
}));

afterEach(() => {
  cleanup();
});

function createSession(roles: string[]) {
  return {
    user: { name: "Alice", email: "alice@example.com", image: null },
    expires: new Date(Date.now() + 86400000).toISOString(),
    accessToken: "mock-token",
    roles,
  };
}

describe("Sidebar", () => {
  it("should render navigation entries for Firmen and Admin", () => {
    const { container } = renderWithProviders(<Sidebar />);

    const links = container.querySelectorAll("a");
    const linkTexts = Array.from(links).map((link) => link.textContent);

    expect(linkTexts).toContain(de.nav.companies);
    expect(linkTexts).toContain(de.nav.admin);
  });

  it("should render app title", () => {
    const { container } = renderWithProviders(<Sidebar />);

    const titles = container.querySelectorAll("a");
    const titleTexts = Array.from(titles).map((t) => t.textContent);

    expect(titleTexts).toContain(de.app.title);
  });

  it("should have hamburger button for mobile", () => {
    const { container } = renderWithProviders(<Sidebar />);

    const hamburgerButton = container.querySelector("button");
    expect(hamburgerButton).toBeInTheDocument();
  });

  it("should link Firmen to /companies", () => {
    const { container } = renderWithProviders(<Sidebar />);

    const companyLink = Array.from(container.querySelectorAll("a")).find(
      (link) => link.textContent?.includes(de.nav.companies),
    );

    expect(companyLink).toHaveAttribute("href", "/companies");
  });

  it("should link Admin to /admin", () => {
    const { container } = renderWithProviders(<Sidebar />);

    const adminLink = Array.from(container.querySelectorAll("a")).find(
      (link) => link.textContent?.includes(de.nav.admin),
    );

    expect(adminLink).toHaveAttribute("href", "/admin");
  });

  it("should render Kontakte navigation entry", () => {
    const { container } = renderWithProviders(<Sidebar />);

    const links = container.querySelectorAll("a");
    const linkTexts = Array.from(links).map((link) => link.textContent);

    expect(linkTexts).toContain(de.nav.contacts);
  });

  it("should link Kontakte to /contacts", () => {
    const { container } = renderWithProviders(<Sidebar />);

    const contactsLink = Array.from(container.querySelectorAll("a")).find(
      (link) => link.textContent?.includes(de.nav.contacts),
    );

    expect(contactsLink).toHaveAttribute("href", "/contacts");
  });

  it("should show Contacts in English when language is en", () => {
    const { container } = renderWithProviders(<Sidebar />, { language: "en" });

    const links = container.querySelectorAll("a");
    const linkTexts = Array.from(links).map((link) => link.textContent);

    expect(linkTexts).toContain(en.nav.contacts);
  });

  it("should show Kontakte in German when language is de", () => {
    const { container } = renderWithProviders(<Sidebar />, { language: "de" });

    const links = container.querySelectorAll("a");
    const linkTexts = Array.from(links).map((link) => link.textContent);

    expect(linkTexts).toContain(de.nav.contacts);
  });
});

describe("Sidebar — Role Tooltip", () => {
  it("should show user name when session has user", () => {
    const session = createSession(["CRM-ADMIN"]);
    renderWithProviders(<Sidebar />, { session });

    expect(screen.getByText("Alice")).toBeInTheDocument();
  });

  it("should show single role in tooltip on hover", async () => {
    const session = createSession(["CRM-ADMIN"]);
    renderWithProviders(<Sidebar />, { session });

    const userName = screen.getByText("Alice");
    fireEvent.pointerEnter(userName);
    fireEvent.focus(userName);

    await waitFor(() => {
      expect(screen.getByText("CRM-ADMIN")).toBeInTheDocument();
    });
  });

  it("should show multiple roles comma-separated in tooltip", async () => {
    const session = createSession(["CRM-ADMIN", "CRM-READONLY"]);
    renderWithProviders(<Sidebar />, { session });

    const userName = screen.getByText("Alice");
    fireEvent.pointerEnter(userName);
    fireEvent.focus(userName);

    await waitFor(() => {
      expect(screen.getByText("CRM-ADMIN, CRM-READONLY")).toBeInTheDocument();
    });
  });

  it("should show 'Keine Rollen zugewiesen' for empty roles in German", async () => {
    const session = createSession([]);
    renderWithProviders(<Sidebar />, { session, language: "de" });

    const userName = screen.getByText("Alice");
    fireEvent.pointerEnter(userName);
    fireEvent.focus(userName);

    await waitFor(() => {
      expect(screen.getByText(de.user.noRoles)).toBeInTheDocument();
    });
  });

  it("should show 'No roles assigned' for empty roles in English", async () => {
    const session = createSession([]);
    renderWithProviders(<Sidebar />, { session, language: "en" });

    const userName = screen.getByText("Alice");
    fireEvent.pointerEnter(userName);
    fireEvent.focus(userName);

    await waitFor(() => {
      expect(screen.getByText(en.user.noRoles)).toBeInTheDocument();
    });
  });
});
