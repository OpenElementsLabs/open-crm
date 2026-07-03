import { describe, it, expect, afterEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { renderWithProviders } from "@/test/test-utils";
import { EnrichmentSettings } from "@/components/enrichment-settings";
import { de as S } from "@/lib/i18n/de";

const mockGet = vi.fn();
const mockUpdate = vi.fn();
const mockDelete = vi.fn();

vi.mock("@/lib/api", () => ({
  getEnrichmentSettings: (...args: unknown[]) => mockGet(...args),
  updateEnrichmentSettings: (...args: unknown[]) => mockUpdate(...args),
  deleteEnrichmentSettings: (...args: unknown[]) => mockDelete(...args),
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("EnrichmentSettings", () => {
  it("shows unconfigured state with an input for both services", async () => {
    mockGet.mockResolvedValue({ configured: false });
    renderWithProviders(<EnrichmentSettings />);

    await waitFor(() => {
      expect(screen.getAllByPlaceholderText(S.enrichment.settings.apiKeyPlaceholder)).toHaveLength(2);
    });
    expect(mockGet).toHaveBeenCalledWith("dropcontact");
    expect(mockGet).toHaveBeenCalledWith("cognism");
  });

  it("stores a key when saving", async () => {
    mockGet.mockResolvedValue({ configured: false });
    mockUpdate.mockResolvedValue({ configured: true });
    renderWithProviders(<EnrichmentSettings />);

    const inputs = await screen.findAllByPlaceholderText(S.enrichment.settings.apiKeyPlaceholder);
    fireEvent.change(inputs[0], { target: { value: "dc-key" } });
    const saveButtons = screen.getAllByText(S.enrichment.settings.save);
    fireEvent.click(saveButtons[0]);

    await waitFor(() => {
      expect(mockUpdate).toHaveBeenCalledWith("dropcontact", "dc-key");
    });
  });

  it("shows the configured badge when a key exists", async () => {
    mockGet.mockResolvedValue({ configured: true });
    renderWithProviders(<EnrichmentSettings />);

    await waitFor(() => {
      expect(screen.getAllByText(S.enrichment.settings.configured)).toHaveLength(2);
    });
    expect(screen.getAllByText(S.enrichment.settings.remove)).toHaveLength(2);
  });
});
