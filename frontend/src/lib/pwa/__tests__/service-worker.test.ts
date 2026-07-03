import { describe, it, expect, vi } from "vitest";
import { renderServiceWorker } from "@/lib/pwa/service-worker-source";

const CACHE_VERSION = "abc123def456";
const CACHE_NAME = `oe-offline-${CACHE_VERSION}`;
const OFFLINE_RESPONSE = { body: "offline" };
const NETWORK_RESPONSE = { body: "network" };

/**
 * Loads the rendered service-worker source into a mock worker environment and returns the captured
 * event handlers plus the spies, so the real emitted SW logic is exercised (not a re-implementation).
 */
function loadServiceWorker(fetchImpl: (req: unknown) => Promise<unknown>) {
  const handlers: Record<string, (event: unknown) => void> = {};
  const cacheStore = { add: vi.fn().mockResolvedValue(undefined) };
  const caches = {
    open: vi.fn().mockResolvedValue(cacheStore),
    keys: vi.fn().mockResolvedValue([]),
    delete: vi.fn().mockResolvedValue(true),
    match: vi.fn().mockResolvedValue(OFFLINE_RESPONSE),
  };
  const self = {
    addEventListener: (type: string, handler: (event: unknown) => void) => {
      handlers[type] = handler;
    },
    skipWaiting: vi.fn().mockResolvedValue(undefined),
    clients: { claim: vi.fn().mockResolvedValue(undefined) },
  };
  const source = renderServiceWorker({ cacheVersion: CACHE_VERSION });
  new Function("self", "caches", "fetch", source)(self, caches, fetchImpl);
  return { handlers, caches, cacheStore, self };
}

describe("service worker", () => {
  it("serves the precached offline page for a navigation request when offline", async () => {
    const fetchImpl = vi.fn().mockRejectedValue(new Error("offline"));
    const { handlers, caches } = loadServiceWorker(fetchImpl);
    const respondWith = vi.fn();
    handlers.fetch({ request: { mode: "navigate" }, respondWith });

    expect(respondWith).toHaveBeenCalledTimes(1);
    await expect(respondWith.mock.calls[0][0]).resolves.toBe(OFFLINE_RESPONSE);
    expect(caches.match).toHaveBeenCalledWith("/offline.html");
  });

  it("returns the network response for a navigation request when online and caches nothing", async () => {
    const fetchImpl = vi.fn().mockResolvedValue(NETWORK_RESPONSE);
    const { handlers, caches } = loadServiceWorker(fetchImpl);
    const respondWith = vi.fn();
    handlers.fetch({ request: { mode: "navigate" }, respondWith });

    await expect(respondWith.mock.calls[0][0]).resolves.toBe(NETWORK_RESPONSE);
    expect(caches.match).not.toHaveBeenCalled();
    expect(caches.open).not.toHaveBeenCalled();
  });

  it("does not intercept non-navigation requests", () => {
    const fetchImpl = vi.fn();
    const { handlers } = loadServiceWorker(fetchImpl);
    const respondWith = vi.fn();
    handlers.fetch({ request: { mode: "cors" }, respondWith });

    expect(respondWith).not.toHaveBeenCalled();
    expect(fetchImpl).not.toHaveBeenCalled();
  });

  it("precaches the offline page and skips waiting on install", async () => {
    const { handlers, caches, cacheStore, self } = loadServiceWorker(vi.fn());
    let waited: Promise<unknown> | undefined;
    handlers.install({ waitUntil: (p: Promise<unknown>) => { waited = p; } });
    await waited;

    expect(caches.open).toHaveBeenCalledWith(CACHE_NAME);
    expect(cacheStore.add).toHaveBeenCalledWith("/offline.html");
    expect(self.skipWaiting).toHaveBeenCalled();
  });

  it("deletes stale caches and claims clients on activate", async () => {
    const { handlers, caches, self } = loadServiceWorker(vi.fn());
    caches.keys.mockResolvedValue(["oe-offline-old", CACHE_NAME]);
    let waited: Promise<unknown> | undefined;
    handlers.activate({ waitUntil: (p: Promise<unknown>) => { waited = p; } });
    await waited;

    expect(caches.delete).toHaveBeenCalledWith("oe-offline-old");
    expect(caches.delete).not.toHaveBeenCalledWith(CACHE_NAME);
    expect(self.clients.claim).toHaveBeenCalled();
  });
});
