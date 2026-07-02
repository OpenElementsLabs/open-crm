"use client";

import { useCallback, useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import { Badge, Button, Card, CardContent, CardHeader, CardTitle, Input } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import {
  deleteEnrichmentSettings,
  getEnrichmentSettings,
  updateEnrichmentSettings,
} from "@/lib/api";

type Service = "dropcontact" | "cognism";

interface ServiceSettingsCardProps {
  readonly service: Service;
  readonly title: string;
}

/**
 * API-key management for one enrichment service, mirroring the Brevo settings panel: shows a
 * "configured" badge with change/remove actions, or an input to store a new key. The key itself is
 * never returned by the backend — only a boolean status.
 */
function ServiceSettingsCard({ service, title }: ServiceSettingsCardProps) {
  const t = useTranslations();
  const S = t.enrichment.settings;

  const [configured, setConfigured] = useState(false);
  const [loading, setLoading] = useState(true);
  const [apiKey, setApiKey] = useState("");
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchSettings = useCallback(async () => {
    try {
      const settings = await getEnrichmentSettings(service);
      setConfigured(settings.configured);
    } catch {
      setError(S.errorGeneric);
    } finally {
      setLoading(false);
    }
  }, [service, S.errorGeneric]);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  const handleSave = async () => {
    if (!apiKey.trim()) return;
    setSaving(true);
    setError(null);
    try {
      const result = await updateEnrichmentSettings(service, apiKey.trim());
      setConfigured(result.configured);
      setApiKey("");
      setEditing(false);
    } catch (e: unknown) {
      if (e instanceof Error && e.message.toLowerCase().includes("invalid")) {
        setError(S.errorInvalid);
      } else {
        setError(S.errorGeneric);
      }
    } finally {
      setSaving(false);
    }
  };

  const handleRemove = async () => {
    if (!confirm(S.removeConfirm)) return;
    setError(null);
    try {
      await deleteEnrichmentSettings(service);
      setConfigured(false);
    } catch {
      setError(S.errorGeneric);
    }
  };

  return (
    <Card className="border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-lg text-oe-dark">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="flex items-center gap-2 text-oe-gray-mid">
            <Loader2 className="h-4 w-4 animate-spin" />
          </div>
        ) : configured && !editing ? (
          <div className="flex flex-col gap-3">
            <Badge className="bg-oe-green/20 text-oe-green border-oe-green/30 w-fit">{S.configured}</Badge>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => setEditing(true)}>
                {S.change}
              </Button>
              <Button
                variant="outline"
                size="sm"
                className="text-oe-red border-oe-red/30 hover:bg-oe-red/10"
                onClick={handleRemove}
              >
                {S.remove}
              </Button>
            </div>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {!configured && !editing && (
              <p className="text-sm text-oe-gray-mid">{S.notConfigured}</p>
            )}
            <div className="flex gap-2">
              <Input
                type="password"
                placeholder={S.apiKeyPlaceholder}
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                className="max-w-md"
              />
              <Button onClick={handleSave} disabled={saving || !apiKey.trim()}>
                {saving ? S.saving : S.save}
              </Button>
              {editing && (
                <Button
                  variant="outline"
                  onClick={() => {
                    setEditing(false);
                    setApiKey("");
                    setError(null);
                  }}
                >
                  {t.companies.form.cancel}
                </Button>
              )}
            </div>
          </div>
        )}
        {error && <p className="mt-3 text-sm text-oe-red">{error}</p>}
      </CardContent>
    </Card>
  );
}

/**
 * Admin panel bundling the Dropcontact and Cognism API-key settings. Gravatar needs no key and is
 * therefore not listed here.
 */
export function EnrichmentSettings() {
  const t = useTranslations();
  return (
    <div className="flex flex-col gap-6">
      <p className="text-sm text-oe-gray-mid">{t.enrichment.settings.intro}</p>
      <ServiceSettingsCard service="dropcontact" title={t.enrichment.services.dropcontact} />
      <ServiceSettingsCard service="cognism" title={t.enrichment.services.cognism} />
    </div>
  );
}
