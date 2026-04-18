"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button, Input, Textarea, cn } from "@open-elements/ui";
import type { TagDto } from "@open-elements/ui";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { createTag, updateTag } from "@/lib/api";
import { useTranslations } from "@/lib/i18n/language-context";
import { Check } from "lucide-react";

const PALETTE_COLORS = [
  "#EF4444", "#F97316", "#F59E0B", "#EAB308",
  "#84CC16", "#22C55E", "#14B8A6", "#06B6D4",
  "#3B82F6", "#6366F1", "#A855F7", "#EC4899",
];

const HEX_REGEX = /^#[0-9A-Fa-f]{6}$/;

function getContrastColor(hex: string): string {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.5 ? "#1A1A1A" : "#FFFFFF";
}

interface TagFormProps {
  readonly tag?: TagDto;
}

export function TagForm({ tag }: TagFormProps) {
  const t = useTranslations();
  const router = useRouter();
  const isEdit = !!tag;

  const [name, setName] = useState(tag?.name ?? "");
  const [description, setDescription] = useState(tag?.description ?? "");
  const [color, setColor] = useState(tag?.color ?? "");
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<{ name?: string; color?: string }>({});

  function validate(): boolean {
    const newErrors: { name?: string; color?: string } = {};
    if (!name.trim()) {
      newErrors.name = t.tags.form.nameRequired;
    }
    if (!color.trim()) {
      newErrors.color = t.tags.form.colorRequired;
    } else if (!HEX_REGEX.test(color)) {
      newErrors.color = t.tags.form.colorInvalid;
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    try {
      const data = { name: name.trim(), description: description.trim() || null, color };
      if (isEdit) {
        await updateTag(tag.id, data);
      } else {
        await createTag(data);
      }
      router.push("/tags");
    } catch (err) {
      if (err instanceof Error && err.message === "CONFLICT") {
        setErrors({ name: t.tags.form.nameConflict });
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-6 font-heading text-2xl font-bold text-oe-dark">
        {isEdit ? t.tags.editTag : t.tags.createTag}
      </h1>
      <Card>
        <CardHeader>
          <CardTitle>{isEdit ? t.tags.editTag : t.tags.createTag}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="name">{t.tags.form.name} *</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => { setName(e.target.value); setErrors((prev) => ({ ...prev, name: undefined })); }}
                placeholder={t.tags.form.namePlaceholder}
              />
              {errors.name && <p className="text-sm text-oe-red">{errors.name}</p>}
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">{t.tags.form.description}</Label>
              <Textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder={t.tags.form.descriptionPlaceholder}
                rows={3}
              />
            </div>

            <div className="space-y-2">
              <Label>{t.tags.form.color} *</Label>
              <div className="flex flex-wrap gap-2 mb-3">
                {PALETTE_COLORS.map((c) => (
                  <button
                    key={c}
                    type="button"
                    className={cn(
                      "h-8 w-8 rounded-full border-2 flex items-center justify-center transition-all",
                      color.toUpperCase() === c.toUpperCase()
                        ? "border-oe-dark scale-110"
                        : "border-transparent hover:border-oe-gray-light",
                    )}
                    style={{ backgroundColor: c }}
                    onClick={() => { setColor(c); setErrors((prev) => ({ ...prev, color: undefined })); }}
                  >
                    {color.toUpperCase() === c.toUpperCase() && (
                      <Check className="h-4 w-4" style={{ color: getContrastColor(c) }} />
                    )}
                  </button>
                ))}
              </div>
              <div className="flex items-center gap-3">
                <Input
                  id="color"
                  value={color}
                  onChange={(e) => { setColor(e.target.value); setErrors((prev) => ({ ...prev, color: undefined })); }}
                  placeholder={t.tags.form.colorPlaceholder}
                  className="max-w-40"
                />
                {HEX_REGEX.test(color) && (
                  <span
                    className="inline-block h-8 w-8 rounded-full border border-oe-gray-light"
                    style={{ backgroundColor: color }}
                  />
                )}
              </div>
              {errors.color && <p className="text-sm text-oe-red">{errors.color}</p>}
            </div>

            <div className="flex gap-3 pt-2">
              <Button
                type="submit"
                disabled={submitting}
                className="bg-oe-green hover:bg-oe-green/90 text-white"
              >
                {t.tags.form.save}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => router.push("/tags")}
              >
                {t.tags.form.cancel}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
