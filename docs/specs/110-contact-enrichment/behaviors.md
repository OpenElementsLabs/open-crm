# Behaviors: Contact Enrichment via Gravatar, Dropcontact & Cognism

## Permissions & visibility

### Non-admin does not see the enrichment action
- **Given** a logged-in user without APP-ADMIN or IT-ADMIN role
- **When** they open a contact detail view
- **Then** the "Anreichern" button is not shown (and the backend enrich endpoints reject them with 403)

### Admin sees the enrichment action
- **Given** a logged-in user with APP-ADMIN or IT-ADMIN role
- **When** they open a contact detail view
- **Then** the "Anreichern" dropdown button is shown

### Unconfigured services are hidden in the menu
- **Given** an admin, Dropcontact configured, Cognism not configured
- **When** they open the "Anreichern" dropdown
- **Then** Gravatar and Dropcontact entries are enabled and the Cognism entry is hidden/disabled

### Gravatar is always available
- **Given** an admin and no external service configured at all
- **When** they open the "Anreichern" dropdown
- **Then** the Gravatar entry is always available (Gravatar needs no API key)

## Settings management

### IT-admin stores a valid Dropcontact key
- **Given** an IT-admin and a valid Dropcontact API key
- **When** they PUT the key to `/api/dropcontact/settings`
- **Then** the key is validated against Dropcontact, stored via SettingsDataService, and the response is `{ configured: true }`

### Invalid key is rejected
- **Given** an IT-admin and an invalid API key
- **When** they PUT it to `/api/{service}/settings`
- **Then** the response is 400 and no key is stored

### Non-IT-admin cannot manage keys
- **Given** an APP-ADMIN who is not an IT-admin
- **When** they call any `/api/{service}/settings` endpoint
- **Then** the response is 403

### Removing a key disables the service
- **Given** a configured Dropcontact key
- **When** an IT-admin DELETEs `/api/dropcontact/settings`
- **Then** the response is 204 and `GET /api/dropcontact/settings` reports `{ configured: false }`

### Keys are never returned to the client
- **Given** a configured service
- **When** any settings endpoint responds
- **Then** the body contains only a `configured` boolean, never the key value

## Matching — input selection

### Email match is preferred
- **Given** a contact with an email address
- **When** the admin runs enrichment via any service
- **Then** the service is queried by email

### Falls back to name+company without email
- **Given** a contact without an email but with a name and company
- **When** the admin runs Dropcontact or Cognism enrichment
- **Then** the service is queried by name+company

### Gravatar is skipped without email
- **Given** a contact without an email
- **When** the admin opens the "Anreichern" menu
- **Then** the Gravatar entry is unavailable (Gravatar requires an email)

## Matching — candidate outcomes

### Exactly one candidate skips the selection list
- **Given** an email match returning exactly one candidate
- **When** the search completes
- **Then** the selection list is skipped and the preview is shown directly

### Multiple candidates require a selection
- **Given** a name+company match returning multiple candidates
- **When** the search completes
- **Then** a selection list of candidates (label "Name @ Company") is shown, and the preview appears only after the user picks one

### No candidate shows the no-match state
- **Given** a search that returns zero candidates
- **When** the search completes
- **Then** a "kein Treffer gefunden" info state is shown and no apply action is offered

### Candidate list shows minimal info only
- **Given** multiple candidates
- **When** the selection list is shown
- **Then** each entry shows only the minimal label (name + company), with no additional disambiguating fields

## Preview — fill-empty rule

### Only empty fields are proposed
- **Given** a contact with `position = "CTO"` and empty `phoneNumber`
- **And** a candidate providing `position = "Chief Technology Officer"` and a phone number
- **When** the preview is computed
- **Then** the phone number is proposed and the position is not (existing value is never overwritten)

### Nothing to enrich when all fields are filled
- **Given** a candidate whose every provided field is already filled on the contact
- **When** the preview is computed
- **Then** the same info state as no-match is shown ("Nichts anzureichern") and the apply button is disabled

### Social links are evaluated per network
- **Given** a contact with a GitHub social link but no LinkedIn link
- **And** a candidate providing a LinkedIn URL
- **When** the preview is computed
- **Then** the LinkedIn link is proposed as an addition and the existing GitHub link is untouched

### Photo proposed only when the contact has none (Gravatar)
- **Given** a contact without a photo and a Gravatar avatar exists for the email
- **When** Gravatar enrichment runs
- **Then** the photo is proposed
- **And Given** a contact that already has a photo
- **When** Gravatar enrichment runs
- **Then** the photo is not proposed

## Company resolution

### Existing company is linked
- **Given** a contact with no company and a candidate company name matching an existing company
- **When** the preview is computed
- **Then** `companyResolution.kind = MATCHED` and applying links the contact to the existing company (part of all-or-nothing)

### New company requires the checkbox
- **Given** a candidate company name matching no existing company
- **When** the preview is shown
- **Then** a "Neue Firma «X» anlegen" checkbox is offered
- **And** if the checkbox is unchecked on apply, no company is created and `company` stays empty while all other fields still apply
- **And** if the checkbox is checked on apply, a new company is created and linked

### No company data
- **Given** a candidate that provides no company name
- **When** the preview is computed
- **Then** `companyResolution.kind = NONE` and no company change is offered

## Apply

### All-or-nothing apply
- **Given** a preview with several proposed field changes
- **When** the admin presses "Übernehmen"
- **Then** all proposed changes are written together (except company creation, gated by its checkbox)

### Apply re-enforces fill-empty against current state
- **Given** a preview was computed, then the contact's `position` was filled by someone else before apply
- **When** the admin presses "Übernehmen"
- **Then** the now-non-empty `position` is not overwritten (fill-empty is re-checked at apply time)

### Apply publishes an update event
- **Given** a successful apply
- **When** the contact is updated
- **Then** an `OnObjectUpdate<ContactDto>` event is published so audit, search, and webhooks react

### GDPR notice after apply
- **Given** a successful apply
- **When** the update completes
- **Then** the UI shows a GDPR notice reminding the admin about the Art. 14 information obligation

### Nothing changes before apply
- **Given** a computed preview
- **When** the admin closes the dialog without confirming
- **Then** the contact is unchanged (the operation is read-only until explicit apply)

## Error handling

### Generic error on downstream failure
- **Given** the external service times out, returns 5xx, or the key/quota is invalid/exhausted
- **When** the search runs
- **Then** the dialog shows a generic error ("Anreicherung derzeit nicht möglich") and the contact is unchanged
- **And** the specific cause is written only to the server logs

### Search on an unconfigured service is defended
- **Given** Dropcontact is not configured
- **When** a search request for Dropcontact reaches the backend anyway
- **Then** the backend responds 503 (the menu normally prevents this)

### Gravatar placeholder is caught by the human
- **Given** an email with no real Gravatar avatar (a placeholder image is returned)
- **When** the photo appears in the preview
- **Then** the admin can reject it by not applying (implementation may additionally use `d=404` to avoid proposing a placeholder at all)

## Brevo interaction

### Brevo-managed fields are not touched
- **Given** a Brevo-managed contact whose `email`, `firstName`, `lastName` are already filled
- **When** enrichment runs
- **Then** those fields are not proposed (fill-empty rule leaves them untouched) and apply does not fail on them
