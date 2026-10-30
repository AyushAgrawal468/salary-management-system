import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService, CurrentUser } from './auth.service';

const HR: CurrentUser = { username: 'asha', role: 'HR_MANAGER', mustChangePassword: false };

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the current session', async () => {
    const loading = auth.loadSession();
    http.expectOne('/api/auth/me').flush(HR);

    expect(await loading).toEqual(HR);
    expect(auth.user()).toEqual(HR);
  });

  it('treats 401 from /me as signed out', async () => {
    const loading = auth.loadSession();
    http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(await loading).toBeNull();
    expect(auth.user()).toBeNull();
  });

  it('asks the server only once per page load', async () => {
    const first = auth.loadSession();
    http.expectOne('/api/auth/me').flush(HR);
    await first;

    expect(await auth.loadSession()).toEqual(HR);
    http.expectNone('/api/auth/me');
  });

  it('logs in and remembers the user', async () => {
    const login = auth.login('asha', 'secret-password');
    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'asha', password: 'secret-password' });
    req.flush(HR);

    expect(await login).toEqual(HR);
    expect(auth.user()).toEqual(HR);
  });

  it('logs out and forgets the user', async () => {
    const login = auth.login('asha', 'secret-password');
    http.expectOne('/api/auth/login').flush(HR);
    await login;

    const logout = auth.logout();
    http.expectOne({ method: 'POST', url: '/api/auth/logout' }).flush(null, { status: 204, statusText: 'No Content' });
    await logout;

    expect(auth.user()).toBeNull();
  });

  it('clears the must-change flag after a password change', async () => {
    const login = auth.login('newbie', 'temporary-password');
    http.expectOne('/api/auth/login').flush({ ...HR, mustChangePassword: true });
    await login;

    const change = auth.changePassword('temporary-password', 'my-new-passphrase');
    const req = http.expectOne('/api/auth/password');
    expect(req.request.body).toEqual({ currentPassword: 'temporary-password', newPassword: 'my-new-passphrase' });
    req.flush(null, { status: 204, statusText: 'No Content' });
    await change;

    expect(auth.user()?.mustChangePassword).toBe(false);
  });

  it('lets only HR managers and admins edit', () => {
    auth.user.set({ ...HR, role: 'VIEWER' });
    expect(auth.canEdit()).toBe(false);
    auth.user.set({ ...HR, role: 'HR_MANAGER' });
    expect(auth.canEdit()).toBe(true);
    auth.user.set({ ...HR, role: 'ADMIN' });
    expect(auth.canEdit()).toBe(true);
  });
});
