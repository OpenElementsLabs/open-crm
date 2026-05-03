"use client";

import Link from "next/link";
import { ShieldAlert } from "lucide-react";
import { useTranslations } from "@/lib/i18n";
import { Button } from "@open-elements/ui";

export function ForbiddenPage() {
  const t = useTranslations();
  const S = t.errors.forbidden;
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 text-center">
      <ShieldAlert className="h-16 w-16 text-oe-red" />
      <h1 className="font-heading text-3xl font-bold text-oe-dark">{S.title}</h1>
      <p className="max-w-md text-sm text-oe-gray-mid">{S.description}</p>
      <Button asChild className="mt-2">
        <Link href="/companies">{S.backToHome}</Link>
      </Button>
    </div>
  );
}
