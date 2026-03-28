"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Plus, Trash2, RotateCcw, Archive, Building2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import { getCompanies, deleteCompany, restoreCompany, getCompanyLogoUrl } from "@/lib/api";
import type { CompanyDto, Page } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

export function CompanyList() {
  const t = useTranslations();
  const S = t.companies;
  const router = useRouter();
  const [data, setData] = useState<Page<CompanyDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState("name,asc");
  const [nameFilter, setNameFilter] = useState("");
  const [cityFilter, setCityFilter] = useState("");
  const [countryFilter, setCountryFilter] = useState("");
  const [includeDeleted, setIncludeDeleted] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<CompanyDto | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const fetchCompanies = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getCompanies({
        page,
        size: 20,
        sort,
        name: nameFilter || undefined,
        city: cityFilter || undefined,
        country: countryFilter || undefined,
        includeDeleted,
      });
      setData(result);
    } catch {
      console.error("Failed to fetch companies");
    } finally {
      setLoading(false);
    }
  }, [page, sort, nameFilter, cityFilter, countryFilter, includeDeleted]);

  useEffect(() => {
    fetchCompanies();
  }, [fetchCompanies]);

  useEffect(() => {
    setPage(0);
  }, [nameFilter, cityFilter, countryFilter, sort, includeDeleted]);

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
        <Button asChild className="bg-oe-green hover:bg-oe-green-dark text-white">
          <Link href="/companies/new">
            <Plus className="mr-2 h-4 w-4" />
            {S.newCompany}
          </Link>
        </Button>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-col gap-3 sm:flex-row">
        <Input
          placeholder={S.filter.name}
          value={nameFilter}
          onChange={(e) => setNameFilter(e.target.value)}
          className="sm:max-w-[200px]"
        />
        <Input
          placeholder={S.filter.city}
          value={cityFilter}
          onChange={(e) => setCityFilter(e.target.value)}
          className="sm:max-w-[200px]"
        />
        <Input
          placeholder={S.filter.country}
          value={countryFilter}
          onChange={(e) => setCountryFilter(e.target.value)}
          className="sm:max-w-[200px]"
        />
        <Select value={sort} onValueChange={setSort}>
          <SelectTrigger className="sm:max-w-[200px]">
            <SelectValue placeholder={S.sort.label} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="name,asc">{S.sort.nameAsc}</SelectItem>
            <SelectItem value="name,desc">{S.sort.nameDesc}</SelectItem>
            <SelectItem value="createdAt,desc">{S.sort.createdAtDesc}</SelectItem>
            <SelectItem value="createdAt,asc">{S.sort.createdAtAsc}</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Archive toggle */}
      <div className="mb-4">
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
                  <TableHead className="w-[100px] text-right">{S.columns.actions}</TableHead>
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
                          className="h-8 w-8 rounded object-cover"
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
              {S.pagination.page
                .replace("{current}", String(data.number + 1))
                .replace("{total}", String(data.totalPages))}
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={data.first}
                onClick={() => setPage(page - 1)}
              >
                {S.pagination.previous}
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={data.last}
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
    </div>
  );
}
