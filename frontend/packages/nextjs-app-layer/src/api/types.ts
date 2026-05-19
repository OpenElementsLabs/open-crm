export interface Page<T> {
  readonly content: readonly T[];
  readonly page: {
    readonly size: number;
    readonly number: number;
    readonly totalElements: number;
    readonly totalPages: number;
  };
}

export interface UserDto {
  readonly id: string;
  readonly name: string;
  readonly email: string;
  readonly avatarUrl: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export type AuditAction = "INSERT" | "UPDATE" | "DELETE";

export interface AuditLogDto {
  readonly id: string;
  readonly entityType: string;
  readonly entityId: string;
  readonly action: AuditAction;
  readonly user: UserDto;
  readonly createdAt: string;
}

export interface ApiKeyDto {
  readonly id: string;
  readonly name: string;
  readonly keyPrefix: string;
  readonly createdBy: string;
  readonly createdAt: string;
}

export interface ApiKeyCreateDto {
  readonly name: string;
}

export interface ApiKeyCreatedDto {
  readonly id: string;
  readonly name: string;
  readonly keyPrefix: string;
  readonly key: string;
  readonly createdBy: string;
  readonly createdAt: string;
}

export interface WebhookDto {
  readonly id: string;
  readonly url: string;
  readonly active: boolean;
  readonly lastStatus: number | null;
  readonly lastCalledAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface WebhookCreateDto {
  readonly url: string;
}

export interface WebhookUpdateDto {
  readonly url: string;
  readonly active: boolean;
}

export interface TranslationConfigDto {
  readonly configured: boolean;
}

export interface PageRequest {
  readonly page: number;
  readonly size: number;
}
