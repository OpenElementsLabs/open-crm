import type { Metadata, Viewport } from "next";
import { OERootLayout } from "@open-elements/nextjs-app-layer/layout";
import { translations } from "@/lib/i18n";
import { RegisterServiceWorker } from "@/components/pwa/register-service-worker";
import "./globals.css";

export const metadata: Metadata = {
  title: "Open CRM",
  description: "CRM system by Open Elements",
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
