import type { CompanyDto, CompanyCreateDto, Page } from "./types";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

function isServer(): boolean {
  return typeof window === "undefined";
}

function baseUrl(): string {
  return isServer() ? BACKEND_URL : "";
}

export interface CompanyListParams {
  readonly page?: number;
  readonly size?: number;
  readonly sort?: string;
  readonly name?: string;
  readonly city?: string;
  readonly country?: string;
  readonly includeDeleted?: boolean;
}

export async function getCompanies(params: CompanyListParams = {}): Promise<Page<CompanyDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.sort) searchParams.set("sort", params.sort);
  if (params.name) searchParams.set("name", params.name);
  if (params.city) searchParams.set("city", params.city);
  if (params.country) searchParams.set("country", params.country);
  if (params.includeDeleted) searchParams.set("includeDeleted", "true");

  const query = searchParams.toString();
  const url = `${baseUrl()}/api/companies${query ? `?${query}` : ""}`;
  const response = await fetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch companies: ${response.status}`);
  }

  return response.json();
}

export async function getCompany(id: string): Promise<CompanyDto> {
  const url = `${baseUrl()}/api/companies/${id}`;
  const response = await fetch(url, { cache: "no-store" });

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error("Company not found");
    }
    throw new Error(`Failed to fetch company: ${response.status}`);
  }

  return response.json();
}

export async function createCompany(data: CompanyCreateDto): Promise<CompanyDto> {
  const url = `${baseUrl()}/api/companies`;
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create company: ${response.status}`);
  }

  return response.json();
}

export async function updateCompany(id: string, data: CompanyCreateDto): Promise<CompanyDto> {
  const url = `${baseUrl()}/api/companies/${id}`;
  const response = await fetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to update company: ${response.status}`);
  }

  return response.json();
}

export async function deleteCompany(id: string): Promise<void> {
  const url = `${baseUrl()}/api/companies/${id}`;
  const response = await fetch(url, { method: "DELETE" });

  if (!response.ok) {
    if (response.status === 409) {
      throw new Error("CONFLICT");
    }
    throw new Error(`Failed to delete company: ${response.status}`);
  }
}

export async function restoreCompany(id: string): Promise<CompanyDto> {
  const url = `${baseUrl()}/api/companies/${id}/restore`;
  const response = await fetch(url, { method: "POST" });

  if (!response.ok) {
    throw new Error(`Failed to restore company: ${response.status}`);
  }

  return response.json();
}
