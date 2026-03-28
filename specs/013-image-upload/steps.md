# Implementation Steps: Company Logo & Contact Photo Upload

## Step 1: Database Migration

- [ ] Create `backend/src/main/resources/db/migration/V5__add_images.sql`
  - `ALTER TABLE companies ADD COLUMN logo BYTEA;`
  - `ALTER TABLE companies ADD COLUMN logo_content_type VARCHAR(50);`
  - `ALTER TABLE contacts ADD COLUMN photo BYTEA;`
  - `ALTER TABLE contacts ADD COLUMN photo_content_type VARCHAR(50);`

**Acceptance criteria:**
- [ ] Migration runs successfully against the database
- [ ] Flyway version history shows V5
- [ ] Project builds successfully

**Related behaviors:** Logo is preserved when company is soft-deleted, Photo is deleted when contact is deleted

---

## Step 2: Entity Updates

- [ ] Add `logo` (`byte[]`, `@Lob`, `FetchType.LAZY`) and `logoContentType` (`String`) fields to `CompanyEntity`
- [ ] Add `photo` (`byte[]`, `@Lob`, `FetchType.LAZY`) and `photoContentType` (`String`) fields to `ContactEntity`

**Acceptance criteria:**
- [ ] Project builds successfully
- [ ] Hibernate validation passes (ddl-auto: validate)
- [ ] Existing tests still pass

**Related behaviors:** Logo is preserved when company is soft-deleted, Photo is deleted when contact is deleted

---

## Step 3: DTO Updates and Service Image Methods

- [ ] Add `boolean hasLogo` to `CompanyDto` record and update `fromEntity()` factory method
- [ ] Add `boolean hasPhoto` to `ContactDto` record and update `fromEntity()` factory method
- [ ] Create `ImageData` record (or similar) to hold `byte[] data` and `String contentType`
- [ ] Add to `CompanyService`: `uploadLogo(UUID, byte[], String)`, `ImageData getLogo(UUID)`, `deleteLogo(UUID)`
- [ ] Add to `ContactService`: `uploadPhoto(UUID, byte[], String)`, `ImageData getPhoto(UUID)`, `deletePhoto(UUID)`
- [ ] Add content-type validation in service methods (company: SVG/PNG/JPEG; contact: JPEG only)

**Acceptance criteria:**
- [ ] Project builds successfully
- [ ] Existing tests updated for new DTO field and still pass

**Related behaviors:** CompanyDto hasLogo is true when logo exists, CompanyDto hasLogo is false when no logo exists, ContactDto hasPhoto is true when photo exists, ContactDto hasPhoto is false when no photo exists

---

## Step 4: Spring Multipart Configuration and Controller Endpoints

- [ ] Add multipart config to `application.yml` (max-file-size: 2MB, max-request-size: 2MB)
- [ ] Add to `CompanyController`: `POST /api/companies/{id}/logo`, `GET /api/companies/{id}/logo`, `DELETE /api/companies/{id}/logo`
- [ ] Add to `ContactController`: `POST /api/contacts/{id}/photo`, `GET /api/contacts/{id}/photo`, `DELETE /api/contacts/{id}/photo`
- [ ] Add OpenAPI annotations to new endpoints
- [ ] Handle validation errors (invalid content type → 400, file too large → 400, entity not found → 404, no image → 404)

**Acceptance criteria:**
- [ ] Project builds successfully
- [ ] Endpoints accessible via Swagger UI
- [ ] Existing tests still pass

**Related behaviors:** GET returns logo with correct content type, GET returns 404 when no logo exists, GET returns 404 for non-existent company, POST stores logo and returns 200, POST returns 400 for invalid content type, POST returns 400 for file exceeding size limit, DELETE removes logo and returns 204, GET returns photo with correct content type, GET returns 404 when no photo exists, POST returns 400 for non-JPEG format, DELETE removes photo and returns 204

---

## Step 5: Backend Tests for Image API

- [ ] Add logo upload/get/delete tests to `CompanyControllerTest`
  - Upload PNG, JPEG, SVG → 200
  - GET with correct content type → 200
  - GET without logo → 404
  - GET for non-existent company → 404
  - Upload invalid format (GIF) → 400
  - Upload oversized file → 400
  - DELETE logo → 204
  - Upload replaces existing logo
  - hasLogo flag in DTO (true/false)
  - Logo preserved after soft-delete, available after restore
- [ ] Add photo upload/get/delete tests to `ContactControllerTest`
  - Upload JPEG → 200
  - GET with correct content type → 200
  - GET without photo → 404
  - Upload non-JPEG (PNG) → 400
  - DELETE photo → 204
  - Upload replaces existing photo
  - hasPhoto flag in DTO (true/false)
  - Photo deleted with contact (hard delete)

**Acceptance criteria:**
- [ ] All new backend tests pass
- [ ] All existing backend tests still pass
- [ ] Every backend behavior scenario from behaviors.md is covered

**Related behaviors:** All "Backend API" scenarios, all "DTO" scenarios, Logo is preserved when company is soft-deleted, Logo is available after company is restored, Photo is deleted when contact is deleted, Uploading a new logo replaces the old one (backend), Uploading a new photo replaces the old one (backend)

---

## Step 6: Frontend API Functions and Types

- [ ] Add `hasLogo: boolean` to `CompanyDto` type in `types.ts`
- [ ] Add `hasPhoto: boolean` to `ContactDto` type in `types.ts`
- [ ] Add to `api.ts`:
  - `uploadCompanyLogo(id: string, file: File): Promise<void>` (POST multipart)
  - `getCompanyLogoUrl(id: string): string` (returns URL string)
  - `deleteCompanyLogo(id: string): Promise<void>` (DELETE)
  - `uploadContactPhoto(id: string, file: File): Promise<void>` (POST multipart)
  - `getContactPhotoUrl(id: string): string` (returns URL string)
  - `deleteContactPhoto(id: string): Promise<void>` (DELETE)

**Acceptance criteria:**
- [ ] Frontend builds successfully
- [ ] TypeScript types are consistent with backend DTOs

**Related behaviors:** (Foundation for all frontend display and upload scenarios)

---

## Step 7: Frontend i18n Labels

- [ ] Add to `de.ts`: `companies.form.logo`, `companies.form.uploadLogo`, `companies.form.removeLogo`, `contacts.form.photo`, `contacts.form.uploadPhoto`, `contacts.form.removePhoto`, `common.imageErrors.tooLarge`, `common.imageErrors.invalidFormat`
- [ ] Add to `en.ts`: same keys with English translations

**Acceptance criteria:**
- [ ] Frontend builds successfully
- [ ] All i18n keys are present in both language files

**Related behaviors:** German labels for logo upload, English labels for logo upload, German labels for photo upload, English labels for photo upload

---

## Step 8: Frontend List Tables — Image Column

- [ ] Update `company-list.tsx`: add image as first column (thumbnail ~32x32 rounded if `hasLogo`, `Building2` icon placeholder otherwise)
- [ ] Update `contact-list.tsx`: add image as first column (thumbnail ~32x32 rounded-full if `hasPhoto`, `User` icon placeholder otherwise)

**Acceptance criteria:**
- [ ] Frontend builds successfully
- [ ] Images/placeholders render correctly in list tables
- [ ] Column order matches spec: Image, Name, Website, Contacts, Comments, Actions (companies) / Image, First Name, Last Name, Company, Comments, Actions (contacts)

**Related behaviors:** Logo is shown as thumbnail in company list table, Placeholder icon shown in list when no logo exists, Company list has image as first column, Photo is shown as thumbnail in contact list table, Placeholder icon shown in list when no photo exists, Contact list has image as first column

---

## Step 9: Frontend Detail Views — Image Display

- [ ] Update `company-detail.tsx`: show logo (~96x96) near company name, `Building2` placeholder if no logo
- [ ] Update `contact-detail.tsx`: show photo (~96x96, rounded-full) near contact name, `User` placeholder if no photo

**Acceptance criteria:**
- [ ] Frontend builds successfully
- [ ] Larger images display correctly in detail views
- [ ] Placeholders show when no image exists

**Related behaviors:** Logo is shown larger in company detail view, Placeholder icon shown in detail when no logo exists, Photo is shown larger in contact detail view, Placeholder icon shown in detail when no photo exists

---

## Step 10: Frontend Forms — Upload and Remove

- [ ] Update `company-form.tsx`: add file input (`accept="image/svg+xml,image/png,image/jpeg"`), preview, "Remove logo" button, client-side validation (size ≤ 2MB, MIME type)
- [ ] Update `contact-form.tsx`: add file input (`accept="image/jpeg"`), preview, "Remove photo" button, client-side validation
- [ ] Implement upload flow: create/update entity first, then upload image; show error on failure
- [ ] Show error messages using i18n keys for too-large and invalid-format errors

**Acceptance criteria:**
- [ ] Frontend builds successfully
- [ ] Upload, replace, and remove work end-to-end
- [ ] Client-side validation shows localized error messages
- [ ] Existing form tests still pass

**Related behaviors:** Logo can be uploaded as PNG, Logo can be uploaded as JPEG, Logo can be uploaded as SVG, Uploading a new logo replaces the old one, Logo upload rejects files exceeding 2MB, Logo upload rejects invalid formats, Logo can be removed, Photo can be uploaded as JPEG, Uploading a new photo replaces the old one, Photo upload rejects files exceeding 2MB, Photo upload rejects non-JPEG formats, Photo can be removed

---

## Step 11: Frontend Tests — List and Detail Image Display

- [ ] Update `company-list.test.tsx`: test image column renders thumbnail when `hasLogo: true`, placeholder when `hasLogo: false`, correct column order
- [ ] Update `company-detail.test.tsx`: test larger image display, placeholder when no logo
- [ ] Update `contact-list.test.tsx`: test image column renders thumbnail when `hasPhoto: true`, placeholder when `hasPhoto: false`, correct column order
- [ ] Update `contact-detail.test.tsx`: test larger image display, placeholder when no photo

**Acceptance criteria:**
- [ ] All frontend display tests pass
- [ ] All existing frontend tests still pass

**Related behaviors:** Logo is shown as thumbnail in company list table, Placeholder icon shown in list when no logo exists, Company list has image as first column, Logo is shown larger in company detail view, Placeholder icon shown in detail when no logo exists, Photo is shown as thumbnail in contact list table, Placeholder icon shown in list when no photo exists, Contact list has image as first column, Photo is shown larger in contact detail view, Placeholder icon shown in detail when no photo exists

---

## Step 12: Frontend Tests — Form Upload, Validation, and i18n

- [ ] Update `company-form.test.tsx`: test file input renders, client-side size validation error, client-side format validation error, remove logo button, upload triggers API call
- [ ] Update `contact-form.test.tsx`: test file input renders, client-side size validation error, client-side format validation error, remove photo button, upload triggers API call
- [ ] Add i18n tests: verify German labels (Logo, Logo hochladen, Logo entfernen), English labels (Logo, Upload logo, Remove logo), German photo labels, English photo labels

**Acceptance criteria:**
- [ ] All frontend form and i18n tests pass
- [ ] All existing frontend tests still pass
- [ ] Every frontend behavior scenario from behaviors.md is covered

**Related behaviors:** Logo can be uploaded as PNG, Logo can be uploaded as JPEG, Logo can be uploaded as SVG, Uploading a new logo replaces the old one, Logo upload rejects files exceeding 2MB, Logo upload rejects invalid formats, Logo can be removed, Photo can be uploaded as JPEG, Uploading a new photo replaces the old one, Photo upload rejects files exceeding 2MB, Photo upload rejects non-JPEG formats, Photo can be removed, German labels for logo upload, English labels for logo upload, German labels for photo upload, English labels for photo upload

---

## Step 13: Update Project Documentation

- [ ] Update `.claude/conventions/project-specific/project-features.md` — add image upload feature
- [ ] Update `.claude/conventions/project-specific/project-tech.md` — add Spring multipart, bytea storage
- [ ] Update `.claude/conventions/project-specific/project-structure.md` — note new migration and ImageData record
- [ ] Update `.claude/conventions/project-specific/project-architecture.md` — document image storage approach and endpoints
- [ ] Update `README.md` — mention image upload capability if user-facing docs need it

**Acceptance criteria:**
- [ ] All documentation files reflect the changes from this spec
- [ ] Empty template files are filled with meaningful content based on full project state

**Related behaviors:** (none — documentation step)
