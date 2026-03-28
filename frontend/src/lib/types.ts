export interface CompanyDto {
  readonly id: string;
  readonly name: string;
  readonly email: string | null;
  readonly website: string | null;
  readonly street: string | null;
  readonly houseNumber: string | null;
  readonly zipCode: string | null;
  readonly city: string | null;
  readonly country: string | null;
  readonly deleted: boolean;
  readonly hasLogo: boolean;
  readonly contactCount: number;
  readonly commentCount: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CompanyCreateDto {
  readonly name: string;
  readonly email?: string | null;
  readonly website?: string | null;
  readonly street?: string | null;
  readonly houseNumber?: string | null;
  readonly zipCode?: string | null;
  readonly city?: string | null;
  readonly country?: string | null;
}

export interface CommentDto {
  readonly id: string;
  readonly text: string;
  readonly author: string;
  readonly companyId: string | null;
  readonly contactId: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CommentCreateDto {
  readonly text: string;
}

export interface ContactDto {
  readonly id: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string | null;
  readonly position: string | null;
  readonly gender: "MALE" | "FEMALE" | "DIVERSE" | null;
  readonly linkedInUrl: string | null;
  readonly phoneNumber: string | null;
  readonly companyId: string | null;
  readonly companyName: string | null;
  readonly companyDeleted: boolean;
  readonly commentCount: number;
  readonly hasPhoto: boolean;
  readonly birthday: string | null;
  readonly syncedToBrevo: boolean;
  readonly doubleOptIn: boolean;
  readonly language: "DE" | "EN";
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ContactCreateDto {
  readonly firstName: string;
  readonly lastName: string;
  readonly email?: string | null;
  readonly position?: string | null;
  readonly gender?: "MALE" | "FEMALE" | "DIVERSE" | null;
  readonly linkedInUrl?: string | null;
  readonly phoneNumber?: string | null;
  readonly companyId?: string | null;
  readonly language: "DE" | "EN";
  readonly birthday?: string | null;
}

export interface Page<T> {
  readonly content: readonly T[];
  readonly totalElements: number;
  readonly totalPages: number;
  readonly number: number;
  readonly size: number;
  readonly first: boolean;
  readonly last: boolean;
}
