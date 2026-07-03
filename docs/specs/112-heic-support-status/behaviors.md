# Behaviors: HEIC support status indicator in the admin area

## Backend — capabilities endpoint

### Returns HEIC availability for IT-admin
- **Given** an IT-admin and a running backend where `CrmHeicSupportCheck.isHeicAvailable()` is `true`
- **When** they call `GET /api/admin/capabilities`
- **Then** the response is 200 with `{ "heicAvailable": true }`

### Reflects unavailable HEIC decoding
- **Given** a backend where HEIC decoding is not available (libheif missing)
- **When** an IT-admin calls `GET /api/admin/capabilities`
- **Then** the response is 200 with `{ "heicAvailable": false }`

### Non-IT-admin is rejected
- **Given** a logged-in user without the IT-admin role
- **When** they call `GET /api/admin/capabilities`
- **Then** the response is 403

### Reflects the startup-cached value
- **Given** the bean determined availability once at startup
- **When** the endpoint is called multiple times during the container's lifetime
- **Then** it consistently returns that startup value (no live re-probe)

## Library — CapabilityStatus component (ui)

### Available renders green
- **Given** `available = true` with label and availableText
- **When** the component renders
- **Then** a green indicator and the availableText are shown

### Unavailable renders red with hint tooltip
- **Given** `available = false` with a `hint`
- **When** the component renders
- **Then** a red warning indicator and the unavailableText are shown, and the hint is available as a tooltip

### Hint is optional
- **Given** no `hint` is passed
- **When** the component renders
- **Then** it renders without a tooltip and does not error

## Library — status page capabilities rendering (nextjs-app-layer)

### Backwards compatible without capabilities
- **Given** `createServerStatusPage({ auth })` is called without a `capabilities` option
- **When** the status page renders
- **Then** it behaves exactly as before (backend health row only)

### Renders a row per configured capability
- **Given** `capabilities` config with one item (`heicAvailable`) and the endpoint returns `{ heicAvailable: true }`
- **When** the status page renders for an IT-admin
- **Then** the backend health row plus one green HEIC capability row are shown

### Fail-safe on fetch error
- **Given** the capabilities endpoint fetch fails
- **When** the status page renders
- **Then** each configured capability row renders as unavailable (never a false "available")

## Frontend integration (open-crm)

### HEIC row appears on the existing status page
- **Given** the app wires the `capabilities` config into `/admin/status`
- **When** an IT-admin opens `/admin/status`
- **Then** the page shows the backend health status and a HEIC-decoding row (no new route/nav entry)

### Unavailable shows the Dockerfile hint
- **Given** HEIC decoding is unavailable in the running container
- **When** an IT-admin opens `/admin/status`
- **Then** the HEIC row is red and its tooltip reads "HEIC uploads will be rejected with 415 — check Dockerfile"

### Non-IT-admin cannot see the status page
- **Given** a logged-in user without the IT-admin role
- **When** they navigate to `/admin/status`
- **Then** they get the forbidden page (unchanged behavior)

## Localization

### HEIC row label and texts are localized
- **Given** the app language is German
- **When** the HEIC row renders
- **Then** its label, available/unavailable text, and hint are shown in German (English for EN)
