"use client";

import { useCallback, useEffect, useState } from "react";
import { AlertCircle, FileText } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { TablePagination } from "@open-elements/ui";
import {
  getAuditLogEntityTypes,
  getAuditLogs,
  getUsers,
} from "@/lib/api";
import type { AuditLogDto, Page, UserDto } from "@/lib/types";

export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100, 200] as const;
export const DEFAULT_PAGE_SIZE = 20;
export const PAGE_SIZE_STORAGE_KEY = "pageSize.auditLogs";
export const ALL_VALUE = "__all__";
export const SYSTEM_USER = "System";

function readStoredPageSize(): number {
  if (typeof window === "undefined") return DEFAULT_PAGE_SIZE;
  const stored = localStorage.getItem(PAGE_SIZE_STORAGE_KEY);
  const parsed = Number(stored);
  if ((PAGE_SIZE_OPTIONS as readonly number[]).includes(parsed)) return parsed;
  return DEFAULT_PAGE_SIZE;
}

export function AuditLogsClient() {
  const t = useTranslations();
  const [data, setData] = useState<Page<AuditLogDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<number>(() => readStoredPageSize());
  const [entityType, setEntityType] = useState<string | undefined>(undefined);
  const [user, setUser] = useState<string | undefined>(undefined);
  const [entityTypes, setEntityTypes] = useState<readonly string[]>([]);
  const [users, setUsers] = useState<readonly UserDto[]>([]);

  useEffect(() => {
    getAuditLogEntityTypes()
      .then((types) => setEntityTypes(types))
      .catch(() => setEntityTypes([]));
    // Load up to 200 users for the dropdown — matches the largest selectable
    // page size and is enough for any plausible CRM team size.
    getUsers({ size: 200 })
      .then((result) => setUsers(result.content))
      .catch(() => setUsers([]));
  }, []);

  const fetchAuditLogs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await getAuditLogs({ page, size: pageSize, entityType, user });
      setData(result);
    } catch (err: unknown) {
      console.error("Failed to load audit logs", err);
      setError(t.auditLog.loadError);
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, entityType, user, t.auditLog.loadError]);

  useEffect(() => {
    fetchAuditLogs();
  }, [fetchAuditLogs]);

  const totalElements = data?.page.totalElements ?? 0;
  const totalPages = data?.page.totalPages ?? 0;

  const userDropdownOptions = users.filter((u) => u.name !== SYSTEM_USER);

  return (
    <div>
      <h1 className="mb-6 font-heading text-2xl font-bold text-oe-dark">
        {t.auditLog.title}
      </h1>

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2">
          <span className="text-sm text-oe-gray">
            {t.auditLog.filter.entityType}
          </span>
          <Select
            value={entityType ?? ALL_VALUE}
            onValueChange={(v) => {
              setEntityType(v === ALL_VALUE ? undefined : v);
              setPage(0);
            }}
          >
            <SelectTrigger
              className="w-[200px] h-8 text-sm"
              aria-label={t.auditLog.filter.entityType}
              data-testid="audit-logs-entity-type-filter"
            >
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL_VALUE}>
                {t.auditLog.filter.entityTypeAll}
              </SelectItem>
              {entityTypes.map((type) => (
                <SelectItem key={type} value={type}>
                  {type}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-sm text-oe-gray">{t.auditLog.filter.user}</span>
          <Select
            value={user ?? ALL_VALUE}
            onValueChange={(v) => {
              setUser(v === ALL_VALUE ? undefined : v);
              setPage(0);
            }}
          >
            <SelectTrigger
              className="w-[200px] h-8 text-sm"
              aria-label={t.auditLog.filter.user}
              data-testid="audit-logs-user-filter"
            >
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL_VALUE}>
                {t.auditLog.filter.userAll}
              </SelectItem>
              {userDropdownOptions.map((u) => (
                <SelectItem key={u.id} value={u.name}>
                  {u.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {loading ? (
        <div className="space-y-3" data-testid="audit-logs-loading">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : error ? (
        <div
          className="flex flex-col items-center justify-center py-16 text-center"
          data-testid="audit-logs-error"
          role="alert"
        >
          <AlertCircle className="mb-4 h-12 w-12 text-oe-red/70" />
          <p className="text-oe-red">{error}</p>
        </div>
      ) : !data || data.content.length === 0 ? (
        <div
          className="flex flex-col items-center justify-center py-16 text-center"
          data-testid="audit-logs-empty"
        >
          <FileText className="mb-4 h-12 w-12 text-oe-gray/50" />
          <p className="text-oe-gray">{t.auditLog.empty}</p>
        </div>
      ) : (
        <>
          <div className="overflow-hidden rounded-lg border border-oe-gray-light">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t.auditLog.columns.entityType}</TableHead>
                  <TableHead>{t.auditLog.columns.entityId}</TableHead>
                  <TableHead>{t.auditLog.columns.action}</TableHead>
                  <TableHead>{t.auditLog.columns.user}</TableHead>
                  <TableHead>{t.auditLog.columns.createdAt}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((entry) => (
                  <TableRow key={entry.id}>
                    <TableCell className="font-medium text-oe-dark">
                      {entry.entityType}
                    </TableCell>
                    <TableCell className="font-mono text-xs text-oe-gray">
                      {entry.entityId}
                    </TableCell>
                    <TableCell className="text-oe-dark">{entry.action}</TableCell>
                    <TableCell className="text-oe-gray">{entry.user}</TableCell>
                    <TableCell className="text-oe-gray">
                      {new Date(entry.createdAt).toLocaleString()}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <TablePagination
            page={page}
            pageSize={pageSize}
            totalElements={totalElements}
            totalPages={totalPages}
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            storageKey={PAGE_SIZE_STORAGE_KEY}
            translations={t.auditLog.pagination}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        </>
      )}
    </div>
  );
}
