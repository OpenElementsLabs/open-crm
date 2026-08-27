# Behaviors: Per-page HTML metadata for contacts and companies

## Document title

### A contact detail page is titled with the contact's name

- **Given** a contact with first name `Max` and last name `Mustermann`
- **When** `/contacts/{id}` is rendered
- **Then** the document title is `Open CRM: Max Mustermann`

### An academic title is part of the name

- **Given** a contact with title `Dr.`, first name `Max`, last name `Mustermann`
- **When** the page is rendered
- **Then** the document title is `Open CRM: Dr. Max Mustermann`

### A missing academic title leaves no double space

- **Given** a contact whose `title` field is empty or null
- **When** the page is rendered
- **Then** the title is `Open CRM: Max Mustermann` with exactly one space between the names

### A company detail page is titled with the company name

- **Given** a company named `Acme GmbH`
- **When** `/companies/{id}` is rendered
- **Then** the document title is `Open CRM: Acme GmbH`

### Pages without their own metadata keep the plain title

- **Given** the company list, the admin area, or any page that sets no title
- **When** it is rendered
- **Then** the document title is `Open CRM`, unchanged from before this spec
- **And** no `Open CRM: ` prefix appears on its own

## Meta description

### An existing description becomes the meta description

- **Given** a contact whose `description` is `Langjähriger Ansprechpartner für den Bereich Einkauf.`
- **When** the page is rendered
- **Then** `<meta name="description">` carries exactly that text

### A missing description inherits the application description

- **Given** a company whose `description` is empty, null, or only whitespace
- **When** the page is rendered
- **Then** no page-level description is emitted
- **And** the root description `CRM system by Open Elements` applies

### A long description is truncated at a word boundary

- **Given** a description longer than 160 characters
- **When** the page is rendered
- **Then** the meta description is at most 160 characters including the ellipsis
- **And** it does not end mid-word

### Multi-line descriptions are collapsed

- **Given** a description containing newlines, tabs and runs of consecutive spaces
- **When** the page is rendered
- **Then** the meta description contains single spaces only, with no leading or trailing whitespace

### Markup in a description cannot break out of the attribute

- **Given** a description containing `"` and `<script>`
- **When** the page is rendered
- **Then** the value is escaped inside the `content` attribute and no executable markup is produced

### Control characters are stripped

- **Given** a description containing control characters
- **When** the page is rendered
- **Then** they are removed from the meta description

## Open Graph and Twitter tags

### A contact page emits the full tag set

- **Given** a contact with a description
- **When** the page is rendered
- **Then** `og:title` matches the document title, `og:description` matches the meta description, and
  `og:image` points at the route's `opengraph-image`
- **And** the Twitter card tags carry the same values

### Open Graph URLs are absolute

- **Given** `AUTH_URL` is configured
- **When** any detail page is rendered
- **Then** the `og:image` URL is absolute and built on that origin, not on `localhost`

### The application declares itself non-indexable

- **Given** any page of the application
- **When** it is rendered
- **Then** the robots meta declares `noindex` and `nofollow`

## Preview image

### A contact photo is framed at the Open Graph ratio

- **Given** a contact whose `hasPhoto` is true
- **When** `/contacts/{id}/opengraph-image` is requested
- **Then** the response is a PNG of exactly 1200×630
- **And** it contains the photo centre-cropped into the portrait area, the contact's name, and the
  Open CRM wordmark

### A tall portrait photo produces the same dimensions

- **Given** a contact photo far taller than it is wide
- **When** the preview image is generated
- **Then** the output is still exactly 1200×630 and the photo fills the portrait area without
  distortion or letterboxing

### A wide company logo produces the same dimensions

- **Given** a company logo far wider than it is tall
- **When** the preview image is generated
- **Then** the output is still exactly 1200×630 and the logo is centre-cropped to the portrait area

### A contact without a photo gets the initials variant

- **Given** a contact `Max Mustermann` whose `hasPhoto` is false
- **When** the preview image is generated
- **Then** the portrait area shows `MM` on the brand surface
- **And** the frame, name and wordmark are identical to the photo variant

### A company without a logo gets the initials variant

- **Given** a company `Acme GmbH` whose `hasLogo` is false
- **When** the preview image is generated
- **Then** the portrait area shows `AG`

### A single-word company name yields one initial

- **Given** a company named `Acme`
- **When** the preview image is generated
- **Then** the portrait area shows `A`

### A failed image fetch degrades to initials

- **Given** a contact whose `hasPhoto` is true but whose photo request fails or returns a non-image
- **When** the preview image is generated
- **Then** the initials variant is rendered and the route returns 200 rather than an error

### The secondary line is shown when available

- **Given** a contact with position `Head of Sales` at company `Acme GmbH`
- **When** the preview image is generated
- **Then** the secondary line reads the position and company
- **And** for a contact with neither, no empty secondary line is drawn

### No preview image is persisted

- **Given** any number of preview image requests
- **When** they complete
- **Then** no generated image is written to disk or a cache directory

## Request efficiency

### Metadata and page share one backend request

- **Given** a request for `/contacts/{id}`
- **When** `generateMetadata` and the page component both resolve the contact
- **Then** exactly one `GET /api/contacts/{id}` reaches the backend for that render pass

## Error handling

### A missing entity still yields a usable page

- **Given** an id that does not exist
- **When** `/contacts/{id}` is requested
- **Then** `generateMetadata` returns no page-level metadata, the root title applies, and the page
  renders the existing 404

### A backend failure never breaks the page render

- **Given** the backend returns 500 while `generateMetadata` runs
- **When** the page is requested
- **Then** `generateMetadata` does not throw
- **And** the outcome is the normal error page, not a metadata-induced render failure

### A forbidden entity leaks nothing through metadata

- **Given** a user who may not read a particular contact
- **When** they request its detail page
- **Then** the title is the plain `Open CRM` and no name or description appears in the HTML

## Authentication boundary

### The image route stays behind authentication

- **Given** an unauthenticated request
- **When** `/contacts/{id}/opengraph-image` is fetched
- **Then** the middleware redirects to `/login`
- **And** no image bytes and no entity data are returned

### The middleware matcher is unchanged

- **Given** the deployed application after this change
- **When** `frontend/src/middleware.ts` is inspected
- **Then** its matcher is unchanged and no detail or image route has been added to the exclusion list
- **And** the backend still exposes no `permitAll` endpoint
