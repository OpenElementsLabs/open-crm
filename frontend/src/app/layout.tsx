import type { Metadata, Viewport } from "next";
import { OERootLayout } from "@open-elements/nextjs-app-layer/layout";
import { translations } from "@/lib/i18n";
import { RegisterServiceWorker } from "@/components/pwa/register-service-worker";
import { SITE_DESCRIPTION, SITE_NAME, TITLE_TEMPLATE } from "@/lib/metadata/site";
import "./globals.css";

// Absolute base for Open Graph image URLs. Without it Next.js warns at build time and resolves
// image URLs against localhost. AUTH_URL is already present in every environment; the localhost
// fallback is for local development only.
const metadataBase = new URL(process.env.AUTH_URL ?? "http://localhost:3000");

export const metadata: Metadata = {
  metadataBase,
  title: {
    default: SITE_NAME,
    template: TITLE_TEMPLATE,
  },
  description: SITE_DESCRIPTION,
  // The application is not reachable unauthenticated, so this is defence in depth and the honest
  // declaration for an internal CRM — no page should ever be indexed.
  robots: { index: false, follow: false },
  applicationName: "Open CRM",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "Open CRM",
  },
  icons: {
    icon: [
      { url: "/favicon-32.png", sizes: "32x32", type: "image/png" },
      { url: "/favicon-16.png", sizes: "16x16", type: "image/png" },
    ],
    apple: [{ url: "/apple-touch-icon.png", sizes: "180x180", type: "image/png" }],
  },
};

export const viewport: Viewport = {
  themeColor: "#ffffff",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <OERootLayout translations={translations}>
      <RegisterServiceWorker />
      {children}
    </OERootLayout>
  );
}
