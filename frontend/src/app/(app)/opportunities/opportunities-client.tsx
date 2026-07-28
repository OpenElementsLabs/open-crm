"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Plus, Pencil, MessageSquarePlus, User } from "lucide-react";
import {
  Button,
  Input,
  TagMultiSelect,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Skeleton,
  TablePagination,
  TooltipIconButton,
} from "@open-elements/ui";
import { AddCommentDialog } from "@open-elements/nextjs-app-layer";
import { useTranslations, useLanguage } from "@/lib/i18n";
import {
  getOpportunities,
  getCompaniesForSelect,
  createOpportunityComment,
  getTags,
} from "@/lib/api";
import type { OpportunityDto, CompanyDto, OpportunityStatus, Page } from "@/lib/types";
import { STAGE_OPTIONS } from "@/lib/opportunity-options";
import { formatEur } from "@/lib/opportunity-format";
import { OpportunityStatusBadge, statusLabel } from "@/components/opportunity-status-badge";

export function OpportunitiesClient() {
  const t = useTranslations();
  const S = t.opportunities;
  const { language } = useLanguage();
  const router = useRouter();
  const searchParams = useSearchParams();

  const [data, setData] = useState<Page<OpportunityDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(() => {
    if (typeof window === "undefined") return 20;
    const stored = localStorage.getItem("pageSize.opportunities");
    const parsed = Number(stored);
    if ([10, 20, 50, 100, 200].includes(parsed)) return parsed;
    return 20;
  });
  const [searchFilter, setSearchFilter] = useState(searchParams.get("search") ?? "");
  const [statusFilter, setStatusFilter] = useState(searchParams.get("status") ?? "all");
  const [stageFilter, setStageFilter] = useState("all");
  const [companyIdFilter, setCompanyIdFilter] = useState(searchParams.get("companyId") ?? "all");
  const [tagIds, setTagIds] = useState<string[]>(() => {
    const param = searchParams.get("tagIds");
    return param ? param.split(",") : [];
  });

  const [companies, setCompanies] = useState<CompanyDto[]>([]);
  const [commentTarget, setCommentTarget] = useState<OpportunityDto | null>(null);
  const [commentSending, setCommentSending] = useState(false);

  useEffect(() => {
    getCompaniesForSelect()
      .then(setCompanies)
      .catch(() => {});
  }, []);

  const fetchOpportunities = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getOpportunities({
        page,
        size: pageSize,
        search: searchFilter || undefined,
        status: statusFilter !== "all" ? (statusFilter as OpportunityStatus) : undefined,
        stage: stageFilter !== "all" ? stageFilter : undefined,
        companyId: companyIdFilter !== "all" ? companyIdFilter : undefined,
        tagIds: tagIds.length > 0 ? tagIds : undefined,
      });
      setData(result);
    } catch {
      console.error("Failed to fetch opportunities");
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, searchFilter, statusFilter, stageFilter, companyIdFilter, tagIds]);

  useEffect(() => {
    fetchOpportunities();
  }, [fetchOpportunities]);

  useEffect(() => {
    setPage(0);
  }, [searchFilter, statusFilter, stageFilter, companyIdFilter, tagIds]);

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{S.title}</h1>
        <Button asChild>
          <Link href="/opportunities/new">
            <Plus className="mr-2 h-4 w-4" />
            {S.newOpportunity}
          </Link>
        </Button>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
        <Input
          placeholder={S.filter.search}
          value={searchFilter}
          onChange={(e) => setSearchFilter(e.target.value)}
          className="sm:max-w-[200px]"
        />
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="sm:max-w-[180px]">
            <SelectValue placeholder={S.filter.status} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{S.filter.allStatuses}</SelectItem>
            <SelectItem value="OPEN">{S.status.open}</SelectItem>
            <SelectItem value="WON">{S.status.won}</SelectItem>
            <SelectItem value="LOST">{S.status.lost}</SelectItem>
          </SelectContent>
        </Select>
        <Select value={stageFilter} onValueChange={setStageFilter}>
          <SelectTrigger className="sm:max-w-[180px]">
            <SelectValue placeholder={S.filter.stage} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{S.filter.allStages}</SelectItem>
            {STAGE_OPTIONS.map((stage) => (
              <SelectItem key={stage} value={stage}>
                {stage}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
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
        <div className="sm:max-w-[250px]">
          <TagMultiSelect
            selectedIds={tagIds}
            onChange={(ids) => {
              setTagIds(ids);
              const params = new URLSearchParams(window.location.search);
              if (ids.length > 0) {
                params.set("tagIds", ids.join(","));
              } else {
                params.delete("tagIds");
              }
              const query = params.toString();
              router.replace(`/opportunities${query ? `?${query}` : ""}`, { scroll: false });
            }}
            loadTags={async () => {
              const result = await getTags({ size: 1000 });
              return result.content.map((tag) => ({ value: tag.id, label: tag.name, color: tag.color }));
            }}
            translations={{ placeholder: t.tags.label + "...", empty: t.tags.empty }}
          />
        </div>
      </div>

      {loading && (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      )}

      {!loading && data && data.content.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <p className="mb-4 text-oe-gray-mid">{S.empty}</p>
          <Button asChild>
            <Link href="/opportunities/new">
              <Plus className="mr-2 h-4 w-4" />
              {S.newOpportunity}
            </Link>
          </Button>
        </div>
      )}

      {!loading && data && data.content.length > 0 && (
        <>
          <div className="overflow-x-auto rounded-md border border-oe-gray-light">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{S.columns.title}</TableHead>
                  <TableHead>{S.columns.company}</TableHead>
                  <TableHead>{S.columns.mainContact}</TableHead>
                  <TableHead>{S.columns.stage}</TableHead>
                  <TableHead>{S.columns.status}</TableHead>
                  <TableHead className="text-right">{S.columns.value}</TableHead>
                  <TableHead>{S.columns.owner}</TableHead>
                  <TableHead className="w-[110px] text-right">{S.columns.actions}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((opp) => (
                  <TableRow key={opp.id}>
                    <TableCell className="font-medium">
                      <Link href={`/opportunities/${opp.id}`} className="text-oe-green underline hover:text-oe-green-dark">
                        {opp.title}
                      </Link>
                    </TableCell>
                    <TableCell className="text-oe-gray-mid">
                      {opp.companyId && opp.companyName ? (
                        <Link href={`/companies/${opp.companyId}`} className="underline hover:text-oe-green">
                          {opp.companyName}
                        </Link>
                      ) : (
                        "—"
                      )}
                    </TableCell>
                    <TableCell className="text-oe-gray-mid">
                      {opp.mainContactId && opp.mainContactName ? (
                        <Link href={`/contacts/${opp.mainContactId}`} className="underline hover:text-oe-green">
                          {opp.mainContactName}
                        </Link>
                      ) : (
                        "—"
                      )}
                    </TableCell>
                    <TableCell className="text-oe-gray-mid">{opp.stage || "—"}</TableCell>
                    <TableCell>
                      <OpportunityStatusBadge status={opp.status} label={statusLabel(opp.status, S.status)} />
                    </TableCell>
                    <TableCell className="text-right text-oe-gray-mid">
                      {formatEur(opp.estimatedValue, language) ?? "—"}
                    </TableCell>
                    <TableCell>
                      <span className="inline-flex items-center gap-2">
                        {opp.owner.avatarUrl ? (
                          <img src={opp.owner.avatarUrl} alt={opp.owner.name} className="h-6 w-6 rounded-full object-cover" />
                        ) : (
                          <User className="h-6 w-6 text-oe-gray-mid" />
                        )}
                        <span>{opp.owner.name}</span>
                      </span>
                    </TableCell>
                    <TableCell className="text-right">
                      <TooltipIconButton
                        icon={<Pencil />}
                        tooltip={S.detail.edit}
                        onClick={() => router.push(`/opportunities/${opp.id}/edit`)}
                      />
                      <TooltipIconButton
                        icon={<MessageSquarePlus />}
                        tooltip={t.companies.comments.addTitle}
                        onClick={() => setCommentTarget(opp)}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <TablePagination
            page={page}
            pageSize={pageSize}
            totalElements={data.page.totalElements}
            totalPages={data.page.totalPages}
            pageSizeOptions={[10, 20, 50, 100, 200]}
            storageKey="pageSize.opportunities"
            translations={S.pagination}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        </>
      )}

      <AddCommentDialog
        open={commentTarget !== null}
        onOpenChange={(open) => {
          if (!open) setCommentTarget(null);
        }}
        onSubmit={async (text) => {
          setCommentSending(true);
          try {
            await createOpportunityComment(commentTarget!.id, { text });
            setCommentTarget(null);
          } finally {
            setCommentSending(false);
          }
        }}
        sending={commentSending}
        title={t.companies.comments.addTitle}
        placeholder={t.companies.comments.placeholder}
        sendLabel={t.companies.comments.send}
        sendingLabel={t.companies.comments.sending}
        errorTitle={t.companies.comments.errorTitle}
        errorMessage={t.companies.comments.errorGeneric}
      />
    </div>
  );
}
