"use client";

import { useEffect, useState } from "react";
import { getTags } from "@/lib/api";
import { useTranslations } from "@/lib/i18n/language-context";
import type { TagDto } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Check, ChevronsUpDown, X } from "lucide-react";
import { cn } from "@/lib/utils";

interface TagMultiSelectProps {
  readonly selectedIds: readonly string[];
  readonly onChange: (ids: string[]) => void;
}

function isValidHex(color: string): boolean {
  return /^#[0-9A-Fa-f]{6}$/.test(color);
}

export function TagMultiSelect({ selectedIds, onChange }: TagMultiSelectProps) {
  const t = useTranslations();
  const [allTags, setAllTags] = useState<TagDto[]>([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    getTags({ size: 1000 })
      .then((result) => setAllTags([...result.content]))
      .catch(() => {});
  }, []);

  function toggle(id: string) {
    if (selectedIds.includes(id)) {
      onChange(selectedIds.filter((sid) => sid !== id));
    } else {
      onChange([...selectedIds, id]);
    }
  }

  function removeTag(id: string) {
    onChange(selectedIds.filter((sid) => sid !== id));
  }

  const selectedTags = allTags.filter((tag) => selectedIds.includes(tag.id));

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className="w-full justify-between h-auto min-h-10"
        >
          <div className="flex flex-wrap gap-1">
            {selectedTags.length === 0 ? (
              <span className="text-muted-foreground">{t.tags.label}...</span>
            ) : (
              selectedTags.map((tag) => (
                <span
                  key={tag.id}
                  className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
                  style={{
                    backgroundColor: isValidHex(tag.color) ? tag.color : "#6B7280",
                    color: isValidHex(tag.color) ? getContrastColor(tag.color) : "#FFF",
                  }}
                >
                  {tag.name}
                  <button
                    type="button"
                    className="ml-0.5 opacity-70 hover:opacity-100"
                    onClick={(e) => { e.stopPropagation(); removeTag(tag.id); }}
                  >
                    <X className="h-3 w-3" />
                  </button>
                </span>
              ))
            )}
          </div>
          <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-full min-w-[200px] p-0" align="start">
        <div className="max-h-60 overflow-y-auto p-1">
          {allTags.length === 0 ? (
            <p className="px-3 py-2 text-sm text-muted-foreground">{t.tags.empty}</p>
          ) : (
            allTags.map((tag) => {
              const isSelected = selectedIds.includes(tag.id);
              return (
                <button
                  key={tag.id}
                  type="button"
                  className={cn(
                    "flex w-full items-center gap-2 rounded-sm px-3 py-2 text-sm hover:bg-accent transition-colors",
                    isSelected && "bg-accent/50",
                  )}
                  onClick={() => toggle(tag.id)}
                >
                  <span
                    className="inline-block h-4 w-4 rounded-full shrink-0"
                    style={{ backgroundColor: isValidHex(tag.color) ? tag.color : "#6B7280" }}
                  />
                  <span className="flex-1 text-left">{tag.name}</span>
                  {isSelected && <Check className="h-4 w-4 text-oe-green" />}
                </button>
              );
            })
          )}
        </div>
      </PopoverContent>
    </Popover>
  );
}

function getContrastColor(hex: string): string {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.5 ? "#1A1A1A" : "#FFFFFF";
}
