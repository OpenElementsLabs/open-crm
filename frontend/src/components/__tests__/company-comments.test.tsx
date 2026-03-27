import { describe, it, expect, afterEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { CompanyComments } from "@/components/company-comments";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { CommentDto, Page } from "@/lib/types";

const S = de.companies.comments;

const mockGetCompanyComments = vi.fn();
const mockCreateCompanyComment = vi.fn();

vi.mock("@/lib/api", () => ({
  getCompanyComments: (...args: unknown[]) => mockGetCompanyComments(...args),
  createCompanyComment: (...args: unknown[]) => mockCreateCompanyComment(...args),
}));

function makeComment(overrides: Partial<CommentDto> = {}): CommentDto {
  return {
    id: "comment-1",
    text: "Test comment",
    author: "UNKNOWN",
    companyId: "company-1",
    contactId: null,
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

describe("CompanyComments", () => {
  describe("display", () => {
    it("should render comments with author, date, and text", async () => {
      mockGetCompanyComments.mockResolvedValue(
        makePage([
          makeComment({ id: "1", text: "First comment", author: "UNKNOWN" }),
          makeComment({ id: "2", text: "Second comment", author: "UNKNOWN" }),
        ]),
      );

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText("First comment")).toBeInTheDocument();
        expect(screen.getByText("Second comment")).toBeInTheDocument();
        expect(screen.getAllByText(/UNKNOWN/).length).toBeGreaterThanOrEqual(2);
      });
    });

    it("should show empty state when no comments", async () => {
      mockGetCompanyComments.mockResolvedValue(makePage([]));

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText(S.empty)).toBeInTheDocument();
      });

      // Input field should still be visible
      expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
    });

    it("should show loading skeleton while fetching", () => {
      mockGetCompanyComments.mockReturnValue(new Promise(() => {}));

      const { container } = renderWithProviders(<CompanyComments companyId="company-1" />);

      const skeletons = container.querySelectorAll("[data-slot='skeleton']");
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it("should format dates in readable format", async () => {
      mockGetCompanyComments.mockResolvedValue(
        makePage([makeComment({ createdAt: "2026-03-27T15:30:00Z" })]),
      );

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        // Should contain date parts (format varies by locale)
        const dateText = screen.getByText(/27/);
        expect(dateText).toBeInTheDocument();
      });
    });
  });

  describe("create", () => {
    it("should disable send button when text is empty", async () => {
      mockGetCompanyComments.mockResolvedValue(makePage([]));

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        const sendButton = screen.getByText(S.send);
        expect(sendButton.closest("button")).toBeDisabled();
      });
    });

    it("should disable send button when only whitespace", async () => {
      mockGetCompanyComments.mockResolvedValue(makePage([]));

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "   " },
      });

      const sendButton = screen.getByText(S.send);
      expect(sendButton.closest("button")).toBeDisabled();
    });

    it("should create comment and add to top of list", async () => {
      mockGetCompanyComments.mockResolvedValue(
        makePage([makeComment({ id: "1", text: "Existing comment" })]),
      );
      mockCreateCompanyComment.mockResolvedValue(
        makeComment({ id: "2", text: "New comment" }),
      );

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText("Existing comment")).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "New comment" },
      });

      fireEvent.click(screen.getByText(S.send));

      await waitFor(() => {
        expect(mockCreateCompanyComment).toHaveBeenCalledWith("company-1", { text: "New comment" });
        expect(screen.getByText("New comment")).toBeInTheDocument();
      });

      // Textarea should be cleared
      const textarea = screen.getByPlaceholderText(S.placeholder) as HTMLTextAreaElement;
      expect(textarea.value).toBe("");
    });

    it("should send only text field, no author", async () => {
      mockGetCompanyComments.mockResolvedValue(makePage([]));
      mockCreateCompanyComment.mockResolvedValue(makeComment({ text: "Test" }));

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "Test" },
      });

      fireEvent.click(screen.getByText(S.send));

      await waitFor(() => {
        expect(mockCreateCompanyComment).toHaveBeenCalledWith("company-1", { text: "Test" });
        // Verify no author in the call
        const callArgs = mockCreateCompanyComment.mock.calls[0][1];
        expect(callArgs).not.toHaveProperty("author");
      });
    });

    it("should show error dialog on API failure and preserve text", async () => {
      mockGetCompanyComments.mockResolvedValue(makePage([]));
      mockCreateCompanyComment.mockRejectedValue(new Error("Server error"));

      renderWithProviders(<CompanyComments companyId="company-1" />);

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

      // Text should be preserved
      const textarea = screen.getByPlaceholderText(S.placeholder) as HTMLTextAreaElement;
      expect(textarea.value).toBe("Will fail");
    });

    it("should disable send button while sending", async () => {
      mockGetCompanyComments.mockResolvedValue(makePage([]));
      mockCreateCompanyComment.mockReturnValue(new Promise(() => {})); // never resolves

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "Sending..." },
      });

      fireEvent.click(screen.getByText(S.send));

      await waitFor(() => {
        expect(screen.getByText(S.sending)).toBeInTheDocument();
        expect(screen.getByText(S.sending).closest("button")).toBeDisabled();
      });
    });
  });

  describe("pagination", () => {
    it("should show load more button when more pages exist", async () => {
      mockGetCompanyComments.mockResolvedValue(
        makePage([makeComment()], false),
      );

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText(S.loadMore)).toBeInTheDocument();
      });
    });

    it("should not show load more button when all loaded", async () => {
      mockGetCompanyComments.mockResolvedValue(
        makePage([makeComment()], true),
      );

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText("Test comment")).toBeInTheDocument();
      });

      expect(screen.queryByText(S.loadMore)).not.toBeInTheDocument();
    });

    it("should append comments when load more is clicked", async () => {
      mockGetCompanyComments
        .mockResolvedValueOnce(makePage([makeComment({ id: "1", text: "First" })], false))
        .mockResolvedValueOnce({
          content: [makeComment({ id: "2", text: "Second" })],
          totalElements: 2,
          totalPages: 2,
          number: 1,
          size: 20,
          first: false,
          last: true,
        });

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText("First")).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.loadMore));

      await waitFor(() => {
        expect(screen.getByText("First")).toBeInTheDocument();
        expect(screen.getByText("Second")).toBeInTheDocument();
      });
    });
  });
});
