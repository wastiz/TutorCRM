import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { toApiError } from './api-error';

/** Surfaces API errors as snackbars and bounces to /login on 401. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);
  const router = inject(Router);
  const auth = inject(AuthService);

  return next(req).pipe(
    catchError((err) => {
      const apiError = toApiError(err);

      if (err.status === 401 && !req.url.endsWith('/api/auth/me')) {
        auth.refresh();
        router.navigate(['/login']);
      } else if (err.status !== 401) {
        snackBar.open(apiError.message || 'Request failed', 'Dismiss', { duration: 6000 });
      }
      return throwError(() => err);
    }),
  );
};
