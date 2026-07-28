import { Suspense } from "react";
import { OpportunitiesClient } from "./opportunities-client";

export const dynamic = "force-dynamic";

export default function OpportunitiesPage() {
  return (
    <Suspense>
      <OpportunitiesClient />
    </Suspense>
  );
}
