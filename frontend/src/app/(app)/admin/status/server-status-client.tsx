"use client";

import { useEffect, useState } from "react";
import { HealthStatus } from "@/components/health-status";
import { useTranslations } from "@/lib/i18n";

export function ServerStatusClient() {
  const t = useTranslations();
  const [healthy, setHealthy] = useState<boolean | null>(null);

  useEffect(() => {
    fetch("/api/health")
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => setHealthy(data?.status === "UP"))
      .catch(() => setHealthy(false));
  }, []);

  return (
    <div>
      <h1 className="mb-6 font-heading text-2xl font-bold text-oe-dark">
        {t.nav.serverStatus}
      </h1>
      {healthy !== null && <HealthStatus healthy={healthy} />}
    </div>
  );
}
