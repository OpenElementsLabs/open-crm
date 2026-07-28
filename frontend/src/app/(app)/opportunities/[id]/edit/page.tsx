import { notFound } from "next/navigation";
import { OpportunityForm } from "@/components/opportunity-form";
import { getOpportunity } from "@/lib/api";

export const dynamic = "force-dynamic";

interface EditOpportunityPageProps {
  readonly params: Promise<{ id: string }>;
}

export default async function EditOpportunityPage({ params }: EditOpportunityPageProps) {
  const { id } = await params;

  try {
    const opportunity = await getOpportunity(id);
    return <OpportunityForm opportunity={opportunity} />;
  } catch {
    notFound();
  }
}
