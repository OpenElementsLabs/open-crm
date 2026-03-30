"use client";

import { useEffect, useState } from "react";
import { getTags } from "@/lib/api";
import { useTranslations } from "@/lib/i18n/language-context";
import type { TagDto } from "@/lib/types";
import {
  Combobox,
  ComboboxChips,
  ComboboxChip,
  ComboboxChipsInput,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxItem,
  ComboboxList,
  useComboboxAnchor,
} from "@/components/ui/combobox";

interface TagMultiSelectProps {
  readonly selectedIds: readonly string[];
  readonly onChange: (ids: string[]) => void;
}

function isValidHex(color: string): boolean {
  return /^#[0-9A-Fa-f]{6}$/.test(color);
}

function getContrastColor(hex: string): string {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.5 ? "#1A1A1A" : "#FFFFFF";
}

export function TagMultiSelect({ selectedIds, onChange }: TagMultiSelectProps) {
  const t = useTranslations();
  const [allTags, setAllTags] = useState<TagDto[]>([]);
  const anchorRef = useComboboxAnchor();

  useEffect(() => {
    getTags({ size: 1000 })
      .then((result) => setAllTags([...result.content]))
      .catch(() => {});
  }, []);

  const selectedTags = allTags.filter((tag) => selectedIds.includes(tag.id));

  return (
    <Combobox
      multiple
      value={[...selectedIds]}
      onValueChange={(ids: string[]) => onChange(ids)}
    >
      <ComboboxChips ref={anchorRef}>
        {selectedTags.map((tag) => {
          const bgColor = isValidHex(tag.color) ? tag.color : "#6B7280";
          const textColor = isValidHex(tag.color) ? getContrastColor(tag.color) : "#FFFFFF";
          return (
            <ComboboxChip
              key={tag.id}
              style={{ backgroundColor: bgColor, color: textColor }}
            >
              {tag.name}
            </ComboboxChip>
          );
        })}
        <ComboboxChipsInput placeholder={`${t.tags.label}...`} />
      </ComboboxChips>
      <ComboboxContent anchor={anchorRef}>
        <ComboboxEmpty>{t.tags.empty}</ComboboxEmpty>
        <ComboboxList>
          {allTags.map((tag) => (
            <ComboboxItem key={tag.id} value={tag.id}>
              <span
                className="inline-block h-4 w-4 rounded-full shrink-0"
                style={{ backgroundColor: isValidHex(tag.color) ? tag.color : "#6B7280" }}
              />
              {tag.name}
            </ComboboxItem>
          ))}
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  );
}
