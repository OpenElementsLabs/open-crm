"use client";

import { useState } from "react";
import { Sparkles, Loader2 } from "lucide-react";
import {
  Badge,
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Popover,
  PopoverContent,
  PopoverTrigger,
  Separator,
} from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { useEnrichmentConfig } from "@/lib/use-enrichment-config";
import { applyEnrichment, searchEnrichment } from "@/lib/api";
import type {
  ContactDto,
  EnrichmentCandidateDto,
  EnrichmentChangeDto,
  EnrichmentService,
} from "@/lib/types";

type Phase = "loading" | "selection" | "preview" | "info" | "error" | "success";

interface ContactEnrichButtonProps {
  readonly contact: ContactDto;
  readonly onApplied: (updated: ContactDto) => void;
}

/**
 * Admin-only "Anreichern" dropdown plus the shared enrichment dialog. The button offers Gravatar
 * (always, when the contact has an email), Dropcontact and Cognism (only when configured). The
 * dialog walks through loading → optional candidate selection → preview → apply, and is read-only
 * until the admin explicitly presses "Übernehmen".
 */
export function ContactEnrichButton({ contact, onApplied }: ContactEnrichButtonProps) {
  const t = useTranslations();
  const S = t.enrichment;
  const config = useEnrichmentConfig();

  const [menuOpen, setMenuOpen] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [service, setService] = useState<EnrichmentService | null>(null);
  const [phase, setPhase] = useState<Phase>("loading");
  const [candidates, setCandidates] = useState<readonly EnrichmentCandidateDto[]>([]);
  const [selected, setSelected] = useState<EnrichmentCandidateDto | null>(null);
  const [createCompany, setCreateCompany] = useState(false);
  const [applying, setApplying] = useState(false);
  const [gdprNotice, setGdprNotice] = useState<string | null>(null);

  const gravatarAvailable = Boolean(contact.email);
  const services: readonly { id: EnrichmentService; label: string; available: boolean }[] = [
    { id: "gravatar", label: S.services.gravatar, available: gravatarAvailable },
    { id: "dropcontact", label: S.services.dropcontact, available: config.dropcontact === true },
    { id: "cognism", label: S.services.cognism, available: config.cognism === true },
  ];
  const availableServices = services.filter((s) => s.available);

  const startSearch = async (svc: EnrichmentService) => {
    setMenuOpen(false);
    setService(svc);
    setSelected(null);
    setCreateCompany(false);
    setGdprNotice(null);
    setPhase("loading");
    setDialogOpen(true);
    try {
      const result = await searchEnrichment(contact.id, svc);
      if (result.status === "NO_MATCH" || result.candidates.length === 0) {
        setCandidates([]);
        setPhase("info");
        return;
      }
      setCandidates(result.candidates);
      if (result.candidates.length === 1) {
        choose(result.candidates[0]);
      } else {
        setPhase("selection");
      }
    } catch {
      setPhase("error");
    }
  };

  const choose = (candidate: EnrichmentCandidateDto) => {
    setSelected(candidate);
    setCreateCompany(false);
    setPhase(candidate.nothingToEnrich ? "info" : "preview");
  };

  const confirm = async () => {
    if (!service || !selected) return;
    setApplying(true);
    try {
      const result = await applyEnrichment(contact.id, service, selected.payload, createCompany);
      setGdprNotice(result.gdprNotice);
      setPhase("success");
      onApplied(result.contact);
    } catch {
      setPhase("error");
    } finally {
      setApplying(false);
    }
  };

  const fieldLabel = (field: string): string => {
    if (field === "photo") return S.dialog.photo;
    if (field.startsWith("socialLinks.")) {
      const network = field.substring("socialLinks.".length);
      const names: Record<string, string> = {
        GITHUB: t.contacts.form.networkGithub,
        LINKEDIN: t.contacts.form.networkLinkedin,
        X: t.contacts.form.networkX,
        MASTODON: t.contacts.form.networkMastodon,
        BLUESKY: t.contacts.form.networkBluesky,
        DISCORD: t.contacts.form.networkDiscord,
        YOUTUBE: t.contacts.form.networkYoutube,
        WEBSITE: t.contacts.form.networkWebsite,
      };
      return names[network] ?? network;
    }
    switch (field) {
      case "email":
        return t.contacts.detail.email;
      case "position":
        return t.contacts.detail.position;
      case "phoneNumber":
        return t.contacts.detail.phone;
      default:
        return field;
    }
  };

  const renderChange = (change: EnrichmentChangeDto) => {
    if (change.field === "photo" && selected?.payload.photoBase64) {
      const src = `data:${selected.payload.photoContentType ?? "image/jpeg"};base64,${selected.payload.photoBase64}`;
      return (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={src} alt={S.dialog.photo} className="h-16 w-16 rounded-full object-cover" />
      );
    }
    return <span className="text-sm text-oe-black break-all">{change.proposedValue}</span>;
  };

  const closeDialog = () => setDialogOpen(false);

  return (
    <>
      <Popover open={menuOpen} onOpenChange={setMenuOpen}>
        <PopoverTrigger asChild>
          <Button variant="outline">
            <Sparkles className="mr-2 h-4 w-4" />
            {S.button}
          </Button>
        </PopoverTrigger>
        <PopoverContent align="end" className="w-56 p-1">
          {availableServices.length === 0 ? (
            <p className="px-2 py-2 text-sm text-oe-gray-mid">{S.noServices}</p>
          ) : (
            availableServices.map((s) => (
              <Button
                key={s.id}
                variant="ghost"
                className="w-full justify-start"
                onClick={() => startSearch(s.id)}
              >
                {s.label}
              </Button>
            ))
          )}
        </PopoverContent>
      </Popover>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {S.dialog.title.replace("{service}", service ? S.services[service] : "")}
            </DialogTitle>
            <DialogDescription>{S.dialog.subtitle}</DialogDescription>
          </DialogHeader>

          {phase === "loading" && (
            <div className="flex items-center gap-2 py-6 text-oe-gray-mid">
              <Loader2 className="h-4 w-4 animate-spin" />
              {S.dialog.loading}
            </div>
          )}

          {phase === "selection" && (
            <div className="flex flex-col gap-2 py-2">
              <p className="text-sm text-oe-gray-mid">{S.dialog.selectCandidate}</p>
              {candidates.map((c) => (
                <Button
                  key={c.candidateId}
                  variant="outline"
                  className="justify-start"
                  onClick={() => choose(c)}
                >
                  {c.label}
                </Button>
              ))}
            </div>
          )}

          {phase === "preview" && selected && (
            <div className="flex flex-col gap-3 py-2">
              <p className="text-sm text-oe-gray-mid">{S.dialog.previewTitle}</p>
              <dl className="flex flex-col gap-2">
                {selected.changes.map((change) => (
                  <div key={change.field} className="flex items-center justify-between gap-4">
                    <dt className="text-sm font-medium text-oe-gray-mid">{fieldLabel(change.field)}</dt>
                    <dd>{renderChange(change)}</dd>
                  </div>
                ))}
                {selected.companyResolution.kind === "MATCHED" && (
                  <div className="flex items-center justify-between gap-4">
                    <dt className="text-sm font-medium text-oe-gray-mid">{t.contacts.detail.company}</dt>
                    <dd className="text-sm text-oe-black">
                      {S.dialog.companyMatched.replace("{name}", selected.companyResolution.companyName ?? "")}
                    </dd>
                  </div>
                )}
              </dl>
              {selected.companyResolution.kind === "NEW" && (
                <label className="flex items-center gap-2 text-sm text-oe-black">
                  <input
                    type="checkbox"
                    checked={createCompany}
                    onChange={(e) => setCreateCompany(e.target.checked)}
                  />
                  {S.dialog.createCompany.replace("{name}", selected.companyResolution.companyName ?? "")}
                </label>
              )}
            </div>
          )}

          {phase === "info" && (
            <p className="py-6 text-sm text-oe-gray-mid">
              {candidates.length === 0 ? S.dialog.noMatch : S.dialog.nothingToEnrich}
            </p>
          )}

          {phase === "error" && <p className="py-6 text-sm text-oe-red">{S.dialog.error}</p>}

          {phase === "success" && (
            <div className="flex flex-col gap-3 py-2">
              <Badge className="bg-oe-green/20 text-oe-green border-oe-green/30 w-fit">{S.dialog.success}</Badge>
              <Separator />
              <p className="text-sm text-oe-gray-mid">{gdprNotice}</p>
            </div>
          )}

          <DialogFooter>
            {phase === "preview" && selected && (
              <>
                <Button variant="outline" onClick={closeDialog} disabled={applying}>
                  {S.dialog.cancel}
                </Button>
                <Button onClick={confirm} disabled={applying}>
                  {applying ? (
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  ) : null}
                  {S.dialog.apply}
                </Button>
              </>
            )}
            {(phase === "info" || phase === "error" || phase === "success" || phase === "selection") && (
              <Button variant="outline" onClick={closeDialog}>
                {S.dialog.close}
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
