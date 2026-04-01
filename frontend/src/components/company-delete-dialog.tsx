"use client";

import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";

interface CompanyDeleteDialogProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly companyName: string;
  readonly onDeleteAll: () => void;
  readonly onDeleteCompanyOnly: () => void;
  readonly title: string;
  readonly descriptionAll: string;
  readonly descriptionOnly: string;
  readonly deleteAllLabel: string;
  readonly deleteOnlyLabel: string;
  readonly cancelLabel: string;
}

export function CompanyDeleteDialog({
  open,
  onOpenChange,
  companyName,
  onDeleteAll,
  onDeleteCompanyOnly,
  title,
  descriptionAll,
  descriptionOnly,
  deleteAllLabel,
  deleteOnlyLabel,
  cancelLabel,
}: CompanyDeleteDialogProps) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription className="space-y-2">
            <span className="block">{descriptionAll.replace("{name}", companyName)}</span>
            <span className="block">{descriptionOnly.replace("{name}", companyName)}</span>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>{cancelLabel}</AlertDialogCancel>
          <Button
            onClick={onDeleteCompanyOnly}
            variant="outline"
            className="text-oe-red border-oe-red hover:bg-oe-red-lighter"
          >
            {deleteOnlyLabel}
          </Button>
          <Button
            onClick={onDeleteAll}
            className="bg-oe-red hover:bg-oe-red-dark text-white"
          >
            {deleteAllLabel}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
