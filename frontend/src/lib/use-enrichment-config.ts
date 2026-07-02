"use client";

import { useEffect, useState } from "react";
import { getEnrichmentSettings } from "@/lib/api";

type KeyedService = "dropcontact" | "cognism";

const cached: Record<KeyedService, boolean | null> = { dropcontact: null, cognism: null };
const inflight: Record<KeyedService, Promise<boolean> | null> = { dropcontact: null, cognism: null };

/** Test-only: clears the cached configuration so each test starts fresh. */
export function __resetEnrichmentConfigCache(): void {
  cached.dropcontact = null;
  cached.cognism = null;
  inflight.dropcontact = null;
  inflight.cognism = null;
}

export interface EnrichmentConfig {
  /** Gravatar is keyless and always available. */
  readonly gravatar: true;
  readonly dropcontact: boolean | null;
  readonly cognism: boolean | null;
}

/**
 * Returns which enrichment services are available. Gravatar is always available (keyless);
 * Dropcontact and Cognism are available only when an IT admin has stored their API key. The
 * configured status is fetched once per page load and cached in module scope. While a request is in
 * flight the value is {@code null}; the menu hides the entry until a definitive answer arrives.
 */
export function useEnrichmentConfig(): EnrichmentConfig {
  const [dropcontact, setDropcontact] = useState<boolean | null>(cached.dropcontact);
  const [cognism, setCognism] = useState<boolean | null>(cached.cognism);

  useEffect(() => {
    let alive = true;
    for (const service of ["dropcontact", "cognism"] as const) {
      const setter = service === "dropcontact" ? setDropcontact : setCognism;
      if (cached[service] !== null) {
        continue;
      }
      if (!inflight[service]) {
        inflight[service] = getEnrichmentSettings(service)
          .then((s) => {
            cached[service] = s.configured;
            return s.configured;
          })
          .catch(() => {
            cached[service] = false;
            return false;
          });
      }
      inflight[service]!.then((value) => {
        if (alive) {
          setter(value);
        }
      });
    }
    return () => {
      alive = false;
    };
  }, []);

  return { gravatar: true, dropcontact, cognism };
}
