import { de } from "./de";
import { en } from "./en";

export type Language = "de" | "en";

type DeepStringify<T> = {
  readonly [K in keyof T]: T[K] extends string ? string : DeepStringify<T[K]>;
};

export type Translations = DeepStringify<typeof de>;

export const translations: Record<Language, Translations> = {
  de,
  en,
};

export { de, en };
