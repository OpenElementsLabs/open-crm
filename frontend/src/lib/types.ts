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

export interface Page<T> {
  readonly content: readonly T[];
  readonly totalElements: number;
  readonly totalPages: number;
  readonly number: number;
  readonly size: number;
  readonly first: boolean;
  readonly last: boolean;
}
