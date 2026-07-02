import { describe, it, expect, afterEach, beforeAll, beforeEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { renderWithProviders } from "@/test/test-utils";
import { ContactEnrichButton } from "@/components/contact-enrich";
import { __resetEnrichmentConfigCache } from "@/lib/use-enrichment-config";
import { de as S } from "@/lib/i18n/de";
import type { ContactDto, EnrichmentResultDto } from "@/lib/types";

const mockSearch = vi.fn();
const mockApply = vi.fn();
const mockGetSettings = vi.fn();

vi.mock("@/lib/api", () => ({
  searchEnrichment: (...args: unknown[]) => mockSearch(...args),
  applyEnrichment: (...args: unknown[]) => mockApply(...args),
  getEnrichmentSettings: (...args: unknown[]) => mockGetSettings(...args),
}));

beforeAll(() => {
  // Radix primitives use PointerEvent APIs that jsdom does not implement.
  Element.prototype.hasPointerCapture = vi.fn();
  Element.prototype.setPointerCapture = vi.fn();
  Element.prototype.releasePointerCapture = vi.fn();
  Element.prototype.scrollIntoView = vi.fn();
});

beforeEach(() => {
  __resetEnrichmentConfigCache();
  mockGetSettings.mockResolvedValue({ configured: false });
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

function makeContact(overrides: Partial<ContactDto> = {}): ContactDto {
  return {
    id: "c1",
    title: null,
    firstName: "Max",
    lastName: "Müller",
    email: "max@oe.com",
    position: null,
    gender: null,
    socialLinks: [],
    phoneNumber: null,
    description: null,
    companyId: null,
    companyName: null,
    commentCount: 0,
    hasPhoto: false,
    birthday: null,
    brevo: false,
    receivesNewsletter: false,
    language: null,
    tagIds: [],
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

function openMenu() {
  fireEvent.click(screen.getByText(S.enrichment.button));
}

describe("ContactEnrichButton", () => {
  it("offers only Gravatar when the other services are not configured", async () => {
    renderWithProviders(<ContactEnrichButton contact={makeContact()} onApplied={() => {}} />);
    await waitFor(() => expect(mockGetSettings).toHaveBeenCalled());
    openMenu();

    expect(screen.getByText(S.enrichment.services.gravatar)).toBeInTheDocument();
    expect(screen.queryByText(S.enrichment.services.dropcontact)).not.toBeInTheDocument();
    expect(screen.queryByText(S.enrichment.services.cognism)).not.toBeInTheDocument();
  });

  it("hides Gravatar when the contact has no email", async () => {
    renderWithProviders(
      <ContactEnrichButton contact={makeContact({ email: null })} onApplied={() => {}} />,
    );
    await waitFor(() => expect(mockGetSettings).toHaveBeenCalled());
    openMenu();

    expect(screen.queryByText(S.enrichment.services.gravatar)).not.toBeInTheDocument();
    expect(screen.getByText(S.enrichment.noServices)).toBeInTheDocument();
  });

  it("shows Dropcontact when it is configured", async () => {
    mockGetSettings.mockImplementation((service: string) =>
      Promise.resolve({ configured: service === "dropcontact" }),
    );
    renderWithProviders(<ContactEnrichButton contact={makeContact()} onApplied={() => {}} />);
    await waitFor(() => expect(mockGetSettings).toHaveBeenCalledWith("dropcontact"));
    openMenu();

    await waitFor(() =>
      expect(screen.getByText(S.enrichment.services.dropcontact)).toBeInTheDocument(),
    );
  });

  it("shows the preview and applies a single candidate", async () => {
    const result: EnrichmentResultDto = {
      status: "MATCH",
      candidates: [
        {
          candidateId: "h",
          label: "Max",
          changes: [{ field: "position", currentValue: null, proposedValue: "CTO" }],
          companyResolution: { kind: "NONE", companyId: null, companyName: null },
          nothingToEnrich: false,
          payload: {
            email: null, position: "CTO", phoneNumber: null, socialLinks: null,
            companyName: null, photoBase64: null, photoContentType: null,
          },
        },
      ],
    };
    mockSearch.mockResolvedValue(result);
    mockApply.mockResolvedValue({ contact: makeContact({ position: "CTO" }), gdprNotice: "GDPR reminder" });

    const onApplied = vi.fn();
    renderWithProviders(<ContactEnrichButton contact={makeContact()} onApplied={onApplied} />);
    await waitFor(() => expect(mockGetSettings).toHaveBeenCalled());
    openMenu();
    fireEvent.click(screen.getByText(S.enrichment.services.gravatar));

    await waitFor(() => expect(screen.getByText("CTO")).toBeInTheDocument());
    fireEvent.click(screen.getByText(S.enrichment.dialog.apply));

    await waitFor(() => expect(mockApply).toHaveBeenCalledWith("c1", "gravatar", result.candidates[0].payload, false));
    expect(onApplied).toHaveBeenCalled();
    await waitFor(() => expect(screen.getByText("GDPR reminder")).toBeInTheDocument());
  });

  it("shows the nothing-to-enrich info state", async () => {
    mockSearch.mockResolvedValue({
      status: "MATCH",
      candidates: [
        {
          candidateId: "h",
          label: "Max",
          changes: [],
          companyResolution: { kind: "NONE", companyId: null, companyName: null },
          nothingToEnrich: true,
          payload: {
            email: null, position: null, phoneNumber: null, socialLinks: null,
            companyName: null, photoBase64: null, photoContentType: null,
          },
        },
      ],
    } satisfies EnrichmentResultDto);

    renderWithProviders(<ContactEnrichButton contact={makeContact()} onApplied={() => {}} />);
    await waitFor(() => expect(mockGetSettings).toHaveBeenCalled());
    openMenu();
    fireEvent.click(screen.getByText(S.enrichment.services.gravatar));

    await waitFor(() =>
      expect(screen.getByText(S.enrichment.dialog.nothingToEnrich)).toBeInTheDocument(),
    );
  });

  it("offers the create-company checkbox for a new company", async () => {
    mockSearch.mockResolvedValue({
      status: "MATCH",
      candidates: [
        {
          candidateId: "h",
          label: "Max",
          changes: [{ field: "position", currentValue: null, proposedValue: "CTO" }],
          companyResolution: { kind: "NEW", companyId: null, companyName: "Fresh Co" },
          nothingToEnrich: false,
          payload: {
            email: null, position: "CTO", phoneNumber: null, socialLinks: null,
            companyName: "Fresh Co", photoBase64: null, photoContentType: null,
          },
        },
      ],
    } satisfies EnrichmentResultDto);

    renderWithProviders(<ContactEnrichButton contact={makeContact()} onApplied={() => {}} />);
    await waitFor(() => expect(mockGetSettings).toHaveBeenCalled());
    openMenu();
    fireEvent.click(screen.getByText(S.enrichment.services.gravatar));

    await waitFor(() =>
      expect(screen.getByText(S.enrichment.dialog.createCompany.replace("{name}", "Fresh Co"))).toBeInTheDocument(),
    );
  });
});
