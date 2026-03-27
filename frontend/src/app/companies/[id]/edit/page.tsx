import { notFound } from "next/navigation";
import { CompanyForm } from "@/components/company-form";
import { getCompany } from "@/lib/api";

export const dynamic = "force-dynamic";

interface EditCompanyPageProps {
  readonly params: Promise<{ id: string }>;
}

export default async function EditCompanyPage({ params }: EditCompanyPageProps) {
  const { id } = await params;

  try {
    const company = await getCompany(id);
    return <CompanyForm company={company} />;
  } catch {
    notFound();
  }
}
