import { describe, it, expect } from "vitest";
import { buildOfflineHtml, DEFAULT_OFFLINE_MESSAGES } from "@/lib/pwa/build-offline-html";
import { createManifest } from "@/lib/pwa/create-manifest";
import { renderServiceWorker } from "@/lib/pwa/service-worker-source";

describe("buildOfflineHtml", () => {
  const html = buildOfflineHtml({
    appName: "Open CRM",
    primaryColor: "#5cba9e",
    backgroundColor: "#ffffff",
    logoSvg: "<svg id='brand-mark'></svg>",
  });

  it("includes the default German and English copy", () => {
    expect(html).toContain(DEFAULT_OFFLINE_MESSAGES.de.title);
    expect(html).toContain(DEFAULT_OFFLINE_MESSAGES.en.title);
    expect(html).toContain("Deutsch");
    expect(html).toContain("English");
  });

  it("embeds the branding logo inline", () => {
    expect(html).toContain("<svg id='brand-mark'></svg>");
    expect(html).toContain("#5cba9e");
  });

  it("is self-contained — no external resource requests", () => {
    expect(html).not.toMatch(/https?:\/\//); // no external URLs
    expect(html).not.toMatch(/<script/i); // no scripts
    expect(html).toContain("<style>"); // inline CSS only
  });

  it("applies copy overrides while keeping branding", () => {
    const custom = buildOfflineHtml({
      appName: "X",
      primaryColor: "#000",
      backgroundColor: "#fff",
      logoSvg: "<svg/>",
      messages: { en: { title: "Custom EN", body: "custom body" } },
    });
    expect(custom).toContain("Custom EN");
    // German falls back to the default
    expect(custom).toContain(DEFAULT_OFFLINE_MESSAGES.de.title);
  });
});

describe("createManifest", () => {
  const manifest = createManifest({
    name: "Open CRM",
    shortName: "CRM",
    description: "desc",
    themeColor: "#ffffff",
    backgroundColor: "#ffffff",
    icons: [{ src: "/icons/icon-192.png", sizes: "192x192", type: "image/png", purpose: "any" }],
  });

  it("reflects the app-provided values in a standalone manifest", () => {
    expect(manifest.name).toBe("Open CRM");
    expect(manifest.short_name).toBe("CRM");
    expect(manifest.display).toBe("standalone");
    expect(manifest.start_url).toBe("/");
    expect(manifest.scope).toBe("/");
    expect(manifest.theme_color).toBe("#ffffff");
    expect(manifest.icons?.[0]?.src).toBe("/icons/icon-192.png");
  });
});

describe("renderServiceWorker", () => {
  const source = renderServiceWorker({ cacheVersion: "hash123" });

  it("embeds the cache version and offline url", () => {
    expect(source).toContain("oe-offline-hash123");
    expect(source).toContain('"/offline.html"');
  });

  it("activates immediately and only handles navigation", () => {
    expect(source).toContain("skipWaiting");
    expect(source).toContain("clients.claim");
    expect(source).toContain('mode !== "navigate"');
  });
});
