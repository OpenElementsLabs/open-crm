"use client";

import { useEffect, useState } from "react";
import { getTranslationSettings } from "@/lib/api";

let cached: boolean | null = null;
let inflight: Promise<boolean> | null = null;

/** Test-only: clears the cached configuration so each test starts fresh. */
export function __resetTranslationConfigCache(): void {
  cached = null;
  inflight = null;
}

/**
 * Returns whether the backend translation feature is configured.
 *
 * The result is fetched once per page load and cached in module scope so all
 * detail pages, comment lists and description fields share a single network
 * call. While the first request is in flight {@code configured} is {@code null};
 * components should hide translate buttons until a definitive answer arrives.
 */
export function useTranslationConfig(): { readonly configured: boolean | null } {
  const [configured, setConfigured] = useState<boolean | null>(cached);

  useEffect(() => {
    if (cached !== null) {
      return;
    }
    if (!inflight) {
      inflight = getTranslationSettings()
        .then((s) => {
          cached = s.configured;
          return s.configured;
        })
        .catch(() => {
          cached = false;
          return false;
        });
    }
    let alive = true;
    inflight.then((value) => {
      if (alive) {
        setConfigured(value);
      }
    });
    return () => {
      alive = false;
    };
  }, []);

  return { configured };
}
