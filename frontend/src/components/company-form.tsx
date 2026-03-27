"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { createCompany, updateCompany } from "@/lib/api";
import type { CompanyDto, CompanyCreateDto } from "@/lib/types";
import { STRINGS } from "@/lib/constants";

const S = STRINGS.companies.form;

interface CompanyFormProps {
  readonly company?: CompanyDto;
}

export function CompanyForm({ company }: CompanyFormProps) {
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

  const [nameError, setNameError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

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
    };

    setSubmitting(true);
    try {
      let result: CompanyDto;
      if (isEdit) {
        result = await updateCompany(company.id, data);
      } else {
        result = await createCompany(data);
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
