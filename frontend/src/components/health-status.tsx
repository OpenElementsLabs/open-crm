"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { STRINGS } from "@/lib/constants";

interface HealthStatusProps {
  readonly healthy: boolean;
}

export function HealthStatus({ healthy }: HealthStatusProps) {
  const statusText = healthy ? STRINGS.health.statusUp : STRINGS.health.statusDown;

  return (
    <Card className="w-full max-w-md border-oe-gray-light shadow-md">
      <CardHeader>
        <CardTitle className="font-heading text-lg text-oe-dark">{STRINGS.health.title}</CardTitle>
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
