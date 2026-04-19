import type { Language } from "@open-elements/ui";
import { useTranslations as useTranslationsGeneric, useLanguage } from "@open-elements/ui";
import { de } from "./de";
import { en } from "./en";

type DeepStringify<T> = {
  readonly [K in keyof T]: T[K] extends string ? string : DeepStringify<T[K]>;
};

export type Translations = DeepStringify<typeof de>;

export const translations: Record<Language, Translations> = {
  de,
  en,
};

export function useTranslations(): Translations {
  return useTranslationsGeneric<Translations>();
}

export { useLanguage };
export { de, en };
