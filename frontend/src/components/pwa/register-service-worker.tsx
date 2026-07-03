"use client";

import { useEffect } from "react";

/**
 * GENERIC — extraction target for `@open-elements/nextjs-app-layer`.
 *
 * Registers the service worker on mount. Renders nothing. Registration failures are swallowed —
 * the SW is a progressive enhancement (installability + offline page), never required for the app to
 * work.
 */
export interface RegisterServiceWorkerProps {
  /** URL of the service worker; defaults to "/sw.js" (root scope). */
  readonly swUrl?: string;
}

export function RegisterServiceWorker({ swUrl = "/sw.js" }: RegisterServiceWorkerProps) {
  useEffect(() => {
    if (typeof navigator === "undefined" || !("serviceWorker" in navigator)) {
      return;
    }
    navigator.serviceWorker.register(swUrl).catch(() => {
      // Progressive enhancement — ignore registration failures.
    });
  }, [swUrl]);

  return null;
}
