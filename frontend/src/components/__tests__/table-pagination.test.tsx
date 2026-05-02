import { describe, it, expect, afterEach, beforeEach, vi } from "vitest";
import { cleanup, fireEvent, screen } from "@testing-library/react";
import { TablePagination, type PaginationTranslations } from "@/components/table-pagination";
import { renderWithProviders } from "@/test/test-utils";

const translations: PaginationTranslations = {
  perPage: "pro Seite",
  previous: "Zurück",
  next: "Weiter",
  totalOne: "{count} Eintrag",
  totalOther: "{count} Einträge",
};

const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value;
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key];
    }),
    clear: vi.fn(() => {
      store = {};
    }),
  };
})();
Object.defineProperty(globalThis, "localStorage", { value: localStorageMock });

beforeEach(() => {
  localStorageMock.clear();
  localStorageMock.setItem.mockClear();
});

afterEach(() => {
  cleanup();
});

const defaultProps = {
  page: 0,
  pageSize: 20,
  pageSizeOptions: [10, 20, 50, 100, 200] as const,
  storageKey: "pageSize.test",
  translations,
  onPageChange: vi.fn(),
  onPageSizeChange: vi.fn(),
};

describe("TablePagination", () => {
  it("renders the singular total label when only one entry", () => {
    renderWithProviders(
      <TablePagination {...defaultProps} totalElements={1} totalPages={1} />,
    );
    expect(screen.getByText(/· 1 Eintrag/)).toBeInTheDocument();
  });

  it("renders the plural total label when more than one entry", () => {
    renderWithProviders(
      <TablePagination {...defaultProps} totalElements={35} totalPages={2} />,
    );
    expect(screen.getByText(/· 35 Einträge/)).toBeInTheDocument();
  });

  it("hides previous/next buttons when totalPages <= 1", () => {
    renderWithProviders(
      <TablePagination {...defaultProps} totalElements={5} totalPages={1} />,
    );
    expect(screen.queryByText("Zurück")).toBeNull();
    expect(screen.queryByText("Weiter")).toBeNull();
  });

  it("keeps the page-size selector visible when totalPages <= 1", () => {
    renderWithProviders(
      <TablePagination {...defaultProps} totalElements={5} totalPages={1} />,
    );
    expect(screen.getByText("pro Seite")).toBeInTheDocument();
  });

  it("disables previous on the first page and enables next", () => {
    renderWithProviders(
      <TablePagination {...defaultProps} totalElements={35} totalPages={2} />,
    );
    expect(screen.getByText("Zurück").closest("button")).toBeDisabled();
    expect(screen.getByText("Weiter").closest("button")).not.toBeDisabled();
  });

  it("disables next on the last page", () => {
    renderWithProviders(
      <TablePagination {...defaultProps} page={1} totalElements={35} totalPages={2} />,
    );
    expect(screen.getByText("Weiter").closest("button")).toBeDisabled();
  });

  it("calls onPageChange with page+1 when next is clicked", () => {
    const onPageChange = vi.fn();
    renderWithProviders(
      <TablePagination
        {...defaultProps}
        onPageChange={onPageChange}
        totalElements={35}
        totalPages={2}
      />,
    );
    fireEvent.click(screen.getByText("Weiter"));
    expect(onPageChange).toHaveBeenCalledWith(1);
  });

  it("calls onPageChange with page-1 when previous is clicked", () => {
    const onPageChange = vi.fn();
    renderWithProviders(
      <TablePagination
        {...defaultProps}
        page={1}
        onPageChange={onPageChange}
        totalElements={35}
        totalPages={2}
      />,
    );
    fireEvent.click(screen.getByText("Zurück"));
    expect(onPageChange).toHaveBeenCalledWith(0);
  });

  it("sets aria-label on the size selector", () => {
    renderWithProviders(
      <TablePagination {...defaultProps} totalElements={5} totalPages={1} />,
    );
    expect(screen.getByLabelText("pro Seite")).toBeInTheDocument();
  });
});
