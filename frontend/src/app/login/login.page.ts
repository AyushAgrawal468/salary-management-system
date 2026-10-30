import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/api-error';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatProgressBarModule],
  template: `
    <main class="auth-page">
      <mat-card appearance="outlined" class="auth-card">
        @if (busy()) {
          <mat-progress-bar mode="indeterminate" />
        }
        <mat-card-header>
          <mat-card-title>Salary Management</mat-card-title>
          <mat-card-subtitle>Sign in to continue</mat-card-subtitle>
        </mat-card-header>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-card-content>
            <mat-form-field>
              <mat-label>Username</mat-label>
              <input matInput formControlName="username" autocomplete="username" required />
            </mat-form-field>
            <mat-form-field>
              <mat-label>Password</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="current-password" required />
            </mat-form-field>
            @if (error()) {
              <p class="form-error" role="alert">{{ error() }}</p>
            }
            @if (slow()) {
              <p class="hint">Waking up the server. The first sign-in after a quiet spell can take up to a minute.</p>
            }
          </mat-card-content>
          <mat-card-actions align="end">
            <button mat-flat-button type="submit" [disabled]="busy()">Sign in</button>
          </mat-card-actions>
        </form>
      </mat-card>
    </main>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly form = inject(NonNullableFormBuilder).group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });
  readonly busy = signal(false);
  readonly slow = signal(false);
  readonly error = signal('');

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.error.set('');
    // Free hosting sleeps when idle; say so instead of looking frozen.
    const slowTimer = setTimeout(() => this.slow.set(true), 5000);
    try {
      const { username, password } = this.form.getRawValue();
      const user = await this.auth.login(username, password);
      const next = user.mustChangePassword
        ? '/change-password'
        : (this.route.snapshot.queryParamMap.get('returnUrl') ?? '/');
      await this.router.navigateByUrl(next);
    } catch (e) {
      this.error.set(errorMessage(e, 'Could not sign in. Please try again.'));
    } finally {
      clearTimeout(slowTimer);
      this.busy.set(false);
      this.slow.set(false);
    }
  }
}
