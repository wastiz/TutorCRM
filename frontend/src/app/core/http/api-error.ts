import { HttpErrorResponse } from '@angular/common/http';

export interface ApiError {
  code: string;
  message: string;
  timestamp: string;
  path: string;
  errors?: { field: string; message: string }[];
}

export function toApiError(err: unknown): ApiError {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as Partial<ApiError> | undefined;
    if (body && typeof body === 'object' && 'code' in body) {
      return body as ApiError;
    }
    return {
      code: `HTTP_${err.status}`,
      message: err.status === 0 ? 'Cannot reach the server' : err.message,
      timestamp: new Date().toISOString(),
      path: err.url ?? '',
    };
  }
  return {
    code: 'UNKNOWN',
    message: String(err),
    timestamp: new Date().toISOString(),
    path: '',
  };
}
