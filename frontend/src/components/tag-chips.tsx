"use client";

import { useEffect, useState } from "react";
import { getTag } from "@/lib/api";
import { useTranslations } from "@/lib/i18n";
import type { TagDto } from "@open-elements/ui";

function getContrastColor(hex: string): string {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.5 ? "#1A1A1A" : "#FFFFFF";
}

function isValidHex(color: string): boolean {
  return /^#[0-9A-Fa-f]{6}$/.test(color);
}

interface TagChipsProps {
  readonly tagIds: readonly string[];
}

export function TagChips({ tagIds }: TagChipsProps) {
  const t = useTranslations();
  const [tags, setTags] = useState<TagDto[]>([]);

  useEffect(() => {
    if (tagIds.length === 0) return;
    Promise.all(tagIds.map((id) => getTag(id).catch(() => null)))
      .then((results) => setTags(results.filter((r): r is TagDto => r !== null)));
  }, [tagIds]);

  if (tagIds.length === 0 || tags.length === 0) return null;

  return (
    <div className="mt-4">
      <h3 className="mb-2 text-sm font-medium text-oe-gray">{t.tags.label}</h3>
      <div className="flex flex-wrap gap-2">
        {tags.map((tag) => {
          const bgColor = isValidHex(tag.color) ? tag.color : "#6B7280";
          const textColor = isValidHex(tag.color) ? getContrastColor(tag.color) : "#FFFFFF";
          return (
            <span
              key={tag.id}
              className="inline-flex items-center rounded-full px-3 py-1 text-xs font-medium"
              style={{ backgroundColor: bgColor, color: textColor }}
            >
              {tag.name}
            </span>
          );
        })}
      </div>
    </div>
  );
}
