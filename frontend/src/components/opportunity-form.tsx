"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { X } from "lucide-react";
import {
  Button,
  Input,
  TagMultiSelect,
  Label,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import {
  createOpportunity,
  updateOpportunity,
  getCompaniesForSelect,
  getContacts,
  getUserOptions,
  getCurrentUser,
  getTags,
} from "@/lib/api";
import type {
  OpportunityDto,
  OpportunityCreateDto,
  OpportunityUpdateDto,
  OpportunityStatus,
  CompanyDto,
  ContactDto,
  UserOptionDto,
} from "@/lib/types";
import { STAGE_OPTIONS, PRODUCT_OPTIONS } from "@/lib/opportunity-options";

interface OpportunityFormProps {
  readonly opportunity?: OpportunityDto;
}

function contactName(c: ContactDto): string {
  return `${c.title ? c.title + " " : ""}${c.firstName} ${c.lastName}`.trim();
}

export function OpportunityForm({ opportunity }: OpportunityFormProps) {
  const t = useTranslations();
  const S = t.opportunities.form;
  const router = useRouter();
  const isEdit = !!opportunity;

  const [title, setTitle] = useState(opportunity?.title ?? "");
  const [stage, setStage] = useState(opportunity?.stage ?? "");
  const [status, setStatus] = useState<OpportunityStatus>(opportunity?.status ?? "OPEN");
  const [product, setProduct] = useState(opportunity?.product ?? "");
  const [value, setValue] = useState(
    opportunity?.estimatedValue !== null && opportunity?.estimatedValue !== undefined
      ? String(opportunity.estimatedValue)
      : "",
  );
  const [companyId, setCompanyId] = useState(opportunity?.companyId ?? "");
  const [mainContactId, setMainContactId] = useState(opportunity?.mainContactId ?? "");
  const [additionalContactIds, setAdditionalContactIds] = useState<string[]>([
    ...(opportunity?.additionalContactIds ?? []),
  ]);
  const [ownerId, setOwnerId] = useState(opportunity?.owner.id ?? "");
  const [tagIds, setTagIds] = useState<string[]>([...(opportunity?.tagIds ?? [])]);

  const [companies, setCompanies] = useState<CompanyDto[]>([]);
  const [contacts, setContacts] = useState<ContactDto[]>([]);
  const [owners, setOwners] = useState<UserOptionDto[]>([]);

  const [titleError, setTitleError] = useState<string | null>(null);
  const [companyError, setCompanyError] = useState<string | null>(null);
  const [mainContactError, setMainContactError] = useState<string | null>(null);
  const [valueError, setValueError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getCompaniesForSelect()
      .then(setCompanies)
      .catch(() => {});
    getContacts({ size: 1000 })
      .then((p) => setContacts([...p.content]))
      .catch(() => {});
    getUserOptions()
      .then(setOwners)
      .catch(() => {});
  }, []);

  // On create, pre-select the current user as owner (spec 114) once options load.
  useEffect(() => {
    if (isEdit || ownerId) return;
    getCurrentUser()
      .then((user) => setOwnerId((prev) => prev || user.id))
      .catch(() => {});
  }, [isEdit, ownerId]);

  const contactsById = useMemo(() => new Map(contacts.map((c) => [c.id, c])), [contacts]);

  const mainContactMismatch = useMemo(() => {
    if (!companyId || !mainContactId) return false;
    const c = contactsById.get(mainContactId);
    return c !== undefined && c.companyId !== companyId;
  }, [companyId, mainContactId, contactsById]);

  const additionalContactMismatch = useMemo(() => {
    if (!companyId) return false;
    return additionalContactIds.some((id) => {
      const c = contactsById.get(id);
      return c !== undefined && c.companyId !== companyId;
    });
  }, [companyId, additionalContactIds, contactsById]);

  const additionalOptions = useMemo(
    () => contacts.filter((c) => c.id !== mainContactId && !additionalContactIds.includes(c.id)),
    [contacts, mainContactId, additionalContactIds],
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setTitleError(null);
    setCompanyError(null);
    setMainContactError(null);
    setValueError(null);
    setApiError(null);

    let hasError = false;
    if (!title.trim()) {
      setTitleError(S.titleRequired);
      hasError = true;
    }
    if (!companyId) {
      setCompanyError(S.companyRequired);
      hasError = true;
    }
    if (!mainContactId) {
      setMainContactError(S.mainContactRequired);
      hasError = true;
    }

    let estimatedValue: number | null = null;
    const raw = value.trim();
    if (raw !== "") {
      const normalized = raw.replace(",", ".");
      if (!/^-?\d+(\.\d{1,2})?$/.test(normalized)) {
        setValueError(S.valueInvalid);
        hasError = true;
      } else {
        const n = Number(normalized);
        if (n < 0) {
          setValueError(S.valueNegative);
          hasError = true;
        } else {
          estimatedValue = n;
        }
      }
    }

    if (hasError) return;

    const payload: OpportunityCreateDto & OpportunityUpdateDto = {
      title: title.trim(),
      stage: stage.trim() || null,
      status,
      product: product.trim() || null,
      estimatedValue,
      companyId,
      mainContactId,
      additionalContactIds,
      ownerId,
      tagIds,
    };

    setSubmitting(true);
    try {
      const result = isEdit
        ? await updateOpportunity(opportunity.id, payload)
        : await createOpportunity(payload);
      router.push(`/opportunities/${result.id}`);
    } catch {
      setApiError(S.errorGeneric);
    } finally {
      setSubmitting(false);
    }
  };

  const cancelHref = isEdit ? `/opportunities/${opportunity.id}` : "/opportunities";

  return (
    <Card className="mx-auto max-w-2xl border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-xl text-oe-dark">
          {isEdit ? S.editTitle : S.createTitle}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="title">{S.title} *</Label>
            <Input
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder={S.titlePlaceholder}
              className={titleError ? "border-oe-red" : ""}
            />
            {titleError && <p className="mt-1 text-sm text-oe-red">{titleError}</p>}
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="stage">{S.stage}</Label>
              <Input
                id="stage"
                list="opportunity-stage-options"
                value={stage}
                onChange={(e) => setStage(e.target.value)}
                placeholder={S.stagePlaceholder}
              />
              <datalist id="opportunity-stage-options">
                {STAGE_OPTIONS.map((s) => (
                  <option key={s} value={s} />
                ))}
              </datalist>
            </div>
            <div>
              <Label htmlFor="status">{S.status}</Label>
              <Select value={status} onValueChange={(v) => setStatus(v as OpportunityStatus)}>
                <SelectTrigger id="status">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="OPEN">{t.opportunities.status.open}</SelectItem>
                  <SelectItem value="WON">{t.opportunities.status.won}</SelectItem>
                  <SelectItem value="LOST">{t.opportunities.status.lost}</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="product">{S.product}</Label>
              <Input
                id="product"
                list="opportunity-product-options"
                value={product}
                onChange={(e) => setProduct(e.target.value)}
                placeholder={S.productPlaceholder}
              />
              <datalist id="opportunity-product-options">
                {PRODUCT_OPTIONS.map((p) => (
                  <option key={p} value={p} />
                ))}
              </datalist>
            </div>
            <div>
              <Label htmlFor="value">{S.value}</Label>
              <div className="flex items-center gap-2">
                <Input
                  id="value"
                  inputMode="decimal"
                  value={value}
                  onChange={(e) => setValue(e.target.value)}
                  placeholder={S.valuePlaceholder}
                  className={valueError ? "border-oe-red" : ""}
                />
                <span className="text-oe-gray-mid">€</span>
              </div>
              {valueError && <p className="mt-1 text-sm text-oe-red">{valueError}</p>}
            </div>
          </div>

          <div>
            <Label htmlFor="company">{S.company} *</Label>
            <Select value={companyId} onValueChange={setCompanyId}>
              <SelectTrigger id="company" className={companyError ? "border-oe-red" : ""}>
                <SelectValue placeholder={S.selectCompany} />
              </SelectTrigger>
              <SelectContent>
                {companies.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {companyError && <p className="mt-1 text-sm text-oe-red">{companyError}</p>}
          </div>

          <div>
            <Label htmlFor="mainContact">{S.mainContact} *</Label>
            <Select value={mainContactId} onValueChange={setMainContactId}>
              <SelectTrigger id="mainContact" className={mainContactError ? "border-oe-red" : ""}>
                <SelectValue placeholder={S.selectMainContact} />
              </SelectTrigger>
              <SelectContent>
                {contacts.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {contactName(c)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {mainContactError && <p className="mt-1 text-sm text-oe-red">{mainContactError}</p>}
            {mainContactMismatch && (
              <p className="mt-1 text-sm text-oe-yellow-dark">{S.contactMismatchWarning}</p>
            )}
          </div>

          <div>
            <Label>{S.additionalContacts}</Label>
            {additionalContactIds.length > 0 && (
              <ul className="mb-2 flex flex-wrap gap-2">
                {additionalContactIds.map((id) => {
                  const c = contactsById.get(id);
                  return (
                    <li
                      key={id}
                      className="inline-flex items-center gap-1 rounded border border-oe-gray-light bg-oe-gray-light/30 px-2 py-0.5 text-sm"
                    >
                      <span>{c ? contactName(c) : id}</span>
                      <button
                        type="button"
                        className="text-oe-red hover:text-oe-red-dark"
                        onClick={() =>
                          setAdditionalContactIds((prev) => prev.filter((x) => x !== id))
                        }
                        aria-label={S.cancel}
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
            <Select
              value=""
              onValueChange={(id) => setAdditionalContactIds((prev) => [...prev, id])}
            >
              <SelectTrigger>
                <SelectValue placeholder={S.addContact} />
              </SelectTrigger>
              <SelectContent>
                {additionalOptions.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {contactName(c)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {additionalContactMismatch && (
              <p className="mt-1 text-sm text-oe-yellow-dark">{S.contactMismatchWarning}</p>
            )}
          </div>

          <div>
            <Label htmlFor="owner">{S.owner}</Label>
            <Select value={ownerId} onValueChange={setOwnerId}>
              <SelectTrigger id="owner">
                <SelectValue placeholder={S.selectOwner} />
              </SelectTrigger>
              <SelectContent>
                {owners.map((o) => (
                  <SelectItem key={o.id} value={o.id}>
                    {o.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>{t.tags.label}</Label>
            <TagMultiSelect
              selectedIds={tagIds}
              onChange={(ids) => setTagIds(ids)}
              loadTags={async () => {
                const result = await getTags({ size: 1000 });
                return result.content.map((tag) => ({
                  value: tag.id,
                  label: tag.name,
                  color: tag.color,
                }));
              }}
              translations={{ placeholder: t.tags.label + "...", empty: t.tags.empty }}
            />
          </div>

          {apiError && <p className="text-sm text-oe-red">{apiError}</p>}

          <div className="flex gap-3 pt-4">
            <Button type="submit" disabled={submitting}>
              {S.save}
            </Button>
            <Button type="button" variant="outline" onClick={() => router.push(cancelHref)}>
              {S.cancel}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
