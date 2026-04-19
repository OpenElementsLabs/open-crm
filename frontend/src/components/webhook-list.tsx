"use client";

import { useCallback, useEffect, useState } from "react";
import { Plus, Trash2, Radio, Webhook } from "lucide-react";
import { Button, Input, Tooltip, TooltipTrigger, TooltipContent, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, Skeleton } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import {
  getWebhooks,
  createWebhook,
  updateWebhook,
  deleteWebhook,
  pingWebhook,
} from "@/lib/api";
import type { WebhookDto, Page } from "@/lib/types";

function formatStatus(
  status: number | null,
  t: ReturnType<typeof useTranslations>,
): string {
  if (status === null) return t.webhooks.status.neverCalled;
  if (status === -1) return t.webhooks.status.timeout;
  if (status === 0) return t.webhooks.status.connectionError;
  if (status >= 200 && status < 300) return t.webhooks.status.ok;
  return `${t.webhooks.status.badCall} (${status})`;
}

function formatTimestamp(ts: string | null): string {
  if (!ts) return "—";
  return new Date(ts).toLocaleString();
}

export function WebhookList() {
  const t = useTranslations();
  const [data, setData] = useState<Page<WebhookDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(() => {
    if (typeof window === "undefined") return 20;
    const stored = localStorage.getItem("pageSize.webhooks");
    const parsed = Number(stored);
    if ([10, 20, 50, 100, 200].includes(parsed)) return parsed;
    localStorage.setItem("pageSize.webhooks", "20");
    return 20;
  });
  const [deleteTarget, setDeleteTarget] = useState<WebhookDto | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createUrl, setCreateUrl] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [createSubmitting, setCreateSubmitting] = useState(false);

  const fetchWebhooks = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getWebhooks({ page, size: pageSize });
      setData(result);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    fetchWebhooks();
  }, [fetchWebhooks]);

  const handleCreate = async () => {
    if (!createUrl.trim()) {
      setCreateError(t.webhooks.createDialog.urlRequired);
      return;
    }
    setCreateError(null);
    setCreateSubmitting(true);
    try {
      await createWebhook({ url: createUrl.trim() });
      setCreateOpen(false);
      setCreateUrl("");
      setCreateError(null);
      fetchWebhooks();
    } catch (e) {
      setCreateError(
        e instanceof Error ? e.message : t.webhooks.createDialog.error,
      );
    } finally {
      setCreateSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteWebhook(deleteTarget.id);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchWebhooks();
    } catch (e) {
      setDeleteError(e instanceof Error ? e.message : "Unknown error");
    }
  };

  const handleToggleActive = async (webhook: WebhookDto) => {
    try {
      await updateWebhook(webhook.id, {
        url: webhook.url,
        active: !webhook.active,
      });
      fetchWebhooks();
    } catch {
      // Silently fail — user can retry
    }
  };

  const handlePing = async (webhook: WebhookDto) => {
    try {
      await pingWebhook(webhook.id);
    } catch {
      // Fire-and-forget — no visible feedback
    }
  };

  const totalElements = data?.page.totalElements ?? 0;
  const totalPages = data?.page.totalPages ?? 0;

  return (
    <div>
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">
          {t.webhooks.title}
        </h1>
        <Button
          className="bg-oe-green hover:bg-oe-green/90 text-white"
          onClick={() => {
            setCreateUrl("");
            setCreateError(null);
            setCreateOpen(true);
          }}
        >
          <Plus className="mr-2 h-4 w-4" />
          {t.webhooks.newWebhook}
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
          <Webhook className="mb-4 h-12 w-12 text-oe-gray/50" />
          <p className="mb-4 text-oe-gray">{t.webhooks.empty}</p>
          <Button
            className="bg-oe-green hover:bg-oe-green/90 text-white"
            onClick={() => {
              setCreateUrl("");
              setCreateError(null);
              setCreateOpen(true);
            }}
          >
            {t.webhooks.createFirst}
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
                    {t.webhooks.columns.url}
                  </th>
                  <th className="px-4 py-3 text-center font-medium text-oe-gray w-20">
                    {t.webhooks.columns.active}
                  </th>
                  <th className="hidden px-4 py-3 text-left font-medium text-oe-gray md:table-cell">
                    {t.webhooks.columns.lastStatus}
                  </th>
                  <th className="hidden px-4 py-3 text-left font-medium text-oe-gray md:table-cell">
                    {t.webhooks.columns.lastCalledAt}
                  </th>
                  <th className="px-4 py-3 text-right font-medium text-oe-gray w-24">
                    {t.webhooks.columns.actions}
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((webhook) => (
                  <tr
                    key={webhook.id}
                    className="border-b border-oe-gray-light last:border-0"
                  >
                    <td className="px-4 py-3 font-medium text-oe-dark truncate max-w-xs">
                      {webhook.url}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span
                        className={`inline-block h-3 w-3 rounded-full ${webhook.active ? "bg-oe-green" : "bg-oe-gray"}`}
                      />
                    </td>
                    <td className="hidden px-4 py-3 text-oe-gray md:table-cell">
                      {formatStatus(webhook.lastStatus, t)}
                    </td>
                    <td className="hidden px-4 py-3 text-oe-gray md:table-cell">
                      {formatTimestamp(webhook.lastCalledAt)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 text-oe-gray hover:text-oe-dark"
                              onClick={() => handleToggleActive(webhook)}
                            >
                              {webhook.active ? (
                                <span className="text-xs font-bold">OFF</span>
                              ) : (
                                <span className="text-xs font-bold">ON</span>
                              )}
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>
                            {webhook.active
                              ? t.webhooks.actions.deactivate
                              : t.webhooks.actions.activate}
                          </TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 text-oe-gray hover:text-oe-dark"
                              onClick={() => handlePing(webhook)}
                            >
                              <Radio className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>
                            {t.webhooks.actions.ping}
                          </TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 text-oe-gray hover:text-oe-red"
                              onClick={() => {
                                setDeleteTarget(webhook);
                                setDeleteError(null);
                              }}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>
                            {t.webhooks.actions.delete}
                          </TooltipContent>
                        </Tooltip>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="mt-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Select
                value={String(pageSize)}
                onValueChange={(v) => {
                  const n = Number(v);
                  setPageSize(n);
                  localStorage.setItem("pageSize.webhooks", v);
                  setPage(0);
                }}
              >
                <SelectTrigger className="w-[80px] h-8 text-sm">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {[10, 20, 50, 100, 200].map((s) => (
                    <SelectItem key={s} value={String(s)}>
                      {s}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <span className="text-sm text-oe-gray">
                {t.webhooks.pagination.perPage}
              </span>
              <span className="text-sm text-oe-gray">
                · {totalElements} {t.webhooks.title}
              </span>
            </div>
            {totalPages > 1 && (
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  {t.webhooks.pagination.previous}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {t.webhooks.pagination.next}
                </Button>
              </div>
            )}
          </div>
        </>
      )}

      {/* Create dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t.webhooks.createDialog.title}</DialogTitle>
            <DialogDescription>
              {t.webhooks.createDialog.urlLabel}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <Input
              placeholder={t.webhooks.createDialog.urlPlaceholder}
              value={createUrl}
              onChange={(e) => {
                setCreateUrl(e.target.value);
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
              {t.webhooks.createDialog.cancel}
            </Button>
            <Button
              className="bg-oe-green hover:bg-oe-green/90 text-white"
              onClick={handleCreate}
              disabled={createSubmitting}
            >
              {t.webhooks.createDialog.create}
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
        title={t.webhooks.deleteDialog.title}
        description={t.webhooks.deleteDialog.description}
        confirmLabel={t.webhooks.deleteDialog.confirm}
        cancelLabel={t.webhooks.deleteDialog.cancel}
        error={deleteError}
        errorTitle={t.webhooks.deleteDialog.errorTitle}
      />
    </div>
  );
}
