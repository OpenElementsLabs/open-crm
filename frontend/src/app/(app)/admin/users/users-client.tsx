"use client";

import { useCallback, useEffect, useState } from "react";
import { User as UserIcon, Users as UsersIcon } from "lucide-react";
import {
  Button,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Skeleton,
} from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { getUsers } from "@/lib/api";
import type { Page, UserDto } from "@/lib/types";

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100, 200] as const;
const DEFAULT_PAGE_SIZE = 20;
const PAGE_SIZE_STORAGE_KEY = "pageSize.users";

function readStoredPageSize(): number {
  if (typeof window === "undefined") return DEFAULT_PAGE_SIZE;
  const stored = localStorage.getItem(PAGE_SIZE_STORAGE_KEY);
  const parsed = Number(stored);
  if ((PAGE_SIZE_OPTIONS as readonly number[]).includes(parsed)) return parsed;
  localStorage.setItem(PAGE_SIZE_STORAGE_KEY, String(DEFAULT_PAGE_SIZE));
  return DEFAULT_PAGE_SIZE;
}

export function UsersClient() {
  const t = useTranslations();
  const [data, setData] = useState<Page<UserDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<number>(() => readStoredPageSize());

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getUsers({ page, size: pageSize });
      setData(result);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const totalElements = data?.page.totalElements ?? 0;
  const totalPages = data?.page.totalPages ?? 0;
  const totalLabel =
    totalElements === 1
      ? t.users.pagination.totalOne.replace("{count}", String(totalElements))
      : t.users.pagination.totalOther.replace("{count}", String(totalElements));

  return (
    <div>
      <h1 className="mb-6 font-heading text-2xl font-bold text-oe-dark">
        {t.users.title}
      </h1>

      {loading ? (
        <div className="space-y-3" data-testid="users-loading">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div
          className="flex flex-col items-center justify-center py-16 text-center"
          data-testid="users-empty"
        >
          <UsersIcon className="mb-4 h-12 w-12 text-oe-gray/50" />
          <p className="text-oe-gray">{t.users.empty}</p>
        </div>
      ) : (
        <>
          <div className="overflow-hidden rounded-lg border border-oe-gray-light">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-oe-gray-light bg-oe-gray-lightest">
                  <th
                    scope="col"
                    className="px-4 py-3 text-left font-medium text-oe-gray w-16"
                  >
                    {t.users.columns.avatar}
                  </th>
                  <th
                    scope="col"
                    className="px-4 py-3 text-left font-medium text-oe-gray"
                  >
                    {t.users.columns.name}
                  </th>
                  <th
                    scope="col"
                    className="px-4 py-3 text-left font-medium text-oe-gray"
                  >
                    {t.users.columns.email}
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((user) => (
                  <tr
                    key={user.id}
                    className="border-b border-oe-gray-light last:border-0"
                  >
                    <td className="px-4 py-3">
                      {user.avatarUrl ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          src={user.avatarUrl}
                          alt=""
                          className="h-8 w-8 rounded-full object-cover"
                        />
                      ) : (
                        <div
                          className="flex h-8 w-8 items-center justify-center rounded-full bg-oe-gray-lightest text-oe-gray"
                          data-testid="user-avatar-fallback"
                          aria-hidden="true"
                        >
                          <UserIcon className="h-4 w-4" />
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3 font-medium text-oe-dark">
                      {user.name}
                    </td>
                    <td className="px-4 py-3 text-oe-gray">{user.email}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Select
                value={String(pageSize)}
                onValueChange={(v) => {
                  const n = Number(v);
                  setPageSize(n);
                  localStorage.setItem(PAGE_SIZE_STORAGE_KEY, v);
                  setPage(0);
                }}
              >
                <SelectTrigger
                  className="w-[80px] h-8 text-sm"
                  aria-label={t.users.pagination.perPage}
                >
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PAGE_SIZE_OPTIONS.map((s) => (
                    <SelectItem key={s} value={String(s)}>
                      {s}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <span className="text-sm text-oe-gray">
                {t.users.pagination.perPage}
              </span>
              <span className="text-sm text-oe-gray">· {totalLabel}</span>
            </div>
            {totalPages > 1 && (
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  {t.users.pagination.previous}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {t.users.pagination.next}
                </Button>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
