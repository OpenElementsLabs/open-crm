"use client";

import { useCallback, useEffect, useState } from "react";
import { Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { getCompanyComments, createCompanyComment } from "@/lib/api";
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

interface CompanyCommentsProps {
  readonly companyId: string;
}

export function CompanyComments({ companyId }: CompanyCommentsProps) {
  const t = useTranslations();
  const S = t.companies.comments;

  const [comments, setComments] = useState<CommentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  const [text, setText] = useState("");
  const [sending, setSending] = useState(false);
  const [errorOpen, setErrorOpen] = useState(false);

  const fetchComments = useCallback(async (pageNum: number, append: boolean) => {
    if (append) {
      setLoadingMore(true);
    } else {
      setLoading(true);
    }
    try {
      const result = await getCompanyComments(companyId, pageNum);
      if (append) {
        setComments((prev) => [...prev, ...result.content]);
      } else {
        setComments([...result.content]);
      }
      setHasMore(!result.last);
      setPage(pageNum);
    } catch {
      if (!append) {
        setComments([]);
      }
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, [companyId]);

  useEffect(() => {
    fetchComments(0, false);
  }, [fetchComments]);

  const handleSend = async () => {
    if (!text.trim()) return;
    setSending(true);
    try {
      const newComment = await createCompanyComment(companyId, { text: text.trim() });
      setComments((prev) => [newComment, ...prev]);
      setText("");
    } catch {
      setErrorOpen(true);
    } finally {
      setSending(false);
    }
  };

  const handleLoadMore = () => {
    fetchComments(page + 1, true);
  };

  const isTextEmpty = !text.trim();

  return (
    <Card className="border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-lg text-oe-dark">{S.title}</CardTitle>
      </CardHeader>
      <CardContent>
        {/* Input area */}
        <div className="mb-6">
          <textarea
            className="w-full rounded-md border border-oe-gray-light p-3 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green resize-y min-h-[80px]"
            placeholder={S.placeholder}
            value={text}
            onChange={(e) => setText(e.target.value)}
            rows={3}
          />
          <div className="mt-2 flex justify-end">
            <Button
              onClick={handleSend}
              disabled={isTextEmpty || sending}
              className="bg-oe-green hover:bg-oe-green-dark text-white"
            >
              <Send className="mr-2 h-4 w-4" />
              {sending ? S.sending : S.send}
            </Button>
          </div>
        </div>

        {/* Loading skeleton */}
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

        {/* Empty state */}
        {!loading && comments.length === 0 && (
          <p className="text-sm text-oe-gray-mid">{S.empty}</p>
        )}

        {/* Comment list */}
        {!loading && comments.length > 0 && (
          <div className="space-y-4">
            {comments.map((comment) => (
              <div key={comment.id} className="border-b border-oe-gray-light pb-4 last:border-b-0">
                <p className="text-xs text-oe-gray-mid">
                  {comment.author} &middot; {formatDate(comment.createdAt, "de")}
                </p>
                <p className="mt-1 text-sm text-oe-black whitespace-pre-wrap break-words">
                  {comment.text}
                </p>
              </div>
            ))}
          </div>
        )}

        {/* Load more */}
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

      {/* Error dialog */}
      <AlertDialog open={errorOpen} onOpenChange={setErrorOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{S.errorTitle}</AlertDialogTitle>
            <AlertDialogDescription>{S.errorGeneric}</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>OK</AlertDialogCancel>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  );
}
