import { notFound } from "next/navigation";
import { CompanyDetail } from "@/components/company-detail";
import { getCompany } from "@/lib/api";

export const dynamic = "force-dynamic";

interface CompanyDetailPageProps {
  readonly params: Promise<{ id: string }>;
}

export default async function CompanyDetailPage({ params }: CompanyDetailPageProps) {
  const { id } = await params;

  try {
    const company = await getCompany(id);
    return <CompanyDetail company={company} />;
  } catch {
    notFound();
  }
}
