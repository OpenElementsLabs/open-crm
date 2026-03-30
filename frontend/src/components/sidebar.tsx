"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { useSession } from "next-auth/react";
import { Building2, CircleUser, LayoutDashboard, LogOut, Menu, Settings, Tag, Users } from "lucide-react";
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

function NavLinks({ onNavigate }: { readonly onNavigate?: () => void }) {
  const t = useTranslations();
  const pathname = usePathname();

  const mainItems: NavItem[] = [
    { label: t.nav.companies, href: "/companies", icon: <Building2 className="h-5 w-5" /> },
    { label: t.nav.contacts, href: "/contacts", icon: <Users className="h-5 w-5" /> },
    { label: t.nav.tags, href: "/tags", icon: <Tag className="h-5 w-5" /> },
  ];

  const adminItem: NavItem = {
    label: t.nav.admin,
    href: "/admin",
    icon: <Settings className="h-5 w-5" />,
  };

  function renderItem(item: NavItem) {
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
  }

  return (
    <div className="flex flex-1 flex-col">
      <nav className="flex flex-col gap-1 px-3 py-4">
        {mainItems.map(renderItem)}
      </nav>
      <div className="mt-auto px-3 pb-2">
        {renderItem(adminItem)}
      </div>
    </div>
  );
}

function SidebarHeader() {
  const t = useTranslations();
  return (
    <div className="border-b border-oe-white/10 px-6 py-4">
      <Link href="/companies" className="flex items-center gap-3">
        <LayoutDashboard className="h-6 w-6 text-oe-green" />
        <span className="font-heading text-lg font-bold text-oe-white">{t.app.title}</span>
      </Link>
      <a
        href="https://open-elements.com"
        target="_blank"
        rel="noopener noreferrer"
        className="mt-2 flex items-center gap-2 opacity-70 hover:opacity-100 transition-opacity"
      >
        <span className="text-xs text-oe-gray-light">{t.app.developedBy}</span>
        <img
          src="/oe-logo-landscape-dark.svg"
          alt="Open Elements"
          className="h-4"
        />
      </a>
    </div>
  );
}

function UserSection() {
  const t = useTranslations();
  const { data: session } = useSession();

  const userName = session?.user?.name ?? "User";
  const userImage = session?.user?.image;

  return (
    <div className="border-t border-oe-white/10 px-6 py-4">
      <div className="flex items-center gap-3">
        {userImage ? (
          <img
            src={userImage}
            alt={userName}
            className="h-8 w-8 rounded-full object-cover"
          />
        ) : (
          <CircleUser className="h-8 w-8 text-oe-gray-light" />
        )}
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-oe-white truncate">{userName}</p>
        </div>
        <Button
          variant="ghost"
          size="icon"
          className="text-oe-white/70 hover:bg-oe-white/10 hover:text-oe-white"
          onClick={() => { window.location.href = "/api/logout"; }}
          title={t.user.logout}
        >
          <LogOut className="h-4 w-4" />
        </Button>
      </div>
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
        <div>
          <div className="border-t border-oe-white/10 px-6 py-4">
            <LanguageSwitch />
          </div>
          <UserSection />
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
            <div>
              <div className="border-t border-oe-white/10 px-6 py-4">
                <LanguageSwitch />
              </div>
              <UserSection />
            </div>
          </SheetContent>
        </Sheet>
        <span className="ml-3 font-heading text-lg font-bold text-oe-white">{t.app.title}</span>
      </header>
    </>
  );
}
