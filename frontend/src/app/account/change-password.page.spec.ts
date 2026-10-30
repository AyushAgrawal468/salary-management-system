import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { ChangePasswordPage } from './change-password.page';
import { AuthService } from '../core/auth.service';

describe('ChangePasswordPage', () => {
  let changePassword: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.spyOn>;

  async function render() {
    TestBed.configureTestingModule({
      imports: [ChangePasswordPage],
      providers: [provideRouter([]), { provide: AuthService, useValue: { changePassword } }],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(ChangePasswordPage);
    await fixture.whenStable();
    return fixture;
  }

  async function fillAndSubmit(fixture: Awaited<ReturnType<typeof render>>, current: string, next: string, confirm: string) {
    const el = fixture.nativeElement as HTMLElement;
    const inputs = Array.from(el.querySelectorAll('input'));
    [current, next, confirm].forEach((value, i) => {
      inputs[i].value = value;
      inputs[i].dispatchEvent(new Event('input'));
    });
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  beforeEach(() => {
    changePassword = vi.fn();
  });

  it('changes the password and continues into the app', async () => {
    changePassword.mockResolvedValue(undefined);
    const fixture = await render();

    await fillAndSubmit(fixture, 'temporary-password', 'my-new-passphrase', 'my-new-passphrase');

    expect(changePassword).toHaveBeenCalledWith('temporary-password', 'my-new-passphrase');
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('requires the confirmation to match', async () => {
    const fixture = await render();

    await fillAndSubmit(fixture, 'temporary-password', 'my-new-passphrase', 'something-else-entirely');

    expect(changePassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Passwords do not match');
  });

  it('requires at least 12 characters', async () => {
    const fixture = await render();

    await fillAndSubmit(fixture, 'temporary-password', 'short', 'short');

    expect(changePassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('At least 12 characters');
  });

  it('shows the server message when the change is refused', async () => {
    changePassword.mockRejectedValue(
      new HttpErrorResponse({ status: 400, error: { detail: 'Current password is incorrect' } }),
    );
    const fixture = await render();

    await fillAndSubmit(fixture, 'wrong-current', 'my-new-passphrase', 'my-new-passphrase');

    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Current password is incorrect');
    expect(navigate).not.toHaveBeenCalled();
  });
});
