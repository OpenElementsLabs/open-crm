"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { User } from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getContacts, getContactPhotoUrl } from "@/lib/api";
import type { ContactDto } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

export const dynamic = "force-dynamic";

export default function ContactPrintPage() {
  return (
    <Suspense fallback={<p className="p-8 text-oe-gray-mid">Loading...</p>}>
      <ContactPrintContent />
    </Suspense>
  );
}

function ContactPrintContent() {
  const t = useTranslations();
  const S = t.contacts;
  const P = t.print;
  const searchParams = useSearchParams();

  const [allRecords, setAllRecords] = useState<ContactDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const searchFilter = searchParams.get("search") ?? "";
  const companyIdFilter = searchParams.get("companyId") ?? "";
  const noCompanyFilter = searchParams.get("noCompany") === "true";
  const languageFilter = searchParams.get("language") ?? "";
  const brevoFilter = searchParams.get("brevo") ?? "";

  useEffect(() => {
    const sidebar = document.querySelector("aside");
    const main = document.querySelector("main");
    if (sidebar) sidebar.style.display = "none";
    if (main) main.style.marginLeft = "0";
    return () => {
      if (sidebar) sidebar.style.display = "";
      if (main) main.style.marginLeft = "";
    };
  }, []);

  useEffect(() => {
    async function loadAll() {
      try {
        const records: ContactDto[] = [];
        let page = 0;
        let isLast = false;
        while (!isLast) {
          const result = await getContacts({
            page,
            size: 250,
            search: searchFilter || undefined,
            companyId: companyIdFilter || undefined,
            noCompany: noCompanyFilter || undefined,
            language: languageFilter || undefined,
            brevo: brevoFilter === "" ? undefined : brevoFilter === "true",
          });
          records.push(...result.content);
          isLast = result.page.number >= result.page.totalPages - 1;
          page++;
        }
        setAllRecords(records);
      } catch {
        setError("Failed to load contacts");
      } finally {
        setLoading(false);
      }
    }
    loadAll();
  }, [searchFilter, companyIdFilter, noCompanyFilter, languageFilter, brevoFilter]);

  useEffect(() => {
    if (!loading && !error) {
      const timer = setTimeout(() => window.print(), 300);
      return () => clearTimeout(timer);
    }
  }, [loading, error]);

  const filterParts: string[] = [];
  if (searchFilter) filterParts.push(`${S.filter.search.replace("...", "")}: ${searchFilter}`);
  if (noCompanyFilter) filterParts.push(`${S.columns.company}: ${S.form.noCompany}`);
  else if (companyIdFilter) filterParts.push(`${S.columns.company}: ${companyIdFilter}`);
  if (languageFilter) filterParts.push(`${S.filter.language}: ${languageFilter}`);
  if (brevoFilter === "true") filterParts.push(`Brevo: ${P.filterYes}`);
  if (brevoFilter === "false") filterParts.push(`Brevo: ${P.filterNo}`);
  const filterSummary = filterParts.length > 0 ? filterParts.join(" \u00B7 ") : P.noFilters;

  if (loading) {
    return <p className="p-8 text-oe-gray-mid">{P.loading}</p>;
  }

  if (error) {
    return <p className="p-8 text-oe-red">{error}</p>;
  }

  return (
    <div>
      <h1 className="mb-2 font-heading text-2xl font-bold text-oe-dark">{S.title}</h1>
      <p className="mb-4 text-sm text-oe-gray-mid">{filterSummary}</p>

      {allRecords.length === 0 ? (
        <p className="py-10 text-center text-oe-gray-mid">{P.noRecords}</p>
      ) : (
        <div className="rounded-md border border-oe-gray-light overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-[50px]"></TableHead>
                <TableHead>{S.columns.name}</TableHead>
                <TableHead>{S.columns.email}</TableHead>
                <TableHead>{S.columns.company}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {allRecords.map((contact) => (
                <TableRow key={contact.id}>
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
                  <TableCell className="font-medium whitespace-normal break-words">
                    {`${contact.firstName} ${contact.lastName}`.trim()}
                  </TableCell>
                  <TableCell className="text-oe-gray-mid whitespace-normal break-words">
                    {contact.email ?? "\u2014"}
                  </TableCell>
                  <TableCell className="text-oe-gray-mid whitespace-normal break-words">
                    {contact.companyName ?? ""}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
