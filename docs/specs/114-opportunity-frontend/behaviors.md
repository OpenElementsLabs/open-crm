# Behaviors: Opportunity (Deal) — Frontend

## Navigation

### Sidebar entry navigates to the list

- **Given** a logged-in user on any page
- **When** the "Opportunitäten" (DE) / "Opportunities" (EN) sidebar entry is clicked
- **Then** the `/opportunities` list view opens and the sidebar entry is marked active

## List view

### List shows opportunities with all columns

- **Given** opportunities exist in the backend
- **When** `/opportunities` is opened
- **Then** a table shows title, company, main contact, stage, status badge, formatted EUR value, and owner (avatar + name) per row, with the record count and pagination below

### Empty list state

- **Given** no opportunities exist
- **When** `/opportunities` is opened
- **Then** an empty-state message is shown instead of the table

### Value formatting

- **Given** an opportunity with estimatedValue 25000 and one with null
- **When** the list is displayed with UI language DE
- **Then** the first shows "25.000,00 €" and the second shows an empty/dash value cell

### Status filter

- **Given** opportunities with statuses OPEN, WON, and LOST
- **When** the status filter is set to "gewonnen"
- **Then** only WON opportunities are listed and the record count updates

### Combined filters

- **Given** opportunities across several companies, stages, and tags
- **When** a title search term, a company, a stage, and a tag filter are set together
- **Then** only opportunities matching all filters are listed

### Pagination and page size

- **Given** more opportunities than the selected page size
- **When** the user switches pages or changes the page size (10/20/50/100/200)
- **Then** the table updates accordingly and the count display shows the correct page info

### Row navigation and actions

- **Given** the list is displayed
- **When** the user clicks the title, the company cell, or the main-contact cell
- **Then** the opportunity detail, company detail, or contact detail opens respectively; edit and comment row-action buttons (with tooltips) work as in the company/contact lists

## Create form

### Create with defaults

- **Given** a logged-in user U opens `/opportunities/new`
- **When** the form loads
- **Then** status is pre-selected as "offen" and the owner combobox is pre-selected with user U; stage, product, value, additional contacts, and tags are empty

### Successful creation

- **Given** the form is filled with title, company, and main contact (rest defaults)
- **When** the user saves
- **Then** the opportunity is created via `POST /api/opportunities` and the user is redirected to its detail view

### Required-field validation

- **Given** the form with title, company, or main contact missing
- **When** the user tries to save
- **Then** client-side validation messages appear and no request is sent

### Stage and product comboboxes

- **Given** the create form
- **When** the stage combobox is opened
- **Then** it offers Lead, Erstkontakt, Qualifiziert, Angebot, Gewonnen, Verloren; the product combobox offers Support & Care and Digital Trust; both may remain empty

### Value input validation

- **Given** the create form
- **When** a negative value or more than two decimal places is entered
- **Then** a validation message appears and saving is prevented

### Owner can be changed to any user

- **Given** the create form
- **When** the owner combobox is opened
- **Then** all users (from `GET /api/users/options`) are offered with avatar and name, and a different user can be selected as owner

## Company/contact mismatch warning

### Warning for a foreign main contact

- **Given** company A is selected and a contact belonging to company B is chosen as main contact
- **When** the selection is made
- **Then** a non-blocking warning appears below the main-contact field, and the form can still be saved

### Warning for a foreign additional contact

- **Given** company A is selected and an additional contact without any company is chosen
- **When** the selection is made
- **Then** the warning appears for the additional-contacts field; saving remains possible

### Warning disappears when consistent

- **Given** the mismatch warning is visible
- **When** the user switches the company (or contact) so that all contacts belong to the selected company
- **Then** the warning disappears

### Main contact excluded from additional contacts

- **Given** contact A is selected as main contact
- **When** the additional-contacts selector is opened
- **Then** contact A is not offered as an option

## Edit form

### Edit pre-fills all fields

- **Given** an existing opportunity
- **When** `/opportunities/[id]/edit` is opened
- **Then** all fields including stage, status, product, value, company, contacts, owner, and tags are pre-filled with current values

### Successful update

- **Given** the edit form with changed values
- **When** the user saves
- **Then** the opportunity is updated via `PUT /api/opportunities/{id}` and the detail view shows the new values

### Server error surfaces in the form

- **Given** the edit form
- **When** the backend responds with 400 (e.g. race: main contact also submitted as additional contact)
- **Then** an error message is shown and the entered data is preserved

## Detail view

### Detail shows all fields

- **Given** an opportunity with all fields set
- **When** its detail view is opened
- **Then** title, status badge, tag chips, stage, product, formatted value, company link, main-contact link, additional-contact links, owner (avatar + name), and created/updated timestamps are displayed

### Navigation to company and contacts

- **Given** the detail view
- **When** the company or a contact link is clicked
- **Then** the corresponding detail view opens

### Comments lifecycle

- **Given** the detail view of an opportunity
- **When** the user adds a comment via the modal dialog
- **Then** the comment appears with the user as author and the comment count in the heading updates immediately; admins can delete comments with a confirmation dialog, updating the count

### Delete restricted to admins

- **Given** the detail view
- **When** viewed without the ADMIN role
- **Then** the delete button is visible but disabled (with tooltip); with ADMIN role, delete opens a confirmation dialog and redirects to the list after success

## Global search

### Opportunities section in search results

- **Given** an indexed opportunity "Muster-Bank CRA"
- **When** the user searches "muster" on `/search`
- **Then** an "Opportunities" section shows the hit with the title, linking to the opportunity detail; the section is hidden when there are no opportunity hits

## Updates feed

### Opportunity events are rendered

- **Given** a user created and updated an opportunity
- **When** the updates view is opened
- **Then** entries like "hat die Opportunität 'X' angelegt/geändert" (DE) appear with author avatar and the title linking to the opportunity detail

### Deleted opportunity event without link

- **Given** an opportunity was deleted
- **When** the updates view is opened
- **Then** the delete entry shows the trash icon and the title as plain text without a link

## Tag list

### Opportunity count column

- **Given** a tag assigned to two opportunities
- **When** the tag list is opened
- **Then** the tag row shows an opportunity count of 2, linking to `/opportunities` filtered by that tag

## i18n

### Language toggle covers all new texts

- **Given** any opportunity view is open
- **When** the language is switched between DE and EN
- **Then** all labels, filters, warnings, empty states, and update-feed texts switch language; stage and product values remain untranslated
