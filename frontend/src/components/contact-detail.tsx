"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Pencil, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import { ContactComments } from "@/components/contact-comments";
import { deleteContact } from "@/lib/api";
import type { ContactDto } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

function DetailField({
  label,
  value,
}: {
  readonly label: string;
  readonly value: string | null;
}) {
  return (
    <div>
      <dt className="text-sm font-medium text-oe-gray-mid">{label}</dt>
      <dd className="mt-1 text-sm text-oe-black">{value || "—"}</dd>
    </div>
  );
}

function CheckboxField({
  label,
  checked,
}: {
  readonly label: string;
  readonly checked: boolean;
}) {
  return (
    <div className="flex items-center gap-2">
      <input type="checkbox" checked={checked} disabled className="h-4 w-4" />
      <span className="text-sm text-oe-gray-mid">{label}</span>
    </div>
  );
}

function genderLabel(gender: string | null, t: ReturnType<typeof useTranslations>): string | null {
  if (!gender) return null;
  const S = t.contacts.form;
  switch (gender) {
    case "MALE":
      return S.male;
    case "FEMALE":
      return S.female;
    case "DIVERSE":
      return S.diverse;
    default:
      return gender;
  }
}

interface ContactDetailProps {
  readonly contact: ContactDto;
}

export function ContactDetail({ contact }: ContactDetailProps) {
  const t = useTranslations();
  const S = t.contacts;
  const router = useRouter();
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const handleDelete = async () => {
    try {
      await deleteContact(contact.id);
      router.push("/contacts");
    } catch {
      setDeleteError(S.form.errorGeneric);
    }
  };

  const fullName = `${contact.firstName} ${contact.lastName}`;

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{fullName}</h1>
        <div className="flex gap-2">
          <Button asChild variant="outline">
            <Link href={`/contacts/${contact.id}/edit`}>
              <Pencil className="mr-2 h-4 w-4" />
              {S.detail.edit}
            </Link>
          </Button>
          <Button
            variant="outline"
            className="text-oe-red border-oe-red hover:bg-oe-red-lighter"
            onClick={() => {
              setDeleteError(null);
              setDeleteOpen(true);
            }}
          >
            <Trash2 className="mr-2 h-4 w-4" />
            {S.detail.delete}
          </Button>
        </div>
      </div>

      <Card className="border-oe-gray-light">
        <CardHeader>
          <CardTitle className="font-heading text-lg text-oe-dark">{S.detail.title}</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-4 sm:grid-cols-2">
            <DetailField label={S.detail.firstName} value={contact.firstName} />
            <DetailField label={S.detail.lastName} value={contact.lastName} />
            <DetailField label={S.detail.email} value={contact.email} />
            <DetailField label={S.detail.position} value={contact.position} />
            <DetailField label={S.detail.gender} value={genderLabel(contact.gender, t)} />
            <DetailField label={S.detail.phone} value={contact.phoneNumber} />
            <DetailField label={S.detail.linkedIn} value={contact.linkedInUrl} />
            <DetailField label={S.detail.language} value={contact.language} />
            <div>
              <dt className="text-sm font-medium text-oe-gray-mid">{S.detail.company}</dt>
              <dd className="mt-1 text-sm text-oe-black">
                {contact.companyId && contact.companyName ? (
                  contact.companyDeleted ? (
                    <span>
                      {contact.companyName}
                      <span className="ml-2 inline-block rounded bg-oe-gray-light px-2 py-0.5 text-xs text-oe-gray-mid">
                        {S.detail.archivedBadge}
                      </span>
                    </span>
                  ) : (
                    <Link
                      href={`/companies/${contact.companyId}`}
                      className="text-oe-green hover:text-oe-green-dark underline"
                    >
                      {contact.companyName}
                      <span className="ml-1 text-xs no-underline">({S.detail.showCompany})</span>
                    </Link>
                  )
                ) : (
                  "—"
                )}
              </dd>
            </div>
          </dl>

          <div className="mt-6 flex gap-6">
            <CheckboxField label={S.detail.syncedToBrevo} checked={contact.syncedToBrevo} />
            <CheckboxField label={S.detail.doubleOptIn} checked={contact.doubleOptIn} />
          </div>
        </CardContent>
      </Card>

      <Separator className="my-8" />

      <ContactComments contactId={contact.id} />

      <DeleteConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title={S.deleteDialog.title}
        description={S.deleteDialog.description.replace("{name}", fullName)}
        confirmLabel={S.deleteDialog.confirm}
        cancelLabel={S.deleteDialog.cancel}
        onConfirm={handleDelete}
        error={deleteError}
      />
    </div>
  );
}
