"use client";

import { ServerStatusClient } from "@open-elements/nextjs-app-layer";
import { useTranslations } from "@/lib/i18n";

/**
 * Client wrapper around the shared {@link ServerStatusClient} that supplies the app-specific
 * runtime-capability rows. Localised strings are resolved here (client-side) because the app's
 * i18n is context-based; the library stays app-agnostic and only receives serialisable strings.
 */
export function StatusPageClient() {
  const t = useTranslations();

  return (
    <ServerStatusClient
      capabilities={{
        endpoint: "/api/admin/capabilities",
        items: [
          {
            id: "heicAvailable",
            label: t.admin.capabilities.heic.label,
            availableText: t.admin.capabilities.heic.available,
            unavailableText: t.admin.capabilities.heic.unavailable,
            hint: t.admin.capabilities.heic.hint,
          },
        ],
      }}
    />
  );
}
