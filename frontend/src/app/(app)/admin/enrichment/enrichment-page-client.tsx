"use client";

import { EnrichmentSettings } from "@/components/enrichment-settings";
import { useTranslations } from "@/lib/i18n";

export function EnrichmentPageClient() {
  const t = useTranslations();

  return (
    <div>
      <h1 className="mb-6 font-heading text-2xl font-bold text-oe-dark">
        {t.nav.enrichment}
      </h1>
      <EnrichmentSettings />
    </div>
  );
}
