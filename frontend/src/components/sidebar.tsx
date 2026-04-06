"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useSession } from "next-auth/react";
import { Activity, Building2, CheckSquare, ChevronDown, CircleUser, KeyRound, LayoutDashboard, LogOut, Menu, RefreshCw, Settings, Tag, Users, Webhook } from "lucide-react";
import { getCurrentUser, getUserAvatarUrl, uploadUserAvatar } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipTrigger, TooltipContent } from "@/components/ui/tooltip";
import { Sheet, SheetContent, SheetTrigger, SheetTitle } from "@/components/ui/sheet";
import { LanguageSwitch } from "@/components/language-switch";
import { useTranslations } from "@/lib/i18n/language-context";
import { cn } from "@/lib/utils";

interface NavItem {
  readonly label: string;
  readonly href: string;
  readonly icon: React.ReactNode;
}

const ADMIN_PREFIXES = ["/admin", "/api-keys", "/webhooks"];

function isAdminRoute(pathname: string): boolean {
  return ADMIN_PREFIXES.some((prefix) => pathname.startsWith(prefix));
}

function NavLinks({ onNavigate, mobile }: { readonly onNavigate?: () => void; readonly mobile?: boolean }) {
  const t = useTranslations();
  const pathname = usePathname();
  const [adminOpen, setAdminOpen] = useState(() => isAdminRoute(pathname));

  // Sync admin open state with pathname changes
  useEffect(() => {
    setAdminOpen(isAdminRoute(pathname));
  }, [pathname]);

  const mainItems: NavItem[] = [
    { label: t.nav.companies, href: "/companies", icon: <Building2 className="h-5 w-5" /> },
    { label: t.nav.contacts, href: "/contacts", icon: <Users className="h-5 w-5" /> },
    { label: t.nav.tasks, href: "/tasks", icon: <CheckSquare className="h-5 w-5" /> },
    { label: t.nav.tags, href: "/tags", icon: <Tag className="h-5 w-5" /> },
  ];

  const adminSubItems: NavItem[] = [
    { label: t.nav.serverStatus, href: "/admin/status", icon: <Activity className="h-5 w-5" /> },
    { label: t.nav.bearerToken, href: "/admin/token", icon: <KeyRound className="h-5 w-5" /> },
    { label: t.nav.brevo, href: "/admin/brevo", icon: <RefreshCw className="h-5 w-5" /> },
    { label: t.nav.apiKeys, href: "/api-keys", icon: <KeyRound className="h-5 w-5" /> },
    { label: t.nav.webhooks, href: "/webhooks", icon: <Webhook className="h-5 w-5" /> },
  ];

  const anyAdminActive = isAdminRoute(pathname);

  function renderItem(item: NavItem, indented?: boolean) {
    const isActive = pathname.startsWith(item.href);
    return (
      <Link
        key={item.href}
        href={item.href}
        onClick={onNavigate}
        className={cn(
          "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
          indented && "pl-10",
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
        {mainItems.map((item) => renderItem(item))}
      </nav>
      <div className="mt-auto flex flex-col gap-1 px-3 pb-2">
        {mobile ? (
          /* Mobile: flat list of all admin sub-items */
          adminSubItems.map((item) => renderItem(item))
        ) : (
          /* Desktop: collapsible admin group */
          <>
            <button
              type="button"
              onClick={() => setAdminOpen((prev) => !prev)}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors w-full text-left",
                anyAdminActive
                  ? "text-oe-white"
                  : "text-oe-white/70 hover:bg-oe-white/10 hover:text-oe-white",
              )}
            >
              <Settings className="h-5 w-5" />
              {t.nav.admin}
              <ChevronDown
                className={cn(
                  "ml-auto h-4 w-4 transition-transform",
                  adminOpen ? "rotate-0" : "-rotate-90",
                )}
              />
            </button>
            {adminOpen && (
              <div className="flex flex-col gap-1">
                {adminSubItems.map((item) => renderItem(item, true))}
              </div>
            )}
          </>
        )}
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
  const [hasAvatar, setHasAvatar] = useState(false);
  const [avatarKey, setAvatarKey] = useState(0);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const userName = session?.user?.name ?? "User";

  useEffect(() => {
    getCurrentUser()
      .then((user) => setHasAvatar(user.hasAvatar))
      .catch(() => {});
  }, []);

  const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const updated = await uploadUserAvatar(file);
      setHasAvatar(updated.hasAvatar);
      setAvatarKey((k) => k + 1);
    } catch {
      // ignore
    }
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  return (
    <div className="border-t border-oe-white/10 px-6 py-4">
      <div className="flex items-center gap-3">
        <Tooltip>
          <TooltipTrigger asChild>
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="shrink-0"
            >
              {hasAvatar ? (
                <img
                  key={avatarKey}
                  src={getUserAvatarUrl()}
                  alt={userName}
                  className="h-8 w-8 rounded-full object-cover"
                />
              ) : (
                <CircleUser className="h-8 w-8 text-oe-gray-light" />
              )}
            </button>
          </TooltipTrigger>
          <TooltipContent>{t.sidebar.uploadAvatar}</TooltipContent>
        </Tooltip>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png"
          onChange={handleAvatarUpload}
          className="hidden"
        />
        <div className="flex-1 min-w-0">
          <Tooltip>
            <TooltipTrigger asChild>
              <p className="text-sm font-medium text-oe-white truncate cursor-default">{userName}</p>
            </TooltipTrigger>
            <TooltipContent>
              {session?.roles && session.roles.length > 0
                ? session.roles.join(", ")
                : t.user.noRoles}
            </TooltipContent>
          </Tooltip>
        </div>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="text-oe-white/70 hover:bg-oe-white/10 hover:text-oe-white"
              onClick={() => { window.location.href = "/api/logout"; }}
            >
              <LogOut className="h-4 w-4" />
            </Button>
          </TooltipTrigger>
          <TooltipContent>{t.user.logout}</TooltipContent>
        </Tooltip>
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
          <Tooltip>
            <TooltipTrigger asChild>
              <SheetTrigger asChild>
                <Button variant="ghost" size="icon" className="text-oe-white hover:bg-oe-white/10">
                  <Menu className="h-6 w-6" />
                  <span className="sr-only">{t.sidebar.menu}</span>
                </Button>
              </SheetTrigger>
            </TooltipTrigger>
            <TooltipContent>{t.sidebar.menu}</TooltipContent>
          </Tooltip>
          <SheetContent side="left" className="w-64 bg-oe-dark p-0 border-none flex flex-col">
            <SheetTitle className="sr-only">Navigation</SheetTitle>
            <SidebarHeader />
            <NavLinks onNavigate={() => setOpen(false)} mobile />
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
