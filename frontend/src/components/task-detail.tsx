"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Pencil, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import { DetailField } from "@/components/detail-field";
import { TagChips } from "@/components/tag-chips";
import { deleteTask } from "@/lib/api";
import type { TaskDto, TaskStatus } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

const STATUS_BADGE_CLASSES: Record<TaskStatus, string> = {
  OPEN: "bg-blue-100 text-blue-800",
  IN_PROGRESS: "bg-yellow-100 text-yellow-800",
  DONE: "bg-green-100 text-green-800",
};

export function TaskDetail({ task }: { readonly task: TaskDto }) {
  const t = useTranslations();
  const S = t.tasks;
  const router = useRouter();
  const [deleteOpen, setDeleteOpen] = useState(false);

  const handleDelete = async () => {
    try {
      await deleteTask(task.id);
      router.push("/tasks");
    } catch {
      console.error("Failed to delete task");
    }
  };

  function formatDate(dateStr: string): string {
    try {
      return new Date(dateStr).toLocaleDateString();
    } catch {
      return dateStr;
    }
  }

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{task.action}</h1>
        <div className="flex gap-2">
          <Button asChild variant="outline">
            <Link href={`/tasks/${task.id}/edit`}>
              <Pencil className="mr-2 h-4 w-4" />
              {S.edit}
            </Link>
          </Button>
          <Button
            variant="outline"
            className="text-oe-red border-oe-red hover:bg-oe-red-lighter"
            onClick={() => setDeleteOpen(true)}
          >
            <Trash2 className="mr-2 h-4 w-4" />
            {S.deleteDialog.confirm}
          </Button>
        </div>
      </div>

      <Card className="border-oe-gray-light">
        <CardHeader>
          <CardTitle className="font-heading text-lg text-oe-dark">{S.columns.status}</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-4 sm:grid-cols-2">
            <DetailField label={S.fields.status} value={null}>
              <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_BADGE_CLASSES[task.status]}`}>
                {S.status[task.status]}
              </span>
            </DetailField>
            <DetailField label={S.fields.dueDate} value={formatDate(task.dueDate)} />
            {task.companyId && task.companyName && (
              <DetailField label={S.fields.company} value={null}>
                <Link href={`/companies/${task.companyId}`} className="text-oe-blue hover:underline">
                  {task.companyName}
                </Link>
              </DetailField>
            )}
            {task.contactId && task.contactName && (
              <DetailField label={S.fields.contact} value={null}>
                <Link href={`/contacts/${task.contactId}`} className="text-oe-blue hover:underline">
                  {task.contactName}
                </Link>
              </DetailField>
            )}
          </dl>
        </CardContent>
      </Card>

      <TagChips tagIds={task.tagIds} />

      <div className="mt-4">
        <h3 className="text-sm font-medium text-oe-gray-mid">{S.fields.action}</h3>
        <p className="mt-1 text-sm text-oe-black whitespace-pre-line">{task.action}</p>
      </div>

      <DeleteConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title={S.deleteDialog.title}
        description={S.deleteDialog.description}
        confirmLabel={S.deleteDialog.confirm}
        cancelLabel={S.deleteDialog.cancel}
        onConfirm={handleDelete}
      />
    </div>
  );
}
