import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ContactDetail } from "@/components/contact-detail";
import { getCachedContact } from "@/lib/cached-entities";
import { buildContactMetadata } from "@/lib/metadata/entity";

export const dynamic = "force-dynamic";

interface ContactDetailPageProps {
  readonly params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: ContactDetailPageProps): Promise<Metadata> {
  const { id } = await params;
  // Never throw here: an exception fails the whole page render, turning a cosmetic metadata problem
  // into an outage. A missing/forbidden/failed fetch yields empty metadata → the root title applies.
  const contact = await getCachedContact(id).catch(() => null);
  return buildContactMetadata(contact);
}

export default async function ContactDetailPage({ params }: ContactDetailPageProps) {
  const { id } = await params;

  try {
    const contact = await getCachedContact(id);
    return <ContactDetail contact={contact} />;
  } catch {
    notFound();
  }
}
