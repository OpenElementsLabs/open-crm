"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Pencil, Trash2, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import { ContactComments } from "@/components/contact-comments";
import { TagChips } from "@/components/tag-chips";
import { DetailField } from "@/components/detail-field";
import { deleteContact, getContactPhotoUrl } from "@/lib/api";
import type { ContactDto } from "@/lib/types";
import { useTranslations, useLanguage } from "@/lib/i18n/language-context";

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

function languageLabel(language: string | null, t: ReturnType<typeof useTranslations>): string {
  const S = t.contacts.detail;
  switch (language) {
    case "DE":
      return S.languageDE;
    case "EN":
      return S.languageEN;
    default:
      return S.languageUnknown;
  }
}

function formatBirthday(dateStr: string | null, lang: string): string | null {
  if (!dateStr) return null;
  const [year, month, day] = dateStr.split("-");
  if (lang === "de") return `${day}.${month}.${year}`;
  return `${month}/${day}/${year}`;
}

interface ContactDetailProps {
  readonly contact: ContactDto;
}

export function ContactDetail({ contact }: ContactDetailProps) {
  const t = useTranslations();
  const S = t.contacts;
  const { language } = useLanguage();
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
        <div className="flex items-center gap-4">
          {contact.hasPhoto ? (
            <img
              src={getContactPhotoUrl(contact.id)}
              alt={fullName}
              className="h-24 w-24 rounded-full object-cover"
            />
          ) : (
            <User className="h-24 w-24 text-oe-gray-mid" />
          )}
          <div className="flex flex-col">
            <h1 className="font-heading text-2xl font-bold text-oe-dark">{fullName}</h1>
            <div className="h-6">
              {contact.brevo && (
                <span className="inline-block rounded border border-oe-gray-light bg-oe-gray-light/30 px-2 py-0.5 text-xs text-oe-gray-mid">
                  Brevo
                </span>
              )}
            </div>
          </div>
        </div>
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
            <DetailField label={S.detail.email} value={contact.email} copyable mailable />
            <DetailField label={S.detail.position} value={contact.position} />
            <DetailField label={S.detail.gender} value={genderLabel(contact.gender, t)} />
            <DetailField label={S.detail.phone} value={contact.phoneNumber} copyable callable />
            <DetailField label={S.detail.linkedIn} value={contact.linkedInUrl} copyable linkable />
            <DetailField label={S.detail.birthday} value={formatBirthday(contact.birthday, language)} />
            <DetailField label={S.detail.language} value={languageLabel(contact.language, t)} />
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
        </CardContent>
      </Card>

      <TagChips tagIds={contact.tagIds} />

      <Separator className="my-8" />

      <ContactComments contactId={contact.id} totalCount={contact.commentCount} />

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
