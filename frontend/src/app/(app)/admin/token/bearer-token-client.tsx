"use client";

import { BearerTokenCard } from "@/components/bearer-token-card";
import { useTranslations } from "@/lib/i18n";

export function BearerTokenClient() {
  const t = useTranslations();

  return (
    <div>
      <h1 className="mb-6 font-heading text-2xl font-bold text-oe-dark">
        {t.nav.bearerToken}
      </h1>
      <BearerTokenCard />
    </div>
  );
}
