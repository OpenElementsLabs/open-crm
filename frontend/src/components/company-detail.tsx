"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Pencil, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import { CompanyComments } from "@/components/company-comments";
import { deleteCompany } from "@/lib/api";
import type { CompanyDto } from "@/lib/types";
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

export function CompanyDetail({ company }: { readonly company: CompanyDto }) {
  const t = useTranslations();
  const S = t.companies;
  const router = useRouter();
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const handleDelete = async () => {
    try {
      await deleteCompany(company.id);
      router.push("/companies");
    } catch (error: unknown) {
      if (error instanceof Error && error.message === "CONFLICT") {
        setDeleteError(S.deleteDialog.errorConflict);
      } else {
        setDeleteError(S.deleteDialog.errorConflict);
      }
    }
  };

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{company.name}</h1>
        <div className="flex gap-2">
          <Button asChild variant="outline">
            <Link href={`/companies/${company.id}/edit`}>
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
            <DetailField label={S.form.name} value={company.name} />
            <DetailField label={S.detail.email} value={company.email} />
            <DetailField label={S.detail.website} value={company.website} />
            <DetailField label={S.detail.street} value={company.street} />
            <DetailField label={S.detail.houseNumber} value={company.houseNumber} />
            <DetailField label={S.detail.zipCode} value={company.zipCode} />
            <DetailField label={S.detail.city} value={company.city} />
            <DetailField label={S.detail.country} value={company.country} />
          </dl>
        </CardContent>
      </Card>

      <Separator className="my-8" />

      <CompanyComments companyId={company.id} />

      <DeleteConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title={S.deleteDialog.title}
        description={S.deleteDialog.description.replace("{name}", company.name)}
        confirmLabel={S.deleteDialog.confirm}
        cancelLabel={S.deleteDialog.cancel}
        onConfirm={handleDelete}
        error={deleteError}
        errorTitle={S.deleteDialog.errorTitle}
      />
    </div>
  );
}
