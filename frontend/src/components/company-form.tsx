"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Building2, Trash2, Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { TagMultiSelect } from "@/components/tag-multi-select";
import { createCompany, updateCompany, uploadCompanyLogo, deleteCompanyLogo, getCompanyLogoUrl } from "@/lib/api";
import type { CompanyDto, CompanyCreateDto } from "@/lib/types";
import { useTranslations } from "@/lib/i18n/language-context";

const ALLOWED_LOGO_TYPES = ["image/svg+xml", "image/png", "image/jpeg"];
const MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

interface CompanyFormProps {
  readonly company?: CompanyDto;
}

export function CompanyForm({ company }: CompanyFormProps) {
  const t = useTranslations();
  const S = t.companies.form;
  const router = useRouter();
  const isEdit = !!company;

  const [name, setName] = useState(company?.name ?? "");
  const [email, setEmail] = useState(company?.email ?? "");
  const [website, setWebsite] = useState(company?.website ?? "");
  const [street, setStreet] = useState(company?.street ?? "");
  const [houseNumber, setHouseNumber] = useState(company?.houseNumber ?? "");
  const [zipCode, setZipCode] = useState(company?.zipCode ?? "");
  const [city, setCity] = useState(company?.city ?? "");
  const [country, setCountry] = useState(company?.country ?? "");
  const [phoneNumber, setPhoneNumber] = useState(company?.phoneNumber ?? "");
  const [description, setDescription] = useState(company?.description ?? "");
  const [tagIds, setTagIds] = useState<string[]>([...(company?.tagIds ?? [])]);
  const [tagIdsChanged, setTagIdsChanged] = useState(false);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [filePreview, setFilePreview] = useState<string | null>(null);
  const [hasExistingLogo, setHasExistingLogo] = useState(company?.hasLogo ?? false);
  const [removeExistingLogo, setRemoveExistingLogo] = useState(false);
  const [imageError, setImageError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [nameError, setNameError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setImageError(null);
    const file = e.target.files?.[0];
    if (!file) return;

    if (!ALLOWED_LOGO_TYPES.includes(file.type)) {
      setImageError(S.imageInvalidFormat);
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      setImageError(S.imageTooLarge);
      return;
    }

    setSelectedFile(file);
    setFilePreview(URL.createObjectURL(file));
    setRemoveExistingLogo(false);
  };

  const handleRemoveLogo = () => {
    setSelectedFile(null);
    setFilePreview(null);
    setRemoveExistingLogo(true);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setNameError(null);
    setApiError(null);

    if (!name.trim()) {
      setNameError(S.nameRequired);
      return;
    }

    const data: CompanyCreateDto = {
      name: name.trim(),
      email: email.trim() || null,
      website: website.trim() || null,
      street: street.trim() || null,
      houseNumber: houseNumber.trim() || null,
      zipCode: zipCode.trim() || null,
      city: city.trim() || null,
      country: country.trim() || null,
      phoneNumber: phoneNumber.trim() || null,
      description: description.trim() || null,
      ...(tagIdsChanged ? { tagIds } : {}),
    };

    setSubmitting(true);
    try {
      let result: CompanyDto;
      if (isEdit) {
        result = await updateCompany(company.id, data);
      } else {
        result = await createCompany(data);
      }

      // Handle logo upload/delete after entity save
      if (removeExistingLogo && !selectedFile) {
        await deleteCompanyLogo(result.id);
      }
      if (selectedFile) {
        await uploadCompanyLogo(result.id, selectedFile);
      }

      router.push(`/companies/${result.id}`);
    } catch {
      setApiError(S.errorGeneric);
    } finally {
      setSubmitting(false);
    }
  };

  const cancelHref = isEdit ? `/companies/${company.id}` : "/companies";

  return (
    <Card className="mx-auto max-w-2xl border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-xl text-oe-dark">
          {isEdit ? S.editTitle : S.createTitle}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="name">{S.name} *</Label>
            <Input
              id="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={S.namePlaceholder}
              className={nameError ? "border-oe-red" : ""}
            />
            {nameError && <p className="mt-1 text-sm text-oe-red">{nameError}</p>}
          </div>

          <div>
            <Label htmlFor="email">{S.email}</Label>
            <Input
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder={S.emailPlaceholder}
            />
          </div>

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
            <Label htmlFor="website">{S.website}</Label>
            <Input
              id="website"
              value={website}
              onChange={(e) => setWebsite(e.target.value)}
              placeholder={S.websitePlaceholder}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="street">{S.street}</Label>
              <Input
                id="street"
                value={street}
                onChange={(e) => setStreet(e.target.value)}
                placeholder={S.streetPlaceholder}
              />
            </div>
            <div>
              <Label htmlFor="houseNumber">{S.houseNumber}</Label>
              <Input
                id="houseNumber"
                value={houseNumber}
                onChange={(e) => setHouseNumber(e.target.value)}
                placeholder={S.houseNumberPlaceholder}
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <Label htmlFor="zipCode">{S.zipCode}</Label>
              <Input
                id="zipCode"
                value={zipCode}
                onChange={(e) => setZipCode(e.target.value)}
                placeholder={S.zipCodePlaceholder}
              />
            </div>
            <div>
              <Label htmlFor="city">{S.city}</Label>
              <Input
                id="city"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                placeholder={S.cityPlaceholder}
              />
            </div>
            <div>
              <Label htmlFor="country">{S.country}</Label>
              <Input
                id="country"
                value={country}
                onChange={(e) => setCountry(e.target.value)}
                placeholder={S.countryPlaceholder}
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label>{t.tags.label}</Label>
            <TagMultiSelect
              selectedIds={tagIds}
              onChange={(ids) => { setTagIds(ids); setTagIdsChanged(true); }}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">{S.description}</Label>
            <Textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={S.descriptionPlaceholder}
              rows={3}
            />
          </div>

          {/* Logo upload */}
          <div>
            <Label>{S.logo}</Label>
            <div className="mt-2 flex items-center gap-4">
              {filePreview ? (
                <img src={filePreview} alt="Logo preview" className="h-16 w-16 object-contain" />
              ) : hasExistingLogo && !removeExistingLogo ? (
                <img src={getCompanyLogoUrl(company!.id)} alt="Current logo" className="h-16 w-16 object-contain" />
              ) : (
                <Building2 className="h-16 w-16 text-oe-gray-mid" />
              )}
              <div className="flex flex-col gap-2">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/svg+xml,image/png,image/jpeg"
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
                  {S.uploadLogo}
                </Button>
                {(hasExistingLogo || selectedFile) && !removeExistingLogo && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="text-oe-red border-oe-red"
                    onClick={handleRemoveLogo}
                  >
                    <Trash2 className="mr-2 h-4 w-4" />
                    {S.removeLogo}
                  </Button>
                )}
              </div>
            </div>
            {imageError && <p className="mt-1 text-sm text-oe-red">{imageError}</p>}
          </div>

          {apiError && (
            <p className="text-sm text-oe-red">{apiError}</p>
          )}

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
