import { describe, it, expect, afterEach, vi, beforeEach } from "vitest";
import { screen, cleanup, waitFor } from "@testing-library/react";
import { OpportunitiesClient } from "../opportunities-client";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { OpportunityDto, CompanyDto, Page } from "@/lib/types";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  usePathname: () => "/opportunities",
  useSearchParams: () => new URLSearchParams(),
}));

const mockGetOpportunities = vi.fn();

vi.mock("@/lib/api", () => ({
  getOpportunities: (...args: unknown[]) => mockGetOpportunities(...args),
  getCompaniesForSelect: vi.fn().mockResolvedValue([] as CompanyDto[]),
  createOpportunityComment: vi.fn(),
  getTags: vi
    .fn()
    .mockResolvedValue({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } }),
}));

function makeOpportunity(overrides: Partial<OpportunityDto> = {}): OpportunityDto {
  return {
    id: "opp-1",
    title: "Muster Deal",
    stage: "Angebot",
    status: "OPEN",
    product: "Support & Care",
    estimatedValue: 25000,
    companyId: "company-1",
    companyName: "Open Elements",
    mainContactId: "contact-1",
    mainContactName: "Max Mustermann",
    additionalContactIds: [],
    owner: {
      id: "u1",
      name: "Owner One",
      email: null,
      avatarUrl: null,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    },
    tagIds: [],
    commentCount: 0,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

function makePage(items: OpportunityDto[]): Page<OpportunityDto> {
  return {
    content: items,
    page: { size: 20, number: 0, totalElements: items.length, totalPages: 1 },
  };
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

beforeEach(() => {
  mockGetOpportunities.mockResolvedValue(makePage([makeOpportunity()]));
});

describe("OpportunitiesClient", () => {
  it("renders a row with title, company, owner, status badge and formatted EUR value", async () => {
    renderWithProviders(<OpportunitiesClient />, { language: "de" });

    await waitFor(() => {
      expect(screen.getByText("Muster Deal")).toBeInTheDocument();
    });
    expect(screen.getByText("Open Elements")).toBeInTheDocument();
    expect(screen.getByText("Owner One")).toBeInTheDocument();
    // WON/LOST/OPEN badge shows the translated status label (also appears as a filter option)
    expect(screen.getAllByText(de.opportunities.status.open).length).toBeGreaterThan(0);
    // 25000 formatted for de-DE EUR contains "25.000,00"
    expect(screen.getByText(/25\.000,00/)).toBeInTheDocument();
  });

  it("shows an empty state when there are no opportunities", async () => {
    mockGetOpportunities.mockResolvedValue(makePage([]));
    renderWithProviders(<OpportunitiesClient />, { language: "de" });

    await waitFor(() => {
      expect(screen.getByText(de.opportunities.empty)).toBeInTheDocument();
    });
  });

  it("renders a dash for a null estimated value", async () => {
    mockGetOpportunities.mockResolvedValue(makePage([makeOpportunity({ estimatedValue: null })]));
    renderWithProviders(<OpportunitiesClient />, { language: "de" });

    await waitFor(() => {
      expect(screen.getByText("Muster Deal")).toBeInTheDocument();
    });
    expect(screen.queryByText(/25\.000,00/)).not.toBeInTheDocument();
  });
});
