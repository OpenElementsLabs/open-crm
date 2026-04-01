"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useTranslations } from "@/lib/i18n/language-context";

interface HealthStatusProps {
  readonly healthy: boolean;
}

export function HealthStatus({ healthy }: HealthStatusProps) {
  const t = useTranslations();
  const statusText = healthy ? t.health.statusUp : t.health.statusDown;

  return (
    <Card className="border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-lg text-oe-dark">{t.health.title}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-center gap-3">
          <span
            className={`inline-block h-4 w-4 rounded-full ${healthy ? "bg-oe-green" : "bg-oe-red"}`}
            aria-label={statusText}
          />
          <span className="text-base font-medium">{statusText}</span>
        </div>
      </CardContent>
    </Card>
  );
}
