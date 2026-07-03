# Behaviors: PWA Support (installable frontend)

## Installability

### App is installable on a supporting browser
- **Given** the app is served over HTTPS with a valid manifest and a registered service worker
- **When** a Lighthouse installability audit runs
- **Then** it reports the app as installable

### Installed app launches standalone
- **Given** the app has been installed
- **When** the user launches it from the homescreen/dock
- **Then** it opens in a standalone window (app icon, no browser address bar)

## Service worker — fetch behavior

### Navigation offline serves the offline page
- **Given** the service worker is active and `offline.html` is precached
- **When** a navigation request is made and the network is unavailable
- **Then** the precached `offline.html` is served

### Navigation online is served fresh from the network
- **Given** the service worker is active
- **When** a navigation request is made and the network is available
- **Then** the network response is returned (network-first) and no app HTML is cached

### Non-navigation requests pass through untouched
- **Given** the service worker is active
- **When** a request to `/api/[...path]`, `/api/auth/*`, a JS/CSS/image asset, or an OIDC redirect is made
- **Then** the service worker does not intercept or cache it — it goes straight to the network

### App is fresh after a deploy
- **Given** a new version of the app has been deployed
- **When** the user reloads
- **Then** the fresh app is loaded (only `offline.html` is cached; app chunks/HTML/API are never cached)

## Service worker — lifecycle & versioning

### New service worker activates immediately
- **Given** a new service worker is installed
- **When** it reaches the waiting state
- **Then** `skipWaiting()` + `clients.claim()` make it active immediately without requiring a full restart

### Offline page cache refreshes only when its content changes
- **Given** the offline page is precached under a cache name derived from its content hash
- **When** a deploy changes nothing in `offline.html`
- **Then** the cache is reused (no re-fetch)
- **And When** `offline.html` content changes
- **Then** the cache version changes and the new offline page is cached

## Offline page

### Offline page is bilingual and self-contained
- **Given** the app is opened offline
- **When** the offline page renders
- **Then** it shows the message in both German and English, styled with inline CSS and an embedded logo, with no external resource requests

## Install affordance — desktop/Android

### Install button shows only when installable
- **Given** the browser fired `beforeinstallprompt` and the app is not installed
- **When** the sidebar renders
- **Then** an install button is shown in the sidebar footer

### Install button hidden when not installable
- **Given** the browser did not fire `beforeinstallprompt` (e.g. already installed or unsupported)
- **When** the sidebar renders
- **Then** no install button is shown

### Clicking install triggers the native prompt
- **Given** the install button is visible
- **When** the user clicks it
- **Then** the saved `beforeinstallprompt` event is used to show the native install prompt

### Affordance hides after installation
- **Given** the app has just been installed (`appinstalled` fired / running standalone)
- **When** the sidebar renders
- **Then** the install affordance is no longer shown

## Install affordance — iOS Safari

### iOS shows an instruction hint instead of a button
- **Given** the app runs in iOS Safari and is not in standalone mode
- **When** the sidebar renders
- **Then** the sidebar footer shows an instruction hint ("Share → Add to Home Screen" / "Teilen → Zum Home-Bildschirm") instead of the native-prompt button

### iOS hint hidden when already installed
- **Given** the app runs in iOS standalone mode (already added to homescreen)
- **When** the sidebar renders
- **Then** no install hint is shown

## Install affordance — persistence

### Affordance reappears every session
- **Given** the user ignored or closed the install affordance in a previous session and has not installed the app
- **When** they open a new session
- **Then** the install affordance appears again (no persistent dismissal)

## Manifest & icons

### Manifest is served with English metadata
- **Given** the app is running
- **When** the browser fetches `/manifest.webmanifest`
- **Then** it returns English `name`/`short_name`/`description`, `display: standalone`, white theme/background colors, and the icon set

### Icons include maskable and Apple variants
- **Given** the manifest and head are configured
- **When** a browser/OS reads the icons
- **Then** 192×192, 512×512, a maskable variant, an `apple-touch-icon`, and a favicon are available, with the mark on the green brand background

## Build & deployment

### PWA files are present in the standalone build
- **Given** the prebuild script runs during `pnpm build`
- **When** the Docker image is built
- **Then** `public/` contains the committed icons, the generated `offline.html`, and the generated `sw.js` (with the `offline.html` content-hash injected as cache version), all copied into the image

### offline.html and sw.js are generated, icons committed
- **Given** a fresh checkout
- **When** the prebuild script runs
- **Then** `offline.html` and `sw.js` are generated (icons are already committed static files)

## Generic vs. app-specific boundary

### Offline template ships default bilingual copy
- **Given** the app calls `buildOfflineHtml(...)` with only branding overrides (logo, colors, app name)
- **When** the offline page is generated
- **Then** the default German/English offline copy from the `ui` generator is used, with the app's branding applied

### Manifest built via the generic helper
- **Given** the app's `app/manifest.ts` calls `createManifest({...})` with app values
- **When** the manifest is generated
- **Then** the output reflects the app-provided name, description, colors, and icons
