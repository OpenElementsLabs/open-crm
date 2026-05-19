/**
 * Thrown by API client implementations when the backend returns HTTP 403.
 * Callers can distinguish this from generic errors via `instanceof ForbiddenError`
 * to surface a permission-specific message.
 */
export class ForbiddenError extends Error {
  constructor(message = "Forbidden") {
    super(message);
    this.name = "ForbiddenError";
  }
}
