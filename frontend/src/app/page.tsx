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

export default async function Home() {
  const healthy = await checkHealth();

  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-oe-white px-4">
      <div className="flex flex-col items-center gap-8">
        <div className="text-center">
          <h1 className="font-heading text-4xl font-bold tracking-tight text-oe-dark">
            {STRINGS.app.title}
          </h1>
          <p className="mt-2 text-oe-gray-mid">{STRINGS.app.description}</p>
        </div>
        <HealthStatus healthy={healthy} />
      </div>
    </main>
  );
}
