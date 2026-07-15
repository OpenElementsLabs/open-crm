import { describe, it, expect, afterEach, vi } from "vitest";
import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { StatusPageClient } from "../status-page-client";
import { renderWithProviders } from "@/test/test-utils";
import { de } from "@/lib/i18n/de";
import { en } from "@/lib/i18n/en";

function jsonResponse(body: unknown): Response {
  return { ok: true, json: () => Promise.resolve(body) } as unknown as Response;
}

function stubFetch(handlers: Record<string, () => Promise<Response>>) {
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL) => {
      const url = typeof input === "string" ? input : input.toString();
      const handler = handlers[url];
      if (!handler) {
        return Promise.reject(new Error(`unexpected fetch: ${url}`));
      }
      return handler();
    }),
  );
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("StatusPageClient", () => {
  it("shows a HEIC capability row on the status page (English) alongside the health row", async () => {
    stubFetch({
      "/api/health": () => Promise.resolve(jsonResponse({ status: "UP" })),
      "/api/admin/capabilities": () => Promise.resolve(jsonResponse({ heicAvailable: true })),
    });

    renderWithProviders(<StatusPageClient />, { language: "en" });

    await waitFor(() =>
      expect(screen.getByText(en.admin.capabilities.heic.label)).toBeInTheDocument(),
    );
    expect(screen.getByText(en.admin.capabilities.heic.available)).toBeInTheDocument();
    // Backend health row still present.
    expect(screen.getByText(en.health.title)).toBeInTheDocument();
  });

  it("shows the Dockerfile hint as a tooltip when HEIC decoding is unavailable", async () => {
    stubFetch({
      "/api/health": () => Promise.resolve(jsonResponse({ status: "UP" })),
      "/api/admin/capabilities": () => Promise.resolve(jsonResponse({ heicAvailable: false })),
    });

    renderWithProviders(<StatusPageClient />, { language: "en" });

    await waitFor(() =>
      expect(screen.getByText(en.admin.capabilities.heic.unavailable)).toBeInTheDocument(),
    );

    const trigger = screen.getByRole("note");
    fireEvent.focus(trigger);
    const tooltip = await screen.findByRole("tooltip");
    expect(tooltip).toHaveTextContent(en.admin.capabilities.heic.hint);
  });

  it("localizes the HEIC row into German", async () => {
    stubFetch({
      "/api/health": () => Promise.resolve(jsonResponse({ status: "UP" })),
      "/api/admin/capabilities": () => Promise.resolve(jsonResponse({ heicAvailable: true })),
    });

    renderWithProviders(<StatusPageClient />, { language: "de" });

    await waitFor(() =>
      expect(screen.getByText(de.admin.capabilities.heic.label)).toBeInTheDocument(),
    );
    expect(screen.getByText(de.admin.capabilities.heic.available)).toBeInTheDocument();
  });

  it("fails safe to unavailable when the capabilities endpoint errors", async () => {
    stubFetch({
      "/api/health": () => Promise.resolve(jsonResponse({ status: "UP" })),
      "/api/admin/capabilities": () => Promise.reject(new Error("network down")),
    });

    renderWithProviders(<StatusPageClient />, { language: "en" });

    await waitFor(() =>
      expect(screen.getByText(en.admin.capabilities.heic.unavailable)).toBeInTheDocument(),
    );
    expect(screen.queryByText(en.admin.capabilities.heic.available)).not.toBeInTheDocument();
  });
});
