import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.loaded()) {
    await auth.init();
  }
  if (auth.isAuthenticated()) {
    return true;
  }
  return router.parseUrl('/login');
};

export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.loaded()) {
    await auth.init();
  }
  return auth.isAuthenticated() ? router.parseUrl('/') : true;
};
