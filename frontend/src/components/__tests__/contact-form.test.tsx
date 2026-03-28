import { describe, it, expect, afterEach, beforeEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { ContactForm } from "@/components/contact-form";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { ContactDto, CompanyDto } from "@/lib/types";

const S = de.contacts.form;

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
  usePathname: () => "/contacts/new",
}));

const mockCreateContact = vi.fn();
const mockUpdateContact = vi.fn();
const mockGetCompaniesForSelect = vi.fn();
const mockUploadContactPhoto = vi.fn();
const mockDeleteContactPhoto = vi.fn();
const mockGetContactPhotoUrl = vi.fn().mockReturnValue("/api/contacts/test-id/photo");

vi.mock("@/lib/api", () => ({
  createContact: (...args: unknown[]) => mockCreateContact(...args),
  updateContact: (...args: unknown[]) => mockUpdateContact(...args),
  getCompaniesForSelect: (...args: unknown[]) => mockGetCompaniesForSelect(...args),
  uploadContactPhoto: (...args: unknown[]) => mockUploadContactPhoto(...args),
  deleteContactPhoto: (...args: unknown[]) => mockDeleteContactPhoto(...args),
  getContactPhotoUrl: (...args: unknown[]) => mockGetContactPhotoUrl(...args),
}));

const testCompanies: CompanyDto[] = [
  {
    id: "company-1",
    name: "Open Elements",
    email: null,
    website: null,
    street: null,
    houseNumber: null,
    zipCode: null,
    city: null,
    country: null,
    deleted: false,
    hasLogo: false,
    contactCount: 0,
    commentCount: 0,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  },
];

const existingContact: ContactDto = {
  id: "test-id",
  firstName: "Max",
  lastName: "Mustermann",
  email: "max@example.com",
  position: "CEO",
  gender: "MALE",
  linkedInUrl: "https://linkedin.com/in/max",
  phoneNumber: "+49 123 456",
  companyId: "company-1",
  companyName: "Open Elements",
  companyDeleted: false,
  commentCount: 0,
  hasPhoto: false,
  birthday: "1990-03-15",
  syncedToBrevo: false,
  doubleOptIn: false,
  language: "DE",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("ContactForm", () => {
  beforeEach(() => {
    mockGetCompaniesForSelect.mockResolvedValue(testCompanies);
  });

  describe("create mode", () => {
    it("should render form fields with save and cancel buttons", () => {
      renderWithProviders(<ContactForm />);

      expect(screen.getByLabelText(new RegExp(S.firstName))).toBeInTheDocument();
      expect(screen.getByLabelText(new RegExp(S.lastName))).toBeInTheDocument();
      expect(screen.getByLabelText(S.email)).toBeInTheDocument();
      expect(screen.getByLabelText(S.position)).toBeInTheDocument();
      expect(screen.getByLabelText(S.phone)).toBeInTheDocument();
      expect(screen.getByLabelText(S.linkedIn)).toBeInTheDocument();
      expect(screen.getByText(S.save)).toBeInTheDocument();
      expect(screen.getByText(S.cancel)).toBeInTheDocument();
    });

    it("should show title for create mode", () => {
      renderWithProviders(<ContactForm />);
      expect(screen.getByText(S.createTitle)).toBeInTheDocument();
    });

    it("should validate firstName is required", async () => {
      renderWithProviders(<ContactForm />);

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(screen.getByText(S.firstNameRequired)).toBeInTheDocument();
      });

      expect(mockCreateContact).not.toHaveBeenCalled();
    });

    it("should validate lastName is required", async () => {
      renderWithProviders(<ContactForm />);

      fireEvent.change(screen.getByLabelText(new RegExp(S.firstName)), {
        target: { value: "Max" },
      });

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(screen.getByText(S.lastNameRequired)).toBeInTheDocument();
      });

      expect(mockCreateContact).not.toHaveBeenCalled();
    });

    it("should validate language is required", async () => {
      renderWithProviders(<ContactForm />);

      fireEvent.change(screen.getByLabelText(new RegExp(S.firstName)), {
        target: { value: "Max" },
      });
      fireEvent.change(screen.getByLabelText(new RegExp(S.lastName)), {
        target: { value: "Mustermann" },
      });

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(screen.getByText(S.languageRequired)).toBeInTheDocument();
      });

      expect(mockCreateContact).not.toHaveBeenCalled();
    });

    it("should load companies for select dropdown", async () => {
      renderWithProviders(<ContactForm />);

      await waitFor(() => {
        expect(mockGetCompaniesForSelect).toHaveBeenCalled();
      });
    });

    it("should show noCompany option for company select", () => {
      renderWithProviders(<ContactForm />);

      // The default company selection should indicate "No company"
      const companyTriggers = screen.getAllByText(S.noCompany);
      expect(companyTriggers.length).toBeGreaterThanOrEqual(1);
    });

    it("should show notSpecified option for gender select", () => {
      renderWithProviders(<ContactForm />);

      // Gender defaults to "Not specified"
      const genderTriggers = screen.getAllByText(S.notSpecified);
      expect(genderTriggers.length).toBeGreaterThanOrEqual(1);
    });

    it("should navigate to contact list on cancel", () => {
      renderWithProviders(<ContactForm />);

      fireEvent.click(screen.getByText(S.cancel));

      expect(mockPush).toHaveBeenCalledWith("/contacts");
    });
  });

  describe("edit mode", () => {
    it("should pre-fill text fields with existing data", () => {
      renderWithProviders(<ContactForm contact={existingContact} />);

      expect(screen.getByDisplayValue("Max")).toBeInTheDocument();
      expect(screen.getByDisplayValue("Mustermann")).toBeInTheDocument();
      expect(screen.getByDisplayValue("max@example.com")).toBeInTheDocument();
      expect(screen.getByDisplayValue("CEO")).toBeInTheDocument();
      expect(screen.getByDisplayValue("+49 123 456")).toBeInTheDocument();
      expect(screen.getByDisplayValue("https://linkedin.com/in/max")).toBeInTheDocument();
    });

    it("should show title for edit mode", () => {
      renderWithProviders(<ContactForm contact={existingContact} />);
      expect(screen.getByText(S.editTitle)).toBeInTheDocument();
    });

    it("should submit update and redirect to detail", async () => {
      mockUpdateContact.mockResolvedValue(existingContact);

      renderWithProviders(<ContactForm contact={existingContact} />);

      fireEvent.change(screen.getByLabelText(new RegExp(S.firstName)), {
        target: { value: "Maximilian" },
      });

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(mockUpdateContact).toHaveBeenCalledWith(
          "test-id",
          expect.objectContaining({ firstName: "Maximilian" }),
        );
        expect(mockPush).toHaveBeenCalledWith("/contacts/test-id");
      });
    });

    it("should validate required fields on edit", async () => {
      renderWithProviders(<ContactForm contact={existingContact} />);

      fireEvent.change(screen.getByLabelText(new RegExp(S.firstName)), {
        target: { value: "" },
      });

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(screen.getByText(S.firstNameRequired)).toBeInTheDocument();
      });

      expect(mockUpdateContact).not.toHaveBeenCalled();
    });

    it("should show error on API failure", async () => {
      mockUpdateContact.mockRejectedValue(new Error("Server error"));

      renderWithProviders(<ContactForm contact={existingContact} />);

      fireEvent.change(screen.getByLabelText(new RegExp(S.firstName)), {
        target: { value: "Updated" },
      });

      fireEvent.click(screen.getByText(S.save));

      await waitFor(() => {
        expect(screen.getByText(S.errorGeneric)).toBeInTheDocument();
      });
    });

    it("should navigate to detail on cancel", () => {
      renderWithProviders(<ContactForm contact={existingContact} />);

      fireEvent.click(screen.getByText(S.cancel));

      expect(mockPush).toHaveBeenCalledWith("/contacts/test-id");
    });
  });

  describe("image upload", () => {
    it("should show upload photo button", () => {
      renderWithProviders(<ContactForm />);

      expect(screen.getByText(S.uploadPhoto)).toBeInTheDocument();
    });

    it("should show client-side error for invalid format", async () => {
      renderWithProviders(<ContactForm />);

      const fileInput = document.querySelector("input[type='file']") as HTMLInputElement;
      const pngFile = new File(["fake-png-data"], "photo.png", {
        type: "image/png",
      });

      fireEvent.change(fileInput, { target: { files: [pngFile] } });

      await waitFor(() => {
        expect(screen.getByText(S.imageInvalidFormat)).toBeInTheDocument();
      });
    });
  });
});
