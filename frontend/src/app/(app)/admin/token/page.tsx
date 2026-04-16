import { auth } from "@/auth";
import { ForbiddenPage } from "@/components/forbidden-page";
import { ROLE_IT_ADMIN } from "@/lib/roles";
import { BearerTokenClient } from "./bearer-token-client";

export default async function BearerTokenPage() {
  const session = await auth();
  if (!session?.roles?.includes(ROLE_IT_ADMIN)) {
    return <ForbiddenPage />;
  }
  return <BearerTokenClient />;
}
