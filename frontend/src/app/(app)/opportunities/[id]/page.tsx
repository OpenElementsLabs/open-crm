import { notFound } from "next/navigation";
import { OpportunityDetail } from "@/components/opportunity-detail";
import { getOpportunity } from "@/lib/api";

export const dynamic = "force-dynamic";

interface OpportunityDetailPageProps {
  readonly params: Promise<{ id: string }>;
}

export default async function OpportunityDetailPage({ params }: OpportunityDetailPageProps) {
  const { id } = await params;

  try {
    const opportunity = await getOpportunity(id);
    return <OpportunityDetail opportunity={opportunity} />;
  } catch {
    notFound();
  }
}
