import { auth } from "@/auth";
import { ForbiddenPage, ROLE_IT_ADMIN } from "@open-elements/nextjs-app-layer";
import { StatusPageClient } from "./status-page-client";

export default async function StatusPage() {
  const session = await auth();
  if (!session?.roles?.includes(ROLE_IT_ADMIN)) {
    return <ForbiddenPage />;
  }
  return <StatusPageClient />;
}
