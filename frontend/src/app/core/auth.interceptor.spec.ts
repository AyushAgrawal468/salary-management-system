import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    auth.user.set({ username: 'asha', role: 'HR_MANAGER', mustChangePassword: false });
    const router = TestBed.inject(Router);
    Object.defineProperty(router, 'url', { get: () => '/employees/42' });
    navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('on an expired session, forgets the user and returns to login', async () => {
    const call = firstValueFrom(http.get('/api/employees'));
    backend.expectOne('/api/employees').flush(null, { status: 401, statusText: 'Unauthorized' });

    await expect(call).rejects.toBeTruthy();
    expect(auth.user()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/employees/42' } });
  });

  it('leaves 401s from the auth endpoints to the login flow', async () => {
    const call = firstValueFrom(http.post('/api/auth/login', {}));
    backend.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });

    await expect(call).rejects.toBeTruthy();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('passes other errors through untouched', async () => {
    const call = firstValueFrom(http.get('/api/employees'));
    backend.expectOne('/api/employees').flush(null, { status: 500, statusText: 'Server Error' });

    await expect(call).rejects.toBeTruthy();
    expect(auth.user()).not.toBeNull();
    expect(navigate).not.toHaveBeenCalled();
  });
});
