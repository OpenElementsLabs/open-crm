import { render, type RenderOptions } from "@testing-library/react";
import { LanguageProvider } from "@/lib/i18n/language-context";
import type { Language } from "@/lib/i18n/index";

interface TestRenderOptions extends Omit<RenderOptions, "wrapper"> {
  readonly language?: Language;
}

function renderWithProviders(ui: React.ReactElement, options?: TestRenderOptions) {
  const { language = "de", ...renderOptions } = options ?? {};

  function TestWrapper({ children }: { readonly children: React.ReactNode }) {
    return <LanguageProvider defaultLanguage={language}>{children}</LanguageProvider>;
  }

  return render(ui, { wrapper: TestWrapper, ...renderOptions });
}

export { renderWithProviders };
