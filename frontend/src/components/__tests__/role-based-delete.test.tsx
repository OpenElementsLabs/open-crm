import { describe, it, expect, afterEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { TaskDetail } from "@/components/task-detail";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { Session } from "next-auth";
import type { TaskDto } from "@/lib/types";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => "/tasks/test-id",
}));

const mockDeleteTask = vi.fn();

vi.mock("@/lib/api", async () => {
  // Re-use the real ForbiddenError so `instanceof` checks in the component code
  // match what the test throws. The separate forbidden-error module avoids the
  // next-auth chain that @/lib/api pulls in via @/auth.
  const { ForbiddenError } = await import("@/lib/forbidden-error");
  return {
    deleteTask: (...args: unknown[]) => mockDeleteTask(...args),
    getTaskComments: vi.fn().mockResolvedValue({
      content: [],
      page: { number: 0, size: 20, totalElements: 0, totalPages: 0 },
    }),
    createTaskComment: vi.fn(),
    ForbiddenError,
  };
});

function sessionWithRoles(roles: string[]): Session {
  return {
    user: { name: "Alice", email: "alice@example.com", image: null },
    expires: new Date(Date.now() + 86400000).toISOString(),
    roles,
  };
}

const task: TaskDto = {
  id: "test-id",
  action: "Prepare quarterly report",
  dueDate: "2026-04-30",
  status: "OPEN",
  companyId: null,
  companyName: null,
  contactId: null,
  contactName: null,
  tagIds: [],
  commentCount: 0,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

afterEach(() => {
  cleanup();
  mockDeleteTask.mockReset();
});

describe("Role-based delete button (TaskDetail)", () => {
  it("is disabled for a user without ADMIN role", () => {
    renderWithProviders(<TaskDetail task={task} />, {
      session: sessionWithRoles([]),
    });

    const deleteButton = screen.getByRole("button", { name: new RegExp(de.tasks.deleteDialog.confirm) });
    expect(deleteButton).toBeDisabled();
  });

  it("is disabled for a user with only IT-ADMIN", () => {
    renderWithProviders(<TaskDetail task={task} />, {
      session: sessionWithRoles(["IT-ADMIN"]),
    });

    const deleteButton = screen.getByRole("button", { name: new RegExp(de.tasks.deleteDialog.confirm) });
    expect(deleteButton).toBeDisabled();
  });

  it("is enabled for a user with ADMIN role", () => {
    renderWithProviders(<TaskDetail task={task} />, {
      session: sessionWithRoles(["ADMIN"]),
    });

    const deleteButton = screen.getByRole("button", { name: new RegExp(de.tasks.deleteDialog.confirm) });
    expect(deleteButton).not.toBeDisabled();
  });

  it("is enabled for a user with both ADMIN and IT-ADMIN", () => {
    renderWithProviders(<TaskDetail task={task} />, {
      session: sessionWithRoles(["ADMIN", "IT-ADMIN"]),
    });

    const deleteButton = screen.getByRole("button", { name: new RegExp(de.tasks.deleteDialog.confirm) });
    expect(deleteButton).not.toBeDisabled();
  });

  it("clicking the disabled button does not open the confirmation dialog", () => {
    renderWithProviders(<TaskDetail task={task} />, {
      session: sessionWithRoles([]),
    });

    const deleteButton = screen.getByRole("button", { name: new RegExp(de.tasks.deleteDialog.confirm) });
    fireEvent.click(deleteButton);

    expect(screen.queryByText(de.tasks.deleteDialog.description)).not.toBeInTheDocument();
    expect(mockDeleteTask).not.toHaveBeenCalled();
  });

  it("shows permission-specific error when the DELETE returns 403", async () => {
    const { ForbiddenError } = await import("@/lib/forbidden-error");
    mockDeleteTask.mockRejectedValue(new ForbiddenError());

    renderWithProviders(<TaskDetail task={task} />, {
      session: sessionWithRoles(["ADMIN"]),
    });

    // Open the confirmation dialog
    const deleteButtons = screen.getAllByRole("button", { name: new RegExp(de.tasks.deleteDialog.confirm) });
    fireEvent.click(deleteButtons[0]);

    // Confirm the delete inside the dialog
    await waitFor(() => {
      expect(screen.getByText(de.tasks.deleteDialog.description)).toBeInTheDocument();
    });
    const confirmButtons = screen.getAllByRole("button", { name: new RegExp(de.tasks.deleteDialog.confirm) });
    // The last button with the confirm label is the one inside the dialog.
    fireEvent.click(confirmButtons[confirmButtons.length - 1]);

    await waitFor(() => {
      expect(
        screen.getByText(de.errors.forbidden.deleteNoPermission),
      ).toBeInTheDocument();
    });
  });
});
