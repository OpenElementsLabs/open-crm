"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import Link from "next/link";
import { Pencil, Trash2, User } from "lucide-react";
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  DeleteConfirmDialog,
  DetailField,
  Separator,
  Tooltip,
  TooltipContent,
  TooltipTrigger,
  TagChips,
} from "@open-elements/ui";
import type { TagDto } from "@open-elements/ui";
import { useTranslations, useLanguage } from "@/lib/i18n";
import { OpportunityComments } from "@/components/opportunity-comments";
import { deleteOpportunity, ForbiddenError, getTag, getContact } from "@/lib/api";
import type { OpportunityDto, ContactDto } from "@/lib/types";
import { hasAppAdmin } from "@/lib/roles";
import { formatEur } from "@/lib/opportunity-format";
import { OpportunityStatusBadge, statusLabel } from "@/components/opportunity-status-badge";

function formatTimestamp(dateString: string, language: string): string {
  return new Date(dateString).toLocaleString(language === "de" ? "de-DE" : "en-US", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface OpportunityDetailProps {
  readonly opportunity: OpportunityDto;
}

export function OpportunityDetail({ opportunity }: OpportunityDetailProps) {
  const t = useTranslations();
  const S = t.opportunities;
  const { language } = useLanguage();
  const router = useRouter();
  const { data: session } = useSession();
  const canDelete = hasAppAdmin(session);

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [tags, setTags] = useState<TagDto[]>([]);
  const [additionalContacts, setAdditionalContacts] = useState<ContactDto[]>([]);

  useEffect(() => {
    if (opportunity.tagIds.length === 0) {
      setTags([]);
      return;
    }
    Promise.all(opportunity.tagIds.map((id) => getTag(id).catch(() => null))).then((results) =>
      setTags(results.filter((r): r is TagDto => r !== null)),
    );
  }, [opportunity.tagIds]);

  useEffect(() => {
    if (opportunity.additionalContactIds.length === 0) {
      setAdditionalContacts([]);
      return;
    }
    Promise.all(
      opportunity.additionalContactIds.map((id) => getContact(id).catch(() => null)),
    ).then((results) => setAdditionalContacts(results.filter((r): r is ContactDto => r !== null)));
  }, [opportunity.additionalContactIds]);

  const handleDelete = async () => {
    try {
      await deleteOpportunity(opportunity.id);
      router.push("/opportunities");
    } catch (e) {
      if (e instanceof ForbiddenError) {
        setDeleteError(t.errors.forbidden.deleteNoPermission);
      } else {
        setDeleteError(S.form.errorGeneric);
      }
    }
  };

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-col gap-2">
          <div className="flex items-center gap-3">
            <h1 className="font-heading text-2xl font-bold text-oe-dark">{opportunity.title}</h1>
            <OpportunityStatusBadge
              status={opportunity.status}
              label={statusLabel(opportunity.status, S.status)}
            />
          </div>
          <TagChips tags={tags} label={t.tags.label} />
        </div>
        <div className="flex gap-2">
          <Button asChild variant="outline">
            <Link href={`/opportunities/${opportunity.id}/edit`}>
              <Pencil className="mr-2 h-4 w-4" />
              {S.detail.edit}
            </Link>
          </Button>
          <Tooltip>
            <TooltipTrigger asChild>
              <span>
                <Button
                  variant="outline"
                  className="border-oe-red text-oe-red hover:bg-oe-red-lighter"
                  disabled={!canDelete}
                  onClick={() => {
                    setDeleteError(null);
                    setDeleteOpen(true);
                  }}
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  {S.detail.delete}
                </Button>
              </span>
            </TooltipTrigger>
            {!canDelete && <TooltipContent>{t.errors.roleRequired.admin}</TooltipContent>}
          </Tooltip>
        </div>
      </div>

      <Card className="border-oe-gray-light">
        <CardHeader>
          <CardTitle className="font-heading text-lg text-oe-dark">{S.detail.title}</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-4 sm:grid-cols-2">
            <DetailField label={S.detail.stage} value={opportunity.stage} />
            <DetailField label={S.detail.product} value={opportunity.product} />
            <DetailField
              label={S.detail.value}
              value={formatEur(opportunity.estimatedValue, language)}
            />
            <div>
              <dt className="text-sm font-medium text-oe-gray-mid">{S.detail.company}</dt>
              <dd className="mt-1 text-sm text-oe-black">
                {opportunity.companyId && opportunity.companyName ? (
                  <Link
                    href={`/companies/${opportunity.companyId}`}
                    className="text-oe-green underline hover:text-oe-green-dark"
                  >
                    {opportunity.companyName}
                    <span className="ml-1 text-xs no-underline">({S.detail.showCompany})</span>
                  </Link>
                ) : (
                  "—"
                )}
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-oe-gray-mid">{S.detail.mainContact}</dt>
              <dd className="mt-1 text-sm text-oe-black">
                {opportunity.mainContactId && opportunity.mainContactName ? (
                  <Link
                    href={`/contacts/${opportunity.mainContactId}`}
                    className="text-oe-green underline hover:text-oe-green-dark"
                  >
                    {opportunity.mainContactName}
                  </Link>
                ) : (
                  "—"
                )}
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-oe-gray-mid">{S.detail.additionalContacts}</dt>
              <dd className="mt-1 text-sm text-oe-black">
                {additionalContacts.length === 0 ? (
                  "—"
                ) : (
                  <ul className="flex flex-col gap-1">
                    {additionalContacts.map((c) => (
                      <li key={c.id}>
                        <Link
                          href={`/contacts/${c.id}`}
                          className="text-oe-green underline hover:text-oe-green-dark"
                        >
                          {`${c.title ? c.title + " " : ""}${c.firstName} ${c.lastName}`.trim()}
                        </Link>
                      </li>
                    ))}
                  </ul>
                )}
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-oe-gray-mid">{S.detail.owner}</dt>
              <dd className="mt-1 text-sm text-oe-black">
                <span className="inline-flex items-center gap-2">
                  {opportunity.owner.avatarUrl ? (
                    <img
                      src={opportunity.owner.avatarUrl}
                      alt={opportunity.owner.name}
                      className="h-6 w-6 rounded-full object-cover"
                    />
                  ) : (
                    <User className="h-6 w-6 text-oe-gray-mid" />
                  )}
                  <span>{opportunity.owner.name}</span>
                </span>
              </dd>
            </div>
            <DetailField
              label={S.detail.createdAt}
              value={formatTimestamp(opportunity.createdAt, language)}
            />
            <DetailField
              label={S.detail.updatedAt}
              value={formatTimestamp(opportunity.updatedAt, language)}
            />
          </dl>
        </CardContent>
      </Card>

      <Separator className="my-8" />

      <OpportunityComments opportunityId={opportunity.id} totalCount={opportunity.commentCount} />

      <DeleteConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title={S.deleteDialog.title}
        description={S.deleteDialog.description.replace("{name}", opportunity.title)}
        confirmLabel={S.deleteDialog.confirm}
        cancelLabel={S.deleteDialog.cancel}
        onConfirm={handleDelete}
        error={deleteError}
      />
    </div>
  );
}
