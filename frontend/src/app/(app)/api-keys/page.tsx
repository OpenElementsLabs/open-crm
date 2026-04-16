import { auth } from "@/auth";
import { ApiKeyList } from "@/components/api-key-list";
import { ForbiddenPage } from "@/components/forbidden-page";
import { ROLE_IT_ADMIN } from "@/lib/roles";

export default async function ApiKeysPage() {
  const session = await auth();
  if (!session?.roles?.includes(ROLE_IT_ADMIN)) {
    return <ForbiddenPage />;
  }
  return <ApiKeyList />;
}
