"use client";

import { createContext, useContext, useMemo, type ReactNode } from "react";
import { useLanguage } from "@open-elements/ui";
import { de } from "./de";
import { en } from "./en";

type DeepStringify<T> = {
  readonly [K in keyof T]: T[K] extends string ? string : DeepStringify<T[K]>;
};

export type AppLayerTranslations = DeepStringify<typeof de>;

export const appLayerTranslations: { readonly de: AppLayerTranslations; readonly en: AppLayerTranslations } = {
  de,
  en,
};

const AppLayerTranslationContext = createContext<AppLayerTranslations | null>(null);

export function AppLayerTranslationProvider({ children }: { readonly children: ReactNode }) {
  const { language } = useLanguage();
  const value = useMemo(() => {
    const lang = language as keyof typeof appLayerTranslations;
    return appLayerTranslations[lang] ?? appLayerTranslations.en;
  }, [language]);
  return (
    <AppLayerTranslationContext.Provider value={value}>
      {children}
    </AppLayerTranslationContext.Provider>
  );
}

export function useAppLayerTranslations(): AppLayerTranslations {
  const value = useContext(AppLayerTranslationContext);
  if (value === null) {
    throw new Error(
      "useAppLayerTranslations must be used within <AppLayerTranslationProvider>",
    );
  }
  return value;
}
