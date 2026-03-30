"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { User, Trash2, Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { TagMultiSelect } from "@/components/tag-multi-select";
import { createContact, updateContact, getCompaniesForSelect, uploadContactPhoto, deleteContactPhoto, getContactPhotoUrl } from "@/lib/api";
import type { ContactDto, ContactCreateDto, CompanyDto } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

const MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

interface ContactFormProps {
  readonly contact?: ContactDto;
}

export function ContactForm({ contact }: ContactFormProps) {
  const t = useTranslations();
  const S = t.contacts.form;
  const router = useRouter();
  const isEdit = !!contact;
  const isBrevoEdit = isEdit && (contact?.brevo === true);

  const [firstName, setFirstName] = useState(contact?.firstName ?? "");
  const [lastName, setLastName] = useState(contact?.lastName ?? "");
  const [email, setEmail] = useState(contact?.email ?? "");
  const [position, setPosition] = useState(contact?.position ?? "");
  const [gender, setGender] = useState(contact?.gender ?? "");
  const [linkedInUrl, setLinkedInUrl] = useState(contact?.linkedInUrl ?? "");
  const [phoneNumber, setPhoneNumber] = useState(contact?.phoneNumber ?? "");
  const [companyId, setCompanyId] = useState(contact?.companyId ?? "");
  const [language, setLanguage] = useState(contact?.language ?? "");
  const [birthday, setBirthday] = useState(contact?.birthday ?? "");
  const [tagIds, setTagIds] = useState<string[]>([...(contact?.tagIds ?? [])]);
  const [tagIdsChanged, setTagIdsChanged] = useState(false);

  const [companies, setCompanies] = useState<CompanyDto[]>([]);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [filePreview, setFilePreview] = useState<string | null>(null);
  const [hasExistingPhoto, setHasExistingPhoto] = useState(contact?.hasPhoto ?? false);
  const [removeExistingPhoto, setRemoveExistingPhoto] = useState(false);
  const [imageError, setImageError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setImageError(null);
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.type !== "image/jpeg") {
      setImageError(S.imageInvalidFormat);
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      setImageError(S.imageTooLarge);
      return;
    }

    setSelectedFile(file);
    setFilePreview(URL.createObjectURL(file));
    setRemoveExistingPhoto(false);
  };

  const handleRemovePhoto = () => {
    setSelectedFile(null);
    setFilePreview(null);
    setRemoveExistingPhoto(true);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const [firstNameError, setFirstNameError] = useState<string | null>(null);
  const [lastNameError, setLastNameError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getCompaniesForSelect()
      .then(setCompanies)
      .catch(() => {});
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFirstNameError(null);
    setLastNameError(null);
    setApiError(null);

    let hasError = false;

    if (!firstName.trim()) {
      setFirstNameError(S.firstNameRequired);
      hasError = true;
    }
    if (!lastName.trim()) {
      setLastNameError(S.lastNameRequired);
      hasError = true;
    }

    if (hasError) return;

    const data: ContactCreateDto = {
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      email: email.trim() || null,
      position: position.trim() || null,
      gender: gender && gender !== "none" ? (gender as "MALE" | "FEMALE" | "DIVERSE") : null,
      linkedInUrl: linkedInUrl.trim() || null,
      phoneNumber: phoneNumber.trim() || null,
      companyId: companyId && companyId !== "none" ? companyId : null,
      language: language && language !== "unknown" ? (language as "DE" | "EN") : null,
      birthday: birthday || null,
      ...(tagIdsChanged ? { tagIds } : {}),
    };

    setSubmitting(true);
    try {
      let result: ContactDto;
      if (isEdit) {
        result = await updateContact(contact.id, data);
      } else {
        result = await createContact(data);
      }

      // Handle photo upload/delete after entity save
      if (removeExistingPhoto && !selectedFile) {
        await deleteContactPhoto(result.id);
      }
      if (selectedFile) {
        await uploadContactPhoto(result.id, selectedFile);
      }

      router.push(`/contacts/${result.id}`);
    } catch {
      setApiError(S.errorGeneric);
    } finally {
      setSubmitting(false);
    }
  };

  const cancelHref = isEdit ? `/contacts/${contact.id}` : "/contacts";

  return (
    <Card className="mx-auto max-w-2xl border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-xl text-oe-dark">
          {isEdit ? S.editTitle : S.createTitle}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="firstName">{S.firstName} *</Label>
              <Input
                id="firstName"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                placeholder={S.firstNamePlaceholder}
                className={firstNameError ? "border-oe-red" : ""}
                disabled={isBrevoEdit}
              />
              {isBrevoEdit && <p className="mt-1 text-xs text-oe-gray-mid">{S.managedByBrevo}</p>}
              {firstNameError && <p className="mt-1 text-sm text-oe-red">{firstNameError}</p>}
            </div>
            <div>
              <Label htmlFor="lastName">{S.lastName} *</Label>
              <Input
                id="lastName"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                placeholder={S.lastNamePlaceholder}
                className={lastNameError ? "border-oe-red" : ""}
                disabled={isBrevoEdit}
              />
              {isBrevoEdit && <p className="mt-1 text-xs text-oe-gray-mid">{S.managedByBrevo}</p>}
              {lastNameError && <p className="mt-1 text-sm text-oe-red">{lastNameError}</p>}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="email">{S.email}</Label>
              <Input
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder={S.emailPlaceholder}
                disabled={isBrevoEdit}
              />
              {isBrevoEdit && <p className="mt-1 text-xs text-oe-gray-mid">{S.managedByBrevo}</p>}
            </div>
            <div>
              <Label htmlFor="position">{S.position}</Label>
              <Input
                id="position"
                value={position}
                onChange={(e) => setPosition(e.target.value)}
                placeholder={S.positionPlaceholder}
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="gender">{S.gender}</Label>
              <Select value={gender || "none"} onValueChange={setGender}>
                <SelectTrigger id="gender">
                  <SelectValue placeholder={S.notSpecified} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">{S.notSpecified}</SelectItem>
                  <SelectItem value="MALE">{S.male}</SelectItem>
                  <SelectItem value="FEMALE">{S.female}</SelectItem>
                  <SelectItem value="DIVERSE">{S.diverse}</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label htmlFor="language">{S.language}</Label>
              <Select value={language || "unknown"} onValueChange={setLanguage} disabled={isBrevoEdit}>
                <SelectTrigger id="language">
                  <SelectValue placeholder={S.language} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="unknown">{S.languageUnknown}</SelectItem>
                  <SelectItem value="DE">DE</SelectItem>
                  <SelectItem value="EN">EN</SelectItem>
                </SelectContent>
              </Select>
              {isBrevoEdit && <p className="mt-1 text-xs text-oe-gray-mid">{S.managedByBrevo}</p>}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="phone">{S.phone}</Label>
              <Input
                id="phone"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder={S.phonePlaceholder}
              />
            </div>
            <div>
              <Label htmlFor="linkedIn">{S.linkedIn}</Label>
              <Input
                id="linkedIn"
                value={linkedInUrl}
                onChange={(e) => setLinkedInUrl(e.target.value)}
                placeholder={S.linkedInPlaceholder}
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="birthday">{S.birthday}</Label>
              <Input
                id="birthday"
                type="date"
                value={birthday}
                onChange={(e) => setBirthday(e.target.value)}
              />
            </div>
          </div>

          <div>
            <Label htmlFor="company">{S.company}</Label>
            <Select value={companyId || "none"} onValueChange={setCompanyId}>
              <SelectTrigger id="company">
                <SelectValue placeholder={S.noCompany} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="none">{S.noCompany}</SelectItem>
                {companies.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>{t.tags.label}</Label>
            <TagMultiSelect
              selectedIds={tagIds}
              onChange={(ids) => { setTagIds(ids); setTagIdsChanged(true); }}
            />
          </div>

          {/* Photo upload */}
          <div>
            <Label>{S.photo}</Label>
            <div className="mt-2 flex items-center gap-4">
              {filePreview ? (
                <img src={filePreview} alt="Photo preview" className="h-16 w-16 rounded-full object-cover" />
              ) : hasExistingPhoto && !removeExistingPhoto ? (
                <img src={getContactPhotoUrl(contact!.id)} alt="Current photo" className="h-16 w-16 rounded-full object-cover" />
              ) : (
                <User className="h-16 w-16 text-oe-gray-mid" />
              )}
              <div className="flex flex-col gap-2">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg"
                  onChange={handleFileChange}
                  className="hidden"
                />
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => fileInputRef.current?.click()}
                >
                  <Upload className="mr-2 h-4 w-4" />
                  {S.uploadPhoto}
                </Button>
                {(hasExistingPhoto || selectedFile) && !removeExistingPhoto && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="text-oe-red border-oe-red"
                    onClick={handleRemovePhoto}
                  >
                    <Trash2 className="mr-2 h-4 w-4" />
                    {S.removePhoto}
                  </Button>
                )}
              </div>
            </div>
            {imageError && <p className="mt-1 text-sm text-oe-red">{imageError}</p>}
          </div>

          {apiError && <p className="text-sm text-oe-red">{apiError}</p>}

          <div className="flex gap-3 pt-4">
            <Button
              type="submit"
              disabled={submitting}
              className="bg-oe-green hover:bg-oe-green-dark text-white"
            >
              {S.save}
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push(cancelHref)}
            >
              {S.cancel}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
