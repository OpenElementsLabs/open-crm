"use client";

import { useState, useEffect } from "react";
import { Download } from "lucide-react";
import { Button, Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { PrimaryButton } from "@/components/primary-button";

interface Column {
  readonly key: string;
  readonly label: string;
}

interface CsvExportDialogProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly columns: readonly Column[];
  readonly onDownload: (selectedColumns: string[]) => void;
}

export function CsvExportDialog({
  open,
  onOpenChange,
  columns,
  onDownload,
}: CsvExportDialogProps) {
  const t = useTranslations();
  const E = t.csvExport;
  const [selected, setSelected] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (open) {
      setSelected(new Set(columns.map((c) => c.key)));
    }
  }, [open, columns]);

  const toggleColumn = (key: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  const selectAll = () => setSelected(new Set(columns.map((c) => c.key)));
  const deselectAll = () => setSelected(new Set());

  const handleDownload = () => {
    const ordered = columns.filter((c) => selected.has(c.key)).map((c) => c.key);
    onDownload(ordered);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="font-heading">{E.title}</DialogTitle>
        </DialogHeader>
        <div className="flex gap-2 mb-3">
          <Button variant="outline" size="sm" onClick={selectAll}>
            {E.selectAll}
          </Button>
          <Button variant="outline" size="sm" onClick={deselectAll}>
            {E.deselectAll}
          </Button>
        </div>
        <div className="grid grid-cols-2 gap-2 max-h-[300px] overflow-y-auto">
          {columns.map((col) => (
            <label
              key={col.key}
              className="flex items-center gap-2 cursor-pointer text-sm text-oe-black hover:text-oe-green"
            >
              <input
                type="checkbox"
                checked={selected.has(col.key)}
                onChange={() => toggleColumn(col.key)}
                className="h-4 w-4 rounded border-oe-gray-light text-oe-green focus:ring-oe-green accent-oe-green"
              />
              {col.label}
            </label>
          ))}
        </div>
        <DialogFooter className="mt-4">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {E.cancel}
          </Button>
          <PrimaryButton
            onClick={handleDownload}
            disabled={selected.size === 0}
          >
            <Download className="mr-2 h-4 w-4" />
            {E.download}
          </PrimaryButton>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
