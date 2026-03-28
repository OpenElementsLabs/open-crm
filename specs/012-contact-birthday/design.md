# Design: Contact Birthday Field

## GitHub Issue

_(to be linked once created)_

## Summary

Contacts in the CRM currently have no birthday field. Users want to know when a contact's birthday is so they can send greetings. This spec adds an optional birthday field to contacts — a standard date stored as a full date (day, month, year) in PostgreSQL.

## Goals

- Add an optional birthday field to contacts
- Allow setting and editing birthday via the contact form
- Display birthday in the contact detail view

## Non-goals

- Displaying birthday in the contact list table
- Filtering or sorting by birthday
- "Upcoming birthdays" feature or notifications
- Stripping or ignoring the year — the full date is stored as entered

## Technical Approach

### Backend: Database Migration

**New file:** `backend/src/main/resources/db/migration/V4__add_contact_birthday.sql`

```sql
ALTER TABLE contacts ADD COLUMN birthday DATE;
```

A single nullable `DATE` column. No default value — existing contacts will have `NULL`.

### Backend: Entity Update

**File:** `backend/src/main/java/com/openelements/crm/contact/ContactEntity.java`

Add:
- `private LocalDate birthday` — nullable, mapped to the `birthday` column

### Backend: DTO Updates

**File:** `backend/src/main/java/com/openelements/crm/contact/ContactDto.java`

Add:
- `LocalDate birthday` — included in the response, resolved from entity

**File:** `backend/src/main/java/com/openelements/crm/contact/ContactCreateDto.java`

Add:
- `LocalDate birthday` — optional field for creation

**File:** `backend/src/main/java/com/openelements/crm/contact/ContactUpdateDto.java`

Add:
- `LocalDate birthday` — optional field for updates

### Backend: Service Update

**File:** `backend/src/main/java/com/openelements/crm/contact/ContactService.java`

- Set `birthday` on entity in `create()` and `update()` methods

### Frontend: TypeScript Type

**File:** `frontend/src/lib/types.ts`

Add to `ContactDto`:
- `birthday: string | null` — ISO date string (`YYYY-MM-DD`) or null

Add to `ContactCreateDto`:
- `birthday?: string | null`

### Frontend: Contact Form

**File:** `frontend/src/components/contact-form.tsx`

Add a standard shadcn/ui date picker for the birthday field. The field is optional — the user can leave it empty. Uses the existing Popover + Calendar pattern from shadcn/ui.

**Rationale:** A standard date picker avoids custom component complexity. The year is included because removing it would require a custom widget, which is not justified for this non-essential feature.

### Frontend: Contact Detail View

**File:** `frontend/src/components/contact-detail.tsx`

Display birthday as a `DetailField` in the contact details card. Format the date according to the current locale:
- DE: `DD.MM.YYYY` (e.g., `15.03.1990`)
- EN: `MM/DD/YYYY` (e.g., `03/15/1990`)

When no birthday is set, display "—" (consistent with other optional fields).

### Frontend: i18n Labels

**Files:** `frontend/src/lib/i18n/de.ts`, `frontend/src/lib/i18n/en.ts`

| Key | DE | EN |
|-----|----|----|
| `contacts.detail.birthday` | Geburtstag | Birthday |
| `contacts.form.birthday` | Geburtstag | Birthday |

## GDPR Consideration

Birthday is personal data under DSGVO. It is covered by the same legal basis (berechtigtes Interesse / legitimate interest) that applies to all other contact data in the CRM. The field is optional and can be deleted by clearing it in the edit form. When a contact is deleted (hard delete), the birthday is deleted with it.

## Dependencies

- shadcn/ui Calendar and Popover components must be available (may need to be added via `npx shadcn@latest add calendar popover` if not already present)

## Open Questions

- None
