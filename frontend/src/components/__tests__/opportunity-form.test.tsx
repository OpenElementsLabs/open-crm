import { describe, it, expect, afterEach, vi, beforeEach } from "vitest";
import { screen, cleanup, waitFor } from "@testing-library/react";
import { OpportunityForm } from "../opportunity-form";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { OpportunityDto, ContactDto, CompanyDto, UserOptionDto, Page } from "@/lib/types";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  usePathname: () => "/opportunities",
  useSearchParams: () => new URLSearchParams(),
}));

const mockGetContacts = vi.fn();
const mockGetCompaniesForSelect = vi.fn();
const mockGetUserOptions = vi.fn();
const mockGetCurrentUser = vi.fn();

vi.mock("@/lib/api", () => ({
  createOpportunity: vi.fn(),
  updateOpportunity: vi.fn(),
  getCompaniesForSelect: (...a: unknown[]) => mockGetCompaniesForSelect(...a),
  getContacts: (...a: unknown[]) => mockGetContacts(...a),
  getUserOptions: (...a: unknown[]) => mockGetUserOptions(...a),
  getCurrentUser: (...a: unknown[]) => mockGetCurrentUser(...a),
  getTags: vi
    .fn()
    .mockResolvedValue({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } }),
}));

const companies: CompanyDto[] = [
  { id: "company-A", name: "Company A" } as CompanyDto,
  { id: "company-B", name: "Company B" } as CompanyDto,
];

function makeContact(id: string, companyId: string | null): ContactDto {
  return {
    id,
    title: null,
    firstName: "First" + id,
    lastName: "Last",
    email: null,
    position: null,
    gender: null,
    socialLinks: [],
    phoneNumber: null,
    description: null,
    companyId,
    companyName: companyId,
    commentCount: 0,
    hasPhoto: false,
    birthday: null,
    brevo: false,
    receivesNewsletter: false,
    language: null,
    tagIds: [],
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  };
}

function contactsPage(items: ContactDto[]): Page<ContactDto> {
  return { content: items, page: { size: 1000, number: 0, totalElements: items.length, totalPages: 1 } };
}

const owner: UserOptionDto = { id: "u1", name: "Test User", avatarUrl: null };

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

beforeEach(() => {
  mockGetCompaniesForSelect.mockResolvedValue(companies);
  mockGetUserOptions.mockResolvedValue([owner]);
  mockGetCurrentUser.mockResolvedValue({
    id: "u1",
    name: "Test User",
    email: null,
    avatarUrl: null,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  });
  mockGetContacts.mockResolvedValue(contactsPage([makeContact("contact-X", "company-B")]));
});

describe("OpportunityForm", () => {
  it("shows the create title and defaults status to open and owner to the current user", async () => {
    renderWithProviders(<OpportunityForm />, { language: "de" });

    expect(screen.getByText(de.opportunities.form.createTitle)).toBeInTheDocument();
    // status select defaults to OPEN -> the translated "offen" label is rendered (trigger + option)
    expect(screen.getAllByText(de.opportunities.status.open).length).toBeGreaterThan(0);
    // owner is pre-selected with the current user once options + current user load
    await waitFor(() => {
      expect(screen.getByText("Test User")).toBeInTheDocument();
    });
  });

  it("shows the non-blocking mismatch warning when the main contact belongs to another company", async () => {
    // Edit mode: opportunity for company-A, but its main contact belongs to company-B
    const opportunity: OpportunityDto = {
      id: "opp-1",
      title: "Deal",
      stage: null,
      status: "OPEN",
      product: null,
      estimatedValue: null,
      companyId: "company-A",
      companyName: "Company A",
      mainContactId: "contact-X",
      mainContactName: "FirstX Last",
      additionalContactIds: [],
      owner: { ...owner, email: null, createdAt: "2026-01-01T00:00:00Z", updatedAt: "2026-01-01T00:00:00Z" },
      tagIds: [],
      commentCount: 0,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    };

    renderWithProviders(<OpportunityForm opportunity={opportunity} />, { language: "de" });

    await waitFor(() => {
      expect(screen.getByText(de.opportunities.form.contactMismatchWarning)).toBeInTheDocument();
    });
  });

  it("does not warn when the main contact belongs to the selected company", async () => {
    mockGetContacts.mockResolvedValue(contactsPage([makeContact("contact-X", "company-A")]));
    const opportunity: OpportunityDto = {
      id: "opp-2",
      title: "Deal",
      stage: null,
      status: "OPEN",
      product: null,
      estimatedValue: null,
      companyId: "company-A",
      companyName: "Company A",
      mainContactId: "contact-X",
      mainContactName: "FirstX Last",
      additionalContactIds: [],
      owner: { ...owner, email: null, createdAt: "2026-01-01T00:00:00Z", updatedAt: "2026-01-01T00:00:00Z" },
      tagIds: [],
      commentCount: 0,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    };

    renderWithProviders(<OpportunityForm opportunity={opportunity} />, { language: "de" });

    await waitFor(() => {
      expect(mockGetContacts).toHaveBeenCalled();
    });
    expect(
      screen.queryByText(de.opportunities.form.contactMismatchWarning),
    ).not.toBeInTheDocument();
  });
});
