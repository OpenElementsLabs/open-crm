"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Building2 } from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getCompanies, getCompanyLogoUrl, getTag } from "@/lib/api";
import type { CompanyDto } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

export const dynamic = "force-dynamic";

export default function CompanyPrintPage() {
  return (
    <Suspense fallback={<p className="p-8 text-oe-gray-mid">Loading...</p>}>
      <CompanyPrintContent />
    </Suspense>
  );
}

function CompanyPrintContent() {
  const t = useTranslations();
  const S = t.companies;
  const P = t.print;
  const searchParams = useSearchParams();

  const [allRecords, setAllRecords] = useState<CompanyDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tagNames, setTagNames] = useState<string[]>([]);

  const nameFilter = searchParams.get("name") ?? "";
  const brevoFilter = searchParams.get("brevo") ?? "";
  const includeDeleted = searchParams.get("includeDeleted") === "true";
  const tagIdsParam = searchParams.get("tagIds") ?? "";
  const tagIds = tagIdsParam ? tagIdsParam.split(",") : [];

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
        const records: CompanyDto[] = [];
        let page = 0;
        let isLast = false;
        while (!isLast) {
          const result = await getCompanies({
            page,
            size: 250,
            name: nameFilter || undefined,
            includeDeleted,
            brevo: brevoFilter === "" ? undefined : brevoFilter === "true",
            tagIds: tagIds.length > 0 ? tagIds : undefined,
          });
          records.push(...result.content);
          isLast = result.page.number >= result.page.totalPages - 1;
          page++;
        }
        setAllRecords(records);
      } catch {
        setError("Failed to load companies");
      } finally {
        setLoading(false);
      }
    }
    loadAll();
  }, [nameFilter, brevoFilter, includeDeleted, tagIds.join(",")]);

  useEffect(() => {
    if (tagIds.length === 0) return;
    Promise.all(tagIds.map((id) => getTag(id).catch(() => null)))
      .then((results) => setTagNames(results.filter((r) => r !== null).map((r) => r!.name)));
  }, [tagIds.join(",")]);

  useEffect(() => {
    if (!loading && !error) {
      const timer = setTimeout(() => window.print(), 300);
      return () => clearTimeout(timer);
    }
  }, [loading, error]);

  const filterParts: string[] = [];
  if (nameFilter) filterParts.push(`${S.columns.name}: ${nameFilter}`);
  if (brevoFilter === "true") filterParts.push(`Brevo: ${P.filterYes}`);
  if (brevoFilter === "false") filterParts.push(`Brevo: ${P.filterNo}`);
  if (includeDeleted) filterParts.push(`${S.showArchived}: ${P.filterArchived}`);
  if (tagNames.length > 0) filterParts.push(`Tags: ${tagNames.join(", ")}`);
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
                <TableHead>{S.columns.website}</TableHead>
                <TableHead>{S.columns.contacts}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {allRecords.map((company) => (
                <TableRow key={company.id} className={company.deleted ? "opacity-50" : ""}>
                  <TableCell>
                    {company.hasLogo ? (
                      <img
                        src={getCompanyLogoUrl(company.id)}
                        alt={company.name}
                        className="h-8 w-8 object-contain"
                      />
                    ) : (
                      <Building2 className="h-8 w-8 text-oe-gray-mid" />
                    )}
                  </TableCell>
                  <TableCell className="font-medium whitespace-normal break-words">{company.name}</TableCell>
                  <TableCell className="text-oe-gray-mid whitespace-normal break-words">
                    {company.website ?? "\u2014"}
                  </TableCell>
                  <TableCell className="whitespace-normal break-words">{company.contactCount}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
