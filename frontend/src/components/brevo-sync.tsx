"use client";

import { useCallback, useEffect, useState } from "react";
import { Loader2, ChevronDown, ChevronUp } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useTranslations } from "@/lib/i18n/language-context";
import {
  getBrevoSettings,
  updateBrevoSettings,
  deleteBrevoSettings,
  startBrevoSync,
} from "@/lib/api";
import type { BrevoSyncResultDto } from "@/lib/types";

export function BrevoSync() {
  const t = useTranslations();
  const S = t.brevo;

  const [apiKeyConfigured, setApiKeyConfigured] = useState(false);
  const [loading, setLoading] = useState(true);
  const [apiKey, setApiKey] = useState("");
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [settingsError, setSettingsError] = useState<string | null>(null);

  const [syncing, setSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState<BrevoSyncResultDto | null>(null);
  const [syncError, setSyncError] = useState<string | null>(null);
  const [errorsExpanded, setErrorsExpanded] = useState(false);

  const fetchSettings = useCallback(async () => {
    try {
      const settings = await getBrevoSettings();
      setApiKeyConfigured(settings.apiKeyConfigured);
    } catch {
      setSettingsError(S.settings.errorGeneric);
    } finally {
      setLoading(false);
    }
  }, [S.settings.errorGeneric]);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  const handleSave = async () => {
    if (!apiKey.trim()) return;
    setSaving(true);
    setSettingsError(null);
    try {
      const result = await updateBrevoSettings(apiKey.trim());
      setApiKeyConfigured(result.apiKeyConfigured);
      setApiKey("");
      setEditing(false);
    } catch (error: unknown) {
      if (error instanceof Error && error.message.toLowerCase().includes("invalid")) {
        setSettingsError(S.settings.errorInvalid);
      } else {
        setSettingsError(S.settings.errorGeneric);
      }
    } finally {
      setSaving(false);
    }
  };

  const handleRemove = async () => {
    if (!confirm(S.settings.removeConfirm)) return;
    setSettingsError(null);
    try {
      await deleteBrevoSettings();
      setApiKeyConfigured(false);
      setSyncResult(null);
    } catch {
      setSettingsError(S.settings.errorGeneric);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    setSyncError(null);
    setSyncResult(null);
    try {
      const result = await startBrevoSync();
      setSyncResult(result);
    } catch (error: unknown) {
      if (error instanceof Error && error.message.toLowerCase().includes("conflict")) {
        setSyncError(S.sync.errorConflict);
      } else {
        setSyncError(S.sync.errorGeneric);
      }
    } finally {
      setSyncing(false);
    }
  };

  return (
    <div>
      <h1 className="mb-6 font-heading text-2xl font-bold text-oe-dark">{S.title}</h1>

      <div className="flex flex-col gap-6 max-w-2xl">
        {/* Settings Card */}
        <Card>
          <CardHeader>
            <CardTitle className="text-oe-dark">{S.settings.title}</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="flex items-center gap-2 text-oe-gray-mid">
                <Loader2 className="h-4 w-4 animate-spin" />
              </div>
            ) : apiKeyConfigured && !editing ? (
              <div className="flex flex-col gap-3">
                <div>
                  <Badge className="bg-oe-green/20 text-oe-green border-oe-green/30">
                    {S.settings.configured}
                  </Badge>
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setEditing(true);
                      setSettingsError(null);
                    }}
                  >
                    {S.settings.change}
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="text-oe-red border-oe-red/30 hover:bg-oe-red/10"
                    onClick={handleRemove}
                  >
                    {S.settings.remove}
                  </Button>
                </div>
              </div>
            ) : (
              <div className="flex flex-col gap-3">
                {!apiKeyConfigured && !editing && (
                  <p className="text-sm text-oe-gray-mid">{S.settings.notConfigured}</p>
                )}
                <div className="flex gap-2">
                  <Input
                    type="password"
                    placeholder={S.settings.apiKeyPlaceholder}
                    value={apiKey}
                    onChange={(e) => setApiKey(e.target.value)}
                    className="max-w-md"
                  />
                  <Button
                    className="bg-oe-green hover:bg-oe-green-dark text-white"
                    onClick={handleSave}
                    disabled={saving || !apiKey.trim()}
                  >
                    {saving ? S.settings.saving : S.settings.save}
                  </Button>
                  {editing && (
                    <Button
                      variant="outline"
                      onClick={() => {
                        setEditing(false);
                        setApiKey("");
                        setSettingsError(null);
                      }}
                    >
                      {t.companies.form.cancel}
                    </Button>
                  )}
                </div>
              </div>
            )}
            {settingsError && (
              <p className="mt-3 text-sm text-oe-red">{settingsError}</p>
            )}
          </CardContent>
        </Card>

        {/* Sync Card */}
        <Card>
          <CardHeader>
            <CardTitle className="text-oe-dark">{S.sync.title}</CardTitle>
          </CardHeader>
          <CardContent>
            {!apiKeyConfigured ? (
              <p className="text-sm text-oe-gray-mid">{S.sync.configureFirst}</p>
            ) : (
              <div className="flex flex-col gap-4">
                <div className="flex items-center gap-3">
                  <Button
                    className="bg-oe-green hover:bg-oe-green-dark text-white"
                    onClick={handleSync}
                    disabled={syncing}
                  >
                    {S.sync.start}
                  </Button>
                  {syncing && (
                    <span className="flex items-center gap-2 text-sm text-oe-gray-mid">
                      <Loader2 className="h-4 w-4 animate-spin" />
                      {S.sync.running}
                    </span>
                  )}
                </div>

                {syncError && (
                  <p className="text-sm text-oe-red">{syncError}</p>
                )}

                {syncResult && (
                  <div className="flex flex-col gap-4">
                    <div className="grid grid-cols-3 gap-3">
                      <ResultCell label={S.sync.companiesImported} value={syncResult.companiesImported} />
                      <ResultCell label={S.sync.companiesUpdated} value={syncResult.companiesUpdated} />
                      <ResultCell label={S.sync.companiesFailed} value={syncResult.companiesFailed} error />
                      <ResultCell label={S.sync.contactsImported} value={syncResult.contactsImported} />
                      <ResultCell label={S.sync.contactsUpdated} value={syncResult.contactsUpdated} />
                      <ResultCell label={S.sync.contactsFailed} value={syncResult.contactsFailed} error />
                    </div>

                    {syncResult.errors.length > 0 ? (
                      <div>
                        <button
                          type="button"
                          className="flex items-center gap-1 text-sm font-medium text-oe-red hover:underline"
                          onClick={() => setErrorsExpanded(!errorsExpanded)}
                        >
                          {S.sync.errors} ({syncResult.errors.length})
                          {errorsExpanded ? (
                            <ChevronUp className="h-4 w-4" />
                          ) : (
                            <ChevronDown className="h-4 w-4" />
                          )}
                        </button>
                        {errorsExpanded && (
                          <ul className="mt-2 space-y-1 text-sm text-oe-red">
                            {syncResult.errors.map((err, i) => (
                              <li key={i} className="rounded bg-oe-red/5 px-3 py-1.5">
                                {err}
                              </li>
                            ))}
                          </ul>
                        )}
                      </div>
                    ) : (
                      <p className="text-sm text-oe-green">{S.sync.noErrors}</p>
                    )}
                  </div>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function ResultCell({
  label,
  value,
  error = false,
}: {
  readonly label: string;
  readonly value: number;
  readonly error?: boolean;
}) {
  return (
    <div className="rounded-md border border-oe-gray-light p-3 text-center">
      <p className={`text-2xl font-bold ${error && value > 0 ? "text-oe-red" : "text-oe-dark"}`}>
        {value}
      </p>
      <p className="text-xs text-oe-gray-mid">{label}</p>
    </div>
  );
}
