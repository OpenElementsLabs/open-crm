"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useSession } from "next-auth/react";
import Link from "next/link";
import { Plus, Trash2, Building2, Printer, Pencil, MessageSquarePlus, CheckSquare, FileDown, Copy, Check, ExternalLink } from "lucide-react";
import { Button, Input, TagMultiSelect, Tooltip, TooltipTrigger, TooltipContent, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, Skeleton } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { CompanyDeleteDialog } from "@/components/company-delete-dialog";
import { AddCommentDialog } from "@/components/add-comment-dialog";
import { getCompanies, deleteCompany, getCompanyLogoUrl, createCompanyComment, getCompanyExportUrl, getTags, ForbiddenError } from "@/lib/api";
import { CsvExportDialog } from "@/components/csv-export-dialog";
import type { CompanyDto, Page } from "@/lib/types";
import { hasRole, ROLE_ADMIN } from "@/lib/roles";

const ACTION_ICON = "h-3.5 w-3.5 text-oe-gray-light hover:text-oe-dark [@media(pointer:coarse)]:text-oe-dark transition-colors";

function WebsiteCell({ value }: { readonly value: string | null }) {
  const [copied, setCopied] = useState(false);
  if (!value) return <TableCell className="text-oe-gray-mid">—</TableCell>;
  return (
    <TableCell className="text-oe-gray-mid">
      <span className="inline-flex items-center gap-1">
        <span>{value}</span>
        <span className="inline-flex gap-0.5 shrink-0">
          <button onClick={(e) => { e.stopPropagation(); navigator.clipboard.writeText(value); setCopied(true); setTimeout(() => setCopied(false), 2000); }}>
            {copied ? <Check className={`${ACTION_ICON} text-oe-green`} /> : <Copy className={ACTION_ICON} />}
          </button>
          <button onClick={(e) => { e.stopPropagation(); window.open(value.startsWith("http") ? value : `https://${value}`, "_blank"); }}>
            <ExternalLink className={ACTION_ICON} />
          </button>
        </span>
      </span>
    </TableCell>
  );
}

function ContactCountCell({ count, companyId }: { readonly count: number; readonly companyId: string }) {
  const router = useRouter();
  return (
    <TableCell>
      <span className="inline-flex items-center gap-1">
        <span>{count}</span>
        {count > 0 && (
          <button onClick={(e) => { e.stopPropagation(); router.push(`/contacts?companyId=${companyId}`); }}>
            <ExternalLink className={ACTION_ICON} />
          </button>
        )}
      </span>
    </TableCell>
  );
}

export function CompanyList() {
  const t = useTranslations();
  const S = t.companies;
  const router = useRouter();
  const searchParams = useSearchParams();
  const { data: session } = useSession();
  const canDelete = hasRole(session, ROLE_ADMIN);
  const [data, setData] = useState<Page<CompanyDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(() => {
    if (typeof window === "undefined") return 20;
    const stored = localStorage.getItem("pageSize.companies");
    const parsed = Number(stored);
    if ([10, 20, 50, 100, 200].includes(parsed)) return parsed;
    localStorage.setItem("pageSize.companies", "20");
    return 20;
  });
  const [nameFilter, setNameFilter] = useState("");
  const [brevoFilter, setBrevoFilter] = useState("all");
  const [tagIds, setTagIds] = useState<string[]>(() => {
    const param = searchParams.get("tagIds");
    return param ? param.split(",") : [];
  });

  const [deleteTarget, setDeleteTarget] = useState<CompanyDto | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [commentTarget, setCommentTarget] = useState<CompanyDto | null>(null);
  const [commentSending, setCommentSending] = useState(false);
  const [csvOpen, setCsvOpen] = useState(false);

  const companyColumns = Object.entries(t.csvExport.companyColumns).map(([key, label]) => ({
    key: key.replace(/([A-Z])/g, "_$1").toUpperCase(),
    label,
  }));

  const fetchCompanies = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getCompanies({
        page,
        size: pageSize,
        name: nameFilter || undefined,
        brevo: brevoFilter === "all" ? undefined : brevoFilter === "true",
        tagIds: tagIds.length > 0 ? tagIds : undefined,
      });
      setData(result);
    } catch {
      console.error("Failed to fetch companies");
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, nameFilter, brevoFilter, tagIds]);

  useEffect(() => {
    fetchCompanies();
  }, [fetchCompanies]);

  useEffect(() => {
    setPage(0);
  }, [nameFilter, brevoFilter, tagIds]);

  const handleDeleteAll = async () => {
    if (!deleteTarget) return;
    try {
      await deleteCompany(deleteTarget.id, true);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchCompanies();
    } catch (e) {
      if (e instanceof ForbiddenError) {
        setDeleteError(t.errors.forbidden.deleteNoPermission);
      } else {
        console.error("Failed to delete company");
      }
    }
  };

  const handleDeleteCompanyOnly = async () => {
    if (!deleteTarget) return;
    try {
      await deleteCompany(deleteTarget.id, false);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchCompanies();
    } catch (e) {
      if (e instanceof ForbiddenError) {
        setDeleteError(t.errors.forbidden.deleteNoPermission);
      } else {
        console.error("Failed to delete company");
      }
    }
  };

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{S.title}</h1>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => {
              const params = new URLSearchParams();
              if (nameFilter) params.set("name", nameFilter);
              if (brevoFilter !== "all") params.set("brevo", brevoFilter);
              if (tagIds.length > 0) params.set("tagIds", tagIds.join(","));
              const query = params.toString();
              window.open(`/companies/print${query ? `?${query}` : ""}`, "_blank");
            }}
          >
            <Printer className="mr-2 h-4 w-4" />
            {t.print.button}
          </Button>
          <Button
            variant="outline"
            disabled={!data || data.page.totalElements === 0}
            onClick={() => setCsvOpen(true)}
          >
            <FileDown className="mr-2 h-4 w-4" />
            {t.csvExport.button}
          </Button>
          <Button asChild className="bg-oe-green hover:bg-oe-green-dark text-white">
            <Link href="/companies/new">
              <Plus className="mr-2 h-4 w-4" />
              {S.newCompany}
            </Link>
          </Button>
        </div>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-col gap-3 sm:flex-row">
        <Input
          placeholder={S.filter.name}
          value={nameFilter}
          onChange={(e) => setNameFilter(e.target.value)}
          className="sm:max-w-[200px]"
        />
        <Select value={brevoFilter} onValueChange={setBrevoFilter}>
          <SelectTrigger className="sm:max-w-[200px]">
            <SelectValue placeholder={t.brevoFilter.label} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t.brevoFilter.all}</SelectItem>
            <SelectItem value="true">{t.brevoFilter.fromBrevo}</SelectItem>
            <SelectItem value="false">{t.brevoFilter.notFromBrevo}</SelectItem>
          </SelectContent>
        </Select>
        <div className="sm:max-w-[250px]">
          <TagMultiSelect
            selectedIds={tagIds}
            onChange={(ids) => {
              setTagIds(ids);
              const params = new URLSearchParams(window.location.search);
              if (ids.length > 0) {
                params.set("tagIds", ids.join(","));
              } else {
                params.delete("tagIds");
              }
              const query = params.toString();
              router.replace(`/companies${query ? `?${query}` : ""}`, { scroll: false });
            }}
            loadTags={async () => { const result = await getTags({ size: 1000 }); return result.content.map(tag => ({ value: tag.id, label: tag.name, color: tag.color })); }}
            translations={{ placeholder: t.tags.label + "...", empty: t.tags.empty }}
          />
        </div>
      </div>

      {/* Loading */}
      {loading && (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && data && data.content.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <p className="mb-4 text-oe-gray-mid">{S.empty}</p>
          <Button asChild className="bg-oe-green hover:bg-oe-green-dark text-white">
            <Link href="/companies/new">
              <Plus className="mr-2 h-4 w-4" />
              {S.newCompany}
            </Link>
          </Button>
        </div>
      )}

      {/* Table */}
      {!loading && data && data.content.length > 0 && (
        <>
          <div className="rounded-md border border-oe-gray-light overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[50px]"></TableHead>
                  <TableHead>{S.columns.name}</TableHead>
                  <TableHead>{S.columns.website}</TableHead>
                  <TableHead>{S.columns.contacts}</TableHead>
                  <TableHead className="w-[140px] text-right">{S.columns.actions}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((company) => (
                  <TableRow
                    key={company.id}
                    className="cursor-pointer"
                    onClick={() => router.push(`/companies/${company.id}`)}
                  >
                    <TableCell>
                      {company.hasLogo ? (
                        <img
                          src={getCompanyLogoUrl(company.id)}
                          alt={company.name}
                          className="h-8 w-8 object-contain"
                        />
                      ) : (
                        <Building2 className="h-8 w-8 text-oe-gray-mid" />
                      )}
                    </TableCell>
                    <TableCell className="font-medium">{company.name}</TableCell>
                    <WebsiteCell value={company.website} />
                    <ContactCountCell count={company.contactCount} companyId={company.id} />
                    <TableCell className="text-right">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            title={S.detail.edit}
                            onClick={(e) => {
                              e.stopPropagation();
                              router.push(`/companies/${company.id}/edit`);
                            }}
                          >
                            <Pencil className="h-4 w-4 text-oe-blue" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>{S.detail.edit}</TooltipContent>
                      </Tooltip>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={(e) => {
                              e.stopPropagation();
                              setCommentTarget(company);
                            }}
                          >
                            <MessageSquarePlus className="h-4 w-4 text-oe-blue" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>{S.comments.addTitle}</TooltipContent>
                      </Tooltip>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={(e) => {
                              e.stopPropagation();
                              router.push(`/tasks/new?companyId=${company.id}`);
                            }}
                          >
                            <CheckSquare className="h-4 w-4 text-oe-blue" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>{S.detail.createTask}</TooltipContent>
                      </Tooltip>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <span>
                            <Button
                              variant="ghost"
                              size="icon"
                              title={S.detail.delete}
                              disabled={!canDelete}
                              onClick={(e) => {
                                e.stopPropagation();
                                setDeleteError(null);
                                setDeleteTarget(company);
                              }}
                            >
                              <Trash2 className="h-4 w-4 text-oe-red" />
                            </Button>
                          </span>
                        </TooltipTrigger>
                        <TooltipContent>{canDelete ? S.detail.delete : t.errors.roleRequired.admin}</TooltipContent>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          {/* Pagination */}
          <div className="mt-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Select value={String(pageSize)} onValueChange={(v) => { const n = Number(v); setPageSize(n); localStorage.setItem("pageSize.companies", v); setPage(0); }}>
                <SelectTrigger className="w-[80px] h-8 text-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {[10, 20, 50, 100, 200].map((s) => (
                    <SelectItem key={s} value={String(s)}>{s}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <span className="text-sm text-oe-gray-mid">{S.pagination.perPage}</span>
              <span className="text-sm text-oe-gray-mid">·</span>
              <span className="text-sm text-oe-gray-mid">
                {(data.page.totalElements === 1 ? S.pagination.totalOne : S.pagination.totalOther).replace("{count}", String(data.page.totalElements))} · {S.pagination.page
                  .replace("{current}", String(data.page.number + 1))
                  .replace("{total}", String(data.page.totalPages))}
              </span>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={data.page.number === 0}
                onClick={() => setPage(page - 1)}
              >
                {S.pagination.previous}
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={data.page.number >= data.page.totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                {S.pagination.next}
              </Button>
            </div>
          </div>
        </>
      )}

      {/* Delete dialog */}
      <CompanyDeleteDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) {
            setDeleteTarget(null);
            setDeleteError(null);
          }
        }}
        companyName={deleteTarget?.name ?? ""}
        onDeleteAll={handleDeleteAll}
        onDeleteCompanyOnly={handleDeleteCompanyOnly}
        title={S.deleteDialog.title}
        descriptionAll={S.deleteDialog.descriptionAll}
        descriptionOnly={S.deleteDialog.descriptionOnly}
        deleteAllLabel={S.deleteDialog.deleteAll}
        deleteOnlyLabel={S.deleteDialog.deleteOnly}
        cancelLabel={S.deleteDialog.cancel}
        error={deleteError}
      />

      <AddCommentDialog
        open={commentTarget !== null}
        onOpenChange={(open) => { if (!open) setCommentTarget(null); }}
        onSubmit={async (text) => {
          setCommentSending(true);
          try {
            await createCompanyComment(commentTarget!.id, { text });
            setCommentTarget(null);
          } finally {
            setCommentSending(false);
          }
        }}
        sending={commentSending}
        title={S.comments.addTitle}
        placeholder={S.comments.placeholder}
        sendLabel={S.comments.send}
        sendingLabel={S.comments.sending}
        errorTitle={S.comments.errorTitle}
        errorMessage={S.comments.errorGeneric}
      />

      <CsvExportDialog
        open={csvOpen}
        onOpenChange={setCsvOpen}
        columns={companyColumns}
        onDownload={(columns) => {
          const url = getCompanyExportUrl(
            {
              name: nameFilter || undefined,
              brevo: brevoFilter === "all" ? undefined : brevoFilter === "true",
              tagIds: tagIds.length > 0 ? tagIds : undefined,
            },
            columns,
          );
          window.open(url, "_blank");
        }}
      />
    </div>
  );
}
