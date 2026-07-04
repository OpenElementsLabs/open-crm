/**
 * GENERIC — extraction target for `@open-elements/ui`.
 *
 * Produces a fully self-contained offline fallback page (inline CSS, embedded logo, no external
 * requests). Ships default German + English copy; the app passes only branding overrides. Because the
 * page never references the app's hashed chunks, a deploy can never break it. See
 * `docs/specs/111-pwa-support/design.md`.
 */
export interface OfflineMessage {
  readonly title: string;
  readonly body: string;
}

export interface BuildOfflineHtmlOptions {
  /** Application name shown as the page/heading brand. */
  readonly appName: string;
  /** Accent color (headings). */
  readonly primaryColor: string;
  /** Page background color. */
  readonly backgroundColor: string;
  /** Raw inline SVG markup for the logo (embedded directly — no external file). */
  readonly logoSvg: string;
  /** Optional copy overrides; defaults to the bundled DE/EN copy. */
  readonly messages?: {
    readonly de?: OfflineMessage;
    readonly en?: OfflineMessage;
  };
}

/** Default bilingual offline copy shipped by the (future) library. */
export const DEFAULT_OFFLINE_MESSAGES: { readonly de: OfflineMessage; readonly en: OfflineMessage } = {
  de: {
    title: "Keine Verbindung",
    body: "Du bist offline. Bitte stelle eine Internetverbindung her und versuche es erneut.",
  },
  en: {
    title: "You're offline",
    body: "You appear to be offline. Please check your connection and try again.",
  },
};

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/**
 * Builds the offline page HTML string.
 *
 * @param options branding + optional copy overrides
 * @return a self-contained HTML document
 */
export function buildOfflineHtml(options: BuildOfflineHtmlOptions): string {
  const de = options.messages?.de ?? DEFAULT_OFFLINE_MESSAGES.de;
  const en = options.messages?.en ?? DEFAULT_OFFLINE_MESSAGES.en;
  const appName = escapeHtml(options.appName);
  return `<!doctype html>
<html lang="de">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>${appName} — offline</title>
    <style>
      * { box-sizing: border-box; }
      html, body { height: 100%; margin: 0; }
      body {
        display: flex; align-items: center; justify-content: center;
        min-height: 100vh; padding: 24px;
        font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
        background: ${options.backgroundColor}; color: #1f2937;
      }
      .card { max-width: 28rem; text-align: center; }
      .logo { width: 220px; max-width: 70%; height: auto; margin: 0 auto 2rem; }
      h1 { font-size: 1.5rem; margin: 0 0 .5rem; color: ${options.primaryColor}; }
      p { margin: 0 0 1.5rem; line-height: 1.5; color: #4b5563; }
      .divider { border: none; border-top: 1px solid #e5e7eb; margin: 1.5rem 0; }
      .lang { font-size: .75rem; letter-spacing: .08em; text-transform: uppercase; color: #9ca3af; margin-bottom: .25rem; }
    </style>
  </head>
  <body>
    <main class="card">
      <div class="logo">${options.logoSvg}</div>
      <p class="lang">Deutsch</p>
      <h1>${escapeHtml(de.title)}</h1>
      <p>${escapeHtml(de.body)}</p>
      <hr class="divider" />
      <p class="lang">English</p>
      <h1>${escapeHtml(en.title)}</h1>
      <p>${escapeHtml(en.body)}</p>
    </main>
  </body>
</html>
`;
}
