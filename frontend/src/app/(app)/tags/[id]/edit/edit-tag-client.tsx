"use client";

import { useRouter } from "next/navigation";
import { TagForm } from "@open-elements/ui";
import type { TagDto } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { updateTag } from "@/lib/api";

export function EditTagClient({ tag }: { tag: TagDto }) {
  const t = useTranslations();
  const router = useRouter();

  return (
    <TagForm
      tag={tag}
      onSave={async (data) => { await updateTag(tag.id, data); router.push("/tags"); }}
      onCancel={() => router.push("/tags")}
      translations={{
        title: t.tags.editTag,
        name: t.tags.form.name,
        nameRequired: t.tags.form.nameRequired,
        namePlaceholder: t.tags.form.namePlaceholder,
        nameConflict: t.tags.form.nameConflict,
        description: t.tags.form.description,
        descriptionPlaceholder: t.tags.form.descriptionPlaceholder,
        color: t.tags.form.color,
        colorRequired: t.tags.form.colorRequired,
        colorInvalid: t.tags.form.colorInvalid,
        colorPlaceholder: t.tags.form.colorPlaceholder,
        save: t.tags.form.save,
        cancel: t.tags.form.cancel,
      }}
    />
  );
}
