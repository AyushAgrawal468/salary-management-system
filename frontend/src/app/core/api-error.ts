import { HttpErrorResponse } from '@angular/common/http';

/** The API answers errors as RFC 7807 problem+json: {detail, errors?: {field: message}}. */
export function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'Cannot reach the server. Check your connection and try again.';
    const detail = error.error?.detail;
    if (typeof detail === 'string' && detail) return detail;
  }
  return fallback;
}

/** Per-field validation messages from a 400, keyed by request field name. */
export function fieldErrors(error: unknown): Record<string, string> {
  return error instanceof HttpErrorResponse && error.error?.errors ? error.error.errors : {};
}
