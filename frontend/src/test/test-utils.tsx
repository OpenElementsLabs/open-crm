import { render, type RenderOptions } from "@testing-library/react";
import { SessionProvider } from "next-auth/react";
import { LanguageProvider } from "@/lib/i18n/language-context";
import type { Language } from "@/lib/i18n/index";
import type { Session } from "next-auth";
interface TestRenderOptions extends Omit<RenderOptions, "wrapper"> {
  readonly language?: Language;
  readonly session?: Session | null;
}

function renderWithProviders(ui: React.ReactElement, options?: TestRenderOptions) {
  const { language = "de", session = null, ...renderOptions } = options ?? {};

  function TestWrapper({ children }: { readonly children: React.ReactNode }) {
    return (
      <SessionProvider session={session}>
        <LanguageProvider defaultLanguage={language}>{children}</LanguageProvider>
      </SessionProvider>
    );
  }

  return render(ui, { wrapper: TestWrapper, ...renderOptions });
}

export { renderWithProviders };
