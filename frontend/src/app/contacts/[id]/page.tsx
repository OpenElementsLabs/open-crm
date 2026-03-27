import { notFound } from "next/navigation";
import { ContactDetail } from "@/components/contact-detail";
import { getContact, getCompany } from "@/lib/api";

export const dynamic = "force-dynamic";

interface ContactDetailPageProps {
  readonly params: Promise<{ id: string }>;
}

export default async function ContactDetailPage({ params }: ContactDetailPageProps) {
  const { id } = await params;

  try {
    const contact = await getContact(id);
    let companyDeleted = false;
    if (contact.companyId) {
      try {
        const company = await getCompany(contact.companyId);
        companyDeleted = company.deleted;
      } catch {
        // Company may have been hard-deleted or inaccessible
      }
    }
    return <ContactDetail contact={contact} companyDeleted={companyDeleted} />;
  } catch {
    notFound();
  }
}
