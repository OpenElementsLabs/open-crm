import type { Metadata } from "next";
import { Montserrat, Lato } from "next/font/google";
import { Sidebar } from "@/components/sidebar";
import { LanguageProvider } from "@/lib/i18n/language-context";
import "./globals.css";

const montserrat = Montserrat({
  subsets: ["latin"],
  variable: "--font-heading",
  display: "swap",
});

const lato = Lato({
  subsets: ["latin"],
  weight: ["400", "700"],
  variable: "--font-body",
  display: "swap",
});

export const metadata: Metadata = {
  title: "Open CRM",
  description: "CRM system by Open Elements",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={`${montserrat.variable} ${lato.variable}`}>
      <body className="antialiased">
        <LanguageProvider>
          <Sidebar />
          <main className="md:ml-64 min-h-screen bg-oe-white">
            <div className="p-6 md:p-8">{children}</div>
          </main>
        </LanguageProvider>
      </body>
    </html>
  );
}
