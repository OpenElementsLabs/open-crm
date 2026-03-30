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

interface TagOption {
  readonly value: string;
  readonly label: string;
  readonly color: string;
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

  const tagOptions: TagOption[] = allTags.map((tag) => ({
    value: tag.id,
    label: tag.name,
    color: tag.color,
  }));

  const selectedOptions = tagOptions.filter((opt) => selectedIds.includes(opt.value));
  const selectedValues = selectedOptions;

  return (
    <Combobox
      multiple
      value={selectedValues}
      onValueChange={(options: TagOption[]) => onChange(options.map((o) => o.value))}
    >
      <ComboboxChips ref={anchorRef}>
        {selectedOptions.map((opt) => {
          const bgColor = isValidHex(opt.color) ? opt.color : "#6B7280";
          const textColor = isValidHex(opt.color) ? getContrastColor(opt.color) : "#FFFFFF";
          return (
            <ComboboxChip
              key={opt.value}
              style={{ backgroundColor: bgColor, color: textColor }}
            >
              {opt.label}
            </ComboboxChip>
          );
        })}
        <ComboboxChipsInput placeholder={`${t.tags.label}...`} />
      </ComboboxChips>
      <ComboboxContent anchor={anchorRef}>
        <ComboboxEmpty>{t.tags.empty}</ComboboxEmpty>
        <ComboboxList>
          {tagOptions.map((opt) => (
            <ComboboxItem key={opt.value} value={opt}>
              <span
                className="inline-block h-4 w-4 rounded-full shrink-0"
                style={{ backgroundColor: isValidHex(opt.color) ? opt.color : "#6B7280" }}
              />
              {opt.label}
            </ComboboxItem>
          ))}
        </ComboboxList>
      </ComboboxContent>
    </Combobox>
  );
}
