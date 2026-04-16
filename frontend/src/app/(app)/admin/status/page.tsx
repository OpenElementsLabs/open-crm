import { auth } from "@/auth";
import { ForbiddenPage } from "@/components/forbidden-page";
import { ROLE_IT_ADMIN } from "@/lib/roles";
import { ServerStatusClient } from "./server-status-client";

export default async function ServerStatusPage() {
  const session = await auth();
  if (!session?.roles?.includes(ROLE_IT_ADMIN)) {
    return <ForbiddenPage />;
  }
  return <ServerStatusClient />;
}
