import { notFound } from "next/navigation";
import { ContactDetail } from "@/components/contact-detail";
import { getContact } from "@/lib/api";

export const dynamic = "force-dynamic";

interface ContactDetailPageProps {
  readonly params: Promise<{ id: string }>;
}

export default async function ContactDetailPage({ params }: ContactDetailPageProps) {
  const { id } = await params;

  try {
    const contact = await getContact(id);
    return <ContactDetail contact={contact} />;
  } catch {
    notFound();
  }
}
