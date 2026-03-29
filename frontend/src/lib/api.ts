import type {
  CompanyDto,
  CompanyCreateDto,
  ContactDto,
  ContactCreateDto,
  CommentDto,
  CommentCreateDto,
  BrevoSettingsDto,
  BrevoSyncResultDto,
  Page,
} from "./types";

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
  readonly name?: string;
  readonly includeDeleted?: boolean;
  readonly brevo?: boolean;
}

export async function getCompanies(params: CompanyListParams = {}): Promise<Page<CompanyDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.name) searchParams.set("name", params.name);
  if (params.includeDeleted) searchParams.set("includeDeleted", "true");
  if (params.brevo !== undefined) searchParams.set("brevo", String(params.brevo));

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

// Company Logo API

export async function uploadCompanyLogo(id: string, file: File): Promise<void> {
  const formData = new FormData();
  formData.append("file", file);
  const url = `${baseUrl()}/api/companies/${id}/logo`;
  const response = await fetch(url, { method: "POST", body: formData });

  if (!response.ok) {
    throw new Error(`Failed to upload logo: ${response.status}`);
  }
}

export function getCompanyLogoUrl(id: string): string {
  return `${baseUrl()}/api/companies/${id}/logo`;
}

export async function deleteCompanyLogo(id: string): Promise<void> {
  const url = `${baseUrl()}/api/companies/${id}/logo`;
  const response = await fetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete logo: ${response.status}`);
  }
}

// Contact API

export interface ContactListParams {
  readonly page?: number;
  readonly size?: number;
  readonly firstName?: string;
  readonly lastName?: string;
  readonly email?: string;
  readonly companyId?: string;
  readonly language?: string;
  readonly brevo?: boolean;
}

export async function getContacts(params: ContactListParams = {}): Promise<Page<ContactDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.firstName) searchParams.set("firstName", params.firstName);
  if (params.lastName) searchParams.set("lastName", params.lastName);
  if (params.email) searchParams.set("email", params.email);
  if (params.companyId) searchParams.set("companyId", params.companyId);
  if (params.language) searchParams.set("language", params.language);
  if (params.brevo !== undefined) searchParams.set("brevo", String(params.brevo));

  const query = searchParams.toString();
  const url = `${baseUrl()}/api/contacts${query ? `?${query}` : ""}`;
  const response = await fetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch contacts: ${response.status}`);
  }

  return response.json();
}

export async function getContact(id: string): Promise<ContactDto> {
  const url = `${baseUrl()}/api/contacts/${id}`;
  const response = await fetch(url, { cache: "no-store" });

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
  const response = await fetch(url, {
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
  const response = await fetch(url, {
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
  const response = await fetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete contact: ${response.status}`);
  }
}

// Contact Photo API

export async function uploadContactPhoto(id: string, file: File): Promise<void> {
  const formData = new FormData();
  formData.append("file", file);
  const url = `${baseUrl()}/api/contacts/${id}/photo`;
  const response = await fetch(url, { method: "POST", body: formData });

  if (!response.ok) {
    throw new Error(`Failed to upload photo: ${response.status}`);
  }
}

export function getContactPhotoUrl(id: string): string {
  return `${baseUrl()}/api/contacts/${id}/photo`;
}

export async function deleteContactPhoto(id: string): Promise<void> {
  const url = `${baseUrl()}/api/contacts/${id}/photo`;
  const response = await fetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete photo: ${response.status}`);
  }
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
  const response = await fetch(url, { cache: "no-store" });

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
  const response = await fetch(url, {
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
  const response = await fetch(url, { cache: "no-store" });

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
  const response = await fetch(url, {
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
  const response = await fetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch Brevo settings: ${response.status}`);
  }

  return response.json();
}

export async function updateBrevoSettings(apiKey: string): Promise<BrevoSettingsDto> {
  const url = `${baseUrl()}/api/brevo/settings`;
  const response = await fetch(url, {
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
  const response = await fetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete Brevo settings: ${response.status}`);
  }
}

export async function startBrevoSync(): Promise<BrevoSyncResultDto> {
  const url = `${baseUrl()}/api/brevo/sync`;
  const response = await fetch(url, { method: "POST" });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to start Brevo sync: ${response.status}`);
  }

  return response.json();
}
