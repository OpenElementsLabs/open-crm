import { auth } from "@/auth";
import { ForbiddenPage, ROLE_IT_ADMIN } from "@open-elements/nextjs-app-layer";
import { EnrichmentPageClient } from "./enrichment-page-client";

export default async function EnrichmentPage() {
  const session = await auth();
  if (!session?.roles?.includes(ROLE_IT_ADMIN)) {
    return <ForbiddenPage />;
  }
  return <EnrichmentPageClient />;
}
