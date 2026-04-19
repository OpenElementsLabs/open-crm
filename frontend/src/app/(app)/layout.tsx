import { Sidebar } from "@/components/sidebar";
import { TooltipProvider } from "@open-elements/ui";

export default function AppLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <TooltipProvider>
      <Sidebar />
      <main className="md:ml-64 h-screen overflow-y-auto bg-oe-white">
        <div className="p-6 md:p-8">{children}</div>
      </main>
    </TooltipProvider>
  );
}
