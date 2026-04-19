"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { useSession } from "next-auth/react";
import { Activity, Building2, CheckSquare, KeyRound, RefreshCw, Settings, Tag, Users, Webhook } from "lucide-react";
import { Sidebar, NavItem, CollapsibleGroup, TooltipProvider } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { getCurrentUser, getUserAvatarUrl, uploadUserAvatar } from "@/lib/api";
import { hasRole, ROLE_IT_ADMIN } from "@/lib/roles";

const ADMIN_PREFIXES = ["/admin", "/api-keys", "/webhooks"];

function isAdminRoute(pathname: string): boolean {
  return ADMIN_PREFIXES.some((prefix) => pathname.startsWith(prefix));
}

function CrmSidebar() {
  const t = useTranslations();
  const pathname = usePathname();
  const { data: session } = useSession();
  const canSeeAdmin = hasRole(session, ROLE_IT_ADMIN);
  const [hasAvatar, setHasAvatar] = useState(false);
  const [avatarKey, setAvatarKey] = useState(0);
  const fileInputRef = useRef<HTMLInputElement>(null);

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
    <>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png"
        onChange={handleAvatarUpload}
        className="hidden"
      />
      <Sidebar
        appTitle={t.app.title}
        homeHref="/companies"
        developedByText={t.app.developedBy}
        menuLabel={t.sidebar.menu}
        user={{
          userName: session?.user?.name ?? "User",
          avatarUrl: hasAvatar ? `${getUserAvatarUrl()}?v=${avatarKey}` : undefined,
          roles: session?.roles,
          onAvatarClick: () => fileInputRef.current?.click(),
          onLogout: () => { window.location.href = "/api/logout"; },
          translations: {
            uploadAvatar: t.sidebar.uploadAvatar,
            logout: t.user.logout,
            noRoles: t.user.noRoles,
          },
        }}
        bottomChildren={
          canSeeAdmin ? (
            <CollapsibleGroup
              icon={<Settings className="h-5 w-5" />}
              label={t.nav.admin}
              defaultOpen={isAdminRoute(pathname)}
              active={isAdminRoute(pathname)}
            >
              <NavItem href="/admin/status" icon={<Activity className="h-5 w-5" />} label={t.nav.serverStatus} active={pathname.startsWith("/admin/status")} indented />
              <NavItem href="/admin/token" icon={<KeyRound className="h-5 w-5" />} label={t.nav.bearerToken} active={pathname.startsWith("/admin/token")} indented />
              <NavItem href="/admin/brevo" icon={<RefreshCw className="h-5 w-5" />} label={t.nav.brevo} active={pathname.startsWith("/admin/brevo")} indented />
              <NavItem href="/api-keys" icon={<KeyRound className="h-5 w-5" />} label={t.nav.apiKeys} active={pathname.startsWith("/api-keys")} indented />
              <NavItem href="/webhooks" icon={<Webhook className="h-5 w-5" />} label={t.nav.webhooks} active={pathname.startsWith("/webhooks")} indented />
            </CollapsibleGroup>
          ) : undefined
        }
      >
        <NavItem href="/companies" icon={<Building2 className="h-5 w-5" />} label={t.nav.companies} active={pathname.startsWith("/companies")} />
        <NavItem href="/contacts" icon={<Users className="h-5 w-5" />} label={t.nav.contacts} active={pathname.startsWith("/contacts")} />
        <NavItem href="/tasks" icon={<CheckSquare className="h-5 w-5" />} label={t.nav.tasks} active={pathname.startsWith("/tasks")} />
        <NavItem href="/tags" icon={<Tag className="h-5 w-5" />} label={t.nav.tags} active={pathname.startsWith("/tags")} />
      </Sidebar>
    </>
  );
}

export default function AppLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <TooltipProvider>
      <CrmSidebar />
      <main className="md:ml-64 h-screen overflow-y-auto bg-oe-white">
        <div className="p-6 md:p-8">{children}</div>
      </main>
    </TooltipProvider>
  );
}
