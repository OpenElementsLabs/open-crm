"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Plus, Trash2, RotateCcw, Archive, Building2, Printer, Pencil, MessageSquarePlus, FileDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import { AddCommentDialog } from "@/components/add-comment-dialog";
import { getCompanies, deleteCompany, restoreCompany, getCompanyLogoUrl, createCompanyComment, getCompanyExportUrl } from "@/lib/api";
import { CsvExportDialog } from "@/components/csv-export-dialog";
import { TagMultiSelect } from "@/components/tag-multi-select";
import type { CompanyDto, Page } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

export function CompanyList() {
  const t = useTranslations();
  const S = t.companies;
  const router = useRouter();
  const searchParams = useSearchParams();
  const [data, setData] = useState<Page<CompanyDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [nameFilter, setNameFilter] = useState("");
  const [brevoFilter, setBrevoFilter] = useState("all");
  const [includeDeleted, setIncludeDeleted] = useState(false);
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
        size: 20,
        name: nameFilter || undefined,
        includeDeleted,
        brevo: brevoFilter === "all" ? undefined : brevoFilter === "true",
        tagIds: tagIds.length > 0 ? tagIds : undefined,
      });
      setData(result);
    } catch {
      console.error("Failed to fetch companies");
    } finally {
      setLoading(false);
    }
  }, [page, nameFilter, brevoFilter, includeDeleted, tagIds]);

  useEffect(() => {
    fetchCompanies();
  }, [fetchCompanies]);

  useEffect(() => {
    setPage(0);
  }, [nameFilter, brevoFilter, includeDeleted, tagIds]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteCompany(deleteTarget.id);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchCompanies();
    } catch (error: unknown) {
      if (error instanceof Error && error.message === "CONFLICT") {
        setDeleteError(S.deleteDialog.errorConflict);
      } else {
        setDeleteError(S.deleteDialog.errorConflict);
      }
    }
  };

  const handleRestore = async (id: string) => {
    try {
      await restoreCompany(id);
      fetchCompanies();
    } catch {
      console.error("Failed to restore company");
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
              if (includeDeleted) params.set("includeDeleted", "true");
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
          />
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => setIncludeDeleted(!includeDeleted)}
          className={includeDeleted ? "border-oe-blue text-oe-blue" : ""}
        >
          <Archive className="mr-2 h-4 w-4" />
          {includeDeleted ? S.hideArchived : S.showArchived}
        </Button>
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
                  <TableHead>{S.columns.comments}</TableHead>
                  <TableHead className="w-[140px] text-right">{S.columns.actions}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((company) => (
                  <TableRow
                    key={company.id}
                    className={`cursor-pointer ${company.deleted ? "opacity-50" : ""}`}
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
                    <TableCell className="text-oe-gray-mid">
                      {company.website ?? "—"}
                    </TableCell>
                    <TableCell>{company.contactCount}</TableCell>
                    <TableCell>{company.commentCount}</TableCell>
                    <TableCell className="text-right">
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
                      <Button
                        variant="ghost"
                        size="icon"
                        title={S.comments.addTitle}
                        onClick={(e) => {
                          e.stopPropagation();
                          setCommentTarget(company);
                        }}
                      >
                        <MessageSquarePlus className="h-4 w-4 text-oe-blue" />
                      </Button>
                      {company.deleted ? (
                        <Button
                          variant="ghost"
                          size="icon"
                          title={S.detail.restore}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleRestore(company.id);
                          }}
                        >
                          <RotateCcw className="h-4 w-4 text-oe-blue" />
                        </Button>
                      ) : (
                        <Button
                          variant="ghost"
                          size="icon"
                          title={S.detail.delete}
                          onClick={(e) => {
                            e.stopPropagation();
                            setDeleteError(null);
                            setDeleteTarget(company);
                          }}
                        >
                          <Trash2 className="h-4 w-4 text-oe-red" />
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          {/* Pagination */}
          <div className="mt-4 flex items-center justify-between">
            <p className="text-sm text-oe-gray-mid">
              {(data.page.totalElements === 1 ? S.pagination.totalOne : S.pagination.totalOther).replace("{count}", String(data.page.totalElements))} · {S.pagination.page
                .replace("{current}", String(data.page.number + 1))
                .replace("{total}", String(data.page.totalPages))}
            </p>
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
      <DeleteConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) {
            setDeleteTarget(null);
            setDeleteError(null);
          }
        }}
        title={S.deleteDialog.title}
        description={S.deleteDialog.description.replace("{name}", deleteTarget?.name ?? "")}
        confirmLabel={S.deleteDialog.confirm}
        cancelLabel={S.deleteDialog.cancel}
        onConfirm={handleDelete}
        error={deleteError}
        errorTitle={S.deleteDialog.errorTitle}
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
              includeDeleted,
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
