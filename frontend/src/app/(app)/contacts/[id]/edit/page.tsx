import { notFound } from "next/navigation";
import { ContactForm } from "@/components/contact-form";
import { getContact } from "@/lib/api";

export const dynamic = "force-dynamic";

interface EditContactPageProps {
  readonly params: Promise<{ id: string }>;
}

export default async function EditContactPage({ params }: EditContactPageProps) {
  const { id } = await params;

  try {
    const contact = await getContact(id);
    return <ContactForm contact={contact} />;
  } catch {
    notFound();
  }
}
