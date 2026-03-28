import { describe, it, expect, afterEach, vi } from "vitest";
import { render, screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { ContactComments } from "@/components/contact-comments";
import { LanguageProvider } from "@/lib/i18n/language-context";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { CommentDto, Page } from "@/lib/types";

const S = de.companies.comments;

const mockGetContactComments = vi.fn();
const mockCreateContactComment = vi.fn();

vi.mock("@/lib/api", () => ({
  getContactComments: (...args: unknown[]) => mockGetContactComments(...args),
  createContactComment: (...args: unknown[]) => mockCreateContactComment(...args),
}));

function makeComment(overrides: Partial<CommentDto> = {}): CommentDto {
  return {
    id: "comment-1",
    text: "Test comment",
    author: "UNKNOWN",
    companyId: null,
    contactId: "contact-1",
    createdAt: "2026-03-27T15:30:00Z",
    updatedAt: "2026-03-27T15:30:00Z",
    ...overrides,
  };
}

function makePage(comments: CommentDto[], last: boolean = true): Page<CommentDto> {
  return {
    content: comments,
    totalElements: comments.length,
    totalPages: last ? 1 : 2,
    number: 0,
    size: 20,
    first: true,
    last,
  };
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("ContactComments", () => {
  describe("comment count live update", () => {
    it("should show totalCount in heading", async () => {
      mockGetContactComments.mockResolvedValue(makePage([]));

      renderWithProviders(<ContactComments contactId="contact-1" totalCount={2} />);

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (2)`)).toBeInTheDocument();
      });
    });

    it("should increment count after adding a comment", async () => {
      mockGetContactComments.mockResolvedValue(makePage([]));
      mockCreateContactComment.mockResolvedValue(
        makeComment({ id: "new", text: "New comment" }),
      );

      renderWithProviders(<ContactComments contactId="contact-1" totalCount={2} />);

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (2)`)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.add));

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "New comment" },
      });
      fireEvent.click(screen.getByText(S.send));

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (3)`)).toBeInTheDocument();
      });
    });

    it("should not increment count on API failure", async () => {
      mockGetContactComments.mockResolvedValue(makePage([]));
      mockCreateContactComment.mockRejectedValue(new Error("Server error"));

      renderWithProviders(<ContactComments contactId="contact-1" totalCount={2} />);

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (2)`)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.add));

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "Will fail" },
      });
      fireEvent.click(screen.getByText(S.send));

      await waitFor(() => {
        expect(screen.getByText(S.errorGeneric)).toBeInTheDocument();
      });

      expect(screen.getByText(`${S.title} (2)`)).toBeInTheDocument();
    });

    it("should not show count when totalCount is undefined", async () => {
      mockGetContactComments.mockResolvedValue(makePage([]));
      mockCreateContactComment.mockResolvedValue(
        makeComment({ id: "new", text: "New comment" }),
      );

      renderWithProviders(<ContactComments contactId="contact-1" />);

      await waitFor(() => {
        expect(screen.getByText(S.title)).toBeInTheDocument();
      });

      expect(screen.queryByText(/\(\d+\)/)).not.toBeInTheDocument();

      fireEvent.click(screen.getByText(S.add));

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "New comment" },
      });
      fireEvent.click(screen.getByText(S.send));

      await waitFor(() => {
        expect(screen.getByText("New comment")).toBeInTheDocument();
      });

      expect(screen.queryByText(/\(\d+\)/)).not.toBeInTheDocument();
    });

    it("should reset count when totalCount prop changes", async () => {
      mockGetContactComments.mockResolvedValue(makePage([]));
      mockCreateContactComment.mockResolvedValue(
        makeComment({ id: "new", text: "New comment" }),
      );

      const { rerender } = render(
        <LanguageProvider defaultLanguage="de">
          <ContactComments contactId="contact-1" totalCount={2} />
        </LanguageProvider>,
      );

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (2)`)).toBeInTheDocument();
      });

      // Add a comment to increment to 3
      fireEvent.click(screen.getByText(S.add));
      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });
      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "New comment" },
      });
      fireEvent.click(screen.getByText(S.send));
      await waitFor(() => {
        expect(screen.getByText(`${S.title} (3)`)).toBeInTheDocument();
      });

      // Simulate navigation to a different contact by changing the prop
      rerender(
        <LanguageProvider defaultLanguage="de">
          <ContactComments contactId="contact-2" totalCount={0} />
        </LanguageProvider>,
      );

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (0)`)).toBeInTheDocument();
      });
    });
  });
});
