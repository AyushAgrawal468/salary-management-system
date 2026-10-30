import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { LoginPage } from './login.page';
import { AuthService, CurrentUser } from '../core/auth.service';

describe('LoginPage', () => {
  let login: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.spyOn>;
  let returnUrl: string | null;

  async function render() {
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { login } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(returnUrl ? { returnUrl } : {}) } },
        },
      ],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    return fixture;
  }

  function fill(el: HTMLElement, username: string, password: string) {
    const [user, pass] = Array.from(el.querySelectorAll('input'));
    user.value = username;
    user.dispatchEvent(new Event('input'));
    pass.value = password;
    pass.dispatchEvent(new Event('input'));
  }

  async function submit(fixture: Awaited<ReturnType<typeof render>>) {
    (fixture.nativeElement as HTMLElement).querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  beforeEach(() => {
    returnUrl = null;
    login = vi.fn();
  });

  it('signs in and goes to the page the user asked for', async () => {
    returnUrl = '/employees/42';
    login.mockResolvedValue({ username: 'asha', role: 'HR_MANAGER', mustChangePassword: false } as CurrentUser);
    const fixture = await render();

    fill(fixture.nativeElement, 'asha', 'secret-password');
    await submit(fixture);

    expect(login).toHaveBeenCalledWith('asha', 'secret-password');
    expect(navigate).toHaveBeenCalledWith('/employees/42');
  });

  it('sends users with a temporary password to change it', async () => {
    login.mockResolvedValue({ username: 'newbie', role: 'VIEWER', mustChangePassword: true } as CurrentUser);
    const fixture = await render();

    fill(fixture.nativeElement, 'newbie', 'temporary-password');
    await submit(fixture);

    expect(navigate).toHaveBeenCalledWith('/change-password');
  });

  it('shows the server message when sign-in fails', async () => {
    login.mockRejectedValue(new HttpErrorResponse({ status: 401, error: { detail: 'Invalid username or password' } }));
    const fixture = await render();

    fill(fixture.nativeElement, 'asha', 'wrong-password');
    await submit(fixture);

    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Invalid username or password');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('does not call the server with empty fields', async () => {
    const fixture = await render();

    await submit(fixture);

    expect(login).not.toHaveBeenCalled();
  });
});
