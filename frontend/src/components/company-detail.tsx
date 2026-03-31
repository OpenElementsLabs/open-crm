"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { CheckSquare, Pencil, Trash2, Users, Building2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import { CompanyComments } from "@/components/company-comments";
import { DetailField } from "@/components/detail-field";
import { TagChips } from "@/components/tag-chips";
import { deleteCompany, getCompanyLogoUrl } from "@/lib/api";
import type { CompanyDto } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

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
        <div className="flex items-center gap-4">
          {company.hasLogo ? (
            <img
              src={getCompanyLogoUrl(company.id)}
              alt={company.name}
              className="h-24 w-24 object-contain"
            />
          ) : (
            <Building2 className="h-24 w-24 text-oe-gray-mid" />
          )}
          <div className="flex flex-col">
            <h1 className="font-heading text-2xl font-bold text-oe-dark">{company.name}</h1>
            <div className="h-6">
              {company.brevo && (
                <span className="inline-block rounded border border-oe-gray-light bg-oe-gray-light/30 px-2 py-0.5 text-xs text-oe-gray-mid">
                  Brevo
                </span>
              )}
            </div>
          </div>
        </div>
        <div className="flex gap-2">
          {company.deleted ? (
            <Button variant="outline" disabled className="opacity-50">
              <Users className="mr-2 h-4 w-4" />
              {S.detail.showEmployees} ({company.contactCount})
            </Button>
          ) : (
            <Button asChild variant="outline">
              <Link href={`/contacts?companyId=${company.id}`}>
                <Users className="mr-2 h-4 w-4" />
                {S.detail.showEmployees} ({company.contactCount})
              </Link>
            </Button>
          )}
          <Button asChild variant="outline">
            <Link href={`/companies/${company.id}/edit`}>
              <Pencil className="mr-2 h-4 w-4" />
              {S.detail.edit}
            </Link>
          </Button>
          <Button asChild variant="outline">
            <Link href={`/tasks/new?companyId=${company.id}`}>
              <CheckSquare className="mr-2 h-4 w-4" />
              {S.detail.createTask}
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
            <DetailField label={S.detail.email} value={company.email} copyable mailable />
            <DetailField label={S.detail.phone} value={company.phoneNumber} copyable callable />
            <DetailField label={S.detail.website} value={company.website} copyable linkable />
            {(() => {
              const lines: string[] = [];
              if (company.street) {
                lines.push(company.houseNumber ? `${company.street} ${company.houseNumber}` : company.street);
              }
              const line2Parts = [company.zipCode, company.city].filter(Boolean);
              if (line2Parts.length > 0) {
                lines.push(line2Parts.join(" "));
              }
              if (company.country) {
                lines.push(company.country);
              }
              const addressText = lines.length > 0 ? lines.join("\n") : null;
              return (
                <DetailField label={S.detail.address} value={addressText} copyable multiline />
              );
            })()}
          </dl>
        </CardContent>
      </Card>

      <TagChips tagIds={company.tagIds} />

      {company.description && (
        <div className="mt-4">
          <h3 className="text-sm font-medium text-oe-gray-mid">{S.detail.description}</h3>
          <p className="mt-1 text-sm text-oe-black whitespace-pre-line">{company.description}</p>
        </div>
      )}

      <Separator className="my-8" />

      <CompanyComments companyId={company.id} totalCount={company.commentCount} />

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
