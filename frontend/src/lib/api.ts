import type {
  CompanyDto,
  CompanyCreateDto,
  ContactDto,
  ContactCreateDto,
  CommentDto,
  CommentCreateDto,
  TagDto,
  TagCreateDto,
  BrevoSettingsDto,
  BrevoSyncResultDto,
  Page,
} from "./types";

import { auth } from "@/auth";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

function isServer(): boolean {
  return typeof window === "undefined";
}

function baseUrl(): string {
  return isServer() ? BACKEND_URL : "";
}

async function apiFetch(url: string, init?: RequestInit): Promise<Response> {
  if (isServer()) {
    try {
      const session = await auth();
      if (session?.accessToken) {
        const headers = new Headers(init?.headers);
        headers.set("Authorization", `Bearer ${session.accessToken}`);
        return fetch(url, { ...init, headers });
      }
    } catch {
      // No session available
    }
  }
  return fetch(url, init);
}

export interface CompanyListParams {
  readonly page?: number;
  readonly size?: number;
  readonly name?: string;
  readonly includeDeleted?: boolean;
  readonly brevo?: boolean;
  readonly tagIds?: readonly string[];
}

export async function getCompanies(params: CompanyListParams = {}): Promise<Page<CompanyDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.name) searchParams.set("name", params.name);
  if (params.includeDeleted) searchParams.set("includeDeleted", "true");
  if (params.brevo !== undefined) searchParams.set("brevo", String(params.brevo));
  if (params.tagIds) {
    for (const id of params.tagIds) {
      searchParams.append("tagIds", id);
    }
  }

  const query = searchParams.toString();
  const url = `${baseUrl()}/api/companies${query ? `?${query}` : ""}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch companies: ${response.status}`);
  }

  return response.json();
}

export async function getCompany(id: string): Promise<CompanyDto> {
  const url = `${baseUrl()}/api/companies/${id}`;
  const response = await apiFetch(url, { cache: "no-store" });

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
  const response = await apiFetch(url, {
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
  const response = await apiFetch(url, {
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
  const response = await apiFetch(url, { method: "DELETE" });

  if (!response.ok) {
    if (response.status === 409) {
      throw new Error("CONFLICT");
    }
    throw new Error(`Failed to delete company: ${response.status}`);
  }
}

export async function restoreCompany(id: string): Promise<CompanyDto> {
  const url = `${baseUrl()}/api/companies/${id}/restore`;
  const response = await apiFetch(url, { method: "POST" });

  if (!response.ok) {
    throw new Error(`Failed to restore company: ${response.status}`);
  }

  return response.json();
}

// Company Logo API

export async function uploadCompanyLogo(id: string, file: File): Promise<void> {
  const formData = new FormData();
  formData.append("file", file);
  const url = `${baseUrl()}/api/companies/${id}/logo`;
  const response = await apiFetch(url, { method: "POST", body: formData });

  if (!response.ok) {
    throw new Error(`Failed to upload logo: ${response.status}`);
  }
}

export function getCompanyLogoUrl(id: string): string {
  return `${baseUrl()}/api/companies/${id}/logo`;
}

export async function deleteCompanyLogo(id: string): Promise<void> {
  const url = `${baseUrl()}/api/companies/${id}/logo`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete logo: ${response.status}`);
  }
}

// Contact API

export interface ContactListParams {
  readonly page?: number;
  readonly size?: number;
  readonly search?: string;
  readonly companyId?: string;
  readonly noCompany?: boolean;
  readonly language?: string;
  readonly brevo?: boolean;
  readonly tagIds?: readonly string[];
}

export async function getContacts(params: ContactListParams = {}): Promise<Page<ContactDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.search) searchParams.set("search", params.search);
  if (params.companyId) searchParams.set("companyId", params.companyId);
  if (params.noCompany) searchParams.set("noCompany", "true");
  if (params.language) searchParams.set("language", params.language);
  if (params.brevo !== undefined) searchParams.set("brevo", String(params.brevo));
  if (params.tagIds) {
    for (const id of params.tagIds) {
      searchParams.append("tagIds", id);
    }
  }

  const query = searchParams.toString();
  const url = `${baseUrl()}/api/contacts${query ? `?${query}` : ""}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch contacts: ${response.status}`);
  }

  return response.json();
}

export async function getContact(id: string): Promise<ContactDto> {
  const url = `${baseUrl()}/api/contacts/${id}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error("Contact not found");
    }
    throw new Error(`Failed to fetch contact: ${response.status}`);
  }

  return response.json();
}

export async function createContact(data: ContactCreateDto): Promise<ContactDto> {
  const url = `${baseUrl()}/api/contacts`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create contact: ${response.status}`);
  }

  return response.json();
}

export async function updateContact(id: string, data: ContactCreateDto): Promise<ContactDto> {
  const url = `${baseUrl()}/api/contacts/${id}`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to update contact: ${response.status}`);
  }

  return response.json();
}

export async function deleteContact(id: string): Promise<void> {
  const url = `${baseUrl()}/api/contacts/${id}`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete contact: ${response.status}`);
  }
}

// Contact Photo API

export async function uploadContactPhoto(id: string, file: File): Promise<void> {
  const formData = new FormData();
  formData.append("file", file);
  const url = `${baseUrl()}/api/contacts/${id}/photo`;
  const response = await apiFetch(url, { method: "POST", body: formData });

  if (!response.ok) {
    throw new Error(`Failed to upload photo: ${response.status}`);
  }
}

export function getContactPhotoUrl(id: string): string {
  return `${baseUrl()}/api/contacts/${id}/photo`;
}

export async function deleteContactPhoto(id: string): Promise<void> {
  const url = `${baseUrl()}/api/contacts/${id}/photo`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete photo: ${response.status}`);
  }
}

export function getCompanyExportUrl(params: CompanyListParams, columns: string[]): string {
  const searchParams = new URLSearchParams();
  if (params.name) searchParams.set("name", params.name);
  if (params.includeDeleted) searchParams.set("includeDeleted", "true");
  if (params.brevo !== undefined) searchParams.set("brevo", String(params.brevo));
  for (const col of columns) {
    searchParams.append("columns", col);
  }
  return `${baseUrl()}/api/companies/export?${searchParams.toString()}`;
}

export function getContactExportUrl(params: ContactListParams, columns: string[]): string {
  const searchParams = new URLSearchParams();
  if (params.search) searchParams.set("search", params.search);
  if (params.companyId) searchParams.set("companyId", params.companyId);
  if (params.noCompany) searchParams.set("noCompany", "true");
  if (params.language) searchParams.set("language", params.language);
  if (params.brevo !== undefined) searchParams.set("brevo", String(params.brevo));
  for (const col of columns) {
    searchParams.append("columns", col);
  }
  return `${baseUrl()}/api/contacts/export?${searchParams.toString()}`;
}

export async function getCompaniesForSelect(): Promise<CompanyDto[]> {
  const data = await getCompanies({ includeDeleted: false, size: 1000 });
  return data.content as CompanyDto[];
}

// Company Comment API

export async function getCompanyComments(
  companyId: string,
  page: number = 0,
): Promise<Page<CommentDto>> {
  const url = `${baseUrl()}/api/companies/${companyId}/comments?page=${page}&size=20&sort=createdAt,desc`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch comments: ${response.status}`);
  }

  return response.json();
}

export async function createCompanyComment(
  companyId: string,
  data: CommentCreateDto,
): Promise<CommentDto> {
  const url = `${baseUrl()}/api/companies/${companyId}/comments`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create comment: ${response.status}`);
  }

  return response.json();
}

// Contact Comment API

export async function getContactComments(
  contactId: string,
  page: number = 0,
): Promise<Page<CommentDto>> {
  const url = `${baseUrl()}/api/contacts/${contactId}/comments?page=${page}&size=20&sort=createdAt,desc`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch contact comments: ${response.status}`);
  }

  return response.json();
}

export async function createContactComment(
  contactId: string,
  data: CommentCreateDto,
): Promise<CommentDto> {
  const url = `${baseUrl()}/api/contacts/${contactId}/comments`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create contact comment: ${response.status}`);
  }

  return response.json();
}

// Brevo Sync API

export async function getBrevoSettings(): Promise<BrevoSettingsDto> {
  const url = `${baseUrl()}/api/brevo/settings`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch Brevo settings: ${response.status}`);
  }

  return response.json();
}

export async function updateBrevoSettings(apiKey: string): Promise<BrevoSettingsDto> {
  const url = `${baseUrl()}/api/brevo/settings`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ apiKey }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to update Brevo settings: ${response.status}`);
  }

  return response.json();
}

export async function deleteBrevoSettings(): Promise<void> {
  const url = `${baseUrl()}/api/brevo/settings`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete Brevo settings: ${response.status}`);
  }
}

export async function startBrevoSync(): Promise<BrevoSyncResultDto> {
  const url = `${baseUrl()}/api/brevo/sync`;
  const response = await apiFetch(url, { method: "POST" });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to start Brevo sync: ${response.status}`);
  }

  return response.json();
}

// Tags

export interface TagListParams {
  readonly page?: number;
  readonly size?: number;
  readonly name?: string;
  readonly includeCounts?: boolean;
}

export async function getTags(params: TagListParams = {}): Promise<Page<TagDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.name) searchParams.set("name", params.name);
  if (params.includeCounts) searchParams.set("includeCounts", "true");
  searchParams.set("sort", "name,asc");

  const url = `${baseUrl()}/api/tags?${searchParams.toString()}`;
  const response = await apiFetch(url);

  if (!response.ok) {
    throw new Error(`Failed to fetch tags: ${response.status}`);
  }

  return response.json();
}

export async function getTag(id: string): Promise<TagDto> {
  const url = `${baseUrl()}/api/tags/${id}`;
  const response = await apiFetch(url);

  if (!response.ok) {
    throw new Error(`Failed to fetch tag: ${response.status}`);
  }

  return response.json();
}

export async function createTag(data: TagCreateDto): Promise<TagDto> {
  const url = `${baseUrl()}/api/tags`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (response.status === 409) {
    throw new Error("CONFLICT");
  }

  if (!response.ok) {
    throw new Error(`Failed to create tag: ${response.status}`);
  }

  return response.json();
}

export async function updateTag(id: string, data: TagCreateDto): Promise<TagDto> {
  const url = `${baseUrl()}/api/tags/${id}`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (response.status === 409) {
    throw new Error("CONFLICT");
  }

  if (!response.ok) {
    throw new Error(`Failed to update tag: ${response.status}`);
  }

  return response.json();
}

export async function deleteTag(id: string): Promise<void> {
  const url = `${baseUrl()}/api/tags/${id}`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete tag: ${response.status}`);
  }
}
