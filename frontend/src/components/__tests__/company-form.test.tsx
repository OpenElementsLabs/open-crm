import { describe, it, expect, afterEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { CompanyForm } from "@/components/company-form";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { CompanyDto } from "@/lib/types";

const S = de.companies.form;

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
  usePathname: () => "/companies/new",
}));

const mockCreateCompany = vi.fn();
const mockUpdateCompany = vi.fn();
const mockUploadCompanyLogo = vi.fn();
const mockDeleteCompanyLogo = vi.fn();
const mockGetCompanyLogoUrl = vi.fn().mockReturnValue("/api/companies/test-id/logo");

vi.mock("@/lib/api", () => ({
  createCompany: (...args: unknown[]) => mockCreateCompany(...args),
  updateCompany: (...args: unknown[]) => mockUpdateCompany(...args),
  uploadCompanyLogo: (...args: unknown[]) => mockUploadCompanyLogo(...args),
  deleteCompanyLogo: (...args: unknown[]) => mockDeleteCompanyLogo(...args),
  getCompanyLogoUrl: (...args: unknown[]) => mockGetCompanyLogoUrl(...args),
  getTags: vi.fn().mockResolvedValue({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } }),
}));

const existingCompany: CompanyDto = {
  id: "test-id",
  name: "Open Elements GmbH",
  email: "info@open-elements.com",
  website: "https://open-elements.com",
  street: "Musterstraße",
  houseNumber: "42",
  zipCode: "12345",
  city: "Berlin",
  country: "Germany",
  phoneNumber: null,
  description: null,
  bankName: null,
  bic: null,
  iban: null,
  vatId: null,
  brevo: false,
  hasLogo: false,
  contactCount: 0,
  commentCount: 0,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
  tagIds: [],
};

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("CompanyForm", () => {
  describe("create mode", () => {
    it("should render all form fields with save and cancel buttons", () => {
      renderWithProviders(<CompanyForm />);

      expect(screen.getByLabelText(new RegExp(S.name))).toBeInTheDocument();
      expect(screen.getByLabelText(S.email)).toBeInTheDocument();
      expect(screen.getByLabelText(S.website)).toBeInTheDocument();
      expect(screen.getByLabelText(S.street)).toBeInTheDocument();
      expect(screen.getByLabelText(S.houseNumber)).toBeInTheDocument();
      expect(screen.getByLabelText(S.zipCode)).toBeInTheDocument();
      expect(screen.getByLabelText(S.city)).toBeInTheDocument();
      expect(screen.getByLabelText(S.country)).toBeInTheDocument();
      expect(screen.getByText(S.save)).toBeInTheDocument();
      expect(screen.getByText(S.cancel)).toBeInTheDocument();
    });

    it("should show title for create mode", () => {
      renderWithProviders(<CompanyForm />);
      expect(screen.getByText(S.createTitle)).toBeInTheDocument();
    });

    it("should validate name is required", async () => {
      renderWithProviders(<CompanyForm />);

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(screen.getByText(S.nameRequired)).toBeInTheDocument();
      });

      expect(mockCreateCompany).not.toHaveBeenCalled();
    });

    it("should submit and redirect to detail page", async () => {
      mockCreateCompany.mockResolvedValue({ ...existingCompany, id: "new-id" });

      renderWithProviders(<CompanyForm />);

      fireEvent.change(screen.getByLabelText(new RegExp(S.name)), {
        target: { value: "New Corp" },
      });

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(mockCreateCompany).toHaveBeenCalledWith(
          expect.objectContaining({ name: "New Corp" }),
        );
        expect(mockPush).toHaveBeenCalledWith("/companies/new-id");
      });
    });

    it("should navigate to list on cancel", () => {
      renderWithProviders(<CompanyForm />);

      fireEvent.click(screen.getByText(S.cancel));

      expect(mockPush).toHaveBeenCalledWith("/companies");
    });

    it("should show error on API failure", async () => {
      mockCreateCompany.mockRejectedValue(new Error("Server error"));

      renderWithProviders(<CompanyForm />);

      fireEvent.change(screen.getByLabelText(new RegExp(S.name)), {
        target: { value: "Test" },
      });

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(screen.getByText(S.errorGeneric)).toBeInTheDocument();
      });
    });
  });

  describe("edit mode", () => {
    it("should pre-fill form with existing data", () => {
      renderWithProviders(<CompanyForm company={existingCompany} />);

      expect(screen.getByDisplayValue("Open Elements GmbH")).toBeInTheDocument();
      expect(screen.getByDisplayValue("info@open-elements.com")).toBeInTheDocument();
      expect(screen.getByDisplayValue("Berlin")).toBeInTheDocument();
    });

    it("should show title for edit mode", () => {
      renderWithProviders(<CompanyForm company={existingCompany} />);
      expect(screen.getByText(S.editTitle)).toBeInTheDocument();
    });

    it("should submit update and redirect to detail", async () => {
      mockUpdateCompany.mockResolvedValue(existingCompany);

      renderWithProviders(<CompanyForm company={existingCompany} />);

      fireEvent.change(screen.getByLabelText(new RegExp(S.name)), {
        target: { value: "Updated Corp" },
      });

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(mockUpdateCompany).toHaveBeenCalledWith(
          "test-id",
          expect.objectContaining({ name: "Updated Corp" }),
        );
        expect(mockPush).toHaveBeenCalledWith("/companies/test-id");
      });
    });

    it("should validate name is required on edit", async () => {
      renderWithProviders(<CompanyForm company={existingCompany} />);

      fireEvent.change(screen.getByLabelText(new RegExp(S.name)), {
        target: { value: "" },
      });

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(screen.getByText(S.nameRequired)).toBeInTheDocument();
      });

      expect(mockUpdateCompany).not.toHaveBeenCalled();
    });

    it("should navigate to detail on cancel", () => {
      renderWithProviders(<CompanyForm company={existingCompany} />);

      fireEvent.click(screen.getByText(S.cancel));

      expect(mockPush).toHaveBeenCalledWith("/companies/test-id");
    });
  });

  describe("finance fields", () => {
    it("should render finance section with all four fields", () => {
      renderWithProviders(<CompanyForm />);

      expect(screen.getByText(S.financeTitle)).toBeInTheDocument();
      expect(screen.getByLabelText(S.bankName)).toBeInTheDocument();
      expect(screen.getByLabelText(S.bic)).toBeInTheDocument();
      expect(screen.getByLabelText(S.iban)).toBeInTheDocument();
      expect(screen.getByLabelText(S.vatId)).toBeInTheDocument();
    });

    it("should pre-fill finance fields in edit mode", () => {
      const companyWithFinance: CompanyDto = {
        ...existingCompany,
        bankName: "Deutsche Bank",
        bic: "DEUTDEFF",
        iban: "DE89370400440532013000",
        vatId: "DE123456789",
      };

      renderWithProviders(<CompanyForm company={companyWithFinance} />);

      expect(screen.getByDisplayValue("Deutsche Bank")).toBeInTheDocument();
      expect(screen.getByDisplayValue("DEUTDEFF")).toBeInTheDocument();
      expect(screen.getByDisplayValue("DE89370400440532013000")).toBeInTheDocument();
      expect(screen.getByDisplayValue("DE123456789")).toBeInTheDocument();
    });

    it("should include finance fields in submit data", async () => {
      mockCreateCompany.mockResolvedValue({ ...existingCompany, id: "new-id" });

      renderWithProviders(<CompanyForm />);

      fireEvent.change(screen.getByLabelText(new RegExp(S.name)), {
        target: { value: "Finance Corp" },
      });
      fireEvent.change(screen.getByLabelText(S.iban), {
        target: { value: "DE89370400440532013000" },
      });

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(mockCreateCompany).toHaveBeenCalledWith(
          expect.objectContaining({
            name: "Finance Corp",
            iban: "DE89370400440532013000",
          }),
        );
      });
    });

    it("should submit null for empty finance fields", async () => {
      mockCreateCompany.mockResolvedValue({ ...existingCompany, id: "new-id" });

      renderWithProviders(<CompanyForm />);

      fireEvent.change(screen.getByLabelText(new RegExp(S.name)), {
        target: { value: "No Finance Corp" },
      });

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(mockCreateCompany).toHaveBeenCalledWith(
          expect.objectContaining({
            bankName: null,
            bic: null,
            iban: null,
            vatId: null,
          }),
        );
      });
    });
  });

  describe("image upload", () => {
    it("should show upload logo button", () => {
      renderWithProviders(<CompanyForm />);

      expect(screen.getByText(S.uploadLogo)).toBeInTheDocument();
    });

    it("should show client-side error for oversized file", async () => {
      renderWithProviders(<CompanyForm />);

      const fileInput = document.querySelector("input[type='file']") as HTMLInputElement;
      const oversizedFile = new File(["x".repeat(3 * 1024 * 1024)], "big.png", {
        type: "image/png",
      });

      fireEvent.change(fileInput, { target: { files: [oversizedFile] } });

      await waitFor(() => {
        expect(screen.getByText(S.imageTooLarge)).toBeInTheDocument();
      });
    });
  });
});
