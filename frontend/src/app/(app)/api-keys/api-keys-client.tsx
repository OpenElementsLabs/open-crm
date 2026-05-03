"use client";

import { useCallback, useEffect, useState } from "react";
import { Check, Copy, KeyRound, Plus, Trash2 } from "lucide-react";
import { Button, DeleteConfirmDialog, Input, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, Skeleton } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { TablePagination, TooltipIconButton } from "@open-elements/ui";
import { getApiKeys, createApiKey, deleteApiKey } from "@/lib/api";
import type { ApiKeyDto, ApiKeyCreatedDto, Page } from "@/lib/types";

export function ApiKeysClient() {
  const t = useTranslations();
  const [data, setData] = useState<Page<ApiKeyDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(() => {
    if (typeof window === "undefined") return 20;
    const stored = localStorage.getItem("pageSize.apiKeys");
    const parsed = Number(stored);
    if ([10, 20, 50, 100, 200].includes(parsed)) return parsed;
    localStorage.setItem("pageSize.apiKeys", "20");
    return 20;
  });
  const [deleteTarget, setDeleteTarget] = useState<ApiKeyDto | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createName, setCreateName] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createdKey, setCreatedKey] = useState<ApiKeyCreatedDto | null>(null);
  const [copied, setCopied] = useState(false);

  const fetchApiKeys = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getApiKeys({ page, size: pageSize });
      setData(result);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    fetchApiKeys();
  }, [fetchApiKeys]);

  const handleCreate = async () => {
    if (!createName.trim()) {
      setCreateError(t.apiKeys.createDialog.nameRequired);
      return;
    }
    setCreateError(null);
    setCreateSubmitting(true);
    try {
      const result = await createApiKey({ name: createName.trim() });
      setCreateOpen(false);
      setCreateName("");
      setCreateError(null);
      setCreatedKey(result);
      setCopied(false);
    } catch (e) {
      setCreateError(
        e instanceof Error ? e.message : t.apiKeys.createDialog.error,
      );
    } finally {
      setCreateSubmitting(false);
    }
  };

  const handleCopy = async () => {
    if (createdKey) {
      await navigator.clipboard.writeText(createdKey.key);
      setCopied(true);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteApiKey(deleteTarget.id);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchApiKeys();
    } catch (e) {
      setDeleteError(e instanceof Error ? e.message : "Unknown error");
    }
  };

  const totalElements = data?.page.totalElements ?? 0;
  const totalPages = data?.page.totalPages ?? 0;

  return (
    <div>
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">
          {t.apiKeys.title}
        </h1>
        <Button
          onClick={() => {
            setCreateName("");
            setCreateError(null);
            setCreateOpen(true);
          }}
        >
          <Plus className="mr-2 h-4 w-4" />
          {t.apiKeys.newApiKey}
        </Button>
      </div>

      {/* Loading */}
      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        /* Empty state */
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <KeyRound className="mb-4 h-12 w-12 text-oe-gray/50" />
          <p className="mb-4 text-oe-gray">{t.apiKeys.empty}</p>
          <Button
            onClick={() => {
              setCreateName("");
              setCreateError(null);
              setCreateOpen(true);
            }}
          >
            {t.apiKeys.createFirst}
          </Button>
        </div>
      ) : (
        <>
          {/* Table */}
          <div className="overflow-hidden rounded-lg border border-oe-gray-light">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-oe-gray-light bg-oe-gray-lightest">
                  <th className="px-4 py-3 text-left font-medium text-oe-gray">
                    {t.apiKeys.columns.name}
                  </th>
                  <th className="px-4 py-3 text-left font-medium text-oe-gray">
                    {t.apiKeys.columns.key}
                  </th>
                  <th className="hidden px-4 py-3 text-left font-medium text-oe-gray md:table-cell">
                    {t.apiKeys.columns.createdBy}
                  </th>
                  <th className="hidden px-4 py-3 text-left font-medium text-oe-gray md:table-cell">
                    {t.apiKeys.columns.createdAt}
                  </th>
                  <th className="px-4 py-3 text-right font-medium text-oe-gray w-20">
                    {t.apiKeys.columns.actions}
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((apiKey) => (
                  <tr
                    key={apiKey.id}
                    className="border-b border-oe-gray-light last:border-0"
                  >
                    <td className="px-4 py-3 font-medium text-oe-dark">
                      {apiKey.name}
                    </td>
                    <td className="px-4 py-3 font-mono text-sm text-oe-gray">
                      {apiKey.keyPrefix}
                    </td>
                    <td className="hidden px-4 py-3 text-oe-gray md:table-cell">
                      {apiKey.createdBy}
                    </td>
                    <td className="hidden px-4 py-3 text-oe-gray md:table-cell">
                      {new Date(apiKey.createdAt).toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <TooltipIconButton
                        icon={<Trash2 />}
                        tone="destructive"
                        tooltip={t.apiKeys.actions.delete}
                        onClick={() => {
                          setDeleteTarget(apiKey);
                          setDeleteError(null);
                        }}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <TablePagination
            page={page}
            pageSize={pageSize}
            totalElements={totalElements}
            totalPages={totalPages}
            pageSizeOptions={[10, 20, 50, 100, 200]}
            storageKey="pageSize.apiKeys"
            translations={t.apiKeys.pagination}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        </>
      )}

      {/* Create dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t.apiKeys.createDialog.title}</DialogTitle>
            <DialogDescription>
              {t.apiKeys.createDialog.nameLabel}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <Input
              placeholder={t.apiKeys.createDialog.namePlaceholder}
              value={createName}
              onChange={(e) => {
                setCreateName(e.target.value);
                setCreateError(null);
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") handleCreate();
              }}
            />
            {createError && (
              <p className="text-sm text-oe-red">{createError}</p>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              {t.apiKeys.createDialog.cancel}
            </Button>
            <Button
              onClick={handleCreate}
              disabled={createSubmitting}
            >
              {t.apiKeys.createDialog.create}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Key created dialog */}
      <Dialog
        open={createdKey !== null}
        onOpenChange={(open) => {
          if (!open) {
            setCreatedKey(null);
            fetchApiKeys();
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t.apiKeys.keyDialog.title}</DialogTitle>
            <DialogDescription>
              {t.apiKeys.keyDialog.warning}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="rounded-lg border border-oe-gray-light bg-oe-gray-lightest p-4">
              <code className="block break-all font-mono text-sm text-oe-dark">
                {createdKey?.key}
              </code>
            </div>
            <Button
              variant="outline"
              className="w-full"
              onClick={handleCopy}
            >
              {copied ? (
                <>
                  <Check className="mr-2 h-4 w-4" />
                  {t.apiKeys.keyDialog.copied}
                </>
              ) : (
                <>
                  <Copy className="mr-2 h-4 w-4" />
                  {t.apiKeys.keyDialog.copy}
                </>
              )}
            </Button>
          </div>
          <DialogFooter>
            <Button
              onClick={() => {
                setCreatedKey(null);
                fetchApiKeys();
              }}
            >
              {t.apiKeys.keyDialog.close}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete dialog */}
      <DeleteConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) {
            setDeleteTarget(null);
            setDeleteError(null);
          }
        }}
        onConfirm={handleDelete}
        title={t.apiKeys.deleteDialog.title}
        description={t.apiKeys.deleteDialog.description}
        confirmLabel={t.apiKeys.deleteDialog.confirm}
        cancelLabel={t.apiKeys.deleteDialog.cancel}
        error={deleteError}
        errorTitle={t.apiKeys.deleteDialog.errorTitle}
      />
    </div>
  );
}
