"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Plus, Trash2, User } from "lucide-react";
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
import { getContacts, deleteContact, getCompaniesForSelect, getContactPhotoUrl } from "@/lib/api";
import type { ContactDto, CompanyDto, Page } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

export function ContactList() {
  const t = useTranslations();
  const S = t.contacts;
  const router = useRouter();
  const searchParams = useSearchParams();
  const [data, setData] = useState<Page<ContactDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState("lastName,asc");
  const [firstNameFilter, setFirstNameFilter] = useState("");
  const [lastNameFilter, setLastNameFilter] = useState("");
  const [emailFilter, setEmailFilter] = useState("");
  const [companyIdFilter, setCompanyIdFilter] = useState(searchParams.get("companyId") ?? "");
  const [languageFilter, setLanguageFilter] = useState("");
  const [brevoFilter, setBrevoFilter] = useState("all");

  const [companies, setCompanies] = useState<CompanyDto[]>([]);
  const [deleteTarget, setDeleteTarget] = useState<ContactDto | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  useEffect(() => {
    getCompaniesForSelect()
      .then(setCompanies)
      .catch(() => {});
  }, []);

  const fetchContacts = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getContacts({
        page,
        size: 20,
        sort,
        firstName: firstNameFilter || undefined,
        lastName: lastNameFilter || undefined,
        email: emailFilter || undefined,
        companyId: companyIdFilter && companyIdFilter !== "all" ? companyIdFilter : undefined,
        language: languageFilter && languageFilter !== "all" ? languageFilter : undefined,
        brevo: brevoFilter === "all" ? undefined : brevoFilter === "true",
      });
      setData(result);
    } catch {
      console.error("Failed to fetch contacts");
    } finally {
      setLoading(false);
    }
  }, [page, sort, firstNameFilter, lastNameFilter, emailFilter, companyIdFilter, languageFilter, brevoFilter]);

  useEffect(() => {
    fetchContacts();
  }, [fetchContacts]);

  useEffect(() => {
    setPage(0);
  }, [firstNameFilter, lastNameFilter, emailFilter, companyIdFilter, languageFilter, brevoFilter, sort]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteContact(deleteTarget.id);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchContacts();
    } catch {
      setDeleteError(S.form.errorGeneric);
    }
  };

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{S.title}</h1>
        <Button asChild className="bg-oe-green hover:bg-oe-green-dark text-white">
          <Link href="/contacts/new">
            <Plus className="mr-2 h-4 w-4" />
            {S.newContact}
          </Link>
        </Button>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
        <Input
          placeholder={S.filter.firstName}
          value={firstNameFilter}
          onChange={(e) => setFirstNameFilter(e.target.value)}
          className="sm:max-w-[180px]"
        />
        <Input
          placeholder={S.filter.lastName}
          value={lastNameFilter}
          onChange={(e) => setLastNameFilter(e.target.value)}
          className="sm:max-w-[180px]"
        />
        <Input
          placeholder={S.filter.email}
          value={emailFilter}
          onChange={(e) => setEmailFilter(e.target.value)}
          className="sm:max-w-[180px]"
        />
        <Select value={companyIdFilter} onValueChange={setCompanyIdFilter}>
          <SelectTrigger className="sm:max-w-[200px]">
            <SelectValue placeholder={S.filter.company} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{S.filter.allCompanies}</SelectItem>
            {companies.map((c) => (
              <SelectItem key={c.id} value={c.id}>
                {c.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={languageFilter} onValueChange={setLanguageFilter}>
          <SelectTrigger className="sm:max-w-[180px]">
            <SelectValue placeholder={S.filter.language} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{S.filter.allLanguages}</SelectItem>
            <SelectItem value="UNKNOWN">{S.filter.unknownLanguage}</SelectItem>
            <SelectItem value="DE">DE</SelectItem>
            <SelectItem value="EN">EN</SelectItem>
          </SelectContent>
        </Select>
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
        <Select value={sort} onValueChange={setSort}>
          <SelectTrigger className="sm:max-w-[200px]">
            <SelectValue placeholder={S.sort.label} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="lastName,asc">{S.sort.lastNameAsc}</SelectItem>
            <SelectItem value="lastName,desc">{S.sort.lastNameDesc}</SelectItem>
            <SelectItem value="firstName,asc">{S.sort.firstNameAsc}</SelectItem>
            <SelectItem value="firstName,desc">{S.sort.firstNameDesc}</SelectItem>
            <SelectItem value="createdAt,desc">{S.sort.createdAtDesc}</SelectItem>
            <SelectItem value="createdAt,asc">{S.sort.createdAtAsc}</SelectItem>
          </SelectContent>
        </Select>
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
            <Link href="/contacts/new">
              <Plus className="mr-2 h-4 w-4" />
              {S.newContact}
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
                  <TableHead>{S.columns.firstName}</TableHead>
                  <TableHead>{S.columns.lastName}</TableHead>
                  <TableHead>{S.columns.company}</TableHead>
                  <TableHead>{S.columns.comments}</TableHead>
                  <TableHead className="w-[100px] text-right">{S.columns.actions}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((contact) => (
                  <TableRow
                    key={contact.id}
                    className="cursor-pointer"
                    onClick={() => router.push(`/contacts/${contact.id}`)}
                  >
                    <TableCell>
                      {contact.hasPhoto ? (
                        <img
                          src={getContactPhotoUrl(contact.id)}
                          alt={`${contact.firstName} ${contact.lastName}`}
                          className="h-8 w-8 rounded-full object-cover"
                        />
                      ) : (
                        <User className="h-8 w-8 text-oe-gray-mid" />
                      )}
                    </TableCell>
                    <TableCell className="font-medium">{contact.firstName}</TableCell>
                    <TableCell>{contact.lastName}</TableCell>
                    <TableCell className="text-oe-gray-mid">
                      {contact.companyName ?? ""}
                    </TableCell>
                    <TableCell>{contact.commentCount}</TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="icon"
                        title={S.detail.delete}
                        onClick={(e) => {
                          e.stopPropagation();
                          setDeleteError(null);
                          setDeleteTarget(contact);
                        }}
                      >
                        <Trash2 className="h-4 w-4 text-oe-red" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          {/* Pagination */}
          <div className="mt-4 flex items-center justify-between">
            <p className="text-sm text-oe-gray-mid">
              {(data.totalElements === 1 ? S.pagination.totalOne : S.pagination.totalOther).replace("{count}", String(data.totalElements))} · {S.pagination.page
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
        description={S.deleteDialog.description.replace(
          "{name}",
          deleteTarget ? `${deleteTarget.firstName} ${deleteTarget.lastName}` : "",
        )}
        confirmLabel={S.deleteDialog.confirm}
        cancelLabel={S.deleteDialog.cancel}
        onConfirm={handleDelete}
        error={deleteError}
      />
    </div>
  );
}
