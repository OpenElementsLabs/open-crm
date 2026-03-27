"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { Building2, HeartPulse, Menu, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetTrigger, SheetTitle } from "@/components/ui/sheet";
import { LanguageSwitch } from "@/components/language-switch";
import { useTranslations } from "@/lib/i18n/language-context";
import { cn } from "@/lib/utils";

interface NavItem {
  readonly label: string;
  readonly href: string;
  readonly icon: React.ReactNode;
}

function useNavItems(): readonly NavItem[] {
  const t = useTranslations();
  return [
    { label: t.nav.companies, href: "/companies", icon: <Building2 className="h-5 w-5" /> },
    { label: t.nav.contacts, href: "/contacts", icon: <Users className="h-5 w-5" /> },
    { label: t.nav.health, href: "/health", icon: <HeartPulse className="h-5 w-5" /> },
  ];
}

function NavLinks({ onNavigate }: { readonly onNavigate?: () => void }) {
  const pathname = usePathname();
  const navItems = useNavItems();

  return (
    <nav className="flex flex-col gap-1 px-3 py-4">
      {navItems.map((item) => {
        const isActive = pathname.startsWith(item.href);
        return (
          <Link
            key={item.href}
            href={item.href}
            onClick={onNavigate}
            className={cn(
              "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
              isActive
                ? "bg-oe-green/20 text-oe-green"
                : "text-oe-white/70 hover:bg-oe-white/10 hover:text-oe-white",
            )}
          >
            {item.icon}
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}

function SidebarHeader() {
  const t = useTranslations();
  return (
    <div className="flex h-16 items-center border-b border-oe-white/10 px-6">
      <Link href="/companies" className="font-heading text-lg font-bold text-oe-white">
        {t.app.title}
      </Link>
    </div>
  );
}

export function Sidebar() {
  const t = useTranslations();
  const [open, setOpen] = useState(false);

  return (
    <>
      {/* Desktop sidebar */}
      <aside className="hidden md:flex md:w-64 md:flex-col md:fixed md:inset-y-0 bg-oe-dark">
        <SidebarHeader />
        <NavLinks />
        <div className="mt-auto border-t border-oe-white/10 px-6 py-4">
          <LanguageSwitch />
        </div>
      </aside>

      {/* Mobile header + hamburger */}
      <header className="flex md:hidden h-14 items-center border-b border-oe-gray-light bg-oe-dark px-4">
        <Sheet open={open} onOpenChange={setOpen}>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" className="text-oe-white hover:bg-oe-white/10">
              <Menu className="h-6 w-6" />
              <span className="sr-only">Menu</span>
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="w-64 bg-oe-dark p-0 border-none flex flex-col">
            <SheetTitle className="sr-only">Navigation</SheetTitle>
            <SidebarHeader />
            <NavLinks onNavigate={() => setOpen(false)} />
            <div className="mt-auto border-t border-oe-white/10 px-6 py-4">
              <LanguageSwitch />
            </div>
          </SheetContent>
        </Sheet>
        <span className="ml-3 font-heading text-lg font-bold text-oe-white">{t.app.title}</span>
      </header>
    </>
  );
}
