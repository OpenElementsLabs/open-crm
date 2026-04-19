"use client";

import { useState } from "react";
import { Send } from "lucide-react";
import { Button, Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, AlertDialog, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@open-elements/ui";

interface AddCommentDialogProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly onSubmit: (text: string) => Promise<void>;
  readonly sending: boolean;
  readonly title: string;
  readonly placeholder: string;
  readonly sendLabel: string;
  readonly sendingLabel: string;
  readonly errorTitle: string;
  readonly errorMessage: string;
}

export function AddCommentDialog({
  open,
  onOpenChange,
  onSubmit,
  sending,
  title,
  placeholder,
  sendLabel,
  sendingLabel,
  errorTitle,
  errorMessage,
}: AddCommentDialogProps) {
  const [text, setText] = useState("");
  const [errorOpen, setErrorOpen] = useState(false);

  const isTextEmpty = !text.trim();

  const handleSubmit = async () => {
    if (isTextEmpty || sending) return;
    try {
      await onSubmit(text.trim());
      setText("");
      onOpenChange(false);
    } catch {
      setErrorOpen(true);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleOpenChange = (value: boolean) => {
    if (!value) {
      setText("");
    }
    onOpenChange(value);
  };

  return (
    <>
      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="font-heading">{title}</DialogTitle>
          </DialogHeader>
          <textarea
            className="w-full rounded-md border border-oe-gray-light p-3 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green resize-y min-h-[80px]"
            placeholder={placeholder}
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={handleKeyDown}
            rows={3}
            disabled={sending}
          />
          <DialogFooter>
            <Button
              onClick={handleSubmit}
              disabled={isTextEmpty || sending}
              className="bg-oe-green hover:bg-oe-green-dark text-white"
            >
              <Send className="mr-2 h-4 w-4" />
              {sending ? sendingLabel : sendLabel}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={errorOpen} onOpenChange={setErrorOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{errorTitle}</AlertDialogTitle>
            <AlertDialogDescription>{errorMessage}</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>OK</AlertDialogCancel>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
