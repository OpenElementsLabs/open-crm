"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "next-auth/react";
import { ArrowUpRight, Pencil, Plus, Tag, Trash2 } from "lucide-react";
import { Button, Input, Tooltip, TooltipTrigger, TooltipContent, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Skeleton } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import type { TagDto } from "@open-elements/ui";
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import { getTags, deleteTag, ForbiddenError } from "@/lib/api";
import { hasRole, ROLE_ADMIN } from "@/lib/roles";
import type { Page } from "@/lib/types";

export function TagList() {
  const t = useTranslations();
  const { data: session } = useSession();
  const canDelete = hasRole(session, ROLE_ADMIN);
  const [data, setData] = useState<Page<TagDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(() => {
    if (typeof window === "undefined") return 20;
    const stored = localStorage.getItem("pageSize.tags");
    const parsed = Number(stored);
    if ([10, 20, 50, 100, 200].includes(parsed)) return parsed;
    localStorage.setItem("pageSize.tags", "20");
    return 20;
  });
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<TagDto | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  const fetchTags = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getTags({ page, size: pageSize, name: debouncedSearch || undefined, includeCounts: true });
      setData(result);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, debouncedSearch]);

  useEffect(() => {
    fetchTags();
  }, [fetchTags]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteTag(deleteTarget.id);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchTags();
    } catch (e) {
      if (e instanceof ForbiddenError) {
        setDeleteError(t.errors.forbidden.deleteNoPermission);
      } else {
        setDeleteError(e instanceof Error ? e.message : "Unknown error");
      }
    }
  };

  const totalElements = data?.page.totalElements ?? 0;
  const totalPages = data?.page.totalPages ?? 0;
  const start = totalElements > 0 ? page * pageSize + 1 : 0;
  const end = Math.min((page + 1) * pageSize, totalElements);

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{t.tags.title}</h1>
        <Link href="/tags/new">
          <Button className="bg-oe-green hover:bg-oe-green/90 text-white">
            <Plus className="mr-2 h-4 w-4" />
            {t.tags.newTag}
          </Button>
        </Link>
      </div>

      <div className="mb-4">
        <Input
          placeholder={t.tags.form.namePlaceholder}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="max-w-sm"
        />
      </div>

      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <Tag className="mb-4 h-12 w-12 text-oe-gray-light" />
          <p className="mb-4 text-oe-gray">{t.tags.empty}</p>
          <Link href="/tags/new">
            <Button className="bg-oe-green hover:bg-oe-green/90 text-white">
              {t.tags.createFirst}
            </Button>
          </Link>
        </div>
      ) : (
        <>
          <div className="overflow-hidden rounded-lg border border-oe-gray-light">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-oe-gray-light bg-oe-gray-lightest">
                  <th className="px-4 py-3 text-left font-medium text-oe-gray w-16">{t.tags.columns.color}</th>
                  <th className="px-4 py-3 text-left font-medium text-oe-gray">{t.tags.columns.name}</th>
                  <th className="px-4 py-3 text-left font-medium text-oe-gray hidden md:table-cell">{t.tags.columns.description}</th>
                  <th className="px-4 py-3 text-center font-medium text-oe-gray w-24">{t.nav.companies}</th>
                  <th className="px-4 py-3 text-center font-medium text-oe-gray w-24">{t.nav.contacts}</th>
                  <th className="px-4 py-3 text-center font-medium text-oe-gray w-24">{t.nav.tasks}</th>
                  <th className="px-4 py-3 text-right font-medium text-oe-gray w-24">{t.tags.columns.actions}</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((tag) => (
                  <tr key={tag.id} className="border-b border-oe-gray-light last:border-0">
                    <td className="px-4 py-3">
                      <span
                        className="inline-block h-5 w-5 rounded-full"
                        style={{ backgroundColor: tag.color }}
                      />
                    </td>
                    <td className="px-4 py-3 font-medium text-oe-dark">{tag.name}</td>
                    <td className="px-4 py-3 text-oe-gray hidden md:table-cell truncate max-w-xs">
                      {tag.description ?? ""}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className="text-oe-dark">{tag.companyCount ?? 0}</span>
                      {(tag.companyCount ?? 0) > 0 && (
                        <Link href={`/companies?tagIds=${tag.id}`} className="ml-1 inline-block align-middle text-oe-gray hover:text-oe-green">
                          <ArrowUpRight className="h-4 w-4" />
                        </Link>
                      )}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className="text-oe-dark">{tag.contactCount ?? 0}</span>
                      {(tag.contactCount ?? 0) > 0 && (
                        <Link href={`/contacts?tagIds=${tag.id}`} className="ml-1 inline-block align-middle text-oe-gray hover:text-oe-green">
                          <ArrowUpRight className="h-4 w-4" />
                        </Link>
                      )}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className="text-oe-dark">{tag.taskCount ?? 0}</span>
                      {(tag.taskCount ?? 0) > 0 && (
                        <Link href={`/tasks?tagIds=${tag.id}`} className="ml-1 inline-block align-middle text-oe-gray hover:text-oe-green">
                          <ArrowUpRight className="h-4 w-4" />
                        </Link>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Link href={`/tags/${tag.id}/edit`}>
                              <Button variant="ghost" size="icon" className="h-8 w-8 text-oe-gray hover:text-oe-dark">
                                <Pencil className="h-4 w-4" />
                              </Button>
                            </Link>
                          </TooltipTrigger>
                          <TooltipContent>{t.tags.actions.edit}</TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <span>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-8 w-8 text-oe-gray hover:text-oe-red"
                                disabled={!canDelete}
                                onClick={() => { setDeleteTarget(tag); setDeleteError(null); }}
                              >
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </span>
                          </TooltipTrigger>
                          <TooltipContent>{canDelete ? t.tags.actions.delete : t.errors.roleRequired.admin}</TooltipContent>
                        </Tooltip>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="mt-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Select value={String(pageSize)} onValueChange={(v) => { const n = Number(v); setPageSize(n); localStorage.setItem("pageSize.tags", v); setPage(0); }}>
                  <SelectTrigger className="w-[80px] h-8 text-sm">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {[10, 20, 50, 100, 200].map((s) => (
                      <SelectItem key={s} value={String(s)}>{s}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <span className="text-sm text-oe-gray">{t.tags.pagination.perPage}</span>
                <span className="text-sm text-oe-gray">·</span>
                <span className="text-sm text-oe-gray">
                  {t.tags.pagination.showing} {start}–{end} {t.tags.pagination.of} {totalElements} {t.tags.pagination.tags}
                </span>
              </div>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  {t.tags.pagination.previous}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {t.tags.pagination.next}
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      <DeleteConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => { if (!open) { setDeleteTarget(null); setDeleteError(null); } }}
        onConfirm={handleDelete}
        title={t.tags.deleteDialog.title}
        description={t.tags.deleteDialog.description}
        confirmLabel={t.tags.deleteDialog.confirm}
        cancelLabel={t.tags.deleteDialog.cancel}
        error={deleteError}
        errorTitle={t.tags.deleteDialog.errorTitle}
      />
    </div>
  );
}
