"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Pencil, Plus, Tag, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import { getTags, deleteTag } from "@/lib/api";
import { useTranslations } from "@/lib/i18n/language-context";
import type { TagDto, Page } from "@/lib/types";

export function TagList() {
  const t = useTranslations();
  const [data, setData] = useState<Page<TagDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
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
      const result = await getTags({ page, size: 20, name: debouncedSearch || undefined });
      setData(result);
    } finally {
      setLoading(false);
    }
  }, [page, debouncedSearch]);

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
      setDeleteError(e instanceof Error ? e.message : "Unknown error");
    }
  };

  const totalElements = data?.page.totalElements ?? 0;
  const totalPages = data?.page.totalPages ?? 0;
  const start = totalElements > 0 ? page * 20 + 1 : 0;
  const end = Math.min((page + 1) * 20, totalElements);

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
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Link href={`/tags/${tag.id}/edit`}>
                          <Button variant="ghost" size="icon" className="h-8 w-8 text-oe-gray hover:text-oe-dark">
                            <Pencil className="h-4 w-4" />
                          </Button>
                        </Link>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-oe-gray hover:text-oe-red"
                          onClick={() => { setDeleteTarget(tag); setDeleteError(null); }}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="mt-4 flex items-center justify-between">
              <span className="text-sm text-oe-gray">
                {t.tags.pagination.showing} {start}–{end} {t.tags.pagination.of} {totalElements} {t.tags.pagination.tags}
              </span>
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
