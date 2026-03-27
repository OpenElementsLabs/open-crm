import { HealthStatus } from "@/components/health-status";
import { STRINGS } from "@/lib/constants";

export const dynamic = "force-dynamic";

async function checkHealth(): Promise<boolean> {
  try {
    const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";
    const response = await fetch(`${backendUrl}/api/health`, {
      cache: "no-store",
    });
    if (!response.ok) {
      return false;
    }
    const data = await response.json();
    return data.status === "UP";
  } catch {
    return false;
  }
}

export default async function HealthPage() {
  const healthy = await checkHealth();

  return (
    <div className="flex flex-col items-center justify-center py-20">
      <div className="flex flex-col items-center gap-8">
        <div className="text-center">
          <h1 className="font-heading text-3xl font-bold tracking-tight text-oe-dark">
            {STRINGS.health.title}
          </h1>
        </div>
        <HealthStatus healthy={healthy} />
      </div>
    </div>
  );
}
