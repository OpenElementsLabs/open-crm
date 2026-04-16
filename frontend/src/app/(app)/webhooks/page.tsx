import { auth } from "@/auth";
import { ForbiddenPage } from "@/components/forbidden-page";
import { ROLE_IT_ADMIN } from "@/lib/roles";
import { WebhookList } from "@/components/webhook-list";

export default async function WebhooksPage() {
  const session = await auth();
  if (!session?.roles?.includes(ROLE_IT_ADMIN)) {
    return <ForbiddenPage />;
  }
  return <WebhookList />;
}
