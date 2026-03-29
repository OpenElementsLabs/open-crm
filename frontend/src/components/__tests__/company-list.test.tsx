import { describe, it, expect, afterEach, vi, beforeEach } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { CompanyList } from "@/components/company-list";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { CompanyDto, Page } from "@/lib/types";

const S = de.companies;

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
  usePathname: () => "/companies",
}));

const mockGetCompanies = vi.fn();
const mockDeleteCompany = vi.fn();
const mockRestoreCompany = vi.fn();
const mockGetCompanyLogoUrl = vi.fn().mockReturnValue("/api/companies/test-id/logo");

vi.mock("@/lib/api", () => ({
  getCompanies: (...args: unknown[]) => mockGetCompanies(...args),
  deleteCompany: (...args: unknown[]) => mockDeleteCompany(...args),
  restoreCompany: (...args: unknown[]) => mockRestoreCompany(...args),
  getCompanyLogoUrl: (...args: unknown[]) => mockGetCompanyLogoUrl(...args),
}));

function makeCompany(overrides: Partial<CompanyDto> = {}): CompanyDto {
  return {
    id: "test-id-1",
    name: "Open Elements",
    email: null,
    website: "open-elements.com",
    street: null,
    houseNumber: null,
    zipCode: null,
    city: null,
    country: null,
    deleted: false,
    brevo: false,
    hasLogo: false,
    contactCount: 0,
    commentCount: 0,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

function makePage(companies: CompanyDto[], totalElements?: number): Page<CompanyDto> {
  return {
    content: companies,
    totalElements: totalElements ?? companies.length,
    totalPages: Math.ceil((totalElements ?? companies.length) / 20),
    number: 0,
    size: 20,
    first: true,
    last: (totalElements ?? companies.length) <= 20,
  };
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("CompanyList", () => {
  describe("data display", () => {
    it("should render companies with name and website", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([
          makeCompany({ id: "1", name: "Open Elements", website: "open-elements.com" }),
          makeCompany({ id: "2", name: "Acme Corp", website: "acme.com" }),
        ]),
      );

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByText("Open Elements")).toBeInTheDocument();
        expect(screen.getByText("open-elements.com")).toBeInTheDocument();
        expect(screen.getByText("Acme Corp")).toBeInTheDocument();
        expect(screen.getByText("acme.com")).toBeInTheDocument();
      });
    });

    it("should show empty state when no companies exist", async () => {
      mockGetCompanies.mockResolvedValue(makePage([]));

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByText(S.empty)).toBeInTheDocument();
      });
    });

    it("should show loading skeleton while fetching", () => {
      mockGetCompanies.mockReturnValue(new Promise(() => {})); // never resolves

      const { container } = renderWithProviders(<CompanyList />);

      const skeletons = container.querySelectorAll("[data-slot='skeleton']");
      expect(skeletons.length).toBeGreaterThan(0);
    });
  });

  describe("pagination", () => {
    it("should show pagination controls when more than 20 items", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany()], 25),
      );

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByText(S.pagination.next)).toBeInTheDocument();
        expect(screen.getByText(S.pagination.previous)).toBeInTheDocument();
      });
    });

    it("should show record count in pagination", async () => {
      mockGetCompanies.mockResolvedValue({
        content: [makeCompany()],
        totalElements: 42,
        totalPages: 3,
        number: 0,
        size: 20,
        first: true,
        last: false,
      });

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByText(/42 Firmen/)).toBeInTheDocument();
      });
    });

    it("should navigate to next page when clicking next", async () => {
      mockGetCompanies.mockResolvedValue({
        content: [makeCompany()],
        totalElements: 25,
        totalPages: 2,
        number: 0,
        size: 20,
        first: true,
        last: false,
      });

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByText(S.pagination.next)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.pagination.next));

      await waitFor(() => {
        expect(mockGetCompanies).toHaveBeenCalledWith(
          expect.objectContaining({ page: 1 }),
        );
      });
    });
  });

  describe("filtering", () => {
    beforeEach(() => {
      mockGetCompanies.mockResolvedValue(makePage([makeCompany()]));
    });

    it("should render brevo filter dropdown", async () => {
      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        const allTriggers = screen.getAllByRole("combobox");
        const brevoTrigger = allTriggers.find((el) =>
          el.textContent?.includes(de.brevoFilter.all),
        );
        expect(brevoTrigger).toBeDefined();
      });
    });

    it("should filter by name when typing in name filter", async () => {
      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.filter.name)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.filter.name), {
        target: { value: "open" },
      });

      await waitFor(() => {
        expect(mockGetCompanies).toHaveBeenCalledWith(
          expect.objectContaining({ name: "open" }),
        );
      });
    });

  });

  describe("archived companies", () => {
    it("should exclude soft-deleted by default", async () => {
      mockGetCompanies.mockResolvedValue(makePage([makeCompany()]));

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(mockGetCompanies).toHaveBeenCalledWith(
          expect.objectContaining({ includeDeleted: false }),
        );
      });
    });

    it("should include soft-deleted when toggle clicked", async () => {
      mockGetCompanies.mockResolvedValue(makePage([makeCompany()]));

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByText(S.showArchived)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.showArchived));

      await waitFor(() => {
        expect(mockGetCompanies).toHaveBeenCalledWith(
          expect.objectContaining({ includeDeleted: true }),
        );
      });
    });

    it("should show archived companies with muted style", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany({ id: "deleted-1", name: "Deleted Co", deleted: true })]),
      );

      const { container } = renderWithProviders(<CompanyList />);

      await waitFor(() => {
        const row = container.querySelector("tr.opacity-50");
        expect(row).toBeInTheDocument();
      });
    });

    it("should show restore button for archived companies", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany({ id: "deleted-1", name: "Deleted Co", deleted: true })]),
      );

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        const restoreButton = screen.getByTitle(S.detail.restore);
        expect(restoreButton).toBeInTheDocument();
      });
    });
  });

  describe("navigation", () => {
    it("should navigate to detail when clicking a row", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany({ id: "test-id-1", name: "Open Elements" })]),
      );

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByText("Open Elements")).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText("Open Elements"));

      expect(mockPush).toHaveBeenCalledWith("/companies/test-id-1");
    });

    it("should show Neue Firma button linking to /companies/new", async () => {
      mockGetCompanies.mockResolvedValue(makePage([makeCompany()]));

      const { container } = renderWithProviders(<CompanyList />);

      await waitFor(() => {
        const newButton = Array.from(container.querySelectorAll("a")).find(
          (a) => a.getAttribute("href") === "/companies/new",
        );
        expect(newButton).toBeInTheDocument();
      });
    });
  });

  describe("delete", () => {
    it("should open confirmation dialog when delete button clicked", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany({ id: "1", name: "Test Corp" })]),
      );

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByTitle(S.detail.delete)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTitle(S.detail.delete));

      await waitFor(() => {
        expect(
          screen.getByText(S.deleteDialog.description.replace("{name}", "Test Corp")),
        ).toBeInTheDocument();
      });
    });

    it("should delete company and refresh list on confirm", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany({ id: "1", name: "Test Corp" })]),
      );
      mockDeleteCompany.mockResolvedValue(undefined);

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByTitle(S.detail.delete)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTitle(S.detail.delete));

      await waitFor(() => {
        expect(screen.getByText(S.deleteDialog.confirm)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.deleteDialog.confirm));

      await waitFor(() => {
        expect(mockDeleteCompany).toHaveBeenCalledWith("1");
      });
    });

    it("should close dialog on cancel without deleting", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany({ id: "1", name: "Test Corp" })]),
      );

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByTitle(S.detail.delete)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTitle(S.detail.delete));

      await waitFor(() => {
        expect(screen.getByText(S.deleteDialog.cancel)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.deleteDialog.cancel));

      expect(mockDeleteCompany).not.toHaveBeenCalled();
    });

    it("should show error dialog on 409 conflict", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany({ id: "1", name: "Test Corp" })]),
      );
      mockDeleteCompany.mockRejectedValue(new Error("CONFLICT"));

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByTitle(S.detail.delete)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTitle(S.detail.delete));

      await waitFor(() => {
        expect(screen.getByText(S.deleteDialog.confirm)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.deleteDialog.confirm));

      await waitFor(() => {
        expect(screen.getByText(S.deleteDialog.errorConflict)).toBeInTheDocument();
      });
    });
  });

  describe("image display", () => {
    it("should show logo thumbnail when hasLogo is true", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany({ id: "logo-1", hasLogo: true, name: "Logo Corp" })]),
      );

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        const rows = screen.getAllByRole("row");
        const dataRow = rows[1];
        const firstCell = dataRow.querySelectorAll("td")[0];
        const img = firstCell.querySelector("img");
        expect(img).toBeInTheDocument();
        expect(img?.getAttribute("alt")).toBe("Logo Corp");
      });
    });

    it("should show placeholder icon when hasLogo is false", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany({ hasLogo: false })]),
      );

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        const rows = screen.getAllByRole("row");
        const dataRow = rows[1];
        const firstCell = dataRow.querySelectorAll("td")[0];
        const img = firstCell.querySelector("img");
        expect(img).toBeNull();
        const svg = firstCell.querySelector("svg");
        expect(svg).toBeInTheDocument();
      });
    });

    it("should have correct column order", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany()]),
      );

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        const headerRow = screen.getAllByRole("row")[0];
        const headers = headerRow.querySelectorAll("th");
        expect(headers[0].textContent).toBe("");
        expect(headers[1].textContent).toBe(S.columns.name);
        expect(headers[2].textContent).toBe(S.columns.website);
        expect(headers[3].textContent).toBe(S.columns.contacts);
        expect(headers[4].textContent).toBe(S.columns.comments);
        expect(headers[5].textContent).toBe(S.columns.actions);
      });
    });
  });

  describe("restore", () => {
    it("should restore company when restore button clicked", async () => {
      mockGetCompanies.mockResolvedValue(
        makePage([makeCompany({ id: "deleted-1", deleted: true })]),
      );
      mockRestoreCompany.mockResolvedValue(makeCompany({ id: "deleted-1", deleted: false }));

      renderWithProviders(<CompanyList />);

      await waitFor(() => {
        expect(screen.getByTitle(S.detail.restore)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTitle(S.detail.restore));

      await waitFor(() => {
        expect(mockRestoreCompany).toHaveBeenCalledWith("deleted-1");
      });
    });
  });
});
