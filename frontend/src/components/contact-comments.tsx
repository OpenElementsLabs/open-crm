"use client";

import { useCallback, useEffect, useState } from "react";
import { MessageSquarePlus, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { AddCommentDialog } from "@/components/add-comment-dialog";
import { DeleteConfirmDialog } from "@/components/delete-confirm-dialog";
import { getContactComments, createContactComment, deleteComment } from "@/lib/api";
import type { CommentDto } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

function formatDate(dateString: string, language: string): string {
  const date = new Date(dateString);
  return date.toLocaleString(language === "de" ? "de-DE" : "en-US", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface ContactCommentsProps {
  readonly contactId: string;
  readonly totalCount?: number;
}

export function ContactComments({ contactId, totalCount }: ContactCommentsProps) {
  const t = useTranslations();
  const S = t.companies.comments;

  const [displayCount, setDisplayCount] = useState(totalCount);
  const [comments, setComments] = useState<CommentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [sending, setSending] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const fetchComments = useCallback(async (pageNum: number, append: boolean) => {
    if (append) {
      setLoadingMore(true);
    } else {
      setLoading(true);
    }
    try {
      const result = await getContactComments(contactId, pageNum);
      if (append) {
        setComments((prev) => [...prev, ...result.content]);
      } else {
        setComments([...result.content]);
      }
      setHasMore(result.page.number < result.page.totalPages - 1);
      setPage(pageNum);
    } catch {
      if (!append) {
        setComments([]);
      }
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, [contactId]);

  useEffect(() => {
    setDisplayCount(totalCount);
  }, [totalCount]);

  useEffect(() => {
    fetchComments(0, false);
  }, [fetchComments]);

  const handleSend = async (text: string) => {
    setSending(true);
    try {
      const newComment = await createContactComment(contactId, { text });
      setComments((prev) => [newComment, ...prev]);
      setDisplayCount((prev) => (prev !== undefined ? prev + 1 : undefined));
    } finally {
      setSending(false);
    }
  };

  const handleLoadMore = () => {
    fetchComments(page + 1, true);
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteComment(deleteTarget);
      setComments((prev) => prev.filter((c) => c.id !== deleteTarget));
      setDisplayCount((prev) => (prev !== undefined && prev > 0 ? prev - 1 : prev));
      setDeleteTarget(null);
      setDeleteError(null);
    } catch {
      setDeleteError(S.deleteDialog.title);
    }
  };

  return (
    <Card className="border-oe-gray-light">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="font-heading text-lg text-oe-dark">
          {S.title}{displayCount !== undefined ? ` (${displayCount})` : ""}
        </CardTitle>
        <Button
          onClick={() => setDialogOpen(true)}
          className="bg-oe-green hover:bg-oe-green-dark text-white"
          size="sm"
        >
          <MessageSquarePlus className="mr-2 h-4 w-4" />
          {S.add}
        </Button>
      </CardHeader>
      <CardContent>
        {loading && (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="space-y-2">
                <Skeleton className="h-4 w-1/3" />
                <Skeleton className="h-4 w-full" />
              </div>
            ))}
          </div>
        )}

        {!loading && comments.length === 0 && (
          <p className="text-sm text-oe-gray-mid">{S.empty}</p>
        )}

        {!loading && comments.length > 0 && (
          <div className="space-y-4">
            {comments.map((comment) => (
              <div key={comment.id} className="border-b border-oe-gray-light pb-4 last:border-b-0">
                <div className="flex items-start justify-between">
                  <p className="text-xs text-oe-gray-mid">
                    {comment.author} &middot; {formatDate(comment.createdAt, "de")}
                  </p>
                  <button
                    type="button"
                    onClick={() => { setDeleteTarget(comment.id); setDeleteError(null); }}
                    className="text-oe-red hover:text-oe-red-dark shrink-0 ml-2"
                    title={S.deleteDialog.title}
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
                <p className="mt-1 text-sm text-oe-black whitespace-pre-wrap break-words">
                  {comment.text}
                </p>
              </div>
            ))}
          </div>
        )}

        {!loading && hasMore && (
          <div className="mt-4 flex justify-center">
            <Button
              variant="outline"
              size="sm"
              onClick={handleLoadMore}
              disabled={loadingMore}
            >
              {S.loadMore}
            </Button>
          </div>
        )}
      </CardContent>

      <AddCommentDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onSubmit={handleSend}
        sending={sending}
        title={S.addTitle}
        placeholder={S.placeholder}
        sendLabel={S.send}
        sendingLabel={S.sending}
        errorTitle={S.errorTitle}
        errorMessage={S.errorGeneric}
      />

      <DeleteConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => { if (!open) { setDeleteTarget(null); setDeleteError(null); } }}
        title={S.deleteDialog.title}
        description={S.deleteDialog.description}
        confirmLabel={S.deleteDialog.confirm}
        cancelLabel={S.deleteDialog.cancel}
        onConfirm={handleDelete}
        error={deleteError}
      />
    </Card>
  );
}
