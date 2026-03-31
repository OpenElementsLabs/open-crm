"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { TagMultiSelect } from "@/components/tag-multi-select";
import {
  createTask,
  updateTask,
  getCompaniesForSelect,
  getContactsForSelect,
} from "@/lib/api";
import type { TaskDto, TaskCreateDto, TaskUpdateDto, TaskStatus, CompanyDto, ContactDto } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

type EntityType = "company" | "contact";

interface TaskFormProps {
  readonly task?: TaskDto;
}

export function TaskForm({ task }: TaskFormProps) {
  const t = useTranslations();
  const S = t.tasks;
  const router = useRouter();
  const isEdit = !!task;

  const [action, setAction] = useState(task?.action ?? "");
  const [dueDate, setDueDate] = useState(task?.dueDate ?? "");
  const [status, setStatus] = useState<TaskStatus>(task?.status ?? "OPEN");
  const [entityType, setEntityType] = useState<EntityType>(
    task?.contactId ? "contact" : "company",
  );
  const [companyId, setCompanyId] = useState(task?.companyId ?? "");
  const [contactId, setContactId] = useState(task?.contactId ?? "");
  const [tagIds, setTagIds] = useState<string[]>([...(task?.tagIds ?? [])]);
  const [tagIdsChanged, setTagIdsChanged] = useState(false);

  const [companies, setCompanies] = useState<CompanyDto[]>([]);
  const [contacts, setContacts] = useState<ContactDto[]>([]);

  const [actionError, setActionError] = useState<string | null>(null);
  const [dueDateError, setDueDateError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getCompaniesForSelect()
      .then(setCompanies)
      .catch(() => {});
    getContactsForSelect()
      .then(setContacts)
      .catch(() => {});
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionError(null);
    setDueDateError(null);
    setApiError(null);

    let hasError = false;

    if (!action.trim()) {
      setActionError(S.form.actionRequired);
      hasError = true;
    }
    if (!dueDate) {
      setDueDateError(S.form.dueDateRequired);
      hasError = true;
    }

    if (hasError) return;

    setSubmitting(true);
    try {
      let result: TaskDto;
      if (isEdit) {
        const data: TaskUpdateDto = {
          action: action.trim(),
          dueDate,
          status,
          ...(tagIdsChanged ? { tagIds } : {}),
        };
        result = await updateTask(task.id, data);
      } else {
        const data: TaskCreateDto = {
          action: action.trim(),
          dueDate,
          status,
          companyId: entityType === "company" && companyId && companyId !== "none" ? companyId : null,
          contactId: entityType === "contact" && contactId && contactId !== "none" ? contactId : null,
          ...(tagIdsChanged ? { tagIds } : {}),
        };
        result = await createTask(data);
      }
      router.push(`/tasks/${result.id}`);
    } catch {
      setApiError(S.form.errorGeneric);
    } finally {
      setSubmitting(false);
    }
  };

  const cancelHref = isEdit ? `/tasks/${task.id}` : "/tasks";

  return (
    <Card className="mx-auto max-w-2xl border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-xl text-oe-dark">
          {isEdit ? S.edit : S.new}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="action">{S.fields.action} *</Label>
            <Textarea
              id="action"
              value={action}
              onChange={(e) => setAction(e.target.value)}
              placeholder={S.fields.actionPlaceholder}
              rows={4}
              className={actionError ? "border-oe-red" : ""}
            />
            {actionError && <p className="mt-1 text-sm text-oe-red">{actionError}</p>}
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="dueDate">{S.fields.dueDate} *</Label>
              <Input
                id="dueDate"
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className={dueDateError ? "border-oe-red" : ""}
              />
              {dueDateError && <p className="mt-1 text-sm text-oe-red">{dueDateError}</p>}
            </div>
            <div>
              <Label htmlFor="status">{S.fields.status}</Label>
              <Select value={status} onValueChange={(v) => setStatus(v as TaskStatus)}>
                <SelectTrigger id="status">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="OPEN">{S.status.OPEN}</SelectItem>
                  <SelectItem value="IN_PROGRESS">{S.status.IN_PROGRESS}</SelectItem>
                  <SelectItem value="DONE">{S.status.DONE}</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Entity type selection */}
          <div>
            <Label>{S.fields.entityType}</Label>
            <div className="mt-2 flex gap-4">
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="radio"
                  name="entityType"
                  value="company"
                  checked={entityType === "company"}
                  onChange={() => { setEntityType("company"); setContactId(""); }}
                  disabled={isEdit}
                  className="accent-oe-green"
                />
                {S.fields.company}
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="radio"
                  name="entityType"
                  value="contact"
                  checked={entityType === "contact"}
                  onChange={() => { setEntityType("contact"); setCompanyId(""); }}
                  disabled={isEdit}
                  className="accent-oe-green"
                />
                {S.fields.contact}
              </label>
            </div>
          </div>

          {/* Entity dropdown */}
          {entityType === "company" && (
            <div>
              <Label htmlFor="company">{S.fields.company}</Label>
              <Select
                value={companyId || "none"}
                onValueChange={setCompanyId}
                disabled={isEdit}
              >
                <SelectTrigger id="company">
                  <SelectValue placeholder={S.fields.noCompany} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">{S.fields.noCompany}</SelectItem>
                  {companies.map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {entityType === "contact" && (
            <div>
              <Label htmlFor="contact">{S.fields.contact}</Label>
              <Select
                value={contactId || "none"}
                onValueChange={setContactId}
                disabled={isEdit}
              >
                <SelectTrigger id="contact">
                  <SelectValue placeholder={S.fields.noContact} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">{S.fields.noContact}</SelectItem>
                  {contacts.map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.firstName} {c.lastName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <div className="space-y-2">
            <Label>{t.tags.label}</Label>
            <TagMultiSelect
              selectedIds={tagIds}
              onChange={(ids) => { setTagIds(ids); setTagIdsChanged(true); }}
            />
          </div>

          {apiError && <p className="text-sm text-oe-red">{apiError}</p>}

          <div className="flex gap-3 pt-4">
            <Button
              type="submit"
              disabled={submitting}
              className="bg-oe-green hover:bg-oe-green-dark text-white"
            >
              {S.form.save}
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push(cancelHref)}
            >
              {S.form.cancel}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
