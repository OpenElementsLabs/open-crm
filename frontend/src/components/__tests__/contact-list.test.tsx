import { describe, it, expect, afterEach, vi, beforeEach } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { ContactList } from "@/components/contact-list";
import { de } from "@/lib/i18n/de";
import { en } from "@/lib/i18n/en";
import { renderWithProviders } from "@/test/test-utils";
import type { ContactDto, CompanyDto, Page } from "@/lib/types";

const S = de.contacts;

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: vi.fn() }),
  usePathname: () => "/contacts",
  useSearchParams: () => new URLSearchParams(),
}));

const mockGetContacts = vi.fn();
const mockDeleteContact = vi.fn();
const mockGetCompaniesForSelect = vi.fn();
const mockGetContactPhotoUrl = vi.fn().mockReturnValue("/api/contacts/test-id/photo");
const mockCreateContactComment = vi.fn();

vi.mock("@/lib/api", () => ({
  getContacts: (...args: unknown[]) => mockGetContacts(...args),
  deleteContact: (...args: unknown[]) => mockDeleteContact(...args),
  getCompaniesForSelect: (...args: unknown[]) => mockGetCompaniesForSelect(...args),
  getContactPhotoUrl: (...args: unknown[]) => mockGetContactPhotoUrl(...args),
  createContactComment: (...args: unknown[]) => mockCreateContactComment(...args),
  getTags: vi.fn().mockResolvedValue({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } }),
}));

function makeContact(overrides: Partial<ContactDto> = {}): ContactDto {
  return {
    id: "contact-1",
    firstName: "Max",
    lastName: "Mustermann",
    email: "max@example.com",
    position: "CEO",
    gender: "MALE",
    linkedInUrl: "https://linkedin.com/in/max",
    phoneNumber: "+49 123 456",
    companyId: "company-1",
    companyName: "Open Elements",
    commentCount: 0,
    hasPhoto: false,
    birthday: null,
    brevo: false,
    language: "DE",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    tagIds: [],
    ...overrides,
  };
}

function makePage(contacts: ContactDto[], totalElements?: number): Page<ContactDto> {
  return {
    content: contacts,
    page: {
      size: 20,
      number: 0,
      totalElements: totalElements ?? contacts.length,
      totalPages: Math.ceil((totalElements ?? contacts.length) / 20) || 1,
    },
  };
}

const defaultCompanies: CompanyDto[] = [
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
    phoneNumber: null,
    deleted: false,
    brevo: false,
    hasLogo: false,
    contactCount: 0,
    commentCount: 0,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    tagIds: [],
  },
];

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("ContactList", () => {
  beforeEach(() => {
    mockGetCompaniesForSelect.mockResolvedValue(defaultCompanies);
  });

  describe("data display", () => {
    it("should render contacts with merged name, email, and company", async () => {
      mockGetContacts.mockResolvedValue(
        makePage([
          makeContact({ id: "1", firstName: "Max", lastName: "Mustermann", email: "max@example.com", companyName: "Open Elements" }),
          makeContact({ id: "2", firstName: "Anna", lastName: "Schmidt", email: "anna@example.com", companyName: "Acme Corp" }),
        ]),
      );

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByText("Max Mustermann")).toBeInTheDocument();
        expect(screen.getByText("max@example.com")).toBeInTheDocument();
        expect(screen.getByText("Open Elements")).toBeInTheDocument();
        expect(screen.getByText("Anna Schmidt")).toBeInTheDocument();
        expect(screen.getByText("anna@example.com")).toBeInTheDocument();
        expect(screen.getByText("Acme Corp")).toBeInTheDocument();
      });
    });

    it("should show empty state when no contacts exist", async () => {
      mockGetContacts.mockResolvedValue(makePage([]));

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByText(S.empty)).toBeInTheDocument();
      });
    });

    it("should show empty state when filters match nothing", async () => {
      mockGetContacts.mockResolvedValue(makePage([]));

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByText(S.empty)).toBeInTheDocument();
      });
    });

    it("should show empty company column for unassociated contacts", async () => {
      mockGetContacts.mockResolvedValue(
        makePage([makeContact({ companyId: null, companyName: null })]),
      );

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByText("Max Mustermann")).toBeInTheDocument();
      });

      // The company cell should be empty (no text content beyond header)
      const rows = screen.getAllByRole("row");
      const dataRow = rows[1]; // first data row
      const cells = dataRow.querySelectorAll("td");
      expect(cells[3].textContent).toBe("—");
    });

    it("should show loading skeleton while fetching", () => {
      mockGetContacts.mockReturnValue(new Promise(() => {}));

      const { container } = renderWithProviders(<ContactList />);

      const skeletons = container.querySelectorAll("[data-slot='skeleton']");
      expect(skeletons.length).toBeGreaterThan(0);
    });
  });

  describe("pagination", () => {
    it("should show pagination controls", async () => {
      mockGetContacts.mockResolvedValue({
        content: [makeContact()],
        page: {
          size: 20,
          number: 0,
          totalElements: 25,
          totalPages: 2,
        },
      });

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByText(S.pagination.next)).toBeInTheDocument();
        expect(screen.getByText(S.pagination.previous)).toBeInTheDocument();
      });
    });

    it("should show record count in pagination", async () => {
      mockGetContacts.mockResolvedValue({
        content: [makeContact()],
        page: {
          size: 20,
          number: 0,
          totalElements: 1,
          totalPages: 1,
        },
      });

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByText(/1 Kontakt/)).toBeInTheDocument();
      });
    });

    it("should navigate to next page when clicking next", async () => {
      mockGetContacts.mockResolvedValue({
        content: [makeContact()],
        page: {
          size: 20,
          number: 0,
          totalElements: 25,
          totalPages: 2,
        },
      });

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByText(S.pagination.next)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.pagination.next));

      await waitFor(() => {
        expect(mockGetContacts).toHaveBeenCalledWith(
          expect.objectContaining({ page: 1 }),
        );
      });
    });
  });

  describe("filtering", () => {
    beforeEach(() => {
      mockGetContacts.mockResolvedValue(makePage([makeContact()]));
    });

    it("should render brevo filter dropdown", async () => {
      renderWithProviders(<ContactList />);

      await waitFor(() => {
        const allTriggers = screen.getAllByRole("combobox");
        const brevoTrigger = allTriggers.find((el) =>
          el.textContent?.includes(de.brevoFilter.all),
        );
        expect(brevoTrigger).toBeDefined();
      });
    });

    it("should call API with search parameter", async () => {
      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.filter.search)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.filter.search), {
        target: { value: "Anna" },
      });

      await waitFor(() => {
        expect(mockGetContacts).toHaveBeenCalledWith(
          expect.objectContaining({ search: "Anna" }),
        );
      });
    });

    it("should show language filter with placeholder text", async () => {
      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByText("Max Mustermann")).toBeInTheDocument();
      });

      // The language filter select should be rendered with its placeholder
      const allTriggers = screen.getAllByRole("combobox");
      const langTrigger = allTriggers.find((el) =>
        el.textContent?.includes(S.filter.language),
      );
      expect(langTrigger).toBeDefined();
    });
  });

  describe("navigation", () => {
    it("should navigate to detail when clicking a row", async () => {
      mockGetContacts.mockResolvedValue(
        makePage([makeContact({ id: "contact-1", firstName: "Max" })]),
      );

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByText("Max Mustermann")).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText("Max Mustermann"));

      expect(mockPush).toHaveBeenCalledWith("/contacts/contact-1");
    });
  });

  describe("delete", () => {
    it("should open confirmation dialog with permanent warning when delete clicked", async () => {
      mockGetContacts.mockResolvedValue(
        makePage([makeContact({ id: "1", firstName: "Max", lastName: "Mustermann" })]),
      );

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByTitle(S.detail.delete)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTitle(S.detail.delete));

      await waitFor(() => {
        const expectedText = S.deleteDialog.description.replace("{name}", "Max Mustermann");
        expect(screen.getByText(expectedText)).toBeInTheDocument();
      });
    });

    it("should delete contact and refresh list on confirm", async () => {
      mockGetContacts.mockResolvedValue(
        makePage([makeContact({ id: "1" })]),
      );
      mockDeleteContact.mockResolvedValue(undefined);

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByTitle(S.detail.delete)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTitle(S.detail.delete));

      await waitFor(() => {
        expect(screen.getByText(S.deleteDialog.confirm)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.deleteDialog.confirm));

      await waitFor(() => {
        expect(mockDeleteContact).toHaveBeenCalledWith("1");
      });
    });

    it("should close dialog on cancel without deleting", async () => {
      mockGetContacts.mockResolvedValue(
        makePage([makeContact({ id: "1" })]),
      );

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByTitle(S.detail.delete)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTitle(S.detail.delete));

      await waitFor(() => {
        expect(screen.getByText(S.deleteDialog.cancel)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.deleteDialog.cancel));

      expect(mockDeleteContact).not.toHaveBeenCalled();
    });
  });

  describe("actions", () => {
    it("should show edit button in actions column", async () => {
      mockGetContacts.mockResolvedValue(
        makePage([makeContact({ id: "1", firstName: "Max", lastName: "Mustermann" })]),
      );

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByTitle(S.detail.edit)).toBeInTheDocument();
      });
    });
  });

  describe("print", () => {
    it("should show print button", async () => {
      mockGetContacts.mockResolvedValue(makePage([makeContact()]));

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        expect(screen.getByText(de.print.button)).toBeInTheDocument();
      });
    });
  });

  describe("image display", () => {
    it("should show photo thumbnail when hasPhoto is true", async () => {
      mockGetContacts.mockResolvedValue(
        makePage([makeContact({ id: "photo-1", hasPhoto: true, firstName: "Max", lastName: "Mustermann" })]),
      );

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        const rows = screen.getAllByRole("row");
        const dataRow = rows[1];
        const firstCell = dataRow.querySelectorAll("td")[0];
        const img = firstCell.querySelector("img");
        expect(img).toBeInTheDocument();
        expect(img?.getAttribute("alt")).toBe("Max Mustermann");
      });
    });

    it("should show placeholder icon when hasPhoto is false", async () => {
      mockGetContacts.mockResolvedValue(
        makePage([makeContact({ hasPhoto: false })]),
      );

      renderWithProviders(<ContactList />);

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

    it("should show merged name and email columns", async () => {
      mockGetContacts.mockResolvedValue(
        makePage([makeContact({ firstName: "Max", lastName: "Mustermann", email: "max@example.com" })]),
      );

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        const rows = screen.getAllByRole("row");
        const dataRow = rows[1];
        const cells = dataRow.querySelectorAll("td");
        expect(cells[1].textContent).toBe("Max Mustermann");
        expect(cells[2].textContent).toBe("max@example.com");
      });
    });

    it("should have correct column order", async () => {
      mockGetContacts.mockResolvedValue(
        makePage([makeContact()]),
      );

      renderWithProviders(<ContactList />);

      await waitFor(() => {
        const headerRow = screen.getAllByRole("row")[0];
        const headers = headerRow.querySelectorAll("th");
        expect(headers[0].textContent).toBe("");
        expect(headers[1].textContent).toBe(S.columns.name);
        expect(headers[2].textContent).toBe(S.columns.email);
        expect(headers[3].textContent).toBe(S.columns.company);
        expect(headers[4].textContent).toBe(S.columns.actions);
      });
    });
  });

  describe("i18n", () => {
    it("should render German labels by default", async () => {
      mockGetContacts.mockResolvedValue(makePage([makeContact()]));

      renderWithProviders(<ContactList />, { language: "de" });

      await waitFor(() => {
        expect(screen.getByText(de.contacts.title)).toBeInTheDocument();
        expect(screen.getByText(de.contacts.columns.name)).toBeInTheDocument();
        expect(screen.getByText(de.contacts.columns.email)).toBeInTheDocument();
      });
    });

    it("should render English labels when language is en", async () => {
      mockGetContacts.mockResolvedValue(makePage([makeContact()]));

      renderWithProviders(<ContactList />, { language: "en" });

      await waitFor(() => {
        expect(screen.getByText(en.contacts.title)).toBeInTheDocument();
        expect(screen.getByText(en.contacts.columns.name)).toBeInTheDocument();
        expect(screen.getByText(en.contacts.columns.email)).toBeInTheDocument();
      });
    });
  });
});
