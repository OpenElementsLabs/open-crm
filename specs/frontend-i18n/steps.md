# Implementation Steps: Frontend i18n (DE/EN)

## Step 1: Translation Files and i18n Infrastructure

- [ ] Create `src/lib/i18n/de.ts` — Deutsche Übersetzungen (identisch zur aktuellen `STRINGS`-Struktur)
- [ ] Create `src/lib/i18n/en.ts` — Englische Übersetzungen (gleiche Struktur, englische Texte)
- [ ] Create `src/lib/i18n/index.ts` — `Language` Typ (`"de" | "en"`), `Translations` Typ (abgeleitet von `de`), Sprach-Registry
- [ ] Create `src/lib/i18n/language-context.tsx` — `LanguageProvider`, `useTranslations()` Hook, `useLanguage()` Hook. Liest/schreibt localStorage, erkennt Browser-Sprache, setzt `document.documentElement.lang`

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] `LanguageProvider` und `useTranslations()` sind exportiert und typsicher
- [ ] Beide Übersetzungsdateien haben die exakt gleiche Struktur

**Related behaviors:** None directly — foundation for all i18n steps

---

## Step 2: Language Switch Component and Sidebar Integration

- [ ] Create `src/components/language-switch.tsx` — "DE | EN" Toggle, aktive Sprache grün/fett, inaktive gedämpft
- [ ] Modify `src/components/sidebar.tsx` — `STRINGS` → `useTranslations()`, `LanguageSwitch` am unteren Rand einbauen
- [ ] Modify `src/app/layout.tsx` — `LanguageProvider` um `children` wrappen

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Sprach-Toggle sichtbar in Desktop-Sidebar und Mobile-Menü
- [ ] Aktive Sprache visuell hervorgehoben (oe-green, fett)

**Related behaviors:** Toggle ist in der Desktop-Sidebar sichtbar, Toggle ist im Mobile-Menü sichtbar, Aktive Sprache ist visuell hervorgehoben

---

## Step 3: Migrate All Components to useTranslations

- [ ] Modify `src/components/company-list.tsx` — `STRINGS` → `useTranslations()`
- [ ] Modify `src/components/company-form.tsx` — `STRINGS` → `useTranslations()`
- [ ] Modify `src/components/company-detail.tsx` — `STRINGS` → `useTranslations()`
- [ ] Modify `src/components/health-status.tsx` — `STRINGS` → `useTranslations()`
- [ ] Modify `src/app/health/page.tsx` — hardcodierte Strings umstellen
- [ ] Delete `src/lib/constants.ts` (nicht mehr benötigt)

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Keine Imports von `constants.ts` mehr im Codebase
- [ ] Alle Texte kommen aus `useTranslations()`

**Related behaviors:** Navigation wird übersetzt, Firmen-Liste wird übersetzt, Firmen-Formular wird übersetzt, Firmen-Detail wird übersetzt, Lösch-Dialog wird übersetzt, Fehlermeldungen werden übersetzt, Health-Status wird übersetzt, Leere Zustände werden übersetzt

---

## Step 4: Frontend Tests — Language Detection and Persistence

- [ ] Test: Browser-Sprache `de` → UI auf Deutsch, `html lang="de"`, localStorage gesetzt
- [ ] Test: Browser-Sprache `en` → UI auf Englisch, `html lang="en"`
- [ ] Test: Browser-Sprache `fr` → Fallback Englisch
- [ ] Test: localStorage `"en"` hat Vorrang vor Browser-Sprache `de`
- [ ] Test: localStorage nicht verfügbar → Browser-Sprache als Fallback
- [ ] Update bestehende Tests: `LanguageProvider` als Wrapper hinzufügen

**Acceptance criteria:**
- [ ] `pnpm test` passes

**Related behaviors:** Browser-Sprache Deutsch wird erkannt, Browser-Sprache Englisch wird erkannt, Unbekannte Browser-Sprache fällt auf Englisch zurück, Gespeicherte Sprache hat Vorrang vor Browser-Sprache, localStorage ist nicht verfügbar

---

## Step 5: Frontend Tests — Language Switching

- [ ] Test: Klick auf "DE" wechselt zu Deutsch, aktualisiert localStorage und html lang
- [ ] Test: Klick auf "EN" wechselt zu Englisch
- [ ] Test: Klick auf bereits aktive Sprache → keine Änderung
- [ ] Test: Sprachwahl bleibt nach Navigation erhalten
- [ ] Test: Dynamische Texte mit Platzhaltern (Lösch-Dialog mit Firmenname)
- [ ] Test: Pagination-Text mit Platzhaltern

**Acceptance criteria:**
- [ ] `pnpm test` passes
- [ ] `pnpm build` succeeds (final verification)

**Related behaviors:** Wechsel von Englisch zu Deutsch, Wechsel von Deutsch zu Englisch, Klick auf bereits aktive Sprache hat keine Wirkung, Sprachwahl bleibt nach Seitenwechsel erhalten, Sprachwahl überlebt Browser-Neustart, Dynamische Texte mit Platzhaltern, Pagination-Text mit Platzhaltern

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Browser-Sprache Deutsch wird erkannt | Frontend | Step 4 |
| Browser-Sprache Englisch wird erkannt | Frontend | Step 4 |
| Unbekannte Browser-Sprache fällt auf Englisch zurück | Frontend | Step 4 |
| Gespeicherte Sprache hat Vorrang vor Browser-Sprache | Frontend | Step 4 |
| Wechsel von Englisch zu Deutsch | Frontend | Step 5 |
| Wechsel von Deutsch zu Englisch | Frontend | Step 5 |
| Klick auf bereits aktive Sprache hat keine Wirkung | Frontend | Step 5 |
| Sprachwahl bleibt nach Seitenwechsel erhalten | Frontend | Step 5 |
| Sprachwahl überlebt Browser-Neustart | Frontend | Step 5 |
| Toggle ist in der Desktop-Sidebar sichtbar | Frontend | Steps 2, 5 |
| Toggle ist im Mobile-Menü sichtbar | Frontend | Steps 2, 5 |
| Aktive Sprache ist visuell hervorgehoben | Frontend | Steps 2, 5 |
| Navigation wird übersetzt | Frontend | Steps 3, 5 |
| Firmen-Liste wird übersetzt | Frontend | Step 3 |
| Firmen-Formular wird übersetzt | Frontend | Step 3 |
| Firmen-Detail wird übersetzt | Frontend | Step 3 |
| Lösch-Dialog wird übersetzt | Frontend | Step 3 |
| Fehlermeldungen werden übersetzt | Frontend | Step 3 |
| Health-Status wird übersetzt | Frontend | Step 3 |
| Leere Zustände werden übersetzt | Frontend | Step 3 |
| localStorage ist nicht verfügbar | Frontend | Step 4 |
| Dynamische Texte mit Platzhaltern | Frontend | Step 5 |
| Pagination-Text mit Platzhaltern | Frontend | Step 5 |
