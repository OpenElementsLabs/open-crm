import { describe, it, expect, afterEach, beforeEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { CompanyDetail } from "@/components/company-detail";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { CompanyDto } from "@/lib/types";

const S = de.companies;

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
  usePathname: () => "/companies/test-id",
}));

const mockDeleteCompany = vi.fn();
const mockGetCompanyComments = vi.fn();
const mockGetCompanyLogoUrl = vi.fn().mockReturnValue("/api/companies/test-id/logo");

vi.mock("@/lib/api", () => ({
  deleteCompany: (...args: unknown[]) => mockDeleteCompany(...args),
  getCompanyComments: (...args: unknown[]) => mockGetCompanyComments(...args),
  createCompanyComment: vi.fn(),
  getCompanyLogoUrl: (...args: unknown[]) => mockGetCompanyLogoUrl(...args),
}));

const testCompany: CompanyDto = {
  id: "test-id",
  name: "Open Elements GmbH",
  email: "info@open-elements.com",
  website: "https://open-elements.com",
  street: "Musterstraße",
  houseNumber: "42",
  zipCode: "12345",
  city: "Berlin",
  country: "Germany",
  deleted: false,
  brevo: false,
  hasLogo: false,
  contactCount: 3,
  commentCount: 5,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

beforeEach(() => {
  mockGetCompanyComments.mockResolvedValue({
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 20,
    first: true,
    last: true,
  });
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("CompanyDetail", () => {
  it("should render all company fields", () => {
    renderWithProviders(<CompanyDetail company={testCompany} />);

    // Name appears in heading and detail — check at least one is present
    expect(screen.getAllByText("Open Elements GmbH").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("info@open-elements.com")).toBeInTheDocument();
    expect(screen.getByText("https://open-elements.com")).toBeInTheDocument();
    expect(screen.getByText("Musterstraße")).toBeInTheDocument();
    expect(screen.getByText("42")).toBeInTheDocument();
    expect(screen.getByText("12345")).toBeInTheDocument();
    expect(screen.getByText("Berlin")).toBeInTheDocument();
    expect(screen.getByText("Germany")).toBeInTheDocument();
  });

  it("should show edit button linking to edit page", () => {
    const { container } = renderWithProviders(<CompanyDetail company={testCompany} />);

    const editLink = Array.from(container.querySelectorAll("a")).find(
      (a) => a.getAttribute("href") === "/companies/test-id/edit",
    );

    expect(editLink).toBeInTheDocument();
    expect(editLink?.textContent).toContain(S.detail.edit);
  });

  it("should show delete button", () => {
    renderWithProviders(<CompanyDetail company={testCompany} />);

    const deleteButton = screen.getByText(S.detail.delete);
    expect(deleteButton).toBeInTheDocument();
  });

  it("should show comment section with title, count, and Add Comment button", async () => {
    renderWithProviders(<CompanyDetail company={testCompany} />);

    await waitFor(() => {
      expect(screen.getByText(`${S.comments.title} (5)`)).toBeInTheDocument();
      expect(screen.getByText(S.comments.empty)).toBeInTheDocument();
      expect(screen.getByText(S.comments.add)).toBeInTheDocument();
    });
  });

  it("should open delete dialog and redirect on confirm", async () => {
    mockDeleteCompany.mockResolvedValue(undefined);

    renderWithProviders(<CompanyDetail company={testCompany} />);

    // Click the page-level delete button
    const deleteButtons = screen.getAllByText(S.detail.delete);
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(
        screen.getByText(S.deleteDialog.description.replace("{name}", "Open Elements GmbH")),
      ).toBeInTheDocument();
    });

    // Click the dialog confirm button (last "Löschen" button)
    const allDeleteButtons = screen.getAllByText(S.deleteDialog.confirm);
    fireEvent.click(allDeleteButtons[allDeleteButtons.length - 1]);

    await waitFor(() => {
      expect(mockDeleteCompany).toHaveBeenCalledWith("test-id");
      expect(mockPush).toHaveBeenCalledWith("/companies");
    });
  });

  it("should show error dialog on 409 conflict", async () => {
    mockDeleteCompany.mockRejectedValue(new Error("CONFLICT"));

    renderWithProviders(<CompanyDetail company={testCompany} />);

    // Click the page-level delete button
    const deleteButtons = screen.getAllByText(S.detail.delete);
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      const confirmButtons = screen.getAllByText(S.deleteDialog.confirm);
      expect(confirmButtons.length).toBeGreaterThanOrEqual(2);
    });

    // Click the dialog confirm button
    const allConfirmButtons = screen.getAllByText(S.deleteDialog.confirm);
    fireEvent.click(allConfirmButtons[allConfirmButtons.length - 1]);

    await waitFor(() => {
      expect(screen.getByText(S.deleteDialog.errorConflict)).toBeInTheDocument();
    });
  });

  it("should show Brevo tag when company is from Brevo", () => {
    renderWithProviders(<CompanyDetail company={{ ...testCompany, brevo: true }} />);

    expect(screen.getByText("Brevo")).toBeInTheDocument();
  });

  it("should not show Brevo tag when company is not from Brevo", () => {
    renderWithProviders(<CompanyDetail company={{ ...testCompany, brevo: false }} />);

    expect(screen.queryByText("Brevo")).not.toBeInTheDocument();
  });

  it("should show logo image when company has logo", () => {
    const { container } = renderWithProviders(
      <CompanyDetail company={{ ...testCompany, hasLogo: true }} />,
    );

    const img = container.querySelector("img");
    expect(img).toBeInTheDocument();
    expect(img?.getAttribute("alt")).toBe("Open Elements GmbH");
  });

  it("should show placeholder when company has no logo", () => {
    const { container } = renderWithProviders(
      <CompanyDetail company={{ ...testCompany, hasLogo: false }} />,
    );

    const img = container.querySelector("img");
    expect(img).toBeNull();
    const svg = container.querySelector("svg");
    expect(svg).toBeInTheDocument();
  });
});
