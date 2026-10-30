import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Signed-out users go to login (and come back afterwards); users on a temporary password may
 * only see the change-password page. The server enforces the same rules; this just keeps the UI honest.
 */
export const authGuard: CanActivateFn = async (_route, state) => {
  const router = inject(Router);
  const user = await inject(AuthService).loadSession();
  if (!user) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  if (user.mustChangePassword && state.url !== '/change-password') {
    return router.createUrlTree(['/change-password']);
  }
  return true;
};
