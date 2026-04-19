"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Plus, Pencil } from "lucide-react";
import { Button, TagMultiSelect, Tooltip, TooltipTrigger, TooltipContent, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, Skeleton } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { getTasks, getTags } from "@/lib/api";
import type { TaskDto, TaskStatus, Page } from "@/lib/types";

const STATUS_BADGE_CLASSES: Record<TaskStatus, string> = {
  OPEN: "bg-blue-100 text-blue-800",
  IN_PROGRESS: "bg-yellow-100 text-yellow-800",
  DONE: "bg-green-100 text-green-800",
};

export function TaskList() {
  const t = useTranslations();
  const S = t.tasks;
  const router = useRouter();
  const [data, setData] = useState<Page<TaskDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(() => {
    if (typeof window === "undefined") return 20;
    const stored = localStorage.getItem("pageSize.tasks");
    const parsed = Number(stored);
    if ([10, 20, 50, 100, 200].includes(parsed)) return parsed;
    localStorage.setItem("pageSize.tasks", "20");
    return 20;
  });
  const [statusFilter, setStatusFilter] = useState("all");
  const [tagIds, setTagIds] = useState<string[]>([]);

  const fetchTasks = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getTasks({
        page,
        size: pageSize,
        status: statusFilter !== "all" ? (statusFilter as TaskStatus) : undefined,
        tagIds: tagIds.length > 0 ? tagIds : undefined,
      });
      setData(result);
    } catch {
      console.error("Failed to fetch tasks");
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, statusFilter, tagIds]);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  useEffect(() => {
    setPage(0);
  }, [statusFilter, tagIds]);

  function formatDate(dateStr: string): string {
    try {
      return new Date(dateStr).toLocaleDateString();
    } catch {
      return dateStr;
    }
  }

  function getEntityDisplay(task: TaskDto): { name: string; href: string } | null {
    if (task.companyId && task.companyName) {
      return { name: task.companyName, href: `/companies/${task.companyId}` };
    }
    if (task.contactId && task.contactName) {
      return { name: task.contactName, href: `/contacts/${task.contactId}` };
    }
    return null;
  }

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{S.title}</h1>
        <Button asChild className="bg-oe-green hover:bg-oe-green-dark text-white">
          <Link href="/tasks/new">
            <Plus className="mr-2 h-4 w-4" />
            {S.new}
          </Link>
        </Button>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-col gap-3 sm:flex-row">
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="sm:max-w-[200px]">
            <SelectValue placeholder={S.filter.status} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{S.filter.allStatuses}</SelectItem>
            <SelectItem value="OPEN">{S.status.OPEN}</SelectItem>
            <SelectItem value="IN_PROGRESS">{S.status.IN_PROGRESS}</SelectItem>
            <SelectItem value="DONE">{S.status.DONE}</SelectItem>
          </SelectContent>
        </Select>
        <div className="sm:max-w-[250px]">
          <TagMultiSelect
            selectedIds={tagIds}
            onChange={setTagIds}
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
            <Link href="/tasks/new">
              <Plus className="mr-2 h-4 w-4" />
              {S.new}
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
                  <TableHead>{S.columns.entity}</TableHead>
                  <TableHead>{S.columns.action}</TableHead>
                  <TableHead>{S.columns.status}</TableHead>
                  <TableHead>{S.columns.dueDate}</TableHead>
                  <TableHead className="w-[80px] text-right">{S.columns.actions}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((task) => {
                  const entity = getEntityDisplay(task);
                  return (
                    <TableRow
                      key={task.id}
                      className="cursor-pointer"
                      onClick={() => router.push(`/tasks/${task.id}`)}
                    >
                      <TableCell>
                        {entity ? (
                          <Link
                            href={entity.href}
                            className="text-oe-blue hover:underline"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {entity.name}
                          </Link>
                        ) : (
                          <span className="text-oe-gray-mid">—</span>
                        )}
                      </TableCell>
                      <TableCell className="max-w-[300px] truncate">{task.action}</TableCell>
                      <TableCell>
                        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_BADGE_CLASSES[task.status]}`}>
                          {S.status[task.status]}
                        </span>
                      </TableCell>
                      <TableCell>{formatDate(task.dueDate)}</TableCell>
                      <TableCell className="text-right">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={(e) => {
                                e.stopPropagation();
                                router.push(`/tasks/${task.id}/edit`);
                              }}
                            >
                              <Pencil className="h-4 w-4 text-oe-blue" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>{S.edit}</TooltipContent>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>

          {/* Pagination */}
          <div className="mt-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Select value={String(pageSize)} onValueChange={(v) => { const n = Number(v); setPageSize(n); localStorage.setItem("pageSize.tasks", v); setPage(0); }}>
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
    </div>
  );
}
