"use client";

import { BrevoSync } from "@/components/brevo-sync";
import { useTranslations } from "@/lib/i18n/language-context";

export function BrevoPageClient() {
  const t = useTranslations();

  return (
    <div>
      <h1 className="mb-6 font-heading text-2xl font-bold text-oe-dark">
        {t.nav.brevo}
      </h1>
      <BrevoSync />
    </div>
  );
}
