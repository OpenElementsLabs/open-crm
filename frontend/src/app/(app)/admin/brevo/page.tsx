import { auth } from "@/auth";
import { ForbiddenPage } from "@/components/forbidden-page";
import { ROLE_IT_ADMIN } from "@/lib/roles";
import { BrevoPageClient } from "./brevo-page-client";

export default async function BrevoPage() {
  const session = await auth();
  if (!session?.roles?.includes(ROLE_IT_ADMIN)) {
    return <ForbiddenPage />;
  }
  return <BrevoPageClient />;
}
