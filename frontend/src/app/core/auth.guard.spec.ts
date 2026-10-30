import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService, CurrentUser } from './auth.service';

describe('authGuard', () => {
  let session: CurrentUser | null;

  beforeEach(() => {
    session = null;
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { loadSession: async () => session } },
      ],
    });
  });

  const run = (url: string) =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    ) as Promise<boolean | UrlTree>;

  const serialize = (result: boolean | UrlTree) =>
    result instanceof UrlTree ? TestBed.inject(Router).serializeUrl(result) : result;

  it('sends signed-out users to login, remembering where they were going', async () => {
    expect(serialize(await run('/employees/42'))).toBe('/login?returnUrl=%2Femployees%2F42');
  });

  it('lets signed-in users through', async () => {
    session = { username: 'asha', role: 'HR_MANAGER', mustChangePassword: false };
    expect(await run('/employees')).toBe(true);
  });

  it('holds users with a temporary password on the change-password page', async () => {
    session = { username: 'newbie', role: 'VIEWER', mustChangePassword: true };
    expect(serialize(await run('/employees'))).toBe('/change-password');
    expect(await run('/change-password')).toBe(true);
  });
});
