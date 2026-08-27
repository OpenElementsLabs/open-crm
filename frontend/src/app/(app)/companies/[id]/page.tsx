import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { CompanyDetail } from "@/components/company-detail";
import { getCachedCompany } from "@/lib/cached-entities";
import { buildCompanyMetadata } from "@/lib/metadata/entity";

export const dynamic = "force-dynamic";

interface CompanyDetailPageProps {
  readonly params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: CompanyDetailPageProps): Promise<Metadata> {
  const { id } = await params;
  // Never throw here: an exception fails the whole page render, turning a cosmetic metadata problem
  // into an outage. A missing/forbidden/failed fetch yields empty metadata → the root title applies.
  const company = await getCachedCompany(id).catch(() => null);
  return buildCompanyMetadata(company);
}

export default async function CompanyDetailPage({ params }: CompanyDetailPageProps) {
  const { id } = await params;

  try {
    const company = await getCachedCompany(id);
    return <CompanyDetail company={company} />;
  } catch {
    notFound();
  }
}
