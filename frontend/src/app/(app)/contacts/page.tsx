import { Suspense } from "react";
import { ContactList } from "@/components/contact-list";

export const dynamic = "force-dynamic";

export default function ContactsPage() {
  return (
    <Suspense>
      <ContactList />
    </Suspense>
  );
}
